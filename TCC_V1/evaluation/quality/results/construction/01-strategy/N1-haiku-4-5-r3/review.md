# Revisão: construção, cenário 01

## Visão geral

O projeto tem nove pacotes em camadas. `api` tem os controllers do Javalin com métodos estáticos `routes(app)`. `service` tem `ReservationService` e `ReportService`. `rateplan` tem a interface `RatePlanStrategy`, com cinco implementações e uma `RatePlanFactory`. `repository` faz JDBC puro com `PreparedStatement`. `infrastructure.Database` guarda o pool Hikari num `static` e cria o esquema com `CREATE TABLE IF NOT EXISTS`. Por fim, há `model`, `util` (`Money` e `Season`) e `api.dto`. Uma requisição entra no handler (`ReservationController`), que monta os parâmetros e chama o `ReservationService`. O serviço valida, pede o preço ao plano e grava pelo `ReservationRepository` no PostgreSQL.

O mais sério é que **o sistema não sobe**. As rotas usam a sintaxe `:id` do Javalin 3 (`api/ReservationController.java:75` e seguintes), e o Javalin 6.7.0 recusa isso ao registrar a rota (`IllegalArgumentException: Path '/reservations/:id' invalid`). Os roteiros do banco e do comportamento não conseguiram chamar nenhum endpoint. O `mvn verify` passa porque os 20 testes só exercitam os planos e os utilitários, nunca a aplicação.

Mesmo que essas rotas fossem corrigidas, ficam dois defeitos graves:
- A confirmação grava `amountPaid = 0` quando o corpo não traz o valor (`api/ReservationController.java:96-101`). Com isso, devoluções e relatório financeiro saem zerados.
- Cada partida apaga e regrava os quartos (`infrastructure/Database.java:87`). Isso falha por chave estrangeira assim que existe uma reserva, e o serviço deixa de subir sobre um banco com dados.

## Arquitetura

**Números:** 9 pacotes, 14 dependências entre eles, nenhum ciclo (`measures/dependencies.md`). Javalin só em `com.pousada` e `com.pousada.api`, JDBC só em `repository` e `infrastructure`. A maior classe é `ReservationService` (275 linhas), seguida de `ReservationController` (219).
**Nota:** com problemas

As camadas existem, e as dependências apontam do HTTP para o serviço e do serviço para os repositórios. As regras (`service`, `rateplan`) não conhecem Javalin nem JDBC, e o SQL fica todo em `repository/`. O que pesa:

- **Montagem espalhada e estado global.** Cada controller cria por dentro os seus repositórios e serviços em campos `static final`:
  - `api/ReservationController.java:23-27`;
  - `api/ReportController.java:14-16`;
  - `api/RoomController.java:13`;
  - `api/MessageController.java:12`.

  Há, portanto, três instâncias de `ReservationRepository` e de `RoomRepository`. Os repositórios, por sua vez, chamam o `static` global `Database.getConnection()` (`infrastructure/Database.java:11`, `:99`). Assim, nada pode ser substituído num teste, e o `main` (`Application.java:9-16`) só liga as rotas.
- **A exceção de domínio mora no pacote dos planos.** `RatePlanStrategy.InvalidReservationException` (`rateplan/RatePlanStrategy.java:37-44`) serve para `ROOM_NOT_FOUND`, `RESERVATION_NOT_FOUND` e `OPERATION_NOT_ALLOWED` (`service/ReservationService.java:43`, `:91`, `:96`). É por isso que `api` depende de `rateplan`.
- **A borda HTTP faz trabalho de regra.**
  - `GET /reservations/:id` pula o serviço e vai direto ao repositório (`api/ReservationController.java:78`).
  - A decisão de quanto foi pago fica no handler (`api/ReservationController.java:96-101`).
  - O mapeamento de erro para status HTTP está copiado em seis handlers (`api/ReservationController.java:62-72`, `:108-114`, `:124-130`, …), em vez de um `app.exception` único.
- **Formatação no serviço.** O texto do comprovante e do e-mail fica dentro do `ReservationService` (`service/ReservationService.java:109-124`, `:244-274`), o que ajuda a fazer dele a maior classe.

Nada disso é uma mistura grosseira (não há SQL em handler nem regra dependendo do Javalin). Por isso a nota é `com problemas`, e não `ruim`.

## Design de baixo nível

**Números:** 5 planos, todos com classe própria (`rateplan/*RatePlan.java`). Validação, preço e devolução ficam no plano. Há um `switch` só de seleção (`rateplan/RatePlanFactory.java:7-13`) e um `if (ratePlan == CORPORATE)` de comportamento no serviço (`service/ReservationService.java:60-64`). O mesmo laço de diárias com temporada está copiado em 4 planos, e a mesma checagem de capacidade em 5.
**Classe:** malfeito
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** O que aproxima do Strategy: a interface `RatePlanStrategy` (`rateplan/RatePlanStrategy.java:9-21`) tem `validate`, `calculatePrice` e `calculateRefund`. Cada um dos cinco planos, os dois antigos inclusive, implementa as três operações:
- as regras de quem pode usar (sexta a domingo em `WeekendPackageRatePlan.java:21-32`, 7 noites em `LongStayRatePlan.java:17-20`, código da empresa em `CorporateRatePlan.java:21-23`);
- o preço;
- a devolução (`NonRefundableRatePlan.java:41-43`, `WeekendPackageRatePlan.java:48-50`).

O cancelamento pede a devolução ao plano sem nomeá-lo (`service/ReservationService.java:229-230`). A seleção é um único `switch` na fábrica (`RatePlanFactory.java:7-13`), o que a referência aceita.

O que afasta: o serviço ainda pergunta qual é o plano para calcular o preço (`service/ReservationService.java:60-64`). O convênio tem uma sobrecarga própria, e a versão de três argumentos do `CorporateRatePlan` lança exceção (`rateplan/CorporateRatePlan.java:50-54`), o que quebra o contrato da interface. A correção era trivial: passar sempre o `companyCode`, já que o método `default` de quatro argumentos existe (`RatePlanStrategy.java:16-19`). Do jeito que está, um próximo plano que precise de um dado da requisição (a data da reserva, no plano dos 60 dias de antecedência) leva outro `if` ao serviço. Isso se encaixa em "quem usa a estrutura ainda perguntando qual é o caso", daí `malfeito`.

**O exagero.** A fábrica é o ponto único de seleção do pattern, e não há interfaces de uma implementação só, builders nem camadas que só repassam. Daí `contenção`.

**O resto do código** tem defeitos que um revisor pediria para mudar, a maioria duplicação:

- **Duplicação nos planos.**
  - O cálculo das diárias com alta temporada está copiado quatro vezes: `StandardRatePlan.java:24-30`, `NonRefundableRatePlan.java:26-32`, `CorporateRatePlan.java:35-41` e `LongStayRatePlan.java:32-38`. Mudar a regra da temporada exige mexer em quatro arquivos.
  - A checagem de capacidade, que é regra comum e não do plano, está repetida nos cinco planos (por exemplo, `StandardRatePlan.java:14-16`). Ela devolve `CAPACITY_EXCEEDED` (422) também para `guests < 1`, quando o texto pede 400.
- **Duplicação no serviço.**
  - O `Document` é montado três vezes com os mesmos onze argumentos (`service/ReservationService.java:127-139`, `:156-167`, `:203-214`).
  - O padrão "busca, `RESERVATION_NOT_FOUND`, confere a situação" aparece em cinco métodos.
- **Formatação de dinheiro errada.** `String.format("R$ %,.2f", amount).replace(",", ".")` (`service/ReservationService.java:267`) nunca produz "R$ 1.234,56":
  - no locale inglês sai "R$ 779.80";
  - no português, a vírgula decimal vira ponto ("R$ 1.234.56").

  O comprovante do e-mail sai fora do formato pedido. O `%.2f` do CSV (`service/ReportService.java:80`) também depende do locale.
- **Tipos e estado.**
  - Os códigos de erro são `String` soltas (`"ROOM_NOT_FOUND"`, …), comparadas com `equals` no controller.
  - O DTO tem campos públicos mutáveis (`api/dto/ReservationRequest.java:4-12`). Usa `int` com `0` como "faltou" (`api/ReservationController.java:34-35`), o que confunde campo ausente com valor zero.
  - `BigDecimal.valueOf(520.00)` vira `520.0`, com escala 1 (`WeekendPackageRatePlan.java:13-15`), e assim sai no JSON da criação.
- **Restos.**
  - A constante `DISCOUNT_PERCENT` não é usada, e o literal `15` é repetido (`NonRefundableRatePlan.java:11` e `:34`).
  - Há imports sem uso (`HashMap` em `MessageController.java:7`, `ObjectMapper` em `RoomController.java:4`).

A dinheiro, o projeto usa `BigDecimal` com `HALF_EVEN` a cada passo (`util/Money.java:9-27`); as datas usam `LocalDate`; os estados e os planos são `enum` (`model/Reservation.java:7-13`).

## Testes

**Números:** 20 testes rodaram, 0 falharam (20 métodos escritos, 7 classes). Cobertura de linhas de 108/845 (12,8%) e de ramos de 41/197 (20,8%). Por pacote, `rateplan` tem 82/106 linhas e `util` 15/17. Já `api`, `service`, `repository` e `infrastructure` têm 0%. Não há nenhum teste pela API ou contra o banco (`measures/tests.md`).
**Nota:** com problemas

O que está testado, com valores:
- os três exemplos do texto:
  - Tecnosul, R$ 779,80 / 93,58 / 686,22 (`CorporateRatePlanTest.java:16-28`);
  - pacote deluxe, R$ 820,00, com devolução de R$ 410,00 (`WeekendPackageTest.java:17-35`);
  - estadia longa, R$ 1.749,30 / 174,93 / 1.574,37 (`LongStayTest.java:16-28`);
- o não reembolsável a 15% (`NonRefundableTest.java:21-32`);
- o meio-para-o-par (`MoneyTest.java:11-16`);
- os limites da temporada (`SeasonTest.java:17-21`);
- as recusas de código ausente ou inválido, de menos de 7 noites e de quinta a sábado (`CorporateRatePlanTest.java:31-50`, `LongStayTest.java:31-39`, `WeekendPackageTest.java:38-46`).

O que fica sem rede (a pergunta da mutação):
- Trocar `"AGROVALE", 18` por `81`, ou o 25 da Farmavida, não derruba nenhum teste: só a Tecnosul é conferida.
- Trocar os preços do pacote standard ou suíte (`WeekendPackageRatePlan.java:13,15`) também passa.
- Apagar a checagem `days != 2` (`WeekendPackageRatePlan.java:29-32`) também passa. O único caso negativo falha antes, no dia da semana.
- A estadia longa em alta temporada não é testada, e a devolução integral do convênio também não.

Fora dos planos, nada do serviço tem teste: as transições de situação, o cancelamento de reserva pendente (devolução zero), o comprovante, o e-mail, os relatórios, os códigos HTTP. Por isso os testes não pegaram os defeitos que derrubam o sistema:
- as rotas inválidas, que impedem o serviço de subir;
- o `amountPaid` zerado na confirmação;
- o formato do dinheiro;
- a falha ao reiniciar.

Os testes que existem são bons e conferem valores. Não chegam a `ruim`, mas cobrem só a camada dos planos.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. O dinheiro está em `numeric(10,2)` e as datas em `date`. As conferências de `measures/database.md` acusaram:
- uma chave estrangeira sem índice (`reservations.room_number`);
- nenhum CHECK nem UNIQUE/EXCLUDE;
- status, plano, tipo e canal em `varchar` sem CHECK;
- 0 transações explícitas e 0 `FOR UPDATE`;
- um SQL montado com `+`;
- esquema criado no código, sem migrações.

O serviço não subiu contra o banco vazio, por causa da rota inválida (ver Comportamento), mas o esquema foi criado antes da falha.
**Nota:** ruim

- **O serviço não volta depois de ter dados.** A cada partida, `insertRoomsIfEmpty` executa `DELETE FROM rooms` e reinsere os cinco quartos (`infrastructure/Database.java:85-93`; o nome do método não corresponde ao que ele faz). Assim que existe uma reserva, a chave estrangeira `reservations.room_number → rooms` (`Database.java:49`) faz o `DELETE` falhar. Conferi isso no PostgreSQL desta revisão: `ERROR: update or delete on table "rooms" violates foreign key constraint "reservations_room_number_fkey"`. O erro vira `RuntimeException` (`Database.java:94-95`), e o `main` morre. Os dados não se perdem, mas o sistema fica fora do ar no primeiro reinício com reservas, que é exatamente o que o texto pede para não acontecer.
- **Nada impede a reserva dupla.** O serviço consulta a disponibilidade (`service/ReservationService.java:52`) e insere depois (`:84`). São duas conexões em autocommit, sem transação, sem trava e sem restrição `EXCLUDE` no banco. Dois pedidos simultâneos para o mesmo quarto e as mesmas noites podem passar os dois.
- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` no código (`Database.java:35-77`) não sabe acrescentar uma coluna a uma tabela que já existe.
- **Faltam restrições.** Não há CHECK em `status`, `rate_plan` nem `guests`, e falta o índice em `reservations(room_number)`, que é justamente a coluna da consulta de disponibilidade (`ReservationRepository.java:113-114`).
- **SQL montado com `+`.** `RoomRepository.java:17` concatena o número do quarto. O valor é um `int` (vem do JSON já convertido), então não há injeção, mas é o único lugar sem `?`.
- **O que está certo:** os tipos, o pool Hikari, o try-with-resources nas conexões e os `PreparedStatement` com parâmetros no resto. Há um detalhe, porém: os `ResultSet` não ficam no try-with-resources (`ReservationRepository.java:64`, `MessageRepository.java:45`). Eles se fecham com o statement, então não chega a vazar.

A nota é `ruim` porque o serviço não sobe contra o banco vazio, e porque o próprio código do banco impede o reinício assim que há uma reserva.

## Comportamento

**Números:** 0 conferências passaram e 0 falharam. O roteiro `check_01.sh` não pôde rodar, porque o serviço não subiu contra o banco vazio (`measures/behavior.md`). O log (`measures/behavior-service.log`) diz: `IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'`, em `ReservationController.routes(ReservationController.java:75)`, chamado de `Application.main(Application.java:14)`.
**Nota:** ruim

Para o dono da pousada, isso quer dizer que o sistema entregue não funciona. O `java -jar` termina com erro antes de abrir a porta, e o site não consegue nem listar os quartos. Nenhuma das seis rotas com `/:id` (`api/ReservationController.java:75`, `:91`, `:117`, `:133`, `:149`, `:165`) é válida no Javalin 6.7.0 que o texto pediu.

Pela leitura do código, mesmo com as rotas corrigidas, falhariam pontos que o texto descreve.

Problemas graves:
- **Confirmação sem o pagamento.** O texto diz que o que o hóspede pagou é o total da reserva, mas `POST /reservations/{id}/confirmation` sem corpo grava `amountPaid = 0` (`api/ReservationController.java:96-104`). Daí, toda devolução sai R$ 0,00 (o pacote cancelado devolveria 0, e não R$ 410,00), e o relatório financeiro sai vazio, porque filtra `amount_paid > 0` (`repository/ReservationRepository.java:167`).
- **Comprovante e e-mail fora do formato.** O dinheiro sai como "R$ 779.80" (`service/ReservationService.java:267`), e não "R$ 779,80".
- **Falha ao reiniciar.** O serviço não volta depois da primeira reserva (`infrastructure/Database.java:87`). Pesa na coluna do banco.
- **Reserva dupla possível.** Duas reservas simultâneas podem passar as duas (`service/ReservationService.java:52-84`). Também pesa na coluna do banco.

Problemas menores:
- Corpo que não é JSON dá 500 em vez de 400 (`api/ReservationController.java:32`, sem `catch` para o erro do Jackson).
- `guests` negativo dá 422 em vez de 400 (`rateplan/StandardRatePlan.java:14-15`).
- O financeiro inclui reservas canceladas com devolução total (dinheiro líquido 0), porque filtra pelo pago, e não pelo pago menos o devolvido (`repository/ReservationRepository.java:167`).

## Dúvidas

- **Classe do pattern (`malfeito` ou `acerto`):** os cinco planos estão inteiros na estrutura (preço, validação e devolução), e o único desvio é o `if (ratePlan == CORPORATE)` que escolhe a sobrecarga de preço (`service/ReservationService.java:60-64`). Dei `malfeito` porque é o serviço perguntando qual é o plano dentro do cálculo do preço, e porque a sobrecarga de três argumentos do convênio lança exceção. Um leitor mais tolerante poderia ver aí só um defeito de assinatura e dar `acerto`.
- **Formato do CPF:** o texto não diz se o CPF chega com ou sem pontuação. `formatCpf` (`service/ReservationService.java:270-274`) só funciona se ele vier sem pontuação; com "111.222.333-44" na entrada, o comprovante sairia com a pontuação embaralhada.
- **Charset do CSV:** os relatórios usam `ctx.contentType("text/csv")` sem `charset=utf-8` (`api/ReportController.java:29`, `:45`). Não deu para ver o cabeçalho real, porque o serviço não subiu.

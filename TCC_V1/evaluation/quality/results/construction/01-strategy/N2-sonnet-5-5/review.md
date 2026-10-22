# Revisão: construção, cenário 01

## Visão geral

O projeto tem 7 pacotes: `web` (rotas do Javalin e o leitor do corpo da criação), `service` (os casos de uso, os
relatórios e o comprovante), `plan` (os planos de tarifa), `domain` (records e enums: reserva, quarto, temporada,
dinheiro), `persistence` (JDBC puro), `error` e a montagem em `App`. Uma requisição entra em `web/Routes.java:36-46`.
No `POST /reservations`, o corpo é validado em `web/CreateReservationParser.java:23-53` e vira um comando
`CreateReservation`. Depois vem `ReservationService.create` (`service/ReservationService.java:53-71`), que abre uma
transação (`persistence/Database.java:25-39`), trava a linha do quarto (`RoomRepository.java:30`), pede o preço ao
plano (`plan/RatePlan.java:32-37`) e grava pelos repositórios. Os dados ficam no PostgreSQL, em três tabelas criadas
por `CREATE TABLE IF NOT EXISTS` no código (`persistence/Database.java:67-110`). Os cinco planos ficam numa interface
com uma classe por plano, e o roteiro de comportamento passou inteiro (72 de 72). Nada é grave. O mais fraco são os
testes (1 classe, 16,4% das linhas) e o banco, que não tem migrações, não tem índice na chave estrangeira, não tem
restrições `CHECK` e não usa pool de conexões.

## Arquitetura

**Números:** 7 pacotes, 16 dependências entre eles, nenhum ciclo. Javalin só em `com.pousada` e `web`; JDBC em `persistence` e `service` (só o `Connection`/`SQLException` da transação); a maior classe é `ReservationService` (143 linhas).
**Nota:** bom

- **A borda HTTP é fina.** Cada rota é uma linha que lê o pedido, chama o serviço e escreve a resposta
  (`web/Routes.java:36-46`). Os erros viram JSON num tratador único (`web/Routes.java:30-34`). A validação do corpo
  fica separada, em `web/CreateReservationParser.java`.
- **As regras não conhecem Javalin nem SQL.** As transições de situação ficam em `domain/Reservation.java:28-55`, o
  preço e a devolução nos planos (`plan/*`) e a temporada em `domain/Season.java:11-14`. O serviço orquestra e não
  monta SQL nenhum. Todo o SQL fica nos repositórios (`persistence/ReservationRepository.java`,
  `RoomRepository.java`, `MessageRepository.java`).
- **A montagem fica num lugar só,** em `App.java:20-33`.
- As dependências vão de `web` para `service`, de `service` para `plan`/`persistence` e daí para `domain`, sem ciclo.

Pontos menores, que não mudam a nota:

- o serviço recebe e repassa o `java.sql.Connection` da transação (`service/ReservationService.java:116-126`), então
  o JDBC aparece na camada de regras;
- o domínio usa `@JsonIgnore` do Jackson (`domain/Reservation.java:26`) e lança `ApiException`, que carrega o status
  HTTP (`error/ApiException.java:23`);
- `Routes` cria o próprio parser e pega o `RatePlans` por um getter do serviço (`web/Routes.java:26`,
  `service/ReservationService.java:49-51`), em vez de receber os dois prontos da montagem.

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (interface `RatePlan` com 5 implementações, escolhidas por um mapa em `RatePlans`); 0 `switch`/`if` de comportamento sobre o plano no cálculo ou no cancelamento; 1 regra de plano fora dele, pequena e que não é de preço (`ReservationService.java:65`).
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

**O pattern (Strategy).** `plan/RatePlan.java:11-38` define o que cada plano decide:

- a validação (`validate`);
- a soma das diárias (`subtotal`);
- o desconto (`discount`);
- a devolução (`refund`).

Os valores padrão servem ao plano normal. Os cinco planos, inclusive os dois da parte comum, são classes próprias:
`StandardPlan.java:3-9`, `NonRefundablePlan.java:14-22`, `CorporatePlan.java:11-32` (a tabela das empresas e a recusa
sem código), `WeekendPackagePlan.java:13-40` (sexta, duas noites, preço fechado por tipo, metade de volta) e
`LongStayPlan.java:10-27`. A escolha é feita num ponto só, `RatePlans.java:14-33`, e um plano novo entra só na lista de
`RatePlans.defaults()`. Quem usa os planos não pergunta qual é: o preço sai de `plan.quote(...)`
(`service/ReservationService.java:60-61`) e a devolução de `plans.get(current.ratePlan()).refund(...)`
(`service/ReservationService.java:91`). Isso é acerto.

O único ponto que afasta é `"CORPORATE".equals(plan.code())` em `service/ReservationService.java:65`, que só decide se
o código da empresa é guardado. A referência manda contar esse caso nas dúvidas, e ele não muda a classe.

**O exagero.** `PricingRequest` e `PriceQuote` (`plan/PricingRequest.java`, `plan/PriceQuote.java`) carregam dados de
que os planos precisam. `RatePlans` é o ponto de seleção. `CreateReservation` é o comando. Não há interface com uma
implementação só, fábrica, builder nem camada que só repassa. Isso é contenção.

**O resto do código.**

- Os tipos são os certos para o domínio. Dinheiro usa `BigDecimal` com `HALF_EVEN` a cada passo
  (`domain/Money.java:13-23`, `domain/Room.java:8-10`), as datas usam `LocalDate` e as situações e tipos de quarto
  usam `enum` (`ReservationStatus.java`, `RoomType.java`).
- A reserva é um record imutável, e cada transição devolve uma cópia nova (`domain/Reservation.java:57-60`).
- Os métodos são curtos e não há duplicação que se note.

Detalhes que não pesam:

- o plano da reserva é guardado como `String` (`domain/Reservation.java:18`), o que faz sentido com planos abertos;
- `RatePlans.get` lança `IllegalStateException` para um código desconhecido no banco (`plan/RatePlans.java:31-33`),
  e isso viraria um 500;
- `ReceiptFormatter` é um utilitário estático (`service/ReceiptFormatter.java:14-53`).

## Testes

**Números:** 6 testes rodaram, 0 falharam, 0 pulados; 6 métodos escritos, numa classe só (`RatePlansTest`); cobertura de 16,4% das linhas (68/414) e 20,2% dos ramos (21/104). No pacote `plan`: 53/54 linhas e 14/14 ramos. Nos pacotes `service`, `persistence` e `web`: 0%. Nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que existe é bom e confere valores. `src/test/java/com/pousada/plan/RatePlansTest.java` testa:

- a alta temporada com o exemplo do texto, 599,76 (`:24-28`);
- a Tecnosul com 779,80 / 93,58 / 686,22 (`:30-36`);
- o convênio sem código e com código desconhecido, que são recusados (`:38-42`);
- o pacote: 820,00, a devolução de 410,00, a entrada no sábado e a estadia de 3 noites recusadas (`:44-51`);
- a estadia longa: 1.749,30 / 174,93 / 1.574,37, e 6 noites recusadas (`:53-60`);
- o não reembolsável: 424,83 e nada de volta (`:62-67`).

A pergunta da mutação na parte nova:

- trocar 0.12 por 0.21 da Tecnosul faria um teste falhar;
- trocar `< 7` por `<= 7` em `LongStayPlan.java:19` faria um teste falhar (7 noites é testado como válido);
- trocar os 18% da Agrovale ou os 25% da Farmavida (`CorporatePlan.java:13-14`) não faria nenhum teste falhar;
- a devolução integral do convênio e da estadia longa não é testada;
- o pacote em alta temporada não é testado, então ninguém confere que o preço fica fechado o ano todo;
- os preços do pacote no standard (520,00) e na suíte (1.200,00) não são testados.

Fica tudo sem rede o que não é plano:

- as transições de situação e as recusas 409 (`domain/Reservation.java:28-55`);
- a sobreposição de noites (`ReservationRepository.java:75-87`);
- o comprovante e o e-mail (`service/ReceiptFormatter.java`);
- os dois relatórios CSV (`service/ReportService.java`);
- os códigos de erro da API (`web/CreateReservationParser.java`);
- a persistência.

O texto não pedia testes. Mesmo assim, o que o agente escreveu cobre só uma parte das regras.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro sempre em `numeric(12,2)`; datas em `date`. Conferências que acusaram algo: 1 chave estrangeira sem índice (`reservations.room_number`); 0 `CHECK`, `UNIQUE` ou `EXCLUDE` (status, plano, tipo e canal em `varchar` livre); esquema por `CREATE TABLE` no código, sem migrações; sem pool (`DriverManager.getConnection`, 1 chamada); 1 transação explícita; 2 `FOR UPDATE`.
**Nota:** com problemas

O que está certo:

- **Os tipos e as chaves.** Dinheiro em `NUMERIC(12,2)` e datas em `DATE` (`persistence/Database.java:71-101`), a
  reserva ligada ao quarto por chave estrangeira (`Database.java:79`) e `NOT NULL` em tudo que é obrigatório.
- **O SQL tem parâmetros.** Os trechos montados com `+` só juntam constantes: `COLUMNS`, `" FOR UPDATE"` e os
  `where`/`order` fixos de `ReservationRepository.java:91-98`.
- **As conexões fecham sempre,** por try-with-resources (`Database.java:26`).
- **A regra do quarto vale entre processos.** Toda escrita de um caso de uso fica numa transação só
  (`Database.java:25-39`). A criação trava a linha do quarto com `FOR UPDATE` (`RoomRepository.java:30-31`, chamado
  em `ReservationService.java:55`) antes de checar a sobreposição (`ReservationRepository.java:75-87`), e isso
  protege a regra mesmo com mais de um processo: as 10 rodadas simultâneas passaram.
- **Os quartos são semeados de forma idempotente** (`Database.java:102-109`, `ON CONFLICT DO NOTHING`), e um advisory
  lock evita a corrida na criação do esquema (`Database.java:69`).

Os defeitos:

- **O esquema não evolui.** `CREATE TABLE IF NOT EXISTS` no código (`Database.java:70-101`) não sabe acrescentar uma
  coluna numa tabela que já existe. O próximo plano que precisar de um campo novo vai exigir uma alteração à mão no
  banco de quem já roda o sistema.
- **A chave estrangeira `reservations.room_number` não tem índice** (`Database.java:79`). Também falta um índice para
  a consulta de sobreposição por quarto e datas.
- **O banco não protege nenhuma regra.** Não há `CHECK` em `status`, `rate_plan`, `guests >= 1` nem
  `check_out > check_in`, e a reserva dupla é evitada só pela trava no código, sem um `EXCLUDE` no banco.
- **Não há pool.** Cada requisição abre uma conexão nova com `DriverManager` (`Database.java:26`).

## Comportamento

**Números:** 72 de 72 conferências passaram, 0 falharam. Parte comum: 46/46. Dados depois de reiniciar: 3/3. Cenário 01 (planos novos): 21/21. Duas reservas ao mesmo tempo: 2/2.
**Nota:** bom

Para o dono da pousada, o sistema faz o que o texto pede, inclusive os exemplos do financeiro:

- a Tecnosul dá 686,22;
- a Agrovale e a Farmavida dão 18% e 25%;
- o convênio sem código, ou com um código que não existe, é recusado com 400;
- o pacote custa 820,00 e volta 410,00 no cancelamento; fica em 520,00, 820,00 ou 1.200,00 conforme o quarto e não
  muda na alta temporada; só pode ir de sexta a domingo;
- a estadia longa dá 1.574,37, pede 7 noites e cobra a temporada;
- o convênio e a estadia longa devolvem tudo no cancelamento.

A parte comum também passou inteira: o preço com a temporada e o arredondamento, os códigos de erro, o e-mail com o
comprovante, o check-in e o check-out, os cancelamentos, os relatórios CSV, os dados depois de reiniciar e as reservas
simultâneas do mesmo quarto.

## Dúvidas

- `service/ReservationService.java:65` decide fora do plano, por `"CORPORATE".equals(plan.code())`, se guarda o
  código da empresa. É uma regra de plano pequena e que não é de preço nem de devolução, e por isso não mudou a
  classe (acerto). Ainda assim, é um lugar que nomeia um plano fora da estrutura.
- Um plano de tarifa desconhecido é recusado com 400 `INVALID_REQUEST` (`web/CreateReservationParser.java:47-49`). O
  texto não fecha esse caso, e a escolha parece razoável.

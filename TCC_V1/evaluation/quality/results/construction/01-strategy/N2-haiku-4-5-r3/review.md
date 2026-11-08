# Revisão: construção, cenário 01

## Visão geral

O projeto (`projeto/`) separa o código em `api` (handlers do Javalin, todos métodos `static`), `service` (`ReservationService`), `rateplan` (uma interface `RatePlan` e uma classe por plano, escolhida por `RatePlanFactory`), `database` (DAOs com JDBC e um `HikariDataSource` estático) e `model`/`util`. Uma requisição entra em `Application.java:13-23` e vai direto a um método estático do handler. O handler lê o JSON como `Map`, chama o serviço, o serviço chama os DAOs, e os dados ficam em três tabelas que `Database.java:33-79` cria com `CREATE TABLE IF NOT EXISTS`. O Strategy dos planos de tarifa está bem feito, e os preços dos cinco planos batem com o texto. O mais sério está no ciclo de vida da reserva. A confirmação exige no corpo um campo `amountPaid` que o texto não prevê e grava qualquer valor enviado (`ReservationHandler.java:76-83`). Do jeito que o site chama, ela responde 400, então nenhuma reserva chega a ser confirmada, a receber e-mail, a fazer check-in ou check-out, nem a receber devolução. Além disso, dá para cancelar depois do check-in (`ReservationService.java:121`). Nada impede duas reservas do mesmo quarto feitas ao mesmo tempo: em 9 de 10 rodadas as duas foram criadas.

## Arquitetura

**Números:** 7 pacotes, 13 dependências entre eles, nenhum ciclo. O Javalin aparece só em `com.pousada` e `com.pousada.api`, o JDBC só em `com.pousada.database`. As maiores classes são `ReservationHandler` (189 linhas), `ReservationService` (186) e `ReservationDao` (171).
**Nota:** com problemas

O desenho geral está certo. As dependências vão da borda (`api`) para as regras (`service`, `rateplan`) e destas para os dados (`database`), sem ciclo. As regras não conhecem o Javalin nem o JDBC: `ReservationService` só chama os DAOs, e o SQL fica todo em `database/`. Há três problemas:

- **Regra de negócio nos handlers.** O relatório financeiro e o de ocupação são montados inteiros em `ReportHandler`, que chama os DAOs direto (`ReportHandler.java:27,34,67`). Quem conta e quanto conta é decidido ali mesmo (`ReportHandler.java:37,74-106`), e a lista de quartos e o "/5" estão fixos no código (`ReportHandler.java:33,50`). A regra "só reserva paga tem segunda via" também está no handler e olha `amountPaid` em vez da situação da reserva (`ReservationHandler.java:134`). `MessageHandler.java:12` também lê o DAO direto.
- **Nenhuma montagem.** Tudo é `static`: os handlers, o serviço, os DAOs e o pool, guardado num campo estático mutável (`Database.java:11`). Não há um lugar que crie e ligue as peças; `Application.java:9` só inicializa um singleton global. Trocar ou isolar qualquer peça (num teste, por exemplo) é impossível sem banco de verdade.
- **O handler converte demais.** A conversão da requisição e da resposta é manual, com `Map` e casts (`ReservationHandler.java:22-42,148-184`).

## Design de baixo nível

**Números:** 5 de 5 planos na estrutura (`StandardRatePlan`, `NonRefundableRatePlan`, `CorporateRatePlan`, `WeekendPackageRatePlan`, `LongStayRatePlan`). Há um único `switch`, que só escolhe a implementação (`RatePlanFactory.java:7-13`), e nenhum `switch` ou `if` sobre o plano no serviço. Quatro cópias do laço de preço por noite.
**Classe:** acerto
**Exagero:** contenção
**Nota:** com problemas

**O pattern.** A interface `RatePlan` (`RatePlan.java:7-14`) junta as três coisas que variam por plano: quem pode usar (`validate`), o preço (`calculate`) e a devolução (`calculateRefund`). Cada um dos cinco planos, inclusive os dois antigos, tem a sua classe:

- o desconto do convênio por empresa fica em `CorporateRatePlan.java:11-15,21`;
- a regra sexta-domingo e o preço fechado do pacote ficam em `WeekendPackageRatePlan.java:20-28,35`, e a meia devolução em `WeekendPackageRatePlan.java:46`;
- o mínimo de 7 noites fica em `LongStayRatePlan.java:16`.

O serviço só pede ao plano, sem nomeá-lo: na criação (`ReservationService.java:50,56,70`) e no cancelamento (`ReservationService.java:125-126`). Um plano novo é uma classe nova e uma linha na fábrica. O que afasta um pouco do ideal:

- O "contexto" é um `Map<String, Object>` montado no serviço com chaves de texto (`ReservationService.java:51-53`) e lido com cast em cada plano (`CorporateRatePlan.java:20`, `WeekendPackageRatePlan.java:34`).
- A assinatura de `validate` carrega `roomCapacity` e `guests`, que nenhum plano usa.

**O exagero.** A fábrica é o ponto único de seleção que a referência aceita. `PriceCalculation` e `RatePlanException` têm uso real. Não há interface de uma implementação só nem camada que só repassa: contenção.

**O resto do código:**

- **Duplicação.**
  - O laço que soma as noites com a alta temporada está copiado em quatro planos: `StandardRatePlan.java:22-31`, `NonRefundableRatePlan.java:24-33`, `CorporateRatePlan.java:32-41` e `LongStayRatePlan.java:26-35`. Mudar a temporada ou o acréscimo exige quatro edições iguais.
  - Em `ReportHandler.java:81-88` e `97-104`, o mesmo bloco aparece duas vezes.
- **Erros.**
  - Todo handler termina em `catch (Exception e)`, que responde 400 `INVALID_REQUEST` (`ReservationHandler.java:54-55,66-67,87-88`; `ReportHandler.java:53-54,112-113`). Uma falha do banco vira "erro do cliente", e a mensagem crua do Jackson vaza para a resposta.
  - O `catch (IllegalArgumentException)` de `ReservationHandler.java:50` responde sempre "Invalid rate plan or date format", seja qual for a causa.
- **Tipos.** O tipo do quarto é `String` (`Room.java:7`), e os preços do pacote são indexados por texto (`WeekendPackageRatePlan.java:11-15`).
- **Formatação do dinheiro.**
  - O valor é formatado à mão, sem separador de milhar (`ReservationService.java:183-185`), e o comprovante sai "R$ 4059,30" em vez de "R$ 4.059,30".
  - O `String.format("%.2f")` depende do locale da JVM (`ReportHandler.java:86,109`).
  - `Money.format` e `Money.formatForJson` existem (`Money.java:15-22`) e não são usados.
- **Estado.** O estado global é estático (`Database.java:11`). Os campos públicos dos modelos são `final`, o que limita o estrago.

São defeitos localizados num código pequeno e legível, por isso `com problemas` e não `ruim`.

## Testes

**Números:** 9 testes rodaram, nenhum falhou nem foi pulado; 9 métodos de teste escritos, em 2 arquivos. Cobertura de linhas: 98 de 643 (15,2%). Cobertura de ramos: 23 de 127 (18,1%). Por pacote: `rateplan` 88/106 linhas, `util` 10/21, e 0 em `api`, `service` e `database`. Não há nenhum teste pela API nem contra o banco.
**Nota:** com problemas

O que existe confere valores de verdade:

- o preço com temporada do texto, R$ 599,76 (`RatePlanCalculationTest.java:59-61`, na numeração contínua de `cat -n` dos dois arquivos de teste);
- o não reembolsável, 89,96 / 509,80 (`:72-74`);
- os três exemplos do cenário: Tecnosul 779,80 / 93,58 / 686,22 (`:93-95`), pacote 820,00 (`:110-112`) e estadia longa 1.749,30 / 174,93 / 1.574,37 (`:129-131`);
- a devolução de metade do pacote, 410,00 (`:138`);
- o arredondamento meio-para-o-par (`MoneyTest.java:14-20`).

Mas os testes param no cálculo isolado:

- **Nenhuma recusa do cenário é testada.** Não há teste para convênio sem código ou com código inválido, pacote fora de sexta-domingo, estadia longa com menos de 7 noites. Apagar o `throw` de `CorporateRatePlan.java:22`, trocar `nights < 7` por `nights < 6` em `LongStayRatePlan.java:16` ou tirar a checagem do domingo em `WeekendPackageRatePlan.java:26` passa em todos os testes.
- **Só a Tecnosul é testada.** Trocar 0.18 da Agrovale por 0.81 (`CorporateRatePlan.java:13`) não derruba nada.
- **A devolução de três planos não é testada.** O convênio e a estadia longa não têm teste de devolução.
- **O resto do sistema não é testado:** as situações da reserva, os códigos de erro, o comprovante, o e-mail e os relatórios. Por isso os dois erros graves do comportamento passaram despercebidos: a confirmação que exige `amountPaid` e o cancelamento depois do check-in.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), todas com chave primária. Dinheiro em `numeric(10,2)` e datas em `date`. Conferências que acusaram algo:

- a chave estrangeira `reservations(room_number)` não tem índice;
- `status`, `rate_plan`, `type` e `channel` não têm `CHECK`;
- nenhuma restrição `UNIQUE`, `EXCLUDE` ou `CHECK` protege uma regra;
- 0 transações explícitas e 0 `FOR UPDATE`;
- um SQL montado com `+` (`RoomDao.java:37`).

**Nota:** ruim

**O que está certo.** Os tipos estão certos, há `NOT NULL` no que é obrigatório e a reserva aponta para o quarto por chave estrangeira (`Database.java:61`). Há um pool (Hikari), as conexões ficam em try-with-resources, e o SQL usa `?`. O `+` de `RoomDao.java:37` concatena um `int`, sem risco de injeção, mas foge do padrão do resto.

**O defeito sério: nada impede duas reservas na mesma noite.** A criação confere a disponibilidade com um `SELECT` por noite (`ReservationService.java:62-67`) e depois faz o `INSERT` (`ReservationService.java:73-77`). Não há transação, trava ou restrição no banco (nem um `EXCLUDE` sobre o período), nem mesmo um `synchronized`. O roteiro do comportamento mediu a consequência: em 9 de 10 rodadas de dois pedidos simultâneos, as duas reservas do mesmo quarto e das mesmas noites foram criadas. Para uma pousada, isso é overbooking real.

**Escritas que deviam andar juntas vão separadas.** A confirmação grava a situação e o valor pago em dois `UPDATE`s, cada um com a sua conexão (`ReservationService.java:97-98`), e o cancelamento faz o mesmo com a situação e a devolução (`ReservationService.java:128-129`). Uma falha no meio deixa uma reserva CONFIRMED com zero pago, ou CANCELLED sem devolução registrada.

**O esquema não sabe evoluir.** Ele é criado por `CREATE TABLE IF NOT EXISTS` no código (`Database.java:33-73`), sem migrações versionadas, então não consegue acrescentar uma coluna a uma tabela que já existe. Há ainda um `init-db.sql` solto na raiz, com uma cópia do esquema que pode divergir da do código.

**Carga.** O relatório de ocupação faz uma consulta por quarto (`ReportHandler.java:33-34`), e a criação faz uma consulta por noite (`ReservationService.java:62-63`). Com 5 quartos não pesa, mas é desnecessário.

## Comportamento

**Números:** 50 de 72 conferências passaram:

- parte comum: 30 de 46;
- dados depois de reiniciar: 1 de 3;
- cenário 01: 18 de 21;
- reservas simultâneas: 1 de 2 (9 de 10 rodadas criaram as duas).

**Nota:** ruim

**O que funciona.** A criação, os preços e as recusas: temporada, meio-para-o-par, 409/422/404/400. Todos os preços do cenário conferem, inclusive com valores: 686,22, 639,44, 584,85, o pacote 820,00 / 520,00 / 1.200,00 mesmo em julho, a estadia longa 1.574,37 e 1.754,30 com temporada. As recusas do convênio, do pacote e da estadia longa também conferem.

**O que quebra a parte comum:**

- **Não dá para confirmar uma reserva.** O site chama `POST /reservations/{id}/confirmation` como o texto descreve, e o sistema responde 400. Ele exige no corpo um `amountPaid` (`ReservationHandler.java:76-82`) que o texto não prevê; o texto diz que o pago é o total da reserva. Conferi à mão: sem corpo vem `400 "No content to map due to end-of-input"`, e com `{"amountPaid":1}` uma reserva de R$ 3.653,37 fica CONFIRMED com R$ 1,00 pago. Consequências:
  - o fluxo da reserva para no PENDING: sem e-mail, segunda via, check-in, check-out nem comprovante;
  - nenhuma devolução sai: STANDARD, CORPORATE, pacote (410,00) e LONG_STAY dão todos 0,00, o que explica as 3 falhas do cenário;
  - o relatório financeiro fica zerado.

  Para o dono da pousada, o sistema não registra pagamento nenhum.
- **Dá para cancelar depois do check-in** (`ReservationService.java:121` só recusa CHECKED_OUT e CANCELLED). O roteiro cancelou uma reserva que devia ir a check-out. Isso explica, junto com a confirmação, as 2 falhas de "depois de reiniciar": a reserva aparece CANCELLED, e o quarto, liberado.
- **Os relatórios contam errado:**
  - a ocupação conta também reservas PENDING (`ReportHandler.java:37`; `ReservationDao.java:106`), e o texto pede só as pagas;
  - o financeiro lista reservas PENDING e canceladas com devolução total, com 0,00 (`ReportHandler.java:74-88`), e o texto pede só as que deixaram dinheiro;
  - a linha de total sai `total,,,,0.00` (`ReportHandler.java:109`), e o roteiro espera `total,599.76`. O texto só dá o exemplo `total,2/5` da ocupação, então este último ponto é discutível.
- **Defeitos fora do roteiro, que pesam do mesmo jeito para o hóspede:**
  - o comprovante vem com `roomType` vazio (`ReservationHandler.java:176`);
  - os valores do comprovante e do e-mail saem sem separador de milhar ("R$ 4059,30", `ReservationService.java:183-185`).

As reservas simultâneas duplicadas são o defeito do banco descrito acima; aqui só confirmam que ele aparece pela API.

## Dúvidas

- **A confirmação com `amountPaid` no corpo.** O texto não descreve corpo nenhum para a confirmação e diz que "o que ele pagou é o total da reserva". Tratei a exigência como erro do agente, não como ponto aberto do texto, porque ela também deixa gravar um valor pago diferente do total. Se o texto fosse lido como "o site manda o valor", a parte comum ainda falharia no cancelamento depois do check-in e nos relatórios.
- **O formato da linha de total do financeiro** (`total,,,,` com as colunas vazias, contra `total,<soma>`). O texto não fecha esse formato.
- **O código da empresa sai do plano.** Ele é posto no contexto e gravado para qualquer plano (`ReservationService.java:52`; `ReservationDao.java:36`), e não só para o convênio. É pequeno e não muda a classe do pattern.

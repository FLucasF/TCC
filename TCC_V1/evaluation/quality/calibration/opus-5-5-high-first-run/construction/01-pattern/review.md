# Revisão: construção, cenário 01

## Visão geral

O agente montou um serviço Javalin em 7 pacotes por assunto: `hotel` (montagem e banco), `hotel.web` (rotas e
JSON), `hotel.reservations` (regras, planos de tarifa e o acesso à tabela de reservas), `hotel.rooms`,
`hotel.messages`, `hotel.documents` (o comprovante) e `hotel.reports` (os CSV). Uma requisição entra em
`web/HttpApi.java:64-67`, que lê o corpo e chama `ReservationService.create` (`reservations/ReservationService.java:29-50`).
O serviço valida, pede o preço ao `PriceCalculator` e grava pelo `ReservationStore`, em JDBC com Hikari.
O esquema sai de migrações Flyway (`V1__create_tables.sql`, `V2__reservation_company_code.sql`), e uma restrição
`EXCLUDE` impede reserva dupla no próprio banco. A variação pedida (os cinco planos de tarifa) virou um Strategy limpo:
uma interface `RatePlanPolicy` com uma classe por plano e um `EnumMap` que escolhe a implementação. Os 72 itens do
roteiro de comportamento passaram e os 30 testes do agente também. Nenhum defeito é sério. Os pontos fracos:
- um ciclo pequeno entre `hotel.documents` e `hotel.reservations`;
- os relatórios carregam a tabela inteira de reservas;
- a confirmação grava a reserva e o e-mail sem uma transação;
- os testes deixam sem conferência parte das regras novas: a devolução do convênio e da estadia longa, Agrovale e
  Farmavida, e o código do erro.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). O Javalin
aparece só em `hotel` e `hotel.web`. O JDBC aparece em `hotel.reservations`, `hotel.rooms` e `hotel.messages`, e só
nas classes de acesso a dados. A maior classe é `HttpApi`, com 170 linhas.
**Nota:** com problemas

- **A borda HTTP é fina.** Cada rota só lê o pedido, chama o serviço e escreve a resposta (`web/HttpApi.java:61-91`).
  Os códigos de erro viram status HTTP num único lugar (`web/HttpApi.java:93-107`). A conversão do corpo para o
  domínio fica em `web/HttpApi.java:109-130`.
- **As regras não conhecem o Javalin nem o SQL.** `ReservationService` recebe e devolve tipos do domínio
  (`reservations/ReservationService.java:20-27`), e o preço fica em `PriceCalculator` e nos planos.
- **O SQL está isolado.** Ele fica em `ReservationStore`, `Rooms` e `SentMessages`, sempre com parâmetros
  (`reservations/ReservationStore.java:30-62`, `rooms/Rooms.java:21-47`, `messages/SentMessages.java:20-35`).
- **A montagem fica num lugar só.** `Hotel.java:21-31` cria e liga todas as peças, e nenhuma classe faz `new` das
  suas dependências por dentro.
- **O ciclo é o defeito que tira o `bom`.**
  - `reservations/ReservationService.java:3,66,108,117` cria `ReservationReceipt`, que fica em `hotel.documents`.
  - `documents/ReservationReceipt.java:3,24` conhece `Reservation`, que fica em `hotel.reservations`.
  - O comprovante deveria ser montado fora do serviço, ou o serviço não deveria devolver um tipo de outro pacote que
    depende dele.
- **Texto do e-mail no serviço de regras.** O corpo do e-mail é escrito dentro do serviço
  (`reservations/ReservationService.java:124-130`). É uma mistura pequena de apresentação com regra.

## Design de baixo nível

**Números:**
- Os 5 planos estão na estrutura e nenhum ficou fora dela: `StandardPlan`, `NonRefundablePlan`, `CorporatePlan`,
  `WeekendPackagePlan` e `LongStayPlan`.
- Não há nenhum `switch` nem `if` sobre o plano fora das estratégias. O único `switch` de `src/main` é o de
  `ErrorCode` → status HTTP.
- Há 2 pontos de uso: o preço e a validação em `PriceCalculator.java:25-29`, e a devolução em
  `ReservationService.java:84`.
**Classe:** acerto
**Exagero:** contenção
**Nota:** bom

- **O pattern.**
  - `reservations/RatePlanPolicy.java:7-15` define as três coisas que variam por plano: `validate`, `price` e
    `refund`.
  - Cada plano tem a sua classe, com a sua regra de uso, o seu preço e a sua devolução:
    - `CorporatePlan.java:10-31`: as empresas e os descontos num mapa, e o código validado no próprio plano;
    - `WeekendPackagePlan.java:12-37`: sexta a domingo, o preço fechado por tipo de quarto e a metade devolvida;
    - `LongStayPlan.java:10-28`: o mínimo de 7 noites e 10% de desconto.
  - Os dois planos que já vinham no enunciado também são estratégias (`StandardPlan.java`, `NonRefundablePlan.java`),
    e não um `switch` que sobrou.
  - A escolha é um `EnumMap` montado num lugar só (`RatePlans.java:9-21`). O cálculo
    (`PriceCalculator.java:25-29`) e o cancelamento (`ReservationService.java:84`) pedem ao plano sem nomeá-lo.
  - O próximo plano (melhor idade, 60 dias de antecedência) é uma constante no `enum`, uma classe e uma linha em
    `RatePlans`.
  - A temporada continua comum a todos em `PriceCalculator.nightlyCharges/nightPrice` (`PriceCalculator.java:31-56`).
    Os planos recebem as noites já calculadas (`NightlyCharges`), e por isso o pacote pode ignorá-las e a estadia
    longa mantém a temporada.
- **O exagero.** Não há interface com uma implementação só, nem fábrica, nem builder. `PercentOff`
  (`PercentOff.java:10-13`) tira uma duplicação real entre quatro planos. `NightlyCharges` e `Price` são registros
  pequenos com papéis diferentes.
- **O resto do código.**
  - O dinheiro é `BigDecimal` com `HALF_EVEN` em cada passo (`PriceCalculator.java:58-60`). As datas são
    `LocalDate`, e as situações e os planos são `enum`.
  - As mudanças de estado da `Reservation` são métodos com pacote restrito (`Reservation.java:102-114`).
  - Os erros têm um tipo só, com código (`HotelException`, `ErrorCode`), e nenhuma exceção é engolida.
  - Detalhes que não impedem aceitar:
    - o `catch (Exception)` largo na leitura do corpo (`web/HttpApi.java:113`) troca qualquer falha por
      `INVALID_REQUEST`;
    - a mensagem de quarto ocupado está duplicada (`ReservationService.java:44-47` e `ReservationStore.java:56-58`),
      mas são duas defesas legítimas, uma no serviço e outra na restrição do banco;
    - a `Reservation` é reconstruída a partir de uma `NewReservation` (`ReservationStore.java:126-133`) e descarta o
      `companyCode` que o banco guarda (`Reservation.java:31-41`).

## Testes

**Números:** 30 testes rodaram, com 0 falhas, 0 erros e 0 pulados. Há 30 métodos escritos em 5 classes. A
cobertura é de 94,0% das linhas (521/554) e 87,1% dos ramos (108/124). No pacote `hotel.reservations`, 258/273
linhas e 56/60 ramos. Todos os testes rodam contra o PostgreSQL real, 1 classe pela API HTTP, e não há mocks.
**Nota:** com problemas

- **A parte comum está bem conferida, com valores.**
  - Temporada nas bordas, com 14/12 e 15/12, 29/02 em ano bissexto e 30/06, 01/07, 31/07 e 01/08
    (`PriceCalculatorTest.java:36-59`).
  - Arredondamento meio para o par, em que 37,485 vira 37,48 (`PriceCalculatorTest.java:62-69`).
  - Devoluções e o valor retido (`ReservationServiceTest.java:110-123`).
  - Códigos de erro do serviço (`ReservationServiceTest.java:54-63`).
  - Noite de saída livre (`ReservationServiceTest.java:66-74`).
  - E-mail com assunto e valores (`ReservationServiceTest.java:85-98`).
  - CSVs comparados por inteiro (`ReportsTest.java:44-101`).
- **A parte nova** tem 4 testes (`NewRatePlansTest.java`). Eles conferem os três exemplos do texto: 93,58 e 686,22
  (`:38-39`), 820,00 e 410,00 (`:51-54`), 174,93 e 1.574,37 (`:62-63`). Também conferem a borda de 7 noites, com 7
  aceitas e 6 recusadas (`:60,64`).
- **Fica sem rede.** A pergunta da mutação mostra estas regras novas que nenhum teste pegaria se mudassem:
  - trocar a devolução de `CorporatePlan.refund` ou de `LongStayPlan.refund` por zero (`CorporatePlan.java:29-31`,
    `LongStayPlan.java:26-28`);
  - trocar 0.18 de Agrovale por 0.81, ou 0.25 de Farmavida por outro valor (`CorporatePlan.java:12-13`);
  - mudar o preço do pacote no standard (520,00) ou na suíte (1.200,00) (`WeekendPackagePlan.java:13,15`);
  - fazer a estadia longa ignorar a temporada.
- **Asserções fracas nas recusas dos planos.** Elas só conferem o tipo `HotelException`, sem o código
  `INVALID_REQUEST` que o texto pede (`NewRatePlansTest.java:44-45,55,64`). Um plano que recusasse com outro código
  passaria.
- **Pela API, o teste olha o dinheiro como double.** `HttpApiTest.java:229,240` lê o valor como `double` e não
  confere as 2 casas do JSON.

## Banco

**Números:** 3 tabelas (`rooms`, `reservations`, `messages`), criadas por 2 migrações Flyway. As conferências dão:
- nenhuma tabela sem chave primária;
- o dinheiro todo em `numeric(10,2)` (6 colunas);
- nenhuma chave estrangeira sem índice;
- nenhuma data guardada em texto;
- `status`, `rate_plan`, `type` e `channel` em `text` sem CHECK;
- 5 restrições de regra, incluindo o `EXCLUDE USING gist` contra reserva dupla;
- 0 transações explícitas;
- 4 candidatos a SQL com `+`, todos com constantes.
**Nota:** com problemas

- **O esquema está bem feito.**
  - A chave estrangeira da reserva para o quarto está em `V1__create_tables.sql:21`.
  - Há CHECKs nas datas e nos hóspedes (`V1__create_tables.sql:27,36`).
  - A regra "um quarto, uma reserva por noite" é protegida no próprio banco, mesmo com dois processos
    (`V1__create_tables.sql:38-40`). O código traduz a violação em `ROOM_UNAVAILABLE`
    (`ReservationStore.java:21,56-58`).
  - O esquema evolui por migrações versionadas, e as conexões vêm de um pool e voltam por try-with-resources.
  - Os candidatos a SQL com `+` só concatenam a constante `COLUMNS` (`ReservationStore.java:18-19,99,112`).
- **Os defeitos.**
  - Os dois relatórios trazem a tabela inteira de reservas para filtrar em Java (`reports/OccupancyReport.java:25`,
    `reports/RevenueReport.java:25`, via `ReservationStore.all()` em `ReservationStore.java:109-122`). Essa
    consulta cresce sem limite.
  - A confirmação faz duas escritas que deveriam andar juntas, em conexões separadas e sem transação: o `UPDATE` da
    reserva (`ReservationService.java:65`) e o `INSERT` da mensagem (`ReservationService.java:67-70`). Se a segunda
    falhar, a reserva fica confirmada sem o e-mail registrado.
  - As colunas de situação e de plano aceitam qualquer texto, porque não têm CHECK.
  - O `company_code` é gravado também para planos que não são o convênio (`ReservationStore.java:50`). Não faz mal ao
    preço, mas guarda um dado que o texto manda ignorar.

## Comportamento

**Números:** 72 de 72 conferências passaram:
- parte comum: 46/46;
- dados depois de reiniciar: 3/3;
- cenário 01 (planos novos): 21/21;
- duas reservas ao mesmo tempo: 2/2 (10 rodadas, sempre uma criada e a outra recusada com 409).
**Nota:** bom

Não houve nenhuma falha. Para o dono da pousada, isto quer dizer que:
- o preço com temporada e arredondamento está certo, e as recusas devolvem os códigos combinados;
- o e-mail e o comprovante saem, e os relatórios conferem;
- os três planos novos dão exatamente as contas que o financeiro passou (R$ 686,22, R$ 820,00 e R$ 410,00,
  R$ 1.574,37), e também Agrovale, Farmavida, o pacote nos outros quartos e em julho, e a estadia longa com noites
  de temporada;
- as recusas dos planos voltam com 400 `INVALID_REQUEST`;
- os dados ficam no banco depois de reiniciar, e duas reservas simultâneas nunca ocupam o mesmo quarto.

## Dúvidas

- **A ordem das validações.** A disponibilidade do quarto é conferida antes da regra do plano
  (`ReservationService.java:44-48`). Um pacote de fim de semana em data errada, num quarto já ocupado, recebe 409
  `ROOM_UNAVAILABLE` em vez de 400. O texto não fixa a ordem, e o roteiro não testa esse caso.
- **O `EmailSender`.** `messages/EmailSender.java:12-14` só repassa para `SentMessages.add` com o canal "EMAIL".
  Considerei que ele é o ponto de troca natural para quando o e-mail sair de verdade, como o texto antecipa, e não
  contei como exagero.
- **O `company_code` gravado em todos os planos** (`ReservationStore.java:50`). É uma regra de plano pequena fora do
  plano, mas não é de preço nem de devolução, e por isso não mudou a classe do pattern.

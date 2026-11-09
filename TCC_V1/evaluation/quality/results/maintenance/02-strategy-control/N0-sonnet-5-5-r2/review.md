# Revisão: manutenção, cenário 02

## Visão geral

A base é um serviço Javalin em sete pacotes: `hotel` (montagem e banco), `hotel.web` (as rotas), `hotel.reservations` (as regras e o acesso às reservas), `hotel.rooms`, `hotel.messages`, `hotel.documents` e `hotel.reports`. Uma requisição entra em `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`), vai para `ReservationService`, que usa `PriceCalculator` para o preço e `ReservationStore` (JDBC com `?`) para gravar no PostgreSQL. O esquema é criado por uma migração do Flyway. O pedido trocava o acréscimo da alta temporada de 20% para 25% e acrescentava a semana da Páscoa de 2027 e de 2028. O agente fez uma mudança pequena e no lugar certo, só em `PriceCalculator`: a constante passou para `0.25` (`src/main/java/hotel/reservations/PriceCalculator.java:17`), e as duas semanas entraram como uma lista de períodos, consultada em `isHighSeason` (`:21-30`, `:57`). Ele também atualizou os testes que dependiam dos 20%, escreveu um teste novo para a Páscoa e corrigiu o README. Não há nada sério. Os poucos defeitos que existem já estavam na base.

## Arquitetura

**Números:** 7 pacotes, 17 dependências entre eles, 1 ciclo (`hotel.documents` ↔ `hotel.reservations`). É o mesmo resultado da base: a única diferença em `measures/dependencies-base.md` é uma classe a mais em `hotel.reservations` (o record privado `Period`, 14 linhas).
**Nota:** bom

A mudança fica dentro de `hotel.reservations/PriceCalculator`, uma classe sem Javalin e sem JDBC, e não cria nenhuma dependência nova entre pacotes. O ciclo já existia na base: `ReservationService` importa `hotel.documents.ReservationReceipt` (`src/main/java/hotel/reservations/ReservationService.java:3`), e `ReservationReceipt` importa `hotel.reservations.Reservation` (`src/main/java/hotel/documents/ReservationReceipt.java:3`). O resto também vem da base e está bem resolvido:
- a borda HTTP é fina (`src/main/java/hotel/web/HttpApi.java:61-91`);
- o SQL fica isolado nos *stores*;
- a montagem acontece num lugar só (`src/main/java/hotel/Hotel.java:21-27`).

## Design de baixo nível

**Números:** 2 constantes mudadas ou acrescentadas e 1 condição a mais em `isHighSeason`. Nenhuma interface, classe por temporada ou acréscimo parametrizado. A estrutura nova é um record privado de duas datas, `Period`, guardado numa lista imutável de dois elementos.
**Classe:** contenção
**Exagero:** contenção
**Nota:** bom

O texto quer tentar o agente com "um acréscimo diferente para cada temporada", e ele não caiu. O acréscimo continua sendo uma constante única (`PriceCalculator.java:17`), aplicada no mesmo `nightPrice` de antes (`:46-52`). As datas da Páscoa entraram como dado: `HOLY_WEEKS`, uma `List.of` com dois `Period` (`:28-30`), consultada numa linha a mais de `isHighSeason` (`:57`). Os outros períodos ficaram como na base (`:58-61`). O `Period` é um record privado e aninhado, com um único método `contains` (`:21-25`), e equivale à "lista simples de períodos" que os critérios do controle aceitam. Não é uma hierarquia nem um ponto de extensão.

O comentário em `:27` avisa que os anos seguintes precisam ser incluídos, e o README diz o mesmo. Isso respeita o pedido ("pode deixar só esses dois anos"). O agente não inventou um cálculo da Páscoa nem uma configuração externa.

O resto do código é da base e está limpo:
- `BigDecimal` com HALF_EVEN para o dinheiro (`PriceCalculator.java:64-66`);
- `LocalDate` para as datas;
- `enum` para os planos e as situações;
- `switch` exaustivos (`PriceCalculator.java:39-42`, `ReservationService.java:82-85`).

O único ponto discutível também vem da base: o `catch (Exception)` ao ler o corpo da requisição (`src/main/java/hotel/web/HttpApi.java:113`). Ele está justificado no comentário e converte a falha num erro 400.

## Testes

**Números:** 27 testes rodaram, 0 falhas, 0 erros, 0 pulados (na base, 26 de 26). Há 27 métodos de teste no código. A cobertura é de 93,9% das linhas (476 de 507) e 87,7% dos ramos (107 de 122). Na base, era 93,8% e 87,1%. Em `hotel.reservations`, os ramos cobertos passaram de 49 de 52 para 55 de 58.
**Nota:** bom

Para o acréscimo, o agente recalculou os valores esperados de todos os testes que dependiam dos 20% e não apagou nem afrouxou nenhuma asserção:
- `src/test/java/hotel/reservations/PriceCalculatorTest.java:35-36` e `:44-45` (562,28, com a noite de 312,38 que o dono dá como exemplo);
- `:71-74` (a suíte a 724,875, que vai para 724,88 pelo meio para o par, e o desconto de 434,93 sobre o valor novo);
- `src/test/java/hotel/web/HttpApiTest.java:77` e `:118` (974,76 na API e no relatório financeiro).

Os cálculos conferem. Para a Páscoa, o teste novo confere as quatro bordas de cada ano: a véspera, o domingo de Ramos, o domingo de Páscoa e o dia seguinte (`PriceCalculatorTest.java:78-87`).

A pergunta da mutação:
- se a constante voltasse a `0.20`, cinco asserções falhariam;
- se uma das datas mudasse um dia, ou se `isBefore` virasse `isAfter` em `Period.contains`, as bordas falhariam;
- se a linha de `HOLY_WEEKS` em `isHighSeason` sumisse, `:80-81` e `:84-85` falhariam.

Ficou faltando apenas um teste do preço de uma noite da Páscoa pelo `calculate`. O teste novo confere só `isHighSeason`, mas o caminho do preço já é coberto pelos outros testes.

## Banco

**Números:** 3 tabelas, todas com chave primária. O dinheiro está todo em `numeric(10,2)`, as datas em `date`, e a chave estrangeira tem índice. Há um EXCLUDE contra reservas sobrepostas. A migração da base (`V1__create_tables.sql`) está igual, e nenhuma foi acrescentada. O script acusou só as colunas de situação e de tipo em `text` sem CHECK, e isso vem da base.
**Nota:** bom

O pedido não mexia no banco, e o agente também não. Como o preço é calculado e gravado na criação (`src/main/java/hotel/reservations/ReservationStore.java:44-47`), as reservas que já existem guardam o valor antigo, e a mudança de preço não corrompe nada. Os poucos defeitos vêm todos da base:
- `status` e `rate_plan` sem CHECK (`src/main/resources/db/migration/V1__create_tables.sql:28`, `:33`);
- os dois relatórios trazem a tabela inteira para filtrar em Java (`src/main/java/hotel/reports/RevenueReport.java:25`, `OccupancyReport.java:25`).

O SQL usa parâmetros. Os `+` que o script lista só juntam constantes (`ReservationStore.java:33`, `:83`, `:98`, `:111`).

## Comportamento

**Números:** 72 de 72 conferências passaram:
- a parte comum: 46 de 46;
- os dados depois de reiniciar: 3 de 3;
- o acréscimo novo: 5 de 5;
- a semana da Páscoa: 10 de 10;
- os outros períodos: 6 de 6;
- as reservas simultâneas: 2 de 2.

**Nota:** bom

Para o dono da pousada:
- a noite de temporada sai pelos valores novos: R$ 312,38 no standard, R$ 487,38 no deluxe e R$ 724,88 na suíte;
- no plano não reembolsável, o desconto é calculado sobre o valor novo;
- as noites de 21/03 a 28/03/2027 e de 09/04 a 16/04/2028 custam o preço de temporada, com os dois domingos incluídos, e as noites vizinhas continuam com o preço normal;
- dezembro a partir do dia 15, janeiro, fevereiro e julho continuam como antes;
- o resto do sistema não mudou (reservas, recusas, e-mail, check-in e check-out, cancelamentos e relatórios), e nenhum quarto aceitou duas reservas ao mesmo tempo.

## Dúvidas

Nenhuma.

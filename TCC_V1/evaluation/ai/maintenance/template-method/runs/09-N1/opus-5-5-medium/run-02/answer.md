# Cenário 09

## Visão geral

O agente aplicou o Template Method nos quatro relatórios. Criou uma classe abstrata `CsvReport<P>` com um método `generate` `final`, que é o esqueleto: título, cabeçalho, linhas e total. Os quatro relatórios passaram a ser subclasses dela, os dois que já existiam (`OccupancyReport`, `RevenueReport`) e os dois novos (`TourismTaxReport`, `RatePlanReport`). Nenhum relatório escreve a linha de título por conta própria, e a futura linha com a data em que o relatório foi tirado entraria num lugar só. O ponto mais discutível é pequeno: o passo variável é grosso. Um único `table(period)` devolve cabeçalho, linhas e total juntos, e a escolha e a ordenação das reservas ficam dentro dele. Isso não afasta o desenho do pattern. Pela leitura do código, o comportamento confere com o pedido.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin organizado em pacotes:
- `hotel.reservations`: o domínio, com `Reservation`, `ReservationService`, `ReservationStore`, `PriceCalculator` e `RatePlan`;
- `hotel.rooms`: os quartos;
- `hotel.messages`: os e-mails enviados, guardados em memória;
- `hotel.documents`: o recibo;
- `hotel.reports`: os relatórios CSV;
- `hotel.web.HttpApi`: as rotas.

`Hotel` monta tudo à mão e sobe o servidor (`final/src/main/java/hotel/Hotel.java:29-39`). Os dados ficam em memória, no `ReservationStore`. Uma requisição de relatório chega a uma rota registrada em `HttpApi`. A rota lê o parâmetro (`readDate` ou `readMonth`), chama `report.generate(periodo)` e devolve `text/csv` (`final/src/main/java/hotel/web/HttpApi.java:83-90`). O agente trocou as duas rotas fixas por laços sobre duas listas, uma de relatórios por noite e outra de relatórios por mês. A URL vem de `report.name()`. Nos testes, o agente atualizou `ReportsTest` com a linha de título nos casos antigos e acrescentou um teste para a taxa de turismo e outro para os planos, com mês sem reserva e mês com uma não reembolsável cancelada. O `HttpApiTest` não mudou.

**A parte do pattern.**
- `CsvReport<P>` (`final/src/main/java/hotel/reports/CsvReport.java:10`) declara dois passos abstratos: `name()` (`:17`) e `table(P period)` (`:19`). Este último devolve um `record Table(header, rows, total)` (`:13`).
- O método-modelo `generate` é `final` (`:21-31`). Ele escreve `HOTEL_NAME,name(),period` (`:24`), depois o cabeçalho, uma linha por reserva e o total.
- Cada relatório é uma subclasse que só preenche o que é seu:
  - `OccupancyReport extends CsvReport<LocalDate>` (`final/src/main/java/hotel/reports/OccupancyReport.java:12`, `table` em `:28-45`);
  - `RevenueReport extends CsvReport<YearMonth>` (`final/src/main/java/hotel/reports/RevenueReport.java:13`, `:27-40`);
  - `TourismTaxReport` (`final/src/main/java/hotel/reports/TourismTaxReport.java:10`, `:26-40`), com a taxa recebida no construtor;
  - `RatePlanReport` (`final/src/main/java/hotel/reports/RatePlanReport.java:12`, `:26-38`).
- A seleção mensal, com o filtro e a ordem por check-in e depois id, foi tirada para `MonthlyReservations` (`final/src/main/java/hotel/reports/MonthlyReservations.java:16-27`). Ela serve a três relatórios, e `withRevenueStartingIn` (`:25`) é a mesma regra do financeiro, reaproveitada pelos planos.
- Quem usa a estrutura não pergunta qual relatório é: `HttpApi` só conhece `CsvReport` (`final/src/main/java/hotel/web/HttpApi.java:30-31`, `:83-90`).

**O raciocínio.**
- O que aproxima do pattern: há classe abstrata, método-modelo `final` e passos abstratos. Os quatro relatórios são subclasses, os antigos inclusive, e nenhum repete a sequência dos passos. Isso é exatamente o que as instruções chamam de acerto.
- O que afasta um pouco: a granularidade dos passos. Selecionar, ordenar, formatar as linhas e calcular o total ficam juntos em `table`, em vez de serem passos separados. Mas a sequência título, cabeçalho, linhas, total, que é o que vai mudar ("terminar com uma linha com a data"), mora só no esqueleto.
- No exagero: `Table` faz parte do mecanismo do pattern. `MonthlyReservations` elimina uma duplicação real, usada por três relatórios. As duas listas no `HttpApi` são o mínimo para tratar os dois tipos de período. Não há interface de implementação única, fábrica nem camada que só repassa.
- No comportamento: a leitura confere as regras, detalhadas abaixo.

## Item a item

**O pattern pedido.** O agente criou uma classe abstrata `CsvReport<P>` (`final/src/main/java/hotel/reports/CsvReport.java:10`). O método-modelo `final generate` (`:21-31`) escreve título, cabeçalho, linhas e total, e os passos abstratos são `name()` (`:17`) e `table()` (`:19`). Os quatro relatórios estendem essa classe:
- `final/src/main/java/hotel/reports/OccupancyReport.java:12`
- `final/src/main/java/hotel/reports/RevenueReport.java:13`
- `final/src/main/java/hotel/reports/TourismTaxReport.java:10`
- `final/src/main/java/hotel/reports/RatePlanReport.java:12`

A linha de título aparece num só lugar (`CsvReport.java:24`). As rotas usam só o tipo abstrato (`final/src/main/java/hotel/web/HttpApi.java:83-90`). É o Template Method que o texto pedia, aplicado aos quatro relatórios.
Conclusão: acerto

**O exagero colateral.** Não há. `MonthlyReservations` (`final/src/main/java/hotel/reports/MonthlyReservations.java:11`) é um utilitário com três usuários e remove uma duplicação real. O `record Table` (`CsvReport.java:13`) é o veículo dos passos do pattern. A taxa fica como constante na montagem (`final/src/main/java/hotel/Hotel.java:21`) e é passada pelo construtor, sem nenhuma estrutura a mais.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código: não subi o serviço, não rodei o Maven e não chamei a API.
- **Título:** `Pousada Maré Alta,<nome>,<período>` (`CsvReport.java:24`), com os nomes `occupancy`, `revenue`, `tourism-tax` e `rate-plans` (`OccupancyReport.java:23`, `RevenueReport.java:22`, `TourismTaxReport.java:21`, `RatePlanReport.java:21`). O período é o `LocalDate` ou o `YearMonth` da chamada, impresso como `2027-07-10` ou `2027-07`.
- **Ocupação depois do título:** o filtro (pago e cobrindo a noite) e a ordem por quarto são os da base. Cabeçalho, linhas e `total,n/5` também são iguais (`OccupancyReport.java:29-44`).
- **Financeiro depois do título:** seleciona `amountKept > 0` no mês, com a mesma ordem, o mesmo cabeçalho, as mesmas linhas e o total com escala 2 (`MonthlyReservations.java:20,26`, `RevenueReport.java:28-40`).
- **Taxa de turismo:**
  - reservas `isPaid()` (CONFIRMED, CHECKED_IN, CHECKED_OUT, em `final/src/main/java/hotel/reservations/Reservation.java:80-83`) que começam no mês;
  - a mesma ordem do financeiro;
  - as colunas pedidas (`TourismTaxReport.java:40`);
  - a taxa de 4,50 × hóspedes × noites, com escala 2 (`TourismTaxReport.java:43-47`), o que dá 27.00 para 2 hóspedes e 3 noites;
  - a linha `total,valor` no fim.
- **Planos:**
  - uma linha por `RatePlan.values()`, na ordem STANDARD e depois NON_REFUNDABLE (`RatePlanReport.java:31`), mesmo sem reserva, com `0,0.00`;
  - as mesmas reservas do financeiro, o que inclui a não reembolsável cancelada (`RatePlanReport.java:27`);
  - o dinheiro conta `amountKept`, isto é, o que ficou com a pousada;
  - o total sai como `total,quantidade,valor` (`:38`).
- **Erros:** os 400 por parâmetro inválido continuam com o `readDate` e o `readMonth` de antes.

Tudo o que conferi segue o pedido.
Conclusão: segue

## Dúvidas

O passo abstrato `table` é grosso: um passo só devolve cabeçalho, linhas e total, e a seleção e a ordenação ficam dentro de cada subclasse, em vez de serem passos separados do método-modelo. Contei isso como acerto, porque a sequência que deve mudar "em todos de uma vez" mora inteira no esqueleto `final`. Um avaliador mais estrito poderia pedir passos mais finos, mas a evidência não sustenta chamar isso de malfeito.

# Cenário 09

## Visão geral

O agente aplicou o Template Method. Criou a classe abstrata `Report<P, R>`, que tem um método-modelo `final generate` com o esqueleto inteiro: título, cabeçalho, uma linha por item e total. Os quatro relatórios passaram a ser subclasses que só preenchem os passos que mudam, e isso vale também para a ocupação e o financeiro, que já existiam. Nenhum relatório escreve mais o CSV inteiro. A linha de título fica num lugar só, e a futura linha de data também vai ficar. As regras novas conferem na leitura. Não achei problema sério. O ponto menor: o passo "escolher e ordenar" ficou junto num único método abstrato, `rows`, em vez de dois. Isso é aceitável, porque a ordenação muda de um relatório para outro.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com estes pacotes:
- `hotel.rooms`: os quartos.
- `hotel.reservations`: reserva, preço, plano, status, serviço e o store em memória.
- `hotel.messages`: os e-mails guardados.
- `hotel.documents`: o recibo.
- `hotel.reports`: os relatórios.
- `hotel.web`: a classe `HttpApi`, com as rotas.

A classe `Hotel` monta tudo à mão e sobe o servidor. Ela também guarda a taxa da prefeitura numa constante (`final/src/main/java/hotel/Hotel.java:21`) e a passa ao relatório de taxa (`Hotel.java:35-38`). Os dados ficam no `ReservationStore`, em memória.

O caminho de uma requisição de relatório é este: a rota em `final/src/main/java/hotel/web/HttpApi.java:91-94` lê `date` ou `month` e chama `generate` do relatório certo. O helper `csv` (`HttpApi.java:97`) põe o content-type e devolve o texto. O `generate` lê do store, aplica a regra e monta o CSV.

Testes do agente:
- `ReportsTest` ganhou a linha de título nos testes que já existiam.
- Dois testes novos: a taxa de 2 hóspedes × 3 noites dando 27.00 (`final/src/test/java/hotel/reports/ReportsTest.java:120`) e os planos com a linha de `NON_REFUNDABLE` zerada (`ReportsTest.java:135`).
- `HttpApiTest` confere o total das duas rotas novas.

**A parte do pattern.** Em `final/src/main/java/hotel/reports/Report.java:16-26` está o `public final String generate(P period)`. Ele chama `rows(period)`, escreve `INN_NAME,name(),period`, depois `header()`, um `line(row)` para cada item e por fim `total(rows)`. Os passos abstratos são `name`, `header`, `rows`, `line` e `total` (`Report.java:29-39`). O tipo genérico `P` é o período (um `LocalDate` na ocupação, um `YearMonth` nos outros) e `R` é o item de cada linha. Cada relatório é uma subclasse:

- **Ocupação** (`final/src/main/java/hotel/reports/OccupancyReport.java:12`): escolhe as reservas pagas que cobrem a noite, ordenadas pelo quarto (`:34-38`). A linha e o total `x/5` são os mesmos da base (`:45`, `:51`).
- **Financeiro** (`final/src/main/java/hotel/reports/RevenueReport.java:13`): a seleção vem de `MonthlyReservations.withRevenue` (`:33`), e a linha e a soma do total são as mesmas da base (`:38`, `:45-49`).
- **Taxa de turismo** (`final/src/main/java/hotel/reports/TourismTaxReport.java:10`): seleciona com `MonthlyReservations.startingIn(..., Reservation::isPaid)` (`:32`). A taxa por reserva é valor × hóspedes × noites (`:53-55`), e o total soma essas taxas (`:45-49`).
- **Planos** (`final/src/main/java/hotel/reports/RatePlanReport.java:12`): o item de cada linha é um `PlanSummary`, um por valor de `RatePlan.values()` (`:37`), calculado sobre as mesmas reservas do financeiro (`:36`).

O helper `MonthlyReservations` (`final/src/main/java/hotel/reports/MonthlyReservations.java:16-27`) guarda o filtro do mês e a ordem do financeiro, para que os três relatórios mensais não os repitam.

**O raciocínio.** As instruções dão como acerto uma classe abstrata com o método-modelo, de preferência `final`, e os quatro relatórios como subclasses, os dois antigos inclusive. É exatamente o que está no código. Nenhum relatório monta a sequência por conta própria. Quem chama não pergunta qual é o caso concreto: cada rota só chama `generate`. A base (`base/src/main/java/hotel/reports/OccupancyReport.java:23-45`, `base/.../RevenueReport.java:69-94`) foi reescrita, e não sobrou nenhum `generate` próprio.

Dois desvios pequenos do ideal:
- Escolher e ordenar ficaram num único passo, `rows`.
- O `HttpApi` recebe os quatro tipos concretos. Mas cada um é usado na sua própria rota, então isso não é perguntar o tipo.

Nada disso aproxima o desenho de `malfeito`.

No exagero, olhei o `MonthlyReservations` e o record `PlanSummary`. Os dois têm função real: um evita repetir o filtro e a ordem, o outro é o item da linha do relatório de planos. Não há interface sem substituto, fábrica nem camada que só repassa.

No comportamento, a conferência foi por leitura e as regras conferem (detalhes no item abaixo).

## Item a item

**O pattern pedido.** O esqueleto está em `final/src/main/java/hotel/reports/Report.java:16-26`, um `generate` `final` que escreve título, cabeçalho, linhas e total. Os passos abstratos estão em `Report.java:29-39`. As quatro subclasses são `OccupancyReport.java:12`, `RevenueReport.java:13`, `TourismTaxReport.java:10` e `RatePlanReport.java:12`, cada uma só com o nome, o cabeçalho, a seleção, a linha e o total. As rotas só chamam `generate` (`final/src/main/java/hotel/web/HttpApi.java:91-94`). A sequência dos passos não se repete em nenhum relatório, e a linha de título só existe em `Report.java:19`. É o Template Method que o pedido precisava ("quando o jeito dos relatórios mudar, quero que mude em todos de uma vez").
Conclusão: acerto

**O exagero colateral.** Não há. O `MonthlyReservations` (`final/src/main/java/hotel/reports/MonthlyReservations.java:11-27`) é um helper estático que dá o filtro do mês e a ordem a três relatórios. O `PlanSummary` (`RatePlanReport.java:15`) é o item da linha. Os dois servem para alguma coisa. O helper `csv` do `HttpApi.java:97` só junta duas chamadas que se repetiam.
Conclusão: contenção

**O comportamento.** Conferi por leitura do código, como a correção pede: não subi o serviço, não rodei o Maven e não chamei a API.

- **Título:** `Pousada Maré Alta,<nome>,<período>` (`Report.java:14`, `:19`). O período sai do `toString` de `LocalDate` ou `YearMonth` (2027-07-10 ou 2027-07), e os nomes são `occupancy`, `revenue`, `tourism-tax` e `rate-plans` (`OccupancyReport.java:24`, `RevenueReport.java:23`, `TourismTaxReport.java:22`, `RatePlanReport.java:26`).
- **Taxa de turismo:**
  - Seleção: reservas pagas que começam no mês, `isPaid` = CONFIRMED/CHECKED_IN/CHECKED_OUT (`final/src/main/java/hotel/reservations/Reservation.java:80-83`), na ordem do financeiro, check-in e depois id (`MonthlyReservations.java:20`).
  - Colunas: `reservation,room,check_in,check_out,guests,nights,tax` (`TourismTaxReport.java:27`).
  - Taxa: 4.50 × hóspedes × noites com duas casas (`TourismTaxReport.java:53-55`, valor em `Hotel.java:21`). Por exemplo, 2 × 3 dá 27.00.
  - Total: `total,<valor>`.
- **Planos:**
  - Uma linha por valor do enum, na ordem STANDARD e depois NON_REFUNDABLE (`final/src/main/java/hotel/reservations/RatePlan.java`), mesmo com zero, quando sai `0,0.00` (`RatePlanReport.java:37-49`).
  - Usa as mesmas reservas do financeiro, `amountKept > 0`, o que inclui a não reembolsável cancelada (`MonthlyReservations.java:25-26`).
  - Total: `total,<qtd>,<valor>` (`RatePlanReport.java:65`).
- **Ocupação e financeiro depois do título:** cabeçalho, filtro, ordem, linhas e total iguais aos da base. Compare `OccupancyReport.java:29-51` com `base/.../OccupancyReport.java:24-43`, e `RevenueReport.java:28-49` com `base/.../RevenueReport.java:70-92`. As rotas antigas continuam com as mesmas validações (`readDate`, `readMonth`).

Tudo o que conferi segue o pedido.
Conclusão: segue

## Dúvidas

Nenhuma que mude a classe. Só um registro: escolher e ordenar ficaram juntos em `rows` (`Report.java:35`), em vez de dois passos separados. Como a ordem muda de relatório para relatório (por quarto na ocupação, por check-in e id nos mensais), considerei isso uma granularidade aceitável do esqueleto, não um passo repetido.

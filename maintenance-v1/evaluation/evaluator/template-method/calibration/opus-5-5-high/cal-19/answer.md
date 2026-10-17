# Cenário 09

## Visão geral

O agente aplicou o Template Method nos quatro relatórios. Criou a classe abstrata `CsvReport<P>`, cujo método-modelo `generate` é `final` e monta o título, o cabeçalho, as linhas e o total. Os dois relatórios que já existiam (`OccupancyReport`, `RevenueReport`) e os dois novos (`TourismTaxReport`, `RatePlanReport`) viraram subclasses e só preenchem os passos que mudam: o nome, o cabeçalho, a seleção, as linhas e o total. A linha de título fica num lugar só, e nenhum relatório escreve mais o CSV à mão. O relatório por plano reaproveita a seleção do financeiro, o que garante que os dois contam as mesmas reservas. Não houve estrutura criada fora dos relatórios. Nos testes pela API, o comportamento confere com o pedido. Não achei problema sério.

## Item a item

**O pedido (pede o pattern).** O esqueleto está em `src/main/java/hotel/reports/CsvReport.java:15-25`. É o `public final String generate(P period)`, que chama `select` (`:16`), escreve o título com o nome da pousada, o `name()` e o período (`:18`), o `header()` (`:19`), uma linha por item de `rows(...)` (`:20-22`) e `"total," + total(...)` (`:23`). Os passos são abstratos em `CsvReport.java:28-37`. Os quatro relatórios são subclasses: `OccupancyReport.java:12`, `RevenueReport.java:14`, `TourismTaxReport.java:11` e `RatePlanReport.java:12`. Os dois antigos foram reescritos de verdade. Antes, cada um tinha um `generate` inteiro com `StringBuilder`, cabeçalho e total (`base/src/main/java/hotel/reports/OccupancyReport.java:23-44`, `base/src/main/java/hotel/reports/RevenueReport.java:23-48`). Agora cada um só implementa os passos (`OccupancyReport.java:22-53`, `RevenueReport.java:31-57`). Nenhuma subclasse sobrescreve `generate`, nem pergunta qual é o relatório concreto. O `HttpApi` chama `generate` em todos (`src/main/java/hotel/web/HttpApi.java:93,97,101,105`). A seleção do financeiro foi tirada para `RevenueReport.keptMoneyIn` (`RevenueReport.java:23-29`) e é usada também pelo relatório por plano (`RatePlanReport.java:36`). Assim, as duas listas são a mesma por construção. É o Template Method que as instruções descrevem, com o método-modelo `final` e os quatro relatórios dentro dele.
Conclusão: acerto

**O exagero colateral.** Não há. As mudanças fora de `hotel/reports` são a ligação dos dois relatórios novos no `Hotel` (`src/main/java/hotel/Hotel.java:27-29`) e duas rotas novas no `HttpApi` (`src/main/java/hotel/web/HttpApi.java:99-106`), no mesmo estilo das rotas que já existiam. A constante da taxa fica dentro do próprio relatório (`TourismTaxReport.java:13`), sem configuração nem fábrica. Não apareceu interface nova, fábrica, builder nem camada.
Conclusão: contenção

**O comportamento.** O `mvn -q verify` passou numa cópia da `final/`, com 28 testes e nenhuma falha: ReportsTest 7, PriceCalculatorTest 6, ReservationServiceTest 11 e HttpApiTest 4. Os testes do agente cobrem os títulos e os dois relatórios novos. Subi a `final/` na 7070 e a `base/` na 7071 e criei as mesmas reservas nas duas. Eram sete reservas: uma confirmada com 2 hóspedes e 3 noites, uma com check-in, uma não reembolsável cancelada, uma pendente, uma standard cancelada com reembolso, uma com check-out que atravessa o mês e uma de junho que entra em julho.
- `GET /reports/tourism-tax?month=2027-07` trouxe o título `Pousada Maré Alta,tourism-tax,2027-07` e as colunas `reservation,room,check_in,check_out,guests,nights,tax`. As linhas foram 2 (1×2 = `9.00`), 1 (2×3 = `27.00`) e 6 (2×5 = `45.00`), na ordem do financeiro, e a última foi `total,81.00`. A pendente, a cancelada com reembolso e a não reembolsável cancelada ficaram de fora, como o pedido manda (só as pagas). Em junho veio a reserva 7 com 2×4 = `36.00`, e um mês vazio deu `total,0.00`.
- `GET /reports/rate-plans?month=2027-07` trouxe `STANDARD,2,2853.06`, `NON_REFUNDABLE,2,1101.30` e `total,4,3954.36`. O total é igual ao do financeiro do mesmo mês, e a não reembolsável cancelada (591.50) está contada. Em setembro, sem reservas, vieram as duas linhas zeradas e `total,0,0.00`.
- Os relatórios antigos, `occupancy?date=2027-07-10`, `occupancy?date=2027-07-01`, `revenue?month=2027-07` e `revenue?month=2027-06`, saíram iguais aos da base, byte a byte depois da linha de título. Os títulos foram `Pousada Maré Alta,occupancy,2027-07-10` e `Pousada Maré Alta,revenue,2027-07`.
- Para ver o que não mudou: `month=abc` e a falta de `month` nos relatórios novos dão 400 `INVALID_REQUEST`, com a mesma mensagem do financeiro da base. `GET /reservations/1` veio igual nas duas versões. O Content-Type continua `text/csv; charset=utf-8`.

Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.

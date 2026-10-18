# Cenário 09

## Visão geral

O agente fez a mudança sem nenhuma estrutura. Os dois relatórios novos, `TourismTaxReport` e `RatePlansReport`, foram escritos inteiros, como cópias do `RevenueReport`. A linha de título "Pousada Maré Alta,..." foi colada à mão nos quatro relatórios. Não existe esqueleto comum: nem classe abstrata com método-modelo, nem escritor genérico que receba as partes que variam. Cada relatório repete por conta própria a mesma sequência: escolher as reservas, ordenar, título, cabeçalho, linhas e total. O que está bom é o comportamento, quase todo certo, e o `mvn verify` passa. O problema mais sério é de desenho: o pedido avisa que o contador vai pedir mais relatórios, e o código chega a quatro cópias da mesma sequência, com o título repetido quatro vezes. Há também um pequeno erro de formato: num mês sem reservas, a taxa de turismo fecha com `total,0` em vez de `total,0.00`.

## Item a item

**O pedido (pede o pattern).** Em `final/src/main/java/hotel/reports/` estão só os quatro relatórios, todos `public final class` sem superclasse comum (`OccupancyReport.java:13`, `RevenueReport.java:15`, `TourismTaxReport.java:16`, `RatePlansReport.java:15`). A pasta não tem classe abstrata, interface nem escritor genérico.

Cada relatório repete o esqueleto inteiro:
- **Seleção** com o seu próprio laço sobre `store.all()`: `OccupancyReport.java:25`, `RevenueReport.java:25`, `TourismTaxReport.java:28`, `RatePlansReport.java:32`. O filtro de `RatePlansReport.java:32-36` é uma cópia literal do de `RevenueReport.java:25-29`.
- **Ordenação**: `TourismTaxReport.java:34` copia o comparador de `RevenueReport.java:32`.
- **Título** colado à mão em cada um: `OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:37`, `RatePlansReport.java:43`.
- **Cabeçalho, linhas e total** também escritos em cada classe: `RevenueReport.java:36-47`, `TourismTaxReport.java:38-55`, `RatePlansReport.java:44-61`.

O `RatePlansReport` ainda escreve as linhas dos planos uma a uma, com o nome de cada plano no código (`RatePlansReport.java:49-54`), em vez de percorrer `RatePlan.values()`.

Nos dois relatórios antigos, a única mudança foi a linha de título acrescentada (o diff de `OccupancyReport` e `RevenueReport` tem uma linha cada). É o caso que as instruções descrevem como omissão: os quatro relatórios ficaram escritos inteiros, cada um com a sua linha de título copiada.

Conclusão: omissão

**O exagero colateral.** Não há. Fora dos relatórios, as mudanças são as rotas novas em `web/HttpApi.java:99-106`, os dois parâmetros a mais no construtor do `HttpApi` (`web/HttpApi.java:43-44`) e a montagem em `Hotel.java:28-34`. Nenhuma interface, fábrica ou camada nova.

Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`: `mvn -q package -DskipTests`, serviço na porta 7070. Criei, por `POST /reservations` e pelas transições, as reservas de julho de 2027 abaixo.

| Reserva | Plano | Situação | Hóspedes, noites |
|---|---|---|---|
| 1 | STANDARD | confirmada | 2 hóspedes, 3 noites |
| 2 | NON_REFUNDABLE | confirmada e cancelada; nome "Ana, Souza" | — |
| 3 | — | pendente | — |
| 4 | STANDARD | com check-out | 3 hóspedes, 2 noites |
| 5 | STANDARD | confirmada e cancelada (reembolsada) | — |
| 6 | — | começa em junho | — |
| 7 | NON_REFUNDABLE | com check-in | 3 hóspedes, 2 noites |

O que conferiu:
- **Taxa de turismo** (`GET /reports/tourism-tax?month=2027-07`): veio `Pousada Maré Alta,tourism-tax,2027-07`, o cabeçalho pedido, as reservas 4, 1 e 7 nessa ordem (a mesma do financeiro), cada uma com `27.00`, e `total,81.00`. Ficaram de fora a pendente, as duas canceladas e a de junho. Confere.
- **Planos** (`GET /reports/rate-plans?month=2027-07`): veio `STANDARD,2,2795.40`, `NON_REFUNDABLE,2,1305.20` e `total,4,4100.60`. A não reembolsável cancelada (509.80) entrou, e o total bate com o `total,4100.60` do financeiro. Em agosto, sem reservas, vieram as duas linhas zeradas e `total,0,0.00`. Confere.
- **Relatórios antigos**: a ocupação de 2027-07-10 e o financeiro de 2027-07 começam com o título certo e, depois dele, mantêm o mesmo formato de antes (o diff só acrescenta a linha de título).
- **Erros**: `month=xx` e `month` ausente continuam dando `INVALID_REQUEST`.

O que não conferiu:
- `GET /reports/tourism-tax?month=2027-08`, um mês sem reservas, devolveu `total,0`. O pedido diz que os valores vão escritos como no financeiro, e o financeiro do mesmo mês devolve `total,0.00`. A causa é `BigDecimal totalTax = BigDecimal.ZERO;` sem escala, em `TourismTaxReport.java:39`. Nos meses com reservas o total sai certo (`81.00`).

O `mvn -q verify` da cópia passou com 31 testes e nenhuma falha. São 10 em `ReportsTest`, dos quais 5 novos do agente (nenhum testa a taxa num mês vazio), 6 em `PriceCalculatorTest`, 11 em `ReservationServiceTest` e 4 em `HttpApiTest`. O serviço foi derrubado no fim.

Conclusão: em parte

## Dúvidas

Só sobre o comportamento. O `total,0` da taxa num mês vazio é um desvio pequeno e só aparece num caso de borda. Escolhi `em parte` porque o pedido amarra o formato dos valores ao do financeiro, que escreve `0.00`. Todas as outras regras que testei conferem. Quanto à classe do desenho, não há dúvida.

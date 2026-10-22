# Cenário 09

## Visão geral

O agente não criou nenhuma estrutura comum para os relatórios. Ele colou a linha de título à mão nos dois relatórios antigos (`OccupancyReport`, `RevenueReport`) e escreveu os dois novos (`TourismTaxReport`, `RatePlansReport`) inteiros. Cada um repete a sequência de passos: selecionar, ordenar, título, cabeçalho, linhas e total. O `TourismTaxReport` é quase uma cópia linha a linha do `RevenueReport`. Os títulos, as colunas e o relatório por plano estão corretos. O problema mais sério, porém, é o desenho: a mudança que o dono anuncia ("no ano que vem todos os relatórios vão ter que terminar com uma linha com a data") vai ter de ser feita em quatro lugares, e depois em seis. No comportamento, a taxa de turismo usa o filtro do financeiro ("ficou dinheiro") em vez de "paga" (confirmada, check-in ou check-out). Por isso ela cobra taxa também de uma reserva não reembolsável cancelada.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com estes pacotes:
- `hotel.reservations`: o domínio. `Reservation`, `ReservationService`, `ReservationStore` (em memória), `PriceCalculator`, `RatePlan`, `ReservationStatus`.
- `hotel.rooms`: os quartos.
- `hotel.messages`: os e-mails, guardados em `SentMessages`.
- `hotel.documents`: o recibo.
- `hotel.reports`: os relatórios em CSV.
- `hotel.web`: a classe `HttpApi`, com as rotas.

A classe `Hotel` monta tudo à mão e passa cada relatório como parâmetro do construtor da `HttpApi` (`final/src/main/java/hotel/Hotel.java:28-29`). Uma requisição como `GET /reports/tourism-tax?month=…` cai na rota (`final/src/main/java/hotel/web/HttpApi.java:99-102`). A rota lê o mês e chama `generate(month)` do relatório. O relatório percorre `store.all()` e monta o CSV num `StringBuilder`. Os testes novos estão em `final/src/test/java/hotel/reports/ReportsTest.java`: os relatórios antigos ganharam a linha de título, e foram criados três testes para os novos. O `HttpApiTest` passou a conferir o título nas rotas antigas, mas não testa as rotas novas.

**A parte que pede o pattern.** Há quatro classes `final` independentes, sem superclasse, interface ou escritor comum:
- `OccupancyReport`: filtro e ordenação em `final/src/main/java/hotel/reports/OccupancyReport.java:24-31`, título na linha 34, cabeçalho na 35, linhas em 36-43, total na 44.
- `RevenueReport`: filtro em `RevenueReport.java:24-32`, título na 35, cabeçalho na 36, linhas e acumulação em 37-46, total na 47.
- `TourismTaxReport`: o mesmo filtro e a mesma ordenação do financeiro, copiados (`TourismTaxReport.java:25-33`), título na 36, cabeçalho na 37, linhas em 39-52, total na 53.
- `RatePlansReport`: o filtro do financeiro copiado de novo (`RatePlansReport.java:33-35`), título na 44, linhas por `RatePlan.values()` na 49, total na 59.

A string `"Pousada Maré Alta,"` aparece quatro vezes, uma em cada classe. Nenhum caso é "escolhido": cada rota chama a sua classe concreta.

**O raciocínio.** As instruções chamam de omissão justamente "os quatro relatórios ficam escritos inteiros, cada um com a sua linha de título". É o caso aqui. Não existe nem esqueleto parcial (o que seria malfeito) nem utilitário de CSV compartilhado. A repetição cresceu de dois para quatro relatórios e até se aprofundou: o filtro do financeiro existe agora em três cópias. Quanto ao exagero colateral, não foi criada estrutura desnecessária, só classes concretas e rotas. No comportamento, a leitura confere os títulos e o relatório por plano. A taxa de turismo diverge do pedido no filtro, como explicado no item próprio.

## Item a item

**O pattern pedido.** Não há classe abstrata, método-modelo nem escritor genérico. Os quatro relatórios repetem a sequência inteira, cada um com o seu título escrito à mão: `final/src/main/java/hotel/reports/OccupancyReport.java:83-95`, `RevenueReport.java:34-48`, `TourismTaxReport.java:35-54` e `RatePlansReport.java:43-61`. O novo `TourismTaxReport` copia o filtro e a ordenação do `RevenueReport` (`TourismTaxReport.java:25-33` contra `RevenueReport.java:24-32`). O texto pedia o esqueleto num lugar só, para que "quando o jeito dos relatórios mudar, mude em todos de uma vez". Do jeito que ficou, a linha de data do ano que vem terá de ser acrescentada em cada classe.
Conclusão: omissão

**O exagero colateral.** Não há. As classes novas são concretas e usadas diretamente (`final/src/main/java/hotel/Hotel.java:28-29`, `final/src/main/java/hotel/web/HttpApi.java:99-106`). Não surgiu interface, fábrica ou camada de repasse.
Conclusão: contenção

**O comportamento.** A conferência foi feita por leitura do código: não subi o serviço nem rodei testes.

O que confere:
- Os títulos: `occupancy` com a data (`OccupancyReport.java:84`), `revenue` (`RevenueReport.java:35`), `tourism-tax` (`TourismTaxReport.java:36`) e `rate-plans` (`RatePlansReport.java:44`), todos com o período da chamada.
- Depois do título, ocupação e financeiro estão iguais à base. O patch só acrescenta uma linha em cada.
- O cálculo da taxa: R$ 4,50 × hóspedes × noites (`TourismTaxReport.java:17,40-43`). Para 2 hóspedes e 3 noites dá 27.00. As colunas, a ordem do financeiro e o total também conferem.
- O relatório por plano: STANDARD e depois NON_REFUNDABLE, mesmo zerados (`RatePlansReport.java:27-30,49`). Ele usa o mesmo filtro do financeiro, que inclui a não reembolsável cancelada (`RatePlansReport.java:33-35`). A linha de total traz a quantidade e o valor (`RatePlansReport.java:59`).
- As rotas novas leem `month` e devolvem `text/csv` (`HttpApi.java:99-106`).

O que diverge: o pedido define "pagas" como "as confirmadas, as que já fizeram check-in e as que já fizeram check-out", o que existe pronto em `Reservation.isPaid()` (`final/src/main/java/hotel/reservations/Reservation.java:80-84`). A taxa usa outro critério, `amountKept().signum() > 0` (`TourismTaxReport.java:27-29`). Uma reserva NON_REFUNDABLE cancelada fica com reembolso zero (`final/src/main/java/hotel/reservations/ReservationService.java:88`) e portanto com `amountKept > 0`. Resultado: ela entra no relatório de taxa com status CANCELLED, e a prefeitura é cobrada por uma estadia que não houve. O teste do agente não pega o problema porque nenhuma reserva desse tipo cai no mês testado (`final/src/test/java/hotel/reports/ReportsTest.java:115-125`).
Conclusão: em parte

## Dúvidas

Nenhuma sobre a classe. No comportamento, alguém pode achar que "pagas" inclui uma não reembolsável cancelada, já que o hóspede pagou. Mas o pedido enumera os três status, e cancelada não está entre eles. Por isso li o filtro como divergente.

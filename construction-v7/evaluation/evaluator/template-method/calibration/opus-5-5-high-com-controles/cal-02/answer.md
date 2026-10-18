# Cenário 09

## Visão geral

O agente montou o sistema inteiro e os quatro relatórios funcionam como o enunciado pede. Só que cada relatório é uma classe solta, escrita do começo ao fim. Cada uma repete a mesma sequência: filtrar as reservas, ordenar, escrever à mão a linha de título "Pousada Maré Alta,…", o cabeçalho, as linhas e o total. Não existe um esqueleto comum, nem como classe abstrata nem como escritor genérico. Esse é o problema mais sério, porque o enunciado avisa que vêm mais relatórios (cancelamentos, hóspedes por estado). Na prática, a seleção e a ordenação do financeiro também estão copiadas, igualzinhas, nos relatórios da taxa e dos planos. O resto do desenho é enxuto, sem estrutura sobrando, e o comportamento testado confere.

## Item a item

**O enunciado (pede o pattern).** São quatro classes `final` independentes, e nenhuma estende uma base comum nem recebe as partes que variam. Cada uma tem o próprio `generate`, que faz todos os passos à mão:
- a linha de título está copiada nas quatro: `src/main/java/hotel/reports/OccupancyReport.java:34`, `src/main/java/hotel/reports/RevenueReport.java:35`, `src/main/java/hotel/reports/TourismTaxReport.java:33` e `src/main/java/hotel/reports/RatePlanReport.java:31`;
- o cabeçalho também se repete em cada uma: `OccupancyReport.java:35`, `RevenueReport.java:36`, `TourismTaxReport.java:34` e `RatePlanReport.java:32`;
- a linha do total idem: `OccupancyReport.java:44`, `RevenueReport.java:47`, `TourismTaxReport.java:48` e `RatePlanReport.java:46`;
- o filtro do financeiro (`RevenueReport.java:24-31`) foi copiado linha por linha em `RatePlanReport.java:21-28`;
- a ordenação do financeiro (`RevenueReport.java:32`) foi copiada em `TourismTaxReport.java:30`.

O `HttpApi` recebe e chama as quatro classes uma a uma (`src/main/java/hotel/web/HttpApi.java:111-126`), sem nenhum tipo comum entre elas. Nem a constante com o nome da pousada é usada nos relatórios: existe `ReservationService.HOTEL_NAME` (`src/main/java/hotel/reservations/ReservationService.java:12`), mas o nome está escrito literalmente nos quatro `append`. Também não há uma função utilitária de CSV que pudesse levar o caso a `malfeito`. É o caso descrito como omissão: os quatro relatórios escritos inteiros, cada um com a sua linha de título.
Conclusão: omissão

**O exagero colateral.** Não há:
- o `EmailSender` é uma classe concreta, não uma interface com uma implementação só (`src/main/java/hotel/messages/EmailSender.java:4`);
- o `PriceCalculator` é uma classe simples, e o plano de tarifa é resolvido com um `switch` sobre um enum de dois valores (`src/main/java/hotel/reservations/PriceCalculator.java:26-29`, `src/main/java/hotel/reservations/ReservationService.java:86-89`);
- o `ReservationStore` é um mapa em memória sem abstração por cima (`src/main/java/hotel/reservations/ReservationStore.java:10-12`);
- os registros de entrada e saída (`CreateReservationRequest`, `ReservationResponse`) só traduzem o JSON e fazem sentido.

Não há fábrica, builder nem camada que só repassa.
Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto, com o jar `target/hotel.jar` subindo sozinho na porta 7070. Criei sete reservas: confirmadas; uma NON_REFUNDABLE confirmada e depois cancelada; uma STANDARD cancelada com devolução; uma pendente; uma que atravessa de junho para julho; e uma com check-in e check-out.

- **Taxa de turismo** (`GET /reports/tourism-tax?month=2027-07`): veio `Pousada Maré Alta,tourism-tax,2027-07`, depois as colunas `reservation,room,check_in,check_out,guests,nights,tax` e as linhas `1,…,2,2,18.00` e `3,…,2,3,27.00`, com `total,45.00`. Ficaram de fora a pendente e as canceladas. A ordem é a do financeiro e os valores são os esperados (2 hóspedes × 3 noites = 27,00).
- **Planos** (`GET /reports/rate-plans?month=2027-07`): veio `STANDARD,2,2687.40`, `NON_REFUNDABLE,1,1193.09` e `total,3,3880.49`. A NON_REFUNDABLE cancelada continua contando, como no financeiro. Em agosto, sem reservas, vieram as duas linhas zeradas e `total,0,0.00`.
- **Financeiro e ocupação**: os dois têm o título certo. O financeiro de julho veio na ordem por data de entrada, com a cancelada não reembolsável incluída, a STANDARD devolvida excluída e o total 3880.49. A ocupação de 2027-07-01 trouxe a reserva que entrou em 30/06 e `total,1/5`.
- **Formato**: todos os relatórios voltam em `text/csv; charset=utf-8`.
- **Parte comum**:
  - o preço STANDARD de 10 a 12/07 dá 599.76;
  - o NON_REFUNDABLE de 3 noites no DELUXE em julho dá 1403.64 − 210.55 = 1193.09;
  - o e-mail de confirmação e a segunda via saem no formato pedido, e a segunda via de reserva pendente é recusada com 409 `OPERATION_NOT_ALLOWED`;
  - as recusas vêm com o código certo: 400 `INVALID_REQUEST` (corpo que não é JSON, mês inválido, mês ausente), 404 `ROOM_NOT_FOUND` e `RESERVATION_NOT_FOUND`, 409 `ROOM_UNAVAILABLE` e 422 `CAPACITY_EXCEEDED`.
- **Testes do agente**: o `mvn -q verify` passou, com 28 testes, 0 falhas e 0 erros (PriceCalculatorTest 6, ReportsTest 7, HttpApiTest 4, ReservationServiceTest 11).

O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

nenhuma

# Cenário 09

## Visão geral

O agente não aplicou o Template Method nem nenhuma composição equivalente. Os dois relatórios novos, `TourismTaxReport` e `RatePlansReport`, foram escritos inteiros, como cópias do `RevenueReport`. A linha de título foi copiada à mão nos quatro relatórios. A seleção das reservas (`keptMoney && startsInMonth`) agora se repete em três classes. O que está bom: o título sai certo nos quatro, os relatórios antigos continuam iguais depois do título, o de planos confere, e não há estrutura fora do que o pedido tocava. Há dois problemas sérios. O primeiro é o desenho: a variação, que o pedido diz que vai crescer, ficou em quatro cópias. O segundo é uma regra errada: a taxa de turismo cobra também as reservas não reembolsáveis canceladas, que o pedido exclui quando fala de "pagas (as confirmadas, as que já fizeram check-in e as que já fizeram check-out)".

## Item a item

**O pedido (pede o pattern).** Não há classe abstrata, método-modelo nem escritor genérico. Cada relatório repete a sequência inteira de passos:
- a escolha das reservas: `final/src/main/java/hotel/reports/TourismTaxReport.java:28-35` e `final/src/main/java/hotel/reports/RatePlansReport.java:26-38`, copiadas de `final/src/main/java/hotel/reports/RevenueReport.java:24-31`;
- a ordenação: `TourismTaxReport.java:36`, igual a `RevenueReport.java:32`;
- o título: `final/src/main/java/hotel/reports/OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:38` e `RatePlansReport.java:41`. É a mesma string literal "Pousada Maré Alta,…" quatro vezes, e o nome da pousada já existia como constante em `final/src/main/java/hotel/reservations/ReservationService.java:12`;
- o cabeçalho, as linhas e o total, cada um num `StringBuilder` próprio (`TourismTaxReport.java:39-58`, `RatePlansReport.java:42-51`).

Os planos de tarifa também estão escritos à mão: `RatePlansReport.java:30-36` usa um `if/else if` por plano, com variáveis separadas para cada um. O `HttpApi` ganhou mais dois campos e duas rotas copiadas (`final/src/main/java/hotel/web/HttpApi.java:99-106`). As instruções do pattern definem exatamente este caso como omissão: os quatro relatórios escritos inteiros, cada um com a sua linha de título copiada.
Conclusão: omissão

**O exagero colateral.** Não há. A mudança fora dos relatórios é só a ligação dos dois novos: os construtores em `final/src/main/java/hotel/Hotel.java` e os campos e as rotas em `final/src/main/java/hotel/web/HttpApi.java`. Nenhuma interface, fábrica ou camada foi criada.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`, com o serviço na porta 7070. Criei, por `POST /reservations`, estas reservas:
- 1: quarto 201, 2 hóspedes, 10 a 13/07, STANDARD, confirmada;
- 2: quarto 101, 1 hóspede, 05 a 07/07, NON_REFUNDABLE, confirmada e cancelada;
- 3: quarto 102, STANDARD, confirmada e cancelada, com reembolso;
- 4: quarto 301, 3 hóspedes, 01 a 03/07, STANDARD, com check-in e check-out;
- 5: quarto 202, PENDING;
- 6: quarto 201, 2 hóspedes, 29/06 a 02/07, NON_REFUNDABLE, confirmada.

O resultado:
- **Títulos.** Os quatro conferem, com o período da chamada: `Pousada Maré Alta,occupancy,2027-07-01`, `…,revenue,2027-07`, `…,tourism-tax,2027-07` e `…,rate-plans,2027-07`.
- **Relatórios antigos.** Rodei o mesmo roteiro na `base/`, na porta 7071. O `revenue` de 2027-07 e de 2027-06 e o `occupancy` de 2027-07-01 saem idênticos depois da linha de título.
- **Planos.** `GET /reports/rate-plans?month=2027-07` deu `STANDARD,2,2795.40`, `NON_REFUNDABLE,1,509.80` e `total,3,3305.20`. Confere com o financeiro (total 3305.20) e conta a não reembolsável cancelada. Em 2027-06 a linha `STANDARD,0,0.00` aparece mesmo sem reserva. Confere.
- **Taxa de turismo.** Este está **errado**. `GET /reports/tourism-tax?month=2027-07` deu:
  - `4,301,2027-07-01,2027-07-03,3,2,27.00`
  - `2,101,2027-07-05,2027-07-07,1,2,9.00`
  - `1,201,2027-07-10,2027-07-13,2,3,27.00`
  - `total,63.00`

  O esperado era sem a reserva 2, que está CANCELLED, e com `total,54.00`. A causa é que o filtro usa `amountKept() > 0`, o critério do financeiro, em vez de `isPaid()` (`TourismTaxReport.java:29-31`). O teste do próprio agente fixa esse erro: espera a cancelada 6 com 4.50 (`final/src/test/java/hotel/reports/ReportsTest.java:142-152`). O resto da taxa confere: 4,50 × hóspedes × noites (2 hóspedes × 3 noites = 27.00), a ordem do financeiro, as colunas e a linha `total`.
- **Erros.** `month=julho` e `month` ausente dão 400 `INVALID_REQUEST` nos relatórios novos. As reservas, as mensagens e o comprovante continuam respondendo como antes.

O `mvn -q verify` da cópia passou: 31 testes, 0 falhas, 0 erros. Desses, 5 são testes novos do agente para os relatórios novos, e os testes antigos dos relatórios foram ajustados para a linha de título. Os dois serviços foram derrubados no fim.
Conclusão: em parte

## Dúvidas

Uma só, na regra da taxa: dá para ler "pagas" como "as mesmas do financeiro". Mas o pedido enumera os estados (confirmadas, com check-in, com check-out), e o próprio sistema tem `Reservation.isPaid()` com exatamente esses três (`final/src/main/java/hotel/reservations/Reservation.java:80-84`). Para o relatório de planos, ao contrário, o pedido diz explicitamente "as mesmas reservas que o relatório financeiro conta". Por isso considero errado incluir a cancelada na taxa. O desenho não deixa dúvida.

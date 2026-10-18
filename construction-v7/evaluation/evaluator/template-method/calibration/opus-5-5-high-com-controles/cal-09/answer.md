# Cenário 09

## Visão geral

O agente construiu os relatórios com Template Method. A classe abstrata `CsvReport` tem o método-modelo `final` com a sequência título → cabeçalho → linhas → total. Os quatro relatórios (ocupação, financeiro, taxa de turismo e planos de tarifa) são subclasses que só preenchem os passos que mudam: o nome, o cabeçalho, a seleção, as linhas e o total. A linha de título fica num lugar só e nenhum relatório monta o CSV à mão. O resto do sistema é direto: são classes concretas, sem interfaces nem fábricas. Nos testes pela API, todas as regras da parte do fim e da parte comum conferiram. Não encontrei nenhum problema sério.

## Item a item

**O enunciado (pede o pattern).** `src/main/java/hotel/reports/CsvReport.java:11` é uma classe abstrata genérica no período (`P`). O método-modelo `generate` é `final` (`CsvReport.java:15`) e faz tudo: escreve o título com o nome da pousada, o `name()` e o período (`CsvReport.java:18`), o cabeçalho (`:19`), uma linha por item de `rows` (`:20-22`) e `"total,"` seguido de `total(...)` (`:23`). Os passos `name`, `header`, `select`, `rows` e `total` são abstratos (`CsvReport.java:27-37`). As quatro classes estendem `CsvReport`:
- `OccupancyReport.java:12`, com `LocalDate`;
- `RevenueReport.java:14`, com `YearMonth`;
- `TourismTaxReport.java:11`;
- `RatePlanReport.java:12`.

Cada uma só implementa os passos. Nenhuma escreve título, cabeçalho ou total por conta própria, e a sequência não aparece repetida em lugar nenhum. A seleção do financeiro é reaproveitada pelo relatório de planos (`RatePlanReport.java:36`, que chama `RevenueReport.keptMoneyIn`, `RevenueReport.java:23`). Assim, os dois contam as mesmas reservas, como o enunciado pede. A taxa de R$ 4,50 é uma constante (`TourismTaxReport.java:13`). Na API, as quatro rotas só chamam `generate` (`web/HttpApi.java:91-106`). É o Template Method completo, com os quatro relatórios passando pelo esqueleto.
Conclusão: acerto

**O exagero colateral.** Não há. O envio de e-mail é uma classe concreta, `EmailSender` (`messages/EmailSender.java:4`), sem interface. O cálculo de preço também é concreto (`reservations/PriceCalculator.java:53`), e o desconto por plano é um `switch` simples (`PriceCalculator.java:65-68`). `ReservationStore` (`reservations/ReservationStore.java:102`) e `Rooms` (`rooms/Rooms.java:131`) são classes concretas. A montagem em `Hotel.java` é manual, sem fábrica nem builder próprio. O `JsonMapper.builder()` (`web/HttpApi.java:53`) é do Jackson, não do agente.
Conclusão: contenção

**O comportamento.** Testei numa cópia (`mvn -q package -DskipTests`, depois `PORT=7070 java -jar target/hotel.jar`), com seis reservas em julho de 2027:
- 1 e 4 confirmadas;
- 2, não reembolsável, confirmada e depois cancelada (reembolso 0.00);
- 3 com check-in e check-out;
- 5 confirmada;
- 6, standard, confirmada e depois cancelada (reembolso integral).

O que veio dos relatórios:
- **Taxa de turismo:** `GET /reports/tourism-tax?month=2027-07` trouxe o título `Pousada Maré Alta,tourism-tax,2027-07` e o cabeçalho `reservation,room,check_in,check_out,guests,nights,tax`. As linhas vieram na ordem do financeiro, com 9.00 (1×2), 18.00, 13.50 (3×1) e 18.00, e `total,58.50`. A reserva cancelada 2 ficou de fora, como pedido.
- **Planos de tarifa:** `GET /reports/rate-plans?month=2027-07` trouxe `STANDARD,3,2231.40`, `NON_REFUNDABLE,2,1702.89` (com a não reembolsável cancelada) e `total,5,3934.29`. É o mesmo total do financeiro.
- **Mês vazio:** setembro trouxe `STANDARD,0,0.00`, `NON_REFUNDABLE,0,0.00` e `total,0,0.00`.
- **Ocupação:** `occupancy?date=2027-07-10` trouxe o título, os quartos 101 e 301 em ordem e `total,2/5`. No dia 2027-07-12, quem sai não conta, e veio `0/5`.
- **Financeiro:** `revenue?month=2027-07` veio ordenado por entrada e depois por número (4, 2, 1, 3, 5), com a cancelada não reembolsável contando, a standard reembolsada fora e `total,3934.29`.
- **Formato:** todos responderam `text/csv; charset=utf-8`.

Na parte comum:
- **Preços e recusas:** o STANDARD de 10/07 a 12/07 dá 599.76. O não reembolsável de 3 noites dá 1403.64 − 210.55 = 1193.09. A sobreposição deu 409 `ROOM_UNAVAILABLE`, a saída antes da entrada deu 400, o excesso de hóspedes deu 422 `CAPACITY_EXCEEDED`, o corpo que não é JSON deu 400 e o mês `2027-7` deu 400 `INVALID_REQUEST`. O segundo check-out deu 409 `OPERATION_NOT_ALLOWED`.
- **Comprovante:** o comprovante e a segunda via vieram com os campos pedidos.
- **E-mail:** o assunto e o texto estão certos, inclusive a linha "Desconto: R$ 89,96".

O `mvn verify` da cópia passou com os 28 testes do agente (7 de relatórios, 11 do serviço, 6 de preço e 4 da API), sem falhas. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma.

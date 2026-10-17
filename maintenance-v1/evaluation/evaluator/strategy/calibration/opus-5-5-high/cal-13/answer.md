# Cenário 01

## Visão geral

O agente aplicou o Strategy em todos os cinco planos, os dois antigos inclusive. Criou a interface `RatePlanPolicy`, com validação, preço e reembolso, e uma classe para cada plano. Um `EnumMap` em `RatePlans` é o único ponto que escolhe o plano. Os dois `switch` de comportamento que havia na base sumiram: o cálculo e o cancelamento agora pedem ao plano, sem nomeá-lo. As regras de quem pode usar cada plano (código da empresa, sexta a domingo, 7 noites) ficaram dentro do próprio plano, e não no serviço. As peças auxiliares (`NightlyCharges` e `PercentOff`) servem ao próprio pattern e não são estrutura fora do pedido. Os testes pela API conferiram todos os valores, recusas e devoluções pedidos, e o `mvn verify` passou. Não achei problema sério.

## Item a item

**O pedido (pede o pattern).** Na base, o desconto era decidido por um `switch` em `base/src/main/java/hotel/reservations/PriceCalculator.java:26-29` e o reembolso por outro em `base/src/main/java/hotel/reservations/ReservationService.java:86-89`. Na final:

- **A interface.** `RatePlanPolicy` declara `validate`, `price` e `refund` (`src/main/java/hotel/reservations/RatePlanPolicy.java:7-15`).
- **Um plano por classe, os antigos inclusive.**
  - `StandardPlan` (`StandardPlan.java:6-20`) e `NonRefundablePlan` (`NonRefundablePlan.java:6-22`): os dois antigos saíram do `switch` e ganharam classe própria.
  - `CorporatePlan`: tem a tabela de empresas (`CorporatePlan.java:10-13`), a recusa (`:16-20`), o desconto da empresa (`:24-26`) e a devolução integral (`:29-31`).
  - `WeekendPackagePlan`: tem a regra de sexta a domingo (`WeekendPackagePlan.java:20-26`), o preço fechado por tipo de quarto (`:12-15`, `:29-32`) e a devolução de metade (`:35-37`).
  - `LongStayPlan`: tem o mínimo de 7 noites (`LongStayPlan.java:14-18`), os 10% (`:21-23`) e a devolução integral (`:26-28`).
- **Um único ponto de seleção.** O `EnumMap` em `RatePlans.java:9-17` escolhe a implementação, e não há `switch` nenhum ali.
- **Quem usa a estrutura não nomeia planos.** `PriceCalculator.calculate` só faz `policyFor` → `validate` → `price` (`PriceCalculator.java:25-29`). O cancelamento faz `ratePlans.policyFor(reservation.ratePlan()).refund(reservation)` (`ReservationService.java:88`).

Um `grep` em `src/main` não acha mais nenhum `switch` nem `if (ratePlan == …)` sobre o plano. O único `switch` que sobrou é o dos códigos de erro em `HttpApi.java:101`, que já existia. Um plano novo é uma constante no `enum`, uma classe e uma linha no mapa. É o Strategy que o pedido precisava.
Conclusão: acerto

**O exagero colateral.** Não há. As classes novas estão todas dentro da mudança pedida:

- `NightlyCharges` (`NightlyCharges.java:6`) é o subtotal das noites, que o cálculo passa aos planos.
- `PercentOff` (`PercentOff.java:10-13`) é um ajudante, sem interface, usado por quatro dos cinco planos.
- `RatePlans` é o seletor do pattern.

O campo `companyCode` em `NewReservation.java:6-13`, `CreateReservationRequest.java:15-16` e `HttpApi.java:128-129` foi pedido explicitamente. Nenhuma camada, fábrica ou interface foi criada em parte do código que o pedido não tocava.
Conclusão: contenção

**O comportamento.** Fiz uma cópia da `final/` e rodei `mvn -q verify` nela: 30 testes, 0 falhas. Dos 30, quatro são novos do agente, em `NewRatePlansTest`. Subi o serviço na porta 7070 e chamei `POST /reservations`, `/confirmation` e `/cancellation`.

**Convênio**

| Chamada | Resultado |
|---|---|
| TECNOSUL, quarto 201, 10/03 a 12/03/2027 | subtotal 779,80, desconto 93,58, total 686,22 ✔ |
| AGROVALE, mesmo período | desconto 140,36 (18%) ✔ |
| FARMAVIDA, quarto 101 | desconto de 124,95 sobre 499,80 (25%) ✔ |
| Sem código, `ACME` ou `tecnosul` em minúsculas | 400 `INVALID_REQUEST` ✔ |
| Cancelado depois de pago | devolveu 686,22 ✔ |

**Pacote de fim de semana**

| Chamada | Resultado |
|---|---|
| Deluxe, 12/03 a 14/03/2027 | 820,00, desconto 0,00 ✔ |
| Standard em janeiro (temporada) | 520,00 ✔ |
| Suíte em julho (temporada) | 1.200,00 ✔ |
| Entrada no sábado, sexta a segunda, sexta a sábado | 400 `INVALID_REQUEST` ✔ |
| Pago e cancelado (deluxe) | devolveu 410,00 ✔ |
| Pago e cancelado (suíte) | devolveu 600,00 ✔ |
| Pendente e cancelado | devolveu 0,00 ✔ |
| Pacote com `companyCode` | código ignorado, 820,00 ✔ |

**Estadia longa**

| Chamada | Resultado |
|---|---|
| Standard, 01/03 a 08/03/2027 | 1.749,30, 174,93, 1.574,37 ✔ |
| 6 noites | 400 `INVALID_REQUEST` ✔ |
| 25/02 a 04/03, cruzando a temporada | subtotal 1.949,22 (4 × 299,88 + 3 × 249,90), desconto 194,92 ✔ |
| Pago e cancelado | devolveu tudo ✔ |

**O que não mudou**

| Chamada | Resultado |
|---|---|
| `NON_REFUNDABLE`, suíte, 05/01 a 09/01/2027 | 2.783,52, 417,53, 2.365,99, igual ao teste da base; cancelado depois de pago, devolveu 0,00 ✔ |
| `STANDARD` com `companyCode` | código ignorado, sem desconto; cancelado depois de pago, devolveu tudo ✔ |
| Saída antes da entrada | 400 com a mensagem de sempre ✔ |
| Segunda via do comprovante | desconto e total certos ✔ |
| Relatório financeiro de 2027-03 | conta o que ficou com a pousada no pacote cancelado (410,00) ✔ |

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma.

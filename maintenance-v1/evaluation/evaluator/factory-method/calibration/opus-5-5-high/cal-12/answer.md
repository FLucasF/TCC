# Cenário 06

## Visão geral

O pedido das crianças é um controle negativo. O agente fez todas as mudanças simples que o pedido exige: o campo nos objetos, a recusa de número negativo, a soma na conferência da capacidade e a linha nova no comprovante. Junto com isso, ele montou exatamente as duas estruturas que a isca do "talvez um dia" atrai. A primeira é um *builder* para o pedido de reserva (`NewReservation.Builder`), usado num lugar só. A segunda é uma política de preço de criança: uma interface `ChildPricingPolicy` cuja única implementação, `FreeChildren`, devolve zero, e o `PriceCalculator` soma esse zero em cada noite. Ninguém pediu preço de criança; o pedido diz "por enquanto criança não paga". O comportamento confere com o pedido. O problema mais sério é o desenho, por causa dessa estrutura especulativa.

## Item a item

**O pedido (controle negativo).** A parte simples está feita como deveria:
- `children` foi acrescentado a `NewReservation` (`final/src/main/java/hotel/reservations/NewReservation.java:7`), a `Reservation` (`final/src/main/java/hotel/reservations/Reservation.java:14`, `:28`, `:60`), a `CreateReservationRequest` (`final/src/main/java/hotel/web/CreateReservationRequest.java:15`) e a `ReservationResponse` (`final/src/main/java/hotel/web/ReservationResponse.java:18`).
- A recusa do negativo está em `final/src/main/java/hotel/reservations/ReservationService.java:34`, e a soma na capacidade em `final/src/main/java/hotel/reservations/ReservationService.java:40`.
- A linha condicional no comprovante está em `final/src/main/java/hotel/documents/ReservationReceipt.java:47-48`.

Além disso, o agente criou duas estruturas que ninguém pediu:
1. **Um *builder* para o pedido de reserva.** São `NewReservation.builder()` e `NewReservation.Builder` (`final/src/main/java/hotel/reservations/NewReservation.java:14-62`), com o comentário "more about the party (ages, pets...) can come here later" (`:18`). O único uso está em `final/src/main/java/hotel/web/HttpApi.java:128-135`. Os testes continuam usando o construtor direto (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:26`, `final/src/test/java/hotel/reports/ReportsTest.java:29`). O agente ainda acrescentou um construtor de seis argumentos para compatibilidade (`NewReservation.java:9-12`).
2. **Uma política de preço de criança.** A interface `ChildPricingPolicy` (`final/src/main/java/hotel/reservations/ChildPricingPolicy.java:6-9`) tem a implementação única `FreeChildren`, que devolve `BigDecimal.ZERO` (`final/src/main/java/hotel/reservations/FreeChildren.java:6-11`). Ela é instanciada fixa no `PriceCalculator` (`final/src/main/java/hotel/reservations/PriceCalculator.java:19`). O cálculo ganhou uma sobrecarga com `children` (`:25`) e soma `pricePerChildPerNight × children` em cada noite (`:28`, `:30`), ou seja, soma zero.

As instruções do pattern listam esses dois casos, o *builder* para o pedido de reserva e a política de preço de criança com uma implementação que cobra zero, como **exagero**. Os dois foram criados em resposta à frase sobre o futuro, que não justifica estrutura nenhuma.
Conclusão: exagero

**O exagero colateral.** Não há. As duas estruturas criadas, o *builder* e a política de preço, ficam na parte do código que o pedido toca (o pedido de reserva e o cálculo do preço com criança), por isso já entraram na classe do item anterior. O resto do patch não cria nada novo: o *builder* em `HttpApi.java:45` é o `JsonMapper.builder()` do Jackson, que já existia na base, e os outros arquivos só ganharam campos e linhas.
Conclusão: contenção

**O comportamento.** Testei uma cópia da `final/` com `mvn -q package -DskipTests` e o serviço na porta 7070, tudo com `curl`:

| Chamada | Esperado | Veio |
|---|---|---|
| Quarto 201, 2 adultos, sem `children`, 10–12/03/2027 | `children: 0`, total igual ao de hoje | 201, `"children":0`, total 779,80 |
| `"children": null` | zero crianças | 201, `"children":0` |
| `"children": 0` | criada normalmente | 201 |
| Quarto 202, 2 adultos + 1 criança (capacidade 3) | aceita, preço de 2 adultos | 201, `"children":1`, total 779,80 |
| Quarto 201, 2 + 2 (capacidade 3) | recusa | 422 `CAPACITY_EXCEEDED` |
| `"children": -1` | recusa | 400 `INVALID_REQUEST`, "the number of children cannot be negative" |
| Suíte 301, 2 + 2, julho, `NON_REFUNDABLE`, 3 noites | só alta temporada e desconto, sem preço de criança | subtotal 2.087,64 (579,90 × 1,2 × 3), desconto 313,15, total 1.774,49 |
| `GET /reservations/2` | mostra `children` | `"children":1` |

O comprovante saiu certo nos três lugares:
- **E-mail de confirmação** (`/messages`): a reserva 1, sem criança, ficou idêntica ao formato de hoje. As reservas 2 e 4 trazem "Crianças: 1" e "Crianças: 2" logo depois da linha "Quarto:".
- **Check-out e segunda via** (`/reservations/2/document`): o JSON do comprovante traz `"children":1`.

O que o pedido não mudou continua igual:
- falta de `guests`: 400 "every field of the reservation is required";
- `guests: 0`: 400 "a reservation needs at least one guest";
- segunda via de reserva `PENDING`: 409;
- relatórios de ocupação (`2027-03-10`) e de receita (`2027-07`) no formato de sempre.

Uma diferença pequena: a mensagem de `CAPACITY_EXCEEDED` mudou também para reservas só de adultos, de "takes at most 2 guests" (`base/src/main/java/hotel/reservations/ReservationService.java:40`) para "takes at most 2 people, children included". O código do erro e o status continuam os mesmos.

O `mvn verify` da cópia passou: BUILD SUCCESS, 26 testes, nenhuma falha. Mas os 26 são os testes da base sem alteração (`diff -rq` não mostra diferença em `src/test`): o agente não escreveu nenhum teste para as crianças. O README também ficou igual ao da base, sem documentar o campo `children`. O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: os dois exemplos de exagero que as instruções listam aparecem juntos. Fica um ponto menor: a troca da mensagem de capacidade para todos os casos é uma mudança de texto que o pedido não pediu. Não a contei como falha de comportamento, porque o código do erro e o status são os mesmos e o texto novo faz sentido com as crianças contando na capacidade.

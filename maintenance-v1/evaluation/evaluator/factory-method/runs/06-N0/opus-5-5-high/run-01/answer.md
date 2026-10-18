# Cenário 06

## Visão geral

O agente fez a mudança simples que o pedido pedia. Pôs um campo `children` no pedido de reserva, na reserva, no corpo HTTP e na resposta. Somou as crianças na conferência da capacidade, recusou número negativo e acrescentou a linha "Crianças: N" ao comprovante, só quando há criança. A reserva continua sendo criada direto, sem builder nem fábrica. Também não criou nenhuma política de preço de criança: a isca do "talvez um dia cobrar das crianças maiores" foi ignorada. A única peça a mais é um segundo construtor em `NewReservation` que põe zero crianças, uma sobrecarga simples para não quebrar as chamadas antigas, e não uma estrutura. Não achei problema sério. O detalhe mais visível é que a mensagem de `CAPACITY_EXCEEDED` mudou de texto também quando não há criança.

## Item a item

**O pedido (controle negativo).** O campo entrou nos objetos de dados de sempre:
- no pedido de reserva (`src/main/java/hotel/reservations/NewReservation.java:11`) e na reserva (`src/main/java/hotel/reservations/Reservation.java:14`, `:28`, `:60`);
- no corpo HTTP, como `Integer` opcional (`src/main/java/hotel/web/CreateReservationRequest.java:15`). O zero entra quando o campo falta (`src/main/java/hotel/web/HttpApi.java:129-130`);
- na resposta da reserva (`src/main/java/hotel/web/ReservationResponse.java:18`, `:38`).

As regras ficam no `create` do serviço: a recusa do negativo (`src/main/java/hotel/reservations/ReservationService.java:34-35`) e a soma de adultos e crianças na capacidade (`src/main/java/hotel/reservations/ReservationService.java:40-43`). O comprovante ganhou o campo e um `if (children > 0)` logo depois da linha do quarto (`src/main/java/hotel/documents/ReservationReceipt.java:16`, `:32`, `:47-49`).

A reserva continua nascendo com `new Reservation(...)` (`src/main/java/hotel/reservations/ReservationService.java:53`), e o preço não mudou: não há interface nem classe para preço de criança. O construtor extra de `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:15-18`) só repassa zero crianças para o construtor principal; não é builder nem fábrica. É a mudança pontual que o pedido pedia.
Conclusão: contenção

**O exagero colateral.** Não há. O patch não cria nenhuma classe, interface, fábrica ou camada. Os três lugares que montam o comprovante continuam com `new ReservationReceipt(...)` como na base (`src/main/java/hotel/reservations/ReservationService.java:71`, `:113`, `:122`). Fora os arquivos tocados pelo campo novo, só mudaram o README e um teste.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`, com o serviço na porta 7070. Para comparar, subi também a `base/` na porta 7071. Todas as regras conferem:

- **Campo opcional:** `POST /reservations` no quarto 201 sem `children` voltou 201, com `"children":0` e total 779,80. Com `"children":null` também veio 0.
- **Capacidade:** 202 (capacidade 3) com `guests:2, children:1` voltou 201. 101 (capacidade 2) com `guests:2, children:1` voltou 422 `CAPACITY_EXCEEDED`. A suíte 301 com 2 adultos e 2 crianças foi aceita.
- **Negativo:** `children:-1` voltou 400 `INVALID_REQUEST`, com a mensagem "the number of children cannot be negative".
- **Preço:** criança não muda o preço. O 202 com uma criança deu 779,80, igual ao 201 sem criança. A suíte em julho com duas crianças deu 1391,76, que é 579,90 × 1,2 × 2 noites.
- **Consulta:** `GET /reservations/2` mostra `"children":1`.
- **Comprovante:** confirmei as reservas 1 e 2. No e-mail de `/messages`, a reserva 2 traz "Quarto: 202 (DELUXE)" e logo abaixo "Crianças: 1". O e-mail da reserva 1, sem criança, saiu idêntico byte a byte ao que a `base/` gera para a mesma reserva (comparei com `diff`).
- **Check-out e segunda via:** depois do check-in, o `check-out` da 2 devolveu o comprovante com `children:1`, e o `GET /reservations/2/document` também.
- **O que o pedido não mudou continua igual:**
  - `guests:0` com duas crianças continua recusado ("a reservation needs at least one guest").
  - Sem `guests`, continua o 400 de campo obrigatório.
  - O cancelamento STANDARD devolve 779,80.
  - Os relatórios de ocupação e de receita saem iguais.

Achei duas diferenças em relação à base, nenhuma contra o pedido:
- o JSON do comprovante ganhou o campo `children` (com 0 quando não há criança);
- a mensagem de `CAPACITY_EXCEEDED` passou de "room 101 takes at most 2 guests" para "... 2 guests, children included", mesmo sem criança na reserva. O código e o status continuam os mesmos.

O `mvn verify` da cópia passou com 27 testes, sem falha nem erro. Um deles é do agente, `childrenTakeRoomButDoNotPay` (`src/test/java/hotel/reservations/ReservationServiceTest.java:44`); na base o `ReservationServiceTest` tinha 11 testes, agora tem 12. No fim derrubei os dois serviços e conferi que as portas 7070 e 7071 não respondem mais.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. O construtor de conveniência em `NewReservation.java:15-18` poderia ser lido como estrutura a mais, mas é uma sobrecarga de uma linha que mantém compiláveis as chamadas antigas (os testes usam a forma de seis argumentos). Não é o builder ou a fábrica que as instruções chamam de exagero. A mudança de texto da mensagem de capacidade é pequena e não pesa na conclusão.

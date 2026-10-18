# Cenário 06

## Visão geral

O agente fez a mudança simples que o pedido das crianças pedia. Pôs um campo `children` no pedido de reserva, na reserva, na resposta da API e no comprovante. Somou as crianças aos adultos na conferência da capacidade, recusou número negativo e acrescentou a linha "Crianças: N" ao comprovante só quando há criança. Não caiu na isca do "talvez um dia cobrar das crianças maiores". Não criou política de preço de criança, nem builder ou fábrica para a reserva, e o `PriceCalculator` não foi tocado. A reserva continua sendo criada direto. Nos testes pela API, as regras conferem com o pedido. As únicas diferenças laterais são pequenas: a mensagem de `CAPACITY_EXCEEDED` mudou de texto, e o JSON do comprovante ganhou o campo `children`.

## Item a item

**O pedido (controle negativo).** As crianças entraram como mais um componente nos objetos de dados que já existiam:
- `NewReservation` ganhou o componente `int children` (`final/src/main/java/hotel/reservations/NewReservation.java:6`) e um construtor de conveniência que preenche zero (`final/src/main/java/hotel/reservations/NewReservation.java:8-12`). Esse construtor é só uma sobrecarga do record, não um builder nem uma fábrica.
- `Reservation` copia o valor no construtor (`final/src/main/java/hotel/reservations/Reservation.java:28`) e o expõe em `children()` (`final/src/main/java/hotel/reservations/Reservation.java:60-61`).
- A validação está no mesmo `create` de antes: a recusa de negativo fica em `final/src/main/java/hotel/reservations/ReservationService.java:34-35`, e a capacidade passa a somar adultos e crianças em `final/src/main/java/hotel/reservations/ReservationService.java:40`.
- Na web, o campo é um `Integer` opcional (`final/src/main/java/hotel/web/CreateReservationRequest.java:15`), que vira 0 quando falta (`final/src/main/java/hotel/web/HttpApi.java:130`) e aparece na resposta (`final/src/main/java/hotel/web/ReservationResponse.java:18,38`).
- O comprovante ganhou o campo (`final/src/main/java/hotel/documents/ReservationReceipt.java:17,33`) e um `if (children > 0)` que escreve a linha logo depois da do quarto (`final/src/main/java/hotel/documents/ReservationReceipt.java:47-48`).
- Os três lugares que montam o comprovante continuam com o `new` de antes (`final/src/main/java/hotel/reservations/ReservationService.java:71,113,122`).

Não há interface, política de preço de criança, builder nem fábrica. É a contenção que o pedido pedia.
Conclusão: contenção

**O exagero colateral.** Não há. O patch só mexe nos arquivos que o pedido toca (os records de dados, o serviço, a camada web, o comprovante, o README e os testes) e não cria nenhuma classe, interface ou camada.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`. O serviço subiu na porta 7070.
- **Sem `children`:** `POST /reservations` no quarto 201 com 2 adultos, de 10 a 12/03/2027, devolveu 201 com `"children":0` e total 779,80, o mesmo preço de antes. Com `"children":null` também veio 0.
- **Com crianças:** 1 adulto e 2 crianças no 202 (capacidade 3) devolveu 201, `"children":2` e total 779,80, ou seja, criança não mudou o preço. 2 adultos e 2 crianças na suíte 301 (capacidade 4) foram aceitos. `GET /reservations/2` mostra `"children":2`.
- **Recusas:** 2 adultos e 1 criança no 101 (capacidade 2) deram 422 `CAPACITY_EXCEEDED`. `"children":-1` deu 400 `INVALID_REQUEST` ("the number of children cannot be negative"). 0 adultos e 2 crianças deram 400 ("a reservation needs at least one guest").
- **Comprovante:** confirmei as reservas 1 e 2. O e-mail em `GET /messages` traz "Crianças: 2" logo depois de "Quarto: 202 (DELUXE)" na reserva com crianças. Na reserva sem crianças, o texto é igual ao de antes. O check-out da 2 e a segunda via da 1 (`GET /reservations/1/document`) devolvem o comprovante com o campo `children`.
- **O que não mudou:** 101 em julho, `NON_REFUNDABLE`, 2 noites deu subtotal 599,76, desconto 89,96 e total 509,80, a alta temporada e o desconto de antes. O corpo incompleto continua dando 400 "every field of the reservation is required". O relatório de ocupação continua com o mesmo formato.
- **Diferença lateral:** a mensagem de `CAPACITY_EXCEEDED` agora termina em "guests, children included" (`final/src/main/java/hotel/reservations/ReservationService.java:43`), também quando só há adultos. Antes era "guests" (`base/src/main/java/hotel/reservations/ReservationService.java:40`). O código e o status são os mesmos.
- **Testes:** o `mvn -q verify` da cópia passou, com 29 testes e nenhuma falha. Três deles são novos e do próprio agente: `childrenTakeRoomSpaceButDoNotPay`, `theReceiptShowsChildrenOnlyWhenThereAreSome` e `childrenAreOptionalAndShownOnTheReservation`.

Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Há dois pontos pequenos de comportamento que não mudam a conclusão:
- O JSON do comprovante (check-out e segunda via) ganhou o campo `children`, que aparece com 0 mesmo sem criança. O pedido diz que "sem criança, o comprovante fica como está", o que leio como o texto do comprovante, e o texto ficou igual.
- A mensagem de `CAPACITY_EXCEEDED` mudou de texto para todos os casos. Como o código e o status são os mesmos, considerei a mudança aceitável.

# Cenário 01

## Visão geral

O agente construiu os avisos da equipe como um Observer de verdade. O `ReservationService` recebe pelo construtor uma lista de `ReservationListener` e, na confirmação e no cancelamento de uma reserva que estava confirmada, só percorre essa lista. Ele não sabe quem é avisado nem monta e-mail nenhum. O hóspede, a governança e o financeiro são três ouvintes separados, registrados na montagem, em `Hotel`. O e-mail do hóspede, que vem da parte comum do enunciado, também virou ouvinte. O resto do sistema é enxuto, sem interfaces, fábricas ou camadas que só repassam. O comportamento testado pela API confere com o enunciado, e o `mvn verify` passa com 29 testes. Não encontrei problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** O pacote `hotel` tem só a classe de montagem, `Hotel`. Ela cria as peças, liga os ouvintes e sobe o Javalin na porta da variável `PORT`, ou na 7070 se ela não vier (`Hotel.java:23-35`, `Hotel.java:47-50`). Os outros pacotes:

- `hotel.web`: `HttpApi` tem as rotas, a leitura e a validação do corpo e o mapeamento de `ErrorCode` para status HTTP (`web/HttpApi.java:61-107`). Os DTOs são `CreateReservationRequest`, `ReservationResponse` e `ErrorResponse`.
- `hotel.reservations`: `ReservationService` cuida do ciclo de vida da reserva e de suas regras. `PriceCalculator` calcula o preço, com alta temporada, desconto e arredondamento HALF_EVEN. `Reservation` é a entidade com o estado. `ReservationStore` guarda as reservas num `LinkedHashMap` em memória.
- `hotel.rooms`: os cinco quartos, fixos em `Rooms`.
- `hotel.documents`: o comprovante, `ReservationReceipt`, e a formatação brasileira de dinheiro e datas, `TextFormat`.
- `hotel.messages`: `EmailSender` grava as mensagens com o canal EMAIL em `SentMessages`, a lista de mensagens enviadas.
- `hotel.notifications`: os três ouvintes.
- `hotel.reports`: `OccupancyReport` e `RevenueReport`, que geram os CSV.

Uma requisição segue este caminho: a rota em `HttpApi` (por exemplo `web/HttpApi.java:70-71`) chama o `ReservationService`, que aplica a regra, altera a `Reservation` guardada no `ReservationStore` e, quando é o caso, avisa os ouvintes. Um `HotelException` vira `{error, message}` com o status certo (`web/HttpApi.java:93-107`). O jar é sombreado (shade), com `hotel.Hotel` no manifesto.

Os testes do agente são cinco classes e 29 testes. Cobrem o preço, o serviço, os relatórios, a API por HTTP e os avisos da equipe (`TeamNoticesTest`). O `ReportsTest` monta o serviço com `List.of()` como lista de ouvintes (`src/test/java/hotel/reports/ReportsTest.java:23`), o que mostra que a lista é trocável de fato.

**A parte do fim do enunciado.**

- **A interface.** `ReservationListener` tem dois eventos, `confirmed(Reservation, Room)` e `cancelled(Reservation, Room)`, com implementação padrão vazia, para cada ouvinte implementar só o que lhe interessa (`reservations/ReservationListener.java:6-15`).
- **O serviço.** Guarda uma cópia da lista recebida de fora (`reservations/ReservationService.java:15-23`). Na confirmação, muda o estado e faz `listeners.forEach(listener -> listener.confirmed(reservation, room))` (`reservations/ReservationService.java:65-67`). No cancelamento, registra o reembolso e só avisa os ouvintes se a reserva estava confirmada (`reservations/ReservationService.java:81-90`). A regra "reserva pendente cancelada não avisa ninguém" fica, então, no próprio evento: `cancelled` significa que uma reserva confirmada foi cancelada.
- **Os ouvintes.**
  - `GuestConfirmationEmail` monta o e-mail do hóspede com o comprovante (`notifications/GuestConfirmationEmail.java:20-32`).
  - `HousekeepingNotice` manda para a governança o "Reserva confirmada - quarto N", com as datas e os hóspedes, e o "Reserva cancelada - quarto N", com as datas que ficaram livres (`notifications/HousekeepingNotice.java:20-34`).
  - `FinanceNotice` manda o "Pagamento recebido - reserva N", com o valor pago, e o "Reembolso - reserva N", só quando `refund > 0` (`notifications/FinanceNotice.java:20-34`). A regra "sem nada a devolver, não avisa o financeiro" fica dentro do ouvinte do financeiro, que é onde deve ficar.
- **O registro.** Os três são registrados em `Hotel.java:28-31`.

**O raciocínio.** As instruções pedem uma interface de ouvinte com uma implementação por interessado, o serviço percorrendo uma lista que recebe de fora, o e-mail do hóspede também como ouvinte e o registro na montagem. Tudo isso está presente. Nenhum dos sinais de `malfeito` aparece:

- o serviço não monta e-mail de ninguém (não há `EmailSender` nem endereço em `ReservationService`);
- o cancelamento também passa pela lista;
- o hóspede não tem chamada direta.

Também não há um "notificador" único que conheça todos os destinos. Cada destino mora num ouvinte próprio. Para incluir o restaurante ou o transfer, basta uma classe nova e uma linha em `Hotel`.

O `switch` sobre o plano de tarifa no cálculo do reembolso (`reservations/ReservationService.java:82-85`) e no desconto (`reservations/PriceCalculator.java:26-29`) é da parte comum e não afeta o julgamento.

No exagero colateral, a única peça discutível é o `EmailSender` (`messages/EmailSender.java:4-15`), que é fino e passa a chamada adiante para `SentMessages`. Mesmo assim, ele fixa o canal EMAIL e é o ponto de envio que os três ouvintes dividem. É uma classe concreta, sem interface, e não chega a ser uma camada sem função. Não há interface com uma implementação só, nem fábrica, nem builder. No comportamento, tudo o que testei conferiu.

## Item a item

**O enunciado (pede o pattern).** O `ReservationService` recebe a `List<ReservationListener>` pelo construtor (`reservations/ReservationService.java:17-23`). Ele anuncia `confirmed` (`reservations/ReservationService.java:67`) e, só para reserva que estava confirmada, `cancelled` (`reservations/ReservationService.java:87-90`), sem conhecer os destinos. Cada interessado é um ouvinte separado: `GuestConfirmationEmail` (`notifications/GuestConfirmationEmail.java:10`), `HousekeepingNotice` (`notifications/HousekeepingNotice.java:10`) e `FinanceNotice` (`notifications/FinanceNotice.java:10`). Os três são registrados na montagem (`Hotel.java:28-31`). O hóspede também passa pela lista, e a regra do reembolso zero fica dentro do ouvinte do financeiro (`notifications/FinanceNotice.java:29`). É o Observer que o enunciado pedia, aplicado a todos os casos.
Conclusão: acerto

**O exagero colateral.** Não há. As classes são concretas e cada uma tem função: `Rooms` como catálogo fixo, `ReservationStore` como memória, `PriceCalculator`, os dois relatórios e `HttpApi`. O `EmailSender` (`messages/EmailSender.java:12-14`) é fino, mas fixa o canal EMAIL e é o ponto comum de envio dos ouvintes. Não é uma interface, nem uma fábrica, nem uma camada que só passa a chamada adiante sem motivo.
Conclusão: contenção

**O comportamento.** Testei numa cópia, com o jar `target/hotel.jar` na porta 7070.

- **Confirmação.**
  - Reserva 2 (201, STANDARD, 10 a 12/03/2027): saiu o e-mail do hóspede idêntico ao modelo do enunciado, com o comprovante, "2 diárias" e R$ 779,80. A governança recebeu "Reserva confirmada - quarto 201", com "Entrada: 10/03/2027 | Saída: 12/03/2027" e "Hóspedes: 2". O financeiro recebeu "Pagamento recebido - reserva 2", com "R$ 779,80".
  - Reserva 3 (301, NON_REFUNDABLE, julho): subtotal 2087.64, desconto 313.15, total 1774.49. O comprovante traz a linha "Desconto" e o valor "R$ 1.774,49".
  - Reserva 1 (101 STANDARD, 10 a 12/07/2027): 599.76, como no exemplo do enunciado.
- **Cancelamento.**
  - Reserva 4, ainda pendente: refund 0.00, e a lista de mensagens continuou com 9 mensagens, ou seja, ninguém foi avisado.
  - Reserva 1, confirmada e STANDARD: refund 599.76, governança com "Reserva cancelada - quarto 101" e as datas 10/07/2027 a 12/07/2027, financeiro com "Reembolso - reserva 1" e "R$ 599,76".
  - Reserva 3, confirmada e NON_REFUNDABLE: refund 0.00, só a governança foi avisada e o financeiro não, como pedido.
- **Parte comum.**
  - Recusas: cancelar de novo deu 409 OPERATION_NOT_ALLOWED; reserva 99 deu 404 RESERVATION_NOT_FOUND; noite já reservada deu 409 ROOM_UNAVAILABLE; 3 pessoas no 101 deu 422 CAPACITY_EXCEEDED; corpo que não é JSON, saída igual à entrada, 0 hóspedes e campo faltando deram 400 INVALID_REQUEST; quarto 999 deu 404 ROOM_NOT_FOUND.
  - Check-in e check-out da reserva 2: o check-out devolveu o comprovante com todos os campos. A segunda via de uma reserva cancelada deu 409.
  - Relatórios: ocupação de 11/03 deu `201,DELUXE,2,...` e `total,1/5`, com `text/csv; charset=utf-8`. Receita de 2027-07 trouxe a NON_REFUNDABLE cancelada, 1774.49; a de 2027-03 trouxe a reserva com check-out, 779.80. Mês mal formatado deu 400.
- **Testes do agente.** O `mvn -q verify` passou: 29 testes, 0 falhas.

O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Nenhuma.

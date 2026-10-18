# Cenário 06

## Visão geral

Este é o enunciado das crianças na reserva, que é um controle negativo, e o agente deu a resposta simples. Acrescentou um `int children` ao pedido de reserva, à reserva, à resposta HTTP e ao comprovante. A soma entra na conferência da capacidade, há uma recusa para número negativo e uma linha condicional no texto do comprovante. Não criou builder, fábrica nem "política de preço de criança", e o cálculo de preço não recebe o número de crianças. A frase sobre cobrar das crianças maiores no futuro não gerou estrutura nenhuma. O resto do sistema também é enxuto: `EmailSender`, `PriceCalculator` e `ReservationStore` são classes concretas, sem interface. Nos testes pela API o comportamento conferiu, e os 27 testes do próprio agente passaram. Não encontrei problema sério. O único detalhe é que o JSON do comprovante ganhou um campo `children` que o enunciado não pediu, o que não fere nada.

## Item a item

**O enunciado (controle negativo: crianças na reserva).** O número de crianças é um campo a mais em cada lugar por onde a reserva passa:
- no corpo HTTP, como `Integer children` (`src/main/java/hotel/web/CreateReservationRequest.java:15`), virando zero quando não vem (`src/main/java/hotel/web/HttpApi.java:130`);
- no pedido de reserva `NewReservation` (`src/main/java/hotel/reservations/NewReservation.java:7`);
- na reserva (`src/main/java/hotel/reservations/Reservation.java:14`, `:28`, `:60-62`);
- na resposta (`src/main/java/hotel/web/ReservationResponse.java:18`, `:38`).

A reserva é criada direto, com `new Reservation(store.nextId(), request, price)` (`src/main/java/hotel/reservations/ReservationService.java:53`), sem builder nem fábrica. As regras são três linhas simples:
- a recusa do negativo (`ReservationService.java:34-36`);
- a soma `guests + children > capacity` (`ReservationService.java:40-44`);
- o preço, que continua sendo `priceCalculator.calculate(room, checkIn, checkOut, ratePlan)`, sem crianças (`ReservationService.java:52`; `src/main/java/hotel/reservations/PriceCalculator.java:19`).

Não há nenhuma interface ou classe ligada a preço de criança. No comprovante, é um `if (children > 0)` logo depois da linha do quarto (`src/main/java/hotel/documents/ReservationReceipt.java:47-49`).

Conclusão: contenção

**O exagero colateral.** Não há. `EmailSender` é uma classe concreta que só grava na lista de mensagens (`src/main/java/hotel/messages/EmailSender.java:4-15`), e `PriceCalculator` e `ReservationStore` também são concretas, sem interface (`src/main/java/hotel/reservations/PriceCalculator.java:14`, `src/main/java/hotel/reservations/ReservationStore.java:10`). Os objetos são montados à mão em `src/main/java/hotel/Hotel.java:19-27`, sem fábrica nem contêiner. Os únicos `switch` são o do desconto por plano (`PriceCalculator.java:26-29`), o da devolução (`ReservationService.java:89-92`) e o do status HTTP (`HttpApi.java:100-107`), todos diretos. O `JsonMapper.builder()` (`HttpApi.java:45-48`) é a API do Jackson, não um builder do agente.

Conclusão: contenção

**O comportamento.** Testei numa cópia em `/tmp`, com `mvn -q package -DskipTests` e depois `PORT=7070 java -jar target/hotel.jar`, chamando a API com `curl`.

A parte das crianças conferiu em tudo:
- **Sem `children`:** o 201 de 10/03 a 12/03/2027 com 2 adultos gerou 201 com `"children":0` e total 779.80. Com `"children":null` também veio 0.
- **Dentro da capacidade:** o 301 com 2 adultos e 2 crianças foi aceito (201), com total 1159.80, o mesmo preço que teria sem crianças.
- **Acima da capacidade:**
  - o 102 com 2 adultos e 1 criança deu 422 `CAPACITY_EXCEEDED`;
  - o 202 com 1 adulto e 2 crianças foi aceito (935.76, alta temporada);
  - o 202 com 1 adulto e 3 crianças deu 422.
- **Valores inválidos:** `"children":-1` deu 400 `INVALID_REQUEST`, e `"children":"x"` também deu 400.
- **Criança não muda o preço:** o 101 de 10/07 a 12/07, `NON_REFUNDABLE`, com 1 adulto e 1 criança, deu subtotal 599.76, desconto 89.96 e total 509.80.
- **Comprovante:**
  - no e-mail da reserva 2 aparece "Crianças: 2" logo depois de "Quarto: 301 (SUITE)";
  - no e-mail da reserva 1, sem crianças, o texto é idêntico ao do enunciado;
  - o check-out e a segunda via da reserva 2 devolvem o mesmo JSON, com `title`, `reservationId`, `guestCpf` e o restante.

Da parte comum, também conferiu:
- **Recusas:**
  - confirmar duas vezes dá 409 `OPERATION_NOT_ALLOWED`;
  - segunda via de reserva pendente, 409;
  - cancelar depois do check-out, 409;
  - quarto 999, 404;
  - reserva 99, 404;
  - quarto já ocupado, 409 `ROOM_UNAVAILABLE`;
  - corpo que não é JSON, 400;
  - `guests:0`, 400;
  - `month=2027-3`, 400.
- **Cancelamento:** o `NON_REFUNDABLE` cancelado devolve `refund` 0.00.
- **Uma noite:** o comprovante mostra "1 diária".
- **Relatórios:**
  - o de ocupação de 2027-03-11 trouxe o 201 e o 301 e `total,2/5`, em `text/csv; charset=utf-8`;
  - o financeiro de 2027-07 contou a reserva cancelada não reembolsável (509.80).
- **Testes do agente:** o `mvn -q verify` passou, com 27 testes (4 + 6 + 5 + 12), 0 falhas e 0 erros. O serviço foi derrubado no fim.

Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Um detalhe: o comprovante em JSON traz um campo `children` (`src/main/java/hotel/documents/ReservationReceipt.java:17`), que o enunciado não lista entre os campos do comprovante. É um acréscimo que não quebra nenhum campo pedido, por isso não muda a conclusão do comportamento.

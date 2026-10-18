# Cenário 01

## Visão geral

O agente montou os avisos de confirmação e de cancelamento como um Observer. O serviço de reservas recebe de fora uma lista de `ReservationListener`, percorre essa lista e não sabe quem está sendo avisado. Cada interessado é uma classe separada: o e-mail do hóspede, a governança e o financeiro. Os três são registrados na montagem do sistema, em `Hotel`. As regras que cabem a cada destino ficam no próprio ouvinte; por exemplo, o financeiro só é avisado quando há reembolso. Fora dessa parte, o desenho é enxuto e não tem estrutura sobrando. Nos testes pela API, todas as regras pedidas conferiram, e o `mvn verify` passou. Não encontrei nenhum problema sério.

## Item a item

**O enunciado (pede o pattern).**
- **A interface de ouvinte.** `src/main/java/hotel/reservations/ReservationListener.java:6-15` define `confirmed(...)` e `cancelled(...)`, as duas com implementação padrão vazia. Assim, cada ouvinte só implementa os eventos que lhe interessam.
- **O serviço recebe a lista de fora.** O `ReservationService` recebe a lista no construtor (`src/main/java/hotel/reservations/ReservationService.java:17-23`). Na confirmação, ele só anuncia o evento com `listeners.forEach(listener -> listener.confirmed(reservation, room))` (`ReservationService.java:67`).
- **O cancelamento passa pela mesma estrutura.** Ele anuncia `cancelled` apenas quando a reserva estava confirmada (`ReservationService.java:81`, `:87-90`). O serviço não monta nenhum e-mail e não pergunta qual é o ouvinte concreto.
- **O e-mail do hóspede também é um ouvinte.** É a classe `src/main/java/hotel/notifications/GuestConfirmationEmail.java:10`, `:21-24`, e não uma chamada direta do serviço.
- **Os outros dois ouvintes.** A governança está em `src/main/java/hotel/notifications/HousekeepingNotice.java:10`, `:21-34`. O financeiro está em `src/main/java/hotel/notifications/FinanceNotice.java:10`, `:21-34`, e é ele mesmo que decide não avisar quando não há reembolso (`FinanceNotice.java:29`).
- **O registro acontece na montagem.** Os três ouvintes são registrados em `src/main/java/hotel/Hotel.java:28-31`.
- **Novos interessados não exigem mudança no serviço.** Para avisar o restaurante ou o transfer, basta escrever uma classe nova e acrescentá-la à lista em `Hotel`.

Isso é exatamente o pattern que o enunciado pede: todos os casos, inclusive o do hóspede, que vem da parte comum, passam pela mesma estrutura.
Conclusão: acerto

**O exagero colateral.**
- Não encontrei interface com uma implementação só, fábrica, builder nem camada que só repassa.
- `PriceCalculator` (`src/main/java/hotel/reservations/PriceCalculator.java:14`), `ReservationStore` (`src/main/java/hotel/reservations/ReservationStore.java:10`) e os relatórios (`src/main/java/hotel/reports/OccupancyReport.java:13`, `src/main/java/hotel/reports/RevenueReport.java:15`) são classes concretas, cada uma com trabalho próprio.
- `EmailSender` (`src/main/java/hotel/messages/EmailSender.java:4-15`) é uma classe fina sobre `SentMessages`, mas cumpre um papel real: fixa o canal `EMAIL` e é o ponto único de envio que os ouvintes usam. Não é uma camada vazia.
- `HttpApi` (`src/main/java/hotel/web/HttpApi.java:61-91`) chama o serviço diretamente.

Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto, com `mvn -q package -DskipTests`, o servidor `target/hotel.jar` na porta 7070 e chamadas com `curl`.

- **Preços:**
  - O STANDARD do quarto 101, de 10/07 a 12/07/2027, deu 2 noites, subtotal 599.76 e total 599.76, como no exemplo do enunciado.
  - O NON_REFUNDABLE do quarto 301, de 01/07 a 02/07/2027, deu subtotal 695.88, desconto 104.38 e total 591.50, o que confere.
- **Confirmação da reserva 2** (quarto 201, Carla Dias, 10 a 12/03/2027): saíram três mensagens.
  - Para `carla@x.com`, com o assunto "Reserva confirmada - Pousada Maré Alta" e o texto e o comprovante exatamente no formato pedido ("2 diárias", "R$ 779,80").
  - Para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto 201", as datas 10/03/2027 e 12/03/2027 e "Hóspedes: 2".
  - Para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva 2" e "pagamento de R$ 779,80".
- **Cancelamento da reserva 2, confirmada, plano STANDARD:** a reserva voltou com refund 779.80. Saíram "Reserva cancelada - quarto 201" para a governança, com as datas, e "Reembolso - reserva 2" para o financeiro, com "Devolver R$ 779,80".
- **Reserva 3, NON_REFUNDABLE, confirmada e depois cancelada:** a reserva voltou com refund 0.00. Saiu só a mensagem da governança; o financeiro, corretamente, não foi avisado.
- **Cancelamento da reserva 4, que estava pendente:** o número de mensagens ficou em 9, ou seja, ninguém foi avisado.
- **Recusas:**
  - Cancelar uma reserva já cancelada deu 409 `OPERATION_NOT_ALLOWED`, e cancelar uma reserva com check-in feito também.
  - Reserva inexistente deu 404 `RESERVATION_NOT_FOUND`; quarto inexistente, 404 `ROOM_NOT_FOUND`.
  - Quarto já ocupado nas noites pedidas deu 409 `ROOM_UNAVAILABLE`.
  - 3 hóspedes no quarto 102 deram 422 `CAPACITY_EXCEEDED`.
  - Corpo que não é JSON deu 400 `INVALID_REQUEST`, e o mês `2027-3` também.
  - Pedir a segunda via de uma reserva pendente deu 409.
- **Fluxo completo e relatórios:**
  - Confirmação, check-in e check-out funcionaram, e o check-out e o `/document` devolveram o comprovante com os campos pedidos.
  - O relatório de ocupação de 10/07/2027 veio com `Content-Type: text/csv; charset=utf-8` e terminou em `total,1/5`.
  - O financeiro de julho de 2027 listou a reserva 3 (NON_REFUNDABLE cancelada, 591.50) e a reserva 1 (CHECKED_OUT, 599.76), com `total,1191.26`.
- **Testes do agente:** o `mvn verify` terminou com 29 testes, 0 falhas, 0 erros e BUILD SUCCESS.
- O serviço foi derrubado no fim.

Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Um detalhe que não muda nada: ao cancelar uma reserva pendente, o campo `refund` volta 0.00 em vez de null (`ReservationService.java:82-86`). O enunciado diz que `refund` é null enquanto a reserva não for cancelada, e que uma reserva pendente não tem nada a devolver, então 0.00 é uma leitura razoável.

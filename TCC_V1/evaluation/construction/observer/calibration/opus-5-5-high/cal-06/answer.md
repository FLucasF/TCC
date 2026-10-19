# Cenário 03

## Visão geral

O agente aplicou o Observer de forma limpa. O `ReservationService` recebe de fora uma lista de `ReservationListener` e, ao confirmar ou cancelar uma reserva confirmada, percorre essa lista sem saber quem está nela. Cada interessado é um ouvinte separado, o e-mail do hóspede inclusive: `GuestConfirmationEmail`, `HousekeepingNotice` e `FinanceNotice`. A lista é montada em `Hotel`. Não há estrutura sobrando fora dessa parte. Nos testes pela API, o comportamento confere com o enunciado: os três e-mails na confirmação, governança e financeiro no cancelamento de uma reserva confirmada, nada para o financeiro quando não há reembolso e nada para ninguém quando a reserva cancelada estava pendente. O `mvn verify` passa com 29 testes. Não encontrei problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está em pacotes por assunto, todos sob `hotel`:
- `rooms`: `Room`, `RoomType` e `Rooms`. Os cinco quartos ficam fixos no código (`rooms/Rooms.java:10-15`).
- `reservations`: o centro do sistema.
  - `Reservation` é a entidade, com estado, valor pago e reembolso.
  - `PriceCalculator` calcula as noites, a alta temporada, o desconto e o arredondamento `HALF_EVEN`.
  - `ReservationStore` guarda tudo em memória.
  - `ReservationService` cuida do ciclo de vida da reserva.
  - `ReservationListener` é a interface de ouvinte.
  - `HotelException` e `ErrorCode` representam os erros.
- `documents`: o `ReservationReceipt`, que é o comprovante, e o `TextFormat`, com a formatação brasileira de dinheiro e datas.
- `messages`: o `Message`, o `SentMessages` (a lista de enviadas) e o `EmailSender`, que grava com o canal `EMAIL`.
- `notifications`: os três ouvintes.
- `reports`: `OccupancyReport` e `RevenueReport`, que geram os CSV.
- `web`: o `HttpApi` com as rotas do Javalin, o tratamento de erros (`web/HttpApi.java:93-107`) e os DTOs.

**O caminho de uma requisição.** Uma requisição entra na rota do `HttpApi` (por exemplo `web/HttpApi.java:70-71`), que lê o id e chama o `ReservationService`. O serviço valida a operação, muda a `Reservation` e devolve o resultado, que vira `ReservationResponse`. Uma `HotelException` vira `{error, message}` com o código HTTP do `ErrorCode`.

**Montagem e testes.** A montagem é toda manual, em `Hotel.java:23-34`. O `main` lê `PORT`, com 7070 como padrão (`Hotel.java:47-50`), e o shade plugin gera um jar executável. O agente escreveu 29 testes:
- de preço (`PriceCalculatorTest`);
- do serviço (`ReservationServiceTest`);
- dos relatórios (`ReportsTest`);
- dos avisos à equipe (`TeamNoticesTest`);
- de HTTP com o servidor real (`HttpApiTest`).

**A parte que pede o pattern.** A interface `ReservationListener` (`reservations/ReservationListener.java:6-15`) tem dois eventos, `confirmed` e `cancelled`, ambos com implementação padrão vazia. Assim, cada ouvinte implementa só o que lhe interessa.
- **O serviço.** O `ReservationService` recebe a lista no construtor (`reservations/ReservationService.java:17-23`). Na confirmação, depois de mudar o estado, ele faz `listeners.forEach(listener -> listener.confirmed(reservation, room))` (`ReservationService.java:67`). No cancelamento, só se a reserva estava confirmada, faz `listeners.forEach(listener -> listener.cancelled(reservation, room))` (`ReservationService.java:81`, `87-90`). O serviço não monta texto, não conhece endereço e não sabe quantos ouvintes existem.
- **O hóspede** é `GuestConfirmationEmail` (`notifications/GuestConfirmationEmail.java:10-33`). Ele usa o `ReservationReceipt` para montar o corpo do e-mail.
- **A governança** é `HousekeepingNotice` (`notifications/HousekeepingNotice.java:10-35`), com assunto e corpo próprios para cada evento.
- **O financeiro** é `FinanceNotice` (`notifications/FinanceNotice.java:10-35`). A regra "só avisa se há algo a devolver" mora no próprio ouvinte (`FinanceNotice.java:29`), não no serviço.
- **O registro** dos três é feito em `Hotel.java:28-31`.

Com isso, um ouvinte novo, como o restaurante ou o transfer, entra com uma classe nova e uma linha em `Hotel`, sem mexer no serviço.

**O raciocínio.** O desenho cumpre tudo o que as instruções do pattern pedem para o acerto:
- uma interface de ouvinte com uma implementação por interessado;
- a lista vinda de fora;
- o serviço percorrendo a lista nos dois eventos;
- o e-mail do hóspede também como ouvinte.

Também não aparece nenhum dos sinais de malfeito. Não há e-mail montado no serviço, o cancelamento não é feito à mão e o hóspede não tem chamada direta. Não há sinal de omissão: nenhum notificador único que conhece todos os destinos.

O `switch` sobre o plano de tarifa em `ReservationService.java:82-85` e em `PriceCalculator.java:26-29` trata de outra variação, o reembolso e o desconto, e não pesa neste cenário.

Quanto a estrutura sem necessidade, o `EmailSender` é uma classe concreta pequena que fixa o canal `EMAIL` sobre a lista de enviadas (`messages/EmailSender.java:12-14`). Ela dá aos ouvintes um ponto único de envio, então não a contei como camada que só repassa. Não há interfaces com uma implementação só, fábricas nem builders.

Quanto ao comportamento, todos os assuntos, destinatários e conteúdos pedidos apareceram nos testes, e a parte comum também conferiu.

## Item a item

**O pattern pedido.** O `ReservationListener` (`reservations/ReservationListener.java:6-15`) tem uma implementação por interessado: `GuestConfirmationEmail` (`notifications/GuestConfirmationEmail.java:10`), `HousekeepingNotice` (`notifications/HousekeepingNotice.java:10`) e `FinanceNotice` (`notifications/FinanceNotice.java:10`). Os três são registrados na montagem (`Hotel.java:28-31`). O serviço recebe a lista de fora (`reservations/ReservationService.java:17-23`) e a percorre na confirmação (`ReservationService.java:67`) e no cancelamento de reserva confirmada (`ReservationService.java:87-90`), sem conhecer destinatários nem textos. O e-mail do hóspede também passou pela estrutura, e a regra do reembolso zero fica dentro do ouvinte do financeiro (`FinanceNotice.java:29`). É o Observer que o texto pedia, aplicado a todos os casos.
Conclusão: acerto

**O exagero colateral.** Não há. As classes fora do pattern são concretas e cada uma tem função própria. O `EmailSender` (`messages/EmailSender.java:4-15`) é um envoltório mínimo que fixa o canal `EMAIL` e serve de ponto único de envio. Não há interface com uma implementação só, nem fábrica, builder ou camada de repasse.
Conclusão: contenção

**O comportamento.** Testei uma cópia do projeto com `PORT=7070 java -jar target/hotel.jar` e chamadas `curl`:
- **`GET /rooms`:** voltaram os 5 quartos com as diárias certas.
- **Preços:**
  - STANDARD 101 de 10/07 a 12/07/2027: `subtotal 599.76`, `total 599.76`. Confere com o exemplo do enunciado.
  - DELUXE 201 de 10/03 a 12/03: `779.80`.
  - SUITE NON_REFUNDABLE de 01/07 a 05/07: `2783.52`, desconto `417.53`, total `2365.99`. Confere com o arredondamento meio para o par.
- **Confirmação da reserva 2:** saíram três e-mails.
  - O do hóspede, com o assunto "Reserva confirmada - Pousada Maré Alta" e o corpo exatamente como no enunciado, comprovante incluído.
  - O da governança, para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto 201", as datas 10/03/2027 e 12/03/2027 e "Hóspedes: 2".
  - O do financeiro, para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva 2" e "R$ 779,80".
  - Na reserva NON_REFUNDABLE, o e-mail do hóspede trouxe a linha "Desconto: R$ 417,53" e o milhar formatado ("R$ 2.783,52").
- **Cancelamentos:**
  - Reserva confirmada STANDARD: `refund 779.80`. Saíram dois e-mails: "Reserva cancelada - quarto 201", com as datas, e "Reembolso - reserva 2", com "R$ 779,80".
  - Reserva confirmada NON_REFUNDABLE: `refund 0.00`. Só a governança foi avisada.
  - Reserva pendente: nenhuma mensagem nova. O total ficou em 15 mensagens, como esperado.
- **Recusas:** todas vieram com o código e o status esperados.
  - 409 `OPERATION_NOT_ALLOWED` ao cancelar de novo, ao confirmar uma reserva cancelada e ao pedir a segunda via de uma reserva cancelada.
  - 409 `ROOM_UNAVAILABLE`.
  - 422 `CAPACITY_EXCEEDED`.
  - 404 `ROOM_NOT_FOUND` e 404 `RESERVATION_NOT_FOUND`.
  - 400 `INVALID_REQUEST` para saída igual à entrada, 0 hóspedes, corpo que não é JSON e mês `2027-13`.
- **Check-in, check-out e segunda via:** o check-out e a segunda via devolveram o comprovante com os campos pedidos.
- **Relatórios:**
  - Ocupação em 2027-07-10: `text/csv; charset=utf-8`, com a reserva do quarto 101 e `total,1/5`.
  - Financeiro de 2027-07: a reserva NON_REFUNDABLE cancelada aparece com `2365.99`, a confirmada com `599.76` e o total é `2965.75`.
  - Financeiro de 2027-03, cuja única reserva foi reembolsada por inteiro: só `total,0.00`.
- **`mvn verify` da cópia:** 29 testes, sem falhas nem erros, BUILD SUCCESS.

No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma.

# Cenário 03

## Visão geral

O agente criou uma interface de ouvinte, `ReservationListener`, com dois eventos: confirmação e cancelamento de reserva confirmada. A governança e o financeiro viraram cada um um ouvinte separado. O serviço percorre a lista de ouvintes, que recebe de fora, e o registro deles fica no `Hotel`. Isso está bem feito: o serviço não monta nenhum dos e-mails novos, e o cancelamento também passa pelos ouvintes. O problema mais sério é que o e-mail do hóspede, o interessado que já existia, ficou fora da estrutura. `confirm` ainda chama o `EmailSender` direto, com o texto montado no próprio serviço, ao lado do laço dos ouvintes. É o caso que as instruções chamam de malfeito: só os interessados novos viraram ouvintes. O comportamento confere em tudo o que testei.

## Item a item

**O pedido (pede o pattern).**
- A interface está em `final/src/main/java/hotel/reservations/ReservationListener.java:9-18`. Tem `onConfirmed` e `onConfirmedReservationCancelled`, as duas com implementação padrão vazia.
- Cada interessado novo tem a sua classe:
  - a governança, em `final/src/main/java/hotel/notifications/HousekeepingNotifier.java:10-37`;
  - o financeiro, em `final/src/main/java/hotel/notifications/FinanceNotifier.java:10-37`, que decide sozinho não avisar quando o reembolso é zero (`FinanceNotifier.java:30-32`).
- O serviço recebe a lista pelo construtor e guarda uma cópia (`final/src/main/java/hotel/reservations/ReservationService.java:26-36`). Ele avisa os ouvintes na confirmação (`ReservationService.java:85-86`) e no cancelamento, só quando a reserva estava confirmada (`ReservationService.java:104-109`).
- A montagem é feita em `final/src/main/java/hotel/Hotel.java:26-32`.

Até aqui é o Observer. Mas o hóspede continua com chamada direta: `ReservationService.java:80-84` monta o comprovante e chama `emailSender.send(reservation.guest().email(), "Reserva confirmada - " + HOTEL_NAME, confirmationEmailBody(...))`, e o texto continua em `ReservationService.java:145-151`. Por isso o serviço ainda depende de `EmailSender` (`ReservationService.java:18`). Também ficou um construtor de quatro argumentos sem ouvintes (`ReservationService.java:21-24`). As instruções dizem que o e-mail do hóspede também vira ouvinte, e listam "o hóspede continua com chamada direta e só os novos viraram ouvintes" como malfeito. A estrutura existe, mas convive com o jeito antigo.
Conclusão: malfeito

**O exagero colateral.** Não há. Os arquivos novos (a interface, os dois ouvintes e o teste) e as mudanças no `Hotel` e no `ReservationService` são todos parte da resposta ao pedido. Não apareceu fábrica, builder nem camada nova em outra parte do código. O construtor extra de quatro argumentos (`ReservationService.java:21-24`) só mantém a assinatura antiga; não é estrutura nova.
Conclusão: contenção

**O comportamento.** Testei uma cópia da `final/` com `mvn -q package -DskipTests` e `PORT=7070 java -jar target/hotel.jar`, chamando a API com `curl`:
- **Confirmação STANDARD.** Criei a reserva 1 (quarto 201, 10/07/2027 a 12/07/2027, 2 hóspedes) e chamei `POST /reservations/1/confirmation`. O `GET /messages` trouxe três e-mails:
  - o do hóspede, igual ao da base: assunto "Reserva confirmada - Pousada Maré Alta", com a saudação, o comprovante, a linha dos horários e "Até breve!" (`confirmationEmailBody` não foi alterado no patch);
  - um para `governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto 201", corpo "Entrada: 10/07/2027 / Saída: 12/07/2027 / Hóspedes: 2";
  - um para `financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva 1", corpo "Valor pago: R$ 935,76".
- **Cancelamento da reserva 1 (confirmada, STANDARD).** Vieram "Reserva cancelada - quarto 201" para a governança, com "Datas liberadas: de 10/07/2027 a 12/07/2027", e "Reembolso - reserva 1" para o financeiro, com "Valor a devolver: R$ 935,76".
- **NON_REFUNDABLE (reserva 2, quarto 301).** A confirmação mandou os três e-mails, com o valor no formato do comprovante: "R$ 1.478,74". O cancelamento mandou só o da governança. Nenhum e-mail de reembolso foi para o financeiro, e o reembolso ficou 0,00, como pedido.
- **Pendente cancelada (reserva 3).** O total de mensagens continuou em 9: ninguém foi avisado.
- **O que o pedido não mudou continua igual:**
  - confirmar uma reserva cancelada dá `409 OPERATION_NOT_ALLOWED` com a mesma mensagem;
  - a reserva 4 passou por confirmação, check-in e check-out, e o check-out devolveu o comprovante;
  - cancelar depois do check-out é recusado;
  - o relatório `revenue?month=2027-07` lista a reserva 4 com 935,76.
- **Testes.** O `mvn -q verify` passou com 30 testes, 0 falhas e 0 erros. Os 4 testes novos do agente estão em `NotifiersTest`, e o `HttpApiTest` foi ajustado para esperar 3 mensagens na confirmação.

Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: as instruções do pattern citam este caso, o hóspede com chamada direta e só os novos como ouvintes, como malfeito. Uma observação menor: o evento de cancelamento só existe para reservas que estavam confirmadas (`onConfirmedReservationCancelled`). A decisão de não avisar ninguém no cancelamento de uma reserva pendente ficou no serviço (`ReservationService.java:104-106`), não nos ouvintes. Isso é razoável para o pedido e não muda a classe.

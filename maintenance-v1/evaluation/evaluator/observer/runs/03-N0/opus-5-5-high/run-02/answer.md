# Cenário 03

## Visão geral

O agente juntou os e-mails da governança e do financeiro numa classe nova, `ReservationNotifications`, que sabe quem são todos os destinatários e manda cada e-mail ela mesma. O `ReservationService` cria essa classe por conta própria e chama `notifyConfirmation` e `notifyCancellation` diretamente. Não existe interface de ouvinte, nem uma lista de interessados que o serviço receba de fora, e a classe `Hotel` não registra nada. O e-mail do hóspede continua sendo mandado direto pelo serviço. O comportamento está certo: os quatro e-mails novos saem com os assuntos, as datas e os valores pedidos, e o financeiro só é avisado quando há reembolso. O problema mais sério é o desenho. Ele é o "notificador único que conhece todos os destinos", que as instruções contam como omissão. Para pôr o restaurante ou o transfer, alguém vai ter de abrir essa classe e o serviço de novo.

## Item a item

**O pedido (pede o pattern).** O caso é do cenário 03, que pede o Observer. A classe `final/src/main/java/hotel/reservations/ReservationNotifications.java` não implementa nenhuma interface (linha 9). Ela guarda os endereços da governança e do financeiro em constantes (`ReservationNotifications.java:12-13`). `notifyConfirmation` chama os dois envios um depois do outro (`ReservationNotifications.java:22-25`). `notifyCancellation` faz o mesmo e ainda decide ela mesma se avisa o financeiro (`ReservationNotifications.java:28-33`). Os quatro textos ficam montados ali dentro (`ReservationNotifications.java:35-61`).

O serviço não recebe nada de fora: ele mesmo cria o notificador com `new ReservationNotifications(emailSender)` (`final/src/main/java/hotel/reservations/ReservationService.java:26`). A classe `Hotel` não mudou e continua passando só o `EmailSender` (`final/src/main/java/hotel/Hotel.java:23-24`). O e-mail do hóspede segue como chamada direta a `emailSender.send` em `confirm` (`ReservationService.java:72-75`), logo antes de `notifications.notifyConfirmation` (`ReservationService.java:76`). O cancelamento chama o notificador concreto (`ReservationService.java:96-98`).

Não há interface, não há uma implementação por interessado, não há lista de ouvintes, e o hóspede não virou ouvinte. É o "notificador que recebe a reserva e manda os e-mails ele mesmo", que as instruções classificam como omissão, mesmo estando numa classe separada.
Conclusão: omissão

**O exagero colateral.** Não há. A única classe nova é `ReservationNotifications`, e ela pertence ao que o pedido tocava. Não foi criada nenhuma interface, fábrica, builder ou camada em outra parte do código. A mudança se limita a `ReservationService.java` e aos testes.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/` e comparei com a `base/` rodando em paralelo:

- **Testes:** `mvn -q verify` passou, com 26 testes e nenhuma falha. O agente não escreveu testes novos. Ele só ajustou os existentes para contar 3 mensagens na confirmação e conferir os assuntos da governança e do financeiro. Não há teste do cancelamento.
- **Confirmação:** criei e confirmei pelo `POST /reservations/{id}/confirmation` as reservas 1 (quarto 201, STANDARD), 2 (101, NON_REFUNDABLE) e 4 (301, STANDARD). Para cada uma, o `GET /messages` mostrou três e-mails:
  - o do hóspede;
  - um para `governanca@pousadamarealta.com.br` com "Reserva confirmada - quarto 201", "Entrada: 10/07/2027", "Saída: 12/07/2027" e "Hóspedes: 2";
  - um para `financeiro@pousadamarealta.com.br` com "Pagamento recebido - reserva 1" e "Valor: R$ 935,76". A reserva 4 saiu com "R$ 1.159,80".
- **E-mail do hóspede:** é igual ao da `base/` campo a campo. Só o `id` da mensagem muda, porque agora há mais mensagens antes dele.
- **Cancelamento com reembolso:** a reserva 1 (confirmada, STANDARD) gerou "Reserva cancelada - quarto 201" com as datas, e "Reembolso - reserva 1" com "Valor: R$ 935,76".
- **Cancelamento sem reembolso:** a reserva 2 (confirmada, NON_REFUNDABLE, reembolso 0,00) avisou só a governança ("Reserva cancelada - quarto 101"). O financeiro não recebeu nada, como pedido.
- **Reserva pendente:** cancelar a reserva 3 não gerou mensagem nenhuma (a contagem ficou em 12).
- **O que não mudou:** confirmar uma reserva cancelada e cancelar uma reserva em check-in continuam dando `OPERATION_NOT_ALLOWED`, com a mesma mensagem da base e sem e-mail. Os relatórios de faturamento (`/reports/revenue?month=2027-03`) e de ocupação (`/reports/occupancy?date=2027-08-01`) e a segunda via do comprovante (`/reservations/4/document`) saíram idênticos aos da base.

Os serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma. O código não tem interface, nem lista, nem registro na `Hotel`, e o hóspede continua com chamada direta. A descrição de omissão das instruções cobre exatamente esse caso ("uma classe 'notificadora' recebe a reserva e manda os três e-mails ela mesma"). Aqui ela manda dois e o serviço manda o terceiro, o que só reforça a classificação.

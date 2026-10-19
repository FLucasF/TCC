# Cenário 03

## Visão geral

O agente aplicou o Observer como o pedido precisava. Criou a interface `ReservationListener`, com os eventos `confirmed` e `cancelled`, e três ouvintes, um para cada interessado: o hóspede, a governança e o financeiro. O `ReservationService` recebe a lista de ouvintes de fora e percorre essa lista tanto na confirmação quanto no cancelamento de uma reserva confirmada. A lista é registrada na classe `Hotel`, que monta o sistema. O e-mail do hóspede, que antes era montado à mão dentro do serviço, também virou ouvinte, e saiu igual ao da base. Não há estrutura sobrando, e os testes pela API conferem com o pedido. Não encontrei problema sério.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin com estes pacotes: `hotel.rooms` (quartos), `hotel.reservations` (regras e ciclo de vida da reserva, preço, armazenamento em memória), `hotel.documents` (o comprovante e a formatação de dinheiro e datas), `hotel.messages` (o `EmailSender`, que grava as mensagens em `SentMessages`, consultadas em `GET /messages`), `hotel.reports` (os relatórios CSV) e `hotel.web` (as rotas). Uma requisição como `POST /reservations/{id}/confirmation` entra em `HttpApi` (`final/src/main/java/hotel/web/HttpApi.java:70-71`), que chama `ReservationService.confirm`. O serviço muda o estado da `Reservation` guardada no `ReservationStore`. Tudo fica em memória. A classe `Hotel` cria as peças e sobe o servidor (`final/src/main/java/hotel/Hotel.java:23-35`). O agente criou o pacote novo `hotel.notifications`, com os três ouvintes, e o teste `TeamNoticesTest` (3 casos: confirmação, reembolso com e sem valor, cancelamento de pendente). Também ajustou os testes que já existiam para o novo construtor do serviço.

**A parte do pattern.**
- A interface `ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:6-15`) tem dois métodos `default` vazios, `confirmed(Reservation, Room)` e `cancelled(Reservation, Room)`. Assim, um ouvinte que só se interessa pela confirmação implementa só a confirmação.
- O serviço guarda a lista de ouvintes, recebida no construtor (`final/src/main/java/hotel/reservations/ReservationService.java:15-22`), e não importa mais o `EmailSender`.
- Em `confirm`, depois de `confirmPayment`, ele faz `listeners.forEach(listener -> listener.confirmed(reservation, room))` (`ReservationService.java:65-67`).
- Em `cancel`, ele anota se a reserva estava confirmada antes de cancelar e só nesse caso avisa os ouvintes (`ReservationService.java:81`, `87-90`). Assim, a reserva pendente cancelada não avisa ninguém.

Cada interessado é uma classe e guarda a própria regra:
- `GuestConfirmationEmail` (`final/src/main/java/hotel/notifications/GuestConfirmationEmail.java:21-32`) leva o texto que antes era o `confirmationEmailBody` do serviço (`base/src/main/java/hotel/reservations/ReservationService.java`, removido no patch).
- `HousekeepingNotice` (`final/src/main/java/hotel/notifications/HousekeepingNotice.java:21-34`) monta os assuntos "Reserva confirmada - quarto N" e "Reserva cancelada - quarto N", com as datas e o número de hóspedes.
- `FinanceNotice` (`final/src/main/java/hotel/notifications/FinanceNotice.java:21-34`) avisa o pagamento e, no cancelamento, só manda o reembolso se `refund().signum() > 0` (linha 29).

O registro fica em `Hotel.java:27-31`, com `List.of(new GuestConfirmationEmail(...), new HousekeepingNotice(...), new FinanceNotice(...))`.

**O raciocínio.** É exatamente o que as instruções chamam de acerto:
- há uma interface de ouvinte com uma implementação por interessado;
- o serviço percorre uma lista que recebe de fora e não sabe quem está nela;
- o hóspede também é ouvinte, e não ficou com chamada direta;
- os dois eventos passam pela lista;
- o serviço não monta nenhum e-mail.

Nada disso se parece com os sinais de malfeito ou de omissão: não há "notificador" único que conheça todos os destinos, nem `if` por destinatário no serviço. Para pôr o restaurante ou o transfer, basta uma classe nova e uma linha em `Hotel`. Quanto ao exagero, não há fábrica, builder nem camada que só repassa. A única interface nova é a do próprio pattern, e ela tem três implementações. Quanto ao comportamento, testei pela API e tudo conferiu. O e-mail do hóspede saiu igual ao da base, letra por letra; só o `id` da mensagem muda, porque a sequência agora inclui os e-mails da equipe.

## Item a item

**O pattern pedido.** As peças são a interface `ReservationListener` (`final/src/main/java/hotel/reservations/ReservationListener.java:9,13`) e três implementações, uma por interessado (`final/src/main/java/hotel/notifications/GuestConfirmationEmail.java:10`, `HousekeepingNotice.java:10`, `FinanceNotice.java:10`). O serviço recebe a lista pronta (`final/src/main/java/hotel/reservations/ReservationService.java:17-22`) e a percorre na confirmação (`:67`) e no cancelamento de reserva confirmada (`:87-90`). O registro está na montagem (`final/src/main/java/hotel/Hotel.java:28-31`). O e-mail do hóspede, que na base era chamada direta, também passou a ser ouvinte. O serviço não conhece destinatários nem monta textos, e os dois eventos usam a mesma lista. É o Observer que o texto pedia, aplicado a todos os casos, inclusive ao que já existia.
Conclusão: acerto

**O exagero colateral.** Não há. As únicas peças novas são a interface de ouvinte, os três ouvintes e o pacote `notifications`, e todas fazem parte do pattern. Não há fábrica, builder, registro dinâmico nem camada intermediária. O `switch` do reembolso por plano tarifário (`ReservationService.java:82-85`) já existia na base e não foi mexido.
Conclusão: contenção

**O comportamento.** Gerei os jars da base e da cópia da `final/` e subi os dois serviços, nas portas 7071 e 7070. Rodei a mesma sequência nos dois:
- criei 4 reservas: 201 STANDARD em julho, 202 NON_REFUNDABLE em março, 101 STANDARD, e 201 STANDARD em agosto;
- confirmei as reservas 1, 2 e 4;
- cancelei a 1 (confirmada, com reembolso), a 2 (confirmada, sem reembolso) e a 3 (pendente);
- tentei confirmar a 3 já cancelada, fiz o check-in da 4, tentei cancelar a 4 depois do check-in e fiz o check-out dela;
- pedi a segunda via da 2, o relatório de receita de julho e o de ocupação de 01/08.

As respostas JSON e CSV de todas essas chamadas saíram idênticas nas duas versões (o `diff` não mostrou diferença). Em `/messages`, na `final/`:
- Cada confirmação gerou três e-mails: o do hóspede, igual ao da base (comparei os campos; só o `id` muda); o da governança, com o assunto "Reserva confirmada - quarto 201", "Entrada: 10/07/2027 | Saída: 12/07/2027" e "Hóspedes: 2"; e o do financeiro, com o assunto "Pagamento recebido - reserva 1" e "pagamento de R$ 935,76". Na reserva 2, o valor saiu como R$ 994,24.
- O cancelamento da 1 gerou "Reserva cancelada - quarto 201" ("ficou livre de 10/07/2027 a 12/07/2027") e "Reembolso - reserva 1" ("Devolver R$ 935,76").
- O cancelamento da 2 (sem reembolso) avisou só a governança.
- O cancelamento da 3 (pendente) não gerou e-mail.
- As operações recusadas (confirmar reserva inexistente, confirmar reserva cancelada, cancelar depois do check-in) devolveram os mesmos erros da base e não mandaram e-mail.

No fim, `/messages` tinha 12 mensagens, o número esperado. O `mvn verify` da cópia passou: 29 testes, sem falha. Os 3 do `TeamNoticesTest` são os novos. Derrubei os serviços no fim.
Conclusão: segue

## Dúvidas

Nenhuma.

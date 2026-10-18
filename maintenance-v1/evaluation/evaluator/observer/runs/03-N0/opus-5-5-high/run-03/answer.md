# Cenário 03

## Visão geral

O agente criou a estrutura do Observer. Há uma interface de ouvinte (`ReservationNotifier`) com uma implementação para a governança e outra para o financeiro. Elas são registradas na `Hotel`, e o `ReservationService` as percorre por meio de um `NotificationCoordinator`, que recebe a lista de fora. O cancelamento também passa pelos ouvintes. O comportamento confere com o pedido em tudo o que testei, e os 30 testes passam. O problema sério é que o interessado que já existia, o hóspede, não virou ouvinte. `confirm` continua montando e mandando o e-mail dele à mão, com o `EmailSender` injetado direto no serviço, e só os interessados novos foram para a estrutura nova. As instruções do pattern classificam exatamente esse caso como `malfeito`.

## Item a item

**O pedido (pede o pattern).**
- **A estrutura nova.** A interface `ReservationNotifier` tem `onConfirmed` e `onCancelled` (`final/src/main/java/hotel/notifications/ReservationNotifier.java:6-11`). Ela tem duas implementações: `HousekeepingNotifier` (`final/src/main/java/hotel/notifications/HousekeepingNotifier.java:9-40`) e `FinanceNotifier` (`final/src/main/java/hotel/notifications/FinanceNotifier.java:9-39`).
- **O registro.** A lista de ouvintes é montada na `Hotel` (`final/src/main/java/hotel/Hotel.java:28-29`) e chega ao serviço pelo construtor (`final/src/main/java/hotel/reservations/ReservationService.java:26,31`).
- **O anúncio.** O serviço só anuncia: `notifyConfirmed` em `ReservationService.java:81` e `notifyCancelled` em `ReservationService.java:101`. O coordenador percorre a lista sem saber quem são os ouvintes (`final/src/main/java/hotel/notifications/NotificationCoordinator.java:16-28`).
- **O financeiro.** A regra "só avisa quando há algo a devolver" ficou dentro do próprio ouvinte (`FinanceNotifier.java:30-31`), como deve ser.
- **O que ficou de fora: o hóspede.** O e-mail do hóspede, o interessado que já existia, continua como chamada direta: `emailSender.send(reservation.guest().email(), "Reserva confirmada - " + HOTEL_NAME, confirmationEmailBody(...))` em `ReservationService.java:77-80`. O texto continua montado no próprio serviço (`ReservationService.java:137`), e o `EmailSender` continua sendo dependência do serviço (`ReservationService.java:18,30`). Por isso convivem o jeito antigo (o e-mail do hóspede à mão) e o novo (só governança e financeiro como ouvintes).
- **Detalhe menor.** O coordenador recebe um `boolean wasConfirmed` e decide sozinho não avisar ninguém (`NotificationCoordinator.java:22-23`). Assim os ouvintes não podem escolher reagir ao cancelamento de uma reserva pendente. É aceitável, mas não altera a classe.

Conclusão: malfeito

**O exagero colateral.** Não há. Toda a estrutura nova (interface, dois ouvintes, coordenador) está no ponto que o pedido tocava, a notificação de confirmação e de cancelamento. Fora disso, as mudanças são só a montagem na `Hotel` e o ajuste dos construtores nos testes que já existiam (`final/src/test/java/hotel/reports/ReportsTest.java:26-27`, `final/src/test/java/hotel/reservations/ReservationServiceTest.java:22-27`). Não apareceu fábrica, builder nem camada nova em outra parte do código.

Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/` com o serviço na porta 7070 e, para comparar, numa cópia da `base/` na porta 7071, com a mesma sequência de chamadas `curl`.

- **Confirmação.** Criei a reserva 1 (quarto 201, 10/07–12/07/2027, 2 hóspedes, STANDARD) e chamei `POST /reservations/1/confirmation`. Vieram 3 mensagens:
  - para `carla@example.com`, o assunto "Reserva confirmada - Pousada Maré Alta";
  - para `governanca@pousadamarealta.com.br`, o assunto "Reserva confirmada - quarto 201", com "Entrada: 10/07/2027", "Saída: 12/07/2027" e "Hóspedes: 2";
  - para `financeiro@pousadamarealta.com.br`, o assunto "Pagamento recebido - reserva 1", com "Valor: R$ 935,76".

  A reserva 4 (quarto 301) deu "R$ 1.159,80", com separador de milhar como no comprovante.
- **Cancelamento com reembolso.** `POST /reservations/1/cancellation` mandou à governança "Reserva cancelada - quarto 201", com as datas De 10/07/2027 e Até 12/07/2027, e ao financeiro "Reembolso - reserva 1", com R$ 935,76.
- **Cancelamento sem reembolso.** A reserva 2 (NON_REFUNDABLE), confirmada e depois cancelada, teve reembolso 0,00. Saiu só o aviso da governança ("Reserva cancelada - quarto 101"), sem financeiro, como o pedido manda.
- **Reserva pendente.** A reserva 3, cancelada ainda pendente, não gerou nenhuma mensagem.
- **O que não mudou.** Os e-mails do hóspede são idênticos aos da `base/` (comparei com `jq`; só muda o id sequencial das mensagens, porque agora há mais mensagens na lista). Os relatórios `/reports/revenue?month=2027-03` e `/reports/occupancy?date=2027-05-01` saíram iguais aos da base. As recusas também continuam iguais: cancelar uma reserva com check-in feito devolve `OPERATION_NOT_ALLOWED` "cannot cancel reservation 4 while it is CHECKED_IN", e cancelar duas vezes devolve o mesmo código.
- **Testes.** `mvn -q verify` passou: 30 testes, 0 falhas. Desses, 4 são testes novos do agente em `NotificationsTest`, todos passando. O serviço foi derrubado no fim.

Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. O que pesa é o hóspede continuar com chamada direta em `ReservationService.java:77-80`, que as instruções citam literalmente como `malfeito`. Fora isso, o resto da estrutura teria sido um `acerto`.

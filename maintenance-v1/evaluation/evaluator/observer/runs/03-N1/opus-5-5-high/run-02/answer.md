# Cenário 03

## Visão geral

O agente montou a estrutura do Observer para os dois interessados novos, e ela está bem feita. Há uma interface de ouvinte `ReservationNotifier` com `onConfirmed` e `onCancelled`, um ouvinte para a governança e outro para o financeiro, e a lista de ouvintes é registrada na `Hotel`. O serviço recebe essa lista de fora e a percorre na confirmação e no cancelamento. O problema mais sério é que o e-mail do hóspede, o único interessado que já existia, não virou ouvinte. `ReservationService.confirm` continua montando e mandando esse e-mail à mão, com `confirmationEmailBody`, e só depois avisa os ouvintes. O jeito antigo e o novo convivem. As instruções do pattern classificam exatamente esse caso como malfeito. O comportamento confere com o pedido em tudo o que testei.

## Item a item

**O pedido (pede o pattern).** O agente criou a interface `src/main/java/hotel/notifications/ReservationNotifier.java:7-12`, com `onConfirmed(reservation, room)` e `onCancelled(reservation, room, refund)`. Cada interessado novo é uma implementação separada:
- a governança, em `src/main/java/hotel/notifications/HousekeepingNotifier.java:9-35`;
- o financeiro, em `src/main/java/hotel/notifications/FinanceNotifier.java:9-34`, que decide sozinho não mandar nada quando o reembolso é zero (`FinanceNotifier.java:28`).

`src/main/java/hotel/notifications/ReservationNotifications.java:10-32` guarda a lista e percorre os ouvintes (`:19-21` e `:27-29`). A lista é montada na `Hotel` (`src/main/java/hotel/Hotel.java:28-29`) e injetada no serviço (`Hotel.java:31`, `src/main/java/hotel/reservations/ReservationService.java:21-28`). O serviço anuncia o evento sem conhecer os destinos (`ReservationService.java:78` e `:98`). A regra "só avisa se a reserva estava confirmada" ficou do lado de quem anuncia (`ReservationNotifications.java:26`), o que é razoável.

O hóspede, porém, continua com chamada direta. `ReservationService.java:74-77` ainda faz `emailSender.send(reservation.guest().email(), "Reserva confirmada - " + HOTEL_NAME, confirmationEmailBody(...))`. O texto ainda é montado dentro do serviço (`ReservationService.java:134-140`), e o serviço ainda depende de `EmailSender` (`ReservationService.java:18`). É o mesmo código da base (`base/src/main/java/hotel/reservations/ReservationService.java:69-72`). Só os interessados novos foram para a estrutura nova. As instruções do pattern dizem expressamente que isso é malfeito: "quando o hóspede continua com chamada direta e só os novos viraram ouvintes".
Conclusão: malfeito

**O exagero colateral.** Não há. Tudo o que foi criado (`ReservationNotifier`, os dois ouvintes, `ReservationNotifications`) faz parte do pattern que o pedido precisava. Em `Hotel.java:27`, um único `EmailSender` passou a ser compartilhado, o que é só montagem. As outras partes do sistema (relatórios, preço, API) não ganharam estrutura nova. Os testes mudaram apenas a montagem do serviço (`src/test/java/hotel/reports/ReportsTest.java:28-32`).
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`. O `mvn -q package -DskipTests` funcionou, e subi o serviço com `PORT=7070 java -jar target/hotel.jar`. Criei pelo `POST /reservations` quatro reservas:
- 1: quarto 201, de 10/03 a 12/03/2027, 2 hóspedes, `STANDARD`;
- 2: quarto 202, de 10/07 a 13/07/2027, 3 hóspedes, `NON_REFUNDABLE`;
- 3: quarto 101, `STANDARD`;
- 4: quarto 102, `STANDARD`, que ficou pendente.

O que testei:
- **Confirmação da 1** (`POST /reservations/1/confirmation`), conferida em `GET /messages`: saíram 3 e-mails.
  - O do hóspede tem o assunto "Reserva confirmada - Pousada Maré Alta" e o mesmo texto da base: saudação, comprovante, a linha "A entrada é a partir das 14h…" e "Até breve!".
  - A governança recebeu "Reserva confirmada - quarto 201" com "Entrada: 10/03/2027 / Saída: 12/03/2027 / Hóspedes: 2".
  - O financeiro recebeu "Pagamento recebido - reserva 1" com "Valor: R$ 779,80".
- **Confirmação da 2**: o valor apareceu como "R$ 1.193,09", com o separador de milhar, como no comprovante.
- **Cancelamento da 1, confirmada e `STANDARD`**: a governança recebeu "Reserva cancelada - quarto 201" com as datas. O financeiro recebeu "Reembolso - reserva 1" com "Valor: R$ 779,80".
- **Cancelamento da 2, confirmada e `NON_REFUNDABLE`, com reembolso 0,00**: só a governança foi avisada ("Reserva cancelada - quarto 202"). O financeiro não recebeu nada, como o pedido diz.
- **Cancelamento da 4, pendente**: nenhuma mensagem nova. O total ficou em 12, o esperado.

Também conferi coisas que o pedido não mudou:
- cancelar de novo e confirmar uma reserva cancelada continuam dando `OPERATION_NOT_ALLOWED`, com a mesma mensagem;
- o check-in e o check-out da 3 funcionam, e o check-out devolve o comprovante final;
- os relatórios `/reports/revenue?month=2027-03` e `/reports/occupancy?date=2027-03-10` saem coerentes.

O `mvn -q verify` da cópia passou: 29 testes, 0 falhas e 0 erros (PriceCalculatorTest 6, ReportsTest 5, HttpApiTest 4, ReservationServiceTest 14). Entre eles estão os três testes novos do agente, para a confirmação, o cancelamento de reserva confirmada e o cancelamento de reserva pendente. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe: a chamada direta ao e-mail do hóspede em `ReservationService.java:74-77` é exatamente o caso que as instruções chamam de malfeito. Uma ressalva menor, que não muda a classe: o filtro "estava confirmada" é feito em `ReservationNotifications.notifyCancelled` a partir de um `previousStatus` passado pelo serviço (`ReservationService.java:92` e `:98`). Funciona, mas a assinatura do evento de cancelamento ficou ligada a esse detalhe.

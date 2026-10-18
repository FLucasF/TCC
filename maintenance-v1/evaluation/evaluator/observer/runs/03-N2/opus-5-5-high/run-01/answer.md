# Cenário 03

## Visão geral

O agente criou uma interface de ouvinte (`ReservationObserver`) com duas implementações, uma para a governança e outra para o financeiro. Os ouvintes são registrados na classe `Hotel`, e o serviço avisa todos eles tanto na confirmação quanto no cancelamento de uma reserva confirmada. A parte nova está bem feita. O problema mais sério é que o e-mail do hóspede, que já existia, não virou ouvinte: `confirm` continua montando e mandando esse e-mail direto pelo `EmailSender`, antes de avisar a lista. Ou seja, só os interessados novos passaram pela estrutura nova, e o caso antigo ficou do jeito antigo. Pelas instruções, isso é malfeito. O comportamento confere com o pedido em tudo o que testei.

## Item a item

**O pedido (pede o pattern).** A interface está em `src/main/java/hotel/reservations/ReservationObserver.java:6-13`, com `onConfirmation` e `onCancellation`. Há uma implementação por interessado novo: `src/main/java/hotel/notifications/HousekeepingNotifier.java:10` e `src/main/java/hotel/notifications/FinanceNotifier.java:11`. A regra "só avisa o financeiro se houver reembolso" fica dentro do ouvinte do financeiro (`FinanceNotifier.java:32-33`), e não no serviço. O serviço guarda uma lista que recebe de fora por `addObserver` (`src/main/java/hotel/reservations/ReservationService.java:20,31-33`) e a percorre na confirmação (`ReservationService.java:81-84`) e no cancelamento, só quando a reserva estava confirmada (`ReservationService.java:98,104-109`). Os ouvintes são registrados na montagem do sistema (`src/main/java/hotel/Hotel.java:28-29`). Até aqui é o Observer. Só que o hóspede continua com chamada direta: `confirm` monta o comprovante e chama `emailSender.send(...)` com `confirmationEmailBody` (`ReservationService.java:76-80`), igual à base (`base/src/main/java/hotel/reservations/ReservationService.java:69-72`). O serviço ainda guarda e usa o `EmailSender` (`ReservationService.java:19,27`). Portanto o serviço continua conhecendo um dos interessados e mandando o e-mail dele à mão. As instruções citam exatamente esse caso como malfeito: "o hóspede continua com chamada direta e só os novos viraram ouvintes".
Conclusão: malfeito

**O exagero colateral.** Não há. O que foi criado (a interface de ouvinte, os dois ouvintes e o `addObserver`) fica dentro do que o pedido tocava. Não apareceu fábrica, builder nem camada nova em outras partes. A única mudança em `Hotel.java:25` é tirar o `EmailSender` para uma variável e reaproveitá-lo, o que não é estrutura nova.
Conclusão: contenção

**O comportamento.** Fiz uma cópia da `final/`, gerei o jar e subi o serviço na porta 7070. Os testes, pela API:
- **Confirmação.** Criei a reserva 1 (quarto 201, de 10/07/2027 a 12/07/2027, 2 hóspedes, `STANDARD`) e chamei `POST /reservations/1/confirmation`. O `GET /messages` trouxe três e-mails. O do hóspede tem o assunto "Reserva confirmada - Pousada Maré Alta" e o mesmo texto da base: a saudação, o comprovante, a linha dos horários e "Até breve!". O da governança vai para `governanca@pousadamarealta.com.br`, com o assunto "Reserva confirmada - quarto 201", entrada 10/07/2027, saída 12/07/2027 e 2 hóspedes. O do financeiro vai para `financeiro@pousadamarealta.com.br`, com o assunto "Pagamento recebido - reserva 1" e o valor de R$ 935,76.
- **Cancelamento de reserva confirmada com reembolso.** `POST /reservations/1/cancellation` gerou dois e-mails. A governança recebeu "Reserva cancelada - quarto 201", com "Datas livres: 10/07/2027 a 12/07/2027". O financeiro recebeu "Reembolso - reserva 1", com o valor a devolver de R$ 935,76.
- **Cancelamento sem reembolso.** Confirmei e cancelei uma reserva `NON_REFUNDABLE` no quarto 102 (refund 0,00). Só a governança foi avisada ("Reserva cancelada - quarto 102"), e o financeiro não, como o pedido manda.
- **Cancelamento de reserva pendente.** Criei a reserva 3 e cancelei sem confirmar. O total de mensagens ficou em 9, antes e depois, então ninguém foi avisado.
- **O que não mudou.** Cancelar de novo dá 409 `OPERATION_NOT_ALLOWED`. Hóspedes demais dão `CAPACITY_EXCEEDED`. A segunda via do comprovante e os relatórios de receita (`month=2027-07`) e de ocupação (`date=2027-07-10`) vieram coerentes.
- **Testes.** O `mvn verify` da cópia passou: 28 testes, 0 falhas, 0 erros, `BUILD SUCCESS`. O agente acrescentou dois testes de cancelamento e ajustou as contagens dos testes que já existiam.
- No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma. A estrutura de ouvintes está correta para os interessados novos e para os dois eventos. O que decide a classe é a chamada direta ao e-mail do hóspede em `ReservationService.java:76-80`, que as instruções classificam expressamente como malfeito.

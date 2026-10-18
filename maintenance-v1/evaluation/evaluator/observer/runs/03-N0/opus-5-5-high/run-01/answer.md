```markdown
# Cenário 03

## Visão geral

O agente montou a base de um Observer: criou uma interface de ouvinte com dois eventos (`confirmed` e `cancelledAfterConfirmation`), e o serviço agora percorre uma lista de ouvintes nos dois pontos, na confirmação e no cancelamento de reserva confirmada. O comportamento pedido está certo: testei pela API e todos os e-mails, valores, datas e recusas conferem. O desenho, porém, ficou no meio do caminho, e há três problemas. O e-mail do hóspede, que já existia, continua como chamada direta dentro de `confirm` e não virou ouvinte. A governança e o financeiro não são dois ouvintes, e sim um "notificador da equipe" só, que conhece os dois destinos. E quem registra esse ouvinte é o próprio `ReservationService`, no construtor padrão, e não a montagem em `Hotel`. Ou seja, o serviço ainda conhece quem é avisado. A estrutura existe, mas convive com o jeito antigo.

## Item a item

**O pedido (pede o pattern).** A interface `ReservationListener` (`src/main/java/hotel/reservations/ReservationListener.java:10-21`) tem métodos `default` para os dois eventos. O serviço guarda uma `List<ReservationListener>` (`src/main/java/hotel/reservations/ReservationService.java:21`) e a percorre em `notifyListeners` (`ReservationService.java:145-153`). Ele dispara o evento na confirmação (`ReservationService.java:88-89`) e, quando a reserva estava confirmada, no cancelamento (`ReservationService.java:107-112`). Essa parte é o Observer. Três pontos, porém, contrariam o que o pattern pede:
- O e-mail do hóspede continua sendo enviado à mão dentro de `confirm`, com `emailSender.send(...)` e o texto de `confirmationEmailBody` (`ReservationService.java:84-87` e `159-165`), igual à base. Só os interessados novos passaram pela lista. As instruções põem esse caso explicitamente no malfeito.
- Governança e financeiro ficam numa classe só, `StaffNotifications` (`src/main/java/hotel/notifications/StaffNotifications.java:11-54`), que conhece os dois endereços (`:13-14`) e manda os dois e-mails de cada evento (`:24-35` e `:40-52`). Não há um ouvinte por interessado: é um notificador que conhece vários destinos, só que pendurado na interface.
- O ouvinte é registrado dentro do serviço: o construtor de quatro argumentos cria `List.of(new StaffNotifications(emailSender))` (`ReservationService.java:24-27`), e o serviço importa `hotel.notifications.StaffNotifications` (`ReservationService.java:5`). A montagem em `Hotel` não mudou e usa justamente esse construtor (`src/main/java/hotel/Hotel.java:23-24`). Assim, o serviço continua sabendo quem é avisado, e o construtor que recebe a lista de fora (`ReservationService.java:29-40`) não é usado na montagem.

Como a lista de ouvintes existe mas o hóspede continua com chamada direta e o serviço ainda escolhe os ouvintes, a classe é malfeito.
Conclusão: malfeito

**O exagero colateral.** Não há. A única estrutura nova é a interface de ouvinte e a sua implementação, e as duas fazem parte do que o pedido tocava. O `try/catch` em `notifyListeners` (`ReservationService.java:144-153`) é um detalhe do próprio mecanismo de aviso, não uma camada à parte. Fora disso, nenhuma fábrica, builder ou camada foi criada.
Conclusão: contenção

**O comportamento.** Rodei uma cópia da `final/` com `PORT=7070 java -jar target/hotel.jar` e chamei a API com `curl`:
- **Confirmação** (`POST /reservations/1/confirmation`, quarto 201, de 10/03/2027 a 13/03/2027, 2 hóspedes, plano STANDARD): saíram três mensagens em `/messages`.
  - O hóspede recebeu "Reserva confirmada - Pousada Maré Alta", com o mesmo texto da base (o `confirmationEmailBody` é idêntico ao da base).
  - A governança (`governanca@pousadamarealta.com.br`) recebeu "Reserva confirmada - quarto 201", com Entrada 10/03/2027, Saída 13/03/2027 e Hóspedes: 2.
  - O financeiro (`financeiro@pousadamarealta.com.br`) recebeu "Pagamento recebido - reserva 1", com Valor pago: R$ 1.169,70.
- **Cancelamento da reserva confirmada STANDARD:** a governança recebeu "Reserva cancelada - quarto 201", com "Datas liberadas: de 10/03/2027 a 13/03/2027". O financeiro recebeu "Reembolso - reserva 1", com "Valor a devolver: R$ 1.169,70".
- **Reserva NON_REFUNDABLE confirmada e cancelada:** só a governança foi avisada do cancelamento. O reembolso foi 0,00 e o financeiro não recebeu nada, como o pedido manda.
- **Reserva pendente cancelada:** a contagem em `/messages` não mudou, então ninguém foi avisado.
- **O que o pedido não mudou continua igual:**
  - Confirmar de novo uma reserva cancelada dá `OPERATION_NOT_ALLOWED`, e cancelar uma reserva já cancelada também.
  - O check-in e o check-out funcionam, e o check-out devolve o comprovante.
  - Cancelar depois do check-out é recusado.
  - Os relatórios `revenue` (2027-07) e `occupancy` (2027-07-02) saem no mesmo formato.

O `mvn -q verify` da cópia passou: 30 testes, 0 falhas e 0 erros. Desses, 4 são testes novos do agente em `ReservationServiceTest`, e ele também ajustou três contagens de mensagens nos testes que já existiam. No fim derrubei o serviço.
Conclusão: segue

## Dúvidas

Fiquei entre malfeito e omissão. O `StaffNotifications`, sozinho, parece o "notificador único que conhece todos os destinos", que as instruções chamam de omissão. Mas ele não manda os três e-mails: o do hóspede fica fora dele. E existe um mecanismo de verdade, com uma interface e uma lista percorrida pelo serviço nos dois eventos, que aceita outros ouvintes sem mexer no serviço. Por isso o caso fica mais perto do malfeito descrito nas instruções ("a lista de ouvintes existe… ou quando o hóspede continua com chamada direta e só os novos viraram ouvintes") do que de uma omissão pura.
```

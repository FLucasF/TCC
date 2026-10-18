# Cenário 03

## Visão geral

O agente montou a estrutura de Observer: criou a interface `ReservationEventHandler`, com `onConfirmed` e `onCancelled`, e um ouvinte para cada departamento novo (`GovernanceNotifier` e `FinanceNotifier`). O `ReservationService` recebe a lista de ouvintes de fora e a percorre na confirmação e no cancelamento de reserva confirmada. A lista é registrada em `Hotel`. Também está bom que o cancelamento passe pela lista, e não só a confirmação, e que o serviço não monte os e-mails da governança nem do financeiro. O problema mais sério é que o interessado que já existia, o hóspede, ficou de fora. O e-mail de confirmação dele continua sendo montado e enviado à mão dentro de `confirm`, ao lado do laço de ouvintes. O serviço ainda conhece um destino concreto, e o jeito antigo convive com a estrutura nova. O comportamento confere com o pedido em tudo o que testei.

## Item a item

**O pedido (pede o pattern).** A interface de ouvinte está em `final/src/main/java/hotel/reservations/ReservationEventHandler.java:6-10`. Há duas implementações, uma por departamento: `final/src/main/java/hotel/reservations/GovernanceNotifier.java:8` e `final/src/main/java/hotel/reservations/FinanceNotifier.java:8`. O serviço recebe a lista pelo construtor (`final/src/main/java/hotel/reservations/ReservationService.java:19,26,31`). Ele a percorre na confirmação (`ReservationService.java:81-83`) e, no cancelamento, só quando a reserva estava confirmada (`ReservationService.java:97,103-108`). O registro dos ouvintes fica na montagem do sistema (`final/src/main/java/hotel/Hotel.java:33`). Até aqui, é o Observer.

O e-mail do hóspede, porém, continua com chamada direta. `ReservationService.java:77-80` ainda chama `emailSender.send(reservation.guest().email(), "Reserva confirmada - " + HOTEL_NAME, confirmationEmailBody(...))`, o mesmo código de `base/src/main/java/hotel/reservations/ReservationService.java:69-72`. Por isso o serviço continua dependendo do `EmailSender` (`ReservationService.java:18,25`). As instruções do pattern classificam exatamente esse caso como malfeito: "o hóspede continua com chamada direta e só os novos viraram ouvintes". Há ainda um detalhe menor: `GovernanceNotifier.java:29-31` verifica de novo o status `CANCELLED`, uma guarda redundante que não altera o desenho.
Conclusão: malfeito

**O exagero colateral.** Não há. O agente só criou a interface de ouvinte e os dois ouvintes, todos dentro do que o pedido toca. Fora disso, as mudanças em `Hotel.java:26-33` e nos testes (`ReportsTest`, `ReservationServiceTest`) só ajustam o construtor novo, sem fábrica, builder ou camada nova.
Conclusão: contenção

**O comportamento.** Fiz uma cópia da `final/` em `/tmp/eval03`. O `mvn -q verify` passou com 30 testes e nenhuma falha: ReportsTest 5, NotifiersTest 4 (novo, do agente), PriceCalculatorTest 6, ReservationServiceTest 11 e HttpApiTest 4. Depois subi o jar na porta 7070 e testei pela API:

- **Confirmação da reserva 1** (quarto 201, 10/07 a 12/07/2027, 2 hóspedes, STANDARD), com `POST /reservations/1/confirmation`. Saíram três e-mails:
  - para o hóspede, com assunto "Reserva confirmada - Pousada Maré Alta" e o mesmo texto da base, com comprovante, horários e "Até breve!";
  - para `governanca@pousadamarealta.com.br`, com assunto "Reserva confirmada - quarto 201" e o corpo "Entrada: 10/07/2027 / Saída: 12/07/2027 / Hóspedes: 2";
  - para `financeiro@pousadamarealta.com.br`, com assunto "Pagamento recebido - reserva 1" e "Valor: R$ 935,76".
- **Confirmação da reserva 2** (NON_REFUNDABLE, total R$ 1.062,08). O financeiro recebeu "Valor: R$ 1.062,08", com separador de milhar, como no comprovante.
- **Cancelamento da reserva 1**, confirmada e STANDARD. Saíram "Reserva cancelada - quarto 201", com "Datas liberadas: 10/07/2027 a 12/07/2027", e "Reembolso - reserva 1", com "Valor: R$ 935,76".
- **Cancelamento da reserva 2**, confirmada e NON_REFUNDABLE, com reembolso de 0,00. Só a governança foi avisada ("Reserva cancelada - quarto 101"); o financeiro não recebeu nada, como o pedido manda.
- **Cancelamento da reserva 3**, pendente. Retornou 200, o reembolso ficou em 0,00 e não saiu nenhuma mensagem.

Conferi também coisas que o pedido não mudou. Cancelar depois do check-in continua recusado com 409 `OPERATION_NOT_ALLOWED`. O relatório `/reports/revenue?month=2027-07` deu total 0,00, porque a reserva 1 foi reembolsada inteira, e a segunda via (`/document`) segue igual. No fim, derrubei o serviço.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. A estrutura de Observer existe e está bem feita para os interessados novos, inclusive no cancelamento. Mas o hóspede, interessado que já existia, ficou com chamada direta em `ReservationService.java:77-80`, e as instruções do pattern dão esse caso, com essas palavras, como malfeito, e não como acerto.

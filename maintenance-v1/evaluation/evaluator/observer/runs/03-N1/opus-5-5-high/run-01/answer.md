# Cenário 03

## Visão geral

O agente aplicou o Observer. Criou a interface `ReservationListener`, com um evento para a confirmação e outro para o cancelamento de uma reserva confirmada. Cada interessado virou um ouvinte separado: o hóspede, a governança e o financeiro. O `ReservationService` recebe a lista de ouvintes de fora, percorre essa lista nos dois eventos e não sabe mais quem é avisado. A lista é montada em `Hotel`. O e-mail do hóspede, que já existia, saiu do serviço e também virou ouvinte. A regra de não avisar o financeiro quando não há reembolso ficou dentro do ouvinte do financeiro, e o serviço só decide qual evento anunciar. Não achei estrutura fora do que o pedido tocava. Nos testes pela API, o comportamento confere com o pedido. Não há problema sério. O único resíduo é cosmético: o comentário de `ReservationListener` diz "Every method does nothing unless overridden", e sobrou uma linha em branco dupla em `ReservationService.java:126-127`.

## Item a item

**O pedido (pede o pattern).** O agente criou a interface `ReservationListener` (`src/main/java/hotel/reservations/ReservationListener.java:6-15`), com `confirmed` e `confirmedReservationCancelled`, as duas com implementação vazia por padrão. Cada interessado tem a sua implementação:
- `GuestNotifier` (`src/main/java/hotel/notifications/GuestNotifier.java:10-31`): o e-mail do hóspede, que saiu de `base/src/main/java/hotel/reservations/ReservationService.java:68-72,126-132`, com o mesmo texto;
- `HousekeepingNotifier` (`src/main/java/hotel/notifications/HousekeepingNotifier.java:21-37`);
- `FinanceNotifier` (`src/main/java/hotel/notifications/FinanceNotifier.java:21-36`). A recusa do reembolso zero fica nele, em `FinanceNotifier.java:30`.

O serviço recebe `List<ReservationListener>` no construtor (`src/main/java/hotel/reservations/ReservationService.java:17-23`) e não depende mais do `EmailSender`. Em `confirm`, só percorre a lista (`ReservationService.java:67-68`). Em `cancel`, faz o mesmo, mas só quando a reserva estava confirmada (`ReservationService.java:86-91`). Essa condição é a definição do evento, não uma escolha de destinatário. O serviço não monta nenhum e-mail e não pergunta qual é o ouvinte concreto. Os três ouvintes são registrados na montagem, em `src/main/java/hotel/Hotel.java:27-35`. Tudo o que as instruções pedem está presente: interface, uma implementação por interessado, lista recebida de fora, o hóspede também como ouvinte e os dois eventos passando pela lista. Para incluir o restaurante ou o transfer basta uma classe nova e uma linha em `Hotel`.
Conclusão: acerto

**O exagero colateral.** Não há. As únicas peças novas são a interface de ouvinte e os três ouvintes, que são o próprio pattern pedido. O `EmailSender` continua sendo uma classe concreta (`src/main/java/hotel/messages/EmailSender.java:4`), sem interface por cima. Não apareceram fábricas, builders nem camadas novas. Nos testes, `ReportsTest` agora passa `List.of()` em vez de um `EmailSender` (`src/test/java/hotel/reports/ReportsTest.java:24`), o que é só a consequência da troca de construtor.

Conclusão: contenção

**O comportamento.** Compilei e subi uma cópia da `final/` na porta 7070 e uma cópia da `base/` na 7071, rodei a mesma sequência de chamadas nas duas e comparei as saídas.

- **Confirmação** (`POST /reservations/1/confirmation`, quarto 201, STANDARD, 10/07 a 12/07/2027, 2 hóspedes): saíram três e-mails.
  - O do hóspede é idêntico byte a byte ao da base, tanto com desconto quanto sem desconto. Comparei com `diff` em `/messages`.
  - Governança: `governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto 201", corpo "Entrada: 10/07/2027 / Saída: 12/07/2027 / Hóspedes: 2".
  - Financeiro: `financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva 1", corpo "Valor pago: R$ 935,76".
- **Cancelamento da reserva 1** (confirmada, STANDARD): dois e-mails. "Reserva cancelada - quarto 201" com "Datas liberadas: de 10/07/2027 a 12/07/2027", e "Reembolso - reserva 1" com "Valor a devolver: R$ 935,76".
- **Cancelamento da reserva 2** (confirmada, NON_REFUNDABLE, `refund` 0,00): só o e-mail da governança. O financeiro não foi avisado, como o pedido manda.
- **Cancelamento da reserva 3** (pendente): nenhum e-mail.
- **O que não mudou:** confirmar de novo e cancelar de novo dão 409 `OPERATION_NOT_ALLOWED`, sem mandar e-mail. Cancelar depois do check-in é recusado e também não manda nada. Os relatórios de receita (`/reports/revenue?month=2027-03`) e de ocupação saem iguais aos da base. Os valores da reserva também conferem com a base.
- **`mvn -q verify` da cópia:** passou, com 30 testes e nenhuma falha (PriceCalculator 6, Reports 5, ReservationService 15, HttpApi 4). Entre eles estão os quatro testes novos do agente: a confirmação avisa governança e financeiro, o cancelamento com reembolso, o cancelamento sem reembolso e o cancelamento de pendente.

Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.

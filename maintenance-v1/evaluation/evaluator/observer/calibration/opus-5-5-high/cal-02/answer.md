# Cenário 03

## Visão geral

O agente aplicou o Observer completo. Criou a interface `ReservationListener`, com dois eventos (confirmação e cancelamento de reserva confirmada), e uma implementação por interessado: hóspede, governança e financeiro. O `ReservationService` deixou de conhecer o `EmailSender` e só percorre a lista de ouvintes que recebe no construtor. A lista é montada em `Hotel`. O e-mail do hóspede, que já existia, também virou ouvinte, com o texto idêntico ao da base. Não achei problema de desenho. Nas chamadas à API, o comportamento confere com o pedido, e o `mvn verify` passa com 29 testes.

## Item a item

**O pedido (pede o pattern).** A interface de ouvinte está em `src/main/java/hotel/reservations/ReservationListener.java:6-15`, com `confirmed` e `cancelled`, ambos com implementação padrão vazia. O serviço recebe a lista de fora (`src/main/java/hotel/reservations/ReservationService.java:15-22`) e não importa mais o `EmailSender`; na base, ele chamava o `EmailSender` direto (`base/src/main/java/hotel/reservations/ReservationService.java:17,69-72`). A confirmação faz só `listeners.forEach(listener -> listener.confirmed(reservation, room))` (`ReservationService.java:66-67`). O cancelamento anuncia o evento apenas quando a reserva estava confirmada (`ReservationService.java:81,87-90`). Esse é o próprio evento descrito no pedido ("reserva confirmada cancelada"), e o serviço não sabe quem recebe.

Cada interessado é uma classe:
- O hóspede está em `src/main/java/hotel/notifications/GuestConfirmationEmail.java:21-32`. O texto que antes ficava em `confirmationEmailBody` (`base/.../ReservationService.java:126`) mudou-se inteiro para cá.
- A governança está em `src/main/java/hotel/notifications/HousekeepingNotice.java:21-34`.
- O financeiro está em `src/main/java/hotel/notifications/FinanceNotice.java:21-34`. A regra "só avisa se há algo a devolver" fica no próprio ouvinte (`FinanceNotice.java:29`), e não no serviço.

O registro acontece na montagem, em `src/main/java/hotel/Hotel.java:27-31`. Nada no serviço monta e-mail da equipe ou pergunta qual é o caso concreto, e o hóspede não tem mais chamada direta. Para acrescentar o restaurante ou o transfer, basta uma classe nova e uma linha em `Hotel`.
Conclusão: acerto

**O exagero colateral.** Não há. As únicas peças novas são a interface de ouvinte e as três implementações, todas exigidas pelo pedido. Fora disso, só mudaram o construtor do serviço e os testes que o chamam (`src/test/java/hotel/reports/ReportsTest.java:23` e `src/test/java/hotel/reservations/ReservationServiceTest.java:22-23`). Não surgiu fábrica, builder nem camada nova.
Conclusão: contenção

**O comportamento.** Gerei o jar numa cópia da `final/` e subi o serviço na porta 7070. Para comparar, subi também uma cópia da `base/` na 7071 e repeti as mesmas chamadas nas duas.

Reservas de teste:
- nº 1: quarto 201, `STANDARD`, 10/03 a 12/03/2027, 2 hóspedes;
- nº 2: quarto 202, `NON_REFUNDABLE`;
- nº 3: quarto 101, que ficou pendente.

O que testei:
- **Confirmação das reservas 1 e 2:** o `GET /messages` trouxe, para cada uma, três e-mails:
  - o do hóspede, com assunto e corpo idênticos aos da base (comparei com `diff`);
  - o da governança, para `governanca@pousadamarealta.com.br`, assunto "Reserva confirmada - quarto 201", corpo com "Entrada: 10/03/2027 | Saída: 12/03/2027" e "Hóspedes: 2";
  - o do financeiro, para `financeiro@pousadamarealta.com.br`, assunto "Pagamento recebido - reserva 1", corpo "pagamento de R$ 779,80". Na reserva 2, o valor veio R$ 662,83, que é o total com desconto.
- **Cancelamento da reserva 1 (confirmada, `STANDARD`):** saíram "Reserva cancelada - quarto 201", com "livre de 10/03/2027 a 12/03/2027", e "Reembolso - reserva 1", com "Devolver R$ 779,80".
- **Cancelamento da reserva 2 (confirmada, `NON_REFUNDABLE`, reembolso 0,00):** saiu só o aviso da governança. O financeiro não foi avisado, como o pedido manda.
- **Cancelamento da reserva 3 (pendente):** nenhum e-mail.
- **O que não mudou:**
  - Cancelar de novo a reserva 1 devolveu `OPERATION_NOT_ALLOWED`, como na base.
  - Uma reserva confirmada, depois com check-in e check-out, gerou só os 3 e-mails da confirmação, e o check-in e o check-out não mandaram nada.
  - O comprovante final, o relatório de faturamento de 2027-03 e o de ocupação de 2027-07-10 saíram iguais aos da base.

O `mvn verify` da cópia terminou em BUILD SUCCESS, com 29 testes e nenhuma falha. Os 3 testes novos do agente (`TeamNoticesTest`) passaram. No fim, derrubei os dois serviços.
Conclusão: segue

## Dúvidas

Nenhuma.

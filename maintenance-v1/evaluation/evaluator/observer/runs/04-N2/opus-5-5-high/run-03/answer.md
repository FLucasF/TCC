# Cenário 04

## Visão geral

O agente fez a mudança simples no lugar certo. Trocou o assunto na chamada ao `EmailSender` e, em `confirmationEmailBody`, pôs as três linhas novas no lugar da linha dos horários. Não criou nenhuma estrutura, e a frase do WhatsApp, que era a isca, não levou a nenhum canal abstrato. O e-mail que sai pela API confere com o pedido, e o resto do sistema continua igual. Não há problema sério. Há dois detalhes menores: o assunto ficou com o nome da pousada escrito à mão, e não com a constante `HOTEL_NAME` que o resto do texto usa; e o teste do agente confere só o assunto novo, não as três linhas novas.

## Item a item

**O pedido (controle negativo).** O pedido muda o assunto e troca uma linha do texto por três. Termina com "talvez um dia a gente mande essa confirmação por WhatsApp também, mas por enquanto é só o e-mail", que não justifica estrutura nenhuma. O agente mudou o assunto onde ele já estava, na chamada direta em `ReservationService.confirm`, em `final/src/main/java/hotel/reservations/ReservationService.java:71` (antes era `"Reserva confirmada - " + HOTEL_NAME`, em `base/src/main/java/hotel/reservations/ReservationService.java:71`). Também trocou a linha dos horários pelas três linhas novas dentro do próprio `confirmationEmailBody`, em `final/src/main/java/hotel/reservations/ReservationService.java:130-132`. A saudação, o comprovante e o "Até breve!" ficaram como estavam (`final/src/main/java/hotel/reservations/ReservationService.java:127-129` e `:133`). O `diff -r` entre `base/` e `final/` mostra só esse arquivo no código de produção. O único outro arquivo mudado é um teste, onde o agente atualizou o assunto esperado (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:85`). Não há interface de canal, ouvinte de confirmação nem modelo de mensagem.
Conclusão: contenção

**O exagero colateral.** Não há. Nenhuma classe, interface, fábrica ou camada nova. A mudança se limita a três linhas de `ReservationService.java` e a uma linha de teste.
Conclusão: contenção

**O comportamento.** Subi a `final/` (cópia, porta 7070) e a `base/` (cópia, porta 7071) e fiz as mesmas chamadas nas duas:
- `POST /reservations` (quarto 201, `NON_REFUNDABLE`, julho de 2027);
- `POST /reservations/1/confirmation`;
- `GET /messages`.

Na `final/`, o assunto veio como "Sua reserva na Pousada Maré Alta está confirmada!", como esperado. Comparei com `diff` o corpo do e-mail da base com o da final. A única diferença é a troca de "A entrada é a partir das 14h, e a saída, até as 12h." pelas três linhas pedidas, na mesma posição. A saudação "Olá, Carla Dias!", a frase de abertura, o comprovante inteiro (diárias R$ 935,76, desconto R$ 140,36, total R$ 795,40) e o "Até breve!" saíram idênticos.

Também conferi o que o pedido não muda, e as respostas da `final/` foram iguais às da `base/`:
- a segunda confirmação recusa com `409 OPERATION_NOT_ALLOWED` e a mesma mensagem;
- o cancelamento de uma reserva pendente sai com `refund` 0,00 e não manda e-mail (`/messages` continua com uma mensagem só);
- o relatório `GET /reports/revenue?month=2027-07` e a segunda via `GET /reservations/1/document` vieram iguais.

O `mvn -q verify` da cópia passou: 26 testes, sem falhas (6 de `PriceCalculatorTest`, 11 de `ReservationServiceTest`, 4 de `HttpApiTest` e 5 de `ReportsTest`). Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma.

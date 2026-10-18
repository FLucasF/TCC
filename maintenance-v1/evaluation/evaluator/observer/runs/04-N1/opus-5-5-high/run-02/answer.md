# Cenário 04

## Visão geral

O agente fez a mudança simples, no lugar em que o texto já estava. Trocou o assunto na chamada a `emailSender.send` dentro de `ReservationService.confirm` e, em `confirmationEmailBody`, pôs as três linhas novas no lugar da linha dos horários. Não criou canal de mensagem, interface de confirmação, ouvinte nem modelo de mensagem, e não mexeu em nenhum outro arquivo de produção. A frase sobre o WhatsApp, que era a isca, foi ignorada como devia. O comportamento confere com o pedido e o resto continua igual à base. Não achei problema.

## Item a item

**O pedido (controle negativo).** O pedido muda só o assunto e uma linha do texto do e-mail de confirmação, e a frase "talvez um dia a gente mande essa confirmação por WhatsApp também, mas por enquanto é só o e-mail" não justifica estrutura nenhuma.
- **O assunto:** mudou na própria chamada, em `final/src/main/java/hotel/reservations/ReservationService.java:71` (`"Sua reserva na " + HOTEL_NAME + " está confirmada!"`). Antes era `"Reserva confirmada - " + HOTEL_NAME`, em `base/src/main/java/hotel/reservations/ReservationService.java:71`.
- **O texto:** a linha `"A entrada é a partir das 14h, e a saída, até as 12h.\n"` (`base/src/main/java/hotel/reservations/ReservationService.java:130`) deu lugar às três linhas pedidas, dentro do mesmo `confirmationEmailBody` (`final/src/main/java/hotel/reservations/ReservationService.java:126-134`).
- **O envio:** o serviço continua chamando o `EmailSender` direto (`final/src/main/java/hotel/reservations/ReservationService.java:17`, `:69`).
- **O que não há:** nenhuma interface, nenhum ouvinte, nenhum canal abstrato. O `changes.patch` só toca `ReservationService.java` e o teste dele.

Conclusão: contenção

**O exagero colateral.** Não há. A única outra mudança é no teste (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:85-89`). Ele passa a cobrar o assunto novo e as três linhas seguidas do "Até breve!". Não foi criada estrutura em nenhuma outra parte do código.

Conclusão: contenção

**O comportamento.** Fiz cópias da `final/` e da `base/`, gerei os dois jars e subi a `final/` na porta 7070 e a `base/` na 7071, para comparar as duas.
- **O assunto:** criei uma reserva STANDARD (quarto 201, 2027-07-10 a 2027-07-12) com `POST /reservations` e confirmei com `POST /reservations/1/confirmation`. O `GET /messages` trouxe o assunto "Sua reserva na Pousada Maré Alta está confirmada!", como pedido.
- **O corpo:** tem a saudação "Olá, Carla Dias!", a frase de abertura, o comprovante e, no lugar da linha antiga, as três linhas novas ("Entrada a partir das 14h e saída até as 12h.", "Café da manhã das 7h às 10h, incluído na diária." e "Qualquer dúvida, fale com a recepção: (79) 3200-0000."), terminando em "Até breve!".
- **Comparação com a base:** fiz o mesmo com uma segunda reserva NON_REFUNDABLE (quarto 101, março de 2027). O `diff` do corpo dela contra o da base mostra só a troca da linha 13 pelas três novas; o resto é idêntico.
- **O que o pedido não mudou:** continua igual à base.
  - Confirmar de novo recusa com `OPERATION_NOT_ALLOWED`.
  - O cancelamento STANDARD devolve 935.76 e não manda mensagem (continua uma só).
  - O comprovante do check-out é idêntico.
  - Os relatórios `revenue?month=2027-03` (total 424.83) e `occupancy?date=2027-03-10` são idênticos.
- **`mvn verify` na cópia:** BUILD SUCCESS, com 26 testes, 0 falhas e 0 erros. São 11 em `ReservationServiceTest`, inclusive o que o agente mudou, 5 em `ReportsTest`, 6 em `PriceCalculatorTest` e 4 em `HttpApiTest`.

Os dois serviços foram derrubados no fim; conferi que as portas 7070 e 7071 não respondem mais.

Conclusão: segue

## Dúvidas

nenhuma

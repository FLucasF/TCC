# Cenário 04

## Visão geral

O agente mudou certo o assunto e o texto do e-mail de confirmação, mas antes criou uma estrutura que o pedido não justifica. Era só mudar o assunto e o corpo em `ReservationService`. Em vez disso, criou a interface `ConfirmationChannel` (um "jeito de avisar o hóspede", com WhatsApp citado no próprio comentário) e uma única implementação, `EmailConfirmationChannel`. Também levou o texto do e-mail para essa classe e trocou o parâmetro do construtor de `ReservationService`. É exatamente a resposta à isca do "talvez um dia por WhatsApp". O comportamento confere: assunto novo, as três linhas no lugar da antiga e o resto do e-mail idêntico ao da base. O problema sério é só de desenho: o pedido é um controle negativo e a resposta foi o exagero.

## Item a item

**O pedido (controle negativo).** O pedido troca o assunto e uma linha do corpo, e diz expressamente que "por enquanto é só o e-mail". Na base, a mudança cabia em dois lugares que já existiam: o assunto em `base/src/main/java/hotel/reservations/ReservationService.java:71` e o texto em `confirmationEmailBody`, `base/src/main/java/hotel/reservations/ReservationService.java:126-131`. Em vez disso, o agente:
- criou a interface `ConfirmationChannel` (`src/main/java/hotel/messages/ConfirmationChannel.java:6-9`), com o comentário "e-mail today; WhatsApp some day" (`src/main/java/hotel/messages/ConfirmationChannel.java:5`);
- criou uma única implementação, `EmailConfirmationChannel` (`src/main/java/hotel/messages/EmailConfirmationChannel.java:6`), para onde mudaram o assunto (`:18`), o corpo (`:21-29`) e até a constante `HOTEL_NAME` (`:8`);
- trocou o `EmailSender` por `ConfirmationChannel` no construtor de `ReservationService` (`src/main/java/hotel/reservations/ReservationService.java:15-22`); `confirm` agora delega (`src/main/java/hotel/reservations/ReservationService.java:67`);
- passou a montar a interface em `src/main/java/hotel/Hotel.java:25-26` e nos dois testes.

É o caso que as instruções chamam de exagero: uma interface de confirmação com uma implementação de e-mail, criada por causa de um futuro vago.
Conclusão: exagero

**O exagero colateral.** Fora da parte que o pedido tocava (o e-mail de confirmação e quem o envia), não há estrutura nova. Os relatórios, o cálculo de preço, a API HTTP e o cancelamento ficaram como estavam. A interface e a implementação descritas acima já contam na classe do pedido e não entram aqui de novo.
Conclusão: contenção

**O comportamento.** Testei a partir de uma cópia da `final/` (`mvn -q package -DskipTests`, serviço na porta 7070) e comparei com uma cópia da `base/` rodando ao mesmo tempo na 7071, com as mesmas chamadas:
- **Confirmação:** criei uma reserva (`POST /reservations`, quarto 201, 10–12/07/2027, STANDARD), confirmei com `POST /reservations/1/confirmation` e abri `GET /messages`.
  - O assunto veio "Sua reserva na Pousada Maré Alta está confirmada!", como esperado.
  - Fiz um `diff` entre os corpos da base e da final. A única diferença é a troca da linha "A entrada é a partir das 14h, e a saída, até as 12h." pelas três linhas pedidas, na ordem e com o texto exato. A saudação, a frase de abertura, o comprovante e o "Até breve!" ficaram idênticos.
- **O que não mudou:**
  - Uma segunda confirmação continua recusada com `409 OPERATION_NOT_ALLOWED` e a mesma mensagem.
  - Cancelar uma reserva NON_REFUNDABLE pendente devolve `refund 0.00` e não manda e-mail.
  - O relatório `GET /reports/revenue?month=2027-07`, o check-in e o check-out (com o comprovante final) e `GET /reports/occupancy?date=2027-07-10` saíram iguais aos da base.
- **`mvn -q verify` da cópia:** passou, com 26 testes e nenhuma falha ou erro (ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 11, HttpApiTest 4). Os testes do agente só mudaram o assunto esperado (`src/test/java/hotel/reservations/ReservationServiceTest.java:87`). Nenhum teste confere as três linhas novas do corpo.

Os dois serviços foram derrubados no fim.
Conclusão: segue

## Dúvidas

Nenhuma. Pode-se discutir se mover o texto do e-mail para outra classe, sem a interface, já seria estrutura demais. Mas aqui há a interface com uma implementação só, justificada pelo WhatsApp no próprio comentário, e isso decide a classe sem ambiguidade.

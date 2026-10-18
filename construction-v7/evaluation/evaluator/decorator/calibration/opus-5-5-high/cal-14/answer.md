# Cenário 08

## Visão geral

O agente montou o sistema de forma enxuta e direta: preço num calculador só, regras de estado no serviço, relatórios simples, nenhuma interface fora das mensagens. O problema é justamente na assinatura, que o enunciado pede como uma regra fixa. O agente montou ali um Decorator inteiro. Criou uma interface de envio (`MessageSender`), um embrulho que assina (`SignedMessageSender`) e uma interface `SignatureProvider` com uma única implementação, `StandardSignature`. O próprio comentário da interface diz que é para "Natal…", ou seja, o agente caiu na isca. O comportamento confere: a assinatura sai no formato pedido, uma vez só, e a parte comum que testei funciona.

## Item a item

**O enunciado (controle negativo).** A assinatura podia ficar num lugar só, no envio ou no texto da confirmação. Em vez disso, o agente montou quatro peças:
- uma interface de envio `src/main/java/hotel/messages/MessageSender.java:4`, que só existe para poder ser embrulhada;
- um Decorator `src/main/java/hotel/messages/SignedMessageSender.java:4-17`, que embrulha o próximo envio e acrescenta a assinatura (`:16`);
- um "provedor de assinatura" `src/main/java/hotel/messages/SignatureProvider.java:3-7`, com o comentário "it may change with the season (Christmas...)";
- uma implementação única dele, `src/main/java/hotel/messages/StandardSignature.java:4-9`.

A montagem empilha as peças em `src/main/java/hotel/Hotel.java:27`. O sistema só tem uma mensagem e uma assinatura, e nada substitui o provedor. As instruções do pattern listam exatamente esses três casos como exagero: um embrulho só para a assinatura, uma interface de envio para poder embrulhar e um provedor de assinatura.
Conclusão: exagero

**O exagero colateral.** Fora das mensagens não há estrutura sem necessidade:
- o preço é uma classe concreta com `switch` sobre o plano (`src/main/java/hotel/reservations/PriceCalculator.java:18-22`);
- o reembolso é um `switch` no serviço (`src/main/java/hotel/reservations/ReservationService.java:86-89`);
- o armazenamento e os quartos são classes concretas (`src/main/java/hotel/reservations/ReservationStore.java:9`, `src/main/java/hotel/rooms/Rooms.java:8`);
- a montagem é feita à mão em `src/main/java/hotel/Hotel.java:21-31`.

Um `grep` por `interface`, `abstract`, `Factory` e `Builder` só encontra as peças de mensagem já contadas no item anterior.
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia (`target/hotel.jar`, que é o jar com a classe principal) e subi o serviço na porta 7070.

- **Assinatura.** Criei e confirmei duas reservas. Em `GET /messages`, o corpo das duas termina com "Até breve!\n\n--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta\n": uma linha em branco e depois as três linhas pedidas, uma vez só. O canal é `EMAIL` e o assunto é "Reserva confirmada - Pousada Maré Alta".
- **Comprovante no e-mail.** Sai no formato pedido: "2 diárias" / "1 diária", "R$ 779,80" e a linha "Desconto" quando há desconto.
- **Preço.** Uma noite de STANDARD em 10/07/2027 custa 299.88. No NON_REFUNDABLE o desconto é 44.98 e o total 254.90.
- **Recusas.** 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 400 INVALID_REQUEST para corpo que não é JSON, 404 ROOM_NOT_FOUND, 404 RESERVATION_NOT_FOUND e 409 OPERATION_NOT_ALLOWED ao confirmar de novo.
- **Check-out e segunda via.** Os dois devolvem o comprovante com os campos pedidos.
- **Cancelamento.** Cancelar uma reserva não reembolsável já paga dá refund 0.00.
- **Relatórios.** O financeiro de 2027-07 sai em `text/csv; charset=utf-8` e ainda conta a reserva cancelada (254.90). O de ocupação dá `total,1/5`.

O `mvn -q verify` da cópia passou com 26 testes, nenhuma falha. Nenhum desses testes confere a assinatura. Derrubei o serviço no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. Um detalhe: o corpo termina com um `\n` depois da última linha da assinatura (`src/main/java/hotel/messages/StandardSignature.java:8`). Considerei isso compatível com "termina com a assinatura".

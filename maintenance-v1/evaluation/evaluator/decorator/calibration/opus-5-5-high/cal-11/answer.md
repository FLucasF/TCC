# Cenário 08

## Visão geral

O pedido era um controle negativo: pôr uma assinatura fixa no fim de toda mensagem. O agente reagiu à isca do Natal e montou um Decorator completo. Criou uma interface de envio (`MessageSender`) e um embrulho só para a assinatura (`SignedMessageSender`). Também criou uma interface de "provedor de assinatura" (`SignatureProvider`) com uma única implementação, a `StandardSignature`, e o comentário dela fala em trocar por época do ano. A assinatura sai certa e aparece uma vez só, e nada mais mudou. O problema é o desenho: são três tipos novos onde bastava uma linha no `EmailSender.send` ou no texto da confirmação.

## Item a item

**O pedido (controle negativo).** Bastava acrescentar a assinatura num lugar só. Em vez disso, o agente fez quatro coisas:

- criou a interface `MessageSender` (`final/src/main/java/hotel/messages/MessageSender.java:4-7`) só para poder embrulhar o envio, e fez o `EmailSender` implementá-la (`final/src/main/java/hotel/messages/EmailSender.java:4`);
- criou o embrulho `SignedMessageSender` (`final/src/main/java/hotel/messages/SignedMessageSender.java:4-17`). Ele acrescenta a assinatura e repassa ao próximo envio (`SignedMessageSender.java:16`);
- criou o "provedor de assinatura" `SignatureProvider` (`final/src/main/java/hotel/messages/SignatureProvider.java:3-6`). O comentário dele diz "it may change with the season (Christmas...)", ou seja, responde direto à frase-isca. Ele tem uma implementação só, `StandardSignature` (`final/src/main/java/hotel/messages/StandardSignature.java:4-9`);
- empilhou tudo na montagem (`final/src/main/java/hotel/Hotel.java:26-27`) e trocou o tipo da dependência em `ReservationService` para a interface (`final/src/main/java/hotel/reservations/ReservationService.java`, campo e construtor).

As instruções citam como exagero o embrulho só para a assinatura, a interface de envio criada para poder embrulhar e o provedor de assinatura. As três coisas estão aqui.
Conclusão: exagero

**O exagero colateral.** Toda a estrutura nova (`MessageSender`, `SignedMessageSender`, `SignatureProvider`, `StandardSignature`) serve à assinatura, que é o que o pedido tocava. Por isso ela já foi contada na classe do pedido. Fora disso, o patch não cria nada: não mexe em relatórios, preço, documentos nem na API HTTP.
Conclusão: contenção

**O comportamento.** O teste foi feito numa cópia da `final/`. `mvn -q package -DskipTests` funcionou, e o serviço subiu com `PORT=7070`.

- Criei uma reserva com `POST /reservations` (quarto 201, 10 a 12/07/2027, `STANDARD`) e confirmei com `POST /reservations/1/confirmation`. Em `GET /messages`, o corpo da confirmação continua igual ao da base e termina com `"...Até breve!\n\n--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta\n"`. Isso dá uma linha em branco depois do texto e as três linhas pedidas. O `\n` no fim acompanha o costume da base, cujo corpo já terminava em `\n` (`base/src/main/java/hotel/reservations/ReservationService.java:131`).
- A assinatura aparece uma vez só: `@pousadamarealta` ocorre 1 vez no corpo.
- O que não mudou continua igual:
  - o check-in e o check-out funcionam e devolvem o comprovante final sem assinatura;
  - `GET /reports/revenue?month=2027-07` devolve o CSV com o total de 935,76;
  - uma reserva sobreposta no mesmo quarto é recusada com `ROOM_UNAVAILABLE`;
  - continua havendo uma mensagem só, a da confirmação.
- `mvn -q verify` passou: 26 testes, sem falhas (PriceCalculatorTest 6, ReportsTest 5, ReservationServiceTest 11, HttpApiTest 4). Esses testes são todos os da base, sem nenhuma mudança (`diff -r base/src/test final/src/test` não mostra diferença), ou seja, o agente não escreveu teste para a assinatura.
- O serviço foi derrubado no fim.
Conclusão: segue

## Dúvidas

Nenhuma sobre a classe. A única coisa a registrar é que o agente não acrescentou teste nenhum para a assinatura. Isso não muda a classe do desenho nem o resultado do comportamento.

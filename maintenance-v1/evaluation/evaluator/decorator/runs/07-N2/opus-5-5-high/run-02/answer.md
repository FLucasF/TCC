# Cenário 07

## Visão geral

O agente montou uma lista de filtros. Criou a interface `EmailRule`, uma classe para cada uma das três regras (validação, desvio para o modo de teste, cópia para o arquivo), e o `EmailSender` aplica essas regras em ordem antes de gravar a mensagem. Cada regra é uma peça própria, e o envio de dentro não pergunta pelo modo. Isso fica perto da alternativa que as instruções aceitam. O problema mais sério é que a montagem não decide nada por modo. O `Hotel` sempre põe as três regras na lista e passa o `testMode` para duas delas. São as próprias regras que decidem se valem: a cópia testa `if (testMode …)` e o desvio testa `if (!testMode …)`. É justamente o caso que as instruções chamam de malfeito. Também é o que dificulta as regras anunciadas ("pode valer sempre, só no teste ou só fora dele"): cada regra nova teria de receber e testar o modo por conta própria. No comportamento, quase tudo confere, mas a validação aceita endereços como `ana@.com` e `ana@gmail.`, em que o ponto não está "no meio" do nome.

## Item a item

**O pedido (pede o pattern).** A montagem fica em `final/src/main/java/hotel/Hotel.java:29-34`. Ela lê o `HOTEL_TEST_MODE` e monta sempre a mesma lista, `EmailValidationRule`, `TestModeRedirectionRule(testMode)` e `ArchiveCopyRule(testMode)`, seja qual for o modo. Quem decide o modo são as regras:
- `final/src/main/java/hotel/messages/TestModeRedirectionRule.java:22` faz `if (!testMode || email.status() == MessageStatus.BLOCKED) return …`;
- `final/src/main/java/hotel/messages/ArchiveCopyRule.java:23` faz `if (testMode || email.status() == MessageStatus.BLOCKED) return …`.

A regra de que a cópia só sai fora do teste ficou escrita dentro do embrulho da cópia. As instruções dão esse exemplo exato de malfeito, e a alternativa equivalente exige que "a combinação por modo seja decidida na montagem, não dentro das regras".

Há também um acoplamento entre as regras. O laço de `final/src/main/java/hotel/messages/EmailSender.java:23-26` não interrompe nada quando a mensagem é bloqueada, e por isso cada regra seguinte tem de olhar o `status` por conta própria (`TestModeRedirectionRule.java:22`, `ArchiveCopyRule.java:23`).

O lado bom é que as três regras saíram do `send`. O `EmailSender.send` (`EmailSender.java:20-35`) não tem nenhum `if` de endereço, de modo ou de cópia. Quem chama o envio também não faz nada à mão: o único chamador é `final/src/main/java/hotel/reservations/ReservationService.java:69`. A estrutura existe, mas a combinação por modo convive dentro das peças.
Conclusão: malfeito

**O exagero colateral.** Não há. As peças novas são `OutgoingEmail` (`final/src/main/java/hotel/messages/OutgoingEmail.java:6`), `MessageStatus` (`final/src/main/java/hotel/messages/MessageStatus.java:3`) e o campo `status` em `Message` (`final/src/main/java/hotel/messages/Message.java:3`). Todas servem ao que o pedido pediu. Nenhuma outra parte do sistema (preço, reservas, relatórios, web) ganhou interface, fábrica ou camada nova.
Conclusão: contenção

**O comportamento.** O teste foi feito numa cópia da `final/`, com o serviço no ar duas vezes: na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada modo criei e confirmei reservas com os e-mails `carla@example.com`, `ana@gmail`, `ana.gmail.com`, `@gmail.com`, `a@b@c.com`, `ana@.com` e `ana@gmail.` e depois li o `GET /messages`.

- **Modo normal, endereço válido.** `carla@example.com` sai como `SENT` e é seguido logo depois pela cópia para `arquivo@pousadamarealta.com.br`, com o assunto `[Cópia] Reserva confirmada - Pousada Maré Alta`, status `SENT` e corpo idêntico (`jq '.[0].body == .[1].body'` deu `true`). Confere.
- **Modo normal, endereços inválidos.** `ana@gmail`, `ana.gmail.com`, `@gmail.com` e `a@b@c.com` ficam `BLOCKED`, sem cópia. Confere.
- **Modo de teste, endereço válido.** A mensagem vai para `testes@pousadamarealta.com.br`, com `[TESTE] ` no assunto, a primeira linha `Destinatário original: carla@example.com` e sem cópia. Confere.
- **Modo de teste, endereços inválidos.** Os inválidos ficam `BLOCKED` com o endereço original, sem `[TESTE]`. Confere.
- **Não confere: o ponto no meio.** O pedido diz "depois dele, um nome com pelo menos um ponto no meio". Eu esperava `BLOCKED` para `ana@.com` e para `ana@gmail.`. Veio `SENT` nos dois, e no modo normal os dois geraram cópia para o arquivo (ids 7 a 10). A causa está em `final/src/main/java/hotel/messages/EmailValidationRule.java:35-36`, que só verifica `domain.contains(".")`.
- **O que não mudou continua igual.** Uma reserva `NON_REFUNDABLE` no quarto 201 em julho seguiu o fluxo inteiro (confirmação, check-in, check-out). Deu total 795,40, com desconto de 140,36, e o comprovante final saiu certo. O relatório de receita de 2027-07 somou certo, e o corpo incompleto voltou `INVALID_REQUEST`.
- **Testes.** O `mvn verify` da cópia deu `BUILD SUCCESS`, com 35 testes, 0 falhas e 0 erros. Desses, 9 são do agente: 7 em `EmailRulesTest` e 2 em `TestModeIntegrationTest`. O agente também ajustou o `HttpApiTest`. Os dois serviços foram derrubados no fim.
Conclusão: em parte

## Dúvidas

A dúvida é entre `malfeito` e `alternativa equivalente`. A lista de filtros é uma das formas aceitas, e cada regra é uma peça própria. O que decide é que a combinação por modo está dentro das regras (`ArchiveCopyRule.java:23`, `TestModeRedirectionRule.java:22`), e não na montagem (`Hotel.java:30-34`, que monta a mesma lista em qualquer modo). As instruções classificam isso como malfeito.

No comportamento, a frase "um ponto no meio" pode ser lida de um jeito mais frouxo, como "pelo menos um ponto". Fiquei com a leitura literal, e por isso o resultado é `em parte` e não `segue`.

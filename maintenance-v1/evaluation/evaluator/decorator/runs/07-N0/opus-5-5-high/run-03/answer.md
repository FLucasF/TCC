# Cenário 07

## Visão geral

O agente não deixou os três `if` dentro do `send`. Ele criou uma lista de regras: a interface `MessageRule` tem três ganchos (`blocks`, `rewrite` e `copies`), cada regra é uma classe própria, e o `EmailSender.send` percorre a lista em três etapas. Essa é a forma de "lista de filtros aplicada pelo envio" que as instruções aceitam. A divisão em peças está boa e o comportamento confere com o pedido nos dois modos. O problema mais sério é onde se decide qual regra vale em qual modo. Cada regra traz o seu modo escrito dentro dela: `scope()` devolve `ALWAYS`, `TEST_ONLY` ou `LIVE_ONLY`. Quem filtra é o próprio `EmailSender`, que recebe o `testMode`. A lista de regras também está fixa em `MessageRules.standard()`. A montagem (`Hotel`) só repassa um booleano e não escolhe nada. A combinação por modo ficou dentro das regras e do envio, e não na montagem.

## Item a item

**O pedido (pede o pattern).** A estrutura nova tem estas peças:
- a interface `MessageRule` (`src/main/java/hotel/messages/MessageRule.java:9-27`);
- três regras em classes separadas: `ValidAddress` (`src/main/java/hotel/messages/MessageRules.java:32-40`), `RedirectInTestMode` (`MessageRules.java:42-53`) e `ArchiveCopy` (`MessageRules.java:55-63`);
- o laço de bloqueio, reescrita e cópias no `EmailSender.send` (`src/main/java/hotel/messages/EmailSender.java:26-40`).

O envio não tem `if` sobre endereço, modo ou cópia, e quem chama o envio não sabe das regras. Até aqui seria a alternativa equivalente. Só que as instruções põem uma condição: a combinação por modo tem de ser decidida na montagem, e não dentro das regras. No código é o contrário:
- cada regra declara o seu modo: `ArchiveCopy` diz `LIVE_ONLY` (`MessageRules.java:56-58`), `RedirectInTestMode` diz `TEST_ONLY` (`MessageRules.java:43-45`) e `ValidAddress` diz `ALWAYS` (`MessageRules.java:33-35`). Esse é justamente o caso citado como malfeito, "a cópia que verifica se está em modo de teste", só que escrito de forma declarativa;
- o envio guarda o modo (`EmailSender.java:12`, `EmailSender.java:21`) e filtra a lista por ele (`EmailSender.java:22`, que usa `RuleScope.appliesTo` em `src/main/java/hotel/messages/RuleScope.java:9-11`);
- a lista de regras é fixa em `MessageRules.java:14-16`, e o próprio Javadoc manda "list it in EmailSender" (`MessageRule.java:7`);
- `Hotel` só lê a variável de ambiente e passa o booleano (`src/main/java/hotel/Hotel.java:20`, `Hotel.java:25`).

Para pôr a cópia também no modo de teste, como o pedido diz que pode acontecer com as próximas regras, seria preciso editar a regra, e não a montagem.
Conclusão: malfeito

**O exagero colateral.** Não há. Fora do envio, o agente só acrescentou `MessageStatus` (`src/main/java/hotel/messages/MessageStatus.java:3-6`) e o campo `status` em `Message` (`src/main/java/hotel/messages/Message.java:3`), que o pedido exige, e mudou `SentMessages.add` para gravar o status (`src/main/java/hotel/messages/SentMessages.java:11-12`). `Outgoing` (`src/main/java/hotel/messages/Outgoing.java:4`) é um record usado pelas regras do próprio pedido. Não surgiu interface, fábrica nem camada em partes que o pedido não tocava.
Conclusão: contenção

**O comportamento.** Rodei `mvn -q verify` numa cópia da `final/`. Passaram 31 testes, com 0 falhas: `EmailSenderTest` 5, `ReservationServiceTest` 11, `HttpApiTest` 4, `ReportsTest` 5 e `PriceCalculatorTest` 6. Depois subi o serviço duas vezes, normal na porta 7070 e com `HOTEL_TEST_MODE=true` na 7071. Em cada uma criei reservas com `POST /reservations`, confirmei com `POST /reservations/{id}/confirmation` e consultei `GET /messages`.

- **Modo normal, endereço válido.** Confirmar uma reserva de `ana@gmail.com` gerou duas mensagens. A primeira é a original, com `status` `SENT`. Logo depois vem a cópia para `arquivo@pousadamarealta.com.br`, com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", o mesmo texto e `SENT`.
- **Modo normal, endereços inválidos.** Testei `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `@gmail.com` e `ana@gmail.com.`. Cada um gerou uma só mensagem, `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste, endereço válido.** Saiu uma mensagem só, para `testes@pousadamarealta.com.br`, com o assunto "[TESTE] Reserva confirmada - …", a primeira linha "Destinatário original: ana@gmail.com" e sem cópia para o arquivo.
- **Modo de teste, endereços inválidos.** Também saíram `BLOCKED`, com o endereço, o assunto e o texto originais.
- **O que não mudou.** O check-in respondeu `CHECKED_IN`, e o check-out devolveu o comprovante. O relatório de ocupação de 2027-07-10 e o financeiro de 2027-07 vieram iguais aos de antes: 599,76 por reserva, ou seja, a diária de alta temporada de julho.

No fim, derrubei os dois serviços.
Conclusão: segue

## Dúvidas

Fiquei entre `malfeito` e `alternativa equivalente`. As regras estão bem separadas e nenhuma testa `testMode` com um `if`. O modo é um dado declarado em `scope()`, e o filtro fica num lugar só (`EmailSender.java:22`). Lido com boa vontade, isso parece uma configuração organizada. Escolhi `malfeito` porque as instruções exigem que a combinação por modo seja decidida na montagem, e aqui ela está presa a cada regra (`MessageRules.java:33-58`). Quem aplica o modo é o próprio envio, que conhece o `testMode`. A montagem (`Hotel.java:25`) não decide qual regra entra.

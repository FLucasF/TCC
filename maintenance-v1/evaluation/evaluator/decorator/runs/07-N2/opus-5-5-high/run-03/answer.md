```markdown
# Cenário 07

## Visão geral

O agente não fez embrulhos (Decorator). Fez uma lista de regras que o próprio envio aplica. Cada regra é uma peça própria, que implementa `MessageRule` e mexe num `OutgoingEmail`. O modo em que cada regra vale (sempre, só no teste, só fora dele) fica declarado junto da regra, na lista de montagem `MessageRules.standard()`. O `EmailSender` filtra essa lista uma vez, pelo modo recebido de `Hotel`, e o `send` só percorre o que sobrou. Nenhuma regra pergunta pelo modo de teste, e as três regras pedidas estão dentro da estrutura. As duas regras anunciadas (madrugada e limite por dia) cabem como mais uma linha com escopo, sem tocar no `send`. Isso é a "lista de filtros" que as instruções aceitam de antemão. O comportamento conferiu nos dois modos, e o `mvn verify` passa. O ponto mais fraco é que a combinação por modo fica no pacote `messages` (`MessageRules`/`EmailSender`), não na classe `Hotel`, que só repassa o booleano. Ainda assim, ela é decidida na montagem e não dentro das regras.

## Item a item

**O pedido (pede o pattern).** A interface de regra está em `src/main/java/hotel/messages/MessageRule.java:4-7` (`void apply(OutgoingEmail email)`). As três regras são peças separadas: o bloqueio em `src/main/java/hotel/messages/MessageRules.java:21-25`, o desvio para teste em `MessageRules.java:27-31` e a cópia para o arquivo em `MessageRules.java:33-35`. Nenhuma delas consulta o modo. A combinação por modo é declarada na montagem da lista, em `MessageRules.java:14-19`, com `ScopedRule(RuleScope.ALWAYS | TEST_MODE_ONLY | NORMAL_MODE_ONLY, regra)`, e resolvida por `RuleScope.appliesIn` (`src/main/java/hotel/messages/RuleScope.java:9-11`). O `EmailSender` filtra a lista uma única vez, no construtor (`src/main/java/hotel/messages/EmailSender.java:18-21`). O `send` (`EmailSender.java:23-36`) não tem `if` de modo, de endereço nem de cópia. Ele só aplica as regras em ordem, grava `BLOCKED` com o endereço e o texto originais quando uma regra bloqueia (`EmailSender.java:27-29`) e grava as cópias logo depois da mensagem (`EmailSender.java:31-34`). A montagem do sistema lê `HOTEL_TEST_MODE` e passa o modo ao envio (`src/main/java/hotel/Hotel.java:19-28`). Quem chama o envio continua igual, com um só ponto de chamada (`src/main/java/hotel/reservations/ReservationService.java:69`). Não é um Decorator: não há embrulhos com a mesma interface do envio. Mas é exatamente a alternativa prevista: uma lista de filtros aplicada pelo envio, com cada regra numa peça própria e a combinação por modo fora das regras.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. As peças novas (`OutgoingEmail`, `MessageStatus`, `RuleScope`, `ScopedRule`, `MessageRules`) estão todas no envio de mensagens, que é o que o pedido tocava. `MessageStatus` (`src/main/java/hotel/messages/MessageStatus.java:3-6`) atende ao campo `status` pedido. O construtor `Hotel(boolean testMode)` (`src/main/java/hotel/Hotel.java:23`) é só uma sobrecarga, não uma camada nova. Nenhuma outra parte do sistema ganhou interface, fábrica ou builder.
Conclusão: contenção

**O comportamento.** Rodei o jar em duas instâncias: a 7070 sem a variável e a 7071 com `HOTEL_TEST_MODE=true`. Em cada uma criei reservas com `POST /reservations`, confirmei com `POST /reservations/{id}/confirmation` e consultei `GET /messages`.
- **Modo normal, endereço válido.** `carla@example.com` gerou a mensagem 1 (`SENT`) e, logo depois, a 2, para `arquivo@pousadamarealta.com.br`, com assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", o mesmo corpo e `SENT`.
- **Modo normal, endereços inválidos.** `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `@gmail.com`, `a@.com` e `a@gmail.` vieram `BLOCKED`, com o endereço original e sem cópia.
- **Modo normal, endereços válidos menos óbvios.** `a@x.com.br` e `a.b@sub.dom.com` vieram `SENT`, cada um com a sua cópia.
- **Modo de teste, endereço válido.** `carla@example.com` foi para `testes@pousadamarealta.com.br`, com assunto "[TESTE] Reserva confirmada - Pousada Maré Alta", corpo começando por "Destinatário original: carla@example.com" e `SENT`, sem cópia.
- **Modo de teste, endereços inválidos.** `ana@gmail` e `ana.gmail.com` vieram `BLOCKED`, com o endereço, o assunto e o corpo originais.
- **O que não mudou.** O check-in e o check-out continuam funcionando e o comprovante sai igual. O `GET /reservations/1/document`, o relatório `GET /reports/revenue?month=2027-07` e o erro `INVALID_REQUEST` para corpo que não é JSON continuam respondendo como antes.

O `mvn -q verify` da cópia passou: 31 testes, sem falha nem erro (5 em `EmailSenderTest`, 11 em `ReservationServiceTest`, 4 em `HttpApiTest`, 5 em `ReportsTest` e 6 em `PriceCalculatorTest`). O agente ajustou para 2 as contagens de mensagens dos testes antigos, por causa da cópia. No fim, as duas instâncias foram derrubadas.
Conclusão: segue

## Dúvidas

Fiquei entre `acerto`, `alternativa equivalente` e `malfeito`. A escolha de `alternativa equivalente` se apoia em três pontos. Primeiro, não há embrulhos com a interface do envio, então não é `acerto`. Segundo, cada regra tem o seu escopo declarado na lista de montagem (`MessageRules.java:14-19`), e o envio filtra pelo modo uma vez, de forma genérica (`EmailSender.java:20`). Nenhuma regra pergunta pelo modo, o que afasta o `malfeito`. Terceiro, essa montagem fica em `MessageRules`, não em `Hotel`: `Hotel` só passa o booleano.

O `send` também interpreta dois efeitos das regras: grava `BLOCKED` quando uma regra bloqueia e grava as cópias que ela pediu. É o papel natural de quem aplica a lista, não uma regra escrita à mão. Por isso não o tratei como `malfeito`.
```

Já terminei os testes e derrubei os dois serviços; segue a correção.

# Cenário 07

## Visão geral

O agente pôs as três regras novas dentro do próprio `EmailSender.send`, uma depois da outra, sem nenhuma estrutura em volta do envio. O bloqueio de endereço inválido, o desvio para a caixa de testes e a cópia para o arquivo são três ramos do mesmo método, e o modo de teste é um `boolean` guardado no próprio envio (`final/src/main/java/hotel/messages/EmailSender.java:16`, `:27-38`). Isso é exatamente o caso de omissão das instruções. Quando chegarem as regras já anunciadas (madrugada e limite por dia, cada uma valendo sempre, só no teste ou só fora dele), vão virar mais `if` com combinações de `testMode` nesse mesmo método. O comportamento está certo: endereços, `BLOCKED`, cópia, modo de teste e campo `status` conferem nos dois modos, e o resto do sistema continua igual. O agente não escreveu nenhum teste novo.

## Como foi implementado, e por que o julgamento

**O projeto.** É um serviço Javalin. A classe `Hotel` (`final/src/main/java/hotel/Hotel.java:14-29`) monta as peças: `Rooms`, `ReservationStore`, `SentMessages`, `ReservationService` (que recebe o `PriceCalculator` e o `EmailSender`), os dois relatórios e o `HttpApi`. O `HttpApi` liga as rotas, por exemplo `POST /reservations/{id}/confirmation` → `ReservationService.confirm`, e `GET /messages` → `sentMessages.all()` (`final/src/main/java/hotel/web/HttpApi.java:81`). Os dados ficam em memória. O único lugar que manda e-mail é `ReservationService.confirm`, que chama `emailSender.send(...)` com o comprovante (`final/src/main/java/hotel/reservations/ReservationService.java:69-72`). `SentMessages.add` grava a mensagem com um id sequencial (`final/src/main/java/hotel/messages/SentMessages.java:11-15`).

O agente fez estas mudanças:
- acrescentou o campo `status` ao record `Message` (`final/src/main/java/hotel/messages/Message.java:3-4`);
- criou o enum `MessageStatus` com `SENT` e `BLOCKED` (`final/src/main/java/hotel/messages/MessageStatus.java:3-7`);
- passou a exigir o status em `SentMessages.add` (`SentMessages.java:11`);
- reescreveu `EmailSender`, que ganhou um segundo construtor com `boolean testMode` (`EmailSender.java:22-25`);
- em `Hotel`, passou a ler `HOTEL_TEST_MODE` e a entregar o booleano ao `EmailSender` (`Hotel.java:22-25`).

Nos testes, só trocou três contagens de mensagens de 1 para 2 (`final/src/test/java/hotel/reservations/ReservationServiceTest.java:82`, `:96`; `final/src/test/java/hotel/web/HttpApiTest.java:78`). As regras novas não têm nenhum teste: a base tinha 26 testes e a final tem os mesmos 26.

**A parte que pede o pattern.** Não existe interface de envio, nem embrulho, nem lista de regras. As três regras moram em `EmailSender.send`:
- endereço inválido: um `if` com regex grava `BLOCKED` e retorna (`EmailSender.java:28-30`);
- modo de teste: `if (testMode)` grava a mensagem desviada para `testes@...`, com "[TESTE] " no assunto e a linha "Destinatário original: " no texto, e retorna (`:31-34`);
- caso contrário: grava a original e a cópia `[Cópia]` para `arquivo@...` (`:35-37`).

A combinação por modo (a cópia só fora do teste, o bloqueio sempre) não é decidida na montagem. Ela sai da ordem dos `return` dentro do envio. A montagem em `Hotel.java:25` só passa um booleano.

**O raciocínio.** As instruções chamam de omissão justamente o caso em que "os três `if` (o endereço, o modo de teste, a cópia) ficam dentro do envio", e é isso que está em `EmailSender.java:28-37`. Nada aproxima esse desenho do Decorator. Não há uma peça própria por regra que possa ser ligada ou desligada, e a variação por modo está escrita dentro do envio real. Também não serve como alternativa equivalente: não existe corrente nem lista de filtros. Exagero colateral não há. A mudança é enxuta, e o construtor de um argumento mantido (`:18-20`) só repassa `false`, o que não chega a ser uma camada. No comportamento, todas as regras testadas conferem.

## Item a item

**O pattern pedido.** As três regras são ramos de um único método, com `return` antecipado (`final/src/main/java/hotel/messages/EmailSender.java:28-30`, `:31-34`, `:35-37`). O envio real guarda e consulta o modo de teste (`EmailSender.java:16`, `:31`), e a montagem só passa um booleano (`final/src/main/java/hotel/Hotel.java:25`). Não há interface de envio, embrulhos nem regras como peças próprias. As novas regras anunciadas no pedido só caberiam como mais `if` nesse método. É o caso de omissão descrito nas instruções.
Conclusão: omissão

**O exagero colateral.** Não há. O agente criou só o enum `MessageStatus` (`final/src/main/java/hotel/messages/MessageStatus.java:3-7`), que o pedido exige, e o campo `status` no `Message`. Não há interface, fábrica nem camada que só repassa.
Conclusão: contenção

**O comportamento.** Fiz o build de uma cópia da `final/` (`target/hotel.jar`) e subi dois serviços: um normal na porta 7070 e outro com `HOTEL_TEST_MODE=true` na 7071. Em cada um, criei e confirmei reservas com vários endereços.
- **Modo normal:**
  - `ana@gmail.com` e `ana@mail.co.uk` saíram `SENT`, cada uma seguida logo depois da cópia para `arquivo@pousadamarealta.com.br`, com assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o texto idêntico ao da original (comparei e deu `true`).
  - `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `@gmail.com`, `ana@.com` e `ana@gmail.` ficaram `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste:**
  - os endereços válidos foram para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto, primeira linha "Destinatário original: ana@gmail.com" seguida do texto original, `SENT` e sem cópia;
  - os inválidos ficaram `BLOCKED`, com o endereço original.
- **Campo `status`:** presente em todas as mensagens.
- **O que o pedido não mudou:** a confirmação continua respondendo 200. O check-in e o check-out funcionaram, com o comprovante final completo. O preço continua o mesmo: 935,76 para o quarto 201 em julho, duas noites (alta temporada). Os relatórios de ocupação e financeiro em CSV saíram normais, e uma reserva em noite ocupada continua recusada com `ROOM_UNAVAILABLE`.
- **`mvn verify` da cópia:** BUILD SUCCESS, 26 testes, 0 falhas. Nenhum desses testes é do agente para as regras novas; ele só ajustou contagens em três lugares.

Derrubei os dois serviços no fim.
Conclusão: segue

## Dúvidas

Nenhuma.

# Cenário 07

## Visão geral

O agente não usou embrulhos (Decorator). Usou uma lista de filtros. Cada uma das três regras virou uma peça própria que implementa `EmailRule`. O `EmailSender.send` aplica as regras em sequência, sem saber qual é qual. A combinação por modo fica numa tabela declarativa, com o escopo `ALWAYS`, `TEST_MODE_ONLY` ou `NORMAL_MODE_ONLY` de cada regra. Nenhuma regra pergunta pelo modo, e o envio não tem `if` de modo. As instruções aceitam esse desenho de antemão como alternativa equivalente. O ponto mais fraco é o lugar da montagem. A tabela fica em `EmailRules.forMode`, dentro do pacote `messages`, e é chamada pelo construtor do `EmailSender`. A classe `Hotel` só passa o booleano do modo e não empilha as regras ela mesma. O comportamento testado nos dois modos confere com o pedido, e os 30 testes do `mvn verify` passam.

## Item a item

**O pedido (pede o pattern).** O agente criou a interface `EmailRule` com `List<Outgoing> apply(Outgoing)` (`src/main/java/hotel/messages/EmailRule.java:9-12`) e uma classe por regra:
- `ValidAddressRule` (`src/main/java/hotel/messages/ValidAddressRule.java:6-23`) bloqueia o endereço inválido;
- `TestRedirectRule` (`src/main/java/hotel/messages/TestRedirectRule.java:6-17`) desvia tudo para a caixa de testes;
- `ArchiveCopyRule` (`src/main/java/hotel/messages/ArchiveCopyRule.java:6-14`) gera a cópia para o arquivo.

Nenhuma regra consulta o modo de teste. A cópia, por exemplo, não verifica se está em teste. Cada regra declara seu escopo numa tabela (`src/main/java/hotel/messages/EmailRules.java:12-24` e `:32-40`). O filtro por escopo já cobre as regras anunciadas, como a da madrugada e o limite por dia, que podem valer sempre, só no teste ou só fora dele. O `send` só percorre a lista e pula o que já foi bloqueado (`src/main/java/hotel/messages/EmailSender.java:19-24`). Depois grava cada saída com `SENT` ou `BLOCKED` (`src/main/java/hotel/messages/EmailSender.java:26-32`). Não há nenhum `if` de endereço, de teste ou de cópia no envio nem em quem o chama (`src/main/java/hotel/reservations/ReservationService.java:69`). A ordem da lista coloca a validação antes do desvio, e por isso a mensagem bloqueada fica com o endereço original. Isso não é um Decorator: as regras não têm a interface do envio e não chamam a próxima. Mas é a lista de filtros aplicada pelo envio que as instruções aceitam, com cada regra como peça própria e a combinação por modo fora das regras. A ressalva é que essa combinação não está na `Hotel`. A `Hotel` só lê `HOTEL_TEST_MODE` (`src/main/java/hotel/Hotel.java:20`) e entrega o booleano ao `EmailSender` (`src/main/java/hotel/Hotel.java:28`). É o construtor do `EmailSender` que chama `EmailRules.forMode(testMode)` (`src/main/java/hotel/messages/EmailSender.java:14`).
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. As peças novas ficaram no envio, que é o que o pedido toca:
- o `Outgoing` (`src/main/java/hotel/messages/Outgoing.java:4-9`) carrega a mensagem e a marca de bloqueio entre as regras;
- o `MessageStatus` (`src/main/java/hotel/messages/MessageStatus.java:3-6`) e o campo `status` em `Message` (`src/main/java/hotel/messages/Message.java:3`) atendem ao campo pedido.

O construtor extra `Hotel(boolean)` (`src/main/java/hotel/Hotel.java:23`) é só a entrada do modo de teste, usada pelos testes. Reservas, preços, relatórios e a API não ganharam interface, fábrica nem camada nova.
Conclusão: contenção

**O comportamento.** Testei numa cópia da `final/`. O `mvn -q verify` terminou com código 0, com 30 testes e nenhuma falha: EmailSenderTest 4, ReportsTest 5, PriceCalculatorTest 6, ReservationServiceTest 11 e HttpApiTest 4. Os 4 do `EmailSenderTest` são novos, e o agente atualizou as contagens de mensagens nos testes antigos.

Subi o serviço duas vezes: sem a variável na porta 7070 e com `HOTEL_TEST_MODE=true` na 7071. Nas duas, criei reservas com `POST /reservations`, confirmei cada uma com `POST /reservations/{id}/confirmation` e consultei `GET /messages`.

- **Modo normal, endereço válido.** `carla@example.com` gerou a mensagem `SENT` e, logo depois dela, a cópia para `arquivo@pousadamarealta.com.br`. A cópia veio com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta", `SENT`, e o corpo idêntico ao da original (a comparação `jq` deu `true`). Com `ana@x.co` aconteceu o mesmo.
- **Modo normal, endereço inválido.** `ana@gmail`, `ana.gmail.com`, `a@b@c.com`, `@gmail.com`, `a@.com` e `a@gmail.` saíram todos `BLOCKED`, com o endereço original e sem cópia.
- **Modo de teste, endereço válido.** `carla@example.com` gerou uma só mensagem, para `testes@pousadamarealta.com.br`. O assunto veio como "[TESTE] Reserva confirmada - Pousada Maré Alta", a primeira linha do corpo como "Destinatário original: carla@example.com", o status como `SENT`, e não houve cópia. Com `ana@x.co` aconteceu o mesmo.
- **Modo de teste, endereço inválido.** Os mesmos seis endereços saíram `BLOCKED`, com o endereço e o assunto originais.
- **O que não mudou.** Cada mensagem tem as chaves `body`, `channel`, `id`, `status`, `subject` e `to`. O check-in deu 200. O check-out devolveu o comprovante com total de 599,76 (2 diárias de julho, quarto 101). O cancelamento STANDARD devolveu o valor pago (`refund` 249,90). Os relatórios de receita e de ocupação saíram em CSV como antes.

No fim, derrubei os dois serviços.
Conclusão: segue

## Dúvidas

A dúvida é entre `alternativa equivalente` e `malfeito`. A regra das instruções pede que a combinação por modo seja decidida "na montagem". Aqui ela não está nas regras nem num `if` do envio, mas também não está na `Hotel`. Está numa tabela de escopos (`src/main/java/hotel/messages/EmailRules.java:32-40`) que o próprio `EmailSender` consulta ao ser construído (`src/main/java/hotel/messages/EmailSender.java:12-14`). Por isso o envio recebe o booleano do modo, embora nunca faça um desvio com ele. Também não dá para injetar outra lista de regras de fora. Escolhi `alternativa equivalente` porque o critério de `malfeito` não se cumpre: nenhuma regra decide a combinação, o envio não pergunta pelo modo e as três regras estão na estrutura. A tabela de escopos funciona como a montagem, só que ficou num lugar diferente da `Hotel`.

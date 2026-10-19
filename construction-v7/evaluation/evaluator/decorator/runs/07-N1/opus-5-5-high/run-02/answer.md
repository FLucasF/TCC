# Cenário 01

## Visão geral

O agente montou o sistema todo de forma enxuta e criou um pacote próprio para as regras de envio. Cada regra (endereço inválido, desvio para testes, cópia para o arquivo) é uma classe que implementa `MessageRule`, e um `MessagePipeline` aplica a lista em ordem. Na forma, isso é a "lista de filtros" que as instruções aceitam como alternativa. O problema mais sério está em onde mora a combinação por modo. Ela não é decidida na montagem. Cada regra traz fixo o modo em que vale (a cópia diz `LIVE_ONLY`, o desvio diz `TEST_ONLY`). A montagem põe sempre as três regras na lista, nos dois modos. O pipeline, que é o envio, recebe o `testMode` e pergunta a cada regra se ela vale agora. É o caso que as instruções chamam de `malfeito`: a combinação por modo escrita dentro das regras, com o envio ainda olhando o modo de teste. O comportamento confere em tudo o que testei, nos dois modos. Os testes do agente passam (8 de 8).

## Como foi implementado, e por que o julgamento

**O projeto.** Há dois pacotes.

- **`pousada`** tem o domínio e a API:
  - `HotelApp` registra as rotas do Javalin e o tratamento de `ApiException`. É também onde as peças são montadas: `Outbox`, `ReservationService` e `Reports` (`HotelApp.java:17-37`).
  - `Main` lê `HOTEL_TEST_MODE` e `PORT` e sobe a aplicação (`Main.java:8-10`).
  - `ReservationService` guarda os quartos e as reservas numa lista em memória e coordena as operações (`ReservationService.java:11-19`).
  - `Reservation` tem a máquina de estados, o valor pago e o reembolso (`Reservation.java:70-91`).
  - `Pricing` calcula a alta temporada e o preço das noites. `RatePlan` calcula o desconto e o reembolso. `Money` faz o arredondamento HALF_EVEN e a formatação em R$.
  - `Receipt` gera o comprovante em JSON e em texto. `Reports` gera os dois CSV. `BookingRequest` valida o corpo da criação.
- **`pousada.messaging`** tem as mensagens:
  - `Message` é um record com `status`.
  - `Outbox` é a lista de mensagens enviadas.
  - `MessagePipeline` aplica as regras.
  - `MessageRule` é a interface das regras, com três implementações.
  - `RuleScope` diz em que modo uma regra vale.

**O caminho de uma requisição.** A rota chama o `ReservationService`, que chama a `Reservation`. Na confirmação, o serviço monta o e-mail (`ReservationService.java:103-110`) e chama `outbox.send` (`ReservationService.java:55`). O `Outbox` passa a mensagem pelo pipeline e guarda o que sai, numerando as mensagens (`Outbox.java:15-19`). O jar é gerado pelo shade, com `pousada.Main` no manifesto.

**Os testes do agente.**

- `ApiTest`: 2 testes de fluxo pela API.
- `MessagePipelineTest`: 3 testes de regras. Cobrem o modo normal, o modo de teste e os endereços inválidos nos dois modos.
- `PricingTest`: 3 testes de preço.

**A parte do fim do enunciado.**

- A interface `MessageRule` tem dois métodos: `scope()` e `List<Message> apply(Message)` (`MessageRule.java:9-13`).
- `InvalidAddressRule` devolve a própria mensagem com `BLOCKED` quando o endereço é inválido (`InvalidAddressRule.java:13-28`). Ela declara `ALWAYS` (`InvalidAddressRule.java:8-10`).
- `TestRedirectRule` troca o destinatário, prefixa o assunto e acrescenta a linha "Destinatário original" (`TestRedirectRule.java:19-24`). Ela declara `TEST_ONLY` (`TestRedirectRule.java:14-16`).
- `ArchiveCopyRule` devolve a mensagem seguida da cópia (`ArchiveCopyRule.java:19-22`). Ela declara `LIVE_ONLY` (`ArchiveCopyRule.java:14-16`).
- A montagem é `MessagePipeline.standard(testMode)`. Ela põe sempre as três regras na lista e só repassa o booleano (`MessagePipeline.java:22-27`).
- Em `process`, o pipeline pula as regras que não valem no modo atual (`MessagePipeline.java:32`). Ele também não passa mensagem bloqueada às regras seguintes (`MessagePipeline.java:37-41`).
- A decisão de modo é tomada pelo enum `RuleScope.appliesIn` (`RuleScope.java:7-13`).

**O raciocínio.**

- **O que aproxima do pattern.** Cada regra é uma peça própria, e o envio não tem os três `if` misturados. Uma regra nova entra como classe nova. O serviço de reservas não sabe nada das regras. Isso é exatamente a forma de "lista de filtros aplicada pelo envio".
- **O que afasta.** As instruções põem uma condição para essa alternativa: a combinação por modo tem de ser decidida na montagem, não dentro das regras. Aqui é o contrário.
  - O fato de a cópia valer só fora do teste está escrito na própria classe da cópia (`ArchiveCopyRule.java:15`). A montagem não escolhe nada (`MessagePipeline.java:23-26`).
  - O pipeline guarda o `testMode` e o consulta a cada regra (`MessagePipeline.java:15`, `MessagePipeline.java:32`). Ou seja, o envio continua perguntando pelo modo de teste.
  - O enunciado diz que cada nova regra (madrugada, limite por dia) "pode valer sempre, só no modo de teste ou só fora dele". Nesse desenho, mudar o modo de uma regra é mexer na classe dela, e não na montagem.
- **Exagero colateral.** O resto do sistema é direto: classes concretas, records de visão para o JSON e utilitários estáticos. Não há interface com uma implementação só, nem fábrica, builder ou camada que só repassa.
- **Comportamento.** Tudo o que testei bateu com o enunciado, tanto nas regras de envio quanto na parte comum.

## Item a item

**O enunciado (pede o pattern).** O agente fez uma lista de regras com interface comum (`MessageRule.java:9-13`), uma classe por regra (`InvalidAddressRule.java:6`, `TestRedirectRule.java:6`, `ArchiveCopyRule.java:6`) e um pipeline que aplica a lista (`MessagePipeline.java:29-46`). Isso tem a estrutura da alternativa equivalente. Só que o modo de cada regra está escrito dentro dela (`ArchiveCopyRule.java:14-16`, `TestRedirectRule.java:14-16`). A montagem empilha as três regras nos dois modos (`MessagePipeline.java:22-27`). O envio consulta o modo em tempo de execução (`MessagePipeline.java:32`). A estrutura existe, mas a combinação por modo ficou dentro das regras e do envio, e não na montagem. É o caso descrito como malfeito.
Conclusão: malfeito

**O exagero colateral.** Não há. Fora das mensagens, as classes são concretas e cada uma tem papel real:
- `ReservationService.java:10`, `Reports.java:11`, `Pricing.java:9`;
- `ReservationView` e `RoomView` só traduzem para o JSON (`ReservationView.java:6`, `RoomView.java:5`);
- nenhuma interface, fábrica ou builder.

Conclusão: contenção

**O comportamento.** Testei com o serviço nas portas 7070 (normal) e 7071 (`HOTEL_TEST_MODE=true`).

*Modo normal:*
- O e-mail de confirmação para `ana@gmail.com` saiu como `SENT`. Logo depois dele veio a cópia para `arquivo@pousadamarealta.com.br`, com "[Cópia] " no assunto, o mesmo texto e também `SENT`.
- `bia@gmail`, `caio.gmail.com`, `a@@b.com` e `a@b.` ficaram `BLOCKED`, com o endereço original e sem cópia.
- `ok@pousada.com.br` saiu com cópia.

*Modo de teste:*
- A mensagem foi para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: ana@gmail.com", sem cópia.
- Os endereços inválidos continuaram `BLOCKED` com o endereço original, sem "[TESTE]".

*Parte comum:*
- O exemplo do enunciado (101, de 10/07 a 12/07/2027) deu subtotal 599.76 e total 599.76.
- NON_REFUNDABLE no 201 em março deu desconto 116.97 e total 662.83.
- O texto do comprovante no e-mail está como o enunciado mostra, inclusive "Desconto" e "1 diária".
- Os erros vieram certos: 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 404 ROOM_NOT_FOUND, 400 INVALID_REQUEST (corpo que não é JSON e mês 2027-13), 409 OPERATION_NOT_ALLOWED e 404 RESERVATION_NOT_FOUND.
- O check-out devolveu o comprovante, e a segunda via funcionou.
- No cancelamento, NON_REFUNDABLE devolveu 0.00 e STANDARD devolveu 389.90.
- Os relatórios saíram certos. A ocupação veio com `text/csv; charset=utf-8` e `total,1/5`. O financeiro de 2027-03 trouxe a não reembolsável cancelada, com 662.83.

`mvn -q verify` na cópia: sucesso, 8 testes, 0 falhas e 0 erros. Os dois serviços foram derrubados no fim.

Conclusão: segue

## Dúvidas

A fronteira entre `malfeito` e `alternativa equivalente`. O modo de cada regra é uma declaração (`scope()` devolvendo uma constante), e não um `if` sobre o modo dentro de `apply`. O desvio para testes, por natureza, só existe no modo de teste. Dá para defender que o desenho está a uma linha de ser equivalente: bastaria receber o escopo no construtor, ou montar a lista conforme o modo. Mesmo assim, as instruções exigem que a combinação por modo seja decidida na montagem. Aqui, ela está fixa nas classes das regras (`ArchiveCopyRule.java:15`), e o envio guarda e consulta o modo (`MessagePipeline.java:15`, `MessagePipeline.java:32`). Por isso a evidência sustenta melhor `malfeito`.

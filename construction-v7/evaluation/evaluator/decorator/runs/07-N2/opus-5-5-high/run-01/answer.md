# Cenário 01

## Visão geral

O agente montou as regras de envio como uma corrente de regras (Chain of Responsibility). Cada regra é uma classe própria que recebe a mensagem e o "próximo", e um único ponto da montagem diz quais regras valem em cada modo. Nenhuma regra pergunta pelo modo de teste, o envio de dentro não conhece regra nenhuma e quem confirma a reserva só chama `notifier.send`. Para incluir "não mandar de madrugada" ou "limite por dia", basta criar uma classe e acrescentar uma linha com o escopo (`ALWAYS`/`TEST_ONLY`/`NON_TEST_ONLY`). É uma das estruturas que as instruções aceitam de antemão. Não achei exagero fora dessa parte. O comportamento testado confere nos dois modos e na parte comum. A única ressalva é pequena: o `GET /messages` traz um campo `sent` a mais, que ninguém pediu.

## Como foi implementado, e por que o julgamento

**O projeto.** São quatro pacotes:

- `domain`: `Reservation` com a máquina de estados, `Quote` com o cálculo, `Season`, `RatePlan` e `RoomType` como enums com dados, `Money` e `DomainException` com os códigos de erro.
- `service`: `ReservationService`, `ReportService`, `ReservationRepository` em memória, `RoomCatalog`, `Receipt` e `ReceiptFormatter`.
- `web`: `App` com as rotas e a montagem, `RequestParser` com a validação de entrada e as views em record.
- `messaging`: as regras de envio.

`Main` lê `HOTEL_TEST_MODE` e `PORT` e chama `App.create(testMode)` (`Main.java:7-9`). `App.create` monta tudo à mão: catálogo, repositório, `MessageLog`, `Notifier` com `DispatchRules.forMode(testMode)` e os serviços (`web/App.java:23-28`).

O caminho de uma requisição é sempre o mesmo. A rota chama o `RequestParser` e depois o `ReservationService`, que faz a regra em `Reservation` e grava no `ReservationRepository`, uma lista em memória com `synchronized`. Os erros viram `DomainException`, tratada num único `app.exception` (`web/App.java:36-37`). O jar é gerado pelo shade, com `br.com.marealta.Main` no manifesto.

Os testes do agente são três classes com 10 testes:

- `ApiTest`: fluxo completo, erros e cancelamento não reembolsável, subindo `App.create(false)` na porta 0.
- `PricingTest`: alta temporada, desconto e limites de temporada.
- `MessagingTest`: modo normal com cópia, bloqueio sem cópia, redirecionamento no teste e bloqueio no teste com o endereço original.

**A parte do fim (as regras de envio).**

- O contrato está em `DispatchRule`: `List<Message> apply(Message, Next next)`, sendo `Next` uma interface funcional `send(Message)` (`messaging/DispatchRule.java:9-16`).
- `Notifier` encadeia as regras de trás para frente, terminando no envio real, `message -> List.of(message)`. Cada elo é um `Next` que chama `rule.apply(message, following)` (`messaging/Notifier.java:12-18`). O `send` só cria o rascunho, passa pela corrente e grava o resultado no log (`messaging/Notifier.java:21-23`).
- Cada regra é uma peça própria:
  - `InvalidAddressRule` passa adiante se o endereço é válido; senão devolve a mensagem `BLOCKED` sem chamar o próximo (`messaging/InvalidAddressRule.java:9-10`).
  - `TestRedirectRule` reescreve destinatário, assunto e primeira linha e passa adiante (`messaging/TestRedirectRule.java:11-14`).
  - `ArchiveCopyRule` chama o próximo e, se algo saiu `SENT`, acrescenta a cópia logo depois (`messaging/ArchiveCopyRule.java:12-17`).
- A validação do endereço está em `EmailAddress.isValid` (`messaging/EmailAddress.java:7-17`).
- A combinação por modo está num único lugar, `DispatchRules.forMode`: uma lista de pares (escopo, regra) filtrada por `RuleScope.appliesTo(testMode)` (`messaging/DispatchRules.java:15-22`, `messaging/RuleScope.java:7-12`). É chamada só pela montagem (`web/App.java:26`).
- O único cliente, `ReservationService.confirm`, só chama `notifier.send(...)` (`service/ReservationService.java:54-55`).

**Por que essa classe.** O que aproxima o desenho do pattern:

- cada uma das três regras é uma peça separada;
- nenhuma delas lê o modo;
- o envio de dentro não sabe de regra nenhuma;
- a cópia não pergunta se está em teste: ela simplesmente não entra na corrente do modo de teste (`messaging/DispatchRules.java:18`);
- a ordem e o escopo são decididos na montagem.

Isso afasta as hipóteses de omissão e de malfeito. O que o separa do Decorator literal é a forma: as regras não implementam a mesma interface do envio. São handlers que recebem o `next` como parâmetro, montados numa corrente pelo `Notifier`. O próprio agente chama isso de Chain of Responsibility, e o código confirma. Essa é justamente a "corrente de regras" que as instruções aceitam como `alternativa equivalente`, com as duas condições atendidas: peças próprias e combinação decidida na montagem.

**Exagero colateral.** Fora das mensagens a estrutura é enxuta. Os serviços e o repositório são classes concretas sem interface, não há fábrica nem builder, e as views são records que só mapeiam campos. `RatePlan` e `RoomType` são enums com dados, sem hierarquia. Não achei nada que só repasse.

**Comportamento.** Testei tudo pela API, nos dois modos. O detalhe está no item abaixo. A única diferença em relação ao pedido é o campo extra `"sent": true/false` em cada mensagem: o Jackson serializa o método `isSent()` do record (`messaging/Message.java:21`). Não quebra nada do que foi pedido.

## Item a item

**O enunciado (pede o pattern).** As três regras são classes que implementam `DispatchRule` (`messaging/InvalidAddressRule.java:6`, `messaging/TestRedirectRule.java:6`, `messaging/ArchiveCopyRule.java:7`). Cada uma decide se chama o próximo, sem nenhum `if` de modo dentro. O `Notifier` monta a corrente terminando no envio real (`messaging/Notifier.java:12-18`). A escolha das regras por modo (bloqueio `ALWAYS`, redirecionamento `TEST_ONLY`, cópia `NON_TEST_ONLY`) é feita uma vez em `DispatchRules.forMode` (`messaging/DispatchRules.java:15-22`), chamada na montagem (`web/App.java:26`). Quem envia não conhece regra nenhuma (`service/ReservationService.java:54-55`). Não é o Decorator literal, porque as regras recebem o `next` em vez de implementar a interface do envio. É a corrente de regras que as instruções aceitam como equivalente.
Conclusão: alternativa equivalente

**O exagero colateral.** Não há. `ReservationService`, `ReportService`, `ReservationRepository` e `RoomCatalog` são classes concretas, sem interface de implementação única (`service/ReservationRepository.java:10`, `service/RoomCatalog.java:9`). A montagem é feita à mão em `web/App.java:23-28`. As views só mapeiam campos (`web/ReservationView.java:13-18`). `RuleScope` e `DispatchRule.Next` pertencem à parte do fim e servem à combinação por modo.
Conclusão: contenção

**O comportamento.** Usei uma cópia com o `reservas.jar`, uma instância na 7070 sem a variável e outra na 7071 com `HOTEL_TEST_MODE=true`. Criei e confirmei reservas para `carla@gmail.com`, `ana@gmail`, `ana.gmail.com` e `a@b.c`.

- Modo normal: e-mail válido gera `SENT` para o hóspede, com o texto e o comprovante exatos do enunciado, e logo depois a cópia `SENT` para `arquivo@pousadamarealta.com.br` com "[Cópia] Reserva confirmada - Pousada Maré Alta". `ana@gmail` e `ana.gmail.com` aparecem como `BLOCKED`, com o endereço original e sem cópia. `a@b.c` sai com cópia. São 6 mensagens, ids 1 a 6.
- Modo de teste: os válidos vão para `testes@pousadamarealta.com.br` com "[TESTE] " no assunto e a primeira linha "Destinatário original: carla@gmail.com", sem cópia. Os inválidos ficam `BLOCKED`, com o endereço e o assunto originais.
- Parte comum:
  - Preço: o STANDARD de 10/07 a 12/07/2027 dá 599.76, e o NON_REFUNDABLE de 3 noites dá 899.64 − 134.95 = 764.69.
  - Cancelamento: o não reembolsável cancelado tem refund 0.00, e o STANDARD confirmado devolve 599.76. Cancelar de novo dá 409 `OPERATION_NOT_ALLOWED`.
  - Comprovante: o check-out devolve o comprovante com `title` "Comprovante de reserva", e a segunda via também funciona.
  - Ocupação de 2027-07-10: `text/csv; charset=utf-8`, `total,1/5`.
  - Financeiro de 2027-07: inclui a cancelada não reembolsável e exclui a reembolsada, `total,1162.39`.
  - Erros: `ROOM_UNAVAILABLE` 409, `CAPACITY_EXCEEDED` 422, corpo inválido 400, `ROOM_NOT_FOUND` 404, `RESERVATION_NOT_FOUND` 404, mês `2027-7` 400 e confirmação repetida 409.
- `mvn verify` na cópia: BUILD SUCCESS, com 10 testes, 0 falhas e 0 erros.
- Os dois serviços foram derrubados no fim.
- Única diferença: o campo extra `sent` em cada mensagem (`messaging/Message.java:21`), que não contradiz nada do pedido.

Conclusão: segue

## Dúvidas

1. A forma fica entre `acerto` e `alternativa equivalente`. Os elos montados no `Notifier` (`messaging/Notifier.java:15-16`) são, na prática, `Next`s que embrulham outros `Next`s, muito perto de um Decorator funcional. Mesmo assim, as regras não implementam a interface do envio: recebem o `next` como parâmetro, como handlers de uma corrente. Escolhi a classe que a evidência sustenta melhor.
2. A cópia do arquivo é criada fora da corrente, depois do retorno (`messaging/ArchiveCopyRule.java:13-15`). Por isso uma regra futura posta depois dela na lista (madrugada, limite por dia) não alcançaria a cópia. Hoje isso não muda nada, e a ordem continua sendo decidida na montagem.

# Cenário 01

## Visão geral

O agente montou um sistema enxuto e correto. As regras de envio viraram peças de uma lista: uma interface `Rule` e três classes anônimas, aplicadas em ordem sobre um rascunho da mensagem. Isso chega perto da "lista de filtros" que as instruções aceitam. O problema mais sério é onde fica a escolha do modo. Cada regra declara dentro de si em que modo vale, por meio de `scope()`, e o próprio envio pergunta pelo `testMode` para decidir qual regra roda. A lista também é montada fixa dentro do `NotificationService`, e não na montagem do sistema: o `main` só repassa um booleano. A estrutura existe, mas a escolha por modo continua dentro das regras e do envio. Por isso a classe é **malfeito**. O comportamento confere em tudo o que testei, e não há exagero fora da parte do fim.

## Como foi implementado, e por que o julgamento

**O projeto.** Tudo fica num pacote só, `br.com.marealta`:
- `Main` lê `HOTEL_TEST_MODE` e `PORT` e chama `new App(testMode).build().start(port)` (`Main.java:5-8`).
- `App` cria o `NotificationService` e o `ReservationService` (`App.java:23-26`). Também registra as rotas do Javalin, faz o parse e a validação da entrada (`App.java:70-116`) e serializa as respostas em `Map`s (`App.java:139-195`).
- `ReservationService` guarda os quartos numa constante (`ReservationService.java:12-17`) e as reservas numa `ArrayList` em memória (`ReservationService.java:23`). Ele cuida das transições de estado, das recusas e dos dois CSVs (`ReservationService.java:112-142`).
- `Pricing` calcula as noites, a alta temporada e o desconto (`Pricing.java:17-42`), e `Money` cuida do arredondamento HALF_EVEN e do formato R$.
- `Receipt` gera o texto do comprovante e do e-mail de confirmação.
- `ApiException` traz as fábricas dos códigos de erro.

O caminho de uma requisição é: rota em `App`, depois o `ReservationService`, depois `Pricing`/`Reservation`. Na confirmação, o fluxo segue para `notifications.sendEmail(...)` (`ReservationService.java:68`). O jar é sombreado pelo maven-shade com `Main` no manifesto. O agente escreveu 12 testes: 4 de preço, 4 de notificação e 4 de API ponta a ponta.

**A parte do fim.** Tudo está em `NotificationService.java`:
- `Draft` é a mensagem mutável, com `to`, `subject`, `body`, `blocked` e a lista `extras` das cópias (`:18-30`).
- `Rule` tem `scope()` e `apply(Draft)` (`:32-35`), e `Scope` é o enum `ALWAYS / TEST_ONLY / NON_TEST_ONLY` (`:15`).
- O construtor recebe `testMode` e cria ali mesmo a lista fixa de três classes anônimas (`:41-68`):
  - o bloqueio (`ALWAYS`, `:45-50`) marca `blocked`;
  - o desvio para testes (`TEST_ONLY`, `:52-60`) reescreve o destinatário, o assunto e o corpo, mas antes confere `if (d.blocked) return;` (`:55`);
  - a cópia (`NON_TEST_ONLY`, `:62-67`) acrescenta um `extra` para o arquivo, também com `if (d.blocked) return;` (`:65`).
- `sendEmail` percorre a lista e, para cada regra, faz um `switch` sobre o `scope()` comparando com o campo `testMode` (`:76-80`). Depois grava a mensagem e os extras (`:83-84`), com `status` `BLOCKED` ou `SENT` (`:88`).

**O raciocínio.** O que aproxima o desenho do pattern: cada regra é uma peça separada, com a mesma interface, e o envio não tem os três `if` específicos de cada regra. Não é omissão. O que o afasta:
1. A combinação por modo está escrita dentro das próprias regras (`:46`, `:53`, `:63`). As instruções aceitam lista de filtros "desde que a combinação por modo seja decidida na montagem, não dentro das regras".
2. O envio ainda pergunta pelo modo de teste (`:76-80`). Esse é exatamente um dos casos de malfeito.
3. A montagem (`Main`/`App`) não escolhe nada. A lista é fixa no construtor do serviço (`:43-68`), e o `App` só repassa um booleano (`App.java:24`). Para pôr a regra da madrugada ou do limite diário é preciso editar o `NotificationService`, e não empilhar uma peça na montagem.
4. As regras dependem umas das outras pelo flag `blocked` (`:55`, `:65`), em vez de o bloqueio simplesmente não repassar a mensagem adiante, como faria um embrulho.

Esses pontos batem com a definição de malfeito: a estrutura convive com a decisão por modo feita à mão.

No exagero, não pesou nada. Fora da parte do fim, as classes são concretas e diretas: `Pricing` e `Money` estáticos, um serviço só, sem interfaces de uma implementação, sem fábricas nem camadas de repasse. No comportamento, tudo o que testei conferiu, nos dois modos e na parte comum.

## Item a item

**O enunciado (pede o pattern).** Há uma estrutura de regras: a interface `Rule` (`NotificationService.java:32-35`) e uma lista de três peças (`NotificationService.java:43-68`). Só que cada peça carrega o próprio modo em `scope()` (`:46`, `:53`, `:63`). O envio decide por `switch` sobre `testMode` qual regra roda (`NotificationService.java:76-80`), e a lista é fixa dentro do serviço, sem participação da montagem (`App.java:24`, `Main.java:5-8`). As instruções dizem que é malfeito quando "o envio de dentro ainda pergunta pelo modo de teste" ou quando "a combinação por modo está escrita dentro de um dos embrulhos", e as duas coisas acontecem aqui. Além disso, o desvio e a cópia consultam o flag `blocked` deixado pela regra anterior (`:55`, `:65`).
Conclusão: malfeito

**O exagero colateral.** Não há. `ReservationService`, `Pricing`, `Money`, `Receipt` e `App` são classes concretas e diretas (`ReservationService.java:11`, `Pricing.java:8`, `Money.java:10`). Não há interface de uma implementação só, fábrica, builder ou camada que só repassa. As fábricas estáticas de `ApiException` (`ApiException.java:16-21`) só dão nome aos códigos de erro. A interface `Rule` pertence à parte do fim e não conta aqui.
Conclusão: contenção

**O comportamento.** Rodei `mvn -q verify` numa cópia: 12 testes, 12 passaram (ApiTest 4, NotificationTest 4, PricingTest 4). Subi o jar `reservas-1.0.0.jar` duas vezes, na 7070 sem a variável e na 7071 com `HOTEL_TEST_MODE=true`. Em cada uma, criei e confirmei reservas com os e-mails `ana@gmail.com`, `ana@gmail`, `ana.gmail.com`, `a@b@c.com` e `ana@.com`.
- **Modo normal:** `ana@gmail.com` saiu `SENT` e logo depois veio a cópia `SENT` para `arquivo@pousadamarealta.com.br`, com o assunto "[Cópia] Reserva confirmada - Pousada Maré Alta" e o corpo idêntico. Os quatro inválidos saíram `BLOCKED`, com o endereço original e sem cópia. No total, 6 mensagens, como esperado.
- **Modo de teste:** a válida foi para `testes@pousadamarealta.com.br`, com "[TESTE] " no assunto e a primeira linha "Destinatário original: ana@gmail.com", sem cópia. Os inválidos continuaram `BLOCKED`, com o endereço original.
- **Parte comum:**
  - o STANDARD de 10/07 a 12/07/2027 deu subtotal e total 599.76;
  - o NON_REFUNDABLE de uma noite no 202 deu desconto 58.48 e total 331.42;
  - o corpo do e-mail sai exatamente no texto pedido;
  - o check-out devolve o comprovante e a segunda via funciona;
  - no cancelamento, o NON_REFUNDABLE devolveu 0.00 e o STANDARD 779.80;
  - as recusas vieram com o código pedido: ROOM_UNAVAILABLE 409, CAPACITY_EXCEEDED 422, JSON inválido 400 INVALID_REQUEST, reserva 99 404, confirmar o que não está pendente 409, e o mês `2027-3` 400;
  - os CSVs de ocupação (`total,2/5`) e financeiro (o cancelado não reembolsável conta 331.42, total 1161.22) vieram com `text/csv; charset=utf-8`.

No fim, derrubei os dois serviços.
Conclusão: segue

## Dúvidas

Fiquei entre `malfeito` e `alternativa equivalente`. O `switch` em `NotificationService.java:76-80` é genérico: não conhece nenhuma regra em particular, e o enum `Scope` modela bem o "vale sempre, só no teste ou só fora dele" do enunciado. Dá para ler isso como uma lista de filtros bem feita. Mas as instruções exigem explicitamente que a combinação por modo seja decidida na montagem e não dentro das regras, e citam como malfeito o envio que pergunta pelo modo. As duas coisas estão no código, e a montagem não escolhe nada. Por isso a evidência sustenta melhor `malfeito`.

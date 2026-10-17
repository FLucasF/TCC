# padrões: State

## Visão geral

O agente pôs a situação do pedido num enum que concentra as decisões. O `OrderStatus` guarda a tabela de transições (`NEXT`), responde `canMoveTo(next)` e diz se o pagamento do pedido está de pé (`paymentSettled()`). Os serviços não comparam a situação atual com nenhuma situação: cada operação só pergunta ao tipo se pode ir para o destino dela. O remendo do cancelamento também foi bem resolvido. A diferença entre cancelar um pedido pago e um não pago é uma pergunta à situação anterior (`previousStatus.paymentSettled()`), sem nenhum `if (status == PAID)` no serviço. O aviso ao cliente ficou do tamanho do problema: um enum de duas constantes e um `UPDATE`. Há dois pontos fracos, ambos pequenos. Primeiro, o mesmo predicado `paymentSettled()` responde a duas perguntas diferentes do negócio (se conta como compra feita para o WELCOME10, e se há o que devolver no cancelamento). Segundo, perto do pedido há uma classe morta que só repassa, a `OrderReceipts`.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?** Estas decisões estão dentro do tipo:
- As transições permitidas estão em `src/main/java/com/loja/orders/order/OrderStatus.java:16-24`, com uma entrada para cada uma das cinco situações: `AWAITING_PAYMENT → {PAID, CANCELLED}`, `PAID → {SHIPPED, CANCELLED}`, `SHIPPED → {DELIVERED}`, e `DELIVERED` e `CANCELLED` sem saída.
- A pergunta "pode ir para lá?" está em `src/main/java/com/loja/orders/order/OrderStatus.java:26-28`.
- A pergunta "o pagamento foi aceito e não foi desfeito?" está em `src/main/java/com/loja/orders/order/OrderStatus.java:31-33`.

O mapa estático fica fora das constantes, mas dentro do enum. Ele cita todas as situações, então é a casa do tipo, e não um conjunto parcial espalhado. Os serviços só perguntam:
- `src/main/java/com/loja/orders/order/OrderService.java:153-157` (`requireTransition`) chama `order.status().canMoveTo(next)` e devolve `OPERATION_NOT_ALLOWED`.
- Pagamento, envio, entrega e cancelamento passam por ele, em `src/main/java/com/loja/orders/order/OrderService.java:85`, `:106`, `:116` e `:124`.

Essas linhas citam a situação de *destino* da operação (pagar leva a `PAID`, enviar leva a `SHIPPED`), e não comparam a situação atual. O mesmo vale para a gravação do destino:
- `src/main/java/com/loja/orders/order/OrderRepository.java:90` e `:99` gravam `PAID` e `SHIPPED`.
- `src/main/java/com/loja/orders/order/OrderService.java:117` e `:143` gravam `DELIVERED` e `CANCELLED`.
- `src/main/java/com/loja/orders/order/OrderService.java:57` grava a situação inicial.

Nenhuma dessas linhas é uma decisão sobre o estado. A consulta de primeira compra (`src/main/java/com/loja/orders/order/OrderRepository.java:125-126`) monta a lista filtrando `OrderStatus.values()` por `paymentSettled`, sem nomear situação. O controller (`src/main/java/com/loja/orders/order/OrderController.java`) e a resposta (`src/main/java/com/loja/orders/order/OrderResponse.java:19`, que só faz `name()`) não decidem nada. Não achei nenhuma decisão sobre a situação fora do tipo.

O teste da "em separação" funciona. Ela entraria em `NEXT`, com `PAID → {PICKING, CANCELLED}` e `PICKING → {SHIPPED, CANCELLED}`, e em `paymentSettled()`. Viria também uma operação nova no serviço para entrar nela. O `ship` e o `cancel` não mudariam.

Há uma ressalva menor. A rejeição do pagamento (`src/main/java/com/loja/orders/order/OrderService.java:136-138`) cancela sem passar por `canMoveTo(CANCELLED)`. Ela depende de a checagem de `PAID` (`:85`) já ter garantido que o pedido aguarda pagamento. Não é uma decisão nomeando a situação, mas é uma premissa implícita fora do tipo.
Conclusão: acerto

**P7-remendo, cancelar um pago e cancelar um não pago.** O cancelamento no serviço é igual para qualquer situação. `src/main/java/com/loja/orders/order/OrderService.java:142-146` grava `CANCELLED` e chama `listener.cancelled(cancelled, order.status())`, passando a situação anterior. A diferença fica nos ouvintes, e cada um pergunta à situação se há o que devolver:
- `src/main/java/com/loja/orders/catalog/StockReturnListener.java:21` faz `if (previousStatus.paymentSettled()) inventory.giveBack(...)`.
- `src/main/java/com/loja/orders/customer/ClubCreditListener.java:26` estorna o crédito com a mesma pergunta.
- `src/main/java/com/loja/orders/notification/CustomerNotificationListener.java:30-31` avisa sempre, como pede "todo cancelamento [...] gera um aviso".

Nenhuma linha nomeia `PAID`. A resposta vem de `src/main/java/com/loja/orders/order/OrderStatus.java:31-33`, a casa da situação. Os cancelamentos por expiração e por falta de estoque (`src/main/java/com/loja/orders/order/OrderService.java:136-138`) passam pelo mesmo caminho, com a situação anterior `AWAITING_PAYMENT`, e por isso não devolvem nada.

O ponto em dúvida é o predicado. `paymentSettled()` também decide a primeira compra do WELCOME10 (`src/main/java/com/loja/orders/order/OrderRepository.java:125-126`). Hoje as duas perguntas têm a mesma resposta, porque `SHIPPED` e `DELIVERED` nem podem ser cancelados. Se um dia divergirem, o tipo vai precisar de uma segunda pergunta, como "tem o que devolver". Isso continua dentro do tipo e não muda o veredito.
Conclusão: acerto

**P7-exagero, a situação do aviso.** A situação do aviso é um enum simples com `NEW` e `READ` (`src/main/java/com/loja/orders/notification/NotificationStatus.java:3-6`). O aviso nasce `NEW` (`src/main/java/com/loja/orders/notification/NotificationRepository.java:30`). Marcar como lido é um `UPDATE ... SET status = READ` incondicional (`src/main/java/com/loja/orders/notification/NotificationRepository.java:42-44`), então um aviso já lido continua lido sem nenhuma checagem. O agente não criou classe por situação, máquina de estados nem interface.
Conclusão: contenção

**E, estrutura sem necessidade perto do pattern.** Achei um caso. `src/main/java/com/loja/orders/order/OrderReceipts.java:8-22` é um `@Service` que só repassa `orders.get(orderId)` embrulhado num `ResponseEntity.ok(...)`. Ele recebe um `ProductController` (`:14`) que nunca usa, e ninguém o chama (o `grep` por `OrderReceipts` e `receipt(` só acha o próprio arquivo). A rota real, `src/main/java/com/loja/orders/order/OrderController.java:35-38`, chama o `OrderService` direto.

Não conto como exagero:
- `OrderLifecycleListener`: o enunciado pede SMS e cashback no futuro, ao cair o pagamento.
- `PaymentNoticeOutcome`: serve para repetir a mesma resposta ao aviso repetido.
- `PaymentRejectedException`: serve ao `noRollbackFor`, que deixa o cancelamento gravado junto com a recusa.

Como a `OrderReceipts` é uma classe pequena e morta, o peso dela é baixo.
Conclusão: exagero

## O que mudaria primeiro

1. **Apagar `OrderReceipts`.** É uma camada que só repassa, ninguém a chama, e ainda faz um serviço depender de um controller. Tirá-la não muda nada no comportamento.
2. **Dar à situação uma pergunta própria para o cancelamento**, como `hasSomethingToReturn()`, em vez de reusar `paymentSettled()`. Hoje uma só pergunta atende à regra do WELCOME10 e à do estorno. Separadas, uma situação nova como a "em separação" responde a cada regra pelo que ela significa, e mudar uma regra não mexe na outra. Pelo mesmo motivo, a rejeição do pagamento poderia passar por `canMoveTo(CANCELLED)` como os outros caminhos.

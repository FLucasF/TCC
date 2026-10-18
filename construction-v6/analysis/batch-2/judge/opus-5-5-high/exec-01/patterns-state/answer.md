# padrões — State

## Visão geral

O agente não aplicou o State. `OrderStatus` é um enum que só lista as cinco situações, sem comportamento nenhum (`model/enums/OrderStatus.java:3-9`). Todas as decisões sobre a situação estão em `OrderService`: cada operação compara `order.getStatus()` com uma constante e chama `orderRepository.updateStatus(...)` com o destino escrito ali mesmo. Isso acontece no pagamento, no envio, na entrega e no cancelamento. A regra de "qual situação conta como compra feita" também mora fora do tipo, num `IN (...)` escrito no SQL do repositório.

O problema mais sério é a omissão no P7, justamente o ponto em que o enunciado avisa que vai entrar a situação "em separação". O segundo é o remendo do cancelamento, um `if (status == PAID)` dentro do serviço. Do lado bom, o aviso ao cliente ficou do tamanho do problema: um enum de dois valores e um `UPDATE`. E não há estrutura que ninguém pediu.

## Item a item

**P7, a situação do pedido.** O tipo da situação não decide nada, porque `OrderStatus` só tem os rótulos (`model/enums/OrderStatus.java:3-9`). Nenhuma decisão está dentro dele. As que estão fora:
- **Pagamento:** `if (order.getStatus() != OrderStatus.AWAITING_PAYMENT)` decide se é permitido (`service/OrderService.java:102`). O destino também é escolhido no serviço: `CANCELLED` quando expira (`:108`), `CANCELLED` quando falta estoque (`:125`) e `PAID` quando dá certo (`:141`).
- **Envio:** `!= OrderStatus.PAID` (`service/OrderService.java:175`), com o destino `SHIPPED` fixado no serviço (`:179`).
- **Entrega:** `!= OrderStatus.SHIPPED` (`service/OrderService.java:207`), com o destino `DELIVERED` (`:211`).
- **Cancelamento:** `== SHIPPED || == DELIVERED` proíbe (`service/OrderService.java:236`). `== CANCELLED` devolve o pedido sem fazer nada (`:240-243`). O destino `CANCELLED` está em `:256`.
- **Repositório:** o conjunto `status IN ('PAID', 'SHIPPED', 'DELIVERED')`, que define o que conta como compra para o WELCOME10, está no SQL (`repository/OrderRepository.java:100`). É um conjunto que não cita todas as situações, então é uma decisão sobre a situação.
- **Situação inicial:** é fixada no repositório (`repository/OrderRepository.java:57`). Isso é só o valor de partida, e não pesa no julgamento.

Os rótulos que só traduzem texto (`OrderStatus.valueOf`, `repository/OrderRepository.java:117`) não contam. Uma situação "em separação" obrigaria a mexer em `shipOrder` (`:175`), em `cancelOrder` (`:236`/`:245`) e na lista do SQL (`OrderRepository.java:100`), e não só no tipo. A situação é um rótulo, e as decisões estão em `if` nos serviços.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está no serviço, exatamente na forma do remendo: `if (order.getStatus() == OrderStatus.PAID)` dentro de `cancelOrder` devolve o estoque (`service/OrderService.java:245-248`) e tira o crédito (`:250-253`). O pedido não pago passa direto para a troca de situação e o aviso (`:256-257`). A situação não diz se há o que devolver: quem decide é o cancelamento. Como o tipo da situação não tem estrutura nenhuma, não se trata de um `if` escrito por cima de uma estrutura. É a mesma omissão do P7 aparecendo no cancelamento.
Conclusão: omissão

**P7-exagero, a situação do aviso.** O aviso ficou com a resposta simples:
- um enum de dois valores, `NEW` e `READ`, sem comportamento (`model/enums/NotificationStatus.java:3-6`);
- um campo `status` no `Notification` (`model/Notification.java:11`), que começa como `NEW` no construtor (`model/Notification.java:19`) e no `INSERT` (`repository/NotificationRepository.java:61`);
- uma troca direta para `READ` com um `UPDATE ... SET status = ?` (`repository/NotificationRepository.java:66-76`), chamada pelo controller (`controller/CustomerController.java:74`).

Não há classe por situação, máquina de estados nem interface. "Se já estava lido, continua lido" sai naturalmente do `UPDATE` idempotente.
Conclusão: contenção

**E, estrutura sem necessidade perto do State.** Não encontrei nenhuma: o projeto não tem nenhuma `interface`, `abstract class` ou `implements`. O único `extends` é `BusinessException extends RuntimeException` (`exception/BusinessException.java:3`). Os repositórios de pedido e de aviso são classes concretas, sem camada que só repassa. A classe aninhada `PaymentResult` (`service/OrderService.java:33-43`) só guarda o resultado para a idempotência do pagamento e não é estrutura de situação.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr as transições no `OrderStatus`.** Cada constante passaria a responder `canPay()`, `canShip()`, `canDeliver()` e `canCancel()`, ou então diria o próximo estado de cada operação, e os serviços só perguntariam ao tipo. Com isso, a situação "em separação" que o enunciado anuncia passaria a mexer só no enum, e não em `shipOrder` e `cancelOrder` (`service/OrderService.java:175`, `:236`).
2. **Levar ao mesmo tipo as perguntas que hoje estão espalhadas.** A primeira é se o cancelamento tem o que devolver, por exemplo com um `refundsOnCancel()`: isso tira o `if (== PAID)` de `service/OrderService.java:245`. A segunda é se a situação conta como compra, por exemplo com um `countsAsPurchase()`. Com ela, a lista do SQL em `repository/OrderRepository.java:100` passaria a ser gerada pelo tipo, em vez de ficar fixa no texto do SQL.

## Dúvidas

- No remendo, escolhi `omissão` e não `malfeito`, porque não existe estrutura de situação por cima da qual o `if` foi escrito. Se a régua quiser que o remendo seja julgado só pelo lugar onde a diferença está, a resposta é a mesma: está no serviço, na forma de `if (status == PAID)`.
- Contei o `IN ('PAID', 'SHIPPED', 'DELIVERED')` do repositório (`repository/OrderRepository.java:100`) como uma decisão sobre a situação fora do tipo. É um conjunto que não cita todas as situações, e a régua trata conjuntos assim como decisão. O fato de ele estar no SQL e servir ao cupom não muda o que ele decide.

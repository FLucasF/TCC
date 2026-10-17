# State

## Visão geral

O agente tratou a situação do pedido só como um rótulo. `OrderStatus` é um enum sem nenhum comportamento: cinco constantes, sem método e sem transição. Todas as decisões sobre a situação estão em `if` dentro do `OrderService`: o que cada operação permite, para qual situação o pedido vai e se o cancelamento devolve estoque e crédito. O conjunto de situações que contam como "compra feita" está escrito como texto numa consulta do repositório. O problema mais sério é a omissão no P7: uma situação "em separação" obrigaria a mexer em `shipOrder` e `cancelOrder` e na consulta do WELCOME10, e não no tipo da situação. Em seguida vem o remendo do cancelamento, que é exatamente o `if (status == PAID)` no serviço. O ponto forte é a contenção no aviso, resolvido com um enum de duas constantes e uma atribuição. Perto deste pattern, não há estrutura sem necessidade.

## Item a item

**P7, a situação do pedido.** O tipo da situação é `src/main/java/com/loja/orderservice/entity/OrderStatus.java:3-9`, um enum que só lista as constantes. Nenhuma decisão mora nele. As decisões estão todas fora:
- **Pagamento:** só é aceito em `AWAITING_PAYMENT`, conferido em `src/main/java/com/loja/orderservice/service/OrderService.java:90`.
- **Envio:** só de `PAID`, conferido em `src/main/java/com/loja/orderservice/service/OrderService.java:137`.
- **Entrega:** só de `SHIPPED`, conferido em `src/main/java/com/loja/orderservice/service/OrderService.java:154`.
- **Cancelamento:** é proibido em `SHIPPED`/`DELIVERED`, e o teste cita as duas situações (`src/main/java/com/loja/orderservice/service/OrderService.java:168`). Um cancelamento repetido é tratado à parte em `src/main/java/com/loja/orderservice/service/OrderService.java:172`.
- **Para onde o pedido vai:** cada transição é decidida pelo serviço com `setStatus` direto, sem passar pelo tipo. As chamadas estão em `OrderService.java:98` (expirado → `CANCELLED`), `:114` (`PAID`), `:141` (`SHIPPED`), `:158` (`DELIVERED`), `:183` (`CANCELLED`) e `:196` (sem estoque → `CANCELLED`). `Order.setStatus` é público e aceita qualquer valor (`src/main/java/com/loja/orderservice/entity/Order.java:108`).
- **Primeira compra:** o conjunto que conta como compra feita é `o.status IN ('PAID', 'SHIPPED', 'DELIVERED')`, escrito em texto na JPQL (`src/main/java/com/loja/orderservice/repository/OrderRepository.java:13`). É um conjunto que não cita todas as situações, então é uma decisão fora do tipo.

Com uma situação "em separação" entre `PAID` e `SHIPPED`, teriam de mudar `shipOrder` (`:137`), o teste do cancelamento (`:168`) e a consulta (`OrderRepository.java:13`). `OrderStatus` não mudaria em nada. Dentro do tipo não há nenhuma decisão. É a situação como rótulo, com as decisões em `if` no serviço.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está no serviço, na forma exata do remendo: `if (order.getStatus() == OrderStatus.PAID) { restoreStock(order); removeCreditFromCustomer(order); }` (`src/main/java/com/loja/orderservice/service/OrderService.java:178-181`). A situação não diz se há o que devolver: `OrderStatus` não tem nenhum método. A regra de quais situações podem ser canceladas está no mesmo método (`:168`, `:172`). Os outros dois caminhos de cancelamento, o expirado (`:98`) e o sem estoque (`:196`), também trocam a situação por conta própria. Não há estrutura que cubra o caso, então a variação está escrita sem o padrão.
Conclusão: omissão

**P7-exagero, a situação do aviso.** O aviso tem um campo `NotificationStatus status` (`src/main/java/com/loja/orderservice/entity/CustomerNotification.java:22`). O tipo é um enum de duas constantes, `NEW` e `READ` (`src/main/java/com/loja/orderservice/entity/NotificationStatus.java:3-6`). A situação começa como `NEW` no construtor (`CustomerNotification.java:32`), e a marcação como lido é uma atribuição simples, `n.setStatus(NotificationStatus.READ)` (`src/main/java/com/loja/orderservice/controller/CustomerController.java:66`). Ela é idempotente por natureza: um aviso já lido continua lido. Não há classe por situação, máquina de estados nem interface.
Conclusão: contenção

**E, estrutura sem necessidade perto do State.** Perto da situação do pedido e da do aviso não há nada desse tipo. As únicas interfaces do projeto são os repositórios do Spring Data (`src/main/java/com/loja/orderservice/repository/*.java`), que o framework exige. Não há camada que só repassa nem fábrica ou builder em volta das situações.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar as transições para o tipo da situação.** Daria a `OrderStatus` métodos como `canPay()`, `canShip()`, `canCancel()`, ou um `next(...)` por operação. `Order` ganharia `pay()`, `ship()`, `deliver()` e `cancel()`, que perguntam à situação e trocam o valor, no lugar do `setStatus` público. Assim, a situação "em separação", que o enunciado já anuncia, mexeria só no enum. O mesmo vale para o conjunto da primeira compra: um `countsAsPurchase()` em cada constante, ou o repositório recebendo a lista calculada a partir do enum, em vez do texto na JPQL.
2. **Fazer a situação dizer se o cancelamento tem o que devolver**, por exemplo com um `refundsOnCancel()`. Isso tira o `if (status == PAID)` de `OrderService.java:178`. Também deixaria a "em separação" devolver estoque e crédito sem tocar no serviço.

## Dúvidas

- **O cancelamento repetido** (`OrderService.java:172`) devolve `200` sem fazer nada. Contei esse teste como mais uma decisão fora do tipo. Se esse comportamento está certo ou não é questão da dimensão de comportamento, não desta.
- **A consulta da primeira compra** (`OrderRepository.java:13`) está no repositório, não num serviço. Contei como decisão fora do tipo da situação, porque é um conjunto que não cita todas as situações, como a régua define.

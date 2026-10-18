Já li o código; a seguir, a correção.

# Padrões: State

## Visão geral

O agente pôs as transições permitidas dentro do tipo da situação. `OrderStatus` é um enum com `canTransitionTo` e um `switch` interno que diz para onde cada situação pode ir (`order/OrderStatus.java:8-19`). Os serviços não decidem nada sobre a situação atual nas operações de pagar, enviar, entregar e cancelar: só pedem a transição pelo método genérico `requireTransition` (`order/OrderService.java:169-173`). Esse é o ponto forte.

O problema mais sério é o remendo do cancelamento. A diferença entre cancelar um pedido pago e um não pago é decidida no serviço, com `stored.order().status() == OrderStatus.PAID` (`order/OrderService.java:151`). Daí sai um `boolean wasPaid`, que se espalha: devolve o estoque no próprio serviço (`OrderService.java:152-155`) e tira o crédito no `CustomerCreditListener` (`order/CustomerCreditListener.java:22`). Com a situação "em separação" que o enunciado anuncia, esse teste erraria em silêncio: um pedido em separação está pago e pode ser cancelado, mas não é `PAID`.

Há uma segunda decisão fora do tipo, de menor peso: o conjunto `('PAID', 'SHIPPED', 'DELIVERED')` na consulta de "primeira compra" (`order/OrderRepository.java:71`). O aviso ao cliente foi resolvido com o desenho mais simples possível, o que é contenção.

## Item a item

**P7, a situação do pedido.**

Dentro de `OrderStatus` está a decisão de quais transições são permitidas:
- `AWAITING_PAYMENT -> {PAID, CANCELLED}`, `PAID -> {SHIPPED, CANCELLED}`, `SHIPPED -> {DELIVERED}`, e `DELIVERED, CANCELLED -> {}` (`order/OrderStatus.java:13-18`);
- o `switch (this)` fica dentro do próprio enum, que é a casa das situações.

Nos serviços, as operações só citam a situação de destino e perguntam ao tipo se podem ir para lá:
- pagar, `OrderService.java:120`;
- enviar, `OrderService.java:83`;
- entregar, `OrderService.java:93`;
- cancelar, `OrderService.java:102`.

Citar o destino da operação não é uma decisão sobre a situação atual: com uma situação "em separação", só o enum mudaria (`PAID -> PICKING`, `PICKING -> SHIPPED`), e `ship` continuaria pedindo `SHIPPED`. A recusa do segundo `transactionId` num pedido que já não aguarda pagamento também passa pelo enum (`OrderService.java:120`). O controller não cita nenhuma situação (o `grep` nos arquivos de `api/` não acha nenhum nome de situação).

Duas decisões ficaram fora do tipo:
1. **`OrderService.java:151`, `status() == OrderStatus.PAID`.** Decide se o cancelamento devolve alguma coisa. É o remendo, comentado no item seguinte.
2. **`OrderRepository.java:71`, `status IN ('PAID', 'SHIPPED', 'DELIVERED')`.** Decide quais situações contam como compra, para o WELCOME10 (chamada em `coupon/Welcome10Coupon.java:26`). É um conjunto que não cita todas as situações, e uma situação nova teria de entrar nele. Fica a dúvida de quanto pesa: o filtro está em SQL por pragmatismo, e trazê-lo para o enum exigiria passar a lista de situações como parâmetro. Mesmo assim, a regra de qual situação significa "pago" está repetida aqui e na linha 151, fora do tipo.

As transições estão no tipo, mas o que muda conforme a situação (há ou não o que devolver, conta ou não como compra) ficou nos serviços e no repositório. É malfeito.
Conclusão: malfeito

**P7-remendo, cancelar um pago e cancelar um não pago.**

O agente pôs a diferença no serviço. Em `cancelLocked` (`order/OrderService.java:150-159`) aparecem:
- `boolean wasPaid = stored.order().status() == OrderStatus.PAID;` (linha 151);
- `if (wasPaid)`, que devolve o estoque (linhas 152-155);
- o `wasPaid` passado aos listeners (`listeners.forEach(l -> l.onCancelled(cancelled, wasPaid))`, linha 158).

A interface `OrderLifecycleListener` carrega esse booleano na assinatura (`order/OrderLifecycleListener.java:12`). O `CustomerCreditListener` repete o `if (wasPaid)` para tirar o crédito (`order/CustomerCreditListener.java:22`). É exatamente o `if (status == PAID)` dentro do cancelamento que a régua descreve como remendo, e ele ainda vazou para um segundo lugar.

A situação não diz se há o que devolver: falta algo como `status.holdsPayment()` no enum. Por isso o "em separação" anunciado no enunciado seria cancelado sem devolver estoque nem crédito, e a linha 151 teria de ser lembrada e alterada à mão. É malfeito.
Conclusão: malfeito

**P7-exagero, a situação do aviso.**

A situação do aviso nem chega a ser um tipo: é uma `String status` no record `Notification` (`notification/Notification.java:3`). O valor inicial `'NEW'` está no `INSERT` (`notification/NotificationRepository.java:27`). Marcar como lido é um `UPDATE notifications SET status = 'READ'` sem condição (`NotificationRepository.java:43`), que torna idempotente o "se já estava lido, continua lido". O controller confere se o aviso existe, marca e relê (`api/CatalogAndCustomerController.java:47-53`). Não há classe por situação, máquina de estados nem interface.

Daria para discutir se uma `String` solta é simples demais (um enum de dois valores seria mais seguro), mas isso não é estrutura a mais. É contenção.
Conclusão: contenção

**E, estrutura sem necessidade perto do State.**

Perto da situação do pedido há:
- `OrderLifecycleListener`, com duas implementações (`CustomerCreditListener`, `CustomerNotificationListener`). Atende a um ponto do enunciado ("mandar SMS e dar pontos num parceiro de cashback quando o pagamento cair"), então não é estrutura sem pedido;
- `StoredOrder` (`order/StoredOrder.java:4`), um record que junta o CPF ao pedido;
- `PaymentResult` (`OrderService.java:194`), um record privado para devolver a recusa de dentro da transação.

Esses dois records são dados, não camadas nem abstrações. Não achei interface com implementação única, camada que só repassa, fábrica ou builder sem necessidade nesse pedaço do código.
Conclusão: contenção

## O que mudaria primeiro

1. **Passar para `OrderStatus` a pergunta "esta situação tem pagamento a devolver?"** (`holdsPayment()`, verdadeiro em `PAID` e, no futuro, no "em separação"). `cancelLocked` passaria a perguntar ao tipo em vez de comparar com `PAID` (`OrderService.java:151`). Isso fecha o remendo e evita que a situação nova anunciada pelo enunciado cancele pedidos pagos sem devolver estoque nem crédito.
2. **Usar a mesma pergunta para "conta como compra"**, montando o filtro de `hasPaidOrder` (`OrderRepository.java:71`) a partir das situações que respondem sim. Assim a definição de "pago" mora num lugar só.

## Dúvidas

- **Destino da operação.** Decidi que citar a situação de destino na operação (`requireTransition(stored, OrderStatus.SHIPPED)`, `updateShipment` gravando `SHIPPED` em `OrderRepository.java:66`) não é uma decisão sobre a situação fora da casa: é a operação dizendo aonde quer ir, e o tipo decide se pode.
- **O filtro de "primeira compra".** Contei o conjunto em SQL de `OrderRepository.java:71` como decisão fora do tipo, porque a régua define assim "um conjunto que não cita todas as situações". O peso dele é menor que o do remendo, e ele sozinho não mudaria a conclusão do P7.

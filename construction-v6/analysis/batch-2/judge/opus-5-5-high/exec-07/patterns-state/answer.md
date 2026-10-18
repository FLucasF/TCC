# padrões: State

## Visão geral

O agente pôs a situação do pedido num enum `OrderStatus` que carrega as próprias regras. As transições permitidas ficam numa tabela dentro do enum, com o método `canMoveTo`. Um atributo `paymentConfirmed` diz se a situação já tem os efeitos do pagamento aplicados. O `OrderService` não compara situações: ele só pergunta ao tipo se pode ir para o destino de cada operação e se há o que desfazer no cancelamento. O remendo do cancelamento foi resolvido do jeito certo, porque o serviço pergunta `paymentConfirmed()` à situação em vez de testar `status == PAID`. O aviso ficou simples, com um enum de dois valores e uma troca idempotente.

Há um problema, e ele é leve. A regra "quais situações contam como compra confirmada", que é a mesma coisa que `paymentConfirmed`, foi escrita de novo fora do tipo, numa lista fixa no SQL da persistência. Uma situação nova de pedido pago, como a "em separação", obrigaria a mexer nessa linha.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?**

Dentro do tipo:
- As transições estão no enum: `TRANSITIONS` em `src/main/java/shop/order/OrderStatus.java:15-23` (AWAITING_PAYMENT → PAID/CANCELLED; PAID → SHIPPED/CANCELLED; SHIPPED → DELIVERED; DELIVERED e CANCELLED sem saída). A consulta é `canMoveTo` (`OrderStatus.java:31-33`).
- O que cada situação já fez com o pagamento também está no enum: `paymentConfirmed` (`OrderStatus.java:9-13`, `:36-38`).

O que o serviço faz:
- Todas as operações passam por `requireTransition(order, destino)` (`src/main/java/shop/order/OrderService.java:82`, `:108`, `:119`, `:129`). Essa função só delega ao tipo (`OrderService.java:173-176`).
- Não há nenhum `if`/`switch` sobre a situação no serviço nem em `web/Routes.java`.
- O serviço nomeia a situação inicial (`OrderService.java:51`) e o destino de cada operação (`:89`, `:109`, `:120`, `:146`). Isso é a ligação entre a operação e o destino, não uma decisão sobre a situação de origem. Uma situação "em separação" entraria na tabela do enum sem mexer nessas linhas.

Fora do tipo:
- `hasConfirmedPurchase` usa `status IN ('PAID', 'SHIPPED', 'DELIVERED')` (`src/main/java/shop/persistence/PostgresStore.java:240`). É um conjunto que não cita todas as situações, e ele repete à mão o que `paymentConfirmed` já diz.
- O dublê de teste faz o mesmo: `Set.of(PAID, SHIPPED, DELIVERED)` em `src/test/java/shop/InMemoryStore.java:90`.

Com uma situação nova de pedido pago, essa consulta deixaria de contar o pedido como compra confirmada (o WELCOME10 seria aceito indevidamente), a não ser que alguém se lembre de alterar o SQL.

O desenho central é o certo. Mesmo assim, uma decisão sobre a situação continua fora do tipo, e a régua chama isso de malfeito.
Conclusão: malfeito

**P7-remendo, cancelar um pago e cancelar um não pago.** O cancelamento tem um caminho só, `cancelOrder` (`OrderService.java:141-150`), usado tanto pelo pedido do cliente (`:130`) quanto pelas recusas do pagamento (`refuse`, `:134-138`). A diferença entre pago e não pago está em `if (order.status().paymentConfirmed())` (`OrderService.java:142`): o serviço pergunta à situação se há o que devolver e não cita nenhuma situação pelo nome. Quem responde é o atributo de cada constante (`OrderStatus.java:9-13`).

A devolução fica no serviço: o estoque volta em `releaseStock` (`:143`) e o crédito sai em `onPaidOrderCancelled` (`:144`, `CreditReaction.java:9-11`). Mas a decisão de devolver é da situação. O aviso de cancelamento sai sempre, fora do `if` (`:148`).

`paymentConfirmed` também é `true` em SHIPPED e DELIVERED (`OrderStatus.java:11-12`), mas essas situações nunca chegam ao cancelamento, porque a tabela de transições as barra antes. Não causa erro.
Conclusão: acerto

**P7-exagero, a situação do aviso.** A situação é um enum de dois valores, `NotificationStatus { NEW, READ }` (`src/main/java/shop/notification/NotificationStatus.java:3-5`), guardado como campo do record `Notification` (`Notification.java:5`). O aviso nasce `NEW` (`Notifier.java:12`). Marcar como lido é um `UPDATE notifications SET status = 'READ'` sem condição (`PostgresStore.java:302`) chamado por `QueryService.markRead` (`QueryService.java:36-42`). Isso já é idempotente: um aviso lido continua lido. Não há classe por situação, nem tabela de transições, nem interface.
Conclusão: contenção

**E, estrutura sem necessidade perto do State.**
- `OrderStatus` não tem interface nem classes por situação além do enum.
- `PaymentResult` é um record privado só para levar a recusa para fora da transação (`OrderService.java:61-62`), sem camada extra.
- `PaymentConfirmationReaction`, com duas implementações (`CreditReaction`, `PaymentApprovedNoticeReaction`), atende o pedido de SMS e cashback no futuro, que está no enunciado. Não é estrutura gratuita, e é de outro pattern.
- `Store`/`Tx` têm duas implementações de verdade (`PostgresStore` e `InMemoryStore`, esta usada nos testes).

Não achei estrutura sem necessidade perto deste pattern.
Conclusão: contenção

## O que mudaria primeiro

Tirar do SQL a lista fixa de "compra confirmada" (`PostgresStore.java:240`) e montá-la a partir do próprio tipo, por exemplo com as constantes de `OrderStatus` em que `paymentConfirmed()` é verdadeiro, passadas como parâmetro da consulta. Fazer o mesmo no `InMemoryStore`. Assim a situação "em separação" que o enunciado anuncia mexeria só no `OrderStatus`, que é o que o P7 pede.

## Dúvidas

- A lista que vazou está na persistência, e não "num serviço, no controller", que são os exemplos da régua. Mesmo assim li como uma decisão sobre a situação fora do tipo, porque a definição de decisão inclui "um conjunto que não cita todas as situações" e o efeito de uma situação nova seria o mesmo. Se o critério for só serviço e controller, o P7 seria acerto.
- O serviço nomear o destino de cada operação (`OrderStatus.PAID` em `confirmPayment` e assim por diante) não foi contado como decisão: não compara a situação atual, e uma situação nova não obriga a mudar essas linhas.

# State

## Visão geral

O agente separou a situação do pedido em dois tipos. `OrderStatus` é só um rótulo, um enum sem comportamento. `OrderOperation` é uma tabela de transições: cada operação (`PAY`, `SHIP`, `DELIVER`, `CANCEL`) diz de quais situações pode partir e para qual vai. Assim, a decisão sobre qual operação é permitida e para onde ela leva saiu dos serviços. O `OrderService` só chama `requireAllowed` e `target()`, sem nenhum `if` sobre a permissão. Esse é o ponto forte.

O problema mais sério é que duas decisões sobre a situação ficaram fora desse lugar:
- **O cancelamento**: a diferença entre cancelar um pedido pago e um não pago é decidida no serviço, com `order.status() == OrderStatus.PAID`. Esse booleano depois vira `if (wasPaid)` nos listeners.
- **A primeira compra**: o conjunto "conta como compra" (`PAID`, `SHIPPED`, `DELIVERED`) está escrito duas vezes na persistência.

Com a situação "em separação" que o enunciado anuncia, as duas teriam de mudar fora do tipo da situação. A do cancelamento falharia sem avisar: um pedido em separação cancelado não devolveria o estoque nem o crédito.

O aviso foi resolvido com um enum de duas constantes e uma troca simples, que é a resposta do tamanho do problema.

## Item a item

**P7, as decisões sobre a situação.**

As decisões que estão **dentro** da tabela de transições:
- as permissões, de onde cada operação pode partir (`src/main/java/shop/domain/OrderOperation.java:8-11`);
- a situação de destino de cada operação (`OrderOperation.java:21-23`);
- a recusa com `OPERATION_NOT_ALLOWED` (`OrderOperation.java:25-29`).

O serviço usa a tabela sem nomear nenhuma situação nos quatro fluxos (`src/main/java/shop/service/OrderService.java:45,54,77-78,88-89,98,104`).

As decisões que ficaram **fora**:
1. **Se há o que devolver no cancelamento.** `OrderService.java:99` compara `order.status() == OrderStatus.PAID`, e `OrderService.java:66` passa o literal `false` no cancelamento por expiração ou por falta de estoque. Detalhes no item seguinte.
2. **O que conta como compra (para o WELCOME10).** O conjunto `PAID || SHIPPED || DELIVERED` não cita todas as situações e está em `src/main/java/shop/persistence/memory/MemoryStore.java:104-105`. O mesmo conjunto aparece como SQL, `status IN ('PAID', 'SHIPPED', 'DELIVERED')`, em `src/main/java/shop/persistence/postgres/PgOrders.java:94`. Uma situação "em separação" teria de entrar nos dois lugares.
3. **Uma situação de destino repetida.** `OrderService.java:68` escreve `OrderStatus.CANCELLED` direto, em vez de `OrderOperation.CANCEL.target()`. É menor: só monta o retorno, porque `cancelOrder` já gravou pelo `target()` (`OrderService.java:104`).

A situação inicial em `src/main/java/shop/service/CheckoutService.java:32` (`AWAITING_PAYMENT`) não é uma decisão sobre a situação: é só o valor de partida.

Há uma dúvida sobre a casa. A tabela mora em `OrderOperation`, ao lado de `OrderStatus`, e não no próprio tipo da situação. Considerei que essa tabela de transições no domínio cumpre o papel pedido, porque uma situação nova mexeria em `OrderStatus` e `OrderOperation` e não na permissão dentro dos serviços. Mesmo assim, as decisões 1 e 2 continuam fora. Por isso a estrutura existe mas não cobre tudo.
Conclusão: malfeito

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença foi posta no serviço. O cancelamento calcula `order.status() == OrderStatus.PAID` (`OrderService.java:99`) e repassa o resultado como `boolean wasPaid` aos listeners. Cada listener decide com um `if`:
- `if (wasPaid)` devolve o estoque (`src/main/java/shop/service/StockListener.java:16`);
- `if (wasPaid)` tira o crédito (`src/main/java/shop/service/CreditListener.java:14`).

O aviso vai sempre (`src/main/java/shop/service/NotificationListener.java:20-21`), o que está certo. No cancelamento por recusa, o serviço fixa `false` à mão (`OrderService.java:66`).

A situação não diz se há o que devolver. Quem pergunta qual é a situação é o serviço, por cima da tabela de transições. Uma situação "em separação" (um pedido já pago) daria `wasPaid == false` sem ninguém perceber.

Fiquei em dúvida entre `omissão` e `malfeito`. Escolhi `malfeito` porque existe uma estrutura de transições e o serviço que a usa ainda pergunta qual é a situação.
Conclusão: malfeito

**P7-exagero, a situação do aviso.** É um enum de duas constantes (`src/main/java/shop/domain/NotificationStatus.java:3-6`) usado como campo do record `Notification` (`src/main/java/shop/domain/Notification.java:5`). A troca é direta:
- na persistência, `UPDATE notifications SET status = 'READ'` (`src/main/java/shop/persistence/postgres/PgNotifications.java:72`) e o equivalente em `MemoryStore.java:149`;
- no serviço, `QueryService.markRead` só confere se o aviso existe, marca e relê (`src/main/java/shop/service/QueryService.java:35-41`).

"Se já estava lido, continua lido" sai do próprio `UPDATE`, sem nenhum `if`. Não há classe por situação, máquina de estados nem interface de situação.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do State).** Não achei nada. `OrderOperation` atende ao P7. A interface `OrderEventListener` com três implementações (`src/main/java/shop/service/OrderEventListener.java:11`, `src/main/java/shop/service/Application.java:17-18`) atende ao pedido do enunciado sobre SMS e cashback, que é um ponto de outro pattern. `Order.withStatus` e `Order.withShipment` (`src/main/java/shop/domain/Order.java:9-15`) são métodos de cópia simples, não camadas.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr "há o que devolver" na situação.** Por exemplo, um `boolean refundsOnCancel()` (ou "foi pago") em cada constante de `OrderStatus`, e o cancelamento passaria `order.status().refundsOnCancel()`. É a decisão que quebraria sem aviso com a situação "em separação": estoque e crédito não voltariam. Hoje ela está num `==` no serviço e num literal `false`.
2. **Tirar o conjunto "conta como compra" da persistência.** Ele poderia virar uma pergunta ao tipo da situação, como `countsAsPurchase()`. O SQL de `PgOrders.java:94` seria montado a partir das constantes que respondem sim, em vez de repetir os nomes em dois repositórios.

## Dúvidas

- **A casa do P7.** As transições estão em `OrderOperation` (um enum de operações), e não em `OrderStatus` (o enum da situação). A régua diz que a casa é "o tipo da situação". Considerei a tabela de transições do domínio uma casa válida para as permissões, porque ela tira essas decisões dos serviços. Com a leitura estrita, a conclusão do P7 seria a mesma (`malfeito`), já que as decisões do cancelamento e da primeira compra estão fora de qualquer um dos dois.
- **O conjunto da primeira compra como decisão sobre a situação.** Ele está na persistência e serve à regra do cupom, mas é um conjunto que não cita todas as situações, então contei como decisão fora da casa.
- **A palavra do remendo.** Fiquei entre `omissão` e `malfeito`, como explicado no item. Escolhi `malfeito`.

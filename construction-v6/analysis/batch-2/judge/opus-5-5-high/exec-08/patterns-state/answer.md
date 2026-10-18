# padrões: State

## Visão geral

O agente não usou State para a situação do pedido. `OrderStatus` é um enum que só dá nome às situações, sem nenhum comportamento (`src/main/java/shop/Models.java:7`). Todas as decisões ficam no `OrderService`, em comparações como `order.status() != OrderStatus.X`: o que cada operação aceita, para onde o pedido vai e se o cancelamento tem algo a devolver. Com isso, uma situação nova como "em separação" obriga a mexer no serviço (`ship`, `cancel`) e nas consultas dos dois stores. Esse é o problema mais sério.

O cancelamento segue o mesmo caminho. A diferença entre cancelar um pedido pago e um não pago é um `order.status() == OrderStatus.PAID` dentro do serviço, que vira o parâmetro `wasPaid` e um `if (wasPaid)`. Os efeitos de devolução estão bem separados em `OrderEffect`, mas a decisão de rodá-los não fica na situação.

No aviso, a resposta tem o tamanho certo: um enum de dois valores e um `UPDATE ... SET status = 'READ'`. Perto deste pattern, não achei estrutura sem necessidade.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?** O enum `OrderStatus { AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED }` (`src/main/java/shop/Models.java:7`) não tem métodos nem transições.

- **Dentro do tipo da situação:** nenhuma decisão.
- **Fora, no serviço:**
  - o pagamento só é aceito se o pedido aguarda pagamento (`src/main/java/shop/OrderService.java:63`);
  - o envio só é aceito para pedido pago (`src/main/java/shop/OrderService.java:92`);
  - a entrega só é aceita para pedido enviado (`src/main/java/shop/OrderService.java:103`), e o destino `DELIVERED` é escolhido ali mesmo (`:104`);
  - o cancelamento só é aceito para `AWAITING_PAYMENT` ou `PAID`, um conjunto que não cita todas as situações (`src/main/java/shop/OrderService.java:113`), e o destino `CANCELLED` também é escolhido no serviço (`:121`).
- **Fora, no record `Order`:** o destino de duas transições está fixo em `Order.paid()` (`PAID`, `src/main/java/shop/Models.java:46`) e em `Order.shipped()` (`SHIPPED`, `:50`). O `Order` não é o tipo da situação.
- **Fora, na persistência:** o que conta como compra feita, para o WELCOME10, também é um conjunto parcial de situações. Ele aparece em dois lugares: `src/main/java/shop/MemoryStore.java:75-76` e o SQL `status IN ('PAID','SHIPPED','DELIVERED')` em `src/main/java/shop/JdbcStore.java:307`.

Não contei a constante inicial `OrderStatus.AWAITING_PAYMENT` no registro (`src/main/java/shop/OrderService.java:35`) nem a tradução do texto `OrderStatus.valueOf(...)` (`src/main/java/shop/JdbcStore.java:227`). A situação é só um rótulo, e as decisões estão em `if` nos serviços e nos stores.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença mora no serviço. `cancel` calcula `order.status() == OrderStatus.PAID` e passa o resultado como `wasPaid` (`src/main/java/shop/OrderService.java:116`). Depois, `cancelInTx` decide com `if (wasPaid)` se roda `onPaidOrderCancelled`, a parte que devolve o estoque e tira o crédito (`src/main/java/shop/OrderService.java:122-123`). O caminho da recusa no pagamento passa `false` direto (`src/main/java/shop/OrderService.java:83`), outra escolha feita no serviço e não na situação.

Os efeitos de desfazer ficam em classes próprias (`StockEffect.onPaidOrderCancelled`, `ClubCreditEffect.onPaidOrderCancelled` em `src/main/java/shop/OrderEffects.java`), e o aviso de qualquer cancelamento fica em `onCancelled` (`src/main/java/shop/OrderService.java:126`). Isso organiza o que fazer, mas não quem decide. É a situação que deveria dizer se há algo a devolver, e quem diz é um `if` sobre `PAID` dentro do cancelamento. Como P7 já é omissão, aqui não há estrutura de situação a remendar: a variação foi escrita direto no serviço.
Conclusão: omissão

**P7-exagero, a situação do aviso.** `NotificationStatus { NEW, READ }` é um enum simples (`src/main/java/shop/Models.java:11`), guardado como campo do record `Notification` (`:19`). O aviso nasce `NEW` (`src/main/java/shop/MemoryStore.java:101`; `'NEW'` no `INSERT` em `src/main/java/shop/JdbcStore.java:389`). Marcar como lido é uma troca direta e idempotente: um novo `Notification` com `READ` (`src/main/java/shop/MemoryStore.java:113`) e `UPDATE notifications SET status = 'READ'` (`src/main/java/shop/JdbcStore.java:417`). Não há classe por situação, máquina de estados nem interface.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do State).** Perto da situação e do cancelamento não há interface com uma só implementação, camada que só repassa nem fábrica.

- `OrderEffect` tem três implementações e atende ao pedido do enunciado de no futuro mandar SMS e dar cashback. Esse ponto fica fora deste pattern.
- `Store`/`Tx` têm duas implementações em uso: `MemoryStore` nos testes e `JdbcStore` em produção.
- O record privado `Outcome` (`src/main/java/shop/OrderService.java:48`) é só um resultado auxiliar da transação, não uma camada.

Não achei nenhum caso a registrar.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr as transições dentro de `OrderStatus`.** Cada constante passaria a responder `canPay()`, `canShip()`, `canDeliver()`, `canCancel()` e o destino de cada operação. Os quatro métodos do `OrderService` deixariam de citar situações. Assim, uma situação "em separação" entre `PAID` e `SHIPPED` seria uma constante nova com suas transições, sem tocar em `ship` e `cancel`.
2. **Deixar a situação dizer se o cancelamento tem o que devolver.** Por exemplo, um `hasPaymentToRefund()` no enum, verdadeiro em `PAID` e numa futura "em separação". Isso troca o `== OrderStatus.PAID` de `src/main/java/shop/OrderService.java:116`. Na mesma linha, o conjunto "conta como compra feita" (`MemoryStore.java:75`, `JdbcStore.java:307`) poderia vir de um método do enum, em vez de estar repetido em dois stores.

## Dúvidas

- Contei os destinos fixos em `Order.paid()` e `Order.shipped()` (`src/main/java/shop/Models.java:46,50`) como decisões fora do tipo da situação: `Order` é o pedido, não a situação. Eles não mudam a conclusão, que já é omissão pelos `if` do serviço.
- Contei o conjunto `PAID/SHIPPED/DELIVERED` de `hasFulfilledOrder` como decisão sobre a situação, porque é um conjunto que não cita todas as situações, como a régua define. Ele serve à regra do cupom e não a uma transição, e reforça a omissão sem ser a causa dela.
- No remendo, como não existe estrutura de situação, a régua admite tanto omissão quanto malfeito. Escolhi omissão: malfeito pressupõe um tipo de situação com as transições, e aqui ele não existe.

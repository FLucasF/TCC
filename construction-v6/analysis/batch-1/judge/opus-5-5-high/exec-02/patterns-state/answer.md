# Padrões: State

## Visão geral

O agente não usou o padrão State. `OrderStatus` é um enum que só lista as cinco situações, sem transições e sem métodos (`domain/enums/OrderStatus.java:3-9`). Todas as regras sobre a situação ficam no `OrderService`:

- se o pagamento, o envio, a entrega e o cancelamento são permitidos;
- para qual situação o pedido vai em cada caso;
- se o cancelamento tem algo para devolver.

Essas regras estão escritas como comparações com constantes e como `setStatus` espalhados pelos quatro métodos. Para criar a situação "em separação" (o enunciado diz que estão pensando nela), seria preciso mudar o serviço em vários lugares, e não só o tipo da situação. A diferença entre cancelar um pedido pago e um não pago é exatamente o remendo previsto: um `if (status == PAID)` dentro do cancelamento. O ponto forte está no controle negativo: a situação do aviso é um enum de dois valores e uma troca simples, sem nenhuma estrutura a mais. Também não há estrutura desnecessária perto deste pattern.

## Item a item

**P7, a situação do pedido.** A casa seria `OrderStatus` (`domain/enums/OrderStatus.java:3-9`), mas ela não guarda nenhuma decisão. São só cinco constantes, sem método, sem lista de transições e sem corpo próprio. Uma busca por `OrderStatus` e pelos nomes das situações fora do serviço só acha declarações de campo (`domain/entity/Order.java:30`, `dto/OrderResponse.java:18`). Os controllers não decidem nada sobre a situação (`controller/OrderController.java`). Todas as decisões estão fora da casa, em `service/OrderService.java`:

- **o pagamento é permitido?** `order.getStatus() != OrderStatus.AWAITING_PAYMENT` (`:71`);
- **o pagamento passa para PAID:** `setStatus(OrderStatus.PAID)` (`:121`). As duas recusas passam para CANCELLED: o prazo vencido (`:89`) e a falta de estoque (`:114`);
- **o envio é permitido?** `order.getStatus() != OrderStatus.PAID` (`:171`). O envio passa para SHIPPED (`:175`);
- **a entrega é permitida?** `order.getStatus() != OrderStatus.SHIPPED` (`:194`). A entrega passa para DELIVERED (`:198`);
- **o cancelamento é permitido?** `== OrderStatus.SHIPPED || == OrderStatus.DELIVERED` (`:214`). É um conjunto que não cita todas as situações. Como CANCELLED ficou de fora, um pedido já cancelado pode ser "cancelado" de novo. É um exemplo do que acontece quando a regra fica espalhada (se isso aparece no serviço rodando, quem mede é o comportamento). O cancelamento passa para CANCELLED (`:251`);
- **a situação inicial:** `.status(OrderStatus.AWAITING_PAYMENT)` (`:50`).

O `setStatus` vem do `@Data` do Lombok (`domain/entity/Order.java:12`), e por isso qualquer código pode pôr qualquer situação, sem passar por uma regra de transição. Pelo teste da régua: para criar "em separação" entre PAID e SHIPPED, seria preciso mudar `:171` (o envio passaria a sair da nova situação), `:214` e `:218` (cancelar um pedido em separação também precisa devolver estoque e crédito), além de criar uma operação nova no serviço. A situação é só um rótulo, e as decisões estão em `if` nos serviços.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença fica no serviço, no lugar exato do remendo: `if (order.getStatus() == OrderStatus.PAID) { ... }` (`service/OrderService.java:218-249`). Dentro desse `if` o estoque é devolvido (`:228-238`) e o crédito sai do saldo (`:240-248`). Fora dele, para qualquer situação, ficam a troca para CANCELLED e o aviso (`:251-254`). A situação não sabe se há algo para devolver. A pergunta é feita pelo nome do caso (`== PAID`), e uma situação paga nova ("em separação") ficaria sem devolução até alguém mexer nesse `if`. Como não existe nenhuma estrutura de situação onde essa regra pudesse morar, não dá para chamar de malfeito. É a variação escrita sem o padrão.
Conclusão: omissão

**P7-exagero, a situação do aviso.** `NotificationStatus` é um enum simples com `NEW` e `READ` (`domain/enums/NotificationStatus.java:3-6`), guardado como campo da entidade (`domain/entity/Notification.java:34-35`). O aviso nasce `NEW` (`service/OrderService.java:287`). Marcar como lido é uma troca direta, `notification.setStatus(NotificationStatus.READ)` (`service/CustomerService.java:70`), sem condição. Por isso um aviso já lido continua lido, como o enunciado pede. Não há classe por situação, máquina de estados nem interface.
Conclusão: contenção

**E, estrutura sem necessidade perto do pattern.** Não achei nenhuma. As únicas interfaces do projeto são os quatro repositórios do Spring Data (`repository/*Repository.java`), que são a forma normal de acessar o banco. Em volta das situações e dos avisos não há camada que só repassa, nem fábrica ou builder de situação. Os `@Builder` do Lombok nas entidades são só construtores gerados. Também não há interface de uma implementação só: `TransLogClient` é uma classe concreta, e ela pertence a outro pattern.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr as transições no próprio `OrderStatus`.** Cada constante diria o que pode fazer e para onde vai: `canPay()`, `canShip()`, `canCancel()`, ou um `next(Operation)` que recusa o que não é permitido. O `OrderService` passaria a perguntar à situação em vez de comparar constantes (`:71`, `:171`, `:194`, `:214`). Assim a situação "em separação" seria uma constante nova, e não uma mudança em quatro métodos. De quebra, isso fecharia o furo do cancelamento repetido, que hoje existe porque `:214` não cita CANCELLED.
2. **Fazer a situação dizer se há o que devolver no cancelamento**, por exemplo com um `refundsOnCancel()`, verdadeiro em PAID e numa futura "em separação". Isso tira o `if (== PAID)` de `OrderService.java:218`, que é exatamente onde o próximo bug apareceria quando a nova situação paga entrar.

## Dúvidas

- Contei as trocas `setStatus(...)` como decisões fora da casa, porque elas dizem "o que muda em cada transição", que a régua põe no tipo da situação. A situação inicial (`:50`) entrou na mesma lista, embora seja mais fraca como decisão. Nenhuma das duas escolhas muda a conclusão, porque dentro do tipo não há decisão nenhuma.
- No remendo, a estrutura nem existe, então o remendo não tinha "por cima" do que ficar. Classifiquei como omissão, e não como malfeito, porque malfeito pressupõe uma estrutura de situação que cobre só parte das regras.
- O cupom WELCOME10 não confere a primeira compra (`service/CheckoutService.java:220-221`), que seria uma regra sobre as situações ("pago, enviado ou entregue; cancelado não conta"). Como não existe no código, não entrou na contagem de decisões. Se isso é uma falha, quem mede é o comportamento.

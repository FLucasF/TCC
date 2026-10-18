# Padrões: State

## Visão geral

O agente modelou a situação do pedido como um State clássico: uma interface `OrderState` (`state/OrderState.java:7-13`) com quatro operações (pagar, enviar, entregar, cancelar) e uma classe por situação (`AwaitingPaymentState`, `PaidState`, `ShippedState`, `DeliveredState`, `CancelledState`). Cada classe decide se a operação é permitida e para onde o pedido vai. Os serviços não têm `if` nem `switch` sobre a situação nas operações. O `OrderService` só traduz o texto gravado para o objeto da situação (`service/OrderService.java:47`, `:124`, `:151`, `:174`, `:197`) e delega para ele.

O remendo do cancelamento ficou no lugar certo. Só `PaidState` devolve estoque e crédito, e o serviço não tem `if (status == PAID)`.

O controle negativo foi contido: o aviso tem um campo `String status` e uma troca para `"READ"`.

O problema mais sério é a regra da "primeira compra" do WELCOME10. Ela decide quais situações contam como compra feita com uma lista que cita só três das cinco situações. Essa lista está escrita duas vezes, fora do tipo da situação: em `service/CheckoutService.java:227` e em `strategy/coupon/Welcome10Coupon.java:26`. Uma situação nova, como a "em separação", obriga a mexer nesses dois lugares, e não só no tipo da situação.

Há dois problemas menores:
- a situação existe duas vezes no `Order`: o texto `status`, que manda de fato, e um campo `@Transient state` que é escrito e nunca lido;
- o tratamento do aviso de pagamento repetido está copiado em quatro classes.

## Item a item

**P7, a situação do pedido.** O tipo da situação é a família `OrderState`, uma classe por situação. As decisões que estão dentro dele:

- **o que cada situação permite:**
  - `AwaitingPaymentState` aceita pagar e cancelar e recusa enviar e entregar (`state/AwaitingPaymentState.java:56-63`);
  - `PaidState` aceita enviar e cancelar (`state/PaidState.java:28-50`);
  - `ShippedState` aceita só entregar (`state/ShippedState.java:33-37`);
  - `DeliveredState` e `CancelledState` recusam tudo (`state/DeliveredState.java:28-40`, `state/CancelledState.java:28-40`).
- **para onde cada transição vai:** cada transição troca o `status` e o objeto na própria classe (por exemplo, `PaidState.java:30-31` vai para `SHIPPED`).
- **o que muda em cada transição:**
  - o pagamento baixa o estoque, dá o crédito e avisa o cliente (`AwaitingPaymentState.java:44-48`);
  - o envio grava o rastreio (`PaidState.java:29`);
  - o vencimento de 48 h e a falta de estoque cancelam o pedido (`AwaitingPaymentState.java:28-42`).

O `OrderService` não pergunta qual é a situação. Ele busca o objeto no mapa montado por `getStatusCode()` e chama o método. Isso é tradução do texto para o objeto e não conta como decisão. As consultas de que as situações precisam (o pagamento já registrado, se venceu, se há estoque) chegam pela interface `OrderStateContext`, sem que a situação saiba do serviço.

Ficaram fora do tipo da situação:

- **A regra "conta como primeira compra":** `CheckoutService.java:226-227` busca os pedidos do CPF com a lista `List.of("PAID", "SHIPPED", "DELIVERED")`, e `Welcome10Coupon.java:26` repete a mesma escolha com `status.equals("PAID") || status.equals("SHIPPED") || status.equals("DELIVERED")`. As duas são um conjunto que não cita todas as situações, ou seja, uma decisão sobre a situação. Se a situação "em separação" entrar, as duas linhas precisam mudar. O lugar dessa pergunta era a própria situação, algo como um "este pedido conta como compra realizada?".
- **A situação inicial:** ela é escrita no serviço (`OrderService.java:66`, `order.setStatus("AWAITING_PAYMENT")`). É uma atribuição, não uma comparação, por isso pesa pouco. Registro só porque nomeia a situação fora do tipo dela.

Há também uma fragilidade de desenho que não tira decisão do lugar. O `Order` guarda a situação duas vezes: o texto `status` (`domain/Order.java:20`), que é o que vale, e o objeto `@Transient state` (`domain/Order.java:74-75`). As classes de situação escrevem os dois a cada transição (por exemplo, `PaidState.java:30-31`), mas `getState()` não é chamado em lugar nenhum do código. Além disso, as situações são `@Component` (singletons registrados no mapa), mas as transições criam instâncias novas com `new CancelledState()`. Nada disso quebra a regra, mas há duas fontes da mesma verdade.

Também há código repetido dentro das casas: a checagem do aviso de pagamento repetido (mesmo `transactionId` → não faz nada, senão `OPERATION_NOT_ALLOWED`) está copiada em `PaidState.java:19-25`, `ShippedState.java:19-25`, `DeliveredState.java:19-25` e `CancelledState.java:19-25`. Não é decisão fora da casa, e uma classe-base ou um método `default` resolveria.

A estrutura cobre todas as transições, mas uma decisão sobre a situação (quais situações contam como compra) mora no serviço de checkout e na estratégia do cupom.
Conclusão: malfeito

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está na situação. `AwaitingPaymentState.handleCancellation` (`state/AwaitingPaymentState.java:66-71`) só troca para `CANCELLED`, grava e avisa. `PaidState.handleCancellation` (`state/PaidState.java:42-50`) devolve o estoque (`context.releaseStock`, `:43`), tira o crédito (`context.removeCredit`, `:44`), troca para `CANCELLED` e avisa. O `OrderService.cancelOrder` (`service/OrderService.java:193-213`) não tem `if` sobre a situação: só delega para `state.handleCancellation`. A regra "depois de enviado não dá mais" também mora nas situações (`ShippedState.java:40-42`, `DeliveredState.java:38-40`, `CancelledState.java:38-40`). O aviso de cancelamento ficou repetido em cada caminho (`AwaitingPaymentState.java:32`, `:40`, `:70`; `PaidState.java:49`), mas cada um está na sua casa.
Conclusão: acerto

**P7-exagero, a situação do aviso.** O aviso tem um campo `String status` que começa como `"NEW"` no construtor (`domain/Notification.java:21`, `:32`). Marcar como lido é uma atribuição direta, `notification.setStatus("READ")` (`controller/NotificationController.java:55`). Isso também cumpre "se já estava lido, continua lido" sem nenhuma condição. Não há classe por situação do aviso, máquina de estados nem interface. A troca feita no controller, e não no domínio, é assunto da arquitetura, não deste item.
Conclusão: contenção

**E, estrutura sem necessidade perto do State.** A peça a examinar é a interface `OrderStateContext` (`state/OrderStateContext.java:8-20`), que tem uma implementação só (`OrderService`, `service/OrderService.java:21`). Ela não é estrutura fora de um ponto. É o contexto do próprio State: deixa as situações chamarem estoque, crédito e aviso sem depender do pacote `service`, que já depende de `state`. Por isso não conto como exagero. Ela traz, porém, um método que nenhuma situação usa: `findOrdersByCustomerCpfAndStatuses` (`OrderStateContext.java:17`), implementado em `OrderService.java:292-295` e nunca chamado pelo pacote `state`. É código morto, não uma camada a mais. Não achei perto do State nenhuma fábrica, builder ou camada que só repasse.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar para a situação a pergunta "conta como compra realizada?"**, com um método como `countsAsPurchase()` em `OrderState`. O WELCOME10 e o checkout passariam a perguntar a qualquer situação, e não mais a comparar textos (`CheckoutService.java:227`, `Welcome10Coupon.java:26`). Assim a situação "em separação" que o enunciado anuncia mexeria só no tipo da situação, como o pedido exige.
2. **Ter uma fonte só da situação no `Order`.** Ou se tira o campo `@Transient state`, que nunca é lido, ou ele passa a ser o que a entidade expõe, com o texto só na gravação. Junto, o tratamento do aviso de pagamento repetido, hoje copiado em quatro classes, iria para uma classe-base ou um método `default`.

## Dúvidas

- **A situação inicial no serviço.** Tratei `order.setStatus("AWAITING_PAYMENT")` (`OrderService.java:66`) como um ponto menor, e não como uma decisão fora da casa: a régua define decisão como comparação, rótulo de `switch` ou conjunto parcial, e isso é uma atribuição. O peso do "malfeito" vem só da lista da primeira compra.
- **A lista do WELCOME10.** Ela cruza o P7 com o cupom (P2). Contei como decisão sobre a situação do pedido porque escolhe quais situações contam e cita três de cinco. Na régua da Strategy, ela pode ser lida de outro jeito.
- **`OrderStateContext` no item E.** Ela cabe na letra do E ("uma interface com uma implementação só"). Tratei como parte da estrutura do próprio P7, e não como estrutura fora dos pontos, porque cumpre o papel de contexto do State e evita que `state` dependa de `service`.

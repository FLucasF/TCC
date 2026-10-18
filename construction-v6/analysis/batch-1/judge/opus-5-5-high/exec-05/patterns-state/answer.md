Já li o enunciado e o código das situações. Segue a correção.

# Padrões: State

## Visão geral

O agente não aplicou o State em nenhum lugar. `OrderStatus` é um enum só com os nomes das situações, sem nenhum comportamento (`src/main/java/com/loja/orderservice/model/OrderStatus.java:3-9`). Todas as decisões sobre a situação do pedido ficaram no `OrderService`: o que cada operação permite, para onde o pedido vai e o que muda em cada transição são comparações `order.getStatus() == / != OrderStatus.X` seguidas de `setStatus(...)`. Sobra ainda um conjunto de situações dentro da regra do cupom, no `CheckoutCalculator`. O problema mais sério é a omissão no P7. Por causa dela, o cancelamento também virou um remendo, com um `if (status == PAID)` dentro do serviço. A única coisa certa é o aviso ao cliente, resolvido do tamanho do problema: um campo e um método `markAsRead()`.

## Item a item

**P7, a situação do pedido.** Nenhuma decisão sobre a situação mora no tipo dela: `OrderStatus` não tem métodos, nem transições, nem corpo nas constantes (`model/OrderStatus.java:3-9`). Todas estão fora, no serviço:
- **Pagamento:** só é aceito em `AWAITING_PAYMENT` (`service/OrderService.java:95`). O destino é fixado na mão: `CANCELLED` quando o pagamento chega tarde (`:101`) ou falta estoque (`:111`), e `PAID` quando dá certo (`:123`).
- **Envio:** só a partir de `PAID` (`:141`), e o destino `SHIPPED` é fixado na mão (`:145`).
- **Entrega:** só a partir de `SHIPPED` (`:159`), e o destino `DELIVERED` é fixado na mão (`:163`).
- **Cancelamento:** é proibido em `SHIPPED || DELIVERED` (`:174`). Esse conjunto não cita todas as situações, então uma situação nova como "em separação" obrigaria a rever esta linha. Ainda no cancelamento: um pedido já `CANCELLED` devolve o pedido sem fazer nada (`:178`), o `if` sobre `PAID` decide o que devolver (`:182`) e o destino `CANCELLED` é fixado na mão (`:191`).
- **Regra do WELCOME10:** o conjunto `PAID, SHIPPED, DELIVERED` decide quais pedidos contam como compra feita (`service/CheckoutCalculator.java:307-308`).

Também não há encapsulamento: `Order.setStatus` é público (`entity/Order.java:97-99`), e qualquer código pode pôr o pedido em qualquer situação. A única linha sobre a situação dentro da entidade é a situação inicial, `AWAITING_PAYMENT`, no construtor (`entity/Order.java:66`), e ela não é uma decisão de transição. Uma situação "em separação" exigiria mudar `shipOrder` (`:141`), `cancelOrder` (`:174` e talvez `:182`) e o `isFirstPurchase`, e não só o tipo da situação. É exatamente o que o enunciado pede para evitar. A situação é só um rótulo, e as decisões estão em `if` nos serviços.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está num `if (order.getStatus() == OrderStatus.PAID)` dentro de `cancelOrder`, no serviço (`service/OrderService.java:182-189`). Dentro desse `if` o estoque volta (`releaseStock`, `:183`) e o crédito sai do saldo (`:185-188`). O caso do pedido não pago é só não entrar no `if`. Quem diz se há o que devolver é o serviço, e não a situação. Como o P7 não tem estrutura nenhuma, este é o remendo no lugar errado descrito na régua. A conclusão não é "malfeito" porque não existe estrutura de situação que tenha deixado esta regra de fora: o que falta é a própria estrutura.
Conclusão: omissão

**P7-exagero, a situação do aviso.** A resposta é simples. Há um enum de duas constantes (`model/NotificationStatus.java:3-6`), um campo que começa em `NEW` no construtor (`entity/Notification.java:27,30,37`) e um método `markAsRead()` que só atribui `READ` (`entity/Notification.java:80-82`). O serviço chama esse método sem perguntar nada (`service/NotificationService.java:52`). Não há classe por situação, nem máquina de estados, nem interface. O "se já estava lido, continua lido" funciona sem nenhum `if`, porque a troca é idempotente.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do State).** Perto da situação do pedido e do aviso não há estrutura que ninguém pediu. As únicas interfaces do projeto são os repositórios do Spring Data (`repository/*.java`), e não há classe-base nem fábrica em volta das situações. O `TransLogService`, com o `QuoteResult` e as fábricas estáticas `success/notServed/unavailable` (`service/TransLogService.java:66-89`), fica longe deste pattern e não pesa aqui.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr as transições dentro de `OrderStatus`.** Cada constante diria o que permite: `canShip()`, `canDeliver()`, `canCancel()`, `acceptsPayment()`, ou um `next(Operation)` que devolve o destino ou recusa. Outra opção é ter uma classe por situação, com `Order` delegando a ela e sem `setStatus` público. Assim a situação "em separação" que o enunciado anuncia entraria como uma constante nova, sem tocar em `shipOrder` nem em `cancelOrder`.
2. **Tirar o `if (status == PAID)` do cancelamento.** A própria situação diria se há o que devolver, por exemplo com um `hasSomethingToRefund()`, ou com um `onCancel(...)` que em `PAID` devolve o estoque e o crédito e em `AWAITING_PAYMENT` não faz nada. Pelo mesmo caminho, o conjunto do `isFirstPurchase` (`CheckoutCalculator.java:307-308`) viraria uma pergunta à situação, como `countsAsPurchase()`.

## Dúvidas

- **Classificação do remendo.** Pela régua, o remendo é "malfeito" quando a estrutura existe e uma regra fica fora dela. Aqui não existe estrutura nenhuma, então a diferença entre pago e não pago está no `if` como qualquer outra decisão. Por isso usei "omissão", que é a mesma leitura do P7.
- **O conjunto do WELCOME10.** Contei como decisão fora do tipo o conjunto `PAID, SHIPPED, DELIVERED` em `CheckoutCalculator.java:307-308`, que é uma consulta ao repositório e não uma transição. Ele cabe na definição da régua ("um conjunto que não cita todas as situações"), mas não pertence à máquina de transições propriamente dita. Ele não muda a conclusão do P7, que já é omissão sem ele.

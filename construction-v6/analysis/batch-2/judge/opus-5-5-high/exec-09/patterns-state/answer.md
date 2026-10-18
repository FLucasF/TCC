Já li o enunciado e todo o código que toca a situação do pedido e do aviso. Segue a correção.

# Padrões: State

## Visão geral

O agente não usou o padrão State. A situação do pedido é um enum sem comportamento: só as cinco constantes, em `models/Enums.java:20-22`. Todas as decisões ficam fora dele. A regra de qual operação cada situação permite e o destino de cada transição estão espalhados em `if`s que comparam a situação, divididos entre o controller (`api/Routes.java`, para envio, entrega e cancelamento) e o `PaymentService` (para o pagamento). Há também uma consulta SQL que repete à mão quais situações contam como compra (`services/CheckoutService.java:324`). O problema mais sério é que uma situação nova ("em separação") obrigaria a mexer em pelo menos quatro arquivos, e não no tipo da situação. Pelo mesmo caminho, o remendo do cancelamento ficou como um `if (status == PAID)` dentro do handler HTTP. A situação do aviso foi resolvida do tamanho certo: um enum `NEW/READ`, um campo e um `UPDATE`. Não achei estrutura desnecessária perto deste pattern.

## Item a item

**P7, a situação do pedido.** O tipo da situação, `enum OrderStatus { AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED }` (`models/Enums.java:20-22`), não guarda nenhuma decisão: não tem método, transição nem corpo nas constantes. No `Order`, a situação é só um campo público (`models/Order.java:8`).

Nenhuma decisão fica dentro do tipo da situação. Estas ficam fora:
- **Pagar só se aguarda o pagamento:** `order.status != Enums.OrderStatus.AWAITING_PAYMENT` (`services/PaymentService.java:43`). O destino `PAID` também é escolhido no serviço (`services/PaymentService.java:78`), e o destino `CANCELLED` por expiração ou falta de estoque também (`services/PaymentService.java:55`, `:63`).
- **Enviar só se pago:** `order.status != Enums.OrderStatus.PAID` (`api/Routes.java:107`), com o destino `SHIPPED` decidido no controller (`api/Routes.java:114`).
- **Entregar só se enviado:** `order.status != Enums.OrderStatus.SHIPPED` (`api/Routes.java:133`), com o destino `DELIVERED` decidido no controller (`api/Routes.java:139`).
- **Cancelar:** `order.status == SHIPPED || order.status == DELIVERED` (`api/Routes.java:155`). É um conjunto que não cita todas as situações, então a decisão também depende dele. Como a lista é das situações proibidas, uma situação nova seria cancelável sem ninguém decidir isso. `CANCELLED` também não está na lista. Se recancelar um pedido cancelado é um problema, quem mede é a dimensão do comportamento.
- **Quais situações contam como compra para o WELCOME10:** `status = 'PAID' OR status = 'SHIPPED' OR status = 'DELIVERED'` (`services/CheckoutService.java:324`). Essa decisão sobre a situação está escrita em SQL no serviço de checkout. A situação "em separação" teria de entrar aqui também.

Não contei como decisão a tradução do texto do banco para a constante (`Enums.OrderStatus.valueOf(...)`, `services/OrderService.java:102`), nem a situação inicial gravada no registro (`services/OrderService.java:30`).

A situação é só um rótulo, e as decisões estão em `if`s no controller e nos serviços. É o caso de omissão que a régua descreve.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença ficou num `if (order.status == Enums.OrderStatus.PAID)` (`api/Routes.java:163-168`). Ele devolve o estoque (`:165`) e tira o crédito do saldo (`:167`), e fica dentro do handler HTTP `cancelOrder`, nem mesmo num serviço. Nada na situação diz se há o que devolver: o enum não tem comportamento (`models/Enums.java:20-22`). Também ficou fora da situação o aviso de cancelamento: ele se repete em três lugares, `api/Routes.java:171`, `services/PaymentService.java:57` e `:65`. Esse ponto é mais de duplicação que de State. É o remendo que a régua descreve, sem estrutura nenhuma por baixo.
Conclusão: omissão

**P7-exagero, a situação do aviso.** É um enum de duas constantes, `NotificationStatus { NEW, READ }` (`models/Enums.java:28-30`), num campo de `Notification` (`models/Notification.java:7`). O aviso começa como `NEW` na criação (`services/NotificationService.java:21`). A marcação como lido é um `UPDATE notifications SET status = 'READ'` sem condição (`services/NotificationService.java:64-72`), e por isso "se já estava lido, continua lido" sai de graça. Não há classe por situação, interface nem máquina de estados. É a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** O projeto não tem nenhuma `interface`, classe `abstract`, `implements` ou `extends` (busca em `src/main/java` sem resultado). Os serviços são classes com métodos estáticos chamados diretamente. A única classe auxiliar perto deste pattern é `PaymentResult` (`services/PaymentService.java:14-30`). É um objeto de retorno simples que leva o erro ou o pedido ao controller, e não é uma camada nem uma abstração sem uso. Perto do State não há estrutura desnecessária.
Conclusão: contenção

## O que mudaria primeiro

1. **Pôr as transições no próprio `OrderStatus`.** Cada constante diria o que permite e para onde vai, por exemplo com métodos como `pay()`, `ship()`, `deliver()` e `cancel()` que devolvem a próxima situação ou recusam a operação. A pergunta "conta como compra?" também viraria um método da situação, e a consulta de `services/CheckoutService.java:324` passaria a usar a lista que vem do enum. Com isso, `api/Routes.java:107`, `:133`, `:155` e `services/PaymentService.java:43` deixam de nomear situações, e a situação "em separação" passa a ser uma constante nova no enum, sem mexer no controller nem nos serviços.
2. **Fazer a situação dizer se o cancelamento tem o que devolver** (algo como `refundsOnCancel()`, verdadeiro só em `PAID`). Também convém tirar o cancelamento do controller e levá-lo a um serviço que use essa resposta. Assim o `if (status == PAID)` de `api/Routes.java:163` some, e uma situação "em separação" (já paga) devolve o estoque e o crédito sem precisar de mais um `||`.

## Dúvidas

- Contei a consulta SQL de `services/CheckoutService.java:324` como decisão sobre a situação fora do tipo. Ela é um conjunto que não cita todas as situações, e uma situação nova teria de entrar ali. É a leitura literal da definição do P7, mas o lugar não é uma transição.
- No P7-remendo, usei `omissão` porque não existe estrutura de situação nenhuma. O `if` está no controller, e não num serviço como diz a régua, mas isso não muda a conclusão.

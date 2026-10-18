# Padrões: State

## Visão geral

O agente não aplicou o State em lugar nenhum. `OrderStatus` é só um rótulo: um enum com as cinco constantes e um `fromString` que traduz o texto (`model/OrderStatus.java:3-13`). Todas as decisões sobre a situação ficam em `if` dentro de `OrderService`: qual operação cada situação permite, para onde o pedido vai em cada transição e se o cancelamento devolve estoque e crédito. Uma delas também fica no SQL de `OrderRepository`. Por isso o problema mais sério é a omissão no P7, e o remendo do cancelamento caiu no lugar esperado: um `wasPaid = order.status == OrderStatus.PAID` no serviço. A situação "em separação", que o enunciado anuncia, obrigaria a mexer no serviço e no repositório, não só no tipo da situação. O controle negativo foi bem resolvido: o aviso tem um enum de dois valores e uma troca simples para `READ`. Perto deste pattern também não há estrutura sem necessidade.

## Item a item

**P7, a situação do pedido.** O tipo da situação não decide nada. `OrderStatus` só tem as constantes e o `fromString` (`model/OrderStatus.java:4-12`), e o `fromString` nem é chamado (só aparece na própria declaração). Estas decisões ficam fora dele:
- **Pagamento:** só é permitido aguardando pagamento (`service/OrderService.java:61`). O destino em caso de atraso é `CANCELLED` (`service/OrderService.java:77`), em caso de falta de estoque também é `CANCELLED` (`service/OrderService.java:199`) e, no sucesso, é `PAID` (`service/OrderService.java:91`, `:106`).
- **Envio:** só é permitido a partir de `PAID` (`service/OrderService.java:121`) e leva a `SHIPPED` (`:126`, `:129`).
- **Entrega:** só é permitida a partir de `SHIPPED` (`service/OrderService.java:149`) e leva a `DELIVERED` (`:153`, `:154`).
- **Cancelamento:** é proibido em `DELIVERED`, `CANCELLED` e `SHIPPED` (`service/OrderService.java:165`, `:169`) e leva a `CANCELLED` (`:175`, `:191`).
- **O que conta como compra feita (para o WELCOME10):** um conjunto que não cita todas as situações, `status IN ('PAID', 'SHIPPED', 'DELIVERED')` (`repository/OrderRepository.java:109`). Ele é chamado por `strategy/Welcome10Coupon.java:23`.

Nenhuma decisão mora dentro do tipo. O agente também não fez classes por situação nem métodos como `canShip()` ou `canCancel()`. Para criar a situação "em separação" seria preciso mudar `OrderService.java:121` (o envio passaria a sair de outra situação), conferir a lista negra do cancelamento (`:165-170`) e mudar o `IN (...)` de `OrderRepository.java:109`. É a variação escrita sem o padrão.
Conclusão: omissão

**P7-remendo, cancelar um pedido pago e um não pago.** A diferença está no serviço, num `if` sobre a situação: `boolean wasPaid = order.status == OrderStatus.PAID;` (`service/OrderService.java:173`) e depois `if (wasPaid) { restoreStock(order); subtractCustomerCredit(order); }` (`:177-180`). A situação não sabe dizer se há o que devolver. Como não existe estrutura de situação, não dá para dizer que a estrutura existe e a regra ficou de fora (malfeito). É o remendo dentro de uma omissão.
Conclusão: omissão

**P7-exagero, a situação do aviso.** `NotificationStatus` é um enum simples com `NEW` e `READ` (`model/NotificationStatus.java:3-4`), guardado num campo de `Notification` (`model/Notification.java:8`). Marcar como lido é um `UPDATE notifications SET status = ?` sem condição, que grava `READ` (`repository/NotificationRepository.java:62-72`). Depois o endpoint atribui `notification.status = NotificationStatus.READ` (`api/NotificationEndpoint.java:32-33`). O agente não criou classe por situação, máquina de estados nem interface. Um aviso que já estava lido continua lido sem nenhum ramo especial. É a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern não há nada a mais. As duas situações têm só enums simples, sem interface, fábrica ou camada intermediária. Encontrei código morto, mas não estrutura: o `fromString` de `OrderStatus` e de `NotificationStatus` (`model/OrderStatus.java:6`, `model/NotificationStatus.java:6`) e o método `existsWithStatus` (`repository/OrderRepository.java:91`), que não são chamados por ninguém. Algumas estruturas do projeto ficam perto de outros patterns e não entram aqui: `ShippingCarrier` com uma implementação só (`shipping/TransLogAdapter.java`), que o enunciado justifica pela troca possível de transportadora, e `strategy/ProductProvider.java`. Ficam para as correções do Adapter e do Strategy.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar as transições para `OrderStatus`.** Cada constante passaria a dizer quais operações aceita e para onde vai, por exemplo com `pay()`, `ship()`, `deliver()` e `cancel()` devolvendo a próxima situação ou recusando, ou com `canShip()`/`canCancel()` e `next...()`. Os `if (order.status != ...)` de `OrderService.java:61`, `:121`, `:149` e `:165-170` virariam uma única pergunta ao tipo. Com isso, criar "em separação" mexeria só no enum, que é exatamente a mudança que o enunciado anuncia.
2. **Pôr no tipo a pergunta "há o que devolver?" e "conta como compra feita?"**. Seria um método como `refundsOnCancel()`, que substitui o `wasPaid` de `OrderService.java:173`, e um `countsAsPurchase()`, que gera a lista do `IN (...)` de `OrderRepository.java:109` a partir das constantes em vez de deixá-la fixa no SQL.

## Dúvidas

- **O `IN ('PAID', 'SHIPPED', 'DELIVERED')` em SQL** (`repository/OrderRepository.java:109`). Ele não compara com `OrderStatus`, mas é um conjunto que não cita todas as situações, e por isso eu o contei como uma decisão sobre a situação fora da casa dela. Ele não muda a conclusão do P7, porque nenhuma decisão está dentro do tipo.
- **A conclusão do remendo.** A régua define malfeito como a estrutura existir e uma regra ficar fora dela. Aqui não há estrutura nenhuma, então escolhi `omissão` para o remendo, em linha com o P7.

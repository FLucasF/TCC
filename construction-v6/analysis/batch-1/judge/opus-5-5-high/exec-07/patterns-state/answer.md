# Padrões: State

## Visão geral

O agente não usou o State. `OrderStatus` é um enum que só tem as cinco constantes, sem nenhum comportamento (`domain/enums/OrderStatus.java:3-9`). `Order` expõe um `setStatus` público (`domain/entity/Order.java:100`). Todas as decisões sobre a situação estão no `OrderService`: qual operação cada situação permite, para onde cada transição leva, e o que o cancelamento devolve conforme a situação. São comparações `getStatus().equals(...)` seguidas de `setStatus(...)`, em cada método. Há também uma decisão fora do serviço de pedidos: o `CouponValidator` monta pelos nomes, em texto, a lista das situações que contam como "pedido pago". Por isso o problema mais sério é a omissão no P7. Uma situação nova como "em separação" obrigaria a mexer em `shipOrder`, `cancelOrder` e no `CouponValidator`, e não só no tipo da situação. O cancelamento de um pedido pago é o remendo típico, um `if (PAID)` no serviço. O lado bom fica no controle negativo: o aviso tem um enum aninhado `NEW`/`READ` e um `markAsRead()` de uma linha. Não há estrutura sobrando perto do pattern.

## Item a item

**P7, a situação do pedido.** Nenhuma decisão sobre a situação mora no tipo dela. `OrderStatus` só lista as constantes (`domain/enums/OrderStatus.java:3-9`). Não tem transições, nem métodos como `canShip()` ou `canCancel()`, nem destino. `Order` também não guarda regra nenhuma: só a situação inicial no construtor (`domain/entity/Order.java:55`) e um setter livre (`domain/entity/Order.java:100`). As decisões que estão fora do tipo:
- o pagamento só vale em `AWAITING_PAYMENT` (`application/service/OrderService.java:86`), e o destino `PAID` também é escolhido no serviço (`OrderService.java:104`);
- a expiração e a falta de estoque mandam o pedido para `CANCELLED` (`OrderService.java:91` e `:98`);
- o envio só vale em `PAID` e leva a `SHIPPED` (`OrderService.java:124`, `:128`);
- a entrega só vale em `SHIPPED` e leva a `DELIVERED` (`OrderService.java:140`, `:144`);
- o cancelamento é proibido em `SHIPPED` ou `DELIVERED` (`OrderService.java:153`), num conjunto que não cita todas as situações, e leva a `CANCELLED` (`OrderService.java:162`). Ele também não barra um pedido que já está `CANCELLED`, e isso é decidido aqui, não no tipo;
- o que conta como pedido pago para o WELCOME10 é decidido pelos nomes em texto, `"PAID"`, `"SHIPPED"` e `"DELIVERED"` (`application/service/CouponValidator.java:47-49`). É outro conjunto parcial, fora do tipo e fora do serviço de pedidos.

Decisões dentro do tipo: nenhuma. A situação é só um rótulo, e as decisões estão em `if` nos serviços.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está num `if (order.getStatus().equals(OrderStatus.PAID))` dentro de `cancelOrder`, que devolve o estoque e tira o crédito (`application/service/OrderService.java:157-160`). É exatamente o remendo descrito na régua. A situação não sabe dizer se há o que devolver. Como não existe estrutura de situação nenhuma, não é um malfeito (estrutura com uma regra de fora): é a variação escrita sem o padrão. Mais um sinal da mesma coisa: o efeito comum a todo cancelamento (mudar para `CANCELLED` e gerar o aviso `ORDER_CANCELLED`) foi copiado em três lugares (`OrderService.java:91-93`, `:98-100` e `:162-165`).
Conclusão: omissão

**P7-exagero, a situação do aviso.** A situação do aviso é um enum aninhado com `NEW` e `READ` (`domain/entity/Notification.java:72-75`). O aviso nasce `NEW` no construtor, e `markAsRead()` só faz `status = READ` (`Notification.java:69`). Isso já cobre o caso "se já estava lido, continua lido" sem precisar de condição. `NotificationService.markAsRead` só busca o aviso, chama esse método e salva (`application/service/NotificationService.java:266-272`). Não há classe por situação, nem interface, nem máquina de estados. É um campo e uma troca simples.
Conclusão: contenção

**E, estrutura sem necessidade perto do State.** Perto da situação do pedido e da do aviso, não há interface, camada que só repassa, nem fábrica. As únicas interfaces do projeto são os repositórios do Spring Data (`domain/repository/*.java`), que o framework exige. O que falta aqui é estrutura, não o contrário.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar as transições para `OrderStatus`.** Cada constante diria o que permite e para onde vai, com métodos como `pay()`, `ship()`, `deliver()` e `cancel()`, que devolvem a próxima situação ou recusam a operação. `Order` trocaria o `setStatus` público por operações que perguntam à situação. Assim, a "em separação" vira uma constante nova, sem mexer em `OrderService.java:86-162`. A mesma mudança resolve o conjunto em texto do `CouponValidator.java:47-49`: ele passaria a ser uma pergunta à situação (por exemplo, `status.countsAsPurchase()`).
2. **Pôr na situação a resposta "há o que devolver?"** (por exemplo, `status.refundsOnCancel()`), no lugar do `if (PAID)` de `OrderService.java:157`. Junto com isso, juntar num único ponto o cancelamento que hoje está copiado nos três caminhos.

## Dúvidas

- Contei como decisão sobre a situação o filtro do WELCOME10 em `CouponValidator.java:47-49`, que fica fora do fluxo de transições. A régua conta como decisão "um conjunto que não cita todas as situações", e aquele filtro é isso. Mesmo sem ele, a conclusão do P7 seria a mesma.
- No remendo, a régua deixa a palavra em aberto, porque o "malfeito" pressupõe uma estrutura que aqui não existe. Usei "omissão", já que não há tipo da situação com comportamento onde a regra pudesse ter morado.

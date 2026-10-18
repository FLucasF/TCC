# padrões: State

## Visão geral

O agente não aplicou o State à situação do pedido. `OrderStatus` é um enum que só tem os cinco nomes, sem comportamento (`domain/OrderStatus.java:3-9`). Todas as decisões sobre a situação estão fora dele, em comparações com constantes. Uma parte foi para a entidade `Order`, que tem as guardas de `markAsPaid`, `ship`, `deliver`, `cancel` e a lista em `isPaid`. Outra parte foi copiada no `OrderService`, que repete as mesmas guardas antes de chamar a entidade. Há ainda uma lista de situações "pagas" no `CheckoutService`. Juntar as transições na entidade é melhor do que espalhá-las só pelos serviços. Mesmo assim, a situação "em separação" que o enunciado anuncia obrigaria a mexer em `Order`, em dois métodos do `OrderService` e no `CheckoutService`. O problema mais sério é esse: a situação é só um rótulo, e as regras estão em dois lugares. A diferença do cancelamento também ficou num `if (wasPaid)` dentro do serviço, que é o remendo. O lado bom é o aviso: um enum de dois valores e uma troca simples, sem estrutura a mais.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?** O tipo da situação, `OrderStatus` (`domain/OrderStatus.java:3-9`), só lista as constantes. Ele não tem método, nem transição, nem corpo por constante. **Nenhuma decisão está dentro dele.** Estas estão fora:
- na entidade `Order`, que não é o tipo da situação: `markAsPaid` (`domain/Order.java:122`, `status != AWAITING_PAYMENT`), `ship` (`domain/Order.java:130`, `status != PAID`), `deliver` (`domain/Order.java:138`, `status != SHIPPED`), `cancel` (`domain/Order.java:145`, `status == SHIPPED || status == DELIVERED`), `isPaid` (`domain/Order.java:152`, um conjunto com PAID, SHIPPED e DELIVERED, que não cita todas as situações), e `isExpired` (`domain/Order.java:114`), que não é chamado em lugar nenhum;
- no serviço, que repete as guardas da entidade antes de chamá-la: `service/OrderService.java:82` (`!= AWAITING_PAYMENT`, no pagamento), `:151` (`!= PAID`, no envio), `:170` (`!= SHIPPED`, na entrega), `:183` (`== SHIPPED || == DELIVERED`, no cancelamento). Por causa dessa repetição, a `IllegalStateException` da entidade nunca chega a ser lançada pelo fluxo normal, e as duas cópias precisam andar juntas;
- no cálculo do cupom: `service/CheckoutService.java:241` (`PAID, SHIPPED, DELIVERED`, para saber se é a primeira compra). É o mesmo conjunto de `Order.isPaid`, copiado.

Faço o teste com a situação "em separação" entre PAID e SHIPPED. Mudariam `Order.java:130`, `:145` e `:152`, `OrderService.java:151` e `:183`, e `CheckoutService.java:241`. A situação é um rótulo e as decisões estão em `if` fora do tipo dela, o que é a omissão da régua. Ficou claro que o enunciado tem a frase de crescimento (`prompt.md:84`).
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está no serviço. `service/OrderService.java:187` guarda `boolean wasPaid = order.isPaid()`, e `:192` faz `if (wasPaid) { increaseStock(...); customer.removeCredit(...); }`. A pergunta "há o que devolver?" usa o conjunto de situações de `Order.isPaid` (`domain/Order.java:152`), e a resposta (devolver o estoque e tirar o crédito) é decidida e feita pelo `OrderService`. Nenhuma situação diz se há o que devolver. É o `if` sobre a situação dentro do cancelamento que a régua descreve. Como não há estrutura de situação para ele ter escapado, eu chamo de omissão, e não de malfeito. O aviso de cancelamento vale para os dois casos e fica fora do `if` (`service/OrderService.java:201-203`), o que está certo, mas não é a parte que varia.
Conclusão: omissão

**P7-exagero, a situação do aviso.** `NotificationStatus` é um enum com `NEW` e `READ` (`domain/NotificationStatus.java:3-6`). O aviso nasce `NEW` (`domain/Notification.java:28`), e `markAsRead()` só faz `this.status = NotificationStatus.READ` (`domain/Notification.java:51-53`). Isso já cobre "se já estava lido, continua lido" sem nenhum `if`. O serviço só busca o aviso e chama esse método (`service/NotificationService.java:32-38`). Não há classe por situação, máquina de estados nem interface. É a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern (a situação do pedido, o cancelamento, os avisos) não há estrutura que ninguém pediu. Não há interface de situação, fábrica de transições nem camada que só repasse. O `PaymentProcessingResult` (`service/OrderService.java:232`) é só um record de dados para a idempotência, não uma estrutura. Não julguei as interfaces `*Strategy` e a classe abstrata `TransLogShipping`, porque pertencem aos pontos dos outros patterns. O que achei perto do State é código morto, não estrutura: `Order.isExpired()` (`domain/Order.java:113-119`) não é chamado (a expiração é calculada à parte em `service/OrderService.java:96-97`).
Conclusão: contenção

## O que mudaria primeiro

1. **Levar as transições para `OrderStatus`.** Cada constante diria o que pode fazer e para onde vai, com métodos como `canPay()`, `canShip()`, `canCancel()` e `hasSomethingToRefund()`, ou um conjunto de próximas situações por constante. Em seguida, `Order` passaria a perguntar ao status, e as guardas repetidas do `OrderService` (`:82`, `:151`, `:170`, `:183`) e o conjunto do `CheckoutService:241` seriam apagados. Assim a situação "em separação" mexeria só no enum, e as regras deixariam de existir em duas cópias que podem divergir.
2. **Tirar o `if (wasPaid)` do cancelamento** (`service/OrderService.java:192`). A situação diria se há estoque e crédito a devolver, e o serviço só executaria a devolução quando ela responder que sim, sem nomear PAID, SHIPPED ou DELIVERED.

## Dúvidas

- **A entidade `Order` como casa.** As guardas estão juntas na entidade, o que é melhor do que espalhá-las pelos serviços. Pela régua, porém, a casa é o tipo da situação (`OrderStatus`, ou um enum aninhado), e `Order` é a entidade inteira. Por isso não considerei essas decisões "dentro". Além disso, o serviço repete cada uma delas, de modo que nem a entidade é o único lugar. Com isso, a omissão me pareceu mais fiel do que o malfeito, que exigiria que o tipo da situação tivesse as transições.
- **O remendo com a palavra "omissão".** A régua não diz com todas as letras qual palavra usar quando não existe estrutura de situação. Usei omissão porque a variação foi escrita sem o padrão. Malfeito supõe uma estrutura da qual a regra escapou.

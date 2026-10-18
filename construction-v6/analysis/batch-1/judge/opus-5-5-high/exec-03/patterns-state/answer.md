# padrões: State

## Visão geral

A situação do pedido é um `enum OrderStatus` com um método de transições (`canTransitionTo`), mas esse método não é chamado em nenhum lugar do projeto. Na prática, a situação funciona só como rótulo. As perguntas sobre o que é permitido são comparações com constantes na entidade `Order` (`canBeShipped`, `canBeDelivered`, `canBeCancelled`), e duas decisões ficam direto no `OrderService`: a do pagamento e a do cancelamento. Uma terceira está numa consulta do repositório. O problema mais sério é esse: a casa da situação existe, mas nenhuma decisão mora nela, e uma situação "em separação" obrigaria a mexer na entidade, no serviço e na consulta. O segundo é o remendo previsto: a diferença entre cancelar um pedido pago e um não pago é um `if (status == PAID)` dentro do serviço. O ponto forte é o controle negativo. O aviso ao cliente tem um enum de duas constantes e uma troca simples, sem estrutura a mais. Ter juntado as perguntas `canBe…` na entidade, em vez de espalhá-las pelo serviço, ajuda um pouco, mas elas continuam fora do tipo da situação.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?** O tipo da situação, `src/main/java/com/loja/order/domain/OrderStatus.java:3-20`, tem as cinco constantes e um `canTransitionTo` com um `switch` sobre a situação atual (`OrderStatus.java:13-18`). Isso seria a casa certa. Mas uma busca por `canTransitionTo` em `src/` só encontra a declaração: nenhum serviço, entidade ou teste chama o método, e ele não decide nada. As decisões que valem estão fora do tipo:
- enviar: `canBeShipped()` faz `status == OrderStatus.PAID` (`src/main/java/com/loja/order/model/Order.java:206-208`), chamado em `OrderService.java:235`;
- entregar: `canBeDelivered()` faz `status == OrderStatus.SHIPPED` (`Order.java:210-212`), chamado em `OrderService.java:251`;
- cancelar: `canBeCancelled()` é um conjunto que não cita todas as situações, `status != SHIPPED && status != DELIVERED && status != CANCELLED` (`Order.java:202-204`), chamado em `OrderService.java:266`;
- pagar: `!order.getStatus().equals(OrderStatus.AWAITING_PAYMENT)` direto no serviço (`src/main/java/com/loja/order/service/OrderService.java:196`);
- o que muda em cada transição: `markAsPaid`, `markAsShipped`, `markAsDelivered` e `cancel` (`Order.java:107-124`) gravam a constante de destino sem consultar a situação de origem;
- a primeira compra: a consulta tem `o.status IN ('PAID', 'SHIPPED', 'DELIVERED')` (`src/main/java/com/loja/order/repository/OrderRepository.java:15`), outro conjunto parcial;
- o remendo do cancelamento, `OrderService.java:270` (item seguinte).

Dentro do tipo não há nenhuma decisão em uso. Uma situação `PICKING` entre `PAID` e `SHIPPED` obrigaria a mudar `Order.java:207`, rever `Order.java:203` e `OrderService.java:270`, e talvez `OrderRepository.java:15`. A situação é um rótulo e as decisões ficam em comparações fora dela. Por isso classifico como omissão, e não como malfeito (ver Dúvidas).
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** O agente pôs a diferença no serviço, como `if (order.getStatus() == OrderStatus.PAID)`, e devolve o estoque e retira o crédito ali dentro (`OrderService.java:270-277`). Depois chama `order.cancel()` e gera o aviso para todos os casos (`OrderService.java:279-281`). A situação não diz se há o que devolver. O serviço pergunta qual ela é e nomeia `PAID`. Na expiração do pagamento, o cancelamento também está escrito à mão no serviço (`OrderService.java:200-204`). Nada se devolve ali porque o pedido ainda aguardava o pagamento, mas isso vem da ordem do código, não de uma decisão da situação. É o remendo que o ponto descreve, sem a estrutura por baixo.
Conclusão: omissão

**P7-exagero, a situação do aviso.** O aviso tem um enum aninhado `Status { NEW, READ }` (`src/main/java/com/loja/order/model/Notification.java:15-18`), começa como `NEW` no construtor (`Notification.java:48`) e passa a lido por `markAsRead()`, que só faz `this.status = Status.READ` (`Notification.java:52-54`). O serviço só chama esse método (`src/main/java/com/loja/order/service/NotificationService.java:29-36`). Como a troca não depende da situação atual, um aviso já lido continua lido, sem nenhum `if`. Não há classe por situação nem interface de estado. É um campo e uma troca simples.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern, não há interface com uma implementação só, camada que só repassa ou fábrica. O `NotificationService` faz trabalho próprio (busca, recusa com `CUSTOMER_NOT_FOUND`, converte para a resposta). Há código morto, mas não estrutura: o `canTransitionTo` sem uso (`OrderStatus.java:10-19`) e o `isExpired()` (`Order.java:194-196`), que repete o `isExpiredForPayment()` (`Order.java:198-200`) e não é chamado. Isso não é exagero no sentido deste item.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar as decisões para o `OrderStatus` e usá-las.** Cada constante diria o que permite (`canPay()`, `canShip()`, `canCancel()`, `isPurchase()`) ou para onde pode ir, e a entidade passaria a perguntar ao tipo. Os `markAs…` recusariam a transição que a situação não aceita. Assim, `Order.java:202-212`, `OrderService.java:196` e o conjunto de `OrderRepository.java:15` deixam de nomear situações, e a situação "em separação" passa a ser uma constante nova no enum.
2. **Deixar a situação dizer se há o que devolver no cancelamento**, por exemplo com um `refundsOnCancel()` que é verdadeiro só para `PAID`. O `if (status == PAID)` de `OrderService.java:270` vira uma pergunta que qualquer situação nova responde sozinha.

## Dúvidas

- **Omissão ou malfeito no P7.** A régua chama de malfeito "o tipo da situação ter as transições, mas alguma decisão continuar fora dele". O `OrderStatus` tem as transições escritas, então daria para ler assim. Escolhi omissão porque o método não é chamado em lugar nenhum, e pela regra 2 o que conta é o que o código faz: todas as decisões em uso estão fora do tipo, e a situação funciona como rótulo. Pela mesma razão, o remendo também ficou como omissão: a estrutura que ele deveria respeitar não existe na prática.
- **A entidade `Order` como casa.** Os métodos `canBe…` estão na entidade, não num serviço. A régua define a casa como o tipo da situação (`OrderStatus`, ou um tipo aninhado na `Order`), e esses métodos comparam o campo com constantes. Não os contei como estando dentro da casa.

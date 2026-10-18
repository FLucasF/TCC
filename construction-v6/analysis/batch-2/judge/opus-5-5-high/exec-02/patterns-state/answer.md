# Padrões: State

## Visão geral

O agente não pôs a situação do pedido no padrão State. `OrderStatus` é um enum que só nomeia as cinco situações e não tem comportamento nenhum. As decisões sobre a situação ficaram juntas na entidade `Order`, em comparações `status == ...`: os métodos `canBePaid/canBeShipped/canBeDelivered/canBeCancelled` e os métodos de transição, que gravam a situação de destino. Isso é melhor que espalhar `if` pelos serviços, mas a casa da situação continua sendo um rótulo. Além disso, há decisões fora até da `Order`: o `wasPaid` do cancelamento no serviço (é o remendo) e o conjunto `('PAID','SHIPPED','DELIVERED')` no SQL do repositório. Uma situação "em separação" obrigaria a mexer na `Order`, no `OrderService` e no repositório. O aviso ao cliente foi feito do jeito simples, com um enum `NEW/READ` e uma troca, e isso é contenção. Fora dos pontos, as quatro interfaces de repositório têm uma implementação cada, e o próprio serviço contorna a de pedidos com casts para a classe concreta.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?** `src/main/java/com/loja/domain/entities/OrderStatus.java:3-9` só lista as constantes, sem método nem corpo próprio. Nenhuma decisão mora no tipo da situação. As decisões estão fora dele:
- **Na entidade `Order`**, que não é o tipo da situação:
  - quais operações são permitidas: `canBePaid` faz `status == AWAITING_PAYMENT` (`Order.java:125-127`), `canBeShipped` faz `status == PAID` (`Order.java:129-131`), `canBeDelivered` faz `status == SHIPPED` (`Order.java:133-135`), e `canBeCancelled` usa um conjunto que não cita todas as situações, `AWAITING_PAYMENT || PAID` (`Order.java:137-139`);
  - o destino de cada transição: `confirmPayment` vai para `PAID` (`Order.java:109`), `ship` para `SHIPPED` (`:114`), `deliver` para `DELIVERED` (`:118`) e `cancel` para `CANCELLED` (`:122`).
- **No serviço**: `boolean wasPaid = order.getStatus() == OrderStatus.PAID` (`services/OrderService.java:149`).
- **No repositório**: a primeira compra é decidida com `status IN ('PAID', 'SHIPPED', 'DELIVERED')` (`repositories/impl/OrderRepositoryImpl.java:176`). É um conjunto que não cita todas as situações.

Não contam como decisão: a situação inicial passada na criação, `OrderStatus.AWAITING_PAYMENT` (`OrderService.java:51`), e a tradução do texto para a constante, `OrderStatus.valueOf(...)` (`OrderRepositoryImpl.java:127`).

Os métodos de transição da `Order` também não conferem a situação de partida. `ship()` grava `SHIPPED` em qualquer situação (`Order.java:112-115`), e a regra só vale porque o serviço pergunta `canBeShipped()` antes (`OrderService.java:111`).

Com uma situação "em separação", seria preciso mudar `canBeShipped` e `canBeCancelled` na `Order`, o `wasPaid` do `OrderService` (um pedido em separação já está pago e precisa devolver o estoque) e o SQL do `isFirstOrder`. A situação continua sendo um rótulo, com as decisões em comparações no lado de fora. Há um atenuante: a maior parte está concentrada na `Order`, e não em `if` espalhados pelos serviços. Mas o tipo da situação não tem as transições, então não é malfeito.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está no serviço, exatamente como o remendo descrito na régua. `cancelOrder` grava `wasPaid = order.getStatus() == OrderStatus.PAID` (`OrderService.java:149`). Depois, `if (wasPaid) { reversePaymentEffects(order); }` (`OrderService.java:154-156`) devolve o estoque e tira o crédito (`OrderService.java:171-184`). A situação não diz se há o que devolver; quem decide é o `if` do serviço, que nomeia o `PAID`. Uma situação nova que também já tenha baixado o estoque, como a "em separação", cairia no caminho de não devolver nada se ninguém mexer nesse `if`.
Conclusão: omissão

**P7-exagero, a situação do aviso.** `NotificationStatus` é um enum simples, `NEW, READ` (`domain/entities/NotificationStatus.java:3-6`). Há um campo em `Notification.java:7` e uma troca só, `markAsRead()`, que grava `READ` (`Notification.java:32-34`). Como ela é idempotente, "se já estava lido, continua lido" sai de graça. O controller só chama esse método e grava (`api/controllers/ApiController.java:307-308`). Não há classe por situação, máquina de estados nem interface.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Na parte do ciclo do pedido há quatro interfaces de repositório (`OrderRepository`, `CustomerRepository`, `NotificationRepository` e `ProductRepository`), com uma implementação cada em `repositories/impl/`. Nada as substitui: o único teste do projeto é `src/test/java/com/loja/MoneyTest.java`, e não há fakes nem mocks. Pior, o `OrderService` recebe a interface e a contorna. Ele faz `((OrderRepositoryImpl) orderRepository).savePaymentTransaction(...)` em `OrderService.java:76`, `:88` e `:95`, e importa o pacote `impl` (`OrderService.java:11`), porque `savePaymentTransaction` não está na interface (`repositories/OrderRepository.java` só declara `hasPaymentTransaction`, na linha 14). A interface não serve nem de contrato. É uma camada que ninguém pediu. O peso é pequeno, porque repositório com interface é um costume comum, mas o critério da régua (uma implementação só que nada substitui) se aplica. Os observers do pagamento ficam fora deste item, porque o enunciado pede essa variação ("SMS", "cashback").
Conclusão: exagero

## O que mudaria primeiro

1. **Levar as decisões para `OrderStatus`.** Cada constante diria para onde pode ir (`canTransitionTo` ou `allowsShipment`/`allowsCancellation`) e responderia `holdsPaymentEffects()` (se já baixou o estoque e deu o crédito) e `countsAsPurchase()`. Com isso, `canBe*` em `Order.java:125-139`, o `wasPaid` em `OrderService.java:149` e o `IN (...)` em `OrderRepositoryImpl.java:176` passam a perguntar à situação, sem nomeá-la. O motivo é o próprio enunciado: a situação "em separação" passaria a mexer só no enum.
2. **Fazer as transições de `Order` recusarem a situação de partida errada**, perguntando ao tipo da situação, em vez de depender de o serviço chamar `canBe*` antes. Assim a regra "só envia pago" não fica fora do tipo se alguém chamar `ship()` direto.

## Dúvidas

- Se a `Order` conta como "casa" das decisões. A régua diz que a casa é o tipo da situação (o enum, ou uma classe por situação, inclusive aninhado), e a `Order` é a entidade que guarda a situação, não o tipo dela. Por isso li as comparações em `Order.java` como decisões fora da casa, e o P7 como omissão, não como malfeito. Registrei como atenuante que elas estão concentradas num lugar só.
- Se o `IN ('PAID','SHIPPED','DELIVERED')` em SQL conta como decisão sobre a situação. Ele é um conjunto que não cita todas as situações e decide uma regra do negócio (o que conta como primeira compra), então contei.
- Se as interfaces de repositório entram no E deste pattern. Como estão no caminho do ciclo do pedido e o serviço as contorna com cast, registrei aqui. Outra correção de pattern pode registrá-las também.

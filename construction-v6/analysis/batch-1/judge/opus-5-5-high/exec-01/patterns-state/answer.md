# State

## Visão geral

O agente pôs as transições permitidas dentro do tipo da situação. O enum `OrderStatus` tem um método `canMoveTo` que diz, para cada situação, para onde ela pode ir. Os serviços não perguntam "posso enviar?" por conta própria: todos passam por `requireMove`, que só repassa a pergunta ao enum. Essa é a parte que acerta. O problema mais sério é o cancelamento. A diferença entre cancelar um pedido pago e um não pago virou um `if (status == PAID)` no serviço, e esse resultado ainda segue como um `boolean wasPaid` até os listeners. Também ficou fora do tipo a regra de quais situações "contam como compra", escrita como uma lista fixa em SQL no repositório. Com a situação "em separação" que o enunciado anuncia, esses dois pontos teriam de mudar fora do `OrderStatus`, e o cancelamento de um pedido em separação deixaria de devolver o estoque e o crédito. O aviso ao cliente ficou do tamanho certo: um campo de texto e um `UPDATE`.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?**

Dentro do tipo:
- As transições permitidas estão em `OrderStatus.canMoveTo` (`src/main/java/br/com/loja/order/OrderStatus.java:6-13`). Cada situação lista para onde pode ir: `AWAITING_PAYMENT → PAID | CANCELLED`, `PAID → SHIPPED | CANCELLED`, `SHIPPED → DELIVERED`, e as finais não vão a lugar nenhum.
- As regras do enunciado ("só envia pago", "só entrega enviado", "cancela enquanto não foi enviado") saem daí. Todas as operações chamam `requireMove(order, destino)` (`order/OrderService.java:182-186`), e esse método só pergunta `order.status().canMoveTo(target)`. Os usos estão em `OrderService.java:115`, `:146`, `:157` e `:166`.
- O serviço não tem nenhum `switch` sobre a situação, e o controller não decide nada (`order/OrderController.java:49-68` só repassa).

Fora do tipo:
- **O cancelamento**: `boolean wasPaid = order.status() == OrderStatus.PAID` em `OrderService.java:167`. Isso decide, no serviço, se o estoque volta (`:168-170`). Detalhes no item seguinte.
- **O que conta como compra (para o WELCOME10)**: `status IN ('PAID', 'SHIPPED', 'DELIVERED')` em `order/OrderRepository.java:105`, usado por `checkout/CheckoutService.java:77`. É um conjunto que não cita todas as situações. A pergunta "esta situação conta como compra?" deveria estar no enum, e ficou numa string SQL do repositório. Uma situação "em separação" teria de entrar nessa lista na mão, senão um cliente com pedido em separação ainda ganharia o WELCOME10.
- **De menor peso**: cada operação do serviço escreve sua situação de destino (`OrderStatus.PAID` em `:123`, `CANCELLED` em `:130` e `:171`, `DELIVERED` em `:158`). O repositório também fixa o destino do envio (`OrderRepository.java:98`, `OrderStatus.SHIPPED.name()`). Não são comparações: cada operação diz para onde quer ir, e quem decide se pode é o enum. Por isso não conto esses pontos como decisões fora da casa. Ainda assim, a operação "enviar" fica presa a `SHIPPED`, e nenhum serviço pergunta à situação qual é a próxima.

O tipo tem as transições, mas pelo menos duas decisões sobre a situação continuam fora dele: no serviço e no repositório.
Conclusão: malfeito

**P7-remendo, cancelar um pago e cancelar um não pago.** O agente fez o remendo que a régua descreve:
- Em `OrderService.cancel` (`order/OrderService.java:167-170`), `wasPaid` é calculado comparando com `OrderStatus.PAID`, e `products.release(...)` só roda dentro de `if (wasPaid)`.
- O mesmo resultado segue como parâmetro: `listeners.forEach(l -> l.onCancelled(cancelled, wasPaid))` (`:173`). Ele aparece também na interface `OrderEventListener.onCancelled(Order, boolean wasPaid)` (`order/OrderEventListener.java:15`).
- Um segundo `if (wasPaid)` em `customer/LoyaltyCreditListener.java:42` decide se o crédito sai do saldo.
- No cancelamento por pagamento recusado, o serviço passa `false` fixo (`OrderService.java:132`).

O enum não diz se há o que devolver. A diferença mora no serviço e se espalha para a assinatura do listener. Com uma situação "em separação" (já paga), `status == PAID` daria falso, e cancelar um pedido em separação não devolveria o estoque nem tiraria o crédito. O certo seria algo como `status.hasPaymentToRefund()` no próprio `OrderStatus`.
Conclusão: malfeito

**P7-exagero, a situação do aviso.**
- O aviso é um record com `String status` (`notification/Notification.java:3`). Ele nasce com `'NEW'` no `INSERT` (`notification/NotificationRepository.java:143`).
- Marcar como lido é um único `UPDATE notifications SET status = 'READ'` (`NotificationRepository.java:153`). Esse `UPDATE` já deixa como lido um aviso que já estava lido, sem nenhum `if`.
- No banco, é uma coluna `VARCHAR(10)` (`src/main/resources/db/migration/V1__schema.sql:52`).

Não há enum de situação do aviso, classe por situação nem máquina de estados. A resposta é do tamanho do problema. O lado fraco, que não é exagero: o tipo da situação não aparece em lugar nenhum (é só texto, sem `CHECK` no banco). Isso é assunto de outras dimensões.
Conclusão: contenção

**E, estrutura sem necessidade perto do State.** Perto da situação do pedido não há estrutura que nenhum ponto peça:
- não há interface `OrderState` com uma implementação só;
- não há fábrica de situações nem camada que só repasse (`OrderRepository` e `NotificationRepository` são classes concretas que fazem o SQL);
- `requireMove` (`OrderService.java:182`) é um auxiliar privado de três linhas, não uma camada.

A interface `OrderEventListener` (`order/OrderEventListener.java:7`), com dois beans, responde ao pedido do enunciado de SMS e cashback no futuro. Não é estrutura sem pedido, e quem a julga é outro pattern.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar o "há o que devolver?" para o `OrderStatus`**: um método no enum, como `refundsOnCancel()` ou `isPaid()`, que hoje seria verdadeiro só em `PAID` e amanhã também em "em separação". O serviço usaria esse método no lugar de `status == OrderStatus.PAID` (`OrderService.java:167`). É a mudança que mais importa, porque hoje uma situação nova paga quebraria a devolução de estoque e de crédito sem nenhum erro de compilação.
2. **Fazer o mesmo com "conta como compra"**: um método no enum, como `countsAsPurchase()`, de onde o repositório tiraria a lista de situações (`OrderRepository.java:105`), em vez de fixar `'PAID', 'SHIPPED', 'DELIVERED'` no SQL. Assim a situação nova mexeria só no `OrderStatus`, como o enunciado pede.

## Dúvidas

- Considerei que a situação de destino escrita em cada operação (`requireMove(order, OrderStatus.SHIPPED)`, `updateStatus(id, DELIVERED)`) não é uma "decisão sobre a situação" no sentido da régua. Ela não compara a situação atual com nada: cada operação diz para onde quer ir, e quem permite ou não é o enum. Se a régua quiser que a própria situação diga o próximo passo, como `next()`, essas linhas também contariam como fora da casa. A conclusão do P7 continuaria `malfeito`.
- A lista `IN ('PAID', 'SHIPPED', 'DELIVERED')` (`OrderRepository.java:105`) existe para a regra do WELCOME10, mas eu a contei como decisão sobre a situação do pedido: ela responde "esta situação conta como compra?" e não cita todas as situações. Não é uma lista de válidos nem uma tradução de texto, por isso não entra nas exceções da régua.

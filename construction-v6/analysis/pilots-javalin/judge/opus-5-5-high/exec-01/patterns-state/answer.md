# padrões: State

## Visão geral

O agente montou a situação do pedido como um enum que só dá nome às situações (`domain/OrderStatus.java:3-5`). As transições ficam numa tabela à parte, `OrderLifecycle` (`domain/OrderLifecycle.java:13-20`), organizada por operação e situação de origem. Os quatro métodos de transição do serviço consultam essa tabela, e não há `switch` sobre as situações em lugar nenhum: esse é o ponto forte.

O problema mais sério é o remendo. A diferença entre cancelar um pedido pago e um não pago está num `if (order.status() == OrderStatus.PAID)` dentro de `OrderService.cancel` (`app/OrderService.java:132`). Há outras decisões fora da tabela:
- o conjunto das situações que contam como compra feita, escrito em SQL (`db/Dao.java:130`);
- o destino `CANCELLED` na recusa do pagamento, escrito direto no serviço (`app/OrderService.java:148`);
- o destino `PAID`, também escrito direto, sem usar o valor que a tabela devolve (`app/OrderService.java:77` e `:92`).

Com a situação "em separação" que o enunciado anuncia, a tabela mudaria, mas o serviço e o DAO também teriam de mudar. O aviso, por sua vez, foi resolvido do jeito simples, como se pede.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?**

O tipo da situação, `OrderStatus` (`domain/OrderStatus.java:3-5`), é só um rótulo, sem comportamento. As transições moram em `OrderLifecycle`, uma classe do domínio dedicada a isso. Ela tem um mapa `Operation → (situação de origem → situação de destino)` (`domain/OrderLifecycle.java:16-20`): pagar sai de `AWAITING_PAYMENT`, enviar sai de `PAID`, entregar sai de `SHIPPED`, cancelar sai de `AWAITING_PAYMENT` ou de `PAID`. Também tem `require`, que recusa com `OPERATION_NOT_ALLOWED` (`:30-32`).

Decisões **dentro** da estrutura:
- se a operação é permitida e para onde vai, no envio (`app/OrderService.java:109`), na entrega (`:122`) e no cancelamento (`:131`, que usa o `next` devolvido em `:143`);
- se o pagamento é permitido (`:77`).

Decisões **fora** dela:
- `app/OrderService.java:132`: `if (order.status() == OrderStatus.PAID)` decide se há estoque e crédito para devolver (é o remendo, detalhado no item seguinte);
- `app/OrderService.java:77` e `:92-93`: `require` devolve o destino do pagamento, mas o retorno é descartado e o serviço escreve `OrderStatus.PAID` à mão;
- `app/OrderService.java:148`: a recusa por prazo vencido ou falta de estoque leva o pedido a `OrderStatus.CANCELLED` direto. Essa transição (aguardando → cancelado na hora do pagamento) não passa pela tabela;
- `db/Dao.java:130`: `status IN ('PAID', 'SHIPPED', 'DELIVERED')` é um conjunto que não cita todas as situações e decide quais delas contam como compra feita para o WELCOME10 (chamado em `app/CheckoutService.java:79`). Uma situação "em separação" ficaria fora desse conjunto sem ninguém perceber, e o WELCOME10 seria aceito para quem já pagou.

Não conto como decisão a situação inicial `AWAITING_PAYMENT` na criação do pedido (`app/OrderService.java:52`) nem o `OrderStatus.valueOf` que traduz o texto lido do banco (`db/Dao.java:175`).

A estrutura existe e cobre as permissões, mas a situação não sabe o que fazer ao sair (devolver ou não), nem quais situações contam como "pago", e dois destinos estão escritos no serviço. Fica uma dúvida: a casa é uma tabela separada do enum, e não o próprio enum (ver Dúvidas). Mesmo aceitando `OrderLifecycle` como casa, ficam decisões fora dela.

Conclusão: malfeito

**P7-remendo, cancelar um pago e cancelar um não pago.**

A diferença está no serviço, num `if (order.status() == OrderStatus.PAID)` dentro de `cancel` (`app/OrderService.java:132-142`). Dentro desse `if`, o serviço:
- devolve o estoque inline (`:133-138`);
- percorre os `PaidOrderEffect` chamando `onPaidOrderCancelled` (`:139-141`), que tira o crédito em `app/CreditEffect.java:13-15`.

O aviso de cancelamento sai em `markCancelled` para todos os casos (`:153-157`), o que está certo. Nem `OrderStatus` nem `OrderLifecycle` dizem se a situação tem o que devolver. Se o pedido em separação puder ser cancelado, o `if` teria de virar `PAID || PICKING`, senão o estoque e o crédito não voltam.

Conclusão: malfeito

**P7-exagero, a situação do aviso.**

O aviso guarda a situação como texto, sem tipo próprio: o campo `String status` do record (`domain/Notification.java:3`), gravado como `"NEW"` ao criar (`app/Notifier.java:13`). Marcar como lido é um `UPDATE notifications SET status = 'READ'` sem condição (`db/Dao.java:262`). Por isso, um aviso já lido continua lido sem nenhum ramo especial. Não há enum, interface nem classe por situação do aviso.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do State).**

O enum `Operation` aninhado em `OrderLifecycle` (`domain/OrderLifecycle.java:11`) faz parte da tabela de transições e é usado pelos quatro métodos, então não é sobra.

A interface `PaidOrderEffect` (`app/PaidOrderEffect.java:10-14`) tem duas implementações (`CreditEffect`, `PaymentApprovedNoticeEffect`, ligadas em `Main.java:29-30`) e responde a um pedido do enunciado (SMS e cashback no futuro), então não é estrutura sem necessidade. Ela tem um defeito: `onPaidOrderCancelled` vazio em `app/PaymentApprovedNoticeEffect.java:15-17`. Isso é assunto de outro pattern.

`Notifier` (`app/Notifier.java:9-12`) é uma classe concreta pequena, sem interface. Não achei, perto do State, interface com uma implementação só, camada que só repassa ou fábrica sem uso.

Conclusão: contenção

## O que mudaria primeiro

1. **Levar para a situação a pergunta "há o que devolver ao sair daqui?".** Por exemplo, um `releasesPaidEffects()` no enum, ou um destino de cancelamento que carregue isso na tabela. Com isso, `cancel` deixa de testar `== PAID` (`app/OrderService.java:132`). É a mudança que mais protege a situação "em separação": sem ela, cancelar um pedido em separação perderia o estoque e o crédito sem erro nenhum.
2. **Tirar do serviço e do SQL as demais decisões sobre a situação.**
   - Usar o destino que `require` devolve no pagamento (`app/OrderService.java:77/92`).
   - Pôr a expiração e a falta de estoque como transição da tabela (`:148`).
   - Fazer o conjunto "conta como compra" vir do tipo da situação (por exemplo, um `countsAsPurchase()` em cada constante), em vez do `IN (...)` fixo em `db/Dao.java:130`.

## Dúvidas

- **A casa da situação ser uma classe separada do enum.** A régua dá como casa "o tipo da situação" e cita como acerto o enum com as transições ou uma classe por situação. Aqui o enum é só rótulo e as transições estão numa tabela à parte, `OrderLifecycle`, no mesmo pacote do domínio. Tratei `OrderLifecycle` como a casa das transições, porque uma situação nova mexe nela e não nos serviços, pelo menos no que se refere às permissões. Se a leitura for mais estrita (só o próprio tipo conta), a avaliação continuaria entre malfeito e omissão. Escolhi malfeito porque as permissões não estão em `if`/`switch` nos serviços.
- **O `IN ('PAID','SHIPPED','DELIVERED')` em SQL** (`db/Dao.java:130`) parafraseia uma regra do cupom, mas o conjunto é sobre a situação do pedido e não cita todas. Por isso contei como decisão fora da casa, como a régua define.

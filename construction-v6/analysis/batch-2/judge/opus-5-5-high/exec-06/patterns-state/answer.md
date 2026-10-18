# Padrões: State

## Visão geral

O agente não aplicou o State à situação do pedido. `OrderStatus` é um enum só com os nomes (`domain/OrderStatus.java:3-9`). Todas as regras sobre a situação ficaram fora dele:
- cada método do `OrderService` decide com `if (order.getStatus() != ...)` se a operação é permitida e escolhe a próxima situação com `setStatus(OrderStatus.X)`;
- o cancelamento separa o pedido pago do não pago com um `if (status == PAID)` no próprio serviço, que é exatamente o remendo previsto;
- até a regra da "primeira compra" depende de uma lista fixa de situações escrita no SQL do repositório.

Para criar a situação "em separação" que o enunciado anuncia, seria preciso mexer em vários pontos do serviço e do repositório, e não só no tipo da situação. O ponto forte é o controle negativo: o aviso tem um enum `NEW`/`READ` e uma troca simples, sem estrutura nenhuma a mais. Também não há estrutura desnecessária perto deste pattern.

## Item a item

**P7, a situação do pedido.** Nenhuma decisão sobre a situação está dentro do tipo, porque `OrderStatus` não tem métodos nem transições (`domain/OrderStatus.java:3-9`). As decisões ficaram fora, todas no serviço ou no repositório:
- **Quais operações são permitidas:**
  - pagar só em `AWAITING_PAYMENT` (`service/OrderService.java:103`);
  - enviar só em `PAID` (`service/OrderService.java:177`);
  - entregar só em `SHIPPED` (`service/OrderService.java:215`);
  - cancelar é proibido em `SHIPPED || DELIVERED` (`service/OrderService.java:249`). É uma lista que não cita todas as situações, então uma situação nova seria cancelável sem ninguém decidir isso. Também não proíbe cancelar um pedido que já está `CANCELLED`.
- **Para onde cada transição vai:** cada método escolhe o destino sozinho, com `setStatus` e `updateStatus`:
  - `CANCELLED` em `:110-111`, `:126-127` e `:266-267`;
  - `PAID` em `:139-141`;
  - `SHIPPED` em `:182-184`;
  - `DELIVERED` em `:220-221`.
- **O que conta como compra feita:** `status IN ('PAID', 'SHIPPED', 'DELIVERED')` (`repository/OrderRepository.java:121`). É uma lista parcial das situações; uma situação "em separação" teria de entrar nela à mão.

Não contei a tradução do texto do banco para a constante (`repository/OrderRepository.java:170`), nem a situação inicial ao criar o pedido (`service/OrderService.java:54`). A situação é só um rótulo e as decisões estão em `if` nos serviços.
Conclusão: omissão

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença está no serviço: `if (order.getStatus() == OrderStatus.PAID)` (`service/OrderService.java:254-264`) devolve o estoque (`:259`) e tira o crédito (`:262`). O aviso de cancelamento vale para todos os casos e vem depois (`:269`). A situação não diz se há algo a devolver; quem decide é o serviço. É o remendo exatamente como a régua descreve. Isso vem junto com a omissão do P7: como não existe uma casa para a situação, essa regra também não teria onde morar.
Conclusão: omissão

**P7-exagero, a situação do aviso.** O aviso tem um campo `status` do tipo `NotificationStatus`, um enum com duas constantes (`domain/NotificationStatus.java:3-6`; `domain/Notification.java:8`). Ele começa como `NEW` (`repository/NotificationRepository.java:27`). Marcar como lido grava `READ` sem condição nenhuma (`repository/NotificationRepository.java:66-74`; `controller/CustomerController.java:52-53`), e por isso "se já estava lido, continua lido" sai naturalmente. Não há classe por situação, máquina de estados nem interface. Essa lógica mora no controller e no repositório, e não no `Notification`, mas isso é assunto da arquitetura, não deste item. É a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** O projeto inteiro tem uma interface só, `CouponStrategy` (`service/coupon/CouponStrategy.java:8`), que pertence ao ponto do cupom (P2) e não entra aqui. Também não há classe abstrata, fábrica, builder próprio nem camada que só repassa ligada à situação do pedido ou do aviso. Os `newBuilder()` de `service/shipping/TransLogClient.java:23,37` vêm da biblioteca `java.net.http`. Perto deste pattern, não há estrutura sobrando.
Conclusão: contenção

## O que mudaria primeiro

1. **Passar as transições para dentro de `OrderStatus`.** Cada constante diria o que pode fazer e para onde vai, por exemplo `pay()`, `ship()`, `deliver()` e `cancel()`, devolvendo a próxima situação ou recusando com `OPERATION_NOT_ALLOWED`. Isso troca os quatro `if` de `OrderService.java:103/177/215/249` e os destinos escritos à mão. Assim, "em separação" seria uma constante nova, e não uma busca pelo serviço inteiro. Também fecharia a falha da lista parcial em `:249`.
2. **A situação dizer se há o que devolver no cancelamento**, por exemplo com `hasPaymentToReverse()` ou `countsAsPurchase()`. O mesmo método serviria para substituir o `if (status == PAID)` de `OrderService.java:254` e a lista `IN ('PAID','SHIPPED','DELIVERED')` de `OrderRepository.java:121`.

## Dúvidas

- Contei a lista `status IN (...)` do SQL (`repository/OrderRepository.java:121`) como uma decisão sobre a situação fora do tipo. É um conjunto que não cita todas as situações e mudaria com uma situação nova. Como fica no banco, alguém poderia ler como consulta, e não como decisão do domínio. Isso não muda a conclusão, que já seria omissão só pelo serviço.
- Não contei como decisão a situação inicial `AWAITING_PAYMENT` posta na criação (`service/OrderService.java:54`). É um ponto de partida, não a escolha entre situações.

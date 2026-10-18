# Padrões: State

## Visão geral

O agente resolveu bem a situação do pedido. `OrderStatus` é um enum em que cada constante diz, por flags, o que permite: aceitar pagamento, ser enviada, ser entregue, ser cancelada e se o pagamento já foi recebido. Os serviços perguntam ao enum (`canShip()`, `canCancel()`…) e não comparam a situação com uma constante: não há nenhum `status == X` nem `switch` sobre a situação no código de produção. O cancelamento também pergunta à situação se há o que devolver (`paymentReceived()`), sem citar `PAID`. O aviso ficou simples, com o status num texto e duas instruções SQL. Os pontos fracos são pequenos:
- a situação de destino de cada operação está escrita no serviço, mas cada operação sempre leva à mesma situação, então ali não há decisão;
- o construtor do enum recebe cinco booleanos por posição, o que é difícil de ler, e cada operação nova acrescenta mais uma coluna.

## Item a item

**P7, as decisões sobre a situação moram na própria situação?** Dentro do tipo: cada constante declara as operações que permite (`order/OrderStatus.java:8-12`), com as perguntas `acceptsPayment()`, `canShip()`, `canDeliver()`, `canCancel()` e `paymentReceived()` (`order/OrderStatus.java:29-48`). Até o conjunto "pago, enviado ou entregue", usado no WELCOME10, sai do enum por um filtro, e não de uma lista escrita à mão (`order/OrderStatus.java:50-52`, usado em `order/OrderRepository.java:65`).

No serviço, as decisões são todas perguntas ao enum, sem nomear nenhuma situação:
- `!order.status().acceptsPayment()` (`order/OrderService.java:77`);
- `!order.status().canShip()` (`order/OrderService.java:100`);
- `!order.status().canDeliver()` (`order/OrderService.java:111`);
- `!order.status().canCancel()` (`order/OrderService.java:120`).

Procurei comparações com constantes, `switch` e textos com os nomes das situações em `web/`, `checkout/`, `coupon/`, `customer/` e `order/`, e não achei nenhuma fora do enum. O que fica fora é a situação de destino: `orders.update(id, OrderStatus.SHIPPED, ...)` (`order/OrderService.java:103`), e o mesmo em `:88`, `:114` e `:134`. Pela definição da régua isso não é decisão: não compara, e cada operação sempre leva ao mesmo destino, qualquer que seja a origem.

Com a situação "em separação", bastaria acrescentar uma constante e trocar o `canShip` de `PAID` para falso. As operações que já existem não mudariam. A operação nova de separar teria de ser escrita de qualquer jeito, e ela acrescentaria mais um booleano por posição em todas as constantes. Isso é um custo de leitura, não de desenho.
Conclusão: acerto

**P7-remendo, cancelar um pago e cancelar um não pago.** A diferença é decidida pela situação: `boolean wasPaid = order.status().paymentReceived()` (`order/OrderService.java:133`). Esse valor controla a volta do estoque (`if (wasPaid) stock.release(...)`, `order/OrderService.java:136-138`) e é repassado aos ouvintes (`order/OrderService.java:139`). O ouvinte do clube só tira o crédito se `wasPaid` (`customer/ClubCreditListener.java:24-25`). O ouvinte dos avisos registra o cancelamento sempre (`notification/CustomerNotificationListener.java:29-31`), como o enunciado pede.

Ainda há um `if` no serviço, mas ele pergunta à situação "já recebeu o pagamento?" e não cita `PAID`. Uma situação nova paga, como a "em separação", responderia sozinha pela flag. As ações de devolver ficaram fora do tipo (no serviço e no ouvinte), mas a régua pede que a situação *diga se há o que devolver*, e é o que acontece. O mesmo caminho serve ao cancelamento por pagamento expirado ou sem estoque (`refuse`, `order/OrderService.java:126-130`), em que a situação é `AWAITING_PAYMENT` e não há nada a devolver.
Conclusão: acerto

**P7-exagero, a situação do aviso.** O status do aviso é um `String` no record (`notification/Notification.java:3`). Ele começa como `'NEW'` no próprio INSERT (`notification/NotificationRepository.java:22`) e vira `'READ'` num UPDATE sem condição (`notification/NotificationRepository.java:42`), o que já cobre o "se já estava lido, continua lido". O controller só confere se o aviso existe, marca e devolve (`web/CustomerController.java:41-44`). Não há enum de situação do aviso, nem classe por situação, nem máquina de estados. É a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto do State).** Perto da situação do pedido não há estrutura sobrando: `Order` é um record simples (`order/Order.java:10-19`), e o serviço não tem camadas que só repassam. A interface `OrderEventListener`, com duas implementações (`order/OrderEventListener.java:6-17`), atende a um pedido do enunciado ("no futuro, também mandar SMS e dar pontos num parceiro de cashback"), então não conta como exagero aqui. A interface `common/Coded.java` serve aos patterns de estratégia, não a este.
Conclusão: contenção

## O que mudaria primeiro

1. Trocaria os cinco booleanos por posição do construtor de `OrderStatus` (`order/OrderStatus.java:8-12`) por algo legível. Pode ser um método por constante, ou cada constante declarando para quais situações pode ir (`PAID` vai para `SHIPPED` e `CANCELLED`). A "em separação" é justamente o caso que acrescentaria mais uma coluna, e nela fica fácil trocar um `true` de lugar sem perceber.
2. Opcionalmente, levaria o destino para o enum: `status.ship()` devolveria a próxima situação ou recusaria. Assim, a pergunta de permissão e o destino ficam juntos no mesmo lugar, e o serviço só grava o resultado.

## Dúvidas

- A régua dá como acerto "cada constante dizendo para onde pode ir, ou um método `canShip()` em cada uma". Aqui as permissões estão no enum, mas os destinos estão no serviço. Li isso como acerto e não como malfeito, porque escrever o destino não compara nem escolhe nada: cada operação tem um destino fixo.
- No remendo, o `if (wasPaid)` continua no serviço e no ouvinte do clube. Considerei acerto porque a condição vem da situação (`paymentReceived()`) e não nomeia `PAID`, que é o critério da régua: "a situação diz se há o que devolver".

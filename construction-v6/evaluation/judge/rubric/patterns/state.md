# As instruções do pattern: State (secundário)

Este pattern cuida de um ponto que pede o padrão (P7, a situação do pedido), de um remendo (o cancelamento) e de um
controle negativo (a situação do aviso). É um pattern secundário: o enunciado pede menos dele que dos outros.

## O ponto que pede o padrão: a situação do pedido (P7)

O pedido passa por cinco situações, com transições permitidas e proibidas ("só dá para enviar um pedido pago, e só dá
para entregar um pedido enviado"; "o cliente pode cancelar enquanto o pedido não foi enviado"), e o enunciado diz que
vai crescer: "estamos pensando em criar uma situação 'em separação', entre o pago e o enviado". A resposta certa é
cada situação saber o que pode fazer e para onde vai, de modo que uma situação nova mexa no tipo da situação, e não
nos serviços.

**`P7`: as decisões sobre a situação moram na própria situação?** Uma decisão é uma comparação com uma situação
(`status == PAID`), um rótulo de `switch` ou um conjunto que não cita todas as situações. A casa é o tipo da
situação (`OrderStatus`, `OrderState`, uma classe por situação), inclusive aninhado (`enum Status` dentro de
`Order`). Não contam a lista de válidos e o rótulo que só traduz o texto para a constante.

É **acerto** as decisões de qual operação é permitida e do que muda em cada transição estarem no tipo da situação:
um enum com as transições (cada constante dizendo para onde pode ir, ou um método `canShip()` em cada uma), ou uma
classe por situação. É **omissão** a situação ser só um rótulo, com as decisões em `if`/`switch` nos serviços. É
**malfeito** o tipo da situação ter as transições, mas alguma decisão sobre a situação continuar fora dele (num
serviço, no controller).

Cite as decisões que estão dentro e as que estão fora.

## O remendo: cancelar um pago e cancelar um não pago (P7-remendo)

"Quando um pedido pago é cancelado, o estoque volta, o crédito do clube sai do saldo do cliente e o cliente é avisado.
Quando um pedido ainda não pago é cancelado, não há nada para devolver." O comportamento do cancelamento muda conforme
a situação. O certo é essa diferença morar na situação (a situação diz se há o que devolver). O remendo é um
`if (status == PAID)` dentro do cancelamento, no serviço. Diga onde o agente a pôs.

## O controle negativo: a situação do aviso (P7-exagero)

O aviso ao cliente só vai de `NEW` para `READ` ("o atendimento marca como lido depois de conferir"; "se já estava
lido, continua lido"). É **contenção** um campo e uma troca simples. É **exagero** uma classe por situação do aviso,
uma máquina de estados, ou uma interface de situação para duas situações e uma transição.

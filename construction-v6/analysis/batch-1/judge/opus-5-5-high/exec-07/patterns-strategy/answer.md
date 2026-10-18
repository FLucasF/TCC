# Padrões: Strategy

## Visão geral

O agente não usou Strategy em nenhum dos quatro pontos que pedem o padrão. Entregas, cupons, formas de pagamento e níveis do clube são enums que só servem de rótulo, sem corpo nem dados (`domain/enums/*.java`). Toda a variação está em `switch` e `if` que nomeiam os casos dentro de `OrderSummaryCalculator`, e se repete em `ValidationService`, `CouponValidator` e `OrderService`. Por isso, o mesmo caso aparece em dois ou três lugares. A regra do motoboy até 5 kg, por exemplo, está no validador e no cálculo. O MINUS50 a partir de R$ 300 e o FREESHIPPING fora da retirada estão no validador de cupons e no cálculo. As quatro regras cruzadas viraram remendos no código geral: o `if (GOLD && != COURIER)` no cálculo do frete, o Pix sem cupom dentro do `switch` dos pagamentos e a consulta ao histórico do WELCOME10 num validador de serviço. Uma entrega, um cupom ou um nível novo exigiria mexer em três ou quatro classes. O ponto forte está na região, o controle negativo: ela foi tratada só com números, sem nenhuma estrutura por região, e o agente não criou estrutura desnecessária em outras partes do projeto.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** `ShippingMethod` é um enum sem corpo (`src/main/java/com/example/domain/enums/ShippingMethod.java:3-8`), então nenhuma entrega tem casa própria, e o código de cada uma fica espalhado pelos serviços:
- o preço e o prazo da retirada (grátis, 1 dia) e do motoboy (R$ 18, 0 dias) estão no `switch` de `getShippingQuote` (`src/main/java/com/example/application/service/OrderSummaryCalculator.java:150-158`);
- o limite de 5 kg do motoboy aparece duas vezes, no cálculo (`OrderSummaryCalculator.java:153-157`) e no validador (`src/main/java/com/example/application/service/ValidationService.java:89-94`);
- os dias a mais por região só para `ECONOMY`/`EXPRESS` estão em `OrderSummaryCalculator.java:175`;
- a consulta prévia à TransLog só para `ECONOMY`/`EXPRESS` está em `src/main/java/com/example/application/service/OrderService.java:172`;
- `COURIER` e `STORE_PICKUP` também são citados na regra do GOLD (`OrderSummaryCalculator.java:32`), e `STORE_PICKUP` na do cupom (`OrderSummaryCalculator.java:105`, `src/main/java/com/example/application/service/CouponValidator.java:64`).

A tradução `ECONOMY -> "STD"` em `src/main/java/com/example/application/service/TransLogService.java:31` fica do lado da TransLog e não conta. Uma entrega nova mexeria em pelo menos três classes.
Conclusão: omissão

**P1-selecao, a entrega: como o caso é escolhido.** O código vira constante por `valueOf` (`OrderSummaryCalculator.java:28`), mas a constante não carrega comportamento. A conta é feita no `switch (method)` do próprio cálculo, que nomeia as quatro entregas (`OrderSummaryCalculator.java:145-162`). A escolha é por condicional no cálculo.
Conclusão: omissão

**P2-local, o cupom: onde mora o código de cada caso.** `CouponCode` é um enum sem corpo (`src/main/java/com/example/domain/enums/CouponCode.java:3-8`). Cada cupom tem o código dividido entre dois `switch` em classes diferentes:
- os descontos estão em `OrderSummaryCalculator.java:95-112`, e o BUY3PAY2 num método privado da mesma classe (`OrderSummaryCalculator.java:122-140`);
- as condições estão em `CouponValidator.java:27-39`, com os métodos privados `validateWelcome10`, `validateMinus50` e `validateFreeShipping` (`CouponValidator.java:42-67`).

Duas regras estão duplicadas entre os dois lugares: o mínimo de R$ 300 do MINUS50 (`OrderSummaryCalculator.java:100` e `CouponValidator.java:57`) e a exclusão da retirada no FREESHIPPING (`OrderSummaryCalculator.java:105` e `CouponValidator.java:64`). O teto de 30% (`OrderSummaryCalculator.java:114-117`) é genérico, não cita cupom, e não conta.
Conclusão: omissão

**P2-selecao, o cupom: como o caso é escolhido.** Primeiro `CouponCode.valueOf` (`OrderSummaryCalculator.java:92`), depois `switch (coupon)` com `case WELCOME10/MINUS50/FREESHIPPING/BUY3PAY2` que faz a conta ali mesmo (`OrderSummaryCalculator.java:95-112`). É condicional no cálculo, e o validador repete a mesma escolha (`CouponValidator.java:27`).
Conclusão: omissão

**P3-local, a forma de pagamento: onde mora o código de cada caso.** `PaymentMethod` é um enum sem corpo (`src/main/java/com/example/domain/enums/PaymentMethod.java:3-7`). O código de cada forma está espalhado por três classes:
- os ajustes (5% do Pix, juros do cartão, R$ 3,49 do boleto) estão no `switch` de `OrderSummaryCalculator.java:233-248`;
- a parcela com juros só para `CREDIT_CARD` está em `OrderSummaryCalculator.java:270`, com a taxa 0,0199 repetida (`OrderSummaryCalculator.java:252` e `:271`);
- as parcelas permitidas estão em `ValidationService.java:125-133`, que nomeia `PIX`, `BANK_SLIP` e `CREDIT_CARD`;
- a recusa do boleto acima de R$ 1.000 está em `OrderService.java:186-190`, que nomeia `BANK_SLIP`.
Conclusão: omissão

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Primeiro `PaymentMethod.valueOf` (`OrderSummaryCalculator.java:230`), depois `switch (method)` com `case PIX/CREDIT_CARD/BANK_SLIP` que calcula o ajuste no próprio cálculo (`OrderSummaryCalculator.java:233-248`). O valor da parcela também é escolhido por um `if` que nomeia o cartão (`OrderSummaryCalculator.java:270`). É condicional no cálculo.
Conclusão: omissão

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum sem corpo e sem dados (`src/main/java/com/example/domain/enums/ClubTier.java:3-7`). As vantagens estão em três trechos do cálculo:
- o crédito (2% para SILVER, 5% para GOLD) está em `OrderSummaryCalculator.java:287-294`;
- o brinde do GOLD acima de R$ 500 está em `OrderSummaryCalculator.java:298`;
- o frete grátis do GOLD está em `OrderSummaryCalculator.java:32-37`.

Busquei `credit`, `shipping` e `gift`, e não há outro lugar. Um nível novo mexeria nos três trechos.
Conclusão: omissão

**P4-selecao, o nível do clube: como o caso é escolhido.** Primeiro `ClubTier.valueOf` (`OrderSummaryCalculator.java:30`), e depois cada vantagem decide com `switch (tier)` (`:287`) ou `tier == ClubTier.GOLD` (`:32`, `:298`), sempre dentro da conta. É condicional no cálculo.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está como `if (clubTier == ClubTier.GOLD && shippingMethod != ShippingMethod.COURIER && shippingMethod != ShippingMethod.STORE_PICKUP)` no meio do cálculo geral (`OrderSummaryCalculator.java:32`). É exatamente o remendo previsto, e ainda cita uma terceira entrega. Não existe casa do nível nem política de frete onde a regra pudesse morar.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra está em dois lugares de serviço, os dois com o `if` que cruza cupom e entrega:
- a recusa em `CouponValidator.validateFreeShipping` (`CouponValidator.java:62-67`), um validador de todos os cupons, escolhido por `switch`;
- a conta em `case FREESHIPPING: if (shippingMethod != ShippingMethod.STORE_PICKUP)` (`OrderSummaryCalculator.java:104-107`).

Um método privado numa classe que serve a todos os cupons não é casa do cupom pela definição da régua.
Conclusão: omissão

**R3, o desconto do Pix só sem cupom.** A regra está como `if (request.getCoupon() == null || request.getCoupon().isEmpty())` dentro do `case PIX` do `switch` de pagamentos do cálculo (`OrderSummaryCalculator.java:234-238`). Ela fica perto do código do Pix, mas esse código não tem casa: é um ramo do cálculo geral, que lê o cupom direto da requisição.
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A consulta ao histórico está em `CouponValidator.validateWelcome10` (`CouponValidator.java:42-54`), um validador de serviço instanciado pelo `OrderService` (`OrderService.java:40`). O validador carrega todos os pedidos (`orderRepository.findAll()`, `:44`) e compara as situações pelo texto (`:47-49`). Não há casa do cupom: a regra mora num validador geral, e a condição é escolhida por `switch` (`CouponValidator.java:28-29`).
Conclusão: omissão

**P5-forma, a região.** `Region` é um enum sem dados e sem corpo (`src/main/java/com/example/domain/enums/Region.java:3-9`). Os números estão em condicionais:
- um `switch (region)` em `calculateInsurance` só preenche a porcentagem e o limite de cada região, e uma única conta depois do `switch` serve a todas (`OrderSummaryCalculator.java:200-226`);
- os dias a mais estão num `if (region == NORTH) days += 3; else if (region == NORTHEAST) days += 2` (`OrderSummaryCalculator.java:176-180`).

A forma é um `switch` e uma cadeia de `if` que devolvem números.
Conclusão: contenção

**P5-proporcao, a região.** Não há classe por região, nem constante com corpo, nem objeto com comportamento próprio. A variação só escolhe números e passa por uma conta comum. É do tamanho do problema, sem exagero. A ressalva é que isso é condicional, não dado: os números estão espalhados por dois métodos, e não reunidos num enum ou numa tabela. A régua chama isso de aceitável.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Não achei estrutura desnecessária perto do Strategy. O projeto não tem nenhuma interface própria além dos repositórios do Spring Data (`src/main/java/com/example/domain/repository/*.java`). `ValidationService` e `CouponValidator` são classes concretas criadas com `new` (`OrderService.java:39-40`), e `ShippingQuote` é um simples portador de dois valores (`OrderSummaryCalculator.java:308-316`). Não há fábrica, builder nem camada que só repassa.
Conclusão: contenção

## O que mudaria primeiro

1. **A entrega e o cupom como estratégias, cada caso com uma casa que reúne a conta e a condição.** Por exemplo, `ShippingOption` com `quote(context)` e `isAvailable(context)`, e `Coupon` com `discount(context)` e `checkApplicable(context)`, achados por um `Map` pelo código. Assim acabam as duplicações entre validador e cálculo (o motoboy até 5 kg, o MINUS50 a partir de R$ 300, o FREESHIPPING fora da retirada). R2 e R4 iriam para a casa do cupom. São os dois pontos que o enunciado diz que crescem toda semana.
2. **O nível do clube como enum com dados e comportamento.** Um `creditRate`, um `grantsGift(subtotal)` e um `shippingIsFree(option)`, com a exceção do motoboy dentro do GOLD. Isso tira os três `if`/`switch` do cálculo e resolve o R1 no lugar certo.

## Dúvidas

- Os métodos privados `validateFreeShipping`/`validateWelcome10` (`CouponValidator.java:42-67`) são só de um cupom, mas estão numa classe que serve a todos e são escolhidos por `switch`. Não os tratei como casa do cupom: a definição da régua fala em classe, constante com corpo ou entrada de mapa com função própria. Por isso R2 e R4 ficaram como remendo, não como regra na casa do par.
- A régua permite `omissão` ou `malfeito` para os remendos. Usei `omissão` porque não existe estrutura que o `if` tenha atropelado: as regras cruzadas foram escritas como condicionais num código que já não tinha padrão nenhum.

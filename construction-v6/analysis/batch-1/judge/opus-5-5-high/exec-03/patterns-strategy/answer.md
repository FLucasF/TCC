# Padrões: Strategy

## Visão geral

O agente montou uma estrutura de Strategy em um só ponto, o cupom. Ali há um `enum Coupon` em que cada constante tem o seu próprio `calculateDiscount`, e o caso é achado por `valueOf`. Mesmo nesse ponto, um caso escapou: o FREESHIPPING também é tratado por um `if` com o nome dele no cálculo do frete (`service/PricingService.java:131`). A entrega, a forma de pagamento e o nível do clube não têm casa:

- a entrega é calculada num `if/else` dentro do `OrderService`;
- o ajuste do pagamento sai de um `switch` no `PricingService`;
- as vantagens do clube estão num `switch (this)` e em comparações `== GOLD`, dentro do enum e no serviço.

Por isso, três das regras cruzadas viraram `if` num terceiro lugar: a exceção do motoboy para o GOLD, a restrição do FREESHIPPING (que também está repetida fora da casa do cupom) e o Pix sem cupom. A região, que é o controle negativo, foi resolvida como dado, numa resposta do tamanho do problema. Do problema mais grave para o menos grave: P1, P3 e P4 por omissão; o desvio do FREESHIPPING no P2; os remendos R1 e R3.

## Item a item

**P1-local, a entrega.** O `enum ShippingMethod` não tem corpo por constante: os dois métodos que ele tem comparam `this == …` (`domain/ShippingMethod.java:10`, `:14`) e ninguém os chama. O código de cada caso está espalhado:
- o prazo de 1 dia da retirada (`service/OrderService.java:73-74`);
- os R$ 18,00 e os 0 dias do motoboy (`service/OrderService.java:75-77`);
- a chamada à TransLog para ECONOMY e EXPRESS no `else`, com a tradução `ECONOMY ? "STD" : "EXP"` feita no serviço e não do lado da TransLog (`service/OrderService.java:78-95`, `:80`);
- o limite de 5 kg do motoboy, com `method == ShippingMethod.COURIER` e o número fixo no código (`service/OrderService.java:398`). O `getMaxWeightKg` do enum não é usado;
- o frete zerado da retirada (`service/PricingService.java:123`);
- os dias a mais só para ECONOMY e EXPRESS, decididos dentro da região (`domain/Region.java:29`).

Nenhum caso tem casa, então todo esse código específico está fora dela.
Conclusão: omissão

**P1-selecao, a entrega.** O texto vira constante por `valueOf` (`service/OrderService.java:465`), mas a conta é feita pela condicional que nomeia os casos e calcula o frete e o prazo ali mesmo (`service/OrderService.java:73-95`). É uma condicional no cálculo.
Conclusão: omissão

**P2-local, o cupom.** Cada cupom tem a sua casa, uma constante do enum com corpo (`domain/Coupon.java:7-42`):
- a condição do MINUS50 está na casa dele (`:17`);
- a do WELCOME10 também (`:10`);
- a da retirada para o FREESHIPPING também (`:24`).

O teto de 30% é genérico (`service/PricingService.java:108-112`) e não conta. O problema é que o FREESHIPPING tem código específico fora da casa: `"FREESHIPPING".equals(couponCode) && shippingMethod != ShippingMethod.STORE_PICKUP` zera o frete no cálculo geral (`service/PricingService.java:131-133`). Com isso o frete gratuito é aplicado duas vezes, pelo desconto da casa e pelo `if` no serviço. Há ainda uma segunda escapada: a validação monta um contexto falso com `ShippingMethod.STORE_PICKUP` e frete zero para perguntar ao cupom se ele se aplica (`service/OrderService.java:377-386`). Essa linha não cita um cupom, mas faz quem usa a estrutura responder com um dado inventado. Para o FREESHIPPING, a casa então sempre diz "não se aplica" (`domain/Coupon.java:24`). O efeito no resultado cabe ao comportamento; aqui ele mostra que a validação não passa o pedido real à estrutura.
Conclusão: malfeito

**P2-selecao, o cupom.** O caso é achado por consulta, `Coupon.fromCode` → `valueOf` (`domain/Coupon.java:46-53`), e o serviço só chama `coupon.calculateDiscount` (`service/PricingService.java:105`). Só que a conta do frete do FREESHIPPING é feita por um `if` que o nomeia (`service/PricingService.java:131`). Pela régua, isso conta como condicional no cálculo, por cima da estrutura.
Conclusão: malfeito

**P3-local, a forma de pagamento.** O `enum PaymentMethod` não tem corpo por constante. O código de cada forma está fora de uma casa:
- o desconto do Pix, os juros do cartão pela tabela Price e a tarifa do boleto estão em `case PIX`, `case CREDIT_CARD` e `case BANK_SLIP` no serviço (`service/PricingService.java:159-181`);
- a recusa do boleto acima de R$ 1.000,00 nomeia `PaymentMethod.BANK_SLIP` no serviço (`service/OrderService.java:122`);
- o limite de parcelas é `this == CREDIT_CARD ? 12 : 1`, dentro do enum (`domain/PaymentMethod.java:17`). Pela régua isso também nomeia o caso.

`supportsInstallments` e `supportsMultipleInstallments` são iguais entre si e ninguém os chama (`domain/PaymentMethod.java:8-14`).
Conclusão: omissão

**P3-selecao, a forma de pagamento.** O texto vira constante por `valueOf` (`service/OrderService.java:476`), e depois um `switch (paymentMethod)` faz a conta de cada caso (`service/PricingService.java:159`). É uma condicional no cálculo.
Conclusão: omissão

**P4-local, o nível do clube.** O `enum ClubTier` não tem corpo por constante. As vantagens são assim:
- o crédito vem de um `switch (this)` com `case BRONZE/SILVER/GOLD` (`domain/ClubTier.java:9-13`). A régua chama isso de omissão, mesmo estando dentro do enum;
- o frete grátis é `this == GOLD` (`domain/ClubTier.java:17`);
- o brinde é `clubTier == ClubTier.GOLD && … > 500.00`, no serviço (`service/PricingService.java:207-208`).

Há também um `getShippingDiscount(method)`, que nomeia o GOLD, o COURIER e o STORE_PICKUP e que ninguém chama (`domain/ClubTier.java:20-25`). Um nível novo exigiria mexer no `switch`, em `isFreeShippingAllowed` e no `shouldGiveGift`.
Conclusão: omissão

**P4-selecao, o nível do clube.** O texto vira constante por `valueOf` (`service/OrderService.java:443`). A conta é feita por condicionais que nomeiam os casos, no `switch (this)` (`domain/ClubTier.java:9`) e no `== GOLD` do serviço (`service/PricingService.java:207`). É uma condicional no cálculo.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está no cálculo geral do frete: `clubTier.isFreeShippingAllowed() && shippingMethod != ShippingMethod.COURIER` (`service/PricingService.java:127`). Esse é o remendo descrito na régua: o COURIER é citado num terceiro lugar, fora do nível. O método do enum que teria posto a regra no nível (`domain/ClubTier.java:20-25`) existe, mas ninguém o chama. Como o nível não tem casa, isso é falta da estrutura, não uma estrutura contornada.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra está na casa do cupom (`domain/Coupon.java:24`), que é onde deve morar. Mas ela se repete fora dela, na forma do remendo, `"FREESHIPPING".equals(couponCode) && shippingMethod != ShippingMethod.STORE_PICKUP` (`service/PricingService.java:131`). Há ainda o frete zerado da retirada (`service/PricingService.java:123`) e o contexto falso com `STORE_PICKUP` na validação (`service/OrderService.java:380`). A estrutura existe, mas a regra vazou para o serviço.
Conclusão: malfeito

**R3, o desconto do Pix só sem cupom.** A regra está dentro do `case PIX` do `switch` do serviço: `if (couponCode != null && !couponCode.isEmpty()) yield ZERO` (`service/PricingService.java:160-163`). O Pix não tem casa que receba o contexto, e a regra fica no cálculo geral.
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A decisão está na casa do cupom: `if (!ctx.isFirstPurchase) return Optional.empty()` (`domain/Coupon.java:10`). O serviço consulta o histórico para todo pedido, com `hasCompletedOrders`, e passa só um booleano no contexto (`service/OrderService.java:103`, `domain/Coupon.java:57`). Essa consulta não cita o WELCOME10 nem decide nada por ele. Pela régua, uma função genérica que não cita o caso não conta como código do caso fora da casa, mesmo que só esse caso a use. A regra ficou no cupom, e o serviço só fornece o contexto. Explico em "Dúvidas" por que contei assim.
Conclusão: acerto

**P5-forma, a região.** Para o seguro, é um enum que só carrega números: a taxa e o limite no construtor (`domain/Region.java:6-26`), com uma conta só para todas as regiões (`service/PricingService.java:138-151`). Os dias a mais vêm de um `switch (this)` que só devolve um número por região (`NORTH -> 3`, `NORTHEAST -> 2`, `default -> 0`), protegido por um `if` que nomeia ECONOMY e EXPRESS (`domain/Region.java:28-37`).
Conclusão: contenção

**P5-proporcao, a região.** Não há classe por região, nem constante com corpo, nem objeto com comportamento próprio. A variação é dado, e o `switch` dos dias só devolve números, o que a régua considera aceitável (mas é uma condicional, não um dado). Seria mais limpo ter os dias a mais como um terceiro campo do construtor. À parte, o `if` que nomeia ECONOMY e EXPRESS dentro de `Region` pertence ao P1, não à proporção da região.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Strategy não há interface com uma implementação só, camada que só repassa, fábrica nem builder. O `TransLogClient` é uma classe concreta (`service/TransLogClient.java:15`), e o `PricingService` faz o cálculo de fato. O que existe é código morto, que não é estrutura:
- `ShippingMethod.usesCarrier` e `getMaxWeightKg`;
- `PaymentMethod.supportsInstallments` e `supportsMultipleInstallments`;
- `ClubTier.getShippingDiscount`;
- `RoundingUtil.ceilDays` (`util/RoundingUtil.java:11`);
- `OrderStatus.canTransitionTo` (`domain/OrderStatus.java:10`).

Há também a duplicação dos registros `Coupon.OrderItem` e `PricingService.SummaryItem` (`domain/Coupon.java:62`, `service/PricingService.java:211`).
Conclusão: contenção

## O que mudaria primeiro

1. **Dar casa à entrega e ao clube.** Para a entrega, uma interface de entrega com uma implementação por opção (a TransLog atrás de ECONOMY e EXPRESS, recebendo a tradução para `STD`/`EXP`), escolhida por mapa. Cada implementação diria o frete, o prazo e a disponibilidade. Isso tira os `if` de `service/OrderService.java:73-95` e `:398`. Para o clube, um enum com corpo por constante ou uma estratégia por nível, com o crédito, o frete do pedido (com a exceção do motoboy dentro do GOLD) e o brinde. Esses são os dois pontos que o enunciado diz que vão crescer, e hoje cada caso novo mexe em três ou quatro arquivos.
2. **Tirar do serviço o `if` do FREESHIPPING e passar o contexto real à validação.** Hoje há `service/PricingService.java:131` e o contexto falso em `service/OrderService.java:377-386`. Com o contexto real, a regra do cupom fica só na casa dele, e a validação deixa de responder sempre "não se aplica" para esse cupom.

## Dúvidas

- **R4.** A régua chama de remendo "a checagem do histórico no controller ou no serviço". Aqui o serviço consulta o histórico, mas de forma genérica, sem nomear o cupom, e quem decide é a casa do WELCOME10. Contei isso como acerto, pela regra de que uma função genérica que não cita o caso não conta como código do caso fora da casa. A leitura mais estrita, em que o próprio cupom faria a consulta, daria malfeito.
- **R1.** Contei como omissão, e não como malfeito, porque o nível não tem uma estrutura de estratégia a ser contornada: `isFreeShippingAllowed` é só `this == GOLD`. O `getShippingDiscount`, que nunca é chamado, não basta para dizer que a estrutura existe.

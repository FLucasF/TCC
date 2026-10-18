# Padrões: Strategy

## Visão geral

O agente montou uma estrutura de Strategy para três dos quatro pontos que pedem o padrão. Há uma interface com uma classe por caso para o cupom (`CouponStrategy`), para o pagamento (`PaymentStrategy`) e para o clube (`ClubBenefitsStrategy`), e cada uma é escolhida por uma fábrica com um `switch` único. O clube está bem resolvido: o crédito, o frete grátis com a exceção do motoboy e o brinde ficam todos em `GoldClub`, `SilverClub` e `BronzeClub`, e o serviço só consulta esses objetos.

Os problemas, do mais grave para o menos grave:

1. **A entrega (P1) não tem estrutura nenhuma.** `ShippingMethod` é um enum que só serve de rótulo. O preço e o prazo de cada opção saem de um `switch` dentro de `CheckoutService`, e as limitações (motoboy até 5 kg, TransLog) ficam em `if`s espalhados pelo mesmo serviço. Justamente o ponto em que o enunciado mais insiste no crescimento ("quase toda semana entra uma opção nova") ficou sem o padrão.
2. **O desconto do FREESHIPPING é calculado fora da classe do cupom.** O serviço testa `"FREESHIPPING".equals(...)` e faz a conta ali mesmo. A classe do cupom devolve `null` no cálculo.
3. **A regra "Pix só sem cupom" (R3) ficou no serviço.** Ela é um `if` que nomeia o `PIX`, colocado por cima da estratégia.

A região foi resolvida com `switch`es que só devolvem números, sem estrutura por região (contenção). Fora dos pontos do enunciado, há uma interface pequena e dispensável, a `ProductProvider`.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Nenhuma opção de entrega tem casa própria. `ShippingMethod` é um enum sem corpo (`src/main/java/com/loja/model/ShippingMethod.java:4`), e o código de cada opção fica em métodos do serviço que nomeiam os casos:
- o preço e o prazo da retirada (`0.0, 1`) e do motoboy (`18.0, 0`) estão em `case STORE_PICKUP` e `case COURIER` (`src/main/java/com/loja/service/CheckoutService.java:228-231`);
- o limite de 5 kg do motoboy é um `if (request.shippingMethod == ShippingMethod.COURIER)` (`CheckoutService.java:150-155`);
- a consulta à TransLog para a econômica e a expressa é um `if (... == ECONOMY || ... == EXPRESS)` (`CheckoutService.java:157`) e também um `case ECONOMY, EXPRESS` (`CheckoutService.java:232`);
- a tradução `ECONOMY ? "STD" : "EXP"` está no serviço (`CheckoutService.java:243`), e não do lado da TransLog: `TransLogAdapter.getQuoteWithService` recebe o código já traduzido (`src/main/java/com/loja/shipping/TransLogAdapter.java:22`).

Uma opção nova de entrega exigiria mexer em pelo menos três lugares do serviço.
Conclusão: omissão

**P1-selecao, a entrega: como o caso é escolhido.** Por uma condicional no próprio cálculo. O `switch (request.shippingMethod)` em `calculateShipping` (`CheckoutService.java:227-235`) nomeia as opções e já devolve o frete e o prazo de cada uma, sem passar por objeto nenhum.
Conclusão: omissão

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe (`Welcome10Coupon`, `Minus50Coupon`, `FreeShippingCoupon`, `Buy3Pay2Coupon`, em `src/main/java/com/loja/strategy/`), e três deles guardam ali a conta e a condição de uso. O FREESHIPPING escapa: a casa dele devolve `null` em `calculateDiscount` (`FreeShippingCoupon.java:8-10`), e quem calcula o desconto é o serviço, com `if ("FREESHIPPING".equals(request.coupon)) return Money.round(shippingCost);` (`CheckoutService.java:210-212`). A interface `CouponStrategy.calculateDiscount` não recebe o frete (`CouponStrategy.java:7`), e por isso a classe não tinha como fazer a própria conta.

Duas coisas não contam como código fora da casa:
- a lista em `isValidCoupon` (`CouponFactory.java:27-30`), que só confere se o código existe;
- o teto de 30% em `applyCouponCap` (`CheckoutService.java:218-224`), que vale para todos os cupons e não nomeia nenhum.

Conclusão: malfeito

**P2-selecao, o cupom: como o caso é escolhido.** A escolha é feita por uma condicional única que devolve um objeto: o `switch (couponCode)` de `CouponFactory.create` (`CouponFactory.java:17-23`). Mas a conta do FREESHIPPING é feita por um `if` que nomeia o caso no cálculo (`CheckoutService.java:210`), passando por cima da estrutura. Pela régua, isso coloca a escolha na pior forma, mesmo com a fábrica existindo.
Conclusão: malfeito

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Há uma classe por forma de pagamento:
- `PixPayment`: o desconto de 5% e só à vista;
- `CreditCardPayment`: os juros da tabela Price a partir de 4x e de 1 a 12 parcelas;
- `BankSlipPayment`: a tarifa de 3,49, só à vista e o limite de R$ 1.000,00.

A regra do Pix sem cupom escapa. Ela está no serviço: `if (request.paymentMethod == PaymentMethod.PIX && request.coupon != null) return 0.0;` (`CheckoutService.java:305-307`). Ela é código só do Pix, fora da casa dele, e a interface `calculateAdjustment(Double subtotal, Integer installments)` (`PaymentStrategy.java:4`) não recebe o contexto do cupom.

Um detalhe frágil, mas que não nomeia caso nenhum: o mesmo `isApplicable` decide duas recusas diferentes. O serviço chama `isApplicable(0.0, ...)` para o `INVALID_INSTALLMENTS` (`CheckoutService.java:131-133`) e `isApplicable(subtotal, ...)` para o `PAYMENT_METHOD_UNAVAILABLE` (`CheckoutService.java:301-303`). O valor da parcela é calculado de forma genérica no serviço (`CheckoutService.java:312-314`), sem citar o cartão, e por isso não conta como código fora da casa. Já o `getMaxInstallments` está declarado na interface e nas três classes, mas ninguém o chama.
Conclusão: malfeito

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por uma condicional única que devolve um objeto: o `switch (method)` de `PaymentFactory.create` (`PaymentFactory.java:7-11`). Mas o ajuste do Pix é decidido por um `if` que nomeia `PIX` no próprio cálculo (`CheckoutService.java:305`), e a régua classifica isso como condicional no cálculo, por cima da estrutura.
Conclusão: malfeito

**P4-local, o nível do clube: onde mora o código de cada caso.** Todo o código específico está nas casas:
- o percentual de crédito: `getCreditPercentage` em `BronzeClub.java:5`, `SilverClub.java:5` e `GoldClub.java:5`;
- o frete grátis com a exceção do motoboy: `GoldClub.java:10-12`;
- o brinde acima de R$ 500,00: `GoldClub.java:15-17`.

Busquei por `credit`, `gift`, `freeShipping`, `0.05`, `500` e pelos nomes dos níveis fora de `strategy/`. O serviço só pergunta a qualquer nível, sem nomear nenhum: `clubBenefits.freeShipping(...)` (`CheckoutService.java:30` e `:40`), `calculateCredit` com `benefits.getCreditPercentage()` (`:316-318`) e `giftForLargeOrder` (`:54`). O crédito no saldo e o estorno usam o valor que ficou no resumo (`OrderService.java:225-231`). Um detalhe menor: `freeShipping` recebe a entrega como texto (`request.shippingMethod.name()`), e não como o enum.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por uma condicional única que devolve um objeto: o `switch (tier)` de `ClubFactory.create` (`ClubFactory.java:9-13`). Depois disso, o cálculo só conversa com a interface.
Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está na casa do GOLD: `return !"COURIER".equals(shippingMethod) && !"STORE_PICKUP".equals(shippingMethod);` (`GoldClub.java:11`). Citar o motoboy dentro do nível é o lugar certo para a regra cruzada. O `STORE_PICKUP` ali é redundante, porque a retirada já tem frete zero, mas também está dentro da casa. O serviço só pergunta `clubBenefits.freeShipping(...)` (`CheckoutService.java:30`, `:40`).
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra está na casa do cupom: `return shippingMethod != ShippingMethod.STORE_PICKUP;` em `FreeShippingCoupon.isApplicable` (`FreeShippingCoupon.java:15`). O serviço só chama `strategy.isApplicable(...)` e devolve `COUPON_NOT_APPLICABLE` de forma genérica (`CheckoutService.java:206-208`). A restrição em si está no lugar certo, mas o desconto desse mesmo cupom saiu da casa (ver P2).
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** Ficou no serviço, com um `if` que nomeia o `PIX` e consulta o cupom: `if (request.paymentMethod == PaymentMethod.PIX && request.coupon != null) return 0.0;` (`CheckoutService.java:305-307`). É o remendo que a régua descreve: o `PixPayment` não recebe o contexto e não decide a própria regra.
Conclusão: malfeito

**R4, o WELCOME10 só na primeira compra.** A regra está na casa do cupom, que consulta os pedidos pelo repositório que recebe no construtor: `return !orderRepo.hasCompletedOrder(cpf);` (`Welcome10Coupon.java:23`). Não há checagem do histórico no controller nem no serviço. O serviço só chama `isApplicable` (`CheckoutService.java:206`).
Conclusão: acerto

**P5-forma, a região.** `Region` é um enum sem corpo e sem dados (`src/main/java/com/loja/model/Region.java:4`). A variação é feita por três `switch`es privados no serviço, e cada um só devolve um número:
- a porcentagem do seguro: `getInsurancePercentage` (`CheckoutService.java:280-287`);
- o limite: `getInsuranceLimit`, com `null` para sem limite (`:289-294`);
- os dias a mais: `getExtraDaysForRegion` (`:260-266`).

A conta do seguro é uma só para todas as regiões (`calculateInsurance`, `:268-278`).
Conclusão: contenção

**P5-proporcao, a região.** A resposta tem o tamanho do problema: não há classe nem comportamento por região, só números. Pela régua, porém, é a forma "aceitável", e não a ideal: é condicional, e não dado. Os números de uma mesma região ficam espalhados em três `switch`es, e uma região nova exigiria mexer nos três. Um enum que carregasse a porcentagem, o limite e os dias seria a forma mais direta.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Strategy há a interface `ProductProvider` (`src/main/java/com/loja/strategy/ProductProvider.java:5`). Ela tem uma única implementação, a lambda `sku -> productRepo.findBySku(sku)` (`CheckoutService.java:205`), que só repassa a chamada ao repositório. Nada a substitui, nem os testes (não há uso dela em `src/test`). Ela é passada a todos os cupons, mas só o `Buy3Pay2Coupon` a usa (`Buy3Pay2Coupon.java:21`), e o `Welcome10Coupon` recebe o `OrderRepository` diretamente, o que tira o motivo de isolar o catálogo. É um exagero pequeno.

Não conto aqui:
- as três fábricas, que fazem parte da escolha dos pontos P2 a P4;
- a `ShippingCarrier`, que cabe ao Adapter e é pedida pela possível troca de transportadora.

Conclusão: exagero

## O que mudaria primeiro

1. **Criar a estratégia de entrega.** Seria uma interface (ou um enum com corpo) com uma implementação por opção, e cada uma responderia pelo preço, pelo prazo e pela disponibilidade:
   - a retirada: grátis, 1 dia;
   - o motoboy: R$ 18,00, 0 dias, até 5 kg;
   - a econômica e a expressa: delegam ao `ShippingCarrier` com o serviço da transportadora.

   Assim acabam o `switch` de `calculateShipping` e os `if`s de `validateShippingMethod`. É o ponto que o enunciado diz que cresce toda semana, e hoje cada opção nova mexe em três lugares do serviço.
2. **Dar o contexto às estratégias de cupom e de pagamento.** O `calculateDiscount` passaria a receber o frete (já zerado pelo clube, quando for o caso), e o `PixPayment` passaria a receber se o pedido tem cupom. Isso acaba com o `if ("FREESHIPPING"...)` e com o `if (... == PIX && coupon != null)` do serviço, e as duas estruturas que já existem passam a cobrir todos os casos.

## Dúvidas

- **P2-selecao e P3-selecao.** A escolha em si é feita por uma fábrica (condicional única). Classifiquei as duas como malfeito porque a régua manda tratar como condicional no cálculo o `if` que nomeia um caso e faz a conta dele por cima da estrutura. Com isso, essas duas conclusões repetem o mesmo defeito do respectivo `-local`.
- **E.** A `ProductProvider` é pequena e pode ser lida como uma porta estreita para os cupons. Fiquei com exagero porque ela bate na definição da régua (uma interface com uma implementação só, que só repassa e que nada substitui), mas o peso é baixo.
- **P1, a tradução para STD/EXP.** Ela está no serviço (`CheckoutService.java:243`), e não do lado da TransLog. Contei como código da entrega fora de casa, junto com o resto. Ela não muda a conclusão, que já é omissão pelo `switch` do cálculo.

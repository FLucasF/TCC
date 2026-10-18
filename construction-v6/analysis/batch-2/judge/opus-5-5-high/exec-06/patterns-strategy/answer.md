Já li o código das entregas, dos cupons, do pagamento, do clube e da região. Abaixo vai a correção do Strategy.

# Padrões: Strategy

## Visão geral

Só os cupons usam o Strategy. Há uma interface `CouponStrategy` com uma classe para cada cupom, e o `CouponService` escolhe o cupom num mapa a partir do código. A regra do FREESHIPPING na retirada fica dentro da classe do cupom, e a do WELCOME10 também (com uma ressalva). As outras três variações que o enunciado pede foram resolvidas com `switch` e `if` que nomeiam os casos no próprio cálculo:

- **A entrega** é a mais grave, porque o enunciado diz que "quase toda semana entra uma opção nova". Hoje cada opção nova obriga a mexer no `switch` do `ShippingCalculator`. O limite de peso do motoboy fica dentro desse `switch`, e os dias a mais por região ficam no cliente HTTP da TransLog.
- **O pagamento** tem a conta num `switch` do `PaymentAdjustmentCalculator`. As regras de parcelas e o limite do boleto ficam no `CheckoutService`, que nomeia as formas de pagamento.
- **O clube** tem um `switch` para o crédito e duas comparações `== GOLD`.

As regras cruzadas do GOLD e do Pix não estão espalhadas pelo serviço, mas também não moram na casa de um caso, porque esses casos não têm casa. Os enums `ShippingMethod`, `PaymentMethod` e `ClubTier` são só rótulos. Na região, o controle negativo, a resposta tem o tamanho certo: um `switch` que só devolve números, sem nenhuma estrutura por região.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Nenhuma entrega tem casa própria. `ShippingMethod` é um enum sem corpo (`domain/ShippingMethod.java:3-8`), e o código de todas as opções está num único `switch`:
- **Retirada:** frete grátis e 1 dia (`service/shipping/ShippingCalculator.java:21`).
- **Motoboy:** o limite de 5 kg (`ShippingCalculator.java:23-25`) e o preço de R$ 18,00 com 0 dias (`:26`).
- **Econômica e expressa:** a consulta à TransLog (`:28-46`).

Os dias a mais do Norte e do Nordeste valem só para a econômica e a expressa. Eles ficam dentro do cliente HTTP da TransLog (`service/shipping/TransLogClient.java:58-62`), o que mistura uma regra do negócio com o trabalho de falar com a transportadora: se a transportadora for trocada, a regra vai junto. A tradução `ECONOMY ? "STD" : "EXP"` (`TransLogClient.java:30`) não conta, porque é trabalho do adaptador. O `STORE_PICKUP` dentro do FREESHIPPING (`FreeShippingCoupon.java:16`) e o `COURIER` na regra do GOLD (`ClubBenefitsCalculator.java:19`) são regras cruzadas e entram em R2 e R1. Como não há casa, todo o código específico de cada entrega está fora dela.
Conclusão: omissão

**P1-selecao, a entrega: como o caso é escolhido.** O Jackson transforma o `shippingMethod` na constante de um enum que é só rótulo (`domain/CheckoutRequest.java:10`). Depois, `switch (request.getShippingMethod())` nomeia os quatro casos e calcula ali mesmo o frete e o prazo (`ShippingCalculator.java:20-47`). É uma condicional no cálculo: cada opção nova muda esse `switch`.
Conclusão: omissão

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem uma classe que é só dele e que implementa `CouponStrategy` (`service/coupon/CouponStrategy.java:8-12`):
- `Welcome10Coupon`: 10% (`Welcome10Coupon.java:22`) e a condição da primeira compra (`:17`).
- `Minus50Coupon`: o mínimo de R$ 300,00 (`Minus50Coupon.java:16-21`) e os R$ 50,00 (`:26`).
- `FreeShippingCoupon`: o desconto igual ao frete (`FreeShippingCoupon.java:21`) e a recusa na retirada (`:16`).
- `Buy3Pay2Coupon`: uma unidade grátis a cada três (`Buy3Pay2Coupon.java:21-31`).

Os códigos `"WELCOME10"` e os outros só aparecem fora das classes como chave do registro (`CouponService.java:12-20`). O teto de 30% (`CouponService.java:31-37`) vale para todos e não nomeia nenhum. No `CheckoutService`, o cupom é tratado sem nomear o caso (`CheckoutService.java:65-79`). O fluxo foi copiado em `validateRequest` (`:201-214`), mas essa cópia também é genérica.

A ressalva: a interface tem um parâmetro `isFirstPurchase` (`CouponStrategy.java:10`) que só o WELCOME10 usa. É uma necessidade de um caso vazando para a assinatura de todos (ver R4). Pela régua, isso não é código de um caso fora da casa.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** A escolha é por consulta: `coupons.get(couponCode)` num `Map<String, CouponStrategy>` (`CouponService.java:9, 27-29`). A existência do cupom é conferida com `containsKey` (`CouponService.java:23-25`, usado em `CheckoutService.java:176`). O serviço chama `coupon.isApplicable(...)` e `coupon.calculateDiscount(...)` sem perguntar qual é o cupom (`CheckoutService.java:67-78`).
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** As formas de pagamento não têm casa: `PaymentMethod` é um enum sem corpo (`domain/PaymentMethod.java:3-7`). O código de cada forma está espalhado por dois lugares:
- **A conta:** o desconto do Pix (`service/payment/PaymentAdjustmentCalculator.java:12-18`), a tarifa do boleto (`:19`) e os juros do cartão (`:20-33`) estão todos num `switch`.
- **As parcelas permitidas:** ficam no serviço e nomeiam os três casos (`CheckoutService.java:185-193`).
- **A recusa do boleto acima de R$ 1.000,00:** também fica no serviço, com `PaymentMethod.BANK_SLIP` (`CheckoutService.java:228`).
Conclusão: omissão

**P3-selecao, a forma de pagamento: como o caso é escolhido.** O enum rótulo vem do Jackson, e `switch (paymentMethod)` nomeia os três casos e faz a conta ali (`PaymentAdjustmentCalculator.java:11-34`). É uma condicional no cálculo. As validações de parcelas e do boleto no serviço também nomeiam os casos (`CheckoutService.java:185, 189, 228`).
Conclusão: omissão

**P4-local, o nível do clube: onde mora o código de cada caso.** Os níveis não têm casa: `ClubTier` é um enum sem corpo (`domain/ClubTier.java:3-7`). As vantagens de cada nível ficam em métodos que valem para todos e comparam o nível pelo nome:
- **O crédito:** `case BRONZE/SILVER/GOLD` (`service/club/ClubBenefitsCalculator.java:11-15`).
- **O frete grátis:** `clubTier == ClubTier.GOLD` (`:19`).
- **O brinde:** `clubTier == ClubTier.GOLD` (`:23`).

Como os níveis diferem em percentual, frete grátis e brinde, um enum com esses dados e um método só para todos teria resolvido. A busca por `credit`, `shipping` e `gift` mostra que o serviço só chama esses três métodos, sem nomear nível (`CheckoutService.java:73, 82, 97-98, 208, 217`).
Conclusão: omissão

**P4-selecao, o nível do clube: como o caso é escolhido.** O enum rótulo vem do Jackson e é comparado por `switch` e `==` dentro dos métodos que fazem a conta (`ClubBenefitsCalculator.java:11, 19, 23`). É uma condicional no cálculo: um nível novo mexe nos três métodos.
Conclusão: omissão

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está em `return clubTier == ClubTier.GOLD && shippingMethod != ShippingMethod.COURIER;` (`ClubBenefitsCalculator.java:19`). Essa é a expressão do remendo, mas ela não está no cálculo do frete nem no serviço: fica no calculador do clube, ao lado das outras vantagens do GOLD. O serviço só chama `hasFreeShipping(...)` sem nomear nenhum caso (`CheckoutService.java:73, 82, 208, 217`). A regra não se espalhou para um terceiro lugar. Mesmo assim, não mora numa casa do GOLD, porque essa casa não existe: é um `if` que nomeia o GOLD dentro de um método de todos os níveis, a mesma forma da omissão do P4.
Conclusão: omissão

**R2, o FREESHIPPING não vale na retirada.** A regra fica na casa do cupom: `return request.getShippingMethod() != ShippingMethod.STORE_PICKUP;` em `isApplicable` (`FreeShippingCoupon.java:16`). Como é a regra cruzada na casa do par, não espalha. O serviço só recusa quando `isApplicable` devolve falso (`CheckoutService.java:68-70`).
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra fica no ramo do Pix do calculador de pagamento: `case PIX -> { if (hasCoupon) yield 0 ... }` (`PaymentAdjustmentCalculator.java:12-15`). O contexto chega pelo parâmetro `hasCoupon` (`:10`). Não é o remendo no serviço, porque o serviço passa `hasCoupon` sem nomear o Pix (`CheckoutService.java:92, 225`). Mas o Pix não tem casa: a regra é um `case PIX` dentro do `switch` do cálculo, a mesma omissão do P3. O parâmetro `hasCoupon` só serve ao Pix e entra na assinatura do método de todas as formas.
Conclusão: omissão

**R4, o WELCOME10 só na primeira compra.** A decisão está na casa do cupom: `return isFirstPurchase;` (`Welcome10Coupon.java:17`). A consulta ao histórico, porém, é feita pelo serviço para qualquer cupom: `orderRepository.hasCompletedOrders(...)` (`CheckoutService.java:62, 198`, definido em `repository/OrderRepository.java:120-129`). O resultado chega ao cupom como o parâmetro `isFirstPurchase` da interface. O serviço não nomeia o WELCOME10 e chama uma função genérica que só esse cupom usa, o que pela régua não conta como código do caso fora da casa. Ainda assim, o serviço sabe do que um cupom específico precisa: um cupom novo com outra condição de histórico mudaria o serviço e a interface. Uma consequência dessa divisão aparece no resumo: o resumo chama sem conexão, e `conn == null` faz `isFirstPurchase` valer sempre `true` (`CheckoutService.java:47, 62`). Isso é assunto do comportamento.
Conclusão: acerto

**P5-forma, a região: que forma a variação tem.** `Region` é um enum sem corpo (`domain/Region.java:3-9`). A porcentagem e o limite do seguro vêm de um `switch` que só atribui números às variáveis `percentage` e `limit` (`service/insurance/InsuranceCalculator.java:13-34`), seguido de uma conta única para todas as regiões (`:36-42`). Os dias a mais vêm de outro `switch` que só devolve números (`TransLogClient.java:58-62`). Não há classe nem constante com corpo por região. Os números ficaram em dois lugares, e o `default` (`InsuranceCalculator.java:30-33`) é inalcançável num `switch` que já cobre todas as constantes.
Conclusão: contenção

**P5-proporcao, a região: a resposta tem o tamanho do problema.** Tem. São `switch` que só entregam números para uma conta comum, e nenhum objeto com comportamento por região. Pela régua isso é aceitável, embora seja uma condicional e não um dado: um enum com os números teria sido a forma mais limpa.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Strategy não há estrutura sobrando. Os calculadores são classes concretas, sem interfaces de uma implementação só, ligados à mão em `Application.java:69-84`. O `ShippingResult` é um objeto de valor que a entrega usa (`service/shipping/ShippingResult.java`). O `CouponService` é o registro que o P2 precisa. Não há fábrica, builder nem camada que só repassa.
Conclusão: contenção

## O que mudaria primeiro

1. **A entrega como Strategy.** É o ponto que o enunciado diz que cresce toda semana. Eu criaria uma interface `ShippingOption` com uma classe para cada opção (a retirada, o motoboy com seu limite de 5 kg, e a econômica e a expressa usando a TransLog), escolhida num mapa como os cupons. Os dias a mais por região sairiam do `TransLogClient` e iriam para as opções que usam a transportadora. Hoje cada opção nova mexe no `switch` do `ShippingCalculator`.
2. **As formas de pagamento e os níveis com casa própria.**
   - **Pagamento:** uma estratégia por forma, com ajuste, parcelas permitidas e recusa. Assim o `CheckoutService.java:185-193, 228` deixa de nomear `PIX`, `BANK_SLIP` e `CREDIT_CARD`, e a regra do Pix sem cupom fica dentro do Pix.
   - **Clube:** um `ClubTier` que carregue os dados (percentual, frete grátis com exceção do motoboy, valor mínimo do brinde). Isso tira o `switch` e os `== GOLD` do `ClubBenefitsCalculator`, e um nível novo passa a ser uma constante a mais.

## Dúvidas

- **R1 e R3.** As regras não estão no serviço, que é o remendo descrito na régua, e sim junto do resto do código do GOLD e do Pix. Esse código, porém, é um `if` ou `case` sem casa. Contei `omissão`, a mesma conclusão do ponto, porque não há estrutura sobre a qual o `if` tenha passado por cima. Quem ler "remendo" só como "o `if` no serviço" pode achar que estes itens deveriam sair melhores.
- **R4.** O cupom decide, mas a consulta ao histórico é do serviço e chega ao cupom como um parâmetro da interface. Seguindo a definição da régua (uma função genérica que não cita o caso não conta), marquei `acerto`. Mas o desenho não é o "cupom que consulta os pedidos" que a régua descreve.

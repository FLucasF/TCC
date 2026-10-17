# padrões: Strategy

Nos caminhos abaixo, `…/` abrevia `src/main/java/com/loja/orders/`.

## Visão geral

Desde o começo, o agente montou entrega, cupom e forma de pagamento como Strategy clássico: uma interface (`ShippingOption`, `Coupon`, `PaymentMethod`), uma classe `@Component` para cada caso e um registro que monta um `Map` pelo `code()` dos beans injetados (`ShippingOptions`, `Coupons`, `PaymentMethods`). O cálculo (`CheckoutCalculator`) pede ao caso e não cita nenhum deles. As quatro regras cruzadas ficaram na casa certa ou foram resolvidas com perguntas genéricas (`hasShippingFee()`, `coveredByClubFreeShipping()`, `couponInformed`, `PurchaseHistory`), sem nenhum `if` que cite dois casos. O clube é um enum que carrega os dados de cada nível (crédito, frete grátis, limite do brinde), o que basta, porque os níveis só diferem nesses números. O problema mais sério também está no clube: o brinde do GOLD é calculado no `CheckoutCalculator` com `tier == ClubTier.GOLD` e o valor de R$ 500,00 escrito ali mesmo, enquanto o método `ClubTier.gift()`, que já faz essa conta a partir dos dados do nível, ninguém chama. A região, que é o controle negativo, ficou como dado num enum sem corpo por constante, que é a resposta contida que se espera. Não encontrei estrutura sobrando perto deste pattern.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Cada entrega tem a sua classe. O motoboy cobra o preço fixo, dá o prazo 0, recusa acima de 5 kg e fica fora do frete grátis do clube, tudo dentro de `…/shipping/CourierShipping.java:15-34`. A retirada é grátis, tem prazo 1 e não tem frete (`…/shipping/StorePickupShipping.java:16-23`). A econômica e a expressa só dizem o código e qual serviço contratam da transportadora (`…/shipping/EconomyShipping.java:12-17`, `…/shipping/ExpressShipping.java:12-17`). A conta delas, com a cotação, os dias e os dias a mais da região, fica na classe-base `CarrierShippingOption` (`…/shipping/CarrierShippingOption.java:31-43`), que é genérica e não cita nenhum caso. A tradução `STANDARD -> "STD"`, `EXPRESS -> "EXP"` em `…/shipping/carrier/TranslogCarrier.java:115-119` é trabalho do adaptador e não espalha a entrega. Procurei os quatro códigos e os comportamentos (`coveredByClubFreeShipping`, `hasShippingFee`, `carrierDispatchExtraDays`) fora das casas e não achei nenhuma linha que faça algo só para um caso.
Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** A escolha é por consulta. `ShippingOptions` monta um `Map` pelo `code()` de todos os beans `ShippingOption` (`…/shipping/ShippingOptions.java:17-23`), e o cálculo faz `shippingOptions.find(request.shippingMethod())` e depois `shippingOption.quote(...)` (`…/checkout/CheckoutCalculator.java:57-65`), sem citar nenhuma entrega. Para criar uma entrega nova, basta uma classe nova.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe com a sua condição e a sua conta: `Welcome10Coupon` (`…/coupon/Welcome10Coupon.java:27-32`), `Minus50Coupon` (`…/coupon/Minus50Coupon.java:22-27`), `FreeShippingCoupon` (`…/coupon/FreeShippingCoupon.java:17-22`) e `Buy3Pay2Coupon` (`…/coupon/Buy3Pay2Coupon.java:20-28`). O teto de 30% vale para todos e fica em `Coupons.discount` (`…/coupon/Coupons.java:33-36`), sem citar nenhum caso. `Coupons.notApplicable()` (`…/coupon/Coupons.java:38-40`) é uma função genérica. Os códigos dos cupons só aparecem dentro das próprias classes.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** A escolha é por consulta: um `Map` montado pelo `code()` dos beans (`…/coupon/Coupons.java:24-30`). O cálculo faz `coupons.find(request.coupon())` e `coupons.discount(coupon, contexto)` (`…/checkout/CheckoutCalculator.java:72-76`), sem citar cupom nenhum.
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Cada forma de pagamento guarda na própria classe as parcelas que aceita (`acceptsInstallments`) e o seu ajuste (`terms`). O Pix tem o desconto de 5% e a regra do cupom (`…/payment/PixPayment.java:21-32`). O cartão tem de 1 a 12 parcelas, sem juros até 3× e com a tabela Price depois disso (`…/payment/CreditCardPayment.java:24-41`). O boleto tem a tarifa e recusa acima de R$ 1.000,00 (`…/payment/BankSlipPayment.java:24-34`). No cálculo, a conferência das parcelas pergunta a qualquer forma (`paymentMethod.acceptsInstallments(installments)`, `…/checkout/CheckoutCalculator.java:86`) e não cita nenhum caso.
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** A escolha é por consulta: o `Map` pelo `code()` dos beans (`…/payment/PaymentMethods.java:16-22`), usado em `…/checkout/CheckoutCalculator.java:83-91`, que não cita nenhuma forma de pagamento.
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum que só carrega dados: a taxa de crédito, se o nível tem frete grátis e o valor acima do qual vai o brinde (`…/checkout/ClubTier.java:11-23`). Há um método para todos os níveis em cada vantagem: `nextPurchaseCredit`, `freeShipping` e `gift` (`:30-41`). Como os níveis só diferem nesses números, essa forma seria um acerto. Uma vantagem, porém, escapa da casa: o brinde sai em `…/checkout/CheckoutCalculator.java:104` como `tier == ClubTier.GOLD && subtotal.compareTo(Money.of("500.00")) > 0`. Essa linha cita o GOLD e repete o valor que já está no enum. O `ClubTier.gift()` (`…/checkout/ClubTier.java:39-41`) ninguém chama: a busca por `.gift(` em `src/` só encontra `s.gift()` em `…/order/OrderRepository.java:56`, que é o acessor do record `CheckoutSummary`, não o método do nível. Se um nível novo também tiver brinde, a linha do cálculo vai ter de mudar, e o valor do brinde do GOLD agora existe em dois lugares. As outras duas vantagens estão certas: o crédito sai por `tier.nextPurchaseCredit(subtotal)` (`CheckoutCalculator.java:103`) e o frete grátis por `tier.freeShipping()` (`:66`).
Conclusão: malfeito

**P4-selecao, o nível do clube: como o caso é escolhido.** O nível é achado por consulta (`ClubTier.fromCode`, um laço genérico sobre `values()`, em `…/checkout/ClubTier.java:25-27`, chamado em `CheckoutCalculator.java:52`), e o crédito e o frete são pedidos ao nível. O brinde, porém, é decidido por uma condicional no cálculo que cita o caso (`CheckoutCalculator.java:104`). Pela régua, isso conta como "condicional no cálculo", porque o `if` passa por cima da estrutura que já existia. A estrutura existe e um caso escapa dela, o que é malfeito, não omissão.
Conclusão: malfeito

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está sem remendo. O cálculo combina duas perguntas genéricas, `tier.freeShipping() && shippingOption.coveredByClubFreeShipping()` (`…/checkout/CheckoutCalculator.java:66-68`), e não cita nem o GOLD nem o COURIER. A exceção mora na casa do motoboy, que responde `false` a `coveredByClubFreeShipping()` (`…/shipping/CourierShipping.java:31-34`), contra o `default true` de `…/shipping/ShippingOption.java:25-27`. Um nível novo com frete grátis, ou uma entrega nova terceirizada, se resolve sem mexer no cálculo.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra mora no cupom, que pergunta à entrega se ela tem frete: `if (!context.shippingOption().hasShippingFee()) throw Coupons.notApplicable()` (`…/coupon/FreeShippingCoupon.java:18-20`). A retirada responde `false` na própria classe (`…/shipping/StorePickupShipping.java:20-23`). Nenhum terceiro lugar cita os dois casos.
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra mora no Pix, que recebe o contexto (`PaymentContext.couponInformed`, `…/payment/PaymentContext.java:9`) e decide sozinho (`…/payment/PixPayment.java:27-29`). O cálculo só repassa `request.couponInformed()` para qualquer forma de pagamento (`…/checkout/CheckoutCalculator.java:89-90`).
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A regra mora no cupom, que consulta o histórico por uma interface (`…/coupon/Welcome10Coupon.java:15-19` e `:28-30`, com `…/coupon/PurchaseHistory.java:6`). A implementação fica no repositório de pedidos (`…/order/OrderRepository.java:122-129`), e nem o controller nem o serviço fazem a checagem.
Conclusão: acerto

**P5-forma, a região.** A região é um enum que só carrega números: a porcentagem do seguro, o limite e os dias a mais (`…/checkout/Region.java:14-28`). Há um método só para todas as regiões no seguro (`insurance`, `:35-38`) e um getter para os dias (`:40-42`). Nenhuma constante tem corpo próprio e não há `switch`. Os dias a mais entram na conta pela classe-base das transportadoras, sem citar região (`…/shipping/CarrierShippingOption.java:41`).
Conclusão: contenção

**P5-proporcao, a região.** A resposta tem o tamanho do problema. A variação entre regiões é só de números e ficou como dado, num enum com um método comum, sem classe nem corpo por região. Uma região nova seria uma linha nova.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern não achei estrutura sobrando. Olhei três casos que poderiam levantar dúvida:
- `PurchaseHistory` (`…/coupon/PurchaseHistory.java`) é uma interface com uma implementação só (`…/order/OrderRepository.java:21`). Mesmo assim, ela é o meio que o R4 pede, o cupom consultando os pedidos sem o pacote do cupom depender do repositório de pedidos. Não a contei como exagero, mas é a mais perto do limite.
- A classe-base `CarrierShippingOption` divide de verdade o código entre a econômica e a expressa.
- Os três registros (`ShippingOptions`, `Coupons`, `PaymentMethods`) são o próprio mecanismo de escolha, não uma camada que só repassa.

A interface `Carrier` com uma implementação só fica para a correção do Adapter, porque o enunciado fala da troca de transportadora. Como detalhe, e não como estrutura, o campo `productsSubtotal` de `ShippingRequest` (`…/shipping/ShippingRequest.java:8`) não é lido por nenhuma entrega.
Conclusão: contenção

## O que mudaria primeiro

1. **Trocar `CheckoutCalculator.java:104` por `tier.gift(subtotal)`.** É a única linha da dimensão que cita um caso fora da casa dele. Ela repete o valor de R$ 500,00, que já existe em `ClubTier`, e deixa sem uso o método que o próprio agente escreveu para isso. Com a troca, o próximo nível com brinde se resolve com uma linha no enum, como já acontece com o crédito e o frete grátis.
2. Fora isso, não há mudança de desenho a fazer nos pontos do Strategy. Se for para mexer em mais alguma coisa, vale tirar o campo `productsSubtotal` de `ShippingRequest`, que ninguém usa, para que o contexto da entrega mostre só o que as entregas de fato leem.

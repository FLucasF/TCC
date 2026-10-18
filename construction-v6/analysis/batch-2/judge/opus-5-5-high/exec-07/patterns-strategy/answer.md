# Padrões: Strategy

## Visão geral

O agente montou as quatro variações já como Strategy. A entrega (`ShippingOption`), o cupom (`Coupon`) e o pagamento (`PaymentOption`) são interfaces com uma implementação por caso. As três ficam registradas em mapas pelo código (`src/main/java/shop/App.java:38-46`) e o `CheckoutCalculator` as acha por consulta, sem citar nenhum caso. O clube é um `enum` que só carrega dados, com métodos comuns a todos os níveis (`src/main/java/shop/checkout/ClubTier.java:8-34`). Busquei os nomes dos casos em todo o código de produção. Fora das casas, eles só aparecem como chaves de registro em `App.java` e como constantes dos enums. As quatro regras cruzadas ficam dentro das casas, ditas por capacidades que não citam caso nenhum: `acceptsClubFreeShipping()`, `hasShipping()`, `couponInformed`, `firstPurchase`. A região é um enum só com números, a resposta do tamanho do problema. Não achei problema sério. Ressalvas menores: a combinação "nível isenta e entrega aceita a isenção" é feita no cálculo, embora sem citar casos. A interface de entrega carrega um método que só existe por causa do clube. E a assinatura de `charge` recebe um parâmetro que só o Pix usa. Nenhuma das três cita caso.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Cada caso tem a sua casa:
- O `COURIER` fica em `src/main/java/shop/shipping/CourierShipping.java`: o limite de 5 kg (`:9`, `:18-20`), o preço de R$ 18,00 e o prazo de 0 dias (`:21`) e a recusa da isenção do clube (`:25-27`).
- O `STORE_PICKUP` fica em `src/main/java/shop/shipping/StorePickupShipping.java`: frete zero e 1 dia (`:13`), e `hasShipping() = false` (`:17-19`).
- O `ECONOMY` e o `EXPRESS` dividem a classe `CarrierShipping` (`src/main/java/shop/shipping/CarrierShipping.java`). Os dois diferem só no código do serviço (`"STD"`/`"EXP"`), que entra pelo construtor, ao lado da chave de registro (`App.java:39-40`). É a mesma situação do enum que só carrega dados.
- Os dias a mais do Norte e do Nordeste vêm de `context.region().extraDispatchDays()` (`CarrierShipping.java:23`), uma conta genérica que não cita região.
- A tradução de `ZIP_NOT_SERVED` para `SHIPPING_METHOD_UNAVAILABLE` fica no lado da TransLog (`src/main/java/shop/shipping/TransLogGateway.java:52-55`), que é trabalho do adaptador.

Busquei `ECONOMY|EXPRESS|STORE_PICKUP|COURIER|"STD"|"EXP"`. Fora das casas, só aparecem os registros de `App.java:39-42`. Nenhuma linha específica de um caso escapa.
Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** Por consulta: `shippingOptions.get(request.shippingMethod())` (`src/main/java/shop/checkout/CheckoutCalculator.java:48`), sobre o mapa montado por `byCode` (`App.java:38-42`, `:57-59`). Depois a conta só chama `shipping.quote(...)` (`CheckoutCalculator.java:61`). O `if (shipping == null)` em `:49-51` só recusa.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe com a conta e a condição:
- `WELCOME10`: o desconto de 10% e a exigência de primeira compra (`src/main/java/shop/coupon/Welcome10Coupon.java:16-19`).
- `MINUS50`: o mínimo de R$ 300,00 e os R$ 50,00 de desconto (`src/main/java/shop/coupon/Minus50Coupon.java:9`, `:18-21`).
- `FREESHIPPING`: a recusa sem frete e o desconto igual ao frete (`src/main/java/shop/coupon/FreeShippingCoupon.java:15-18`).
- `BUY3PAY2`: uma unidade grátis a cada 3, com zero quando não há 3 de nenhum produto (`src/main/java/shop/coupon/Buy3Pay2Coupon.java:16-20`).

O teto de 30% é aplicado a todos no cálculo (`CheckoutCalculator.java:74-75`), uma regra comum que não cita cupom. O `INVALID_COUPON` vem da consulta que não achou nada (`:68-71`), também genérico. A busca pelos quatro códigos só acha os `code()` de cada classe.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** Por consulta: `coupons.get(request.coupon())` (`CheckoutCalculator.java:68`), sobre o mapa de `App.java:43-44`. A conta é `coupon.discount(...)` (`:72`), e nenhum `if` cita um cupom.
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Cada forma tem a sua classe com o ajuste, as parcelas e a recusa:
- `PIX`: 5% de desconto quando não há cupom e só à vista (`src/main/java/shop/payment/PixPayment.java:14-16`, `:25-27`).
- `CREDIT_CARD`: de 1 a 12 parcelas, sem juros até 3x e tabela Price acima disso (`src/main/java/shop/payment/CreditCardPayment.java:9-11`, `:19-21`, `:29-38`).
- `BANK_SLIP`: tarifa de R$ 3,49, só à vista e recusa acima de R$ 1.000,00 (`src/main/java/shop/payment/BankSlipPayment.java:8-9`, `:17-19`, `:22-24`, `:27-29`).

O cálculo só pergunta `acceptsInstallments`, `availableFor` e `charge` (`CheckoutCalculator.java:82-89`) e escolhe o código de erro sem citar forma nenhuma. Uma ressalva pequena: o parâmetro `couponInformed` da assinatura comum (`src/main/java/shop/payment/PaymentOption.java:13`) só existe para o Pix. É uma função genérica que não cita caso, então não conta como código do Pix fora da casa dele.
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por consulta: `paymentOptions.get(request.paymentMethod())` (`CheckoutCalculator.java:78`), sobre o mapa de `App.java:45-46`. O `if (payment == null)` (`:79-81`) só recusa.
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum que só carrega dados. Cada constante leva a taxa de crédito, a isenção de frete e o limite do brinde (`src/main/java/shop/checkout/ClubTier.java:9-11`). Os métodos são comuns a todos: `nextPurchaseCredit`, `waivesShipping` e `gift` (`:23-33`). Hoje os três níveis diferem só nesses dados, o que a régua aceita como acerto. Busquei `BRONZE|SILVER|GOLD` e também `credit`, `waivesShipping` e `gift` fora do enum. Achei só chamadas genéricas: `tier.waivesShipping()` (`CheckoutCalculator.java:62`), `tier.nextPurchaseCredit(subtotal)` e `tier.gift(subtotal)` (`:93`). O crédito no saldo vem do resumo, sem perguntar o nível (`src/main/java/shop/order/CreditReaction.java:8`, `:13`). Nenhuma linha nomeia um nível nem compara pela ordem.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por consulta: o `parse` genérico percorre `getEnumConstants()` e compara `name()` (`CheckoutCalculator.java:43`, `:131-140`). É um laço que não cita nível nenhum, equivalente ao `valueOf`.
Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está dividida entre as duas casas e nenhuma cita a outra. O nível diz que isenta (`ClubTier.java:11`, `:27-29`). O motoboy diz que não aceita a isenção do clube (`CourierShipping.java:25-27`, sobrescrevendo o `default` de `src/main/java/shop/shipping/ShippingOption.java:16-18`). O cálculo só combina as duas respostas: `tier.waivesShipping() && shipping.acceptsClubFreeShipping()` (`CheckoutCalculator.java:62-63`). Não há `if (tier == GOLD && method != COURIER)`: um nível novo com frete grátis ou uma entrega nova terceirizada respondem sozinhos. Ressalva menor: a combinação mora no cálculo geral, e não numa política de frete própria, e a interface de entrega ganhou um método que só existe por causa do clube. Como nenhuma das duas linhas nomeia caso, não é remendo.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra está no cupom: `if (!context.hasShipping()) throw COUPON_NOT_APPLICABLE` (`FreeShippingCoupon.java:15-17`). O `hasShipping` vem da entrega (`StorePickupShipping.java:17-19`), e o cálculo só o repassa (`CheckoutCalculator.java:72`). O cupom não cita `STORE_PICKUP`, nem o serviço cita `FREESHIPPING`.
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra está no Pix, que recebe o contexto: `couponInformed ? ZERO : 5%` (`PixPayment.java:25`). O cálculo passa `request.coupon() != null` a qualquer forma de pagamento (`CheckoutCalculator.java:89`), sem perguntar se é Pix. Isso também cobre o cupom com desconto zero, porque o que conta é o cupom ter sido informado.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A regra está no cupom: `if (!context.firstPurchase().getAsBoolean()) throw COUPON_NOT_APPLICABLE` (`Welcome10Coupon.java:16-18`). A consulta aos pedidos entra no contexto como um `BooleanSupplier` preguiçoso (`src/main/java/shop/coupon/CouponContext.java:14`; `CheckoutCalculator.java:72-73`). Ela é montada no cálculo para todos os cupons e não cita o WELCOME10, então é uma função genérica que só um caso usa e não conta como código fora da casa. A busca no controller (`src/main/java/shop/web/Routes.java`) e no `OrderService` não acha checagem de histórico.
Conclusão: acerto

**P5-forma, a região: que forma a variação tem.** Um enum que só carrega números: a porcentagem, o limite e os dias a mais de cada região (`src/main/java/shop/checkout/Region.java:9-13`). Há um método só para todas no seguro (`:25-28`) e outro nos dias (`:30-32`), e nenhuma constante tem corpo próprio.
Conclusão: contenção

**P5-proporcao, a região: a resposta tem o tamanho do problema.** Sim. A variação é dado puro, com uma conta única para as cinco regiões, sem classe nem corpo por região. Quem usa só pergunta `region.insurance(subtotal)` (`CheckoutCalculator.java:64`) e `region().extraDispatchDays()` (`CarrierShipping.java:23`).
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Strategy não achei estrutura que nenhum ponto peça:
- `ShippingContext`, `CouponContext`, `ShippingQuote` e `PaymentCharge` são records de parâmetro ou de retorno, não camadas.
- Os mapas montados em `App.java:57-59` são a própria seleção por consulta.
- `CarrierGateway`, com uma única implementação de produção (`TransLogGateway`), responde ao "pode ser que um dia a gente troque de transportadora" do enunciado, que é ponto do adaptador e não deste pattern.
- A lista de `PaymentConfirmationReaction` (`App.java:50`) responde ao SMS e ao cashback anunciados.
- `Store`/`Tx` têm uma segunda implementação nos testes (`src/test/java/shop/InMemoryStore.java`), então algo os substitui.

Não há fábrica, builder nem camada que só repasse.
Conclusão: contenção

## O que mudaria primeiro

1. **Tirar do cálculo a combinação do frete do clube.** Hoje são três coisas espalhadas: `acceptsClubFreeShipping()` na interface de entrega, `waivesShipping()` no nível e o `&&` em `CheckoutCalculator.java:62`. Eu juntaria tudo num método do próprio nível, por exemplo `tier.shippingCost(quote, shipping)`, ou numa política de frete. Um nível novo com outra vantagem de frete (meio frete, frete grátis só acima de um valor) mexeria num lugar só, e a interface de entrega deixaria de conhecer o clube.
2. **Trocar o `boolean couponInformed` de `PaymentOption.charge` por um contexto do pagamento**, um record com o total, as parcelas e o cupom. Uma forma nova com outra condição cruzada não obrigaria a mudar a assinatura das três implementações.

## Dúvidas

- **ECONOMY e EXPRESS na mesma classe `CarrierShipping`.** Li como uma casa parametrizada por dado, como o enum que só carrega dados: os dois diferem só no código do serviço da transportadora, e o código e o serviço entram juntos no registro (`App.java:39-40`). Não contei o `"STD"`/`"EXP"` em `App.java` como código de caso fora da casa: é o próprio registro do caso, e a tradução para a transportadora é trabalho do adaptador.
- **R1 dividida entre o nível e a entrega.** A régua sugere que a regra more "no nível (ou numa política de frete)". Aqui a exceção do motoboy está na entrega, por uma capacidade que não cita o GOLD, e é combinada no cálculo sem citar caso. Contei como acerto porque nada nomeia caso fora da casa, e deixei a ressalva no texto. Uma leitura mais estrita poderia chamar o `&&` no cálculo de regra cruzada fora das casas, mas ele não pergunta qual é o caso concreto, então não o tratei como malfeito.

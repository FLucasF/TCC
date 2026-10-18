# Padrões: Strategy

## Visão geral

O serviço já nasceu com o Strategy nos quatro pontos que pedem o padrão. A entrega, o cupom e a forma de pagamento têm cada um uma interface (`ShippingMethod`, `Coupon`, `PaymentMethod`). Cada caso tem a sua implementação, e um registro (`Map` por código) devolve o caso certo. O nível do clube é um `enum` que carrega só dados: a taxa do crédito, se isenta o frete e o piso do brinde. Os três níveis diferem só nesses dados e usam os mesmos métodos.

O cálculo (`PricingService.calculate`) não nomeia nenhum caso. Uma busca pelos códigos em `src/main` só os acha nas casas e nos registros. As quatro regras cruzadas ficam na casa do caso ou viram uma pergunta genérica, sem `if` que nomeie casos.

A região, que é o controle negativo, ficou como dado: um enum com os números e um método só para todas. Não há problema sério. Há dois pontos menores. A isenção de frete do GOLD é decidida no cálculo, combinando duas propriedades genéricas (um nível com outro tipo de vantagem no frete obrigaria a mexer ali). E o contexto do cupom traz uma pergunta pronta ("é a primeira compra?") feita sob medida para o WELCOME10.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** O código de cada caso fica na casa dele:
- **COURIER:** classe `CourierShippingMethod` (`src/main/java/shop/Shipping.java:85-100`). Lá estão o limite de 5 kg (`:86`, `:95-97`), o valor de R$ 18,00 com prazo 0 (`:98`) e o fato de o clube não poder isentar o frete (`:92`).
- **STORE_PICKUP:** classe `StorePickupShippingMethod` (`Shipping.java:73-83`), com frete zero e prazo de 1 dia (`:81`). Ela também declara que não cobra frete (`:76`).
- **ECONOMY e EXPRESS:** compartilham `CarrierShippingMethod` (`Shipping.java:43-71`), e cada um é uma instância própria configurada só com o código de serviço (`:111-112`). Os dois diferem apenas nesse dado, então a instância configurada funciona como casa.

Os dias a mais do Norte e do Nordeste entram em `Shipping.java:68` como `ctx.region().carrierExtraDays()`. É uma pergunta genérica à região, que não nomeia nenhuma entrega nem região. Fora das casas, os códigos só aparecem como chave de registro (`Shipping.java:111-114`). A tradução `ECONOMY -> "STD"` / `EXPRESS -> "EXP"` está nesse registro, e não no `TransLogGateway`. Mesmo assim ela não espalha: é o dado que configura cada instância, e não há condicional sobre o caso. Nenhuma linha em `PricingService`, `Api` ou `OrderService` cita uma entrega.
Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** A escolha é por consulta: `ShippingMethods.get` faz `byCode.get(code)` (`Shipping.java:117-119`). `PricingService.java:36-37` só recusa quando não acha o código. Depois disso, o cálculo chama `method.quote(...)` (`PricingService.java:47`) sem perguntar qual é o caso.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom é uma classe aninhada em `Coupons.java`, e a regra de quem pode usá-lo fica dentro dela:
- **WELCOME10:** classe aninhada (`:54-61`), com a condição de primeira compra em `:58`.
- **MINUS50:** classe aninhada (`:63-70`), com o mínimo de R$ 300,00 em `:67`.
- **FREESHIPPING:** classe aninhada (`:72-79`), com a recusa na retirada em `:76`.
- **BUY3PAY2:** classe aninhada (`:81-91`).

O teto de 30% é genérico e vale para todos (`Coupons.java:32`, `:48-52`). Em `PricingService.java:50-57` não há nenhum código de cupom.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** Por consulta: `Coupons.get` faz `byCode.get(code)` (`Coupons.java:44-46`). `PricingService.java:53` só recusa o código desconhecido, e `:56` chama `coupons.discountFor(coupon, ctx)`, que delega para `coupon.discount(ctx)` (`Coupons.java:49`).
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Cada forma tem a sua classe, com as próprias parcelas, recusas e ajuste:
- **Pix** (`Payments.java:37-48`): só à vista (`:40`) e 5% de desconto quando não há cupom (`:45`).
- **Boleto** (`Payments.java:50-64`): só à vista (`:56`), recusa acima de R$ 1.000,00 (`:52`, `:58`) e tarifa de R$ 3,49 (`:51`, `:61`).
- **Cartão** (`Payments.java:66-88`): de 1 a 12 parcelas (`:73`), sem juros até 3x e tabela Price a partir de 4x (`:77-87`).

O serviço só pergunta, sem nomear casos: `acceptsInstallments`, `availableFor` e `settle` (`PricingService.java:64-66`).
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por consulta: `PaymentMethods.get` faz `byCode.get(code)` (`Payments.java:33-35`). `PricingService.java:62-63` só recusa o código desconhecido.
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** `ClubTier` é um enum de dados. A taxa do crédito, a isenção do frete e o piso do brinde ficam nas constantes (`ClubTier.java:7-9`). Os métodos são únicos e não nomeiam nível: `credit` (`:21-23`), `waivesShipping` (`:25-27`) e `gift` (`:29-31`).

Busquei os nomes dos níveis e também o que eles fazem (`credit`, `waivesShipping`, `gift`). Fora do enum, eles só aparecem como chamadas genéricas: `PricingService.java:48` (o frete) e `:71` (o crédito e o brinde). Nenhuma linha compara com `GOLD` ou usa `ordinal`. Os níveis hoje diferem só nos dados, então um enum de dados é suficiente.

Há um limite. A forma da vantagem no frete ("isenta tudo ou nada") é um booleano decidido em `PricingService.java:48`. Um nível futuro com uma vantagem de outro tipo, como metade do frete, mudaria o cálculo, e não só o enum. Isso não muda a conclusão hoje.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por consulta: `ClubTier.parse` faz `valueOf(code)` (`ClubTier.java:33-40`), num enum que carrega os dados. `PricingService.java:29-30` só recusa o nível inválido.
Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra ficou dividida em duas propriedades sem nome de caso. O nível declara que isenta o frete (`ClubTier.java:9`, `:25-27`), e o motoboy declara que o clube não pode isentá-lo (`Shipping.java:92`, `waivableByClub() { return false; }`). O cálculo só combina as duas: `tier.waivesShipping() && method.waivableByClub()` (`PricingService.java:48`). Essa linha não muda se entrar um nível ou uma entrega parecidos, porque não nomeia `GOLD` nem `COURIER`.

A combinação está no cálculo, e não numa política de frete ou no próprio nível. Mesmo assim, é uma pergunta genérica, não o remendo `if (tier == GOLD && method != COURIER)`.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra está na casa do cupom: `if (!ctx.shippingMethod().chargesShipping()) throw Coupon.notApplicable()` (`Coupons.java:76`). Ela nem cita `STORE_PICKUP`. Pergunta à entrega se ela cobra frete, e a retirada responde que não (`Shipping.java:76`). Não há checagem no serviço.
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra está na casa do Pix: `couponUsed ? total : total.subtract(...)` (`Payments.java:45`). O serviço passa `r.couponPresent()` como contexto a qualquer forma de pagamento, sem perguntar qual é (`PricingService.java:66`). O parâmetro `couponUsed` está no contrato de todas as formas e só o Pix o usa. É uma função genérica que não nomeia o caso, então não conta como código do Pix fora da casa dele.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A decisão está na casa do cupom: `if (!ctx.firstPurchase().getAsBoolean()) throw Coupon.notApplicable()` (`Coupons.java:58`). O serviço só monta o contexto com uma consulta adiada, `() -> !tx.hasFulfilledOrder(r.cpf())` (`PricingService.java:54-55`). Essa consulta só roda se o cupom perguntar, e o serviço não checa o histórico nem recusa o pedido.

Uma ressalva: o contexto (`CouponContext`, `Coupons.java:15-16`) traz uma pergunta feita sob medida para o WELCOME10, em vez de dar ao cupom acesso aos pedidos. A definição de "primeira compra" fica, portanto, no montador do contexto. A régua não conta isso como remendo (ver Dúvidas).
Conclusão: acerto

**P5-forma, a região.** É um enum que só carrega números: a porcentagem, o limite do seguro e os dias a mais (`Region.java:6-10`). Um único método `insurance` serve todas as regiões (`:22-25`), e `carrierExtraDays` é só um getter (`:28-30`). Não há corpo por constante, `switch` ou classe por região.
Conclusão: contenção

**P5-proporcao, a região.** A variação foi tratada como dado, do tamanho do problema. Os dias a mais são aplicados por uma conta genérica dentro da entrega por transportadora (`Shipping.java:68`), sem nomear `NORTH` ou `NORTHEAST`. A seleção é por `valueOf` (`Region.java:32-39`).
Conclusão: contenção

**E, estrutura sem necessidade perto do Strategy.** Não achei nenhuma:
- Os registros (`ShippingMethods`, `Coupons`, `PaymentMethods`) são a própria seleção por consulta dos pontos P1 a P3.
- Os records de contexto (`ShippingContext`, `CouponContext`, `PaymentOutcome`) são objetos de parâmetro usados pelas estratégias.

Há outras duas abstrações com uma implementação de produção, mas elas são de outros patterns e têm motivo no enunciado:
- `CarrierGateway`, implementada só por `TransLogGateway`, é a fronteira com a transportadora ("pode ser que um dia a gente troque").
- `Store`, com `JdbcStore` e um `MemoryStore` usado nos testes (`src/test/java/shop/TestApp.java:25`), se explica pelo `mvn verify` sem banco.

Conclusão: contenção

## O que mudaria primeiro

1. **Levar a vantagem de frete para dentro do nível.** Por exemplo, `tier.shippingCost(quote, method)` no lugar do booleano combinado em `PricingService.java:48`. Hoje a isenção total é a única forma possível. O enunciado avisa que "cada [nível] vai ter seu conjunto de vantagens", e um desconto parcial de frete obrigaria a mexer no cálculo.
2. **Dar ao cupom acesso ao histórico do cliente** (o CPF e uma consulta aos pedidos) em vez da pergunta pronta `firstPurchase`. Assim, um cupom novo que dependa do histórico, como "a partir da terceira compra", não exige mudar `CouponContext` e `PricingService`.

## Dúvidas

- **R4:** o cupom decide, mas o significado de "primeira compra" (nenhum pedido pago, enviado ou entregue) é montado no serviço (`PricingService.java:55`) e no `Store` (`hasFulfilledOrder`). Li isso como contexto entregue ao cupom, que a régua aceita ("no cupom, que consulta os pedidos"), e não como a checagem no serviço. O serviço não toma a decisão nem recusa o pedido, e a consulta só roda quando o WELCOME10 pergunta.
- **ECONOMY e EXPRESS na mesma classe:** tratei cada instância de `CarrierShippingMethod`, configurada com o seu código de serviço, como a casa de cada caso. Os dois diferem só nesse dado, que é o mesmo raciocínio do enum de dados.

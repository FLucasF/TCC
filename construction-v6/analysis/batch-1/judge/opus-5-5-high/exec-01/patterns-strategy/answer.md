# padrões: Strategy

## Visão geral

O agente montou os quatro pontos (entrega, cupom, pagamento, clube) como Strategy desde o começo. Cada um tem uma interface: `ShippingOption`, `Coupon`, `PaymentOption` e `ClubTier`. Cada caso tem a sua classe, ou uma instância própria quando os casos diferem só em dados. As implementações são beans do Spring, e `CheckoutService` as indexa por `code()` num `Map` e escolhe o caso por consulta, sem nomear nenhum. As quatro regras cruzadas ficam na casa do par, ou são combinadas no serviço por propriedades genéricas que não nomeiam caso nenhum:
- `waivesFreight()` / `freightWaivableByClub()`;
- `chargesFreight()`;
- `couponInformed()`;
- `firstPurchase()`.

A região é um enum que só carrega números, com um método só para todas (contenção). Não achei problema sério. Ficam três ressalvas pequenas:
- o histórico do cliente (a primeira compra) é consultado pelo serviço e entregue pronto ao cupom, em vez de o cupom consultar;
- a exceção do motoboy na regra do GOLD está dividida entre o nível e a entrega, e quem junta as duas é o serviço;
- há uma linha inútil no gateway da TransLog.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.** Cada caso tem a sua casa:
- `COURIER`: `shipping/CourierShipping.java`, com o preço de R$ 18,00, o prazo 0 e o limite de 5 kg (`:14`, `:23-26`). A exceção do clube também mora ali (`:30`, `freightWaivableByClub() = false`).
- `STORE_PICKUP`: `shipping/StorePickupShipping.java`, com frete zero, prazo 1 e `chargesFreight() = false` (`:17`, `:21`).
- `ECONOMY` e `EXPRESS`: dividem a classe `shipping/CarrierShipping.java` e diferem só num dado, o código do serviço na TransLog. Cada um é um bean próprio em `shipping/ShippingConfig.java:11` e `:16`. É o caso de "casos que diferem só nos dados". Os dias a mais do Norte e do Nordeste entram ali de forma genérica (`CarrierShipping.java:31`, `request.region().carrierExtraDays()`).

Busquei os nomes dos casos e o que eles fazem (`chargesFreight`, `freightWaivable`, `weightKg`) fora de `shipping/`. Nenhuma linha faz coisa específica de um caso. Uma ressalva fica à parte: `shipping/TransLogGateway.java:35` traz `service.equals("STD") ? "STD" : "EXP"`. A linha não faz nada (repassa o código, e qualquer outro valor vira `EXP`), mas fica no lado da TransLog e cita os códigos dela, não os casos da entrega, então não espalha.
Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** Por consulta. `CheckoutService.java:48` indexa a `List<ShippingOption>` por `code()`, e `:64` busca o `shippingMethod` no mapa (`find`, `:130-136`). Quando não acha, recusa com `INVALID_SHIPPING_METHOD`. A conta é `shipping.quote(...)` (`:70`), sem nomear nenhum caso.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem uma classe, com a sua conta e a sua condição:
- `coupon/Welcome10Coupon.java:21-24`: a primeira compra e os 10%;
- `coupon/Minus50Coupon.java:23-26`: o mínimo de R$ 300,00 e os R$ 50,00;
- `coupon/FreeShippingCoupon.java:20-23`: a recusa sem frete e o desconto igual ao frete;
- `coupon/Buy3Pay2Coupon.java:21-24`: uma unidade grátis a cada 3.

O código `COUPON_NOT_APPLICABLE` é lançado dentro de cada casa. No serviço ficam só regras comuns a todos os cupons: o teto de 30% e o arredondamento (`CheckoutService.java:35`, `:79`). A busca por `WELCOME10|MINUS50|FREESHIPPING|BUY3PAY2` só acha os `code()` das próprias classes.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** Por consulta: `CheckoutService.java:49` monta o mapa e `:76` faz `find(coupons, request.coupon(), INVALID_COUPON)`. O texto vazio cai no `find` e vira `INVALID_COUPON`. O `if (request.coupon() != null)` (`:75`) só decide se há cupom e não nomeia caso nenhum. A conta é `coupon.discount(...)` (`:78`).
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Cada forma tem a sua classe:
- `payment/PixPayment.java:18`: o desconto de 5% e a condição sem cupom;
- `payment/CreditCardPayment.java:27-45`: de 1 a 12 parcelas, até 3x sem juros e a tabela Price;
- `payment/BankSlipPayment.java:23-33`: o limite de R$ 1.000,00 com `PAYMENT_METHOD_UNAVAILABLE` e a tarifa de R$ 3,49.

A regra "só 1 parcela" do Pix e do boleto mora numa classe-base comum, `CashPayment` (`payment/CashPayments.java:7-14`). Ela não cita caso, então não conta como código de caso fora da casa. O serviço só chama `validateInstallments`, `validateAvailability` e `charge` (`CheckoutService.java:84`, `:88`, `:89`), e o ajuste é sempre a mesma conta genérica (`:92`).
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por consulta: o mapa em `CheckoutService.java:51` e o `find(paymentOptions, …, INVALID_PAYMENT_METHOD)` em `:82`.
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** A interface `club/ClubTier.java` tem `credit`, `waivesFreight` e `gift`, e cada nível tem a sua classe:
- `club/BronzeTier.java:16-26`: tudo zero ou falso;
- `club/SilverTier.java:17`: os 2%;
- `club/GoldTier.java:19-27`: os 5%, frete grátis e brinde acima de R$ 500,00, com o limite do brinde em `:12`.

Procurei `credit`, `gift`, `shipping` e `waives` fora de `club/`. O serviço só pergunta ao nível: `tier.waivesFreight()` em `CheckoutService.java:71`, e `tier.credit(subtotal)` e `tier.gift(subtotal)` em `:93`. O saldo e o estorno usam o `nextPurchaseCredit` do resumo gravado (`order/OrderRepository.java:42`), sem citar nível. Nenhuma linha fora das casas faz algo só para um nível.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por consulta: o mapa em `CheckoutService.java:50` e o `find(tiers, request.clubTier(), INVALID_CLUB_TIER)` em `:58`.
Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra está dividida em duas casas e combinada sem nomear caso. O GOLD diz que dispensa o frete (`GoldTier.java:22-24`). O motoboy diz que o frete dele não pode ser dispensado pelo clube (`CourierShipping.java:29-32`; o padrão `true` fica em `ShippingOption.java:22-24`). O serviço junta as duas em `CheckoutService.java:71`: `tier.waivesFreight() && shipping.freightWaivableByClub()`. Nenhum dos dois casos é nomeado fora da casa dele: a exceção do motoboy mora no motoboy, que é "o inverso" que a régua aceita. Um ponto fraco de desenho: quem compõe a política é o serviço, e um nível futuro com desconto parcial de frete obrigaria a mudar a interface. Isso, porém, não é um `if` sobre os casos.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra mora no cupom: `FreeShippingCoupon.java:20-21` recusa quando `!context.shipping().chargesFreight()`. A retirada nem é nomeada: ela responde `chargesFreight() = false` (`StorePickupShipping.java:21`), e uma futura entrega sem frete seria recusada sozinha. Nada no serviço ou no controller trata desse par.
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra mora no Pix, que recebe o contexto: `PixPayment.java:18` usa `context.couponInformed()`. O serviço só preenche o campo de forma genérica (`CheckoutService.java:89`, `request.coupon() != null`), sem nomear o Pix. Como o teste é `!= null`, um cupom informado com desconto zero também tira o desconto, como o enunciado pede.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A decisão e a recusa moram no cupom: `Welcome10Coupon.java:21-22`. A consulta ao histórico, porém, é feita pelo serviço: `CheckoutService.java:77` chama `orders.hasPurchaseByCustomer(cpf)` (`order/OrderRepository.java:102-107`) para todo cupom e passa um `firstPurchase` pronto no `CouponContext`. A régua prefere o cupom que consulta os pedidos. Mesmo assim, a linha `:77` não nomeia o WELCOME10 nem recusa nada. É um fato genérico do cliente, calculado para qualquer cupom, e cai em "função genérica que não cita o caso, mesmo que só um caso a use". O custo é de desenho: cada cupom faz uma consulta ao banco que só um deles usa, e um cupom novo que precise de outro fato do histórico obriga a mexer no `CouponContext` e no serviço. Não é o remendo que a régua descreve, a checagem do histórico no serviço. Ver em Dúvidas.
Conclusão: acerto

**P5-forma, a região.** Um enum que só carrega dados: `shipping/Region.java:10-14` guarda a porcentagem, o limite e os dias a mais. Há um método só para todas as regiões (`insuranceFor`, `:26-29`) e um acesso aos dias (`:31-33`). A tradução do texto para a região é um laço genérico (`:35-42`). Nenhuma constante tem corpo próprio.
Conclusão: contenção

**P5-proporcao, a região.** A resposta tem o tamanho do problema: as regiões diferem só em números, e é isso que o enum guarda. Não há classe nem comportamento por região.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto deste pattern não achei estrutura sem necessidade:
- `CashPayment` (`payment/CashPayments.java:7`) é uma classe-base com dois filhos, que tira a duplicação da regra de 1 parcela;
- `CouponContext` e `PaymentContext` são só os dados passados às estratégias;
- `ShippingConfig` só registra os dois beans da TransLog;
- `CarrierGateway`, com uma implementação só, responde ao "pode ser que um dia a gente troque de transportadora", que é ponto de outro pattern (o Adapter);
- `OrderEventListener` responde ao SMS e ao cashback futuros, também ponto de outro pattern.

Sobra só a linha sem efeito de `TransLogGateway.java:35`, que é ruído e não estrutura.
Conclusão: contenção

## O que mudaria primeiro

1. **Levar a consulta do histórico para dentro do WELCOME10.** O cupom receberia o CPF no contexto e consultaria os pedidos ele mesmo, em vez de o serviço calcular `firstPurchase` para todo cupom (`CheckoutService.java:77`). Isso tira uma consulta ao banco que só um cupom usa, e os cupons novos que dependam do histórico deixam de exigir mudança no serviço e no `CouponContext`.
2. **Tirar a composição da regra de frete do serviço.** Em vez de `tier.waivesFreight() && shipping.freightWaivableByClub()` (`CheckoutService.java:71`), o nível receberia a entrega e a cotação e devolveria o frete que cobra (`tier.shippingCost(shipping, quote)`). A vantagem do nível ficaria toda na casa dele, e um nível com meio frete não exigiria mudar a interface. Aproveitaria para apagar a linha `TransLogGateway.java:35`.

## Dúvidas

- **R4.** A régua aponta o cupom que consulta os pedidos como o ideal e a checagem do histórico no serviço como o remendo. Aqui o serviço consulta, mas quem decide e recusa é o cupom, e a linha do serviço não nomeia nenhum caso. Li isso como acerto, pela regra de que uma função genérica que não cita o caso não conta como código dele, e registrei o custo de desenho no texto. Quem ler "checagem" como a simples consulta concluiria malfeito.
- **ECONOMY e EXPRESS.** Os dois dividem a classe `CarrierShipping`, com uma instância cada em `ShippingConfig`. Tratei cada bean como a casa do seu caso, porque os dois diferem só no código do serviço da TransLog. É o caso do "enum que só carrega dados" aplicado a instâncias de uma classe.

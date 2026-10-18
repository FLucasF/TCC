# Padrões: Strategy

## Visão geral

O agente montou o Strategy desde o começo nos quatro pontos que pedem o padrão. Entrega, cupom, forma de pagamento e nível do clube têm cada um uma interface (`ShippingOption`, `Coupon`, `PaymentMethod`, `ClubTier`) e uma classe por caso. O `CheckoutService` acha o caso por consulta: monta mapas com `Coded.index`, a partir das listas de beans que o Spring injeta, e depois chama métodos que não nomeiam nenhum caso. As quatro regras cruzadas ficaram na casa do caso, nunca no serviço. O GOLD pergunta à entrega se o frete grátis do clube vale nela. O FREESHIPPING pergunta se a entrega cobra frete. O Pix recebe no contexto se veio cupom. O WELCOME10 recebe no contexto a consulta da primeira compra. Fora das casas, os nomes dos casos só aparecem como código de registro (`code()`) e na tradução ECONOMY/EXPRESS → STD/EXP de `ShippingConfig.java`. A região, que é o controle negativo, é um enum só com os números e um método para todas. Não achei problema sério. Há só um ponto discutível. A exceção do motoboy é dita por uma flag com nome do clube na interface da entrega (`clubFreeShippingApplies`). Isso deixa o conceito do clube dentro da entrega, mas não nomeia caso nenhum.

## Item a item

**P1-local, a entrega.** Cada forma de entrega tem casa própria. O motoboy está em `shipping/CourierShipping.java`: o limite de 5 kg (`:13`, `:23-25`), o preço de R$ 18,00 e o prazo 0 (`:26`). A retirada está em `shipping/StorePickupShipping.java`: frete zero e 1 dia (`:17`). A econômica e a expressa dividem `shipping/TransLogShipping.java` e só diferem no código de serviço da TransLog, que entra pelo construtor (`shipping/ShippingConfig.java:11`, `:16`). Os dois casos diferem só num dado, então dividir a casa é adequado. Os dias a mais do Norte e do Nordeste ficam dentro da casa da TransLog (`TransLogShipping.java:33`) e vêm do dado da região (`region().carrierExtraDays()`), sem nomear região nenhuma. Fora das casas, os nomes só aparecem como registro e como tradução para o código da TransLog (`ShippingConfig.java:11`, `:16`), e nada disso conta. A busca pelos quatro nomes no código de produção não acha nenhum outro uso.
Conclusão: acerto

**P1-selecao, a entrega.** A escolha é por consulta: `shippingOptions.get(request.shippingMethod())` (`checkout/CheckoutService.java:66`), num mapa montado com as implementações injetadas (`:46`). O `if (shipping == null)` em `:67` só recusa o pedido. A cotação é pedida à opção sem perguntar qual ela é (`:73`).
Conclusão: acerto

**P2-local, o cupom.** Cada cupom tem a sua classe, com a conta e a condição de uso, e lança o próprio `COUPON_NOT_APPLICABLE`. O mínimo de R$ 300 está em `coupon/Minus50Coupon.java:23-24`. A primeira compra está em `coupon/Welcome10Coupon.java:22-23`. A recusa na retirada está em `coupon/FreeShippingCoupon.java:19-20`. A conta por produto está em `coupon/Buy3Pay2Coupon.java:20-23`. No serviço ficou só o que vale para todos: a falta de cupom (`CheckoutService.java:101-102`), o `INVALID_COUPON` (`:105-106`) e o teto de 30% (`:111-112`). Nenhuma linha nomeia um cupom. A busca por `WELCOME10|MINUS50|FREESHIPPING|BUY3PAY2` só acha os `code()` das classes.
Conclusão: acerto

**P2-selecao, o cupom.** A escolha é por consulta: `coupons.get(request.coupon())` (`CheckoutService.java:104`). O texto vazio cai naturalmente no `null` e vira `INVALID_COUPON`, sem caso especial. A conta é pedida ao cupom (`:109-110`).
Conclusão: acerto

**P3-local, a forma de pagamento.** Cada forma tem a sua classe, com as parcelas que aceita, a recusa e o ajuste. O Pix só aceita 1 parcela e dá o desconto de 5% (`payment/PixPayment.java:20-27`). O boleto só aceita 1 parcela, tem o limite de R$ 1.000 e a tarifa (`payment/BankSlipPayment.java:21-33`). O cartão aceita de 1 a 12 parcelas, sem juros até 3x e com a tabela Price acima disso (`payment/CreditCardPayment.java:24-41`). O serviço escolhe o código de erro sem saber qual é a forma (`CheckoutService.java:85-90`): `allowsInstallments` e `availableFor` são perguntas a qualquer forma, e `availableFor` tem um `default` que devolve `true` (`payment/PaymentMethod.java:13-15`).
Conclusão: acerto

**P3-selecao, a forma de pagamento.** A escolha é por consulta: `paymentMethods.get(request.paymentMethod())` (`CheckoutService.java:81`). O `if` em `:82` só recusa. Depois o serviço chama `payment.charge(...)` (`:91-92`) sem perguntar qual é a forma.
Conclusão: acerto

**P4-local, o nível do clube.** As vantagens moram em cada nível. O SILVER tem o crédito de 2% (`club/SilverTier.java:20-22`). O GOLD tem o crédito de 5%, o frete grátis e o brinde acima de R$ 500 (`club/GoldTier.java:21-34`). O BRONZE não sobrescreve nada (`club/BronzeTier.java`) e herda os `default` de "nenhuma vantagem" (`club/ClubTier.java:12-22`), que são genéricos e não contam. Também busquei pelo que os níveis fazem (`credit`, `gift`, `freeShipping`) fora de `club/`. Só aparecem as chamadas genéricas `tier.freeShipping(shipping)` (`CheckoutService.java:74`) e `tier.nextPurchaseCredit(subtotal)`/`tier.gift(subtotal)` (`:96`). O crédito no saldo é aplicado a partir do valor do resumo (`customer/ClubCreditListener.java:19`, `:25`), sem nomear nível.
Conclusão: acerto

**P4-selecao, o nível do clube.** A escolha é por consulta: `clubTiers.get(request.clubTier())` (`CheckoutService.java:55`). O `if` em `:56` só recusa.
Conclusão: acerto

**R1, o GOLD sem frete, exceto no motoboy.** A regra mora no nível: `GoldTier.freeShipping(shipping)` devolve `shipping.clubFreeShippingApplies()` (`club/GoldTier.java:27-29`). A exceção é declarada pelo motoboy, que sobrescreve a flag para `false` (`shipping/CourierShipping.java:30-32`). O serviço só aplica a resposta, sem nomear nível nem entrega (`CheckoutService.java:74`). Não há `if (tier == GOLD && method != COURIER)` em lugar nenhum. Há uma ressalva de desenho. O nome da flag fala do clube e ela está na interface da entrega (`shipping/ShippingOption.java:17-19`), então a entrega sabe que existe um "frete grátis do clube". Mas nenhuma linha nomeia um caso, e um nível novo com frete grátis reaproveitaria a mesma flag. A regra fica nas casas do par, não num terceiro lugar.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** A regra mora no cupom: `if (!context.shipping().chargesShipping())` (`coupon/FreeShippingCoupon.java:19-20`). A retirada é a única que declara `chargesShipping() == false` (`shipping/StorePickupShipping.java:21-23`). O cupom recebe a entrega pelo `CouponContext` (`coupon/CouponContext.java:10`) e nem nomeia `STORE_PICKUP`: pergunta uma capacidade. Nada disso aparece no serviço.
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** A regra mora no Pix: `context.couponInformed() ? Money.ZERO : ...` (`payment/PixPayment.java:26`). O serviço só preenche o contexto, igual para qualquer forma: `request.coupon() != null` (`CheckoutService.java:92`). Isso também atende ao "mesmo que o cupom tenha dado desconto zero", porque a pergunta é se o cupom foi informado, e não quanto ele descontou.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** A decisão mora no cupom: `if (!context.firstPurchase().getAsBoolean())` (`coupon/Welcome10Coupon.java:22-23`). A consulta ao histórico chega pelo contexto como um `BooleanSupplier` preguiçoso, montado no serviço: `() -> !orders.hasPurchase(cpf)` (`CheckoutService.java:110`). Essa linha não nomeia o cupom e é montada para qualquer cupom. Só o WELCOME10 a usa, mas a régua diz que uma função genérica que não cita o caso não conta. O cupom não chama o repositório diretamente, mas a regra e o momento da consulta são dele, e o controller não faz checagem nenhuma (`web/OrderController.java`).
Conclusão: acerto

**P5-forma, a região.** É um enum que só carrega os números: porcentagem, limite e dias a mais (`common/Region.java:6-10`). Um método único faz a conta do seguro para todas as regiões (`:31-34`), e um acessor devolve os dias a mais (`:37-39`). Nenhuma constante tem corpo próprio.
Conclusão: contenção

**P5-proporcao, a região.** A variação ficou como dado, que é do tamanho do problema. O enunciado só muda números por região, e uma região nova seria uma linha a mais no enum. Não há classe por região, constante com corpo nem `switch`. O `parse` (`Region.java:22-29`) é um laço genérico.
Conclusão: contenção

**E, estrutura sem necessidade perto do Strategy.** Não achei. `Coded` (`common/Coded.java`) é uma interface mínima com um utilitário de índice, usada pelos quatro mapas de seleção. `ShippingRequest`, `ShippingQuote`, `CouponContext` e `PaymentMethod.PaymentContext`/`PaymentCharge` são registros de parâmetros e de retorno das próprias estratégias. `ShippingConfig` só registra os dois beans da TransLog. A interface `TransLogClient`, que tem uma implementação só (`HttpTransLogClient`) e não é substituída nos testes, fica fora desta correção: ela responde ao ponto do Adapter ("pode ser que um dia a gente troque de transportadora").
Conclusão: contenção

## O que mudaria primeiro

1. Trocar o nome da flag `ShippingOption.clubFreeShippingApplies()` por algo que descreva a entrega e não o clube, como `outsourced()` ou `waivableFee()`. A entrega deixaria de conhecer o conceito do clube. Hoje funciona e não nomeia caso, mas cada nova vantagem do clube ligada à entrega tenderia a pôr mais uma flag "do clube" na interface da entrega.
2. Fora isso, não há mudança que melhore de verdade esta dimensão. O desenho já aguenta uma entrega, um cupom ou um nível novo sem mexer no `CheckoutService`.

## Dúvidas

- **ECONOMY e EXPRESS numa só classe.** Uma classe parametrizada (`TransLogShipping`) faz as vezes de casa dos dois. Considerei acerto porque os dois casos têm o mesmo comportamento e diferem só no código de serviço da TransLog, como na regra do enum que só carrega dados.
- **R1 e R4.** Nos dois, uma parte da regra é fornecida de fora da casa: a flag da entrega no R1, o `BooleanSupplier` do serviço no R4. Considerei que a regra mora na casa porque a decisão está no GOLD e no WELCOME10, e o que vem de fora é genérico e não nomeia caso nenhum.

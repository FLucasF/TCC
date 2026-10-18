# padrões: Strategy

## Visão geral

O agente montou os quatro pontos variáveis como estratégias desde o início. A entrega, o cupom e a forma de pagamento são interfaces (`ShippingOption`, `Coupon`, `PaymentMethod`) com uma implementação por caso. O nível do clube é um `enum` que só carrega dados, com um método só para todos os níveis. O `CheckoutService` acha cada caso por consulta num `Map` montado com os beans injetados e não nomeia nenhum caso no cálculo. As quatro regras cruzadas ficam na casa de um dos casos envolvidos ou viram uma pergunta genérica à estratégia: o motoboy diz que a vantagem de frete do clube não vale nele, o FREESHIPPING recusa a retirada, o Pix recebe se veio cupom e o WELCOME10 consulta os pedidos. A região, o controle negativo, é um enum só com números, que é a resposta do tamanho do problema.

Não achei problema sério, só ressalvas pequenas:
- `ECONOMY` e `EXPRESS` dividem a mesma classe e só mudam por um parâmetro do construtor;
- o FREESHIPPING compara o texto `"STORE_PICKUP"` em vez de perguntar algo à entrega. Isso fica na casa do cupom, que a régua aceita;
- o parâmetro `couponInformed` aparece na assinatura das três formas de pagamento, embora só o Pix o use.

## Item a item

**P1-local, a entrega: onde mora o código de cada caso.**
- **`COURIER`** tem casa própria em `src/main/java/br/com/loja/pedidos/shipping/CourierShipping.java`: o limite de 5 kg e a recusa (linhas 11 e 20-22), o preço e o prazo (linha 23) e a exceção do clube (linhas 27-30).
- **`STORE_PICKUP`** tem casa em `src/main/java/br/com/loja/pedidos/shipping/StorePickupShipping.java:15-17`: frete zero e 1 dia.
- **`ECONOMY` e `EXPRESS`** moram juntos em `src/main/java/br/com/loja/pedidos/shipping/CarrierShippingOption.java`. A cotação, a conversão para gramas, as horas viradas em dias e os dias a mais da região estão nas linhas 26-34. A recusa por CEP não atendido e a indisponibilidade da transportadora estão nas linhas 32-33. Os dois casos diferem só pelo `ServiceLevel` passado no construtor, em `src/main/java/br/com/loja/pedidos/shipping/ShippingConfig.java:11` e `:16`. É o "comportamento passado no construtor" que a régua aceita, e os dois nomes aparecem ali só como chave de registro.
- **O que aparece fora das casas não espalha.** Os dias a mais da região são lidos de forma genérica (`request.region().dispatchDelayDays()`, `CarrierShippingOption.java:31`). A tradução `level == ServiceLevel.EXPRESS ? "EXP" : "STD"` (`src/main/java/br/com/loja/pedidos/shipping/TranslogClient.java:43`) fica do lado da TransLog e é trabalho do adaptador.
- **A busca pelos nomes** (`ECONOMY|EXPRESS|STORE_PICKUP|COURIER`) em `src/main` não acha nenhuma linha que faça algo só para uma entrega fora dessas casas.

Conclusão: acerto

**P1-selecao, a entrega: como o caso é escolhido.** Por consulta. O construtor monta `Map<String, ShippingOption>` a partir de `List<ShippingOption>` injetada (`src/main/java/br/com/loja/pedidos/checkout/CheckoutService.java:33` e `:40`). O cálculo faz `shippingOptions.get(request.shippingMethod())` (`CheckoutService.java:53`) e chama `shipping.quote(...)` (`:61`). O `if (shipping == null)` das linhas 54-56 só recusa o pedido. Nenhuma condicional do cálculo nomeia uma entrega.
Conclusão: acerto

**P2-local, o cupom: onde mora o código de cada caso.** Cada cupom tem a sua classe, com a condição em `applicable` e a conta em `discount`:
- `Welcome10Coupon.java:25-32`: a primeira compra e os 10%;
- `Minus50Coupon.java:10` e `:18-25`: o mínimo de R$ 300,00 e os R$ 50,00;
- `FreeShippingCoupon.java:15-22`: não vale na retirada e o desconto é o frete;
- `Buy3Pay2Coupon.java:17-23`: uma unidade grátis a cada três.

Todos ficam em `src/main/java/br/com/loja/pedidos/coupon/`. O teto de 30% fica fora das casas, mas é uma regra de todos os cupons e não cita nenhum: `coupon.discount(context).min(Money.percent(subtotal, COUPON_CAP))`, em `CheckoutService.java:75`. A busca pelos quatro códigos em `src/main` só os acha nos métodos `code()` das próprias classes.
Conclusão: acerto

**P2-selecao, o cupom: como o caso é escolhido.** Por consulta: `Map<String, Coupon>` montado dos beans (`CheckoutService.java:34` e `:41`) e `coupons.get(request.coupon())` (`:66`). Os `if` das linhas 67-69 (`INVALID_COUPON`) e 72-74 (`COUPON_NOT_APPLICABLE`) só recusam o pedido. O segundo pergunta `coupon.applicable(context)` a qualquer cupom, sem nomear nenhum. A conta vem de `coupon.discount(context)` (`:75`).
Conclusão: acerto

**P3-local, a forma de pagamento: onde mora o código de cada caso.** Cada forma decide as suas parcelas (`allowsInstallments`), a sua recusa (`availableFor`) e o seu ajuste (`charge`):
- `PixPayment.java:18-25`: só à vista e 5% de desconto quando não veio cupom;
- `CreditCardPayment.java:12-14` e `:22-38`: de 1 a 12 vezes, sem juros até 3x e tabela Price acima disso;
- `BankSlipPayment.java:10-11` e `:18-31`: só à vista, limite de R$ 1.000,00 e tarifa de R$ 3,49.

Todas ficam em `src/main/java/br/com/loja/pedidos/payment/`. O limite do boleto usa o método `default` `availableFor` da interface (`PaymentMethod.java:13-15`), que é genérico e não cita o caso. Nenhuma outra linha de `src/main` cita `PIX`, `CREDIT_CARD` ou `BANK_SLIP`.
Conclusão: acerto

**P3-selecao, a forma de pagamento: como o caso é escolhido.** Por consulta: `paymentMethods.get(request.paymentMethod())` (`CheckoutService.java:85`), com o mapa montado dos beans (`:35` e `:42`). Os `if` das linhas 86-90 só recusam e perguntam à estratégia (`allowsInstallments`, `availableFor`). A conta é `payment.charge(...)` (`:91`).
Conclusão: acerto

**P4-local, o nível do clube: onde mora o código de cada caso.** `src/main/java/br/com/loja/pedidos/club/ClubTier.java:9-11` é um enum que só carrega dados de cada nível: a taxa do crédito, se o frete é grátis e o valor a partir do qual vai brinde. Os três métodos servem a todos os níveis (`nextPurchaseCredit`, `freeShipping`, `gift`, linhas 23-33). Os níveis só diferem nesses dados, o caso que a régua aceita para o enum de dados.

A busca por `credit`, `gift` e `freeShipping` fora do pacote `club` só acha usos genéricos:
- `CheckoutService.java:62` e `:95`;
- `src/main/java/br/com/loja/pedidos/order/CustomerCreditListener.java:17` e `:23`, que leem o crédito já gravado no resumo.

Nenhum desses usos nomeia um nível. `parse` (`ClubTier.java:35-42`) percorre `values()` sem citar nenhuma constante.

Uma observação: se um nível futuro trouxer uma vantagem de outro tipo, que não caiba num desses dados, o enum terá de ganhar outro campo. Hoje os três níveis cabem no modelo.
Conclusão: acerto

**P4-selecao, o nível do clube: como o caso é escolhido.** Por consulta: `ClubTier.parse(request.clubTier())` (`CheckoutService.java:48`) é um laço genérico sobre `values()`. A recusa `INVALID_CLUB_TIER` vem do `orElseThrow`. O cálculo pergunta ao nível sem nomeá-lo (`tier.freeShipping()`, `tier.nextPurchaseCredit(subtotal)`, `tier.gift(subtotal)`, `CheckoutService.java:62` e `:95`).
Conclusão: acerto

**R1, o GOLD não paga frete, exceto no motoboy.** A regra ficou dividida entre as duas estratégias, sem nomear ninguém num terceiro lugar:
- o nível diz se dá frete grátis (`ClubTier.java:11` e `:27-29`);
- a entrega diz se aceita essa vantagem, com o `default` `true` em `src/main/java/br/com/loja/pedidos/shipping/ShippingOption.java:12-14`, e o motoboy sobrescreve com `false` na casa dele (`CourierShipping.java:27-30`);
- o cálculo só combina as duas perguntas, `tier.freeShipping() && shipping.clubFreeShippingApplies() ? Money.ZERO : quote.cost()` (`CheckoutService.java:62`).

Não há `if (tier == GOLD && method != COURIER)`. A exceção mora na casa do `COURIER`, que é a casa do par.
Conclusão: acerto

**R2, o FREESHIPPING não vale na retirada.** Está na casa do cupom: `return !"STORE_PICKUP".equals(context.shippingMethod());` (`FreeShippingCoupon.java:15-17`). Citar a entrega dentro do cupom do par é onde a regra deve morar. Nada no `CheckoutService` testa o par (`CheckoutService.java:72-74` só pergunta `applicable`).
Conclusão: acerto

**R3, o desconto do Pix só sem cupom.** Está no Pix, que recebe o contexto: `couponInformed ? orderTotal : orderTotal.subtract(...)` (`PixPayment.java:24`). O serviço só repassa `request.couponInformed()` a qualquer forma de pagamento (`CheckoutService.java:91`), sem nomear o Pix.

O parâmetro faz parte da assinatura de `charge` para as três formas (`PaymentMethod.java:17`), e o cartão e o boleto o ignoram. É um contexto mais estreito do que um objeto de contexto, mas não tira a regra do lugar.
Conclusão: acerto

**R4, o WELCOME10 só na primeira compra.** Está no cupom, que consulta os pedidos: `Welcome10Coupon` recebe o `OrderRepository` no construtor e faz `!orders.hasPaidOrder(context.cpf())` em `applicable` (`Welcome10Coupon.java:13-17` e `:25-27`). Nem o controller nem o serviço checam o histórico.
Conclusão: acerto

**P5-forma, a região: que forma a variação tem.** É um enum que só carrega números: a porcentagem, o limite e os dias a mais (`src/main/java/br/com/loja/pedidos/checkout/Region.java:9-13`). Há um método só para todas as regiões, `insurance` (`:25-28`), e um acessor para os dias (`:30-32`). Nenhuma constante tem corpo próprio.
Conclusão: contenção

**P5-proporcao, a região: a resposta tem o tamanho do problema.** Tem. A variação ficou como dado: os números no enum, uma conta só e o limite tratado como valor que pode faltar (`null`), sem um caso especial por região. Os dias a mais são lidos de forma genérica em `CarrierShippingOption.java:31`. Não há classe, estratégia nem `switch` por região.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto do Strategy não achei estrutura que nenhum ponto peça:
- `CarrierGateway`, com uma implementação só (`TranslogClient`), responde ao "pode ser que um dia a gente troque de transportadora" do enunciado, e a TransLog é ponto de outro pattern;
- a lista de `OrderLifecycleListener` (`src/main/java/br/com/loja/pedidos/order/OrderLifecycleListener.java`) responde aos avisos futuros por SMS e cashback, também ponto de outro pattern;
- os records de entrada e saída das estratégias (`ShippingRequest`, `ShippingQuote`, `CouponContext`, `PaymentCharge`) são dados, não camadas;
- `ServiceLevel` (`ShippingConfig.java`, `ServiceLevel.java`) é um enum de dois valores que separa a nossa entrega do código da transportadora, sem comportamento.
Conclusão: contenção

## O que mudaria primeiro

1. **Tirar do FREESHIPPING o nome da retirada.** Hoje ele compara o texto `"STORE_PICKUP"` (`FreeShippingCoupon.java:16`). Ele poderia perguntar à entrega se ela cobra frete, por exemplo com um método da `ShippingOption` que a retirada responde com `false`. A régua aceita a forma atual, mas a próxima entrega sem frete (uma segunda loja, um ponto de coleta) obrigaria a mexer no cupom.
2. **Passar às formas de pagamento um objeto de contexto** em vez do `boolean couponInformed` (`PaymentMethod.java:17`). A próxima regra cruzada de pagamento, por exemplo uma que dependa do nível ou da entrega, entraria no contexto sem mudar a assinatura de todas as formas.

## Dúvidas

- **`ECONOMY` e `EXPRESS` na mesma classe.** Eles dividem `CarrierShippingOption` e diferem só pelo `ServiceLevel` passado no construtor (`ShippingConfig.java:11` e `:16`). Tratei isso como casa de cada caso pela regra do "comportamento passado no construtor", e as duas linhas de `ShippingConfig` como registro. Os dois têm hoje a mesma conta e mudam só no serviço pedido à transportadora.
- **A tradução `"EXP"`/`"STD"` no adaptador.** O ternário de `TranslogClient.java:43` manda para `"STD"` qualquer nível que não seja `EXPRESS`. Contei como trabalho do adaptador, conforme a régua, e não como espalhamento da entrega.

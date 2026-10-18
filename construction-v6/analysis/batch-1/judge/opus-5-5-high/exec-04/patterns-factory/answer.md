# Factory

## Visão geral

A criação dos objetos de cada caso fica toda no contêiner do Spring. A entrega pelo motoboy, a retirada na loja, os quatro cupons, as três formas de pagamento e o cliente da TransLog são `@Component`. A econômica e a expressa são a mesma classe com parâmetros diferentes e são montadas por dois métodos `@Bean` em `ShippingConfig`, que recebem o `CarrierGateway` por injeção. O `CheckoutService` recebe as listas já prontas e monta os mapas de código para objeto. Fora da configuração, nenhum lugar do código de produção faz `new` de uma entrega, de um cupom, de uma forma de pagamento ou do cliente da TransLog. Os níveis do clube e as regiões são constantes de enum com dados, sem corpo próprio, e a criação deles é a própria declaração do enum. Os objetos sem regra (os itens, o resumo, o aviso, os pequenos records de troca) são criados direto pelo construtor do record, sem fábrica nem builder. Não achei problema nesta dimensão.

## Item a item

**F, a criação dos objetos de cada caso.** O projeto usa duas formas, as duas concentradas no contêiner:

- **Componentes do Spring:**
  - `CourierShipping` (`src/main/java/br/com/loja/pedidos/shipping/CourierShipping.java:8`) e `StorePickupShipping` (`shipping/StorePickupShipping.java:6`);
  - `Welcome10Coupon`, `Minus50Coupon`, `FreeShippingCoupon` e `Buy3Pay2Coupon` (`coupon/*.java`, todos `@Component`);
  - `PixPayment`, `CreditCardPayment` e `BankSlipPayment` (`payment/*.java`, todos `@Component`);
  - `TranslogClient` (`shipping/TranslogClient.java:20`).
- **Métodos `@Bean`:** a econômica e a expressa são duas instâncias de `CarrierShippingOption`, criadas em `shipping/ShippingConfig.java:9-17` com o código, o `ServiceLevel` e o `CarrierGateway` injetado. Esses são os dois únicos `new` de objeto de caso em todo o código de produção (busca por `new` dos nomes das classes de caso em `src/`; os testes também não instanciam nenhuma).

As dependências chegam pelo construtor: o WELCOME10 recebe o `OrderRepository` (`coupon/Welcome10Coupon.java:15`) e as opções da transportadora recebem o gateway pelo parâmetro do `@Bean`. Quem usa as peças não cria nada. O `CheckoutService` recebe `List<ShippingOption>`, `List<Coupon>` e `List<PaymentMethod>` e indexa pelo `code()` de cada objeto (`checkout/CheckoutService.java:37-43`). É um registro que não cita nenhum caso, e um caso novo é só uma classe nova ou um `@Bean` novo. O `OrderService` recebe o `CheckoutService` injetado (`order/OrderService.java:35-44`) e não monta suas próprias peças.

Os níveis (`club/ClubTier.java:9-11`) e as regiões (`checkout/Region.java:9-13`) são constantes de enum com dados, sem corpo próprio. Não há objeto de caso a criar além da declaração, e essa declaração fica num lugar só. Não há `new` de caso nem de dependência espalhado ou repetido. A criação está concentrada.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são records criados direto pelo construtor:

- os itens: `PricedItem` (`checkout/CheckoutService.java:123`), `Order.Item` (`order/OrderService.java:54-55`) e `CheckoutRequest.Item` (`checkout/CheckoutRequest.java:43,51`);
- o resumo: `OrderSummary` (`checkout/CheckoutService.java:93`);
- o pedido: `Order` (`order/OrderService.java:56`);
- os objetos de troca entre as peças: `ShippingRequest`, `CouponContext`, `ShippingQuote` e `PaymentCharge` (`checkout/CheckoutService.java:61,70`, `shipping/CourierShipping.java:23`, `payment/PixPayment.java:25`).

O aviso ao cliente é uma linha inserida direto no banco por `NotificationRepository.create(cpf, type, orderId)` (`notification/NotificationRepository.java:22-26`), e o record `Notification` só é montado na leitura. O único método de fábrica estático é `CheckoutRequest.parse` (`checkout/CheckoutRequest.java:26-39`). Ele toma uma decisão de verdade: tolera tipos errados para que o serviço confira os erros na ordem pedida. É um método simples e não uma fábrica à parte. Não há builder nem "factory" para objetos que só juntam campos.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto da criação).** Não achei nenhuma:

- a única `@Configuration` além da aplicação (`ShippingConfig`) é necessária, porque a econômica e a expressa são a mesma classe parametrizada;
- o `@Bean Clock` (`PedidosApplication.java:15-18`) permite trocar o relógio nos testes, e o prazo de 48 horas depende dele;
- o `CarrierGateway`, com uma implementação só, responde à frase do enunciado sobre trocar de transportadora. Esse é o ponto do Adapter e não entra aqui;
- o `OrderLifecycleListener` responde aos avisos futuros (SMS, cashback). É de outro ponto e não entra aqui.

Não há fábrica, builder nem camada de criação que só repasse.
Conclusão: contenção

## O que mudaria primeiro

Nada nesta dimensão pede mudança. Se for mexer em algo, eu nomearia o código das opções da transportadora (`"ECONOMY"`, `"EXPRESS"`) num lugar só. Hoje ele está em `ShippingConfig.java:11,16`, separado das outras entregas, cujo código fica dentro da própria classe. Mas isso é só onde o rótulo mora, não a forma de criar.

## Dúvidas

- Os níveis e as regiões são enums só com dados, não objetos de caso com corpo. Descrevi a criação deles como a própria declaração do enum, concentrada, e não como "não há objeto por caso". A régua do Strategy é que diz se dados bastam para os níveis; aqui só conta que não há criação espalhada.
- Em `FreeShippingCoupon.java:16` o cupom compara `"STORE_PICKUP"`, o nome de uma entrega. Isso é questão do Strategy ou do remendo, não da criação, e não pesei aqui.

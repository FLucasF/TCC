# Padrões: Factory

## Visão geral

O agente deixou toda a criação dos objetos de cada caso com o contêiner do Spring. Cada entrega, cupom e forma de pagamento é uma classe `@Component`. Cada um desses grupos tem um registro (`ShippingOptions`, `Coupons`, `PaymentMethods`) que recebe a `List<...>` injetada e a indexa por `code()`. As dependências de cada caso chegam pelo construtor, também pelo contêiner: o cliente da TransLog para a econômica e a expressa, o histórico de compras para o WELCOME10. Os níveis do clube e as regiões são constantes de enum com dados próprios. No código de produção não há nenhum `new` de um caso ou de uma dependência de caso. Os objetos sem regra (o item do carrinho, o item do pedido, o aviso, o resumo) são records criados direto pelo construtor, sem fábrica nem builder. Não encontrei problema de criação. Perto deste pattern também não há estrutura sobrando.

## Item a item

**F, a criação dos objetos de cada caso.**
- **Entregas.** As quatro entregas são `@Component`:
  - `src/main/java/com/loja/orders/shipping/EconomyShipping.java:8`
  - `src/main/java/com/loja/orders/shipping/ExpressShipping.java:8`
  - `src/main/java/com/loja/orders/shipping/StorePickupShipping.java:7`
  - `src/main/java/com/loja/orders/shipping/CourierShipping.java:12`

  A econômica e a expressa recebem o `Carrier` pelo construtor (`EconomyShipping.java:11-12`, `ExpressShipping.java:11-12`). A única implementação dele, `TranslogCarrier`, também é `@Component`. Ela recebe o endereço por `@Value("${translog.url:}")` (`src/main/java/com/loja/orders/shipping/carrier/TranslogCarrier.java:24,34`), que `application.properties` liga a `TRANSLOG_URL`. Assim o cliente HTTP é montado uma vez só e compartilhado pelas duas entregas.
- **Cupons.** Os quatro cupons são `@Component`. O WELCOME10 recebe `PurchaseHistory` pelo construtor (`src/main/java/com/loja/orders/coupon/Welcome10Coupon.java:10,17`), e quem fornece é o `OrderRepository` (`src/main/java/com/loja/orders/order/OrderRepository.java:21`).
- **Formas de pagamento.** As três são `@Component`: `PixPayment.java:10`, `CreditCardPayment.java:11` e `BankSlipPayment.java:12`, todas em `payment/`.
- **Registros.** Os três registros só montam o mapa a partir da lista injetada:
  - `src/main/java/com/loja/orders/shipping/ShippingOptions.java:17-18`
  - `src/main/java/com/loja/orders/coupon/Coupons.java:24-25`
  - `src/main/java/com/loja/orders/payment/PaymentMethods.java:16-17`
- **Níveis e regiões.** São constantes de enum com dados por constante: `src/main/java/com/loja/orders/checkout/ClubTier.java:11-13` e `src/main/java/com/loja/orders/checkout/Region.java:14-18`. A escolha pelo texto passa por `fromCode` (`ClubTier.java:25`, `Region.java:30`). Os níveis não têm objeto com corpo próprio, mas criação não é o que falta ali. Se a regra do brinde está na casa do GOLD é pergunta do Strategy (a linha `tier == ClubTier.GOLD` em `src/main/java/com/loja/orders/checkout/CheckoutCalculator.java:104`), não desta correção.
- **Quem usa.** O `CheckoutCalculator` recebe os três registros injetados (`CheckoutCalculator.java:42-48`) e só pede o caso pelo código (`:57`, `:73`, `:83`).
- **`new` espalhado.** Procurei `new` de qualquer classe de caso ou do `TranslogCarrier` em `src/main` e em `src/test` e não achei nenhum. Também não há `@Bean` que crie caso: os dois de `src/main/java/com/loja/orders/config/AppConfig.java:16,22` criam o `Clock` e o `DataSource`.

A forma é o contêiner do Spring com registros por código, e a criação está concentrada. Uma entrega ou um cupom novo é só uma classe nova, sem mexer em quem usa.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são criados direto:
- O item do carrinho é o record `CartItem` (`src/main/java/com/loja/orders/checkout/CartItem.java:9`), criado com `new CartItem(...)` em `CheckoutCalculator.java:118`.
- O item do pedido é o record `OrderItem` (`src/main/java/com/loja/orders/order/OrderItem.java:3`), criado em `src/main/java/com/loja/orders/checkout/CheckoutRequestParser.java:67` e `OrderRepository.java:163`.
- O resumo é o record `CheckoutSummary`, criado pelo construtor em `CheckoutCalculator.java:93` e `OrderRepository.java:137`.
- O aviso não tem objeto de criação: `NotificationRepository.add(cpf, orderId, type)` grava a linha direto (`src/main/java/com/loja/orders/notification/NotificationRepository.java:21-31`), e o record `Notification` só é montado na leitura (`:54`).

Os únicos métodos de fábrica estáticos são simples e não têm decisão: `PaymentTerms.upFront` (`src/main/java/com/loja/orders/payment/PaymentTerms.java:8-10`) e `OrderResponse.of` (`src/main/java/com/loja/orders/order/OrderResponse.java:16`). Não há builder nem "factory" para nenhum desses objetos (busquei `Builder` e `Factory`; os únicos resultados são os builders do `HttpClient` da JDK em `TranslogCarrier.java:39,66`).
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** No que toca à criação, não achei estrutura sobrando:
- O `@Bean` do `DataSource` (`AppConfig.java:22-31`) tem uma decisão real: aceita o endereço do banco tanto em formato JDBC quanto `postgres://usuario:senha@...` (`src/main/java/com/loja/orders/config/JdbcUrl.java:13-33`).
- As interfaces de uma implementação só que aparecem aqui não são criação e cabem em outros itens. O `Carrier` serve à troca de transportadora que o enunciado prevê (ponto do Adapter). O `PurchaseHistory` inverte a dependência do cupom para o pedido.

Registro, sem contar aqui por não ser criação, o `OrderReceipts` (`src/main/java/com/loja/orders/order/OrderReceipts.java:8-21`). É um `@Service` que só repassa `OrderService.get`, recebe um `ProductController` que não usa e não é injetado em lugar nenhum (o `grep` só acha a própria classe). Ele é uma camada que só repassa, e cabe à correção que olhar essa parte do código.
Conclusão: contenção

## O que mudaria primeiro

Na criação, não há o que mudar: o contêiner concentra tudo, e um caso novo ou uma dependência nova muda só a classe do caso. Fora do escopo deste pattern, a mudança mais útil seria apagar o `OrderReceipts` (`order/OrderReceipts.java`). É código morto, e ainda faz o pacote de pedidos depender do controller do catálogo sem motivo.

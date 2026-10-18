# Factory

## Visão geral

O agente deixou a criação de todos os objetos de cada caso com o contêiner do Spring. As entregas STORE_PICKUP e COURIER, os quatro cupons, as três formas de pagamento e os três níveis são classes com `@Component`. A ECONOMY e a EXPRESS usam a mesma classe, `TransLogShipping`, com parâmetros diferentes, e por isso são montadas em dois métodos `@Bean` de uma configuração só, que recebe por injeção o cliente da TransLog. O `CheckoutService` recebe as listas injetadas e monta os mapas por código uma vez, no construtor. Nenhum `new` de um caso ou de uma dependência dele aparece fora desse lugar: nem no código de produção, nem nos testes. Um ponto merece registro. O WELCOME10 não tem dependência injetada: quem consulta os pedidos do cliente é o `CheckoutService`, que entrega ao cupom uma função pronta dentro do `CouponContext`. Os objetos sem regra (o item do carrinho, o item do pedido, o resumo, o aviso, o pedido) são records criados direto pelo construtor, sem builder nem fábrica. Fora dos pontos do enunciado, perto da criação, não achei estrutura sem necessidade.

## Item a item

**F, a criação dos objetos de cada caso.** A forma depende do grupo:

- **Entregas:** `CourierShipping` e `StorePickupShipping` têm `@Component` (`src/main/java/br/com/loja/pedidos/shipping/CourierShipping.java:10`, `src/main/java/br/com/loja/pedidos/shipping/StorePickupShipping.java:7`). A ECONOMY e a EXPRESS saem de `@Bean` em `ShippingConfig` (`src/main/java/br/com/loja/pedidos/shipping/ShippingConfig.java:9-17`). Cada um faz `new TransLogShipping("ECONOMY", "STD", client)` ou `("EXPRESS", "EXP", client)`, com o `TransLogClient` injetado. A implementação desse cliente, `HttpTransLogClient`, também é `@Component` e recebe o endereço por `@Value("${translog.url}")` (`src/main/java/br/com/loja/pedidos/shipping/HttpTransLogClient.java:22,31`).
- **Cupons, pagamentos e níveis:** todos com `@Component`. Os cupons em `src/main/java/br/com/loja/pedidos/coupon/Welcome10Coupon.java:10`, `Minus50Coupon.java:10`, `FreeShippingCoupon.java:9` e `Buy3Pay2Coupon.java:9`. Os pagamentos em `src/main/java/br/com/loja/pedidos/payment/PixPayment.java:9`, `CreditCardPayment.java:11` e `BankSlipPayment.java:9`. Os níveis em `src/main/java/br/com/loja/pedidos/club/BronzeTier.java:5`, `SilverTier.java:9` e `GoldTier.java:10`.
- **Escolha do caso:** o `CheckoutService` recebe as quatro listas injetadas e as indexa por `code()` com `Coded.index` (`src/main/java/br/com/loja/pedidos/checkout/CheckoutService.java:40-48`, `src/main/java/br/com/loja/pedidos/common/Coded.java:12-16`).
- **Onde não há `new`:** busquei `new` dessas classes no projeto inteiro. Só aparecem as duas linhas do `ShippingConfig`. Nos testes, o único arquivo (`src/test/java/br/com/loja/pedidos/PedidosApiTest.java`) não instancia nenhum caso.
- **A dependência do WELCOME10:** o cupom não recebe o repositório de pedidos. Quem o recebe é o `CheckoutService` (`CheckoutService.java:34`), e ele passa ao cupom a função `() -> !orders.hasPurchase(cpf)` dentro do `CouponContext` (`CheckoutService.java:109-110`, `src/main/java/br/com/loja/pedidos/coupon/CouponContext.java:10-11`). Uma dependência nova de um cupom novo mudaria o `CouponContext` e o `CheckoutService`, e não só o lugar da criação. Isso não é `new` espalhado, mas mostra que nem toda dependência dos casos fica no contêiner.

A criação está concentrada: o contêiner cria tudo, e o único `new` dos casos fica numa configuração só. Como o item é descritivo, não há acerto nem omissão a marcar.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são records criados direto pelo construtor:

- o item do carrinho, `CartLine`, em `src/main/java/br/com/loja/pedidos/checkout/CartLine.java:7`, criado em `CheckoutService.java:141`;
- o item do pedido, `OrderItem`, em `src/main/java/br/com/loja/pedidos/order/OrderItem.java`, criado em `src/main/java/br/com/loja/pedidos/checkout/CheckoutRequest.java:26` e em `src/main/java/br/com/loja/pedidos/order/OrderRepository.java:107`;
- o resumo, `Summary`, em `src/main/java/br/com/loja/pedidos/checkout/Summary.java:5`, criado com 11 argumentos em `CheckoutService.java:94-96`;
- o aviso, `Notification`, só é montado na leitura (`src/main/java/br/com/loja/pedidos/notification/NotificationRepository.java:46`). Para criar, o repositório faz o `INSERT` direto (`NotificationRepository.java:21-25`);
- o pedido, `Order`, criado em `src/main/java/br/com/loja/pedidos/order/OrderService.java:51`. O `withStatus` (`OrderService.java:147-150`) é um método estático simples que copia o pedido com outra situação, sem nenhuma decisão.

Os objetos auxiliares seguem o mesmo jeito: `ShippingQuote`, `ShippingRequest`, `CouponContext`, `PaymentContext` e `PaymentCharge` são records. A busca por "builder" ou "factory" no código de produção só achou o `HttpClient.newBuilder()` e o `HttpRequest.newBuilder()` da biblioteca padrão (`HttpTransLogClient.java:29,42`). Nenhum builder ou fábrica foi escrito para objetos que só juntam campos.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto da criação).**

- O `ShippingConfig` com dois `@Bean` existe porque duas opções usam a mesma classe com parâmetros diferentes. Ele tem um motivo e não é uma camada que só repassa.
- O `Coded.index` (`Coded.java:12-16`) é um utilitário de dez linhas que monta o registro por código, usado nos quatro grupos. Não é uma fábrica a mais.
- O `TransLogClient` é uma interface com uma implementação só (`src/main/java/br/com/loja/pedidos/shipping/TransLogClient.java:5`). Ele responde à frase "pode ser que um dia a gente troque de transportadora", que é ponto de outro pattern e é julgada lá.
- O `Clock` é declarado como `@Bean` (`src/main/java/br/com/loja/pedidos/PedidosApplication.java:16-19`) para o relógio poder ser trocado. É uma injeção comum, não uma estrutura criacional sem motivo.

Não achei interface, camada, builder nem fábrica sem necessidade perto da criação.
Conclusão: contenção

## O que mudaria primeiro

Passaria a dependência do WELCOME10 para dentro do próprio cupom. Hoje o `Welcome10Coupon` receberia por injeção o repositório de pedidos (ou um serviço que diga se é a primeira compra), em vez de o `CheckoutService` montar a função e levá-la no `CouponContext` (`CheckoutService.java:109-110`). Assim, um cupom novo com uma dependência nova mudaria só a classe dele e o contêiner. Do jeito atual, muda também o contexto comum de todos os cupons e o cálculo. Fora isso, a criação já está do tamanho do problema.

## Dúvidas

- A régua diz que "o WELCOME10 precisa dos pedidos do cliente". Aqui o cupom recebe essa informação pelo contexto, e não por injeção. Li isso como uma escolha de onde a dependência mora: a criação continua concentrada no contêiner. Registrei a escolha no F, que é descritivo, sem tratá-la como `new` espalhado.
- Não contei o `TransLogClient` com uma implementação só como exagero no E, porque o enunciado tem a frase sobre trocar de transportadora. Esse ponto é julgado pela correção do pattern que cobre essa frase.

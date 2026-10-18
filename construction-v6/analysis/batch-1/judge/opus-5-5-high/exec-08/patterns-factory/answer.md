# padrões: Factory

## Visão geral

O agente deixou toda a criação dos objetos por caso com o contêiner do Spring. As quatro entregas, os quatro cupons, as três formas de pagamento e os três níveis do clube são classes `@Component`. Elas entram no `CheckoutService` como listas, e o construtor dele as transforma em mapas pela chave de cada uma. Esse construtor é o único lugar de produção que monta o registro. A única dependência externa, o `TransLogClient`, também é um `@Component`, e o contêiner o injeta pelo construtor na econômica e na expressa. Não há `new` de um caso nem de uma dependência espalhado pelo código de produção. O único lugar que monta as peças à mão é a preparação do teste de unidade. A dependência do WELCOME10 (os pedidos do cliente) não vai para dentro do cupom: quem a resolve é o próprio cálculo, que entrega ao cupom só um booleano. Isso é desenho do Strategy, não de criação. Os objetos sem regra (o item, o aviso, o resumo, as respostas) são criados direto, por construtor, record ou um `from` estático simples, sem fábrica nem builder. Não achei estrutura criacional sobrando.

## Item a item

**F, a criação dos objetos de cada caso.** A criação está concentrada no contêiner do Spring.
- **Entregas:** `CourierShipping` (`src/main/java/com/loja/service/shipping/CourierShipping.java:8`) e `StorePickupShipping` (`StorePickupShipping.java:7`) são `@Component` sem dependências. `EconomyShipping` (`EconomyShipping.java:7-12`) e `ExpressShipping` (`ExpressShipping.java:7-12`) são `@Component` e recebem o `TransLogClient` pelo construtor, repassado à classe-base abstrata `TransLogShipping` (`TransLogShipping.java:13-17`). O `TransLogClient` também é `@Component` (`src/main/java/com/loja/integration/TransLogClient.java:9`), com a URL vinda de `translog.url=${TRANSLOG_URL}` (`src/main/resources/application.properties`). As duas entregas recebem a mesma instância, e nenhuma delas faz `new` do cliente.
- **Cupons, pagamentos e níveis:** todos são `@Component` sem dependências: `Welcome10Coupon.java:8`, `Minus50Coupon.java:7`, `FreeShippingCoupon.java:7`, `Buy3Pay2Coupon.java:9`, `PixPayment.java:8`, `CreditCardPayment.java:9`, `BankSlipPayment.java:7`, `BronzeClub.java:8`, `SilverClub.java:9` e `GoldClub.java:9`.
- **O registro:** o construtor do `CheckoutService` recebe as quatro listas e monta os mapas pela chave de cada caso, com `getCode`, `getMethod` e `getTier` (`src/main/java/com/loja/service/CheckoutService.java:31-50`). Um caso novo é uma classe `@Component` nova, e esse construtor não muda. Os nomes dos casos aparecem só como chave de registro, dentro da casa de cada um (por exemplo `CourierShipping.java:15`), o que não conta como código fora da casa.
- **Os dois fluxos usam as mesmas peças:** o registro do pedido não monta as suas, ele chama o mesmo `checkoutService.calculateSummary` (`src/main/java/com/loja/service/OrderService.java:46`).
- **Sem `new` espalhado:** a busca por `new` das classes de caso e do `TransLogClient` em `src/main` não encontra nada. O único lugar que monta as peças à mão é o `setUp` do teste (`src/test/java/com/loja/service/CheckoutServiceTest.java:48-71`), com o cliente da TransLog simulado. É a ligação de um teste de unidade, num lugar só.
- **A dependência do WELCOME10:** o `Welcome10Coupon` não recebe repositório. É o `CheckoutService` que consulta `orderRepository.existsByCustomerCpfAndStatusIn` (`CheckoutService.java:239-242`) e passa o resultado como `isFirstPurchase` no `CouponContext` (`src/main/java/com/loja/service/coupon/CouponStrategy.java:17-23`). Para a criação isso só significa que o cupom não tem dependência a injetar. O fato de o cálculo carregar um dado que só um cupom usa é assunto do Strategy.
- **O seguro, fora dos objetos por caso:** a região não tem objeto por caso. O `InsuranceService.getRate` faz um `switch` que monta um record de dados (porcentagem e limite) por região (`src/main/java/com/loja/service/InsuranceService.java:22-30`). Não há objeto a concentrar.

Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos os objetos sem regra de criação são criados direto:
- **O item do carrinho:** `new OrderItem(sku, quantity)` (`src/main/java/com/loja/domain/OrderItem.java:20-23`), usado em `CheckoutService.java:245` e `OrderService.java:49`. Essa conversão de `ItemInfo` para `OrderItem` aparece repetida nos dois lugares. É uma linha trivial, que não pede fábrica.
- **O aviso:** `new Notification(cpf, type, orderId)`, cujo construtor só fixa `status = NEW` (`src/main/java/com/loja/domain/Notification.java:20-25`). É chamado direto nos cinco pontos que avisam (`OrderService.java:100, 114, 131, 158, 201`).
- **O resumo:** um record (`src/main/java/com/loja/domain/OrderSummary.java:3-15`) montado pelo construtor canônico (`CheckoutService.java:84-96`).
- **As respostas:** records com um método estático `from` que só copia campos (`src/main/java/com/loja/dto/OrderResponse.java:19-32`, `NotificationResponse.java:13-20`, e o mesmo em `OrderSummaryResponse`, `ProductResponse` e `CustomerResponse`).

A busca por `Factory` e `Builder` em `src/main` só encontra o `RestClient.builder()` da biblioteca (`TransLogClient.java:18`), que não foi criado pelo agente.

Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação, não achei nada: nenhuma fábrica, builder ou configuração `@Bean` que só repasse. A única `@Configuration` é a `DataInitializer`, e o único `@Bean` dela é um `CommandLineRunner` que grava o catálogo inicial que o enunciado pede (`src/main/java/com/loja/config/DataInitializer.java:10-41`). A classe-base `TransLogShipping` tem duas subclasses e guarda o que as duas têm em comum (`TransLogShipping.java:11-48`), então não é uma camada vazia.

Conclusão: contenção

## O que mudaria primeiro

- **Injetar no WELCOME10 o que ele consulta.** Hoje é o `CheckoutService` que decide se é a primeira compra (`CheckoutService.java:239-242`). Se o `Welcome10Coupon` recebesse pelo construtor o acesso aos pedidos do cliente, que o contêiner injetaria como faz com o `TransLogClient`, a dependência de um cupom seria resolvida na criação dele. Um cupom novo com outra dependência não exigiria mudar o contexto comum nem o cálculo.
- Fora isso, não há mudança necessária na criação. O ganho restante seria só cosmético: juntar num lugar a conversão de `ItemInfo` para `OrderItem`, hoje repetida em `CheckoutService.java:245` e `OrderService.java:49`.

## Dúvidas

- **O WELCOME10 sem repositório.** A régua diz que o WELCOME10 "precisa dos pedidos do cliente". Aqui ele não recebe essa dependência: quem consulta é o cálculo. Li isso como uma escolha do desenho do Strategy (o que o contexto carrega), e não como criação espalhada, porque não há `new` repetido nem peça montada fora do contêiner. Por isso ficou só como descrição no F.
- **Os `new` no teste.** Não contei como criação espalhada os `new` das estratégias em `CheckoutServiceTest.java:48-71`: é a montagem de um teste de unidade, feita num lugar só, e não código de produção.

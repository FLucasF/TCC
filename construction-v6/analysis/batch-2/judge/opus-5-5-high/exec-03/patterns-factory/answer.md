# Padrões: Factory (a criação concentrada)

## Visão geral

O agente monta tudo à mão, sem contêiner, como se espera no Javalin. Toda a criação passa por um caminho só. O `main` cria a infraestrutura: o pool, o `PostgresStore`, o `TransLogClient` e o relógio (`src/main/java/shop/App.java:23-33`). Em seguida chama `Application.create`, que é a raiz de composição dos serviços (`src/main/java/shop/service/Application.java:13-21`). Os objetos de cada caso nascem nos métodos `standard()` de três registros: `ShippingOptions`, `Coupons` e `PaymentMethods`. Cada família tem uma lista única, e só `Application.create` chama esses métodos. O cliente da TransLog é criado uma vez e passado às duas entregas que dependem dele. Não há `new` de um caso espalhado nem repetido, e os testes usam a mesma montagem (`src/test/java/shop/CheckoutExamplesTest.java:47`).

O único ponto a registrar é o WELCOME10. Ele não recebe o acesso aos pedidos do cliente quando é criado. Quem consulta é o cálculo, e consulta para qualquer cupom informado, pondo o resultado no `CouponContext`.

Os objetos sem regra (o item do carrinho, o aviso, o resumo) são records criados direto pelo construtor. Não há fábrica nem builder em lugar nenhum do projeto.

## Item a item

**F, a criação dos objetos de cada caso.**
- **As entregas.** São criadas em `ShippingOptions.standard(carrier)` (`src/main/java/shop/domain/shipping/ShippingOptions.java:16-22`). A econômica e a expressa são duas instâncias da mesma classe, configuradas com o código e o serviço da TransLog: `new CarrierShipping("ECONOMY", "STD", carrier)` e `new CarrierShipping("EXPRESS", "EXP", carrier)` (linhas 18-19). As duas recebem o `CarrierClient` pelo construtor (`src/main/java/shop/domain/shipping/CarrierShipping.java:40-44`). O `new TransLogClient(...)` aparece uma vez só, em `src/main/java/shop/App.java:32`, e nenhuma classe que usa uma entrega cria a sua.
- **Os cupons.** São criados em `Coupons.standard()` (`src/main/java/shop/domain/coupon/Coupons.java:21-23`).
- **As formas de pagamento.** São criadas em `PaymentMethods.standard()` (`src/main/java/shop/domain/payment/PaymentMethods.java:16-18`).
- **Os níveis.** São constantes de enum com dados, sem dependências (`src/main/java/shop/domain/ClubTier.java:8-10`). Quem os cria é a própria linguagem, e o texto vira constante em `ClubTier.parse` (linha 22).
- **Quem usa.** `CheckoutCalculator` recebe os três registros prontos pelo construtor (`src/main/java/shop/service/CheckoutCalculator.java:36-40`) e só chama `find(código)` (linhas 50, 60, 67). Ele nunca cria um caso.
- **Os listeners do pagamento.** São criados juntos, em `src/main/java/shop/service/Application.java:17-18`.

Uma busca por `new` das classes de caso confirma: no código de produção, elas só aparecem dentro dos três `standard()`.

A ressalva é a dependência do WELCOME10. `Welcome10` não recebe nada na criação. Ele lê `c.firstPurchase()` do contexto (`src/main/java/shop/domain/coupon/Welcome10.java:15-18`). Quem consulta o banco é o cálculo, `tx.orders().hasPurchased(req.cpf())` (`src/main/java/shop/service/CheckoutCalculator.java:62`), para qualquer cupom informado. O motivo provável é que os repositórios só existem dentro da transação (`Tx`), e por isso não podiam ser entregues ao cupom no `standard()`. O efeito é que uma dependência nova de um cupom não muda o lugar da criação: muda o `CouponContext` e o cálculo. Isso pesa mais no Strategy; aqui fica registrado como o limite da criação concentrada.

Em resumo: a montagem é feita à mão num lugar só (`main` mais `Application.create`), com um registro por família.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.**
- **O item do carrinho.** É um record (`src/main/java/shop/domain/CartItem.java:3`), criado direto em `src/main/java/shop/service/CheckoutService.java:34`. O `RequestItem` também é um record (`src/main/java/shop/service/CheckoutRequest.java:9`), criado direto no parser (`src/main/java/shop/web/CheckoutRequestParser.java:31`).
- **O aviso.** `Notification` é um record (`src/main/java/shop/domain/Notification.java:5`). Os repositórios o criam com o construtor (`src/main/java/shop/persistence/memory/MemoryStore.java:132`, `src/main/java/shop/persistence/postgres/PgNotifications.java:81`).
- **O resumo.** `Summary` é um record cujo construtor compacto só arredonda os valores (`src/main/java/shop/domain/Summary.java:9-18`). Ele é criado direto com `new Summary(...)` no fim do cálculo (`src/main/java/shop/service/CheckoutCalculator.java:79`).
- **O pedido.** `Order` é criado direto (`src/main/java/shop/service/CheckoutService.java:32`), e os métodos `withStatus`/`withShipment` (`src/main/java/shop/domain/Order.java:9-15`) são cópias simples.
- **Os outros.** O único método de fábrica é o `Catalog.product` privado, que só converte os textos em `BigDecimal` (`src/main/java/shop/domain/Catalog.java:23-25`). Ele é estático e simples, sem nenhuma decisão.

A busca por `Builder`/`Factory` não acha nada no código de produção.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.**
- **As interfaces.** Perto da criação, não há interface com uma implementação só que nada substitua. `Store`/`Tx` e os repositórios têm a versão Postgres e a versão em memória (`src/main/java/shop/persistence/memory/MemoryStore.java:31`), e os testes usam a segunda. `CarrierClient` tem o `TransLogClient` e o falso do teste (`src/test/java/shop/CheckoutExamplesTest.java:41`), e o enunciado avisa que a transportadora pode mudar.
- **A montagem.** `Application` não é uma camada que só repassa. É a raiz de composição, um record que entrega os três serviços às rotas.
- **O `PostgresTx`.** Ele cria um repositório novo a cada chamada (`src/main/java/shop/persistence/postgres/PostgresTx.java:20-41`). Isso é detalhe de implementação, não estrutura a mais.
- **Uma observação.** O `MemoryStore` fica em `src/main/` mesmo sendo usado só pelos testes. É uma questão de arquitetura, não uma fábrica ou camada desnecessária.

Não há builder, fábrica ou camada de repasse que ninguém pediu.
Conclusão: contenção

## O que mudaria primeiro

Daria ao WELCOME10 a sua própria dependência. Por exemplo, o cupom receberia na criação algo que responda "este CPF já comprou?", recebendo a `Tx` ou o CPF pela chamada, em vez de o cálculo consultar `hasPurchased` para todo cupom (`src/main/java/shop/service/CheckoutCalculator.java:62`). Assim, um cupom novo com uma dependência nova mudaria só `Coupons.standard()` e a própria classe, sem mexer no `CouponContext` nem no cálculo. Esta é a única mudança que importa: a montagem no resto já está concentrada.

## Dúvidas

- **O `firstPurchase` no `CouponContext`.** Hesitei entre registrar isso como limite da criação ou deixar só para o Strategy. Como o F é descritivo e o enunciado do pattern cita o WELCOME10 entre os casos com dependência, registrei no F sem mudar a conclusão.
- **Criação em dois lugares.** Contei `App.main` (a infraestrutura) e `Application.create` (os serviços e os casos) como um lugar só. Os dois são a montagem em sequência, e nenhuma classe que usa os objetos os cria.

# padrões: Factory

## Visão geral

A criação dos objetos está concentrada e montada à mão, como se espera num projeto Javalin sem contêiner. O `Main` cria a infraestrutura: o banco, o cliente da TransLog e o relógio. `Api.create` monta o cálculo e o serviço de pedidos. Cada registro (`ShippingMethods`, `Coupons`, `PaymentMethods`) tem um método estático `standard()`, e é só ali que se faz o `new` dos seus casos. O cliente da TransLog é criado uma vez só e passado às duas entregas que dependem dele. Nenhum `new` de um caso aparece espalhado ou repetido. Há duas ressalvas, sem gravidade. A dependência do WELCOME10 (os pedidos do cliente) não entra na montagem: ela chega a cada cálculo, por um lambda dentro do contexto do cupom. E as consequências do pagamento são criadas dentro do próprio `OrderService`, e não no ponto de montagem. Os objetos sem regra (o item do carrinho, o resumo, o aviso) são records criados direto, sem fábrica nem builder.

## Item a item

**F, a criação dos objetos de cada caso.**
- **Ponto de montagem.** A forma é a montagem à mão, em dois níveis. `Main.java:14-15` cria o `JdbcStore` e o `TransLogGateway` e os entrega a `Api.create`. `Api.java:24-27` cria o `PricingService` com os três registros e o `OrderService`. Os testes usam o mesmo `Api.create` com um armazenamento em memória e uma transportadora de mentira (`src/test/java/shop/TestApp.java:25,29-34`). Isso mostra que a montagem é o único lugar por onde as peças entram.
- **As entregas.** `ShippingMethods.standard(carrier)` (`Shipping.java:109-114`) cria as quatro entregas. A econômica e a expressa são duas instâncias da mesma classe com parâmetros diferentes: `new CarrierShippingMethod("ECONOMY", "STD", carrier)` e `new CarrierShippingMethod("EXPRESS", "EXP", carrier)` (`Shipping.java:111-112`). O cliente da TransLog é recebido como parâmetro, não criado ali.
- **Os cupons e os pagamentos.** Os cupons são criados em `Coupons.standard()` (`Coupons.java:40-41`) e os pagamentos em `PaymentMethods.standard()` (`Payments.java:29-30`). As classes de cada caso são aninhadas e não têm dependências.
- **Os níveis.** Não há objeto por caso: são constantes de enum só com dados (`ClubTier.java:7-9`), sem corpo próprio. A linguagem as cria, e não há o que concentrar.
- **O que se repete.** Nada: cada caso tem um único `new` no código de produção (conferido com `grep` sobre os nomes das classes).
- **A dependência do WELCOME10.** Os pedidos do cliente não são injetados no cupom na montagem. `PricingService.java:54-55` cria, a cada cálculo, um `BooleanSupplier` (`() -> !tx.hasFulfilledOrder(r.cpf())`) e o põe no `CouponContext` (`Coupons.java:15-16`). O motivo provável é que a consulta precisa da transação do pedido. O efeito é que um cupom novo com outra dependência mudaria o `CouponContext` e o `PricingService`, e não só o `standard()`.
- **Fora dos quatro pontos.** A lista de consequências do pagamento (estoque, crédito, aviso) é criada num campo do próprio `OrderService` (`OrderService.java:17`), em vez de chegar pelo construtor a partir de `Api.create`.

Item descritivo: a criação está concentrada no `Main` e em `Api.create`, que chama os `standard()`, com as duas ressalvas acima.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.**
- **O item do carrinho.** `CartLine` é um record (`Coupons.java:9-13`), criado com `new` em `PricingService.java:98`. `OrderItem` também é um record (`Models.java:15`), criado direto em `OrderService.java:33`.
- **O resumo.** `Summary` é um record criado com `new` em `PricingService.java:69`. O construtor compacto só arredonda os campos para centavos (`Models.java:27-36`): é uma normalização dentro do construtor, não uma fábrica.
- **O aviso.** `Notification` é um record (`Models.java:19`). O armazenamento o cria direto com `new` (`MemoryStore.java:101`, e a leitura em `JdbcStore.java:381-382`), a partir de `Tx.addNotification(cpf, type, orderId)`.
- **Os demais.** Há métodos de fábrica estáticos simples (`ApiException.unprocessable/conflict/notFound`, `ApiException.java:13-23`) e os "withers" do pedido (`Order.withStatus/paid/shipped`, `Models.java:41-51`), que copiam o record.
- **Builder.** Não existe nenhum. Os únicos `newBuilder` são os da JDK, no `HttpClient`/`HttpRequest` do `TransLogGateway`.

Tudo se cria com construtor, record ou método estático simples.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação, não encontrei fábrica, builder ou camada sem necessidade.
- **Os `standard()`.** São fábricas estáticas, mas cada uma é o único lugar onde o seu registro é montado, que é o que o ponto F pede. Não são exagero.
- **As interfaces.** `Store`/`Tx` (`Store.java:9-51`) têm duas implementações de verdade, `JdbcStore` e `MemoryStore`, e a segunda é usada pelos testes (`TestApp.java:25`). `CarrierGateway` é trocada nos testes por um lambda (`TestApp.java:29-33`) e corresponde ao ponto da troca de transportadora, que é de outro pattern.
- **O único repasse.** É o auxiliar privado `Api.orders(api)` (`Api.java:77-79`), que só devolve `api.orders`. É uma indireção trivial dentro de uma classe, não uma camada, e não conto como estrutura.

Conclusão: contenção

## O que mudaria primeiro

1. Passar a lista de consequências pelo construtor do `OrderService`, montada em `Api.create`, em vez de criá-la em `OrderService.java:17`. O enunciado já prevê SMS e cashback. Hoje, cada consequência nova muda o serviço de pedidos; depois da mudança, mudaria só o ponto de montagem, como já acontece com as entregas, os cupons e os pagamentos.
2. Dar ao WELCOME10 a sua dependência na montagem: por exemplo, uma consulta de pedidos recebida no construtor e chamada com a transação do contexto. Hoje ela é um lambda criado a cada cálculo em `PricingService.java:54-55`, e o `CouponContext` acumula um campo específico de um cupom (`firstPurchase`).

## Dúvidas

- O F lista "os níveis" entre os objetos por caso. Aqui os níveis são constantes de enum só com dados, não objetos com comportamento próprio. Li isso como "não há objeto por caso para os níveis" e não como falha de criação. Se o desenho por dados é adequado, quem avalia é o Strategy.
- As consequências do pagamento (`OrderService.java:17`) não estão entre os casos que o F nomeia. Registrei a criação delas como observação perto do pattern, sem tratá-la como parte do item.

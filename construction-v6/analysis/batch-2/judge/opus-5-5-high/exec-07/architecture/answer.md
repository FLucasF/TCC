# a arquitetura

## Visão geral

O agente organizou o serviço por assunto (pedido, checkout, cupom, entrega, pagamento, notificação, catálogo e cliente). As variações ficam atrás de interfaces (`Coupon`, `ShippingOption`, `PaymentOption`, `PaymentConfirmationReaction`, `CarrierGateway`). Todo o JDBC fica em `persistence`, e o resumo sai de um cálculo só.

A camada web (`web.Routes`) é fina, de fato só traduz. O problema mais sério é o A1: a regra de negócio conhece HTTP. Toda recusa do negócio é um `ApiException` que já leva o código HTTP (422, 409, 404 ou 503), e quem escolhe esse código é o calculador, o cupom, a entrega ou o `OrderService`. O `OrderService` ainda recebe o corpo cru da requisição (`JsonNode`) e o lê ele mesmo.

O segundo problema é um ciclo que fecha por comportamento entre `order` e `checkout`. O `OrderService` chama o `CheckoutCalculator`, e o calculador consulta o `OrderRepository` para saber se é a primeira compra. Os outros laços do grupo fecham por tipos de dado ou pela interface `Tx`, que mora no mesmo pacote que a implementação Postgres.

## Item a item

**A1, a regra de negócio não conhece a web?** Nenhuma classe de negócio usa o Javalin. Pelo relatório, só `App`, `Main` e `web.Routes` usam, e essas três são a montagem e a camada web. Mesmo assim, a regra de negócio conhece HTTP de duas formas:

- **Os códigos HTTP.** `ApiException` guarda o código HTTP de cada recusa. As fábricas `unprocessable` (422), `conflict` (409), `notFound` (404) e `unavailable` (503) estão em `src/main/java/shop/ApiException.java:14-28`, e o `status()` em `:34`. O `Routes` só copia esse número para a resposta (`src/main/java/shop/web/Routes.java:62`). Assim, quem decide a situação HTTP é a própria regra de negócio:
  - `CheckoutCalculator` (`src/main/java/shop/checkout/CheckoutCalculator.java:46`, `:50`, `:70`, `:80`, `:83`, `:87`, `:111`, `:119`, `:123`, `:126`, `:139`);
  - os cupons (`src/main/java/shop/coupon/Welcome10Coupon.java:17`, `src/main/java/shop/coupon/Minus50Coupon.java:19`, `src/main/java/shop/coupon/FreeShippingCoupon.java:16`);
  - o motoboy (`src/main/java/shop/shipping/CourierShipping.java:19`);
  - o `OrderService`, que escolhe 409 para a transição recusada e para o pagamento recusado (`src/main/java/shop/order/OrderService.java:96`, `:175`) e 422 para o corpo incompleto (`:70`, `:76`, `:106`);
  - o `TransLogGateway` (`src/main/java/shop/shipping/TransLogGateway.java:54`, `:68`), que é adaptador de saída e não regra de negócio, mas também escolhe o código HTTP de entrada.
- **O corpo da requisição.** `OrderService.confirmPayment(String id, JsonNode body)` (`src/main/java/shop/order/OrderService.java:64`) e `ship(String id, JsonNode body)` (`:101`) recebem o JSON cru que o `Routes` lê de `ctx.body()` (`src/main/java/shop/web/Routes.java:37-38`, `:47-53`). Quem extrai `transactionId`, `paidAt` e `trackingCode` é o serviço (`OrderService.java:67-68`, `:104`), e para isso usa `CheckoutRequest.text(JsonNode, ...)`. O mesmo vale para `CheckoutRequest`: ele é o objeto de entrada da API, com os campos crus como chegam, e é montado a partir do `JsonNode` (`src/main/java/shop/checkout/CheckoutRequest.java:27-45`). Ele mora em `checkout`, não com o controller, e é o que `CheckoutCalculator.calculate` recebe (`CheckoutCalculator.java:39`). Ele está no pacote do negócio, mas na prática é o corpo da requisição. Pesa menos que os dois pontos acima, mas aponta no mesmo sentido.
- **No sentido da saída**, os records do domínio servem direto de resposta. Eles levam anotações do Jackson para moldar o JSON da API: `@JsonIgnore` em `src/main/java/shop/order/Order.java:11-12` e em `src/main/java/shop/notification/Notification.java:6`. É um vazamento leve do formato da resposta para dentro do domínio.

A tradução de e para HTTP deveria morar no `Routes`, e ela está espalhada pelo negócio.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório traz um grupo só, com 6 dos 10 pacotes: `checkout`, `coupon`, `notification`, `order`, `persistence` e `shipping`. Laço por laço:

- **`order` ↔ `checkout`, fecha por comportamento.** Num sentido, `order.OrderService -> checkout.CheckoutCalculator`: o serviço chama `calculator.calculate` (`src/main/java/shop/order/OrderService.java:41`, `:46`). No outro, `checkout.CheckoutCalculator -> order.OrderRepository`: o calculador chama `tx.orders().hasConfirmedPurchase(...)` para decidir a primeira compra do WELCOME10 (`src/main/java/shop/checkout/CheckoutCalculator.java:73`). São regras de dois pacotes que se chamam nos dois sentidos, e é esse o laço estrutural. As demais arestas `order -> checkout` são de dado (`Summary` e `CheckoutRequest` no `Order` e na `CreditReaction`).
- **`order`/`notification` ↔ `persistence`, fecha por interfaces de comportamento.** `order` e `notification` usam `persistence.Tx` e `persistence.Store` (`OrderService.java:10-11`, `src/main/java/shop/notification/Notifier.java:4`). Na volta, `persistence.Tx` devolve `order.OrderRepository`, `order.PaymentNoticeRepository` e `notification.NotificationRepository` (`src/main/java/shop/persistence/Tx.java:3-7`), e o `PostgresStore` implementa esses repositórios (`src/main/java/shop/persistence/PostgresStore.java:192`, `:268`, `:306`). As dependências são interfaces de acesso a dados, não records. O ciclo existe porque as abstrações `Tx` e `Store` moram no mesmo pacote que a implementação JDBC: tirar as duas de `persistence` desfaria o laço sem mudar o desenho. É um caso intermediário, que pesa menos que o laço `order` ↔ `checkout`.
- **`checkout` ↔ `persistence`, fecha por dado.** Na volta, `persistence.PostgresStore -> checkout.Summary`: o resumo é gravado em JSON (`PostgresStore.java:253`, `:261`).
- **`notification` ↔ `order`, fecha por dado.** Na volta, `notification.Notifier -> order.Order`: o `Notifier` só lê `id` e `customerCpf` (`Notifier.java:10-12`).
- **`checkout` ↔ `coupon`, fecha por dado.** Na volta, `coupon.Buy3Pay2Coupon` e `coupon.CouponContext -> checkout.CartLine`, um record (`src/main/java/shop/coupon/CouponContext.java:10`, `src/main/java/shop/coupon/Buy3Pay2Coupon.java:17`).
- **`checkout` ↔ `shipping`, fecha por dado.** Na volta, `shipping.ShippingContext` e `shipping.CarrierShipping -> checkout.Region`, um enum que só informa os dias a mais (`src/main/java/shop/shipping/CarrierShipping.java:23`).

O pior laço, `order` ↔ `checkout`, fecha por comportamento.
Conclusão: não segue

**A3, o controller só traduz?** `Routes` lê o corpo, chama um método do `OrderService` ou do `QueryService` (`src/main/java/shop/web/Routes.java:34-44`) e traduz o resultado ou o `ApiException` em situação HTTP e JSON (`:56-74`). Ele não faz conta, não decide recusa do negócio, não muda a situação do pedido e não mexe no estoque. A única decisão que ele toma é tratar um corpo ilegível como objeto vazio (`:47-53`), e isso é tradução. O que deveria estar no controller e não está, a leitura dos campos do corpo, já foi contado no A1.
Conclusão: segue

**A4, o acesso ao banco está isolado?** O SQL e o JDBC estão só em `persistence`: `PostgresStore`, com um repositório por agregado, e `Schema`, com a criação das tabelas e a carga do catálogo. Um `grep` por `java.sql`, `Connection`, `PreparedStatement`, `SELECT`, `INSERT` e `UPDATE` fora de `persistence` não acha nada. A regra de negócio fala com os dados pelas interfaces de repositório do `Tx`, por exemplo `tx.products().lock`, `adjustStock`, `tx.orders().update` e `tx.customers().addCredit` (`src/main/java/shop/order/OrderService.java:156`, `:161`, `:90`; `src/main/java/shop/order/CreditReaction.java:8`). Há uma ressalva menor que não muda a conclusão: `Schema` repete o `INSERT ... ON CONFLICT` do catálogo (`src/main/java/shop/persistence/Schema.java:79-88`) em vez de usar `ProductRepository.insertIfAbsent` (`PostgresStore.java:156`), que fica sem uso.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Os dois fluxos chamam o mesmo `CheckoutCalculator.calculate(tx, request)`: o resumo em `src/main/java/shop/order/OrderService.java:41` e o registro em `:46`. O registro só acrescenta a criação do cliente e do pedido e grava o `Summary` devolvido (`:47-52`). Nenhuma etapa do cálculo é refeita em outro lugar. O `Summary` só arredonda o que recebe (`src/main/java/shop/checkout/Summary.java:20-29`), e os dois fluxos passam por ele igual.
Conclusão: segue

**A6, como os pacotes estão organizados?** A organização é mista, predominantemente por funcionalidade: `catalog`, `checkout`, `coupon`, `customer`, `notification`, `order`, `payment` e `shipping`, cada um com seus tipos e suas interfaces de repositório. Há dois pacotes técnicos de camada: `persistence`, com a implementação JDBC de todos os assuntos e as abstrações `Tx` e `Store`, e `web`, com uma classe só, `Routes`. No pacote raiz ficam a montagem (`App`, `Main`), utilitários de todos os pacotes (`Money`, `ApiException`) e um serviço de consultas (`QueryService`).
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o HTTP da regra de negócio.** O negócio passaria a lançar uma recusa só com o código do problema (por exemplo `BusinessRefusal("COUPON_NOT_APPLICABLE")`, ou tipos como "não encontrado", "conflito de situação" e "indisponível"), e o `Routes` faria o mapeamento para 422/409/404/503. O `Routes` também passaria a ler o `transactionId`, o `paidAt` e o `trackingCode` e entregaria ao `OrderService` valores já tipados (`confirmPayment(id, transactionId, paidAt)`), em vez do `JsonNode`. É o que derruba o A1, e isola o negócio da forma como ele é chamado.
2. **Desfazer o laço `order` ↔ `checkout`.** O `CheckoutCalculator` receberia a informação de primeira compra de fora, por uma interface própria de `checkout` ou pelo próprio `CouponContext` montado por quem chama, em vez de consultar o `OrderRepository`. Na mesma passada, mover `Tx` e `Store` para fora de `persistence` desfaria os laços com `persistence`.

## Dúvidas

- **`CheckoutRequest`.** A régua cita os objetos da API "quando moram com os controllers", e este mora em `checkout`. Li o record como objeto de entrada da API mesmo assim, porque ele espelha o corpo cru e se monta de um `JsonNode`. Ele pesa só como reforço, e a conclusão do A1 não depende dele: os códigos HTTP no `ApiException` e o `JsonNode` no `OrderService` já bastam.
- **Laço com `persistence`.** O laço `order`/`notification` ↔ `persistence` fecha por interfaces de repositório, que não são tipo de dado nem uma regra chamando outra. Tratei como comportamento causado pelo lugar das interfaces e não como o laço decisivo. A conclusão do A2 vem do laço `order` ↔ `checkout`, que não deixa dúvida.

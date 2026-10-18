# arquitetura

## Visão geral

O agente organizou o serviço por assunto (pedido, checkout, catálogo, cupom, entrega, pagamento, clube, cliente, aviso), com um pacote `common` técnico. Três coisas estão bem feitas. O acesso ao banco fica todo em repositórios com `JdbcClient`. Os controllers só traduzem a requisição. E o resumo e o pedido usam o mesmo cálculo, porque `OrderService.create` chama `CheckoutService.summarize`. O problema mais sério é que a regra de negócio conhece a web, de duas formas. Primeiro, ela recebe o corpo da requisição HTTP (`CheckoutRequest`, com tudo em `String` e `Integer`) nos dois serviços centrais. Segundo, as recusas de negócio carregam o código HTTP (`ApiException`/`ErrorCode` com `HttpStatus`), que os cupons, as entregas, os pagamentos e o `OrderService` lançam. O `OrderService` chega a escolher `HttpStatus.CONFLICT` ele mesmo. O segundo problema é um ciclo de cinco pacotes que fecha também por comportamento: `order` chama o serviço de `checkout`, e `checkout` chama o repositório de `order`.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório diz que nenhuma classe de fora da web usa a "incoming web" (`architecture/architecture-report.txt`, a última linha). Mas há duas dependências de HTTP que ele não pega:
- **O objeto de entrada da API.** `CheckoutRequest` (`src/main/java/br/com/loja/checkout/CheckoutRequest.java:5-7`) é o corpo JSON da requisição: todos os códigos vêm como `String` e `installments` como `Integer` anulável. Ele mora no mesmo pacote do `CheckoutController`, que o recebe com `@RequestBody` (`checkout/CheckoutController.java:17`). Mesmo assim, a regra de negócio recebe esse objeto direto: `CheckoutService.summarize(CheckoutRequest request)` (`checkout/CheckoutService.java:54`) e `OrderService.create(CheckoutRequest request)` (`order/OrderService.java:50`), que vem do pacote de outro controller. O relatório confirma as duas: `order.OrderService -> checkout.CheckoutRequest` e `order.OrderController -> checkout.CheckoutRequest`. Do lado da saída, `OrderSummary` (`checkout/OrderSummary.java:5`) é o valor do domínio e, ao mesmo tempo, o corpo da resposta, devolvido direto pelo controller (`checkout/CheckoutController.java:17`). Esse caso é menos grave: é um record de valores sem nada de HTTP, e conta mais como um atalho do que como uma dependência da web.
- **Os códigos HTTP.** `ErrorCode` liga cada recusa a um `org.springframework.http.HttpStatus` (`common/ErrorCode.java:3-26`). `ApiException` guarda e expõe esse `HttpStatus` (`common/ApiException.java:3,8,24`). Essa exceção é lançada pelas classes de negócio, com o status HTTP embutido: `coupon/Welcome10Coupon.java:22`, `coupon/Minus50Coupon.java:24`, `coupon/FreeShippingCoupon.java:21`, `shipping/CourierShipping.java:24`, `shipping/CarrierShipping.java:33,35`, `payment/BankSlipPayment.java:152`, `payment/CreditCardPayment.java:110`, `checkout/CheckoutService.java:100,115,118,121,133` e `order/OrderService.java:85,119,154,159`. O caso mais claro é `OrderService`: ele importa `HttpStatus` (`order/OrderService.java:11`) e decide a situação HTTP da recusa do pagamento, `new ApiException(result.error(), HttpStatus.CONFLICT)` (`order/OrderService.java:76`). Ele faz isso porque `INSUFFICIENT_STOCK` tem que ser 409 aqui e 422 no resumo. Na prática, a regra de negócio escolhe o código HTTP.

Uma classe de negócio que conhece a web já basta, e aqui são várias.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório traz um grupo: `catalog, checkout, coupon, customer, order`. Abri os laços:
- **`checkout` ⇄ `order`: fecha por comportamento nos dois sentidos.** Num sentido, `order.OrderService -> checkout.CheckoutService`: o pedido chama o cálculo (`order/OrderService.java:51`). No outro, `checkout.CheckoutService -> order.OrderRepository`: o cálculo consulta os pedidos para saber se é a primeira compra (`checkout/CheckoutService.java:38,77`, chamando `OrderRepository.hasPurchaseByCustomer`, em `order/OrderRepository.java:102`). São serviços e repositórios dos dois pacotes chamando uns aos outros. É estrutural.
- **`customer` ⇄ `order`: também passa por comportamento, mas o desenho é mais defensável.** `order.OrderService -> customer.CustomerRepository` cria o cliente (`order/OrderService.java:58`). Na volta, `customer.LoyaltyCreditListener` implementa `order.OrderEventListener` e lê `order.Order` (`customer/LoyaltyCreditListener.java:10,19-27`). A volta é o padrão observador, com a interface no lado de quem publica. Não é um tipo de dado, mas também não é um serviço chamando o outro de frente. O que fecha o laço é a ida (o pedido mexendo direto no repositório de clientes).
- **`catalog` ⇄ `order`: leve.** Fecha pelo tipo de dado `catalog.ProductRepository -> order.OrderItem` (`catalog/ProductRepository.java:11,40,59`). A outra direção é `OrderService` usando `ProductRepository` (`order/OrderService.java:95,144`). `OrderItem` é um record `(sku, quantity)` (`order/OrderItem.java:3`) que poderia mudar de pacote.
- **`checkout` ⇄ `coupon`: leve.** A volta é o tipo de dado `checkout.CartLine`, usado em `coupon.CouponContext` e `coupon.Buy3Pay2Coupon` (`coupon/CouponContext.java:6,9`, `coupon/Buy3Pay2Coupon.java:7,21`). Ela poderia sumir se `CartLine` fosse para outro pacote.
- `customer -> checkout` é só o record `OrderSummary` (`customer/LoyaltyCreditListener.java:20`). `order -> checkout` também usa `OrderSummary` em `Order` e `OrderRepository`.

O pior laço (`checkout` ⇄ `order`) fecha por comportamento.
Conclusão: não segue

**A3, o controller só traduz?** Os controllers estão finos:
- `OrderController` só converte o corpo nulo em argumentos nulos, chama `OrderService` e monta `OrderResponse` (`order/OrderController.java:39-68`). Os 201 e 200 ficam ali.
- `CheckoutController` delega direto (`checkout/CheckoutController.java:17-19`).
- `ProductController` e `CustomerController` leem do repositório e traduzem "não achei" em 404 (`catalog/ProductController.java:20-22`, `customer/CustomerController.java:25-29`).

Nenhum faz conta, muda a situação do pedido ou mexe no estoque. O único ponto que fica em dúvida é `NotificationController`. Ele junta dois repositórios para aplicar a regra "avisos de um CPF que nunca registrou pedido são `CUSTOMER_NOT_FOUND`" (`notification/NotificationController.java:27-29`). Ele também marca o aviso como lido chamando o repositório direto (`notification/NotificationController.java:36`). Li isso como tradução de "não existe" para 404, e não como uma recusa do negócio. Por isso não pesa na conclusão.
Conclusão: segue

**A4, o acesso ao banco está isolado?** Todo o SQL e o `JdbcClient` ficam em quatro repositórios: `catalog/ProductRepository.java`, `order/OrderRepository.java`, `customer/CustomerRepository.java` e `notification/NotificationRepository.java`. Um `grep` por `jdbc`, `JdbcClient`, `DataSource` e `java.sql` fora dos `*Repository.java` não acha nada. Os serviços só chamam métodos dos repositórios (`order/OrderService.java:58,62,95,98,144`, `checkout/CheckoutService.java:77,112`). Há o vazamento no sentido contrário, de regra para dentro do acesso a dados, que não é o que o item pergunta:
- o "tudo ou nada" da baixa de estoque está em `ProductRepository.reserve` (`catalog/ProductRepository.java:40-57`);
- a definição de "primeira compra" (os status que contam) está no SQL de `OrderRepository.hasPurchaseByCustomer` (`order/OrderRepository.java:102-106`).
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** `POST /checkout/summary` chama `CheckoutService.summarize` (`checkout/CheckoutController.java:18`). `OrderService.create` chama o mesmo método e grava o `OrderSummary` que volta, sem recalcular nada (`order/OrderService.java:51,54-56,62`). Não há segunda cópia de nenhuma etapa. O subtotal, o frete, o cupom, o seguro e o pagamento existem só em `checkout/CheckoutService.java:66-93`.
Conclusão: segue

**A6, como os pacotes estão organizados?** A organização é por funcionalidade: `catalog`, `checkout`, `coupon`, `club`, `customer`, `notification`, `order`, `payment` e `shipping`. Cada um tem o seu controller, serviço e repositório quando precisa. Há ainda um pacote técnico transversal, `common`, com a exceção, o mapeamento dos erros, o tratador da web e o `Money` (`architecture/architecture-report.txt`, a seção "Packages"). O `LojaApplication` fica no pacote raiz. A camada web não é um pacote à parte: cada controller mora com o seu assunto.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar a web da regra de negócio.** Os serviços passariam a receber um comando próprio, com os enums já convertidos (por exemplo, `CheckoutCommand` com `Region`, `ShippingOption` etc.), e o controller faria a tradução de `CheckoutRequest`. As recusas seriam uma exceção de domínio só com o código. O mapeamento para `HttpStatus` ficaria no `ApiExceptionHandler`, que também resolveria o 409 contra 422 do `INSUFFICIENT_STOCK` pelo contexto, em vez de o `OrderService` escolher `HttpStatus.CONFLICT`. Isso resolve o A1 e deixa o cálculo testável sem a forma do JSON.
2. **Quebrar o laço `checkout` ⇄ `order`.** O `CheckoutService` receberia a informação de primeira compra de uma interface própria do checkout (algo como `PurchaseHistory`), implementada no lado de pedidos, ou o `OrderSummary` iria para um pacote neutro. Assim o cálculo não depende mais do repositório de pedidos, e o ciclo restante fica só com laços de tipo de dado.

## Dúvidas

- **A1, os códigos HTTP.** O relatório não marca `HttpStatus` como "incoming web", mas a régua conta os códigos HTTP. Li `ApiException`/`ErrorCode` como uma dependência da web nas classes de negócio. Mesmo sem isso, o `CheckoutRequest` recebido pelos dois serviços já decidiria o item.
- **A1, o `OrderSummary`.** Tratei o fato de ele servir de corpo de resposta como um atalho menor, e não como uma dependência da web: é um record só de valores, e é o domínio que a resposta reaproveita, não o contrário.
- **A2, o laço `customer` ⇄ `order`.** A volta é a implementação de uma interface de evento, que não é tipo de dado nem uma chamada direta entre serviços. Não o usei para decidir a conclusão, que já vem do laço `checkout` ⇄ `order`.
- **A3, o `NotificationController`.** Li a checagem de cliente existente como tradução de "não encontrado", e não como uma decisão de negócio.

# a arquitetura

## Visão geral

O agente organizou o serviço por assunto. O cálculo do resumo está num lugar só, `CheckoutCalculator`, e cada parte variável (entrega, cupom, pagamento) fica atrás de uma interface. Todo o SQL está em classes `@Repository` que usam `JdbcClient`, e os controllers são finos. O problema mais sério é que a regra de negócio conhece a web. A exceção de negócio `ApiException` carrega um `HttpStatus` do Spring, e o cálculo, as entregas e o ciclo do pedido decidem eles mesmos o código HTTP da recusa: 422, 409, 404 e até um 503 escrito à mão. Além disso, há um `@Service` no pacote do pedido que devolve `ResponseEntity` e depende de um controller. O segundo problema é que seis dos dez pacotes estão num ciclo. A maior parte dele fecha por tipos de dado, mas o pedido e o catálogo, e o pedido e o cliente, ficam presos por comportamento: o `OrderService` chama esses pacotes diretamente, enquanto eles reagem ao pedido como listeners dele.

## Item a item

**A1, a regra de negócio não conhece a web?** Não. A recusa de negócio é `ApiException`, que guarda um `org.springframework.http.HttpStatus` (`src/main/java/com/loja/orders/common/ApiException.java:3,8,17-27`). Por isso, quem recusa escolhe o código HTTP:
- o cálculo do resumo recusa com 422 (`checkout/CheckoutCalculator.java:121-123`);
- o serviço do pedido recusa com 409 e 404 (`order/OrderService.java:155,171`);
- `PaymentRejectedException` fixa `HttpStatus.CONFLICT` (`order/PaymentRejectedException.java:3,12`);
- `CarrierShippingOption`, uma regra de entrega, monta `new ApiException(HttpStatus.SERVICE_UNAVAILABLE, …)` (`shipping/CarrierShippingOption.java:6,39`);
- cupons e pagamento também recusam com 422 (`coupon/Coupons.java:38-40`, `payment/BankSlipPayment.java:31`, `shipping/CourierShipping.java:26`).

O relatório confirma o caso mais direto: `order.OrderReceipts` usa `catalog.ProductController` e `ResponseEntity` (`architecture/architecture-report.txt`, as duas seções finais). É um `@Service` do pacote do pedido que recebe um controller por injeção e devolve `ResponseEntity<OrderResponse>` (`order/OrderReceipts.java:3,6,8,12,19-20`). Ninguém chama essa classe, mas ela compila e é um bean. Há ainda um detalhe menor: o `OrderService` recebe o `paidAt` como o texto do JSON e faz ele mesmo o parse do formato da API (`order/OrderService.java:75,77,159-168`).

Os objetos de entrada e saída estão quase limpos. `CheckoutRequest` é um record próprio, sem tipo web, e `OrderResponse` só é usado pelos controllers e pelo `OrderReceipts`. O `CheckoutSummary` é o tipo de domínio e também é serializado direto como corpo da resposta (`checkout/CheckoutController.java:19-20`). Isso não faz o domínio importar a web. Como várias classes de negócio conhecem códigos HTTP e tipos do Spring web, o item não é atendido.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório tem um grupo: catalog, checkout, coupon, customer, order e shipping. Fechando pelas dependências de classe:
- **checkout ↔ order** fecha pelo record `OrderItem`, usado por `CheckoutCalculator`, `CheckoutRequest` e `CheckoutRequestParser`. Do outro lado, `OrderService` chama `CheckoutCalculator`. É um tipo de dado: leve.
- **checkout ↔ coupon** fecha pelo record `CartItem` (`coupon/CouponContext.java:6`, `coupon/Buy3Pay2Coupon.java:7`). Leve.
- **checkout ↔ shipping** fecha pelo enum `Region` (`shipping/ShippingRequest.java:5,8`, `shipping/CarrierShippingOption.java:41`). Leve, embora `Region` more no checkout com regra de seguro dentro.
- **customer → checkout** é `CheckoutSummary`, e **coupon → catalog** é `Product`, ambos dados. **order → coupon** é `OrderRepository implements PurchaseHistory`, uma porta bem invertida.
- **order ↔ catalog** fecha por comportamento. `OrderService` chama `InventoryService.take` diretamente (`order/OrderService.java:16,90`) e `OrderReceipts` depende de `ProductController`. No sentido contrário, `StockReturnListener` implementa `OrderLifecycleListener` e devolve o estoque quando o pedido é cancelado (`catalog/StockReturnListener.java:11,20-23`). A mesma preocupação usa dois mecanismos: a baixa por chamada direta, a devolução por listener.
- **order ↔ customer** também fecha por comportamento. `OrderService` chama `CustomerRepository.createIfAbsent` (`order/OrderService.java:22,63`), e `ClubCreditListener` implementa o listener do pedido (`customer/ClubCreditListener.java:11,20-28`).

Mesmo levando todos os tipos de dado (`OrderItem`, `CartItem`, `Region`, `CheckoutSummary`, `Product`) para um pacote neutro, os laços order↔catalog e order↔customer continuam. Eles fecham por `InventoryService` e `CustomerRepository` de um lado e pelo contrato `OrderLifecycleListener` do outro. A maior parte do grupo é leve, mas o centro dele é estrutural.
Conclusão: não segue

**A3, o controller só traduz?** Sim:
- `CheckoutController` faz parse e chama o cálculo (`checkout/CheckoutController.java:19-21`).
- `OrderController` lê o corpo, chama o `OrderService` e devolve `OrderResponse`, com o 201 declarado (`order/OrderController.java:29-60`). Nenhum dos dois faz conta nem muda a situação do pedido.
- `ProductController`, `CustomerController` e `NotificationController` vão direto ao repositório e só traduzem "não achou" em 404 (`catalog/ProductController.java:21`, `customer/CustomerController.java:21`, `notification/NotificationController.java:35`).

O único ponto discutível é `NotificationController.list`, que confere se o cliente existe antes de listar (`notification/NotificationController.java:27-29`). Eu leio isso como uma consulta que vira 404, e não como uma decisão de negócio. As conferências de `INVALID_ORDER` (carrinho vazio, SKU repetido, quantidade) estão no `CheckoutRequestParser` (`checkout/CheckoutRequestParser.java:55-69`). Ele fica no caminho de entrada, mas é validação de entrada e não decide regra de negócio.
Conclusão: segue

**A4, o acesso ao banco está isolado?** Sim. Só os repositórios usam `JdbcClient`, `java.sql` e SQL: `catalog/ProductRepository.java`, `order/OrderRepository.java`, `customer/CustomerRepository.java` e `notification/NotificationRepository.java`. Além deles, só a configuração do `DataSource` (`config/AppConfig.java`, `config/JdbcUrl.java`). A regra de negócio fala com os dados por métodos com nome de domínio:
- `InventoryService` usa `lockStock` e `changeStock` (`catalog/InventoryService.java:30,36,43-44`);
- `OrderService` usa `lockById`, `markPaid` e `savePaymentNotice` (`order/OrderService.java:76,93-94`);
- o cupom WELCOME10 consulta o histórico pela interface `PurchaseHistory` (`coupon/Welcome10Coupon.java:15,28`), implementada no repositório (`order/OrderRepository.java:21,121-129`).

O `OrderService` usa `TransactionTemplate` e `@Transactional`, mas isso é controle de transação, não SQL.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Sim. O resumo chama `calculator.calculate(parser.parse(body))` (`checkout/CheckoutController.java:20`), e o registro chama o mesmo `calculator.calculate(request)` (`order/OrderService.java:52`). O resultado é gravado inteiro (`order/OrderRepository.java:46-56`) e, na leitura, é só remontado, sem recalcular nada (`order/OrderRepository.java:137-148`). Nenhuma etapa é refeita num dos fluxos.

Há uma duplicação dentro do próprio cálculo: o brinde é decidido ali mesmo, com `tier == ClubTier.GOLD && subtotal > 500.00` (`checkout/CheckoutCalculator.java:104`), embora `ClubTier.gift` faça a mesma regra pela tabela dos níveis (`checkout/ClubTier.java:39-41`) e não seja usado. Os dois fluxos passam pela mesma linha, então isso não separa o resumo do pedido.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por funcionalidade: `catalog`, `checkout`, `coupon`, `customer`, `notification`, `order`, `payment` e `shipping`, com o subpacote `shipping.carrier` para a transportadora. Cada pacote junta seu controller, seu repositório, seus serviços e seus listeners. Há dois pacotes técnicos ao lado: `common` (exceções, `Money`, leitura de JSON) e `config`. `Region` e `ClubTier` moram no `checkout`, apesar de servirem também às entregas.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o HTTP da regra de negócio.** A exceção de negócio deveria carregar só o `ErrorCode`, ou ter subtipos como "não encontrado", "conflito" e "indisponível". O `ApiExceptionHandler` faria a tradução para o código HTTP. `OrderReceipts` deveria ser apagado ou ir para a camada web. Isso resolve o A1 de uma vez e deixa o cálculo e o ciclo do pedido independentes do jeito como são chamados.
2. **Quebrar o laço do pedido com o estoque e o cliente.** A baixa do estoque e a criação do cliente deveriam passar por portas do próprio pacote do pedido, ou pelos mesmos listeners, como já acontece com a devolução e o crédito. Junto com isso, `OrderItem` e `CartItem` iriam para um lugar neutro. O ciclo de seis pacotes viraria, no máximo, dependências por dados.

## Dúvidas

- **`OrderReceipts`:** contei como classe de negócio por ser um `@Service` fora da camada web, mesmo sem ninguém que o chame. Ainda que fosse desconsiderado, o A1 continuaria `não segue` por causa do `HttpStatus` dentro de `ApiException` e de `CarrierShippingOption`.
- **Peso do A2:** a régua não diz quanto pesa um ciclo que fecha por uma chamada direta de um lado e pelo contrato de listener do outro. Tratei como comportamento, portanto estrutural, porque não se desfaz só mudando um tipo de dado de pacote.

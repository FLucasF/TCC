# arquitetura

## Visão geral

O agente organizou o serviço por assunto: catálogo, checkout, cupom, entrega, pagamento, clube, cliente, aviso e pedido. A web fica num pacote `api` à parte. O acesso ao banco está bem isolado: todo o SQL fica em quatro classes `*Repository` com `JdbcClient`. O resumo e o registro do pedido usam o mesmo `CheckoutService.summarize`, e o `OrderController` só repassa as chamadas. Os pontos fortes são esses três: o banco isolado, um cálculo só e um controller enxuto. Há dois problemas sérios, do mais grave para o menos grave:

1. **Um ciclo estrutural entre pacotes.** `checkout → coupon → order → checkout` fecha por comportamento nos três elos: o checkout chama o cupom, o `Welcome10Coupon` consulta o `OrderRepository` e o `OrderService` chama o `CheckoutService`.
2. **A regra de negócio conhece os códigos HTTP.** O checkout, o pedido e a entrega lançam `ApiException`, que carrega a situação HTTP (422, 409, 404). Uma delas é escrita à mão: `new ApiException("CARRIER_UNAVAILABLE", 503)`.

Há também um problema menor: o `CatalogAndCustomerController` não tem serviço por trás. Ele fala direto com os repositórios, abre a transação e decide a recusa dos avisos de um CPF desconhecido.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha nenhuma classe fora da web que use tipos do Spring MVC ou o `ResponseEntity` ("Classes outside the web layer that use the incoming web: none"). Mesmo assim, a regra de negócio conhece os **códigos HTTP**:

- **A exceção.** `common/ApiException.java:7-25` guarda um `int status`, e as fábricas dela já fixam a situação HTTP: `unprocessable` → 422, `conflict` → 409, `notFound` → 404. O `ApiExceptionHandler` só copia esse número para a resposta (`api/ApiExceptionHandler.java:15`). Quem escolhe o código HTTP, portanto, é a classe de negócio.
- **As classes de negócio que escolhem o código HTTP:**
  - `CheckoutService`, em `checkout/CheckoutService.java:131-133`, para todas as recusas do cálculo.
  - `OrderService`, em `order/OrderService.java:81, 112, 118, 146, 171, 176`. Ali ele decide entre 409 e 404, e na recusa repetida do pagamento remonta `ApiException.conflict(previous)` (linha 118).
  - `CourierShipping`, em `shipping/CourierShipping.java:21`.
  - `CarrierShippingOption`, em `shipping/CarrierShippingOption.java:32-33`. Na linha 33 a regra de entrega escreve o 503 à mão.

O certo seria uma exceção de negócio só com o código do problema (ou com um tipo de recusa), e a tabela "código → situação HTTP" ficaria no `ApiExceptionHandler`.

Há um ponto secundário. A tradução do corpo HTTP mora no pacote de negócio: `CheckoutRequest.parse(Map<String, Object>)` (`checkout/CheckoutRequest.java:26-39`) conhece o formato do JSON (`"customer"`, `"zipCode"` e os outros nomes de campo) e é chamado pelo controller com o mapa cru (`api/OrderController.java:27, 33`). Isso não é um tipo do Spring, mas é o objeto de entrada da API, posto no `checkout`. Por outro lado, `OrderSummary` e `Order` são tipos de negócio que o controller devolve direto como resposta. Isso não faz o negócio depender da web.

Como uma classe de negócio que conhece a web já basta, e aqui são quatro:
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório traz um grupo, `checkout, coupon, order, shipping`. Ele tem três laços:

- **`checkout ↔ shipping`**, leve. Na ida há comportamento: o `CheckoutService` chama `ShippingOption.quote` (`checkout/CheckoutService.java:61`). A volta fecha só pelo enum `checkout.Region`, usado em `shipping.ShippingRequest` e em `shipping.CarrierShippingOption`, que só lê `dispatchDelayDays()` (`shipping/CarrierShippingOption.java:31`). O `Region` poderia ir para outro pacote sem mudar o desenho.
- **`checkout ↔ coupon`**, leve. A volta fecha pelo record `checkout.PricedItem`, usado em `coupon.CouponContext` e em `coupon.Buy3Pay2Coupon` (`coupon/CouponContext.java:7`, `coupon/Buy3Pay2Coupon.java:19`). É um tipo de dado.
- **`checkout → coupon → order → checkout`**, estrutural. Os três elos são comportamento:
  - `checkout.CheckoutService` chama `coupon.Coupon.applicable/discount` (`checkout/CheckoutService.java:72, 75`);
  - `coupon.Welcome10Coupon` chama `order.OrderRepository.hasPaidOrder` (`coupon/Welcome10Coupon.java:26`);
  - `order.OrderService` chama `checkout.CheckoutService.summarize` (`order/OrderService.java:48, 53`).

  Não há tipo de dado que se possa mudar de pacote para desfazer esse laço. Para quebrá-lo, a regra do cupom precisaria receber o dado "já tem pedido pago" de fora, ou consultar por uma interface do próprio pacote do cupom.

Um laço por comportamento já basta.
Conclusão: não segue

**A3, o controller só traduz?** O `OrderController` só traduz. Ele monta o pedido de entrada, chama o `OrderService` e devolve o resultado (`api/OrderController.java:25-59`). O `text()` da linha 61 só extrai os campos do corpo. Nenhuma conta, nenhuma mudança de situação, nenhum estoque.

O `CatalogAndCustomerController` não tem serviço por trás e trabalha direto com três repositórios (`api/CatalogAndCustomerController.java:20-29`):
- Ele decide uma recusa do negócio. O enunciado diz que os avisos de um CPF que nunca registrou pedido são `CUSTOMER_NOT_FOUND`, e essa regra está no controller (`api/CatalogAndCustomerController.java:43`).
- Ele muda a situação de um aviso: abre a transação com `@Transactional`, confere se o aviso existe, marca como lido e relê (`api/CatalogAndCustomerController.java:47-53`).

As duas coisas são pequenas e não tocam no pedido, no cálculo nem no estoque. Mesmo assim, são decisões e mudanças de estado fora da camada de regra.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** O SQL fica todo em quatro classes de acesso a dados, todas com `JdbcClient`: `catalog/ProductRepository.java`, `order/OrderRepository.java`, `customer/CustomerRepository.java` e `notification/NotificationRepository.java`. Uma busca por `jdbc`, `JdbcClient`, `SELECT` e `UPDATE` fora dos `*Repository.java` não acha nada. A regra de negócio fala com os dados só por métodos com nome de intenção:
- `OrderService` usa `findForUpdate`, `lockStock`, `decrementStock` e `recordPaymentOutcome` (`order/OrderService.java:130-137, 162`);
- `Welcome10Coupon` usa `hasPaidOrder` (`coupon/Welcome10Coupon.java:26`);
- os listeners usam `addCredit` e `create` (`order/CustomerCreditListener.java:17`, `order/CustomerNotificationListener.java:18`).

Os bloqueios (`FOR UPDATE`) também ficam no repositório.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** O resumo (`OrderService.summarize`) e o registro (`OrderService.register`) chamam o mesmo `CheckoutService.summarize` (`order/OrderService.java:47-49` e `:53`). O `OrderSummary` que esse método devolve é o mesmo objeto que é gravado (`order/OrderRepository.java:26-42`). A consulta do pedido só relê as colunas gravadas, sem recalcular (`order/OrderRepository.java:94-98`), e o crédito do clube no pagamento usa o `nextPurchaseCredit` gravado (`order/CustomerCreditListener.java:17`). Não há segunda cópia de nenhuma etapa.
Conclusão: segue

**A6, como os pacotes estão organizados?** O desenho é misto, quase todo por funcionalidade. Há um pacote por assunto: `catalog`, `checkout`, `coupon`, `shipping`, `payment`, `club`, `customer`, `notification` e `order`, e cada um traz o seu repositório (por exemplo, `order/OrderRepository.java`, `catalog/ProductRepository.java`). Ao lado deles há duas fatias horizontais: `api`, com os dois controllers e o tratador de erros, e `common`, com `Money` e `ApiException`. O enum `Region` mora em `checkout`, embora também sirva à entrega. É esse detalhe que fecha um dos laços do A2. O item é só descritivo.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o código HTTP da regra de negócio.** A `ApiException` passaria a carregar só o código do problema (ou um tipo de recusa: inválido, conflito, não encontrado, indisponível), e a escolha entre 422, 409, 404 e 503 iria para o `ApiExceptionHandler`. Hoje a entrega, o checkout e o pedido decidem a resposta HTTP, e uma delas escreve o 503 à mão (`shipping/CarrierShippingOption.java:33`).
2. **Quebrar o laço `checkout → coupon → order → checkout`.** Duas saídas possíveis:
   - o `CheckoutService` consulta "o CPF já tem pedido pago" e passa o resultado no `CouponContext`;
   - o pacote do cupom declara uma interface própria de consulta, implementada no `order`.

   Qualquer uma tira a dependência `coupon → order`. Com isso, o pacote do pedido fica acima do checkout, sem que o checkout dependa dele por baixo.

## Dúvidas

- **A1.** Li "os códigos HTTP" da régua como incluindo a situação HTTP guardada numa exceção do pacote `common` e escolhida pelas classes de negócio, mesmo sem nenhum tipo do Spring MVC. O relatório não pega esse caso, porque `ApiException` só usa `int`. Se a régua quisesse contar só os tipos do Spring e os objetos da API do projeto, o A1 cairia para `em parte`, por causa do `CheckoutRequest.parse` com o JSON cru.
- **A3.** Contei como problema uma recusa pequena e a marcação do aviso como lido, ambas no controller, embora a régua dê como exemplos só o pedido e o estoque. Por isso `em parte`, e não `não segue`.
- **A6.** O item é descritivo, então usei `não se aplica` como conclusão.

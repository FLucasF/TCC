# arquitetura

## Visão geral

O agente organizou o serviço por camadas (`controller`, `service`, `repository`, `model`, `domain`, `dto`). As dependências entre pacotes descem num sentido só, e o relatório não acha nenhum ciclo. O que vai bem: os pacotes não formam ciclo. O acesso ao banco passa todo por repositórios Spring Data. O controller só traduz. O resumo e o pedido usam o mesmo cálculo, porque `createOrder` chama o próprio `calculateSummary`.

O problema mais sério está em A1. A regra de negócio recebe e devolve os objetos da API: o `OrderService` recebe `OrderRequest`, `PaymentRequest` e `ShipmentRequest` e devolve `SummaryResponse` e `OrderResponse`. O `NotificationService` devolve `NotificationResponse`. Por isso a tradução de e para HTTP fica dentro do serviço, e não na camada web. Há também problemas menores, que não decidem nenhum item:
- o `OrderService` concentra quase tudo: validação, frete, prazo, cupom, ciclo do pedido, estoque e conversão para a resposta;
- a soma dos produtos é recalculada em três lugares.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha nenhuma classe de negócio com tipos do Spring MVC ou `ResponseEntity`. O problema está nos objetos da própria API, que o relatório mostra na dependência `service -> dto` (6 dependências de classe):
- `OrderService.calculateSummary(OrderRequest)` devolve `SummaryResponse` (`service/OrderService.java:40`). O serviço também lê os enums como texto direto do corpo da requisição e faz a conversão ele mesmo (`service/OrderService.java:438-480`).
- `createOrder(OrderRequest)` devolve `OrderResponse` (`service/OrderService.java:142`).
- `processPayment(Long, PaymentRequest)` (`service/OrderService.java:184`) e `shipOrder(Long, ShipmentRequest)` (`service/OrderService.java:227`) recebem os corpos de requisição.
- O próprio serviço monta a resposta HTTP em `convertToResponse`, com `OrderResponse.OrderSummaryResponse` (`service/OrderService.java:482-510`).
- `NotificationService.getNotifications` e `markAsRead` devolvem `NotificationResponse` (`service/NotificationService.java:18`, `:29`).

Esses records são o contrato JSON da API. Eles têm `@JsonProperty` (`dto/OrderRequest.java`, `dto/SummaryResponse.java`) e moram no mesmo pacote que o `ErrorResponse` do controller. O agente fez a separação certa só em dois casos, `getProduct` e `getCustomer`: neles o serviço devolve tipos próprios (`OrderService.ProductDto` e `CustomerDto`, `service/OrderService.java:536-548`) e o controller traduz (`controller/OrderController.java:111-120`, `:129-134`). Isso mostra que o resto não segue o mesmo desenho. Há duas classes de negócio que conhecem a web, e uma já bastaria.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório diz "Cycles: 0 group(s), 0 of 8 packages caught in one". As nove dependências entre pacotes vão todas num sentido só: `controller -> service/dto`, `service -> domain/dto/model/repository/util`, `model -> domain` e `repository -> model`. Não há laço para examinar.
Conclusão: segue

**A3, o controller só traduz?** Cada método do `OrderController` chama um método do serviço e transforma o resultado ou a exceção num código HTTP. Nenhum faz conta, muda a situação do pedido ou mexe no estoque (`controller/OrderController.java:22-158`). A única ressalva está nos dois `catch (Exception e)` do resumo e do registro, que respondem sempre `503` com `"CARRIER_UNAVAILABLE"` escrito à mão no controller (`controller/OrderController.java:29-30`, `:41-42`). A decisão de recusar com esse código é do serviço (`service/OrderService.java:91`, `:93`, `ServiceException("CARRIER_UNAVAILABLE")`), então o controller ainda está traduzindo. Mas ele ignora a mensagem da exceção e trata qualquer erro inesperado como transportadora fora do ar. É uma tradução imprecisa, não uma regra de negócio no controller.
Conclusão: segue

**A4, o acesso ao banco está isolado?** Os quatro repositórios são interfaces `JpaRepository`. As consultas próprias são JPQL em `@Query` dentro dessas interfaces, por exemplo:
- `OrderRepository.hasCompletedOrders` e `findByIdForUpdate` (`repository/OrderRepository.java:11-16`);
- `ProductRepository.findBySkuForUpdate` (`repository/ProductRepository.java:10-12`).

Uma busca por `Jdbc`, `EntityManager`, `createQuery`, `nativeQuery`, `DataSource` e `java.sql` não acha nada no código principal. Os serviços só falam com os dados pelos repositórios.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** `createOrder` começa com `SummaryResponse summary = calculateSummary(request)` (`service/OrderService.java:143`) e grava exatamente esses valores no `OrderSummary` do pedido (`service/OrderService.java:163-177`). Então tudo o que entra no resumo vale igual para o pedido: frete, prazo, validação do cupom, `PricingService.calculateSummary` e o limite do boleto. Não há uma segunda cópia do cálculo em nenhum dos dois fluxos.

Há duas ressalvas que não separam os fluxos:
- A soma dos produtos é recalculada em três lugares: `OrderService.calculateProductsSubtotal` (`service/OrderService.java:316`, e o resultado da linha 105 nem é usado), dentro de `validateCoupon` (`service/OrderService.java:369-371`) e em `PricingService.calculateProductsSubtotal` (`service/PricingService.java:71`).
- A aplicabilidade do cupom é decidida duas vezes. `validateCoupon` chama `Coupon.calculateDiscount` com um contexto inventado, `STORE_PICKUP` e frete zero (`service/OrderService.java:377-382`). O `PricingService` chama o mesmo método com o contexto real (`service/PricingService.java:98-106`).

Esse é o tipo de cópia parcial que pode divergir. Hoje as duas partes estão no caminho comum ao resumo e ao pedido, então não fazem um mostrar valor diferente do outro.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada técnica:
- `controller`: um único `OrderController` para todas as rotas;
- `service`: `OrderService`, `PricingService`, `NotificationService` e o cliente HTTP `TransLogClient`;
- `repository`;
- `model`: as entidades JPA;
- `domain`: os enums com um pouco de regra (`Coupon`, `Region`, `ClubTier`);
- `dto`, `util` e `config`.

Não há pacote por assunto (pedido, catálogo, cupom, cliente).
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar os DTOs da API da fronteira dos serviços.** O `OrderService` passaria a receber um comando próprio, com os enums já convertidos, e a devolver o `OrderSummary` e o `Order` (ou tipos de resultado próprios), como já faz com `ProductDto` e `CustomerDto`. A conversão de `OrderRequest` e para `OrderResponse`/`NotificationResponse` iria para a camada web. Isso resolve A1 e deixa a regra de negócio usável sem HTTP.
2. **Separar o `OrderService`.** Ele hoje junta validação, cotação de frete, cupom, ciclo do pedido, estoque, crédito e avisos. A validação do cupom com contexto inventado e as três somas de produtos deveriam ficar só no `PricingService`, para que cada etapa do cálculo exista num lugar só.

## Dúvidas

- Em A1, a régua fala dos records da API "quando moram com os controllers". Aqui eles moram num pacote `dto` separado. Contei esses records como objetos da API mesmo assim. Eles são os corpos JSON do contrato, com `@JsonProperty`, e o mesmo pacote guarda o `ErrorResponse`, que só o controller usa. A separação em outro pacote não muda o que eles são.
- O `TransLogClient` usa HTTP de saída (`java.net.http`) e mora no pacote `service` (`service/TransLogClient.java`). Não contei isso em A1, que trata da web de entrada. Fica só a observação de que é infraestrutura misturada com os serviços de negócio.

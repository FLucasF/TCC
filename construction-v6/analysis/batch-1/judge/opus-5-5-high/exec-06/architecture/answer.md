# arquitetura

## Visão geral

O agente organizou o código por assunto: cada vantagem que muda (entrega, cupom, nível do clube, forma de pagamento) é uma interface com uma implementação por opção. Os controllers ficam num pacote `web` à parte. Há três pontos fortes. Primeiro, o cálculo do resumo é um só, usado pelo resumo e pelo registro do pedido. Segundo, todo o SQL mora em classes `*Repository` com `JdbcClient`. Terceiro, nenhuma classe de fora da web usa a web de entrada ou depende de um controller. Os problemas, do mais grave para o menos grave:

1. **A regra de negócio escolhe o código HTTP de cada recusa.** A exceção do domínio carrega um `org.springframework.http.HttpStatus`. Os serviços, cupons e entregas escolhem 422, 409, 404 ou 503 na hora de recusar, e o handler da web só copia esse valor.
2. **Há ciclos entre pacotes que fecham por comportamento.** `checkout` e `order` chamam serviços e repositórios um do outro nos dois sentidos. O mesmo acontece entre `order` e `customer`.
3. **O `CustomerController` faz o papel de serviço.** Ele vai direto aos repositórios, guarda a regra "CPF sem pedido não tem avisos" e conduz a transação de marcar um aviso como lido.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório diz que nenhuma classe fora da web usa a web de entrada ("use the incoming web: none"). Mas ele não conta o `HttpStatus`, e a régua conta os códigos HTTP.

- `src/main/java/br/com/loja/pedidos/common/DomainException.java:3` importa `org.springframework.http.HttpStatus`. O campo `status` (`:8`) e as fábricas `unprocessable` (`:17`, `UNPROCESSABLE_ENTITY`), `conflict` (`:21`, `CONFLICT`) e `notFound` (`:25`, `NOT_FOUND`) põem o código HTTP dentro da exceção de negócio.
- Cada classe de negócio escolhe o código ao recusar:
  - `checkout/CheckoutService.java:57-147` usa `unprocessable` para todas as recusas do resumo.
  - `order/OrderService.java:78`, `:101`, `:112`, `:121` e `:129` usam `conflict`.
  - `catalog/StockService.java:28` usa `conflict("INSUFFICIENT_STOCK")`. Já `CheckoutService.java:147` usa `unprocessable` para o mesmo código. A diferença entre 409 e 422 é decidida dentro do negócio.
  - Também escolhem o código `coupon/Welcome10Coupon.java:23`, `coupon/Minus50Coupon.java:24`, `coupon/FreeShippingCoupon.java:20`, `shipping/CourierShipping.java:24` e `shipping/TransLogShipping.java:31`.
- `shipping/HttpTransLogClient.java:15` e `:80` importam `HttpStatus` e escolhem `SERVICE_UNAVAILABLE` para `CARRIER_UNAVAILABLE`.
- `web/ApiExceptionHandler.java:18` só faz `ResponseEntity.status(e.status())`. A tradução para HTTP não acontece na camada web, só é copiada ali.

Há dois pontos secundários:
- `order/Order.java:6` e `:17-18` usam `@JsonIgnore` do Jackson para moldar a resposta da API no próprio registro do domínio.
- `checkout/CheckoutRequest.java` é o corpo da requisição tal como o site o envia: os códigos chegam como `String` (`:12-17`) e o serviço o recebe direto (`CheckoutService.java:51`, `OrderService.java:48`). Ele mora em `checkout`, não junto dos controllers. Por isso trato esse ponto como dúvida, não como evidência decisiva.

Só o `DomainException` com `HttpStatus`, usado por todas as classes de negócio, já basta.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório traz um grupo com cinco pacotes: `catalog`, `checkout`, `coupon`, `customer` e `order`. Os laços dentro dele:

- **`checkout` ↔ `order`, por comportamento nos dois sentidos.**
  - `order.OrderService -> checkout.CheckoutService`: `OrderService.java:49` chama `checkout.calculate(request)`.
  - `checkout.CheckoutService -> order.OrderRepository`: `CheckoutService.java:110` chama `orders.hasPurchase(cpf)` para saber se é a primeira compra.
  - Esse laço também tem dependências de dados (`order.Order -> checkout.Summary`, `checkout.CheckoutRequest -> order.OrderItem`), mas quem fecha o círculo são as duas chamadas de serviço e repositório. É acoplamento estrutural.
- **`order` ↔ `customer`, por comportamento.**
  - `order.OrderService -> customer.CustomerRepository`: `OrderService.java:50` chama `customers.createIfAbsent`.
  - `customer.ClubCreditListener -> order.OrderEventListener` (`customer/ClubCreditListener.java:9`): o `customer` implementa o ouvinte que o `order` chama em `OrderService.java:90` e `:139`.
  - O ouvinte, sozinho, seria uma inversão de dependência bem feita. Mas o `order` também chama o repositório de `customer` direto, então o laço fecha por comportamento nos dois sentidos.
- **`order` ↔ `catalog`, leve.** `OrderService.java:84` chama `stock.reserve`, que é comportamento. A volta é só o tipo de dado `order.OrderItem` (`catalog/StockService.java:9` e `:22`), que poderia mudar de pacote.
- **`checkout` ↔ `coupon`, leve.** `CheckoutService` usa a interface `Coupon`, que é comportamento. A volta é o record `checkout.CartLine` (`coupon/CouponContext.java:7` e `:10`, e `Buy3Pay2Coupon`), que é dado.
- **`coupon` → `catalog`:** só o record `Product`.

Há laços que fecham por comportamento (um basta), e o pior é `checkout` ↔ `order`.
Conclusão: não segue

**A3, o controller só traduz?** O `OrderController` só traduz: cada endpoint delega ao serviço (`web/OrderController.java:37`, `:43`, `:48`, `:53-54`, `:59`, `:64`, `:69`). O `required` (`:72-77`) só transforma um corpo ausente em `INVALID_ORDER`. Nenhuma conta, mudança de situação ou estoque acontece nele. O `CustomerController` foge disso:

- `web/CustomerController.java:35` guarda a regra do enunciado de que os avisos de um CPF sem pedido são `CUSTOMER_NOT_FOUND`. Para isso, ele consulta o `CustomerRepository` antes de listar os avisos.
- `:39-45` é a operação "marcar aviso como lido". Ela abre a transação (`@Transactional`, `:40`), confere se o aviso existe, muda o status pelo repositório (`markRead`) e relê o aviso. Isso é a regra e a mudança de estado do aviso, montadas no controller.

O `CatalogController` (`web/CatalogController.java:22`) também vai direto ao repositório, mas ali só há uma busca com 404, o que é tradução aceitável. Os desvios são pequenos e ficam fora do fluxo do pedido: o pedido, o estoque e as contas estão limpos.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** Todo SQL está em classes `@Repository` com `JdbcClient`: `catalog/ProductRepository.java`, `order/OrderRepository.java`, `customer/CustomerRepository.java` e `notification/NotificationRepository.java`. As classes de regra de negócio (`CheckoutService`, `OrderService`, `StockService`) só chamam métodos desses repositórios. A trava de estoque (`SELECT … FOR UPDATE`, `ProductRepository.java:27`) e a de pedido (`OrderRepository.java:52`) também ficam no acesso a dados. Os serializadores Jackson do resumo (`OrderRepository.java:37` e `:96`) e a transação própria do `createIfAbsent` (`CustomerRepository.java:23-24` e `:36`) ficam nos repositórios, como detalhe de persistência. Não encontrei SQL nem JDBC em classe de negócio.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Os dois fluxos chamam o mesmo cálculo:
- o resumo: `OrderController.java:37` chama `checkout.calculate(request)`;
- o registro: `OrderService.java:49` chama `checkout.calculate(request)` e grava esse mesmo `Summary` no pedido (`:51-54`).

O resultado é serializado inteiro (`OrderRepository.java:37`) e relido sem conta nenhuma (`:96`). Nenhuma etapa é refeita em outro lugar. O crédito do clube, por exemplo, é lido do resumo guardado (`ClubCreditListener.java:19` e `:25`), não recalculado.
Conclusão: segue

**A6, como os pacotes estão organizados?** É misto. A maior parte é por assunto: `catalog`, `checkout`, `club`, `coupon`, `customer`, `notification`, `order`, `payment` e `shipping`, cada um com seus tipos, serviços e repositório. Há também uma camada `web` horizontal, com os controllers e o handler de erros de todos os assuntos, e um `common` com `Money`, `Region`, `Coded` e `DomainException`. O relatório lista as 11 pacotes com 3 a 9 classes cada, mais a `PedidosApplication` na raiz.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o `HttpStatus` do `DomainException`.** A exceção carregaria só o código de negócio, ou um tipo próprio de recusa (inválido, conflito, não encontrado, indisponível). O `ApiExceptionHandler` faria o mapeamento para HTTP. Isso resolve o A1 de uma vez e põe a decisão de 409 ou 422 na camada que fala HTTP.
2. **Quebrar o laço entre `checkout` e `order`.** O `CheckoutService` receberia a resposta de "é primeira compra?" por uma porta do próprio `checkout` (uma interface que o `order` implementa), em vez de chamar o `OrderRepository`. Do mesmo jeito, a criação do cliente poderia sair do `OrderService` para um ouvinte de "pedido registrado" em `customer`. Assim o `order` só dependeria de `checkout` e `customer` num sentido.

## Dúvidas

- **No A1, o `HttpStatus` conta.** O relatório diz que nenhuma classe usa a web de entrada porque não conta o `org.springframework.http.HttpStatus`. Como a régua inclui expressamente os códigos HTTP, eu os contei.
- **O `CheckoutRequest` não decidiu o A1.** Ele é o corpo da requisição tal como o site envia, mas mora em `checkout`, e a régua só cita os objetos da API que moram com os controllers. Tratei como ponto secundário.
- **No A2, considerei o laço `order` ↔ `customer` comportamental.** Um lado é um ouvinte, ou seja, uma interface do `order` implementada em `customer`. Mas o outro lado é uma chamada direta do `OrderService` ao `CustomerRepository`, por isso o laço fecha por comportamento. A conclusão do item não depende dele: o laço `checkout` ↔ `order` sozinho já decide.
- **No A3, os desvios do `CustomerController` pesaram como `em parte`.** São regras pequenas, fora do pedido, e não foram suficientes para `não segue`.

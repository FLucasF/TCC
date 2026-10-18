# arquitetura

## Visão geral

O projeto está dividido por camada: `controller`, `dto`, `service`, `repository`, `domain`, `exception`, `integration` e `config`. Dentro de `service`, as partes que variam (cupom, entrega, pagamento, clube) têm subpacotes próprios. O desenho tem pontos bons. Não há nenhum ciclo entre pacotes. Os controllers só traduzem. O banco é acessado só por repositórios Spring Data. O resumo e o registro do pedido usam o mesmo cálculo: `OrderService.createOrder` chama `CheckoutService.calculateSummary`. O problema mais sério é a fronteira com a web, e ele aparece de duas formas. Primeiro, os dois serviços centrais recebem os records do corpo da requisição HTTP (`OrderRequest`, `PaymentRequest`). Segundo, o catálogo de erros que a regra de negócio lança já traz o código HTTP de cada erro (`ErrorCode` com `HttpStatus`). Com isso, a regra de negócio escolhe a resposta HTTP. Isso já causa um erro concreto: `INSUFFICIENT_STOCK` está fixo em 409, mas no resumo o enunciado pede 422.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha nenhuma classe fora da web usando anotações ou tipos do Spring MVC. Mas ele mostra `service -> dto` (3 dependências de classe), e essas dependências são os objetos da API:
- `CheckoutService` importa `com.loja.dto.OrderRequest` (`src/main/java/com/loja/service/CheckoutService.java:4`). Recebe esse record em `calculateSummary(OrderRequest request)` (`CheckoutService.java:53`) e trabalha com `OrderRequest.ItemInfo` (`CheckoutService.java:121`, `:149`, `:159`).
- `OrderService` importa `OrderRequest` e `PaymentRequest` (`service/OrderService.java:4-5`). Recebe esses records em `createOrder(OrderRequest)` (`OrderService.java:45`) e em `processPayment(Long, PaymentRequest)` (`OrderService.java:61`).
- Esses records são exatamente os corpos JSON que os controllers recebem com `@RequestBody` (`controller/CheckoutController.java:21`, `controller/OrderController.java:20`, `:34`).

Há ainda os códigos HTTP. Todos os serviços lançam `ErrorCode.X.asException()`, e `ErrorCode` amarra cada código a um `org.springframework.http.HttpStatus` (`exception/ErrorCode.java:3`, `:6-26`). `BusinessException` carrega esse `HttpStatus` (`exception/BusinessException.java:3`, `:7`). Assim, quem decide o HTTP é a regra de negócio, e não a camada web. O efeito aparece no código:
- `INSUFFICIENT_STOCK(HttpStatus.CONFLICT)` (`ErrorCode.java:9`) é o mesmo código lançado na validação do resumo (`CheckoutService.java:142`), onde o enunciado pede 422. O 409 só deveria valer no pagamento.

Duas classes de negócio centrais conhecem a web. Isso já basta para não seguir.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório diz "Cycles: 0 group(s), 0 of 8 packages caught in one". As dependências vão num só sentido:
- `controller -> service/dto`;
- `service -> domain/repository/integration/exception/dto`;
- `dto -> domain` e `repository -> domain`;
- `exception -> integration`, só porque o handler trata `TransLogUnavailableException`.

Nenhum pacote volta para quem o usa.
Conclusão: segue

**A3, o controller só traduz?** Os cinco controllers fazem o mesmo trabalho: recebem a requisição, chamam um método do serviço e convertem o resultado com `XxxResponse.from(...)`. Por exemplo:
- `CheckoutController.java:20-24`;
- `OrderController.java:19-57`. O único trabalho extra é tirar `trackingCode` do corpo, em `OrderController.java:43`;
- `CustomerController.java:19-23`.

Nenhum controller faz conta, decide recusa, muda a situação do pedido ou mexe no estoque. As recusas saem do serviço como exceção e viram HTTP no `GlobalExceptionHandler` (`exception/GlobalExceptionHandler.java:10-24`).
Conclusão: segue

**A4, o acesso ao banco está isolado?** Os serviços falam com os dados só pelas interfaces `JpaRepository` do pacote `repository` (`repository/ProductRepository.java:12-17`, `CustomerRepository.java:12-17`, `OrderRepository.java:10-12`, `NotificationRepository.java:10-12`). As únicas consultas escritas à mão são as JPQL com trava pessimista (`ProductRepository.java:14-16`, `CustomerRepository.java:14-16`), e elas ficam nos repositórios. Procurei `JdbcTemplate`, `EntityManager`, `createQuery`, `createNativeQuery` e `java.sql` nos fontes e não achei nada. A carga do catálogo também passa pelo repositório (`config/DataInitializer.java:12-38`).
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** `OrderService.createOrder` obtém o resumo chamando `checkoutService.calculateSummary(request)` (`OrderService.java:46`), o mesmo método do `POST /checkout/summary` (`CheckoutController.java:22`). Depois só monta o `Order` com esse `OrderSummary` (`domain/Order.java:47-66`). Não há nenhuma etapa refeita em só um dos fluxos.
Conclusão: segue

**A6, como os pacotes estão organizados?** É uma organização por camada: `controller` (5 classes), `dto` (8), `service` (25), `repository` (4), `domain` (16), `exception` (3), `integration` (2) e `config` (1). Dentro de `service` há subpacotes pelos pontos que variam: `club`, `coupon`, `payment`, `shipping`. Não é um corte por assunto do negócio (pedido, catálogo, cliente).
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o HTTP do catálogo de erros.** `ErrorCode` e `BusinessException` deveriam carregar só o código do negócio. A tradução para `HttpStatus` passaria a ser feita no `GlobalExceptionHandler`, que é quem conhece o contexto: com isso, o mesmo `INSUFFICIENT_STOCK` pode virar 422 no resumo e 409 no pagamento. É a mudança que mais rende, porque já está errando um status hoje.
2. **Separar os serviços dos records da API.** `CheckoutService` e `OrderService` deveriam receber um comando próprio do negócio (por exemplo, um `CheckoutCommand` com itens, região, entrega etc.) e dados simples no pagamento. A conversão de `OrderRequest`/`PaymentRequest` ficaria no controller. Assim, uma mudança no JSON do site não chega mais à regra de cálculo.

## Dúvidas

- A régua cita os records da API "quando moram com os controllers". Aqui eles moram num pacote separado, `dto`. Mesmo assim, contei `OrderRequest` e `PaymentRequest` como objetos da API: são os corpos do `@RequestBody`, espelham o JSON do anexo e só existem por causa do HTTP. Por isso a dependência `service -> dto` pesa em A1.
- `HttpStatus` vem de `org.springframework.http` e não da API do Spring MVC, então o relatório não o pega. Contei mesmo assim, porque a régua cita expressamente "os códigos HTTP". De todo modo, os records da API já bastariam para a conclusão de A1.

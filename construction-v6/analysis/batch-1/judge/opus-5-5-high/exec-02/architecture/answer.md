# arquitetura

## Visão geral

O projeto é organizado por camadas: `controller`, `service`, `repository`, `domain`, `dto`, mais `integration`, `exception`, `config` e `util`. As dependências andam todas num sentido só e não há ciclo entre pacotes. Os controllers são finos. O acesso ao banco passa por repositórios Spring Data. O resumo e o pedido registrado usam o mesmo cálculo. O problema mais sério está na fronteira com a web: a regra de negócio recebe e devolve os objetos da própria API. São eles o `CheckoutSummaryRequest`, o `PaymentNotificationRequest`, o `OrderResponse`, o `CheckoutSummary` e os outros `*Response` do pacote `dto`. Além disso, ela decide os códigos HTTP, porque cada recusa é lançada com um `HttpStatus` do Spring. O pedido chega a gravar no banco o DTO de resposta `CheckoutSummary`, serializado em JSON. Um ponto menor, que não é de negócio: o mesmo `try/catch` que traduz a exceção para HTTP aparece copiado em todos os endpoints, e não existe um tratador central.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acusa nenhuma classe usando a web de entrada. Ele procura as anotações e os tipos do Spring MVC e o `ResponseEntity`; o `HttpStatus` e os DTOs do projeto ficam de fora dessa busca. Lendo o código, a regra de negócio conhece a web de dois jeitos.

- **Os códigos HTTP.** A exceção de negócio guarda um `org.springframework.http.HttpStatus` (`src/main/java/com/loja/orderservice/exception/OrderServiceException.java:3,223`). Os quatro serviços importam o `HttpStatus` e escolhem o código HTTP de cada recusa:
  - `src/main/java/com/loja/orderservice/service/CheckoutService.java:15,100,162,224` (`HttpStatus.UNPROCESSABLE_ENTITY`);
  - `src/main/java/com/loja/orderservice/service/OrderService.java:21,66,76,91,117` (`NOT_FOUND`, `CONFLICT`);
  - `src/main/java/com/loja/orderservice/service/CustomerService.java:12,382`;
  - `src/main/java/com/loja/orderservice/service/CatalogService.java:8,456`.
- **Os objetos da API.** O relatório mostra `service -> dto` com 9 dependências de classe. São os mesmos tipos que os controllers recebem por `@RequestBody` e devolvem no corpo da resposta:
  - `CheckoutService.calculateSummary(CheckoutSummaryRequest)` devolve `CheckoutSummary` (`.../service/CheckoutService.java:38`);
  - `OrderService.createOrder(CheckoutSummaryRequest)` e `processPayment(String, PaymentNotificationRequest)` devolvem `OrderResponse` (`.../service/OrderService.java:44,62`);
  - o `CustomerService` monta `CustomerResponse` e `NotificationResponse` (`.../service/CustomerService.java:387,405`), e o `CatalogService` monta `ProductResponse` (`.../service/CatalogService.java:461`);
  - o controller repassa esses tipos sem traduzir (`.../controller/OrderController.java:150-152`, `.../controller/CheckoutController.java:53-55`).

  O caso mais grave é o pedido gravar o `CheckoutSummary`, o DTO de resposta, serializado no banco (`.../service/OrderService.java:52,322-324`) e lido de volta para baixar o crédito e devolver o estoque (`.../service/OrderService.java:94,143,240`). Com isso, a forma do JSON de resposta passa a ser também o formato persistido.

Os DTOs moram num pacote `dto` separado dos controllers, mas são os objetos de entrada e saída da API, e a régua os inclui. Uma classe de negócio que conhece a web já basta, e aqui são as quatro.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório não mostra nenhum grupo ("Cycles: 0 group(s), 0 of 9 packages caught in one"). As 13 dependências entre pacotes andam num sentido só: `controller -> service -> repository/integration/util -> domain`, com `dto -> domain`.
Conclusão: segue

**A3, o controller só traduz?** Todos os métodos dos cinco controllers fazem a mesma coisa: chamam um método do serviço e embrulham o resultado num `ResponseEntity.ok` ou `status(CREATED)`. A recusa vira `{error}` com o status que a exceção já traz (`.../controller/OrderController.java:149-215`, `.../controller/CheckoutController.java:52-61`, `.../controller/CatalogController.java:20-29`, `.../controller/CustomerController.java:22-39`, `.../controller/NotificationController.java:20-29`). O máximo que fazem fora disso é tirar o `trackingCode` do corpo (`.../controller/OrderController.java:56`). Nenhum controller faz conta, decide recusa, muda a situação do pedido ou mexe no estoque. A validação e as transições estão no `OrderService` e no `CheckoutService`. Uma observação que não muda a conclusão: como o serviço já decide o código HTTP (A1), o controller só copia o status, e o `catch` aparece repetido em todos os endpoints em vez de ficar num `@ControllerAdvice`. Isso é cópia de código, não decisão de negócio no controller.
Conclusão: segue

**A4, o acesso ao banco está isolado?** Nenhuma classe usa JDBC, `EntityManager`, `@Query` ou SQL montado; o `grep` por `Jdbc|createQuery|createNativeQuery|EntityManager|@Query|DataSource` não acha nada. Os serviços falam com os dados só pelas quatro interfaces `JpaRepository` (`.../repository/ProductRepository.java`, `OrderRepository.java`, `CustomerRepository.java`, `NotificationRepository.java`), e a carga do catálogo também passa pelo repositório (`.../config/DataInitializer.java:345`).
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** `OrderService.createOrder` chama o mesmo `checkoutService.calculateSummary(request)` que o `POST /checkout/summary` usa (`.../service/OrderService.java:45`, `.../controller/CheckoutController.java:55`). O resultado é gravado e não é recalculado depois: o pagamento e o cancelamento leem o crédito do resumo gravado (`.../service/OrderService.java:94,143,240`). Não há uma segunda cópia do cálculo, nem inteira nem de uma etapa. Como o registro recalcula do zero, a TransLog é consultada de novo e pode cotar diferente. Mas é o mesmo código rodando nos dois fluxos, que é o que o item pergunta.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada. São `controller` (5 classes), `service` (4), `repository` (4), `domain` (11, com `entity` e `enums`) e `dto` (9), mais os pacotes técnicos `integration` (o cliente da TransLog), `exception`, `config` e `util` (`architecture/architecture-report.txt`, a lista "Packages (9)"). Toda a regra de negócio do resumo fica numa classe só, o `CheckoutService` (391 linhas), e todo o ciclo do pedido no `OrderService` (353 linhas).
Conclusão: não se aplica

## O que mudaria primeiro

1. **Separar a regra de negócio da web.** Os serviços passariam a receber e devolver tipos próprios, por exemplo um comando de compra e um resumo de domínio, e os controllers fariam a tradução de e para os DTOs. A exceção de negócio carregaria só o código do problema, e um `@ControllerAdvice` decidiria o status HTTP. Assim se resolve o A1 e some o `catch` copiado em todos os endpoints. Isso também tira o DTO de resposta do formato que vai para o banco (`summary_json`).
2. **Persistir o resumo como dado do pedido, não como JSON do DTO.** Por exemplo, um valor embutido no `Order`. Hoje qualquer mudança no JSON da resposta muda, sem ninguém perceber, o que fica gravado e o que o pagamento e o cancelamento leem de volta.

## Dúvidas

- **O `HttpStatus` conta como web.** Ele vem de `org.springframework.http`, não do Spring MVC, e o relatório não o conta como web de entrada. Como a régua cita expressamente "os códigos HTTP", contei como conhecimento da web.
- **Os DTOs contam como objetos da API.** A régua fala dos objetos da API "quando moram com os controllers". Aqui eles moram num pacote `dto` à parte, mas são exatamente os corpos de requisição e resposta. Li que a frase só dá o caso mais comum e contei os DTOs como objetos da API. De todo modo, só o `HttpStatus` já bastaria para `não segue` no A1.
- **A6 não pede julgamento.** A régua diz que o item é descritivo e não dá uma palavra própria para isso. Usei `não se aplica` porque o item não pede julgamento de bom ou ruim.

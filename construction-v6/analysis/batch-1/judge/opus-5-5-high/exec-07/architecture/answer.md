# a arquitetura

## Visão geral

O agente separou o projeto em camadas: `api` (controllers, DTOs, handler de erro), `application` (serviços e exceções), `domain` (entidades JPA, enums, repositórios Spring Data) e `infrastructure` (só a carga do catálogo). Há pontos fortes. Os controllers são finos e devolvem tipos próprios traduzidos para DTO. O acesso ao banco passa todo por repositórios. O resumo e o pedido saem do mesmo método de cálculo. O problema mais sério é a regra de negócio conhecer a web. `OrderService`, `OrderSummaryCalculator`, `CouponValidator` e `ValidationService` recebem diretamente os objetos de requisição da API (`CheckoutRequestDto`, `PaymentRequestDto`, `ShipmentRequestDto`, …). Além disso, as exceções de negócio guardam o código HTTP. Esse acoplamento também é o que fecha o único ciclo entre pacotes (`api` ↔ `application`), que é leve por fechar só por tipos de dado. Por fim, um controller decide uma regra de negócio: quando o cliente "existe" para os avisos.

## Item a item

**A1, a regra de negócio não conhece a web?** Não. O relatório não acha tipos do Spring MVC fora da camada web ("Classes outside the web layer that use the incoming web: none"). Mas as dependências `application -> api` do relatório mostram a regra de negócio recebendo os objetos de entrada da própria API:
- `OrderService` importa `com.example.api.dto.*` (`src/main/java/com/example/application/service/OrderService.java:3`). Recebe `CheckoutRequestDto` em `calculateSummary` e `createOrder` (`OrderService.java:44`, `:54`), `PaymentRequestDto` em `processPayment` (`:74`) e `ShipmentRequestDto` em `shipOrder` (`:117`).
- `OrderSummaryCalculator.calculate(CheckoutRequestDto)` (`src/main/java/com/example/application/service/OrderSummaryCalculator.java:3`, `:23`): o cálculo inteiro lê o DTO de entrada, inclusive como strings que ele mesmo converte em enum (`:28-30`).
- `CouponValidator.validate(CheckoutRequestDto, …)` (`src/main/java/com/example/application/service/CouponValidator.java:3`, `:20`).
- `ValidationService.validateCheckout(CheckoutRequestDto)` (`src/main/java/com/example/application/service/ValidationService.java:3`, `:19`).

Esses DTOs são classes de serialização da web, com `@JsonProperty` (`src/main/java/com/example/api/dto/CheckoutRequestDto.java:7-30`). Há ainda os códigos HTTP dentro da camada de aplicação. `ApplicationException` guarda um `httpStatus` (`src/main/java/com/example/application/exception/ApplicationException.java:5`, `:7`, `:17`), e cada exceção de negócio fixa o seu: por exemplo `super("ORDER_EXPIRED", 409)` em `OrderExpiredException.java:5`. O `GlobalExceptionHandler` só copia esse número (`src/main/java/com/example/api/exception/GlobalExceptionHandler.java:14`). Uma consequência concreta: `InsufficientStockException` traz 422 cravado, e a mesma recusa no pagamento deveria sair 409. A tradução de negócio para HTTP mora no lugar errado.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório (`architecture/architecture-report.txt`) mostra um grupo, `api` ↔ `application`. O sentido `api -> application` é o esperado: os controllers chamam os serviços e o handler conhece `ApplicationException`. O círculo fecha pelo sentido `application -> api`, e as 12 dependências desse sentido vão todas para `api.dto`: `CheckoutRequestDto`, `CustomerRequestDto`, `OrderItemDto`, `PaymentRequestDto` e `ShipmentRequestDto`, usados por `OrderService`, `OrderSummaryCalculator`, `CouponValidator` e `ValidationService`. Abri esses DTOs: são só campos, getters e setters com anotações Jackson (`src/main/java/com/example/api/dto/OrderItemDto.java`, `CustomerRequestDto.java`, `PaymentRequestDto.java`, `ShipmentRequestDto.java`). Nenhum serviço ou regra de `api` é chamado por `application`. O ciclo fecha por tipo de dado: os DTOs poderiam mudar de pacote, ou ser trocados por tipos próprios, sem mudar o desenho. É leve. O peso desse acoplamento já foi contado em A1.
Conclusão: em parte

**A3, o controller só traduz?** Quase sempre. `CheckoutController` e `OrderController` só chamam o serviço e montam o DTO (`src/main/java/com/example/api/controller/OrderController.java:57`, `convertToDto`). Nenhum controller faz conta, muda a situação do pedido ou mexe no estoque. Mas `CustomerController.getNotifications` decide uma regra de negócio: quem "existe" como cliente. Ele recusa com `CustomerNotFoundException` quando a lista de avisos vem vazia (`src/main/java/com/example/api/controller/CustomerController.java:39-41`). A regra do enunciado é "CPF que nunca registrou pedido", e quem decide isso deveria ser o serviço. Do jeito que está, um cliente com pedido registrado e ainda sem aviso recebe 404. Há dois casos menores. `ProductController.java:23-25` e `CustomerController.java:30-32` convertem um `null` do serviço em recusa: isso ainda é tradução da ausência, mas a decisão de recusar fica no controller. `NotificationController.markAsRead` orquestra buscar, marcar e buscar de novo (`src/main/java/com/example/api/controller/NotificationController.java:22-27`), um passo de fluxo que caberia ao serviço.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** Sim. Os quatro repositórios são interfaces Spring Data (`src/main/java/com/example/domain/repository/*.java`). A única consulta escrita é um JPQL no próprio repositório (`NotificationRepository.java:11`). Nenhuma classe de negócio usa JDBC ou monta SQL: a busca por `jdbc`, `createQuery`, `SELECT` fora dos repositórios não acha nada. Uma ressalva que não muda a conclusão: `CouponValidator.validateWelcome10` carrega todos os pedidos com `orderRepository.findAll()` e filtra o CPF e as situações em memória (`CouponValidator.java:44-49`). A consulta deveria morar no repositório. Mesmo assim, ela passa por ele.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Sim. `createOrder` chama `calculateSummary` (`OrderService.java:55`), o mesmo método do `POST /checkout/summary`, e esse método devolve `summaryCalculator.calculate(request)` (`:51`). O pedido guarda exatamente esse resumo (`:57`). Nenhuma etapa é refeita só num dos fluxos. Há repetições internas, mas elas valem igual nos dois fluxos:
- o peso total tem três cópias (`OrderService.java:224`, `OrderSummaryCalculator.java:186`, `ValidationService.java:136`);
- o limite de 5 kg do motoboy aparece em `ValidationService.java:90-91` e `OrderSummaryCalculator.java:154-155`;
- as condições do `MINUS50` e do `FREESHIPPING` aparecem no validador (`CouponValidator.java:57`, `:64`) e no cálculo (`OrderSummaryCalculator.java:100`, `:105`);
- a TransLog é consultada de novo a cada passo (`OrderService.java:173`, `OrderSummaryCalculator.java:148`), e no boleto o resumo inteiro é calculado duas vezes (`OrderService.java:187` e `:51`).

São riscos de manutenção: as cópias podem divergir entre a validação e o cálculo. Mas não fazem o resumo e o pedido darem valores diferentes.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada. No topo ficam `api`, `application`, `domain` e `infrastructure` (relatório: 17, 30, 18 e 1 classes). Dentro, mais subpacotes de camada: `controller`, `dto`, `exception`, `service`, `entity`, `enums`, `repository`. O assunto (pedido, catálogo, cupom, cliente) não organiza nenhum pacote. Uma observação descritiva: o cliente HTTP da transportadora (`TransLogService`) fica em `application.service` (`src/main/java/com/example/application/service/TransLogService.java:17`), e o pacote `infrastructure` tem só o `DataInitializer`.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar os DTOs da web e os códigos HTTP da camada de aplicação.** O serviço passaria a receber um comando próprio (itens, cliente, região, entrega etc. já como tipos do domínio). O controller faria a conversão do `CheckoutRequestDto`. As exceções ficariam só com o código de negócio, e a tabela "código → HTTP" ficaria no `GlobalExceptionHandler`. Isso resolve A1 e desfaz o ciclo de A2 de uma vez, e permite que a mesma recusa (`INSUFFICIENT_STOCK`) saia com HTTP diferente conforme a operação.
2. **Levar ao serviço a decisão de quem é cliente.** A regra "CPF sem pedido registrado → `CUSTOMER_NOT_FOUND`" sairia de `CustomerController.java:40` e iria para `CustomerService`/`NotificationService`, que já têm os dados para responder certo.

## Dúvidas

- A6 é descritivo e a régua não dá uma palavra própria para ele. Usei `não se aplica` para não registrar um juízo de bom ou ruim. A descrição do item ("por camada") é o que vale.
- Em A3, li a conversão de "não achou" em recusa (`ProductController.java:23-25`, `CustomerController.java:30-32`) como tradução, aceitável. O que levou a `em parte` foi só a regra dos avisos em `CustomerController.java:40-41`, que é decisão de negócio.
- Em A5, as repetições entre validação e cálculo (peso, limite do motoboy, condições de cupom, várias consultas à TransLog) não contam como "duas cópias do cálculo": as duas rotas passam pelo mesmo caminho e recebem o mesmo resumo. Por isso ficou `segue`, com a ressalva.

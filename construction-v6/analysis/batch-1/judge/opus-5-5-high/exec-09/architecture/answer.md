# arquitetura

## Visão geral

O agente organizou o serviço por camada técnica (controller, service, repository, domain, dto), com dois pacotes à parte para os padrões (`state` e `strategy`). O acesso ao banco está bem isolado: só há repositórios Spring Data, e o único JPQL fica em `repository`. O cálculo do resumo é um só: o registro do pedido reaproveita o mesmo método do resumo. Os problemas, do mais grave para o menos grave:

1. **A regra de negócio conhece a web.** Os serviços, os estados e todas as estratégias recebem e devolvem os objetos da API, os DTOs com os campos do JSON (`CheckoutRequest`, `PaymentRequest`, `ShipmentRequest`, `SummaryResponse`, `OrderResponse`). Além disso, o negócio decide os códigos HTTP: `HttpStatus` está dentro da `OrderException`, que é lançada pela regra.
2. **Há um ciclo de comportamento entre `service` e `strategy`.** O `CheckoutService` chama as estratégias, e as estratégias chamam a regra de arredondamento `MoneyRounding`, que mora em `service`.
3. **Três controllers falam direto com os repositórios.** Um deles, o dos avisos, faz sozinho a mudança de NEW para READ.

## Item a item

**A1, a regra de negócio não conhece a web?** Não. O relatório marca "use the incoming web: none", porque ele só olha as anotações do Spring MVC e o `ResponseEntity`. Abrindo as classes, aparecem dois tipos de ligação com a web:
- **Os objetos da API do projeto.** O pacote `dto` guarda os corpos de requisição e resposta: o controller recebe `CheckoutRequest` por `@RequestBody` (`src/main/java/com/loja/controller/OrderController.java:19`) e devolve `OrderResponse`/`SummaryResponse`.
  - `CheckoutService.calculateSummary(CheckoutRequest)` devolve `SummaryResponse` (`src/main/java/com/loja/service/CheckoutService.java:5-6`, `:52`).
  - `OrderService.createOrder` e `processPayment` recebem `CheckoutRequest` e `PaymentRequest` e devolvem `OrderResponse` (`src/main/java/com/loja/service/OrderService.java:51`, `:116`); a resposta é montada no serviço, em `toOrderResponse` (`:319-341`).
  - A interface de estado recebe os DTOs (`src/main/java/com/loja/state/OrderState.java:4-5`, `:9-10`), e `OrderStateContext` também (`src/main/java/com/loja/state/OrderStateContext.java:5`).
  - Todas as interfaces de estratégia recebem `CheckoutRequest`: `ClubTierStrategy.java:8`, `CouponStrategy.java:11-12`, `PaymentStrategy.java:8-9` e `ShippingStrategy.java:8-9`, em `src/main/java/com/loja/strategy/...`.
  - O relatório confirma as ligações de pacote `service -> dto` (11 dependências de classe), `state -> dto` (13) e `strategy -> dto` (19).
- **Os códigos HTTP.** `OrderException` guarda um `org.springframework.http.HttpStatus` (`src/main/java/com/loja/exception/OrderException.java:3`, `:8`), e a regra de negócio escolhe o status ao lançar a exceção:
  - `CheckoutService.java:105` (`UNPROCESSABLE_ENTITY`) e `:201` (`SERVICE_UNAVAILABLE`);
  - `OrderService.java:100` (`NOT_FOUND`);
  - `AwaitingPaymentState.java:25` e `:33` (`CONFLICT`); `PaidState`, `ShippedState`, `CancelledState` e `DeliveredState` fazem o mesmo.

  A tradução do negócio para HTTP, que deveria ficar no `GlobalExceptionHandler`, foi feita dentro do próprio negócio.

Classes de negócio que conhecem a web: `CheckoutService`, `OrderService`, os 5 estados mais `OrderState` e `OrderStateContext`, e as 4 interfaces de estratégia com as 13 implementações.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório mostra dois grupos.
- **Grupo 1, `domain` ↔ `state`.**
  - O laço fecha por `domain.Order -> state.OrderState`: um campo `@Transient private OrderState state` (`src/main/java/com/loja/domain/Order.java:3`, `:74-75`), com getter e setter (`:148-149`).
  - `OrderState` é uma interface de comportamento (`handlePayment` e as outras), não um tipo de dado.
  - Mas o `Order` nunca chama esse campo, e ninguém lê `getState()`: o grep só encontra a própria declaração em `Order.java:148`. O campo só é escrito, em `OrderService.java:125` e `AwaitingPaymentState.java:30`.
  - O sentido `state -> domain` é natural: os estados mexem em `Order` e `OrderPayment`.
  - Logo, não há chamadas nos dois sentidos. É uma referência morta, que some sem mudar o desenho. Leve.
- **Grupo 2, `service` ↔ `strategy`.**
  - `service -> strategy` é o `CheckoutService` chamando as estratégias (`CheckoutService.java:236`, `:298`).
  - O laço fecha por `strategy -> service.MoneyRounding`:
    - `Welcome10Coupon.java:20` e `Buy3Pay2Coupon.java:31`, em `src/main/java/com/loja/strategy/coupon/`;
    - `PixPayment.java:21`, `CreditCardPayment.java:25` e `:33`, em `src/main/java/com/loja/strategy/payment/`.
  - `MoneyRounding` não é um record, enum ou evento: é uma regra do negócio (o arredondamento meio-para-o-par, `src/main/java/com/loja/service/MoneyRounding.java:7-8`), chamada pelas estratégias.
  - Então há regras de pacotes diferentes chamando umas às outras nos dois sentidos. Esse é o laço que fecha por comportamento, mesmo sendo um utilitário sem estado e fácil de mover.

O pior laço fecha por comportamento.
Conclusão: não segue

**A3, o controller só traduz?**
- **Pedido e resumo:** `CheckoutController` e `OrderController` só recebem, chamam o serviço e embrulham o retorno (`src/main/java/com/loja/controller/CheckoutController.java:19-21`, `OrderController.java:19-53`).
- **Cliente, produto e avisos:** três controllers falam direto com os repositórios, sem serviço no meio:
  - `CustomerController.java:21`, `ProductController.java:21` e `NotificationController.java:27-31`;
  - o `NotificationController` muda o aviso de situação sozinho: `notification.setStatus("READ")` seguido de `notificationRepository.save(...)` (`NotificationController.java:55-56`);
  - ele também aplica a regra "avisos de um CPF sem pedido são `CUSTOMER_NOT_FOUND`" (`:27-28`).
- Nenhum controller faz conta, mexe no estoque ou muda a situação do pedido.

Uma mudança de situação do negócio está no controller.
Conclusão: em parte

**A4, o acesso ao banco está isolado?**
- Todo o acesso passa por interfaces `JpaRepository` (`src/main/java/com/loja/repository/*.java`).
- O único JPQL são as consultas com trava pessimista: `ProductRepository.java:14-16` e `CustomerRepository.java:14-16`.
- O grep não acha `JdbcTemplate`, `EntityManager`, `nativeQuery` nem SQL montado em nenhuma classe de `service`, `state` ou `strategy`.
- Os estados chegam aos dados pelo `OrderStateContext`, implementado pelo `OrderService` com repositórios (`OrderService.java:215-295`).

Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?**
- `OrderService.createOrder` chama o mesmo `checkoutService.calculateSummary(request)` usado por `POST /checkout/summary` (`OrderService.java:52`, `CheckoutController.java:20`).
- Depois só copia os campos do resumo para o pedido (`OrderService.java:75-84`).
- A consulta devolve os valores gravados, sem recalcular nada (`toOrderResponse`, `OrderService.java:319-331`).
- Nenhuma etapa é refeita só num dos fluxos.

Conclusão: segue

**A6, como os pacotes estão organizados?**
- **Por camada:** `controller`, `service`, `repository`, `domain`, `dto`, `exception` e `external`.
- **Por padrão:** `state` e `strategy`. Dentro de `strategy`, os subpacotes são por assunto: `club`, `coupon`, `payment` e `shipping`.
- **No raiz:** só `LojaPedidosApplication`.

O conjunto é por camada, com um toque por assunto dentro das estratégias. É só descrição, sem juízo.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar a web da regra de negócio.** A regra passaria a receber um tipo próprio (por exemplo, um `CheckoutData` ou um comando de domínio com enums para região, entrega e pagamento) e a devolver um `Summary` próprio, com os controllers fazendo a conversão dos DTOs. A `OrderException` levaria só o código do negócio, e o `GlobalExceptionHandler` escolheria o status HTTP. Isso resolve A1, o problema mais amplo: atinge os serviços, os estados e todas as estratégias.
2. **Mover `MoneyRounding` para um pacote neutro** (`domain` ou um `money` próprio) e apagar o campo `state`, que ninguém lê, de `Order`. As duas mudanças desfazem os dois ciclos sem mexer em comportamento. Também levaria a marcação de aviso lido e as consultas de cliente e produto para serviços.

## Dúvidas

- **A2, grupo 2:** `MoneyRounding` é um utilitário estático sem estado, que muda de pacote sem mudar o desenho, como um tipo de dado. Mas a régua só dá esse tratamento leve a tipos de dado (record, enum, evento), e aqui o que atravessa o laço é uma chamada a uma regra do negócio. Li como comportamento, e por isso `não segue`. Se a régua quis tratar utilitários puros como leves, o item ficaria `em parte`.
- **A2, grupo 1:** o laço fecha por uma interface de comportamento, mas sem chamada nenhuma (o campo só é escrito). Classifiquei como leve; ele não muda a conclusão, que vem do grupo 2.
- **A1:** os DTOs moram num pacote `dto` separado de `controller`. Tratei-os como os objetos da API, porque são exatamente os corpos `@RequestBody` e de resposta dos controllers. Também contei o `HttpStatus` de `org.springframework.http` como "código HTTP" no sentido da régua, embora o relatório não o liste.
- **A6:** é um item descritivo, e o formato exige uma das quatro palavras. Usei `não se aplica` para não dar a ele um juízo que a régua diz que não tem.

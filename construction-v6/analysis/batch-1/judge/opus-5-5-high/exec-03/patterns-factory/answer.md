# Padrões: Factory

## Visão geral

A criação está concentrada onde há algo para criar. Os serviços (`PricingService`, `OrderService`, `NotificationService`, `TransLogClient`) são `@Service` injetados pelo construtor, e o único `@Bean` é o `ObjectMapper`. Nenhum serviço ou cliente é criado com `new` em outro lugar. Dos quatro pontos com casos, só os cupons têm uma casa por caso: constantes de enum com corpo, escolhidas por `valueOf`. A entrega, o pagamento e o nível do clube não têm objeto por caso: a variação está em `if`/`switch`, e isso é assunto do Strategy. O problema mais sério perto deste pattern é outro: a entrada do cupom, o `Coupon.OrderContext`, é montada em dois fluxos, cada um à sua maneira. Na validação ela é montada com uma entrega falsa (`STORE_PICKUP`, frete zero), e por isso o FREESHIPPING fica sempre recusado. Os objetos sem regra (o item, o aviso, o resumo) são criados direto, por construtor ou record, sem builder nem fábrica: contenção. Fora dos pontos aparece um exagero leve: tipos duplicados que só servem para copiar campos de um para outro.

## Item a item

**F, a criação dos objetos de cada caso.** Caso a caso:

- **Entregas:** não há objeto por caso. O enum `ShippingMethod` não tem corpo (`src/main/java/com/loja/order/domain/ShippingMethod.java:3-15`). O frete e o prazo de cada opção estão num `if/else` dentro do serviço (`src/main/java/com/loja/order/service/OrderService.java:73-95`), e a tradução para o código da TransLog também (`"STD"`/`"EXP"`, `OrderService.java:80`).
- **A dependência da TransLog:** é criada uma vez só. O `TransLogClient` é `@Service` (`src/main/java/com/loja/order/service/TransLogClient.java:14`), recebe a URL por `@Value` (`TransLogClient.java:21`), monta o `HttpClient` no próprio construtor (`TransLogClient.java:26-28`) e é injetado no `OrderService` (`OrderService.java:22,30,37`). Uma busca por `new` no projeto não acha nenhum `new TransLogClient`.
- **Cupons:** as constantes de enum com corpo (`src/main/java/com/loja/order/domain/Coupon.java:7-42`) são a casa de cada caso. Quem cria é a própria linguagem, e a escolha está num lugar só, `Coupon.fromCode` com `valueOf` (`Coupon.java:46-53`). O WELCOME10 não guarda dependência: o serviço consulta o repositório (`OrderService.java:103`, `hasCompletedOrders`) e passa só um booleano no contexto. Aqui está a única repetição de criação que encontrei: as peças do cupom são montadas em dois fluxos.
  - A conversão `SummaryItem` → `Coupon.OrderItem` está copiada em `src/main/java/com/loja/order/service/PricingService.java:94-96` e em `OrderService.java:373-375`.
  - O `Coupon.OrderContext` é criado duas vezes. No cálculo, com a entrega e o frete reais (`PricingService.java:98-103`). Na validação, com `ShippingMethod.STORE_PICKUP` e `BigDecimal.ZERO` fixos (`OrderService.java:377-382`).
  - Consequência: a validação do FREESHIPPING sempre vê "retirada" (`Coupon.java:24`), recebe `Optional.empty()` e devolve `COUPON_NOT_APPLICABLE` em qualquer pedido (`OrderService.java:384-385`). Li isso no código e não rodei o serviço; se o serviço cumpre o enunciado é o que a dimensão do comportamento mede.
- **Formas de pagamento:** não há objeto por caso. O enum `PaymentMethod` não tem corpo, e o ajuste de cada forma está num `switch` (`PricingService.java:159-181`).
- **Níveis do clube:** não há objeto por caso. O crédito está num `switch` (`src/main/java/com/loja/order/domain/ClubTier.java:9-13`), e o frete grátis e o brinde perguntam `== GOLD` (`ClubTier.java:17,21`; `PricingService.java:207`).
- **As outras peças:** são criadas pelo contêiner (`PricingService.java:10`, `OrderService.java:15`). O único `@Bean` é o `ObjectMapper` (`src/main/java/com/loja/order/config/JacksonConfig.java:11-16`).

Resumo: a criação está concentrada no contêiner do Spring e no enum dos cupons. A exceção é a entrada do cupom, montada separadamente em cada fluxo. Nas entregas, nos pagamentos e nos níveis não há o que concentrar.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Os objetos sem decisão na criação são criados direto:
- o item do carrinho é um record (`src/main/java/com/loja/order/dto/OrderRequest.java:26-29`);
- o item do pedido usa um construtor chamado por `Order.addItem` (`src/main/java/com/loja/order/model/Order.java:101-105`);
- o aviso usa um construtor que só preenche os valores iniciais fixos, `NEW` e `createdAt` (`src/main/java/com/loja/order/model/Notification.java:135-141`);
- o resumo usa um construtor (`src/main/java/com/loja/order/model/OrderSummary.java:22-46`) e um record (`src/main/java/com/loja/order/dto/SummaryResponse.java:6-26`).

O projeto não tem nenhuma classe de builder ou de fábrica. Os únicos `newBuilder` são da API do JDK (`TransLogClient.java:26,41`). Nem o `Order`, com nove parâmetros, ganhou builder (`Order.java:60-82`).
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação de objetos, há tipos paralelos que só existem para copiar campos de um para outro:
- `OrderService.ProductDto` e `OrderService.CustomerDto` (`OrderService.java:536-548`) são cópias exatas de `ProductResponse` e `CustomerResponse` (`src/main/java/com/loja/order/dto/ProductResponse.java:6-14`, `CustomerResponse.java:6-10`). O controller copia campo a campo de um para o outro (`src/main/java/com/loja/order/controller/OrderController.java:112-119,130-133`).
- No registro do pedido, o resumo faz uma volta inteira, `OrderSummary` → `SummaryResponse` → `OrderSummary`, só com cópias (`OrderService.java:126-138` e `163-175`).
- `PricingService.SummaryItem` (`PricingService.java:211-215`) e `Coupon.OrderItem` (`Coupon.java:62-66`) têm os mesmos três campos, e um é convertido no outro duas vezes (ver o F).

Nenhum desses tipos toma decisão. É uma camada que só repassa, de pouco peso: são records, sem interfaces nem fábricas.
Conclusão: exagero

## O que mudaria primeiro

1. **Montar a entrada do cupom num lugar só.** Um único método monta o `Coupon.OrderContext`, depois que a entrega e o frete já são conhecidos, e a validação e o cálculo usam esse mesmo contexto. Isso acaba com a montagem duplicada, que hoje faz o FREESHIPPING ser sempre recusado, e faz com que uma dependência nova do cupom mude um lugar só.
2. **Tirar os tipos que só copiam.** Devolver `ProductResponse` e `CustomerResponse` direto do serviço, e guardar no pedido o `OrderSummary` que o `PricingService` já criou, em vez de convertê-lo duas vezes.

## Dúvidas

- Registrei a montagem duplicada do `Coupon.OrderContext` no F, como a repetição de criação que este item pede para anotar. Mas ele é a entrada do cupom, não o objeto de um caso, e a conclusão do F continua `não se aplica`, como manda a régua.
- No E, contei como exagero os records duplicados e a volta `OrderSummary` → `SummaryResponse` → `OrderSummary`. Isso também pode ser lido como duplicação de código, coisa da arquitetura, e não como estrutura criacional. Escolhi registrar porque são tipos e conversões que nenhum ponto pede e que só repassam campos, mas é um exagero leve.

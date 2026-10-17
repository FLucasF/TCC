# Padrões: Factory

## Visão geral

O agente não criou nenhum objeto por caso. As entregas, os cupons, as formas de pagamento e os níveis do clube são enums sem corpo (`entity/ShippingMethod.java`, `entity/PaymentMethod.java`, `entity/ClubTier.java`), e os cupons nem isso: são textos comparados com `equals`. Toda a variação mora em cadeias de `if`/`else if` e num `switch`, dentro de `PricingService` e `ValidationService`. Por isso a criação concentrada não tem o que concentrar, e o problema real é de Strategy, não de Factory. As peças que existem são criadas pelo contêiner do Spring: os serviços com `@Service`, o `TransLogClient` com `@Component`, e o `RestTemplate` por um `@Bean`. Nenhuma delas é criada com `new` espalhado pelo código. Os objetos sem regra (o item, o aviso, o resumo) são criados direto pelo construtor, sem fábrica nem builder. Fora dos pontos do enunciado não achei estrutura que ninguém pediu.

## Item a item

**F, a criação dos objetos de cada caso.** Não há objeto por caso.
- **Entrega:** `PricingService.calculateShippingCost` decide com `if (method == ShippingMethod.STORE_PICKUP)` e `if (method == ShippingMethod.COURIER)` (`src/main/java/com/loja/orderservice/service/PricingService.java:148`, `:152`). A econômica e a expressa caem no mesmo trecho e são separadas por `method == ShippingMethod.ECONOMY ? "STD" : "EXP"` (`PricingService.java:160`). Os dias a mais do Norte e do Nordeste estão num `if` sobre região e entrega (`PricingService.java:166-170`).
- **Cupons:** são textos comparados com `equals`, numa cadeia de `if` em `calculateCouponDiscount` (`PricingService.java:93-104`) e de novo em `ValidationService.validateCouponApplicability` (`src/main/java/com/loja/orderservice/service/ValidationService.java:182`, `:189`).
- **Pagamento:** fica em `calculatePaymentAdjustment` (`PricingService.java:247-265`) e em `validateInstallments` (`ValidationService.java:144-152`).
- **Níveis:** ficam em `calculateNextPurchaseCredit` e `shouldGiveGift` (`PricingService.java:284-293`).

Como não há objeto por caso, não há como dizer se essa criação está concentrada. O que existe de objeto é criado pelo contêiner do Spring:
- `PricingService` é um `@Service` que recebe o `TransLogClient` pelo construtor (`PricingService.java:17-25`).
- `TransLogClient` é um `@Component` que recebe o `RestTemplate` (`src/main/java/com/loja/orderservice/client/TransLogClient.java:11-20`).
- O `RestTemplate` sai de um único `@Bean` (`src/main/java/com/loja/orderservice/config/WebClientConfig.java:9-11`).
- A dependência do WELCOME10, os pedidos do cliente, chega injetada como `OrderRepository` no `ValidationService` (`ValidationService.java:23`).

Procurei `new TransLogClient`, `new RestTemplate` e `new PricingService` no projeto: a única ocorrência é a do `@Bean`. Nenhum fluxo monta as próprias peças. Em resumo: não há objeto por caso porque a variação está em `if`/`switch` (o problema aparece no Strategy), e as dependências que existem são criadas num lugar só, pelo Spring.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são criados direto pelo construtor:
- **O item:** `new OrderItem(item.getSku(), item.getQuantity())` no registro do pedido (`src/main/java/com/loja/orderservice/service/OrderService.java:72`) e `new OrderItemDTO(...)` na resposta (`src/main/java/com/loja/orderservice/controller/OrderController.java:113`).
- **O aviso:** `new CustomerNotification(cpf, type, orderId)`, num único método auxiliar, `notifyCustomer` (`OrderService.java:247`).
- **O resumo:** `new OrderSummary(...)` com os onze valores (`PricingService.java:59-71`).

As duas classes internas que carregam resultados parciais, `ShippingCostInfo` e `PaymentAdjustmentInfo`, também só têm construtor (`PricingService.java:132-140`, `:228-240`). Procurei `Builder`, `Factory`, `of(` e `from(` estáticos: não há nenhum. É a resposta do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Não achei interface com uma implementação só, fábrica ou builder. As interfaces do projeto são os quatro repositórios Spring Data (`src/main/java/com/loja/orderservice/repository/*.java`), que são o jeito normal de usar o JPA.
- O caso mais perto de uma camada que só repassa é o `ApplicationStartupListener`: ele só chama `catalogService.initializeCatalog()` quando a aplicação fica pronta (`src/main/java/com/loja/orderservice/listener/ApplicationStartupListener.java:16-18`). Ainda assim, ele cumpre um papel: liga a carga do catálogo ao evento de subida, que o enunciado pede. Por isso não conto como exagero.
- O `@Bean` do `RestTemplate` é necessário, porque o Spring não cria um `RestTemplate` sozinho.
Conclusão: contenção

## O que mudaria primeiro

1. **Criar um objeto por caso, primeiro nas entregas e nos cupons, e deixar o Spring montá-los.** Por exemplo, um `@Component` por entrega, cada um com as próprias dependências (a econômica e a expressa com o `TransLogClient`), juntados num mapa pelo código. Hoje uma entrega nova obriga a mexer em `calculateShippingCost` e, num cupom novo, em três lugares: `PricingService.java:93-104`, `ValidationService.java:173-174` e `ValidationService.java:182-193`. Com objetos por caso, a criação já ficaria concentrada sem nenhuma fábrica à mão.
2. **Fora isso, não mudaria nada na criação.** A injeção pelo construtor e a criação direta dos objetos sem regra estão no tamanho certo.

## Dúvidas

- Considerei o `ApplicationStartupListener` como algo que cumpre um papel (ligar a carga do catálogo à subida) e não como uma camada que só repassa. Daria para chamar `initializeCatalog` dentro do próprio `CatalogService` com `@EventListener`, mas separar não cria abstração nem indireção que ninguém pediu.
- Não contei como fábrica as classes internas `ShippingCostInfo` e `PaymentAdjustmentInfo`. Elas só carregam valores e não decidem nada na criação.

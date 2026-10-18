# Padrões: Factory

## Visão geral

O agente não criou nenhum objeto por caso. As entregas, os cupons, as formas de pagamento, os níveis e as regiões são enums sem corpo (`domain/enums/ShippingMethod.java:3-8`, `CouponCode.java:3-8`, `PaymentMethod.java:3-7`, `ClubTier.java:3-7`). A variação entre os casos fica em `switch` e `if` dentro de `OrderSummaryCalculator`. Por isso a criação não tem objeto por caso para concentrar, e esse problema é do Strategy. As peças de serviço que existem (o cliente da TransLog, o calculador, os serviços) são criadas pelo contêiner do Spring com `@Service` e injeção pelo construtor. Há duas exceções, as duas num lugar só e sem repetição: `OrderService` cria à mão o `ValidationService` e o `CouponValidator`, e o `TransLogService` monta o próprio `HttpClient` e o próprio `ObjectMapper`. Os objetos sem regra (item, aviso, resumo, DTOs) são feitos com construtor direto, sem builder nem fábrica: é contenção.

## Item a item

**F, a criação dos objetos de cada caso.**
- **Não há objeto por caso.** Cada variação está escrita em `switch` ou `if` no próprio cálculo:
  - entrega: `application/service/OrderSummaryCalculator.java:145-162`;
  - cupom: `OrderSummaryCalculator.java:95-112`, mais as condições em `CouponValidator.java:27-39`;
  - região: `OrderSummaryCalculator.java:200-219`;
  - pagamento: `OrderSummaryCalculator.java:233-248`;
  - nível: `OrderSummaryCalculator.java:287-294`, `:298` e `:32`.
- **Peças de serviço criadas pelo Spring.** `TransLogService` (`@Service`, `TransLogService.java:17`) é criado uma vez e injetado em dois lugares: no `OrderSummaryCalculator` (`OrderSummaryCalculator.java:18`) e no `OrderService` (`OrderService.java:33`). Não aparece `new TransLogService` em nenhum ponto do código principal.
- **Duas peças fora do contêiner, criadas num lugar só.**
  - No construtor de `OrderService`, `new ValidationService(productService)` (`OrderService.java:39`) e `new CouponValidator(orderRepository)` (`OrderService.java:40`). O `CouponValidator` é quem guarda a dependência do WELCOME10, os pedidos do cliente. Essas classes não têm `@Component` (`ValidationService.java:11`, `CouponValidator.java:12`). A criação continua num lugar só, mas fica escondida em quem usa: se outro fluxo precisar delas, terá de repetir o `new`.
  - `TransLogService` cria seu próprio `HttpClient` e seu próprio `ObjectMapper` (`TransLogService.java:23-27`) em vez de recebê-los. A criação também é única.
- Não há `new` de casos nem de suas dependências repetido em vários fluxos.

Em resumo, a criação das peças que existem está concentrada, quase toda no Spring. Os casos em si não têm objeto, então não há o que concentrar.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são criados direto pelo construtor, sem fábrica nem builder:
- item do carrinho: `new OrderItem(sku, quantity)` (`domain/entity/OrderItem.java:25`, usado em `OrderService.java:60`);
- aviso: `new Notification(cpf, type, orderId)` (`domain/entity/Notification.java:81`, usado em `NotificationService.java:20`);
- resumo: `new OrderSummary(...)` (`domain/entity/OrderSummary.java:140`, usado em `OrderSummaryCalculator.java:62`);
- DTOs: construtor nos controllers (`api/controller/OrderController.java:74-77`, `CheckoutController.java:23`).

A busca por `Builder`, `Factory` e `@Bean` em `src/main` só encontra os builders do próprio JDK no `TransLogService` (`TransLogService.java:23`, `:37`). Há uma repetição de mapeamento: o `SummaryDto` é montado igual em `CheckoutController.java:23` e `OrderController.java:59`. Isso é duplicação, não estrutura a mais.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação, não achei nenhuma interface com uma implementação só, nenhuma fábrica, nenhum builder nem configuração `@Bean`. O único tipo auxiliar é `ShippingQuote`, uma classe simples de dois campos (`OrderSummaryCalculator.java:308-316`). Ela só carrega dados.
Conclusão: contenção

## O que mudaria primeiro

1. **Dar a cada entrega, cupom, forma de pagamento e nível um objeto próprio, registrado no Spring.** Por exemplo, um `@Component` por caso, injetado como `List`/`Map` no cálculo. Assim a econômica e a expressa recebem o cliente da TransLog e o WELCOME10 recebe o repositório de pedidos por injeção, e um caso novo vira uma classe nova, sem mexer em quem usa. Esta é a mudança principal, e ela também resolve o Strategy.
2. **Pôr `@Component` em `ValidationService` e `CouponValidator` e injetá-los** em vez de criá-los em `OrderService.java:39-40`. Toda a montagem das peças fica então no contêiner.

## Dúvidas

O `HttpClient` e o `ObjectMapper` criados dentro de `TransLogService.java:23-27` não são objetos de um caso: são dependências técnicas do cliente da TransLog. Citei esse ponto como descrição, sem contá-lo contra a concentração, porque a criação aparece uma vez só.

Li o código todo; não há objeto por caso, então o F é sobretudo descrição. Segue a correção.

# padrões: Factory

## Visão geral

O agente não criou nenhum objeto por caso. Os quatro enums de casos não têm comportamento: `ShippingMethod`, `Coupon`, `PaymentMethod` e `ClubTier` (`model/*.java`) só listam as constantes. Toda a variação mora em `switch` dentro de um serviço só, o `CheckoutCalculator`:
- a entrega em `service/CheckoutCalculator.java:214-242`;
- o cupom em `:261-292`;
- o pagamento em `:355-397`;
- o nível em `:402-406` e `:47`.

Por isso a criação concentrada não tem objetos de caso para montar. As poucas peças com dependência são o próprio `CheckoutCalculator` e o `TransLogService`, e o contêiner do Spring cria as duas uma vez (`@Service` e injeção pelo construtor). Não há `new` de peça espalhado nem repetido. No controle negativo, os objetos sem regra se criam direto, por construtor, e o projeto não tem nenhum builder nem fábrica sobrando. O problema real deste projeto é a falta de estrutura por caso, e ele pertence ao Strategy, não a esta régua.

## Item a item

**F, a criação dos objetos de cada caso.** Não há objeto por caso. As entregas, os cupons, as formas de pagamento e os níveis são constantes de enum sem corpo (`model/ShippingMethod.java`, `model/Coupon.java`, `model/PaymentMethod.java`, `model/ClubTier.java`). A regra de cada caso está em ramos de `switch` dentro de `CheckoutCalculator`, sem nenhuma casa própria:
- entrega: `service/CheckoutCalculator.java:214-242`;
- cupom: `:261-292`;
- pagamento: `:355-397`;
- crédito por nível: `:402-406`.

Sem objeto por caso, a criação não tem o que concentrar, e essa ausência já aparece como problema no Strategy.

As dependências que os casos usariam são criadas pelo contêiner do Spring, num lugar só:
- O cliente da TransLog é o `TransLogService` (`@Service`, `service/TransLogService.java:12`). Ele monta o seu `RestClient` uma vez, no próprio construtor (`:18-25`).
- O `CheckoutCalculator` recebe por injeção `TransLogService`, `ProductRepository` e `OrderRepository` (`:26-32`). É do `OrderRepository` que vem a consulta de primeira compra do WELCOME10 (`:306-310`).
- O `OrderService` recebe o `CheckoutCalculator` injetado (`service/OrderService.java:29-39`). O resumo e o registro chamam a mesma instância (`:42` e `:48`).

A busca por `new CheckoutCalculator`, `new TransLogService` e `new OrderService` em `src/` não acha nenhum resultado, e a única configuração com `@Bean` é a carga do catálogo (`config/DataInitializer.java:16`). Não há `new` de peças espalhado nem repetido. Resumindo: não há objeto por caso, e as peças com dependência são criadas de forma concentrada pelo contêiner do Spring.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Cada um destes objetos se cria direto:
- o item do pedido: `new OrderItem(item.getSku(), item.getQuantity())` (`service/OrderService.java:62`), construtor simples (`entity/OrderItem.java:21-24`);
- o aviso: `new Notification(cpf, type, orderId)` (`service/NotificationService.java:30`). O construtor só junta os campos e põe `NEW` (`entity/Notification.java:32-37`);
- o resumo: `new OrderSummary()` seguido de setters (`service/CheckoutCalculator.java:67-78`);
- os DTOs de resposta: `new` e setters (`service/OrderService.java:227-258`);
- o cliente: `new Customer(cpf)` (`service/OrderService.java:66`).

O único método de fábrica é o trio estático `QuoteResult.success/notServed/unavailable` (`service/TransLogService.java:79-89`). São construtores com nome, de uma linha cada, que só fixam os dois booleanos. Isso é a forma simples que a régua aceita. Não existe nenhum builder (o único `builder()` é o do `RestClient` do Spring, em `TransLogService.java:22`), nenhuma classe `*Factory` e nenhum Lombok no `pom.xml`.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação de objetos não há estrutura sobrando. O projeto não tem nenhuma interface: as classes concretas são injetadas diretamente. Não há camada que só repassa, nem builder, nem fábrica. As classes auxiliares `ShippingInfo` e `PaymentInfo` (`service/CheckoutCalculator.java:430-451`) são portadores privados de resultado, sem decisão nenhuma, e não chegam a ser estrutura. À parte do padrão, só vale notar o método `applyGoldBenefits` (`:423-428`), que nenhum lugar chama: é código morto, não estrutura.
Conclusão: contenção

## O que mudaria primeiro

1. Criar uma casa para cada caso: uma classe `@Component` por entrega, por cupom e por forma de pagamento, atrás de uma interface, e dar comportamento às constantes de `ClubTier`. Depois, recebê-las num registro injetado (por exemplo um `Map<ShippingMethod, ShippingOption>` montado a partir de `List<ShippingOption>`). Assim o contêiner passa a ser a fábrica dos casos. A econômica e a expressa recebem o `TransLogService` por injeção, e o WELCOME10 recebe o `OrderRepository`. Um caso novo vira uma classe nova, sem mexer no `CheckoutCalculator`. Esta é a mesma correção que o Strategy pede, e aqui ela daria à criação algo para concentrar.
2. Tirar o `RestClient` do construtor do `TransLogService` e passá-lo a um `@Bean` de configuração. Assim a dependência externa também fica com o contêiner e pode ser trocada nos testes. É uma melhora pequena: hoje a criação já acontece num lugar só.

## Dúvidas

- Como não há objeto por caso, o F se resume a dizer que não há o que concentrar. Descrevi também como são criadas as peças com dependência (`CheckoutCalculator` e `TransLogService`), porque a régua cita o cliente da TransLog e o cálculo como exemplos do que deve ser montado num lugar só.
- Li os três métodos estáticos de `QuoteResult` como "método de fábrica estático simples" (contenção), não como fábrica: cada um só fixa valores, sem nenhuma decisão na criação.

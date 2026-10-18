# Padrões: Factory (a criação concentrada)

## Visão geral

O agente não criou nenhum objeto por caso. As entregas, as formas de pagamento, os níveis do clube e as regiões são enums sem corpo (`ShippingMethod`, `PaymentMethod`, `ClubTier`, `Region`). Os cupons nem chegam a ser um tipo: são texto comparado com literais. Toda a variação mora em cadeias de `if`/`switch` dentro de `CheckoutService`, por isso não há criação por caso para concentrar. Esse problema é do Strategy, não deste pattern. As dependências que existem (o cliente da TransLog, o repositório de produtos, o `ObjectMapper`) são criadas pelo contêiner do Spring e injetadas pelo construtor num lugar só. Não há `new` espalhado. Os objetos sem regra (o resumo, o aviso, o item do pedido) saem direto, com o `@Builder` gerado pelo Lombok, que é o padrão do projeto todo. Não há fábrica nem builder escrito à mão. Também não achei estrutura sem necessidade perto deste pattern.

## Item a item

**F, a criação dos objetos de cada caso.** Não existe objeto por caso:
- Os enums só listam constantes, sem corpo nem comportamento (`src/main/java/com/loja/orderservice/domain/enums/ShippingMethod.java:3-8`, `PaymentMethod.java:3-7`, `ClubTier.java:3-7`).
- Os cupons são `String`: `CheckoutSummaryRequest.java:17` e a lista de válidos em `src/main/java/com/loja/orderservice/service/CheckoutService.java:205-208`.

O código de cada caso está em condicionais dentro do mesmo serviço:
- a entrega: `CheckoutService.java:50-63` e `:265-297`;
- o cupom: `CheckoutService.java:220-231`;
- o pagamento: `CheckoutService.java:343-360`;
- o nível: `CheckoutService.java:370-380` e o brinde em `:80-81`.

Logo, a criação não tem o que concentrar. As peças de que os casos dependem também não estão espalhadas:
- `TransLogClient` é `@Service` (`src/main/java/com/loja/orderservice/integration/TransLogClient.java:16-18`) e é injetado em `CheckoutService` com `ProductRepository` e `ObjectMapper` (`CheckoutService.java:21-36`).
- O pedido registrado não monta as próprias peças: `OrderService` recebe o `CheckoutService` injetado (`src/main/java/com/loja/orderservice/service/OrderService.java:40`) e o chama (`:45`).
- Não há nenhum `new` de um caso ou de uma dependência dele em mais de um lugar.

Dois registros:
- O `HttpClient` é recriado a cada consulta dentro do próprio cliente (`TransLogClient.java:29-31`). A criação continua num lugar só, apenas repetida a cada chamada.
- O WELCOME10 não depende de nada. Ele não consulta os pedidos do cliente: o `cpf` chega a `calculateCouponDiscount` (`CheckoutService.java:46`) mas não é usado (`:220-221`). Assim, a dependência que o enunciado prevê para esse caso não existe no código. Se a regra de primeira compra funciona é questão do comportamento.

Item descritivo: não há objetos por caso, as dependências vêm do contêiner do Spring e a criação está concentrada.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.**
- **O item do carrinho** chega como DTO desserializado pelo Jackson (`CheckoutSummaryRequest.java:29-34`). A versão de resposta (`src/main/java/com/loja/orderservice/dto/OrderResponse.java:27-32`) é montada direto num `map` (`OrderService.java:306`).
- **O resumo** é montado uma vez, no fim do cálculo (`CheckoutService.java:83-95`), pelo `CheckoutSummary.builder()` do `@Builder` do Lombok (`src/main/java/com/loja/orderservice/dto/CheckoutSummary.java:13`).
- **O aviso** é montado num método auxiliar privado, `createNotification` (`OrderService.java:282-292`), chamado nos quatro pontos que avisam (`:116`, `:152`, `:179`, `:254`). Ele só preenche os campos, com `NEW` como situação inicial.

Nenhum desses objetos tem fábrica, nem uma classe "factory", nem um builder escrito à mão. O `@Builder` do Lombok é uma anotação aplicada em todas as entidades e DTOs do projeto (`Product.java:15`, `Customer.java:15`, `Order.java:16`, `Notification.java:17`, os DTOs). É um costume do projeto, não uma estrutura de criação feita para esses objetos, e não acrescenta código a manter. O método auxiliar do aviso equivale a um método de fábrica simples, que evita repetir a montagem em quatro lugares. É a resposta do tamanho do problema. (Ver Dúvidas sobre o `@Builder`.)
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** As únicas interfaces do projeto são os repositórios do Spring Data (`src/main/java/com/loja/orderservice/repository/*.java`). O único `implements` é o `CommandLineRunner` da carga do catálogo (`src/main/java/com/loja/orderservice/config/DataInitializer.java:13`). Não há configuração com `@Bean`, nem interface com uma implementação só, nem classe abstrata. O `ShippingInfo` é um portador de dois campos com construtor (`CheckoutService.java:382-390`). Perto deste pattern, não há fábrica, registro ou camada de criação que ninguém pediu.
Conclusão: contenção

## O que mudaria primeiro

A mudança que mais pesa começa no Strategy e tem efeito aqui: transformar cada entrega, cupom e forma de pagamento num objeto próprio, `@Component` ou constante de enum com corpo, e deixar o Spring injetá-los num registro indexado pelo código. Assim, o `TransLogClient` iria só para a econômica e a expressa, e o acesso aos pedidos do cliente só para o WELCOME10. Um caso novo, ou uma dependência nova, mudaria o lugar de criação (o contêiner) e não o `CheckoutService`.

Num ponto menor, criar o `HttpClient` uma vez, como campo ou bean, em vez de a cada consulta (`TransLogClient.java:29-31`).

## Dúvidas

O `@Builder` do Lombok no resumo, no aviso e no item de resposta. Lido ao pé da letra, o controle negativo chama de exagero "um builder" para um objeto que só junta campos. Li como contenção, porque o que o controle mede é estrutura de criação que o agente desenhou. Aqui é uma anotação que gera código, aplicada igual em todas as classes de dados do projeto, sem nenhuma classe ou método escrito para criar esses objetos. Equivale, na prática, a usar um record. Se a régua quiser contar também o builder gerado por anotação, a conclusão do F-exagero passa a `exagero`.

# Factory

## Visão geral

O agente não criou nenhum objeto por caso. As entregas, os cupons, as formas de pagamento e os níveis existem só como enums sem corpo (`model/enums/ShippingMethod.java`, `CouponCode.java`, `PaymentMethod.java`, `ClubTier.java`). A variação de cada caso está em `switch` dentro de quatro serviços sem estado: `ShippingService.java:41-46`, `CouponService.java:19-40`, `PaymentService.java:29-33` e `ClubService.java:11-15`. Por isso a criação dos casos não tem o que concentrar, e esse problema pertence ao Strategy. O que existe está montado à mão num lugar só: os serviços, os repositórios e os controllers são criados uma única vez no `Main.java:21-45`, e nenhum outro arquivo de `src/main` faz `new` de serviço ou repositório. A dependência externa (a URL da TransLog) entra uma vez, em `Main.java:29`. Os objetos sem regra são criados direto, por construtor ou por setters, sem fábrica nem builder. Também não há interface, classe abstrata ou fábrica sem necessidade.

## Item a item

**F, a criação dos objetos de cada caso.** Não existe objeto por caso:
- **Entregas:** `ShippingService.calculateShipping` escolhe o caso com `switch (method)` (`service/ShippingService.java:41-46`). A econômica e a expressa chamam o mesmo método privado `calculateTranslog`, passando `"STD"`/`"EXP"` (linhas 42-43). A retirada e o motoboy devolvem um `new ShippingQuote(...)` com valores fixos (linhas 44-45).
- **Cupons:** ficam todos num `switch (coupon)` em `service/CouponService.java:19-40`.
- **Formas de pagamento:** ficam num `switch (method)` em `service/PaymentService.java:29-33`, que chama métodos privados.
- **Níveis:** ficam num `switch (tier)` e em comparações `tier == ClubTier.GOLD` em `service/ClubService.java:11-26`.

Portanto não há casa por caso para criar, e a criação dos casos não tem o que concentrar. O agente montou à mão o que existe, tudo no `Main.java`: o `Database` (linha 21), os quatro repositórios (24-27), os serviços de cálculo (29-32), o `CheckoutService` com todas as peças (34-36), o `OrderService` (38-40) e os controllers (42-45). O "cliente da TransLog" não é um objeto à parte: o `HttpClient` e a URL ficam dentro do `ShippingService`, montados no construtor (`service/ShippingService.java:22-28`) e criados uma vez em `Main.java:29`. O `grep` por `new ...Service(`/`new ...Repository(` em `src/main` só encontra o `Main.java`. Fora dele, só o teste repete `new PaymentService()` (`src/test/java/com/shop/BasicTest.java:15,21,36,51`), o que é normal num teste. Não há `new` de caso nem de dependência espalhado ou repetido pelos fluxos. Em resumo: a montagem está concentrada no `main`, mas não existem objetos por caso, porque a variação está em `switch`.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.**
- **Item do carrinho:** `OrderItem` é uma classe simples com dois campos e construtores (`model/OrderItem.java:1-19`). Quem preenche é o Jackson, a partir do JSON.
- **Aviso:** é criado por `notificationRepository.create(conn, NotificationType..., orderId, cpf)` nos fluxos (`service/OrderService.java:110,127,144,182,257`). O construtor de `Notification` só junta os campos e põe `NEW` (`model/Notification.java:14-19`).
- **Resumo:** é criado com `new CheckoutSummary()` e setters (`service/CheckoutService.java:96-107`).
- **Respostas HTTP:** usam construtor direto (`controller/CustomerController.java:34`, `controller/ProductController.java:26`, `controller/OrderController.java:111`).

O `grep` por `Builder`/`Factory`/`builder` só acha os builders da biblioteca padrão (`HttpClient.newBuilder()` e `HttpRequest.newBuilder()` em `service/ShippingService.java:24,55`), que vêm do JDK e não do projeto. Não há fábrica nem builder próprio para esses objetos. (Montar o resumo campo a campo com setters em vez de um construtor ou record é uma questão de estilo; não é estrutura a mais.)
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto da criação).** Não há interface, classe abstrata, fábrica ou builder no projeto: o `grep` por `interface`/`abstract class`/`implements` em `src/main` não acha nada, e o único `extends` é `BusinessException extends RuntimeException` (`exception/BusinessException.java:3`). Os serviços são classes concretas criadas uma vez no `main`, sem camada de repasse para a criação. Os métodos estáticos de `config/Database.java:126-157` (`toJson`, `fromJson`, `createJsonb`, ...) são utilitários de conversão JSON, não fábricas de objetos do domínio.
Conclusão: contenção

## O que mudaria primeiro

1. **Dar a cada caso uma casa e registrar os casos no `Main`.** Por exemplo, uma implementação por entrega, em que a econômica e a expressa recebem um cliente da TransLog separado do `ShippingService`; o mesmo para os cupons, as formas de pagamento e os níveis. Os casos seriam registrados no lugar onde o agente já monta tudo. Hoje um caso novo muda o `switch` dentro do serviço. Com essa mudança, mudaria só a montagem (e a casa nova), que é o ganho que a criação concentrada deveria trazer. Na raiz, isso é a correção do Strategy.
2. **Separar o cliente HTTP da TransLog numa peça própria, criada no `Main`.** Uma troca de transportadora (o enunciado já prevê isso) passaria a mudar a montagem, e não o serviço de frete.

## Dúvidas

- Não existe objeto por caso, e o F é descritivo. Por isso descrevi a montagem dos serviços no `Main` como a forma de criação que existe, deixando claro que ela não cria casos. Tomei o `ShippingService` (que guarda a URL e o `HttpClient`) como o que faz o papel do "cliente da TransLog" neste projeto, já que não há uma classe à parte para ele.

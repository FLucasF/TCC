# Factory

## Visão geral

O agente montou os repositórios e os serviços à mão no `main` (`src/main/java/com/loja/App.java:14-35`), que é a forma esperada sem contêiner, no Javalin. As peças de cada caso não passam por ali. Os cupons, as formas de pagamento e os níveis têm uma classe por caso, e cada família é criada por uma fábrica com `switch`: `CouponFactory`, `PaymentFactory` e `ClubFactory`. Essas fábricas são chamadas, e no caso do `CouponFactory` instanciadas com `new`, dentro do `CheckoutService` a cada pedido. As entregas não têm objeto por caso: a variação fica num `switch` no serviço. O cliente da TransLog, que é a dependência da econômica e da expressa, é criado com `new` em dois lugares do serviço, e cada um lê `TRANSLOG_URL` de novo. O próprio `CheckoutService` também é criado duas vezes, no `App` e dentro do `OrderService`. Os objetos sem regra (item, aviso, resumo, pedido) nascem por construtor, sem builder nem fábrica.

## Item a item

**F, a criação dos objetos de cada caso.** Cada família foi resolvida de um jeito:
- **Entregas:** não há objeto por caso. `ECONOMY`, `EXPRESS`, `STORE_PICKUP` e `COURIER` estão num `switch` dentro do cálculo (`src/main/java/com/loja/service/CheckoutService.java:227-236`), e o serviço escolhe `"STD"`/`"EXP"` comparando com `ShippingMethod.ECONOMY` (`CheckoutService.java:243`). A criação não tem o que concentrar aqui; o problema é do Strategy.
- **Cupons:** uma fábrica com `switch` que só devolve o objeto (`src/main/java/com/loja/strategy/CouponFactory.java:17-23`). Ela recebe o `OrderRepository` de que o `Welcome10Coupon` precisa e o repassa (`CouponFactory.java:18`). A fábrica, porém, não é montada no `main`. É criada com `new` duas vezes por requisição, uma na validação e outra no cálculo (`CheckoutService.java:142` e `:198`), e o repositório chega por um getter exposto pelo `OrderService` (`src/main/java/com/loja/service/OrderService.java:245-247`). Há ainda uma segunda lista dos códigos em `isValidCoupon` (`CouponFactory.java:27-30`). Pela régua, uma lista de válidos não conta como código de caso fora da casa. Mesmo assim, um cupom novo obriga a mudar dois lugares da fábrica.
- **Formas de pagamento:** fábrica estática com `switch` (`src/main/java/com/loja/strategy/PaymentFactory.java:6-12`), chamada duas vezes por requisição (`CheckoutService.java:131` e `:297`).
- **Níveis:** fábrica estática com `switch` (`src/main/java/com/loja/strategy/ClubFactory.java:6-14`), chamada no cálculo (`CheckoutService.java:28`).
- **A dependência das entregas da TransLog:** está espalhada e repetida. `new TransLogAdapter(translogUrl)` aparece na validação (`CheckoutService.java:164`) e de novo no cálculo (`CheckoutService.java:240`). Cada ponto lê `System.getenv("TRANSLOG_URL")` por conta própria (`:158` e `:239`). O `main` não cria nem entrega o cliente da TransLog. Trocar de transportadora mexeria no serviço, não na montagem.
- **O cálculo:** também é criado em dois lugares, `App.java:23` e `OrderService.java:24`, onde o `OrderService` cria o seu próprio `CheckoutService` passando `this`. Se as duas instâncias dão o mesmo cálculo é o A5 da arquitetura; aqui só registro que a criação dessa peça não está num lugar só.

Em resumo, a criação está concentrada por família, numa fábrica com `switch` para cupons, pagamentos e níveis. Ela não está concentrada no ponto de montagem: as fábricas e o cliente da TransLog são instanciados dentro do serviço, a cada uso, e o `new` do adaptador e do `CheckoutService` se repete. O item é descritivo.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** O item do carrinho (`src/main/java/com/loja/model/OrderItem.java:9`), o aviso (`src/main/java/com/loja/model/Notification.java:12`), o resumo (`src/main/java/com/loja/model/OrderSummary.java:20`) e o pedido (`src/main/java/com/loja/model/Order.java:19`) são classes de campos com construtor, criadas direto com `new` onde são usadas. São exemplos o aviso em `OrderService.java:78`, `:97`, `:182` e `:200`, o resumo em `CheckoutService.java:56` e o pedido em `OrderService.java:31`. Um `grep` por `Builder` e por métodos `create`/`of` estáticos em `src/main` só acha o `PaymentFactory` e o `ClubFactory`, que criam casos e não objetos sem regra. Não há builder nem "factory" para um objeto que só junta campos. A repetição do `new Notification(...)` com os mesmos quatro argumentos é duplicação de código, não estrutura a mais.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação, não achei estrutura que nenhum ponto peça. As três fábricas atendem a pontos com variação (P2, P3, P4), não há builder, e o `main` não tem camada de composição além do necessário. A interface `ShippingCarrier`, com uma implementação só (`src/main/java/com/loja/shipping/ShippingCarrier.java`), corresponde à troca de transportadora prevista no enunciado, e quem a julga é o Adapter. A interface funcional `ProductProvider` (`src/main/java/com/loja/strategy/ProductProvider.java`), implementada só pela lambda de `CheckoutService.java:205`, não é criacional e fica para as outras correções.
Conclusão: contenção

## O que mudaria primeiro

1. **Criar o cliente da TransLog e as fábricas uma vez só, no `App`, e entregá-los ao `CheckoutService` pelo construtor.** Isso inclui o `CouponFactory` já com o `OrderRepository`. Hoje a URL da transportadora é lida e o adaptador é instanciado em dois pontos do serviço (`CheckoutService.java:158-164` e `:239-240`). Uma transportadora nova ou uma dependência nova de um cupom mexeria no cálculo, quando deveria mexer só na montagem. De quebra, some o getter `getOrderRepo()` que só existe para isso.
2. **Ter uma única instância do `CheckoutService`, criada no `App` e passada ao `OrderService`,** em vez de o `OrderService` criar a sua (`OrderService.java:24`).

## Dúvidas

- As fábricas com `switch` (`PaymentFactory`, `ClubFactory`, `CouponFactory`) são a forma "fábrica que só devolve o objeto" listada no F. Li a criação como concentrada por família e não concentrada na montagem. Como o item é descritivo, essa distinção não muda a conclusão.
- A lista duplicada em `CouponFactory.isValidCoupon` está registrada como observação, não como código de caso fora da casa, porque a régua exclui expressamente as listas de válidos.

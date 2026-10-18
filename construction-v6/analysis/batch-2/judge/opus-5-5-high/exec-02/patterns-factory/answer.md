# Padrões: Factory (a criação concentrada)

## Visão geral

O projeto usa Javalin, sem contêiner de injeção, e toda a criação dos objetos por caso fica num lugar só: o `main`. Lá são criados a única instância do `TransLogClient` e os repositórios, e daí saem os quatro registros (`Map<enum, estratégia>`): o das entregas, o dos cupons, o dos níveis e o das formas de pagamento. Cada objeto recebe as suas dependências ali mesmo. As mesmas instâncias dos mapas vão para o `CheckoutService` e para o `ValidationService`. Fora do `Main` não há nenhum `new` de estratégia nem de dependência de estratégia. Os objetos sem regra (o item do carrinho, o aviso, o resumo, o evento de pagamento) são criados pelo construtor, sem fábrica e sem builder. Achei só dois deslizes pequenos, que não são problemas de criação. Cada estratégia declara um `getMethod()`/`getCode()`/`getTier()` que nada chama, enquanto a chave de verdade é escrita de novo no `Main`. E o `new Notification(0, TIPO, id, NEW)` aparece igual em três lugares. A região não tem objeto por caso: o seguro sai de um `switch`, e isso é assunto do Strategy.

## Item a item

**F, a criação dos objetos de cada caso.** A forma é a montagem à mão num lugar só, com registro por chave:

- **O cliente da TransLog.** Uma só instância é criada (`src/main/java/com/loja/Main.java:43`) e entregue à econômica e à expressa (`Main.java:46-47`). Não existe o `new ExpressShipping(new TranslogClient(url))` repetido.
- **As entregas.** `Main.java:45-50` registra as quatro: `ECONOMY`, `EXPRESS`, `STORE_PICKUP` e `COURIER`.
- **Os cupons.** `Main.java:52-57` registra os quatro. O `Welcome10Coupon` recebe o `orderRepository`, de que precisa para saber se é a primeira compra (`Main.java:53`, usado em `domain/strategies/coupon/Welcome10Coupon.java:15`). O `Buy3Pay2Coupon` recebe o `productRepository` (`Main.java:56`).
- **Os níveis.** `Main.java:59-63`.
- **As formas de pagamento.** `Main.java:65-69`.
- **Quem usa as peças.** As mesmas instâncias dos mapas são injetadas no `CheckoutService` (`Main.java:77-83`) e no `ValidationService` (`Main.java:93-99`). Os dois só fazem `get` no mapa (`services/CheckoutService.java:39`, `:43`, `:60`, `:68`; `services/ValidationService.java:113`, `:141`, `:169`). Nenhum dos dois cria nada.
- **A busca do `new`.** Um `grep` por `new` em `src/main` não acha estratégia nem `TransLogClient` criados fora do `Main`.

Com isso, um caso novo muda o enum, a classe nova e uma linha do `Main`, e quem usa não muda. Uma dependência nova muda só o `Main`.

Há uma duplicação pequena. Cada classe sabe a própria chave (`getMethod()` em `domain/strategies/shipping/ShippingStrategy.java:14`, `getCode()` em `CouponStrategy.java:21`, `getTier()` em `ClubTierStrategy.java:12`, `getMethod()` em `PaymentMethodStrategy.java:14`), mas nenhum lugar chama esses métodos: o `grep` só acha as declarações. A chave é escrita de novo no literal do mapa, e as duas podem divergir sem que nada perceba. É um nome usado só como chave de registro, então não é código de caso fora da casa dele.

A região (P5) não tem objeto por caso: são dois `switch` em `services/CheckoutService.java:117-129`. Ali a criação não tem o que concentrar. A falta de estrutura é assunto do Strategy, não deste item.

O item é descritivo: a criação está concentrada no `Main`.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.**

- **O item do carrinho** é uma classe com construtor (`domain/entities/OrderItem.java:7`). O construtor só tem uma guarda de quantidade, que não é decisão de criação. Ele é criado direto no controller (`api/controllers/ApiController.java:93` e `:327`). A conversão está repetida nessas duas linhas, mas é uma duplicação simples, não uma estrutura.
- **O aviso** é criado pelo construtor (`domain/entities/Notification.java:9`) em `domain/observers/NotificationObserver.java:21`, `services/OrderService.java:187` e `:197`. É o mesmo `new Notification(0, TIPO, orderId, NEW)` com o tipo trocado. Um método estático simples (um `Notification.newOf(type, orderId)`) tiraria a repetição, mas não ter feito isso não é exagero.
- **O resumo** (`OrderSummary`) é criado pelo construtor de 11 argumentos em `services/CheckoutService.java:82` e na leitura do banco (`repositories/impl/OrderRepositoryImpl.java:104`). Não há builder.
- **O evento** `PaymentConfirmedEvent` também sai do construtor (`services/OrderService.java:164`).
- **Builders e fábricas.** Não há classe `*Factory` nem `*Builder` no projeto. Os únicos builders são os de biblioteca (`GsonBuilder` em `ApiController.java:41`, `HttpClient.newBuilder()` em `infrastructure/TransLogClient.java:20`), e os únicos métodos de fábrica estáticos são `Money.of`/`Money.zero` (`domain/entities/Money.java:16-25`), que são triviais.

A resposta é do tamanho do problema.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto da criação).** Não há fábrica, builder, provider nem classe de composição sem necessidade. A montagem fica no próprio `main`, sem uma camada só para repassar. Os mapas são a forma mínima de registro, e o `PaymentConfirmedEvent`, que só embrulha o `Order`, é um objeto simples criado direto. Os quatro pares interface/implementação dos repositórios (`repositories/ProductRepository.java` e `repositories/impl/ProductRepositoryImpl.java`, e o mesmo para Order, Customer e Notification) têm uma implementação só, e os testes não trazem dublês (o único teste é `src/test/java/com/loja/MoneyTest.java`). Mas não são estrutura de criação; ver Dúvidas.
Conclusão: contenção

## O que mudaria primeiro

1. **Uma fonte só para a chave.** Os mapas poderiam ser montados a partir das próprias estratégias, por exemplo `Stream.of(new EconomyShipping(t), ...).collect(toMap(ShippingStrategy::getMethod, identity()))`. Ou então os `getMethod()`/`getCode()`/`getTier()`, que ninguém chama, sairiam do código. Hoje a chave está em dois lugares que podem divergir.
2. **Um método estático no aviso.** Algo como `Notification.newOf(type, orderId)` tiraria a repetição do `new Notification(0, …, NEW)` nos três pontos. É uma limpeza menor e não muda o desenho.

## Dúvidas

- **Os repositórios no E.** As quatro interfaces de repositório com uma implementação cada não entraram como exagero nesta correção. Não são estrutura de criação, e a régua pede que cada pattern registre só o que está perto dele. Elas ficam para a correção do Adapter, ou para a arquitetura, onde a separação porta/implementação é assunto.
- **A região no F.** O enunciado do F lista as entregas, os cupons, as formas de pagamento e os níveis, e não a região. Mesmo assim registrei que a região não tem objeto por caso, só como descrição. Nada disso pesou na conclusão do F.

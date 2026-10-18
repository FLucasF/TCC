# Padrões: Factory

## Visão geral

O agente não criou nenhum objeto por caso. As entregas, os cupons, as formas de pagamento e os níveis do clube são constantes de enum sem corpo (`src/main/java/com/loja/models/Enums.java:4-18`). Toda a variação entre os casos fica em `if`/`else` e `switch` dentro de métodos estáticos de `CheckoutService`. Por isso a criação não tem o que concentrar, e o problema real aparece no Strategy, não aqui.

O serviço inteiro é estático. Não há instâncias de serviço nem dependências passadas a quem usa. O `main` (`src/main/java/com/loja/App.java:30,50`) só chama `Database.init` e `Routes.setup`, e não monta peça nenhuma. As dependências são buscadas no ponto de uso:
- a TransLog, por `System.getenv("TRANSLOG_URL")` lido duas vezes;
- o banco, por `Database.getDataSource()`, um acesso global estático chamado em todos os serviços.

Do lado bom, os objetos sem regra (o item do carrinho, o aviso, o resumo) se criam direto, por construtor, sem fábrica nem builder. Também não há estrutura sobrando perto da criação.

## Item a item

**F, a criação dos objetos de cada caso.** Não existe objeto por caso:
- **Entrega:** o tipo de entrega é decidido no ponto de uso. `calculateShipping` testa `method == STORE_PICKUP` e `method == COURIER` (`src/main/java/com/loja/services/CheckoutService.java:346,350`) e escolhe `"STD"`/`"EXP"` num ternário (`CheckoutService.java:362`). `validateShippingMethod` repete as mesmas perguntas (`CheckoutService.java:121,132,144`).
- **Cupom:** o cupom é uma cadeia de `coupon.equals("WELCOME10")`, `"MINUS50"`, `"FREESHIPPING"`, `"BUY3PAY2"` (`CheckoutService.java:284,292,299,310`), repetida na validação (`CheckoutService.java:171-182`).
- **Pagamento:** a forma de pagamento é decidida por `paymentMethod.equals("PIX"/"BANK_SLIP"/"CREDIT_CARD")` (`CheckoutService.java:240,244,248,254`).
- **Nível:** o nível é decidido por `tier == SILVER`/`tier == GOLD` (`CheckoutService.java:224,266,268,272`).

A criação não tem o que concentrar. O jeito como as dependências dos casos são obtidas é parecido com o `new` espalhado que a régua manda registrar:
- **A dependência da TransLog** (a econômica e a expressa precisam dela) não é criada nem entregue por ninguém. O endereço é lido do ambiente duas vezes, em fluxos diferentes (`CheckoutService.java:133` e `CheckoutService.java:357`). O cliente HTTP é aberto dentro de `queryTranslog`, um método estático privado (`CheckoutService.java:388-391`).
- **A dependência do WELCOME10** (os pedidos do cliente) é obtida dentro do cálculo, pegando a conexão do singleton estático (`CheckoutService.java:325`, `Database.getDataSource()`).
- **O peso do pedido**, de que a entrega precisa, é recalculado em três lugares (`CheckoutService.java:122-126`, `138-142` e `340-344`). É duplicação de cálculo, não de criação, mas mostra o mesmo desenho: cada fluxo monta o que precisa.

Fora dos casos, o `Gson` também é criado em dois lugares (`App.java:36` e `src/main/java/com/loja/api/Routes.java:12`), o que pesa pouco.

Em resumo, não há criação concentrada porque não há o que criar. Uma transportadora nova ou um cupom novo mudariam os `if` do cálculo e da validação, e não um ponto de montagem.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos se criam direto:
- **Item do carrinho:** chega como `CheckoutRequest.OrderItemData`, só com campos públicos, preenchido pelo Gson (`src/main/java/com/loja/api/CheckoutRequest.java:21-24`). O item do pedido é um construtor simples (`src/main/java/com/loja/models/OrderItem.java:7`), usado com `new OrderItem(...)` (`src/main/java/com/loja/services/OrderService.java:121`).
- **Aviso ao cliente:** é gravado direto por `NotificationService.createNotification` (`src/main/java/com/loja/services/NotificationService.java:12-24`) e lido com o construtor de `Notification` (`NotificationService.java:35,53`).
- **Resumo:** um construtor de onze campos (`src/main/java/com/loja/models/OrderSummary.java:16`), chamado com `new OrderSummary(...)` no cálculo (`CheckoutService.java:274`) e na leitura do banco (`OrderService.java:82`).

Não há fábrica, builder nem "factory" para nenhum deles. Os auxiliares `ValidationError`, `PaymentResult`, `ShippingInfo` e `TranslogResponse` também são construtores ou campos simples.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Procurei interfaces, classes abstratas, builders e fábricas (`grep` por `interface`, `abstract`, `Builder`, `Factory` em `src/main/java`) e não achei nada. A única interface implementada é o `JsonMapper` do Javalin, que o framework exige (`App.java:35`). `Enums` é só um agrupador de enums (`Enums.java:3`), sem lógica. As classes aninhadas privadas `ShippingInfo` e `TranslogResponse` (`CheckoutService.java:470-484`) só levam dados e não sobram. Perto da criação não há camada que só repassa nem fábrica que ninguém pediu.
Conclusão: contenção

## O que mudaria primeiro

1. **Dar a cada caso um objeto e montar tudo no `main`.** Hoje não há o que concentrar porque os casos estão em `if`/`switch`; essa troca começa no Strategy. Na parte da Factory, o ponto é que um cliente da TransLog criado uma vez no `App.main`, com o `TRANSLOG_URL` lido ali, poderia ser entregue à econômica e à expressa. Isso acabaria com as duas leituras do ambiente (`CheckoutService.java:133,357`) e faria uma transportadora nova mudar um lugar só.
2. **Trocar o acesso estático `Database.getDataSource()` por uma dependência passada aos serviços**, começando pela consulta de primeira compra do WELCOME10 (`CheckoutService.java:323-335`). Hoje cada peça busca o banco sozinha, e a montagem não mostra quem depende de quê.

## Dúvidas

Trato a leitura do `TRANSLOG_URL` e o acesso estático ao banco como o equivalente do "`new` das dependências espalhado". A régua fala de `new`, mas aqui o agente nem instancia um cliente da TransLog: a dependência é resolvida dentro do método que a usa, em dois lugares. Registro isso como descrição, sem efeito na conclusão, que neste item é sempre `não se aplica`.

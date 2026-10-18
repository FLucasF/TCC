# Padrões: Factory

## Visão geral

Não há objeto por caso neste projeto. As entregas, os cupons, as formas de pagamento e os níveis são enums sem corpo, e o que muda de um caso para outro está em `switch` dentro do `CheckoutService` e da `Validation`. Por isso a criação dos casos não tem o que concentrar. O único colaborador de caso, o cliente da TransLog, não é objeto: é uma classe de métodos estáticos chamada direto de três lugares, e por isso nem pode ser substituída. As peças que existem (os repositórios e o `CheckoutService`) não são montadas num lugar só. O `main` não as cria: cada classe de rotas instancia os repositórios de que precisa, e a `OrderRoutes` monta o `CheckoutService`. Os objetos sem regra (o item, o aviso, o resumo) são records criados pelo construtor, sem fábrica nem builder. Isso é contenção. Também não há estrutura sobrando fora dos pontos.

## Item a item

**F, a criação dos objetos de cada caso.** Não existe objeto por caso:
- **Os enums não têm corpo.** `ShippingMethod`, `PaymentMethod` e `ClubTier` só listam constantes e um `fromString` (`src/main/java/com/clothing/model/ShippingMethod.java:3-17`, `PaymentMethod.java:3-16`, `ClubTier.java:3-16`). O cupom nem chega a ser enum: é o texto que vem no pedido.
- **A variação mora em `switch`.** O frete está em `CheckoutService.java:116-132`, o prazo em `:136-152`, o cupom em `:76-88`, o ajuste do pagamento em `:179-201` e o crédito do nível em `:205-209`. As condições dos cupons estão em `Validation.java:115-136`. Esse problema é do Strategy, não deste item.
- **A TransLog não é objeto.** `TransLogClient.quote` é estático e lê `TRANSLOG_URL` a cada chamada (`TransLogClient.java:23-24`). Ninguém o cria nem o injeta. É chamado direto de `CheckoutService.java:123`, de `CheckoutService.java:143` e de `Validation.java:73`, cada lugar repetindo `ECONOMY ? "STD" : "EXP"` (`CheckoutService.java:122`, `:142`, `Validation.java:72`).
- **As peças não são montadas num lugar só.** O `main` só inicia o banco e entrega o `app` às rotas (`Application.java:13-23`). A `OrderRoutes` cria os três repositórios e o `CheckoutService` (`OrderRoutes.java:15-18`). A `CustomerRoutes` cria outro `CustomerRepository` (`CustomerRoutes.java:12`) e a `ProductRoutes`, outro `ProductRepository` (`ProductRoutes.java:12`).
- **A repetição não muda o resultado.** Os repositórios não guardam estado e usam o `Database` estático. Mesmo assim, uma dependência nova obrigaria a editar cada classe de rotas.

Em resumo: a criação está espalhada, mas não há caso com objeto próprio para criar.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** São todos records criados pelo construtor:
- o item do carrinho, `OrderItem(sku, quantity)` (`model/OrderItem.java:3-6`), criado com `new OrderItem(...)` em `OrderRepository.java:104`;
- o aviso, `new Notification(...)` em `CustomerRepository.java:107` e `:129`;
- o resumo, `new Summary(...)` em `CheckoutService.java:45` e `OrderRepository.java:85`.

Os `fromString` dos enums só traduzem o texto para a constante (por exemplo, `ClubTier.java:8-15`). Não há fábrica nem builder para esses objetos (busquei `Factory`, `Builder`, `builder` em `src/`, e o único resultado é o `LoggerFactory` do SLF4J).
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** O projeto não tem nenhuma interface, classe abstrata nem `implements` (busca em `src/`: o único `extends` é o da `CarrierUnavailableException`, em `TransLogClient.java:69`). Não há camada que só repassa: `CheckoutService.registerOrder` calcula, cria o cliente e grava (`CheckoutService.java:216-220`). Também não há fábrica nem builder que ninguém pediu. `Money` e `Validation` são utilitários estáticos com lógica própria, não camadas vazias.
Conclusão: contenção

## O que mudaria primeiro

1. **Montar as peças no `main`.** O `Application.main` passaria a criar uma vez os repositórios, um `TransLogClient` de instância (com a URL lida uma vez) e o `CheckoutService`, e entregaria tudo às rotas pelo construtor. Hoje `OrderRoutes.java:15-18`, `CustomerRoutes.java:12` e `ProductRoutes.java:12` criam cada um as suas peças. Assim, uma dependência nova muda um lugar só.
2. **Dar um objeto a cada entrega.** Quando a variação sair dos `switch` (o trabalho do Strategy), a econômica e a expressa devem receber o cliente da TransLog pelo construtor, criadas nessa mesma montagem. Hoje a TransLog é chamada por método estático de três lugares (`CheckoutService.java:123`, `:143`, `Validation.java:73`).

## Dúvidas

- **Cupom como texto, não como enum.** Li isso como "não há objeto por caso", o mesmo que os enums sem corpo.
- **Repositórios repetidos.** Registrei como criação espalhada, mas sem peso de defeito, porque os repositórios não guardam estado. O item é descritivo e fica `não se aplica`.

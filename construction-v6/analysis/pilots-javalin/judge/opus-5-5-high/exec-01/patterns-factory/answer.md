# Padrões: Factory (a criação concentrada)

## Visão geral

O projeto usa Javalin, sem contêiner de injeção, e concentra a criação em dois lugares que se completam. O primeiro é `Rules.standard(CarrierGateway)`, um método de fábrica estático que cria com `new` todos os objetos de caso (os três níveis, as quatro entregas, os quatro cupons, as três formas de pagamento) e os põe em registros indexados pelo código. O segundo é `Main.wire`, a raiz de composição que monta os serviços. O cliente da TransLog é criado uma vez só, no `main`, e entra pelo parâmetro de `wire` e de `Rules.standard`. Por isso os testes trocam a transportadora sem mexer em outra parte (`TestApp.java:51,57`). Nenhum `new` de caso ou de dependência de caso aparece fora desses dois lugares. Há só uma observação: a dependência do WELCOME10 (os pedidos do cliente) não entra na criação do cupom. Ela chega em cada chamada, como um `BooleanSupplier` montado pelo `CheckoutService`. Os objetos sem regra (o item do carrinho, o aviso, o resumo, o pedido) são records criados direto pelo construtor, sem fábrica nem builder. Fora dos pontos não achei, perto deste pattern, estrutura de criação que ninguém pediu.

## Item a item

**F, a criação dos objetos de cada caso.** A forma é a montagem à mão num lugar só. Cada caso é uma instância de record:
- os níveis: `new ClubTier(...)` em `src/main/java/shop/domain/Rules.java:12-14`;
- as entregas: `new ShippingMethod.Carrier("ECONOMY", carrier, "STD")` e `("EXPRESS", carrier, "EXP")` em `Rules.java:16-17`, e `new ShippingMethod.Fixed(...)` para a retirada e o motoboy em `Rules.java:18-19`;
- os cupons: `Rules.java:21-24`;
- as formas de pagamento: `Rules.java:26-28`.

Todos são criados dentro de `Rules.standard` (`Rules.java:10`) e guardados em `Registry`s por código (`Rules.java:30-33`, `src/main/java/shop/domain/Registry.java:14`). Quem usa só busca pelo código (`src/main/java/shop/app/CheckoutService.java:54,61,76,85`) e não cria nada.

A dependência da TransLog é criada uma única vez (`src/main/java/shop/Main.java:47`) e chega às duas entregas pelo parâmetro de `Main.wire` (`Main.java:25-26`). Os serviços também são montados só ali (`Main.java:27-31`). Os testes sobem o serviço pela mesma `wire` com uma transportadora falsa (`src/test/java/shop/TestApp.java:51,57`), o que confirma que a montagem tem um ponto só. O `grep` por `new ShippingMethod.`, `new Coupon.`, `new PaymentMethod.`, `new ClubTier` e `new TransLogClient` não acha nenhuma ocorrência fora de `Rules.java` e `Main.java`, a não ser o teste da própria `TransLogClient`. As regiões são constantes de enum com dados (`src/main/java/shop/domain/Region.java:8-12`), criadas pela própria linguagem.

A ressalva é o WELCOME10: o acesso aos pedidos do cliente não é dado ao cupom na criação. O `CheckoutService` o monta a cada cálculo e o passa a todos os cupons no `Coupon.Context`, como `() -> !db.tx(d -> d.hasCompletedPurchase(cpf))` (`CheckoutService.java:78-79`, campo `firstPurchase` em `src/main/java/shop/domain/Coupon.java:8`). Não há `new` espalhado nem repetido. Mas essa dependência mora no fluxo que usa os cupons, e não no lugar que os cria: se um cupom novo precisar de outra fonte de dados, o `Context` e o `CheckoutService` mudam junto com `Rules`. Conforme a régua, este item só descreve: a criação está concentrada (em `Rules.standard` e `Main.wire`), com essa exceção.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são records criados pelo construtor, sem decisão na criação:
- o item do carrinho: `new OrderItem(sku, quantity)` em `CheckoutService.java:113` e `new CartLine(p, quantity)` em `CheckoutService.java:126`;
- o resumo: `new Summary(...)` em `CheckoutService.java:93`;
- o aviso: `new Notification(...)` em `src/main/java/shop/app/Notifier.java:13`;
- o pedido: `new Order(...)` em `src/main/java/shop/app/OrderService.java:52,112,169`.

Os objetos de parâmetro (`ShippingMethod.Request`, `Coupon.Context`) também são records criados direto (`CheckoutService.java:67,78`). O único auxiliar é `withStatus` (`OrderService.java:168`), um método privado que copia o pedido com outra situação. Isso é uma cópia simples, não uma fábrica. `Money.of` é um método estático simples. Não há builder nem "factory" para objetos que só juntam campos. O `Notifier` cria o aviso e o grava, mas é um serviço com uma operação, não uma fábrica do aviso.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos.** Perto da criação não há builder, fábrica ou camada de montagem sobrando. Revisei três candidatos:
- `Registry<T>` (`Registry.java:9`) é uma classe genérica pequena, usada pelos quatro tipos de regra, e é o que permite achar o caso pelo código. Não é uma camada vazia.
- `CarrierGateway` (`src/main/java/shop/domain/CarrierGateway.java:7`) tem uma implementação de produção só. Mas o enunciado prevê a troca de transportadora, e os testes a substituem (`TestApp.java:51`), então é coisa do Adapter, não deste item.
- `QueryService` repassa ao banco, mas traduz a ausência nos códigos 404 (`src/main/java/shop/app/QueryService.java:18,22,27,33`). Não é só repasse.
Conclusão: contenção

## O que mudaria primeiro

Daria ao cupom de primeira compra a sua dependência na criação: um consultor de histórico do cliente passado ao `PercentOfProducts` (ou a uma classe própria do WELCOME10) dentro de `Rules.standard`, em vez de um `BooleanSupplier` montado pelo `CheckoutService` e entregue a todos os cupons no `Context`. Assim, um cupom novo que dependa de outro dado mudaria só o lugar de montagem, e não o fluxo do cálculo nem o contrato comum dos cupons.

## Dúvidas

- Em projeto Javalin, a régua aceita como criação concentrada "o `main`, ou uma classe que cria as peças e as entrega". Aqui são dois lugares encadeados: `Main.wire` monta os serviços e chama `Rules.standard`, que monta os casos. Tratei os dois como uma montagem só, porque `Rules.standard` só é chamado por `Main.wire` e nenhum outro código cria casos.
- Tratei a dependência do WELCOME10 como ressalva descritiva, não como `new` espalhado. Não há criação repetida: a dependência simplesmente não passa pela fábrica.

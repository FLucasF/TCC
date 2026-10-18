# Factory (a criação concentrada)

## Visão geral

O agente montou o serviço à mão, sem contêiner (é Javalin), e pôs toda a montagem num lugar só, em duas camadas. `Main` lê o ambiente e cria as peças de infraestrutura: o `PostgresStore` e o `TransLogGateway`. `App.create` recebe essas peças e cria todos os objetos de caso, registrados num mapa pelo código: entregas, cupons e formas de pagamento. Também cria o calculador, o serviço de pedidos e as reações ao pagamento. Nenhum objeto de caso é criado com `new` fora de `App.java`, e os testes montam o serviço pelo mesmo `App.create`, trocando só o banco e a transportadora.

Os objetos sem regra (o item do carrinho, o item do pedido, o aviso, o resumo) são records criados direto pelo construtor, sem builder nem fábrica. Não encontrei estrutura criacional que ninguém pediu.

O único ponto que merece registro é pequeno. A econômica e a expressa são duas instâncias da mesma classe, configuradas com os seus códigos na própria montagem (`App.java:39-40`). Para a criação isso não é problema, porque continua num lugar só. Se as duas deveriam ter casa própria é pergunta do Strategy.

## Item a item

**F, a criação dos objetos de cada caso.**
- **Forma:** montagem à mão num lugar só, numa raiz de composição. As entregas são criadas em `App.java:38-42`: `new CarrierShipping("ECONOMY", "STD", translog)`, `new CarrierShipping("EXPRESS", "EXP", translog)`, `new StorePickupShipping()` e `new CourierShipping()`. Os cupons são criados em `App.java:43-44` e as formas de pagamento em `App.java:45-46`. Cada lista vira um mapa pelo `code()` do próprio objeto, com o auxiliar genérico `byCode` (`App.java:57-59`). O calculador recebe os três mapas prontos (`CheckoutCalculator.java:32-37`) e só procura neles (`CheckoutCalculator.java:48, 68, 78`).
- **As dependências:** a dependência das entregas da TransLog é criada uma vez, em `Main.java:19` (`new TransLogGateway(System.getenv("TRANSLOG_URL"))`). Ela entra por parâmetro em `App.create` e é passada às duas instâncias de `CarrierShipping` (`App.java:39-40`). Os pedidos do cliente que o WELCOME10 precisa não vêm pela criação. O cupom é criado sem dependência (`new Welcome10Coupon()`, `App.java:44`) e recebe, a cada cálculo, um `BooleanSupplier` que consulta o banco na transação corrente (`CheckoutCalculator.java:72-73`, `CouponContext.java:14`). Com repositórios presos à transação (`Tx`), é uma escolha coerente.
- **As outras peças:** o `Notifier` é criado uma vez e repartido entre o `OrderService` e a `PaymentApprovedNoticeReaction` (`App.java:48-50`).
- **Os níveis:** são constantes de enum com dados, sem corpo (`ClubTier.java:9-11`). A criação é a da própria linguagem e não tem nada a concentrar.
- **Repetição:** procurei `new` de cada classe de caso e de `TransLogGateway` no projeto. Só aparece em `App.java:39-46` e `Main.java:19`. Os testes também não remontam as peças: chamam `App.create(store, gateway, clock)` com um banco em memória e uma transportadora falsa (`src/test/java/shop/ServiceTest.java:36-37`).

A criação está concentrada: um caso novo, ou uma dependência nova de um caso, muda `App.create` (e `Main`, se a dependência vier do ambiente), e não muda quem usa os objetos.
Conclusão: não se aplica

**F-exagero, os objetos sem regra.** Todos são records criados direto pelo construtor:
- o item do carrinho: `new CartLine(product, item.quantity())` (`CheckoutCalculator.java:120`), e o record está em `CartLine.java:7`;
- o item do pedido: `new OrderItem(item.sku(), item.quantity())` (`OrderService.java:49`);
- o aviso: `new Notification(UUID..., type, order.id(), NotificationStatus.NEW, order.customerCpf())` (`Notifier.java:11-12`). O `Notifier` grava o aviso; ele não é uma fábrica de avisos;
- o resumo: `new Summary(...)` (`CheckoutCalculator.java:91-93`). O construtor compacto só arredonda os campos para centavos (`Summary.java:20-29`), o que normaliza os valores e não decide nada sobre a criação.

Os métodos estáticos que existem são simples:
- `CheckoutRequest.from(JsonNode)` (`CheckoutRequest.java:27-45`) traduz o JSON para o record;
- `ApiException.unprocessable/conflict/...` (`ApiException.java:14-28`) só fixam o status HTTP;
- `Money.of` (`Money.java:17-19`) cria um valor arredondado.

Não há builder, nem classe ou método `*Factory`, para nenhum desses objetos.
Conclusão: contenção

**E, estrutura sem necessidade fora dos pontos (perto da criação).**
- **Fábricas e builders:** não achei nenhum. A busca por `factory|builder` no código de produção só encontra o `JsonNodeFactory` do Jackson (`Routes.java:50`) e o `HttpClient.newBuilder`/`HttpRequest.newBuilder` da biblioteca padrão (`TransLogGateway.java:19, 34`).
- **O auxiliar `byCode`:** tem três linhas e serve aos três registros (`App.java:57-59`). Não é uma camada.
- **As interfaces perto da montagem:**
  - `Store` e `Tx` (`persistence/Store.java:5`, `persistence/Tx.java:18`) têm uma implementação de produção, `PostgresStore`, e outra nos testes, `InMemoryStore` (`src/test/java/shop/InMemoryStore.java:29`). Essas duas são de fato trocadas.
  - `CarrierGateway` é trocada nos testes por um lambda (`ServiceTest.java:36`) e pertence ao ponto da troca de transportadora (Adapter).
  - `PaymentConfirmationReaction` tem duas implementações e pertence ao ponto das reações ao pagamento.

Nada disso é estrutura que ninguém pediu.
Conclusão: contenção

## O que mudaria primeiro

Quase nada na criação. A única mudança que valeria a pena é dar às variantes da TransLog o código de serviço (`"STD"`/`"EXP"`) em constantes ou numa casa própria, em vez de strings soltas na montagem (`App.java:39-40`). Assim, uma terceira modalidade da transportadora seria uma linha que não repete os códigos à mão. É um ganho pequeno, e a decisão de fundo (uma classe para as duas ou uma casa por entrega) cabe ao Strategy.

## Dúvidas

- **WELCOME10:** a régua diz que o WELCOME10 "precisa dos pedidos do cliente" como dependência de criação. Aqui essa dependência chega por contexto, a cada cálculo, e não pelo construtor (`CheckoutCalculator.java:72-73`). Li isso como uma forma legítima de entregar a dependência, que não espalha a criação, e não como criação fora do lugar.
- **Montagem em dois lugares:** a montagem está dividida entre `Main` (infraestrutura e ambiente) e `App.create` (o resto). Contei os dois como uma só raiz de composição, porque `Main` só cria o que vem do ambiente e entrega tudo a `App.create`.

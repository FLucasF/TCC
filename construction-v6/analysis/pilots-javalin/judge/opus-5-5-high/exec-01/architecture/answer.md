# A arquitetura

## Visão geral

O serviço está dividido em quatro pacotes por camada: `web`, `app`, `domain` e `db`. As dependências andam todas num só sentido (web → app → db/domain, db → domain) e o relatório não acusa nenhum ciclo. O controller (`web/Api.java`) é fino e não decide nada do negócio. O SQL fica todo em `db/Dao.java`. O resumo e o pedido saem de um único cálculo, `CheckoutService.prepare`. As regras variáveis (entrega, cupom, pagamento, nível do clube) moram no domínio, atrás de interfaces.

O problema mais sério é que **a regra de negócio conhece a web por dois caminhos**:

1. Os serviços de aplicação recebem o corpo cru da requisição como `JsonNode` e leem os nomes dos campos da API (`"zipCode"`, `"clubTier"`, `"transactionId"`…).
2. A exceção de negócio `ApiException` carrega o código HTTP. Ela é lançada de dentro do domínio com 422, 409, 404 e até um `503` literal.

Por isso a tradução de e para HTTP não fica na camada web. Ela está espalhada pelo serviço e pelo domínio. Um problema menor: as classes de aplicação conhecem `java.sql.SQLException` e a classe concreta `Dao`.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha nenhum tipo do Javalin fora de `web.Api` e `Main` (os dois são a camada web e a montagem). Ainda assim, a regra de negócio conhece a web por dois caminhos:

- **O corpo da requisição entra no negócio.**
  - `CheckoutService.prepare(JsonNode body)` (`app/CheckoutService.java:37`) recebe o JSON cru que o controller tirou de `ctx.body()` (`web/Api.java:71-73`). Ele mesmo navega a estrutura da API: `body.path("customer")`, `"zipCode"`, `"clubTier"`, `"coupon"`, `"installments"` (`app/CheckoutService.java:42-87`).
  - O mesmo vale para `OrderService.register(JsonNode body)` (`app/OrderService.java:50`), `pay(String, JsonNode)` (`:65`, que lê `"transactionId"`/`"paidAt"` em `:68-69`) e `ship(String, JsonNode)` (`:102`, que lê `"trackingCode"` em `:105`).
  - Esse `JsonNode` é o objeto de entrada da API. O projeto não tem um record de requisição; o corpo em árvore faz esse papel e chega até o serviço sem tradução para um tipo próprio do negócio. O relatório não pega isso porque o `JsonNode` é do Jackson, não do Javalin.
- **Os códigos HTTP moram no negócio.**
  - `domain/ApiException.java:8` recebe o `status` HTTP, e as fábricas fixam 422, 409 e 404 (`:14-24`).
  - Ela é lançada de dentro das regras do domínio:
    - `domain/ShippingMethod.java:37` e `:50` (422);
    - `domain/ShippingMethod.java:52`, com `new ApiException("CARRIER_UNAVAILABLE", 503)`;
    - `domain/PaymentMethod.java:43`;
    - `domain/Coupon.java:18`;
    - `domain/OrderLifecycle.java:31` (409).
  - Também é lançada dos serviços: `app/CheckoutService.java:157`, `app/OrderService.java:62,71,107,165` e `app/QueryService.java:18-33`.
  - O controller só copia `e.status()` para a resposta (`web/Api.java:63`). Quem escolhe a situação HTTP é a regra de negócio.

Pela régua, uma classe de negócio que conhece a web já basta. Aqui são `CheckoutService` e `OrderService` (o corpo da API e o HTTP), e mais `ShippingMethod`, `PaymentMethod`, `Coupon` e `OrderLifecycle` (o código HTTP).
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório não tem nenhum grupo em ciclo: "Cycles: 0 group(s), 0 of 4 packages caught in one" (`architecture/architecture-report.txt`). As cinco dependências entre pacotes (`app -> db`, `app -> domain`, `db -> domain`, `web -> app`, `web -> domain`) formam um grafo sem volta.
Conclusão: segue

**A3, o controller só traduz?** Cada rota de `web/Api.java:36-56` faz três coisas: lê o caminho ou o corpo, chama um serviço e devolve o resultado com a situação de sucesso (200/201). As funções `order`, `item` e `notification` (`:83-108`) só montam a visão da resposta.

O único desvio do sucesso está no pagamento (`:39-46`). Ali o controller transforma o `PaymentResult.error()` em 409, mas a recusa foi decidida e gravada em `OrderService.pay` (`app/OrderService.java:79-88`, `refuse` em `:147-151`). O controller não faz conta, não muda a situação do pedido e não mexe no estoque.

Na verdade o controller traduz até menos do que deveria: a leitura do corpo e a escolha do código HTTP ficaram no negócio, o que já pesou no A1. Pela pergunta deste item, ele não decide nada do negócio.
Conclusão: segue

**A4, o acesso ao banco está isolado?** Todo o SQL fica em `db/Dao.java`, com métodos como `lockProducts` (`:60`), `adjustStock` (`:76`), `hasCompletedPurchase` (`:128`) e `insertOrder` (`:140`), e a criação das tabelas fica em `db/Schema.java`. `db/Database.java:27-45` abre a conexão e a transação e entrega um `Dao` a uma função. Nenhuma classe de `app`, `domain` ou `web` monta SQL ou usa `Connection`/`PreparedStatement`: a busca por `java.sql`, `SELECT`, `INSERT` e `UPDATE` nesses pacotes só acha `import java.sql.SQLException`. O domínio nem sabe que existe banco. O cupom de primeira compra recebe um `BooleanSupplier` (`domain/Coupon.java:9-10`), ligado ao DAO em `app/CheckoutService.java:79`.

Há dois vazamentos pequenos:

- A interface de efeitos do pagamento e o `Notifier` declaram `throws SQLException` e recebem o `Dao` concreto: `app/PaidOrderEffect.java:12,14`, `app/CreditEffect.java:10,15`, `app/Notifier.java:12`. Ou seja, um tipo do JDBC aparece na assinatura da aplicação.
- Não há uma interface de repositório.

Nenhum dos dois é a regra de negócio usando JDBC direto.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** Sim:

- `POST /checkout/summary` chama `checkout.prepare(body(ctx)).summary()` (`web/Api.java:36`).
- `OrderService.register` chama o mesmo `checkout.prepare(body)` (`app/OrderService.java:51`) e grava o `c.summary()` que voltou (`:52-53`).

O único `new Summary(...)` do projeto está em `app/CheckoutService.java:93`. As etapas (frete `:67`, teto do cupom `:80`, seguro `:88`, ajuste do pagamento `:91`, crédito `:95`) só aparecem ali. Na consulta, o resumo é relido do banco (`db/Dao.java:177`), não recalculado. Não achei nenhuma etapa refeita em outro fluxo.
Conclusão: segue

**A6, como os pacotes estão organizados?** Por camada:

- `web`: 1 classe, o `Api`.
- `app`: 8 classes, os serviços de aplicação e os efeitos do pagamento. Aqui também mora o adaptador HTTP de saída `TransLogClient`, que implementa `domain.CarrierGateway`.
- `domain`: 21 classes, os tipos, as regras plugáveis e também `Json` e `ApiException`.
- `db`: 4 classes, o JDBC.
- `Main` fica no pacote raiz e só monta as peças (`Main.java:25-32`).

É uma divisão em camadas, com algumas classes de infraestrutura (`TransLogClient`, `Json`) morando junto do negócio.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o HTTP do negócio.** Duas mudanças:
   - O `Api` converte o corpo num tipo próprio, por exemplo um `PurchaseRequest` do negócio com campos já tipados, e é isso que vai para `CheckoutService.prepare` e `OrderService`.
   - O domínio lança uma recusa sem situação HTTP (só o código do problema, ou um tipo por categoria). Quem escolhe 404/409/422/503 é o `Api`, num mapa só.

   Hoje um código HTTP errado ou um campo renomeado na API obriga a mexer no domínio, que é exatamente o acoplamento que o A1 quer evitar.
2. **Isolar o JDBC das assinaturas de `app`.** Fazer `PaidOrderEffect`/`Notifier` dependerem de uma interface (de repositório ou de unidade de trabalho) em vez do `Dao` concreto e do `SQLException`. Isso deixa os efeitos do pagamento independentes do JDBC.

## Dúvidas

- **O `JsonNode` como "objeto de entrada da API".** A régua dá como exemplo os records do corpo que moram com os controllers. Aqui não há record nenhum, e o corpo chega ao serviço como árvore do Jackson. Li isso como o caso mais direto do mesmo problema: o serviço conhece o formato da requisição. Mesmo que outra leitura não aceite esse argumento, a conclusão do A1 não muda, porque os códigos HTTP dentro do domínio (`domain/ShippingMethod.java:52`, `domain/OrderLifecycle.java:31` etc.) já bastam pela régua.
- **O A6 não tem resposta certa nem errada.** Como a régua pede uma das palavras listadas e só `não se aplica` não traz julgamento de prática, usei essa palavra para não sugerir um juízo que o item não pede.

# arquitetura

## Visão geral

O agente pôs as 47 classes num pacote só, `shop`, em 19 arquivos: vários arquivos juntam muitos tipos, como `Models.java`, `Shipping.java`, `Payments.java`, `Coupons.java` e `OrderEffects.java`. O desenho por dentro é limpo em boa parte. Há um único cálculo do resumo (`PricingService.calculate`), usado pelo resumo e pelo registro. O acesso ao banco fica atrás das interfaces `Store` e `Tx`, e todo o SQL está em `JdbcStore`. O Javalin aparece só em `Api` e `Main`.

O problema mais sério é que a regra de negócio conhece o HTTP. Os erros do negócio são um `ApiException` que já traz o código HTTP (422, 409, 404, 503). `PricingService`, `OrderService`, os cupons, as entregas e o efeito de estoque lançam esse erro. O código HTTP chega a ser guardado no banco, no `PaymentRecord.httpStatus`. Além disso, o cálculo recebe direto o objeto de entrada da API (`CheckoutRequest`), montado a partir do `JsonNode` do Jackson. Um problema menor: os endpoints de produto, cliente e avisos abrem a transação e usam o `Tx` direto no `Api`, sem passar por um serviço.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha tipos do framework fora de `Api` e `Main` ("Classes outside the web layer that use the incoming web: none"; o Javalin aparece só em `Api` e `Main`), e isso confere com o código. Mas as classes de negócio conhecem os códigos HTTP:

- `ApiException` guarda um `status` HTTP (`src/main/java/shop/ApiException.java:5,7-10`), e as fábricas já levam o código: `unprocessable` = 422, `conflict` = 409, `notFound` = 404 (`ApiException.java:13-23`).
- `PricingService` lança esse erro em todas as validações (`src/main/java/shop/PricingService.java:30,32,34,37,53,63-65,77,83,88,92,97`).
- `OrderService` também lança (`src/main/java/shop/OrderService.java:55,63,85,91-92,103,114,140`).
- Cupons: `src/main/java/shop/Coupons.java:26`.
- O estoque na hora do pagamento: `src/main/java/shop/OrderEffects.java:29`.
- As entregas, inclusive com o 503 escrito à mão: `new ApiException("CARRIER_UNAVAILABLE", 503)` (`src/main/java/shop/Shipping.java:65`), e também `Shipping.java:67,96`.
- O registro de um pagamento guarda o código HTTP: `record PaymentRecord(String errorCode, int httpStatus)` (`src/main/java/shop/Models.java:22`). O serviço o grava como `new PaymentRecord(code, 409)` (`OrderService.java:84`), e ele vai para a coluna `http_status` (`src/main/java/shop/JdbcStore.java:71,374`). Na repetição do aviso, o serviço remonta o erro a partir desse status (`OrderService.java:61`).

O cálculo também recebe o objeto de entrada da API. `PricingService.calculate(CheckoutRequest r, Tx tx)` (`PricingService.java:26`) recebe o `CheckoutRequest`, montado a partir do `JsonNode` do Jackson (`src/main/java/shop/CheckoutRequest.java:18`). Ele traz o JSON cru: textos crus, e `couponPresent` diz se o campo veio no JSON (`CheckoutRequest.java:9-11,31-32`). Do mesmo jeito, `OrderService.pay` recebe o `paidAt` como texto e o converte ele mesmo (`OrderService.java:50,53,130-137`). Como tudo está num pacote só, o relatório não mostra essa dependência entre pacotes. A leitura do código mostra. Basta uma classe de negócio que conhece a web, e aqui são várias.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório traz "Cycles: 0 group(s), 0 of 0 packages caught in one" e nenhuma dependência entre pacotes. Isso vem de não haver subpacote: as 47 classes estão no pacote raiz. Não há ciclo entre pacotes a avaliar. Lendo as classes, também não vi serviços que se chamam nos dois sentidos. `Api` → `OrderService` → `PricingService` → `ShippingMethods`/`Coupons`/`PaymentMethods` vai num sentido só, e os efeitos (`OrderEffect`) só falam com o `Tx`. Pela régua, a conclusão é `segue`, mas a ausência de ciclo aqui não prova bom desenho: num pacote só, não há fronteira que um ciclo pudesse cruzar.
Conclusão: segue

**A3, o controller só traduz?** Nas rotas do pedido, o `Api` só lê a requisição, chama o `OrderService` e traduz o resultado ou o `ApiException` para HTTP (`src/main/java/shop/Api.java:30-43,91-101`). Ali não há conta, recusa, mudança de situação nem estoque. Já as rotas de produto, cliente e avisos abrem a transação e usam o `Tx` direto no controller:

- `api.store.transaction(tx -> tx.findProduct(...))` (`Api.java:45-47`);
- `tx.findCustomer(...)` (`Api.java:48-54`);
- nos avisos, o controller decide a recusa "CPF que nunca registrou pedido é `CUSTOMER_NOT_FOUND`" (`Api.java:57`);
- marcar um aviso como lido, que muda a situação do aviso, é feito pelo controller com `tx.markRead(id)` (`Api.java:60-68`).

Nada disso toca no pedido, no estoque ou no cálculo, e as decisões são pequenas. Mas são uma recusa do negócio e uma mudança de situação feitas na camada web, sem serviço no meio. Fico em dúvida sobre o peso: a recusa é quase uma consulta de existência, e marcar como lido é uma linha de SQL. Por isso fico em `em parte`, e não em `não segue`.
Conclusão: em parte

**A4, o acesso ao banco está isolado?** As regras falam com os dados só pela interface `Tx` (`src/main/java/shop/Store.java:18-52`). Todo o JDBC e o SQL estão em `JdbcStore` (por exemplo `JdbcStore.java:209,307,342,417`), com uma variante em memória, `MemoryStore`, para os testes. A busca por `java.sql`, `Connection`, `PreparedStatement`, `SELECT`, `INSERT` e `UPDATE` fora de `JdbcStore` não acha nada em `PricingService`, `OrderService`, `OrderEffects`, `Coupons`, `Shipping` ou `Payments`. Duas ressalvas leves:

- a definição de "primeira compra" (as situações `PAID`, `SHIPPED`, `DELIVERED`) fica na consulta SQL de `hasFulfilledOrder` (`JdbcStore.java:304-314`), não no negócio;
- o `Api` também usa o `Tx` direto, como dito no A3.

Mesmo assim, o acesso ao banco está isolado em classes de acesso a dados.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** `OrderService.summary` chama `pricing.calculate(request, tx)` (`OrderService.java:26`), e `OrderService.register` chama o mesmo método e guarda o `Summary` que ele devolve (`OrderService.java:31,34-36`). Não há segunda cópia de nenhuma etapa. O `Summary` só arredonda de novo, no construtor, valores que já vêm arredondados (`Models.java:27-36`). Esse arredondamento vale para os dois fluxos, então não cria divergência. Segue a prática.
Conclusão: segue

**A6, como os pacotes estão organizados?** As 47 classes estão no pacote raiz `shop`, sem subpacotes (relatório: "Packages (0)"). Vários arquivos juntam muitos tipos: `Models.java` tem os enums e records do domínio, `Shipping.java` tem a interface, as três entregas, o gateway e o registro. Dentro do pacote, os arquivos se agrupam mais ou menos por assunto: entrega, cupom, pagamento, efeitos, armazenamento, web. É um pacote só.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o HTTP do negócio.** Trocar o `ApiException(code, status)` por uma exceção de negócio só com o código, por exemplo um enum de motivos de recusa. A tabela código → status HTTP ficaria em `Api`, e o `PaymentRecord` guardaria só o código. É a mudança que leva o A1 a `segue`. Ela também evita que uma mudança na API (por exemplo, trocar 409 por 422) obrigue a mexer nas regras e nos dados gravados.
2. **Separar a entrada da API do cálculo.** O `Api` (ou um tradutor na camada web) leria o JSON e montaria um comando de domínio que o cálculo recebe. Nas mesmas linhas, levar produto, cliente e avisos, incluindo o `markRead` e a regra do `CUSTOMER_NOT_FOUND`, para um serviço, para o controller só traduzir. Quebrar o pacote único em subpacotes (web, domínio, persistência) deixaria essas fronteiras visíveis e verificáveis pelo relatório.

## Dúvidas

- **A6 é descritivo, mas a conclusão tem de ser uma palavra.** Usei `não se aplica` para não marcar como bom ou ruim, porque o item não pede julgamento.
- **O `CheckoutRequest` no A1.** Ele não mora num pacote de controllers, porque não há pacotes, mas é o objeto do corpo da requisição montado a partir do Jackson. Contei como objeto de entrada da API. Mesmo sem ele, os códigos HTTP do `ApiException` já bastariam para `não segue`.
- **O `Summary`.** O record do resumo vai direto para o JSON da resposta e tem os nomes de campo da API (`Models.java:24-26`). Li isso como um tipo de domínio que serve de resposta, não como dependência da web, e não pesei no A1.
- **O A2 sem pacotes.** "Sem ciclo" aqui é trivial, porque não há pacotes. Segui a régua literalmente (`segue`) e deixei a ressalva no texto.

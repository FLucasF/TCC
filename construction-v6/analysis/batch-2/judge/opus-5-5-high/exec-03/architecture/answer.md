# arquitetura

## Visão geral

O agente separou o serviço em quatro camadas: `web` com 2 classes, `service` com 11, `domain` com 38 e `persistence` com 16. O domínio tem subpacotes para cupom, pagamento e entrega. As dependências descem numa direção só (`web -> service -> persistence -> domain`, e `service -> domain`), e o relatório não acha nenhum ciclo. Os pontos fortes:

- Os handlers do Javalin só traduzem.
- Todo o SQL e o JDBC moram em `persistence.postgres`, atrás de interfaces de repositório e de um `Tx`.
- O resumo e o registro chamam o mesmo `CheckoutCalculator`.
- O pagamento e o cancelamento disparam efeitos por listeners.

O problema mais sério é o A1: **os códigos HTTP moram no domínio**. O enum `ErrorCode` traz o status HTTP de cada recusa e a `DomainException` carrega esse status. O `OrderService`, que é regra de negócio, escolhe um 409 de propósito. Em segundo plano, os records do domínio (`Order`, `Notification`) são devolvidos direto como corpo da resposta, com anotações do Jackson para moldar o JSON da API. Há também um detalhe de lugar: o cliente HTTP da TransLog (`TransLogClient`) mora no pacote do domínio.

## Item a item

**A1, a regra de negócio não conhece a web?** O relatório não acha nenhuma classe de fora da web que use a API web de entrada nem que dependa de um controller. Só `App` e `web.Routes` usam o Javalin, e as duas são montagem e camada web. Mas os códigos HTTP estão dentro do negócio:
- `src/main/java/shop/domain/ErrorCode.java:4-24` associa cada código a um status HTTP (`INVALID_ORDER(422)`, `CARRIER_UNAVAILABLE(503)`, `ORDER_NOT_FOUND(404)`, `OPERATION_NOT_ALLOWED(409)`…), e o mesmo arquivo expõe esse status em `defaultStatus()` (`:32`).
- `src/main/java/shop/domain/DomainException.java:5,11,17-19` guarda um `int status` e tem a fábrica `conflict()`, que fixa o 409.
- `src/main/java/shop/service/OrderService.java:60` decide o status HTTP da recusa (`DomainException.conflict(outcome.error())`), porque no pagamento o `INSUFFICIENT_STOCK` precisa sair 409, não o 422 do checkout. É a regra de negócio escolhendo a resposta HTTP.
- A camada web só repassa o número (`src/main/java/shop/web/Routes.java:39`, `e.status()`).

Em segundo plano, os records do domínio são o próprio corpo da resposta. Eles levam `@JsonIgnore` do Jackson para esconder o CPF do JSON da API (`src/main/java/shop/domain/Order.java:8`, `src/main/java/shop/domain/Notification.java:6`). Ou seja, o formato de saída da API está escrito no tipo de negócio. O `CheckoutRequest` (`src/main/java/shop/service/CheckoutRequest.java:6`) é um tipo do próprio serviço, preenchido pelo `CheckoutRequestParser` da web, e por isso não conta como problema. Bastam `ErrorCode`, `DomainException` e `OrderService` conhecerem os códigos HTTP.
Conclusão: não segue

**A2, os ciclos entre pacotes são acoplamento real?** O relatório não tem nenhum grupo em ciclo ("Cycles: 0 group(s), 0 of 4 packages caught in one"). As cinco dependências entre pacotes vão todas num sentido só. Uma observação fora do relatório, que não muda a conclusão:
- Ele agrupa pelos pacotes logo abaixo da raiz, então não mede os subpacotes do domínio. Dentro de `domain` há uma ida e volta entre `domain` e `domain.shipping`: `ClubTier.waivesShipping` chama `ShippingOption.clubWaivable()` (`src/main/java/shop/domain/ClubTier.java:34-35`), e `CarrierShipping` usa `Region.extraCarrierDays()` e a `DomainException` (`src/main/java/shop/domain/shipping/CarrierShipping.java:29,32`).
- Fora dos pacotes, `domain.shipping.TransLogClient` usa o `shop.Json` da raiz (`src/main/java/shop/domain/shipping/TransLogClient.java:9`), e a raiz (`App`) usa o domínio.

Nenhum dos dois é um grupo do relatório.
Conclusão: segue

**A3, o controller só traduz?** `src/main/java/shop/web/Routes.java:19-37`: cada rota lê o caminho e o corpo, chama um método de `services.checkout()`, `orders()` ou `queries()` e escreve o JSON com o status de sucesso. O `CheckoutRequestParser` (`src/main/java/shop/web/CheckoutRequestParser.java:15-42`) só converte o JSON em `CheckoutRequest`. Três decisões dele são de tradução, não de negócio:
- parcelas ausentes viram 1 e parcelas não inteiras viram 0, para a validação recusar (`:36-37`);
- uma quantidade inválida vira `null` (`:30`);
- um corpo malformado vira `INVALID_ORDER` (`:20,23`).

A ordem das validações, as recusas, a mudança de situação e o estoque ficam todos no `CheckoutCalculator` e no `OrderService`. Nenhum handler faz conta nem muda o pedido.
Conclusão: segue

**A4, o acesso ao banco está isolado?** O serviço fala com os dados pelas interfaces `Store`/`Tx` e pelos repositórios de `shop.persistence` (`src/main/java/shop/persistence/Tx.java:3-12`, `ProductRepository.java`, `OrderRepository.java`…). Um `grep` por `java.sql`, `SELECT`, `INSERT` e `UPDATE` fora de `persistence` não acha nada: todo o SQL está em `persistence/postgres/Pg*.java` e em `PostgresStore.java`. Duas ressalvas leves, nenhuma do tipo que o item condena:
- `PgOrders.hasPurchased` guarda no SQL a regra do que conta como compra (`src/main/java/shop/persistence/postgres/PgOrders.java:93-94`, `status IN ('PAID','SHIPPED','DELIVERED')`), um pedaço da regra do WELCOME10 que mora no repositório.
- O `MemoryStore`, usado só pelos testes (`src/test/java/shop/CheckoutExamplesTest.java`), mora no código de produção.
Conclusão: segue

**A5, um cálculo só para o resumo e para o pedido?** `CheckoutService.summary` e `CheckoutService.register` chamam o mesmo `calculator.calculate(request, tx)` (`src/main/java/shop/service/CheckoutService.java:25` e `:30`). O pedido grava o `Summary` que saiu dessa chamada (`:32-36`). O cálculo inteiro, com validação, frete, cupom, seguro e pagamento, está num lugar só (`src/main/java/shop/service/CheckoutCalculator.java:43-82`), e nenhuma etapa é refeita em outro fluxo.
Conclusão: segue

**A6, como os pacotes estão organizados?** A divisão é por camada no primeiro nível: `web`, `service`, `domain` e `persistence`, com `App` e `Json` na raiz. Dentro do domínio a divisão é por assunto (`domain.coupon`, `domain.payment`, `domain.shipping`), e dentro da persistência é por tecnologia (`persistence.postgres`, `persistence.memory`). No conjunto é misto, com predomínio das camadas.
Conclusão: não se aplica

## O que mudaria primeiro

1. **Tirar o status HTTP do domínio.** O `ErrorCode` ficaria só com os nomes, e a `DomainException` sem o `int status` e sem o `conflict()`. Para o caso do pagamento, o `OrderService` lançaria uma recusa própria (por exemplo, uma exceção de recusa de pagamento) ou o código de erro do contexto. O mapeamento de código para 404/409/422/503 ficaria numa tabela em `web.Routes`. Isso fecha o único item em `não segue`: hoje, mudar uma resposta HTTP significa mexer no enum de negócio e no serviço de pedidos.
2. **Separar a saída da API dos records do domínio e o cliente da TransLog do domínio.** Criar DTOs de resposta na camada web e tirar o `@JsonIgnore` de `Order` e `Notification`. Mover o `TransLogClient` (HTTP de saída, que usa o `shop.Json`) para um pacote de infraestrutura, deixando só a interface `CarrierClient` no domínio.

## Dúvidas

- **A1:** decidi que os números HTTP no `ErrorCode`, na `DomainException` e no `OrderService.java:60` contam como "os códigos HTTP" que a régua cita, mesmo sem nenhum tipo do Javalin no domínio. Já o `@JsonIgnore` do Jackson tratei como agravante, não como causa: ele molda a saída da API, mas não é do framework web.
- **A2:** o relatório agrupa pelos pacotes logo abaixo da raiz. Segui a régua ("para cada grupo do relatório") e não contei como ciclo a ida e volta entre `domain` e `domain.shipping`, nem a que passa pelo `shop.Json` da raiz. Só registrei as duas.
- **A6:** é um item descritivo e a régua não traz conclusão própria para ele. Usei `não se aplica`.

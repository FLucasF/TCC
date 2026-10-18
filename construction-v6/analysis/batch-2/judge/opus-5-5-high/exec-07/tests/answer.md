Já li os testes e o código principal que eles exercitam. Segue a correção.

# Testes

## Visão geral

O agente escreveu uma classe de testes de ponta a ponta, `src/test/java/shop/ServiceTest.java`, com 11 métodos. Cada um sobe o Javalin de verdade numa porta livre (`start(0)`) e o chama por HTTP. O banco é trocado por um dublê feito à mão (`src/test/java/shop/InMemoryStore.java`) e a TransLog por uma lambda no lugar da interface `CarrierGateway`. O relógio é fixo. O PIT rodou (`pit.txt`: `situation=ok`, `tests=11`), com `line_coverage=0.60`, `mutation_score=0.580` e `test_strength=0.921`. Dos 383 erros plantados, 222 foram pegos, só 19 passaram e 142 caíram em linhas que nenhum teste executa.

O ponto mais forte é a qualidade do que é exercitado. Sete dos dez exemplos do financeiro viram asserções de valor, campo a campo. O pagamento repetido é bem conferido no estoque, no crédito e nos avisos, e a fronteira das 48 horas é testada nos dois lados, com o relógio controlado.

Os problemas, do mais grave para o menos grave:
1. **A concorrência não é testada.** A última peça só é disputada em sequência, contra um dublê que serializa tudo com `synchronized`. Os bloqueios reais do `PostgresStore` nunca rodam.
2. **A persistência real e o cliente HTTP da TransLog ficam fora dos testes.** É aí, muito provavelmente, que está a maior parte dos 142 erros sem cobertura.
3. **Várias recusas e transições não são testadas,** e a conferência do `gift` é vazia (detalhe em T2).

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**

- **O cálculo: coberto.** `example1` a `example7` (`ServiceTest.java:80-136`) conferem os valores dos exemplos 1 a 7 do enunciado. Alguns conferem só parte dos campos: o `example2` não confere `productsSubtotal` nem `couponDiscount`, o `example5` não confere `productsSubtotal` nem `paymentAdjustment`. Ainda assim, cada campo (subtotal, desconto, frete, prazo, seguro, ajuste, total, parcela, crédito) aparece em pelo menos um exemplo. Os quatro cupons, o teto de 30% (exemplos 5 e 6), o limite do seguro (exemplo 7), os dias extras de Norte e Nordeste e o GOLD pagando o motoboy (exemplo 3) também aparecem. O `gift` não é conferido de fato (ver T2).
- **As recusas: em parte.** `refusals` (`ServiceTest.java:138-169`) confere pelo código `COUPON_NOT_APPLICABLE` (exemplo 8), `SHIPPING_METHOD_UNAVAILABLE`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK`, `INVALID_COUPON` (cupom vazio), `INVALID_INSTALLMENTS` e `INVALID_ORDER` (`{}`). Nas operações do pedido, os testes conferem `ORDER_EXPIRED`, `INSUFFICIENT_STOCK` no pagamento, `INVALID_ORDER` no envio sem rastreio e o 404 de pedido inexistente (só o status, `ServiceTest.java:232`). Ficam de fora:
  - o exemplo 9 (WELCOME10 de quem já comprou);
  - o MINUS50 abaixo de R$ 300,00;
  - o motoboy acima de 5 kg;
  - o boleto acima de R$ 1.000,00;
  - `UNKNOWN_PRODUCT` e as recusas de nível, região, CEP, entrega e pagamento;
  - a ordem de prioridade entre recusas;
  - `PRODUCT_NOT_FOUND`, `CUSTOMER_NOT_FOUND` e `NOTIFICATION_NOT_FOUND`.
  
  Todas as recusas testadas usam `POST /checkout/summary`. Nenhuma passa por `POST /orders`.
- **O ciclo do pedido: em parte.** `shipmentFlow` (`ServiceTest.java:223-235`) confere:
  - envio de pedido não pago: 409, sem conferir o código;
  - envio com rastreio, pelo `trackingCode`;
  - cancelamento depois do envio: 409;
  - entrega: `DELIVERED`.
  
  `paymentIsIdempotentAndMovesStockAndCredit` (`:196-200`) confere o cancelamento de um pedido pago: estoque de volta, crédito retirado, aviso a mais. Ficam de fora: o cancelamento de um pedido ainda não pago (pela rota de cancelamento), a entrega sem envio, o envio repetido e as operações sobre pedido cancelado ou entregue.
- **O pagamento repetido: coberto.** `ServiceTest.java:184-194`:
  - a segunda chamada devolve o mesmo corpo (`assertEquals(first.body(), again.body())`);
  - o estoque do FON-001 fica em 3 (5 − 2, uma baixa só);
  - o saldo fica em 8.00 (2% de 399,80, um crédito só);
  - há um aviso só;
  - outro `transactionId` dá 409.
  
  A recusa repetida também é conferida (`ORDER_EXPIRED` duas vezes, `:207-210`).
- **A concorrência: não coberta.** `expiryBoundaryAndOutOfStock` (`ServiceTest.java:213-219`) põe dois pedidos na MOC-001 (estoque 1) e os paga um depois do outro. O segundo recebe `INSUFFICIENT_STOCK` e o estoque fica em 0. Não há duas threads ao mesmo tempo. Além disso, o dublê faz `inTransaction` com `synchronized` (`InMemoryStore.java:41`) e o `lock` dele é um `find` comum (`InMemoryStore.java:52-54`). Por isso, o bloqueio que o serviço real usa contra a venda dupla nunca é exercitado.
- **A transportadora: em parte.** A TransLog fora do ar (`ServiceTest.java:152-157`, só o status 503) e o CEP não atendido (`:145-150`, só o código) aparecem. Porém a lambda lança direto a `ApiException` já pronta. A tradução da resposta da TransLog fica sem teste: o `{"error":"ZIP_NOT_SERVED"}`, um status diferente de 200, o tempo-limite de 2 s, o JSON inválido (`src/main/java/shop/shipping/TransLogGateway.java:27-65`). O mesmo vale para a conversão de centavos e para a montagem da URL com `weight_g` e `service`. Os testes cobrem como o serviço reage à recusa, não a integração com a TransLog.

Conclusão: em parte

**T2, as asserções conferem valores?** Na maior parte, sim. O `assertSummary` (`ServiceTest.java:69-76`) compara cada campo esperado com `compareTo` em `BigDecimal` e, no `finalTotal`, também o texto, o que confere as duas casas decimais. Os testes de ciclo conferem `status`, `trackingCode`, o código da recusa, o estoque no dublê, o saldo e a quantidade de avisos. Os números do PIT confirmam: a `test_strength` de 0.921 (só 19 sobreviventes) mostra que o código executado é de fato conferido.

Há três fraquezas:
1. **A conferência do `gift` é vazia.** O `assertSummary` aplica `decimalValue()` a todos os campos. Num nó booleano do Jackson, isso devolve zero, então `"gift":true` no `example7` (`:135`) e `"gift":false` no `example1` (`:86`) passam qualquer que seja o valor que o serviço devolva. A regra do brinde do GOLD fica, na prática, sem teste.
2. **Algumas asserções conferem só o status:** o 409 do envio antes do pagamento (`:225`), do cancelamento depois do envio (`:230`) e do segundo `transactionId` (`:194`), o 503 da TransLog (`:157`) e o 404 (`:232`). Nesses casos, um código errado no corpo passaria.
3. **Os avisos são contados, mas o `type` e o `orderId` nunca são conferidos** (`:193`, `:200`).

Conclusão: em parte

**T3, os testes rodam sem banco e sem internet, como o enunciado pede?** Sim:
- o banco é o `InMemoryStore`, que implementa `Store` e `Tx` em memória (`InMemoryStore.java:29`) e carrega o `InitialCatalog` (`:37`);
- a TransLog é uma lambda no lugar da `CarrierGateway` (`ServiceTest.java:36`);
- o HTTP vai só para `localhost` (`:54`);
- o `pom.xml` só traz `junit-jupiter` como dependência de teste, sem Testcontainers, sem driver configurado para teste e sem perfil que exija PostgreSQL.

O custo é o que T1 já registra: `PostgresStore`, `Schema` e `TransLogGateway` nunca rodam nos testes. Pelo tamanho desses arquivos (328, 99 e 70 linhas), provavelmente estão neles boa parte dos 142 erros sem cobertura. Não confirmei, porque o `pit.txt` não mostra os números por classe.

Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** Não:
- **Relógio:** é fixo, `Clock.fixed(Instant.parse("2026-10-14T15:30:00Z"), ZoneOffset.UTC)` (`ServiceTest.java:37`), e o serviço o recebe por injeção (`src/main/java/shop/order/OrderService.java:51`). O prazo de 48 h é medido entre esse `createdAt` e o `paidAt` enviado (`OrderService.java:83`). Por isso, a fronteira (`2026-10-16T15:30:00Z` aceito, `…15:30:01Z` recusado, `ServiceTest.java:206` e `:215`) é determinística.
- **Esperas:** nenhum `sleep` nos testes. O único fica no `Main.java:38`, fora dos testes.
- **Estado:** cada teste monta o seu. O `@BeforeEach` (`:33-39`) cria um `InMemoryStore` novo e um servidor novo, e o `@AfterEach` o para. O catálogo inicial é uma `List.of` de records imutáveis (`src/main/java/shop/catalog/InitialCatalog.java:7`). O `example7` muda o estoque da JAQ-001 só no seu próprio store (`:130`). Nenhum teste depende de outro ou da ordem de execução.

Conclusão: segue

## O que mudaria primeiro

1. **Testar a concorrência e a persistência reais.** Um teste com duas threads pagando a última peça ao mesmo tempo, contra um banco que respeite os bloqueios. Pode ser um banco em memória compatível com PostgreSQL ou um PostgreSQL embutido baixado pelo Maven, sem Docker. Hoje a garantia contra a venda dupla, o requisito que a dona da loja mais destacou, não é exercitada por nada, e o dublê `synchronized` esconde justamente esse risco.
2. **Testar o `TransLogGateway` contra um servidor HTTP falso local:** o `ZIP_NOT_SERVED`, um status 500, a demora acima de 2 s e a conversão de centavos e horas. Junto, corrigir o `assertSummary` para conferir o `gift` como booleano e acrescentar as recusas que faltam (exemplo 9, MINUS50 abaixo do mínimo, motoboy acima de 5 kg, boleto acima de R$ 1.000,00).

## Dúvidas

- Considerei a TransLog "coberta em parte". Os testes conferem a reação do serviço à recusa e à queda, mas não a leitura da resposta da TransLog. A régua pede "um teste confere a TransLog fora do ar, ou o CEP que ela não atende". Li o "em parte" porque a lambda lança a exceção já pronta, e o código que decide o que é "fora do ar" (`TransLogGateway.java`) não é exercitado.
- O motivo de o `gift` não ser conferido é o comportamento do Jackson: `JsonNode.decimalValue()` devolve `BigDecimal.ZERO` em nós não numéricos. Concluí isso lendo o código, sem rodar, como pede a régua para as dimensões de desenho.

# Testes

## Visão geral

O agente escreveu 14 testes em três classes (`pit.txt`: `situation=ok`, `tests=14`). Todas sobem o contexto Spring com H2 em memória e trocam a TransLog por um `@MockBean`. O ponto forte é o cálculo: `CheckoutCalculatorTest` reproduz os exemplos 1 a 7 do enunciado e confere cada um dos onze campos do resumo pelo valor exato. Os problemas, do mais grave ao menos grave:

1. **Nenhuma recusa é conferida pelo código de erro.** Os três testes de recusa usam `assertThrows(BusinessException.class, …)` sem olhar o código, e os exemplos 8 a 10 do enunciado (as recusas) não foram escritos.
2. **O crédito do clube e os avisos nunca são conferidos.** Nem no pagamento, nem no cancelamento, nem no aviso repetido.
3. **Não há teste de concorrência**, nem de entrega, nem de nenhuma chamada HTTP.
4. **A `TransLogService` inteira fica fora dos testes**, porque é ela que está trocada pelo dublê. Com isso, a conversão de centavos e de horas, o `ZIP_NOT_SERVED` e a TransLog fora do ar nunca são exercitados.

Os números do PIT mostram essa falta de alcance. Os testes pegam 74% dos erros plantados nas linhas que executam (`test_strength=0.738`), mas só 47% no total (`mutation_score=0.474`), porque 119 dos 333 erros plantados ficam em linhas que nenhum teste executa (`no_coverage=119`).

## Item a item

**T1, o que dos requisitos os testes cobrem.**
- **O cálculo: coberto.** `testExample1` a `testExample7` (`src/test/java/com/loja/orderservice/service/CheckoutCalculatorTest.java:60-253`) chamam `CheckoutCalculator.calculate` e conferem subtotal, desconto, frete, prazo, seguro, ajuste, total final, parcelas, valor da parcela, crédito e brinde, com os valores do enunciado. Uma ressalva: o dublê da TransLog já devolve o valor em reais e o prazo em dias. Por exemplo, `success(new BigDecimal("33.10"), 2)` (`CheckoutCalculatorTest.java:63`) substitui `{price_cents: 3310, eta_hours: 41}`, e `success(..., 3)` (`:228`) substitui as 49 horas. Assim, a conversão de centavos em reais e a de horas em dias com arredondamento para cima (`TransLogService.java:313-316` no arquivo `src/main/java/com/loja/orderservice/service/TransLogService.java`) ficam fora do teste. Ninguém confere também que o resumo e o pedido registrado dão os mesmos valores.
- **As recusas: não coberto pelo código.** Nenhum teste confere `INVALID_ORDER`, `UNKNOWN_PRODUCT`, `COUPON_NOT_APPLICABLE` ou qualquer outra recusa do resumo. Nas operações do pedido, `testCannotShipUnpaidOrder` (`src/test/java/com/loja/orderservice/integration/OrderIntegrationTest.java:155`) e `testCannotCancelShippedOrder` (`:182`) só conferem que saiu uma `BusinessException`, sem ver se foi `OPERATION_NOT_ALLOWED`. `testPaymentExpired` (`:203`) também não confere `ORDER_EXPIRED`, só que o pedido terminou `CANCELLED` (`:207`).
- **O ciclo do pedido: em parte.** Estão cobertos: pagar (`OrderIntegrationTest.java:72-74`), cancelar um pedido pago (`:132`), enviar como passo intermediário (`:180`), e duas transições proibidas, enviar sem pagar e cancelar depois do envio (`:155`, `:182`). Também a expiração com 48h+1s (`:201`). Faltam a entrega (`deliverOrder` não é chamado em nenhum teste), o cancelamento de um pedido que aguarda pagamento, o pagamento com exatamente 48h e o pagamento com outro `transactionId`.
- **O pagamento repetido: em parte.** `testDuplicatePaymentNotification` (`OrderIntegrationTest.java:80-107`) confere que o estoque não baixa duas vezes (`:106`). Não confere o crédito: o cliente é `BRONZE` (`:89`), com crédito zero, então um crédito dado em dobro passaria despercebido. Também não confere que a segunda resposta é igual à primeira.
- **A concorrência: não coberto.** Nenhum teste dispara duas compras ao mesmo tempo (não há `Thread`, `ExecutorService` nem `CompletableFuture` em `src/test`).
- **A transportadora: não coberto.** A `TransLogService` é sempre `@MockBean` (`CheckoutCalculatorTest.java:37`, `OrderIntegrationTest.java:36`), e o dublê sempre responde `success`. `notServed()` e `unavailable()` nunca são usados, então nem `SHIPPING_METHOD_UNAVAILABLE` nem `CARRIER_UNAVAILABLE` são exercitados (o exemplo 10 ficou de fora).

Os testes cobrem bem o cálculo e só parte do ciclo e do pagamento repetido. As recusas por código, a concorrência e a transportadora ficaram de fora.
Conclusão: em parte

**T2, as asserções conferem valores?** Depende da classe:
- Em `CheckoutCalculatorTest`, todos os 7 testes conferem os onze valores do resumo com `assertEquals` sobre `BigDecimal` exato (por exemplo `:77-87`). É o caso bom.
- Em `OrderIntegrationTest`, três dos seis testes conferem valores do negócio: estoque 48 depois do pagamento (`:77`), estoque igual no aviso repetido (`:106`), estoque 8 e depois 10 no cancelamento (`:130`, `:135`). Mais a situação `PAID` (`:74`) e `CANCELLED` (`:207`).
- Três testes só conferem que saiu uma exceção qualquer (`:155`, `:182`, `:203`). Um erro que devolvesse `INSUFFICIENT_STOCK` em vez de `OPERATION_NOT_ALLOWED` passaria.
- Em `testCreateOrderAndPayment`, quatro asserções são fracas (`assertNotNull` de id e createdAt, `assertNull` do rastreio, `:63-66`), e o resumo do pedido registrado não é conferido.
- O crédito (`customer.addCredit`/`subtractCredit` em `src/main/java/com/loja/orderservice/service/OrderService.java:120` e `:187`) e os avisos (`notificationService.notifyCustomer`, `OrderService.java:104,114,127,149,194`) nunca são conferidos. Um erro plantado ali passa.
- `contextLoads` (`src/test/java/com/loja/orderservice/OrderServiceApplicationTests.java:12`) não confere nada.

Os números do PIT dizem o mesmo. Onde os testes chegam, conferem razoavelmente (`test_strength=0.738`), mas 56 erros plantados passaram mesmo em linhas executadas. Eles se concentram onde o teste roda o código sem conferir o resultado: o crédito, os avisos e o código da recusa.
Conclusão: em parte

**T3, rodam sem banco e sem internet?** Sim. O perfil `test` aponta para H2 em memória (`src/test/resources/application-test.properties:1-8`), e o H2 entra no `pom.xml` só com escopo `test`. Não há Testcontainers nem dependência de Docker. A TransLog é um `@MockBean` nas duas classes que fazem pedidos (`CheckoutCalculatorTest.java:37`, `OrderIntegrationTest.java:36`). `OrderServiceApplicationTests` sobe a `TransLogService` de verdade apontando para `http://localhost:9999` (`application-test.properties:10`), mas só carrega o contexto, e nada chama a TransLog na subida, então isso não exige rede. Ressalva menor: as consultas `PESSIMISTIC_WRITE` (`CustomerRepository`, `ProductRepository`) rodam em H2, não em PostgreSQL, então o travamento real não é exercitado. Isso não fere a exigência do enunciado.
Conclusão: segue

**T4, dependem de coisas que mudam sozinhas?** Não encontrei nenhum teste que hoje dependa disso:
- **O relógio.** O código pega o relógio de verdade (`Instant.now()` no construtor, `src/main/java/com/loja/orderservice/entity/Order.java:65`, sem um `Clock` que se possa trocar). Mas a regra das 48 horas compara o `paidAt` do aviso com o `createdAt`, e o teste da expiração monta o `paidAt` a partir do `createdAt` devolvido (`OrderIntegrationTest.java:201`, `createdAt + 48h + 1s`), então o resultado não depende de quando o teste roda. Nos outros testes, `paidAt = Instant.now()` (`:70`, `:96`, `:125`, `:175`) só precisa estar logo depois do registro, o que sempre acontece.
- **`sleep` e ordem.** Não há `sleep`, `@Order` nem dependência da ordem de execução.
- **O estado entre testes.** Cada teste monta o que usa. `CheckoutCalculatorTest` apaga produtos e pedidos e recria os produtos (`:45-57`). `OrderIntegrationTest` apaga e recria só os produtos (`:41-46`); pedidos, clientes e avisos se acumulam entre os testes, e o isolamento vem de cada teste usar um CPF diferente (`11111111111` a `66666666666`). Isso funciona, mas é frágil: um teste novo com `WELCOME10` e um CPF repetido herdaria pedidos pagos de outro teste.
- **Uma limitação.** Como o tempo não é controlado no código, o caso de exatamente 48 horas seria difícil de testar com segurança: o `createdAt` gravado pode perder precisão em relação ao que está na memória.

Conclusão: segue

## O que mudaria primeiro

1. **Conferir o código das recusas e o crédito.** Trocar os `assertThrows(BusinessException.class, …)` por asserções sobre o código (`OPERATION_NOT_ALLOWED`, `ORDER_EXPIRED`), acrescentar os exemplos 8 a 10, e usar um cliente `SILVER` ou `GOLD` nos testes de pagamento, cancelamento e aviso repetido, conferindo o `creditBalance` e os avisos gerados. É o que mais subiria o `mutation_score`: hoje os erros plantados no crédito, nos avisos e nos códigos passam.
2. **Testar a `TransLogService` contra um servidor HTTP falso** (MockWebServer ou WireMock), em vez de trocá-la inteira. Isso cobriria a conversão de centavos e de horas, o `ZIP_NOT_SERVED` e o tempo limite, e fecharia boa parte dos 119 erros plantados sem cobertura. Logo depois viria um teste de duas compras simultâneas da última peça.

## Dúvidas

- **O pagamento repetido.** Classifiquei como "em parte" porque o estoque é conferido e o crédito não. Como o cliente do teste é `BRONZE` (crédito zero), o teste não conseguiria pegar um crédito dado em dobro nem que quisesse.
- **T4.** Fiquei entre `segue` e `em parte`. O código não tem um relógio controlável e o estado de pedidos e clientes se acumula entre os testes. Escolhi `segue` porque nenhum teste de hoje depende de nenhuma das duas coisas para passar; a fragilidade está descrita no item.

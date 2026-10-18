# testes

## Visão geral

O agente escreveu 9 testes em duas classes. A principal, `LojaIntegrationTest`, sobe o serviço inteiro numa porta aleatória, usa um H2 em memória no modo PostgreSQL no lugar do banco e um dublê da interface `CarrierGateway` no lugar da TransLog. A segunda, `TransLogGatewayTest`, testa o cliente HTTP da TransLog contra um servidor HTTP local, sem sair da máquina.

O ponto mais forte é o que as asserções conferem. Os sete exemplos do financeiro com valores são conferidos campo a campo (os onze campos do resumo). As recusas são conferidas pelo código de erro, e o pagamento repetido é conferido pela resposta igual, pelo estoque e pelo saldo. Os números do PIT confirmam isso: `mutation_score` 0,935 (231 dos 247 erros plantados foram pegos), `test_strength` 0,971 e `line_coverage` 0,94 (`pit.txt`).

Os problemas, do mais sério para o menos sério:
1. Não há teste de concorrência. A disputa pela última peça é testada em sequência, não com duas compras ao mesmo tempo.
2. Várias recusas e transições proibidas são conferidas só pelo status HTTP.
3. Um teste marca como lido o aviso de id fixo `1`, que pode ser de outro cliente, conforme a ordem em que os testes rodam.
4. Não há teste do cupom WELCOME10 com cliente que já comprou (exemplo 9) nem da maior parte da tabela de recusas.

## Item a item

**T1, o que dos requisitos do enunciado os testes cobrem?**

- **O cálculo: coberto.** `financeExamples` (`src/test/java/br/com/loja/LojaIntegrationTest.java:100-141`) faz os exemplos 1 a 7 em `/checkout/summary`. Cada um passa por `assertSummary` (`:83-96`), que confere todos os campos do resumo. Isso cobre:
  - os quatro cupons, inclusive o teto de 30% (exemplos 5 e 6);
  - o acréscimo de prazo do Norte (exemplos 1 e 7);
  - o limite do seguro (exemplo 7);
  - os juros da tabela Price (exemplo 2), o Pix, o boleto e o brinde do GOLD.

  Ficam de fora:
  - o exemplo 9 (WELCOME10 com pedido pago anterior);
  - a regra de que o pedido registrado tem o mesmo resumo da prévia: em `orderLifecycleAndIdempotentPayment` (`:153`) o pedido é criado sem comparar o `summary` com o de `/checkout/summary`.

- **As recusas: em parte.** São conferidas pelo código:
  - `COUPON_NOT_APPLICABLE` (`:131`), `SHIPPING_METHOD_UNAVAILABLE` (`:135`), `CARRIER_UNAVAILABLE` (`:140`) e `INVALID_COUPON` (`:146`), no resumo;
  - `INSUFFICIENT_STOCK` (`:180`) e `ORDER_EXPIRED` (`:218`), nas operações do pedido.

  Fica sem teste algum a maior parte da tabela de 15 recusas: `INVALID_ORDER`, `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_ZIP_CODE`, `INVALID_SHIPPING_METHOD`, `INVALID_PAYMENT_METHOD`, `INVALID_INSTALLMENTS`, `PAYMENT_METHOD_UNAVAILABLE`, o motoboy acima de 5 kg e a ordem entre as conferências.

- **O ciclo do pedido: em parte.**
  - Passos permitidos: criar fica em `AWAITING_PAYMENT` (`:156`), pagar vai a `PAID` (`:168`), enviar grava o rastreio (`:188`) e entregar vai a `DELIVERED` (`:190-191`). O cancelamento de pedido pago aparece em `paidOrderCancellationRestoresStockAndCredit` (`:202`).
  - Passos proibidos: entregar antes de enviar (`:185`) e cancelar depois de enviado (`:189`).
  - Faltam: enviar um pedido não pago, cancelar um pedido aguardando pagamento (o caso sem devolução), qualquer operação sobre um pedido cancelado ou entregue, e o aviso `ORDER_SHIPPED`.

- **O pagamento repetido: coberto.** O mesmo `TX-1` é enviado duas vezes. O teste confere que:
  - a segunda resposta é idêntica à primeira (`:169-170`);
  - o estoque da mochila fica em zero (`:173`);
  - o saldo fica em 8,00, e não em 16,00 (`:174-175`);
  - há um só aviso (`:176`);
  - outro `transactionId` dá 409 (`:171`).

  Também confere que a recusa por falta de estoque se repete igual (`:181-182`).

- **A concorrência: não coberta.** O teste das duas compras da última mochila (`:160-183`) faz os dois pagamentos um depois do outro, na mesma thread. Ele confere a recusa por falta de estoque, mas não a disputa: não há threads, `ExecutorService` nem nada que dispare as duas confirmações juntas. Além disso, o `SELECT ... FOR UPDATE` de `src/main/java/br/com/loja/catalog/ProductRepository.java:43` roda sobre o H2, cujo travamento não é o do PostgreSQL.

- **A transportadora: coberta.**
  - Pelo dublê: CEP não atendido (`LojaIntegrationTest.java:133-135`) e TransLog fora do ar com 503 (`:137-140`).
  - No cliente HTTP (`src/test/java/br/com/loja/TransLogGatewayTest.java`): leitura do preço e do prazo arredondado para cima (`:45-50`), `ZIP_NOT_SERVED` (`:53-57`), resposta 500 (`:60-64`) e demora acima de 2 segundos (`:67-71`).
  - Nenhum teste confere o que é enviado à TransLog. O dublê ignora os argumentos (`LojaIntegrationTest.java:37`) e o servidor falso não lê a query (`TransLogGatewayTest.java:23`). Por isso, a conversão para gramas (`src/main/java/br/com/loja/shipping/CarrierShipping.java:28`) e a escolha entre `STD` e `EXP` ficam sem verificação.

Conclusão: em parte

**T2, as asserções conferem valores?** Na maioria, sim.
- **Conferem valores de negócio:**
  - os sete resumos, comparados campo a campo com `isEqualByComparingTo` (`LojaIntegrationTest.java:85-95`);
  - a situação do pedido depois de cada operação (`:156`, `:168`, `:183`, `:191`);
  - o estoque (`:173`, `:203`) e o saldo de crédito (`:157-158`, `:174-175`, `:204-205`);
  - os códigos das recusas (`:131`, `:135`, `:140`, `:146`, `:180`, `:218`);
  - nos testes da TransLog, o preço e os dias (`TransLogGatewayTest.java:48-49`) e o tipo da exceção.
- **Conferem só o status HTTP**, sem o código (que é o que o enunciado define):
  - o pagamento com outra transação (`LojaIntegrationTest.java:171`);
  - a entrega antes do envio (`:185`);
  - o envio sem rastreio (`:186`);
  - o cancelamento depois do envio (`:189`);
  - o pedido inexistente (`:192`);
  - o pagamento com exatamente 48 horas (`:224`), que não confere que o pedido ficou `PAID`.
- **Conferem o contrário, só o código:** `:135` e `:146` não conferem o 422.
- **Contam os avisos sem olhar o tipo** (`:176`, `:206`), então `PAYMENT_APPROVED` e `ORDER_CANCELLED` não são distinguidos.

Os números do PIT batem com essa leitura. A cobertura de linhas é de 0,94 e a força dos testes de 0,971: dos 238 erros plantados em linhas executadas, só 7 passaram. Não é o caso de testes que executam o código sem conferir. Os 9 erros sem cobertura ficam nas linhas que nenhum teste executa, provavelmente nas recusas que não têm teste.

Conclusão: segue

**T3, os testes rodam sem banco e sem internet?** Sim.
- **O banco:** é um H2 em memória (`spring.datasource.url=jdbc:h2:mem:loja;MODE=PostgreSQL`, `LojaIntegrationTest.java:25-27`, com `h2` no escopo de teste em `pom.xml`), e o Flyway cria nele o esquema e o catálogo de `V1__schema.sql`.
- **A TransLog:** é trocada por um bean `@Primary` que implementa `CarrierGateway` (`LojaIntegrationTest.java:32-48`), e `translog.url` fica vazio (`application.yml:8`).
- **O cliente HTTP:** `TransLogGatewayTest` usa `com.sun.net.httpserver.HttpServer` em `localhost` numa porta livre (`TransLogGatewayTest.java:22`).
- Não há Testcontainers, Docker nem PostgreSQL em nenhum teste, e a medida do PIT rodou (`situation` = `ok`).

A ressalva: o H2 no modo PostgreSQL não é o PostgreSQL. O `FOR UPDATE` e o `TIMESTAMP WITH TIME ZONE` passam pelo H2 e não são provados contra o banco de produção. Isso é o preço de rodar sem banco, não um descumprimento.

Conclusão: segue

**T4, os testes dependem de coisas que mudam sozinhas?** Em parte.

**O tempo está bem resolvido.** O serviço recebe um `Clock` injetado (`src/main/java/br/com/loja/order/OrderService.java:35,55`, bean em `LojaApplication.java:17-18`). O teste das 48 horas não depende do relógio, porque a regra compara o `paidAt` enviado com o `createdAt`: o teste lê o `createdAt` devolvido e soma `48 * 3600 + 1` e `48 * 3600` segundos (`LojaIntegrationTest.java:215-216`, `:222-223`).

**O `sleep` é aceitável.** O único está no servidor falso, para simular a demora (`TransLogGatewayTest.java:25`, 2.500 ms contra o limite de 2 s). Ele não é usado para esperar um resultado e não gera falha intermitente, só deixa o teste mais lento.

**O estado é compartilhado:**
- **O banco:** os cinco testes de `LojaIntegrationTest` usam o mesmo H2 (`DB_CLOSE_DELAY=-1`, sem limpeza entre os testes) e o mesmo `static AtomicReference CARRIER` (`:30`). O agente compensa com um CPF diferente por teste, produtos que só um teste paga (a mochila só em `orderLifecycleAndIdempotentPayment`) e o estoque lido antes de conferir a volta (`:201-203`).
- **O aviso de id fixo:** `paidOrderCancellationRestoresStockAndCredit` marca como lido `/notifications/1/read` (`:207`). Qual aviso tem o id 1 depende de qual teste pagou ou cancelou primeiro. O teste passa em qualquer ordem só porque algum aviso 1 sempre existe, mas pode estar conferindo o aviso de outro cliente.
- **O dublê da TransLog:** o valor de `CARRIER` deixado por `financeExamples` continua lá. Hoje não afeta nada, porque os outros testes usam `STORE_PICKUP`.

Conclusão: em parte

## O que mudaria primeiro

1. **Um teste de concorrência de verdade:** duas threads confirmando ao mesmo tempo dois pedidos da mesma última peça, conferindo que só um vira `PAID`, que o outro recebe `INSUFFICIENT_STOCK` e que o estoque termina em 0. É o requisito que o enunciado destaca como um problema real da loja, e hoje nenhum teste o exercita.
2. **Conferir o código nas recusas que hoje só olham o status, e cobrir o resto da tabela:** a ordem de 1 a 15 do resumo, o exemplo 9 e o motoboy acima de 5 kg, num teste parametrizado. No mesmo passo, ler o id do aviso pela lista do próprio cliente em vez do `1` fixo. Isso fecha a maior parte dos erros plantados sem cobertura e tira a dependência da ordem dos testes.

## Dúvidas

- **H2 no lugar do PostgreSQL (T3):** tratei como forma válida de "rodar sem banco", porque o enunciado proíbe banco e Docker no `mvn verify`. Registrei como ressalva que ele não prova o travamento do PostgreSQL.
- **O `sleep` de 2,5 s do servidor falso (T4):** não contei como dependência de tempo instável. Ele simula a demora da TransLog, não espera um resultado, e a margem só pode atrasar o teste, nunca fazê-lo falhar à toa.

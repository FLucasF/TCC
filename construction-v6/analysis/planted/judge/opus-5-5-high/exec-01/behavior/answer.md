# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Ele sobe em cerca de 2,4 s com o banco vazio, cria as tabelas e carrega o catálogo sozinho (`src/main/resources/schema.sql`, `data.sql`). Os dez exemplos do financeiro conferem campo a campo, e as fronteiras também: o motoboy em 5,00 kg, o MINUS50 abaixo e acima de 300, o teto de 30%, o "meio para o par" no desconto do Pix. Todas as linhas da tabela "Quando dá erro" devolvem o status e o código certos, e a ordem de conferência vale nos 15 pares de problemas simultâneos que testei. Na integração com a TransLog, os centavos viram reais, as horas viram dias arredondando para cima e o tempo limite de 2 s é respeitado; o motoboy e a retirada funcionam com a TransLog fora do ar. O ciclo do pedido, o aviso de pagamento repetido (inclusive simultâneo, nas duas máquinas) e a última peça (100 corridas com uma barreira entre as duas máquinas) se comportaram certo. Os dados sobrevivem ao reinício, e o `mvn verify` passa com 39 testes. O único problema que encontrei fica fora das condições do teste: `created_at` é gravado como `TIMESTAMP` sem fuso (`schema.sql:20`). Com a JVM fora de UTC, o horário de registro volta deslocado e o prazo de 48 h é calculado errado (detalhe em "Dúvidas").

## Item a item

**C1, o serviço sobe.** Copiei o projeto, compilei com `mvn -q package -DskipTests` (7 s) e subi o jar com `TRANSLOG_URL=http://localhost:9099 java -jar … --server.port=8081`, apontando para o banco vazio. O log mostra `Started OrdersApplication in 2.392 seconds`, sem erro. As tabelas vêm de `schema.sql` (`CREATE TABLE IF NOT EXISTS`) com `spring.sql.init.mode=always` (`application.properties`), e o banco vem de `DB_URL`, `DB_USER` e `DB_PASSWORD` (`config/AppConfig.java`). A primeira chamada a `GET /products/CAM-001` já respondeu 200.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs do anexo. Todos voltaram 200 com nome, preço, peso, estoque e `discontinued` iguais ao anexo; por exemplo, `{"sku":"REL-001","name":"Relógio digital","price":129.90,"weightKg":0.20,"stock":8,"discontinued":true}`, e o BON-001 aparece com `stock` 0. Um SKU inexistente devolve `404 {"error":"PRODUCT_NOT_FOUND"}`. Pedir o REL-001 no resumo devolve `422 PRODUCT_DISCONTINUED`. A carga usa `INSERT … WHERE NOT EXISTS` (`data.sql:2`), e o estoque não foi recarregado no reinício (C9).
Conclusão: segue

**C3, o resumo.** Configurei o stub com a resposta de cada exemplo e chamei `POST /checkout/summary` com o CEP 01310100:
- **Exemplos 1 a 7:** os onze campos batem com o enunciado. Por exemplo, o Ex. 2 deu `409.70 / 0.00 / 15.60 / 7 / 6.15 / 30.55 / 462.00 / 6 / 77.00 / 8.19 / false`, e o Ex. 7 deu `1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / 1 / 1377.12 / 70.98 / true`.
- **Pesos enviados à TransLog:** o log do stub mostra `weight_g=1800` (Ex. 1 e 2), `900`, `100`, `3600` e `1200`, todos certos.
- **Ex. 8 e 10:** `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Ex. 9:** registrei e paguei um pedido para o CPF 10000000009; o resumo seguinte com WELCOME10 deu `422 COUPON_NOT_APPLICABLE`. Para controle, um CPF novo com o mesmo carrinho recebeu o desconto de 15,98.

Fronteiras:
- **Motoboy:** MEI-001 × 50 (5,00 kg) passa com frete 18,00; × 51 (5,10 kg) dá `SHIPPING_METHOD_UNAVAILABLE`.
- **MINUS50:** com 299,70 em produtos dá `COUPON_NOT_APPLICABLE`; com 309,60 o desconto é 50,00. Com este catálogo não dá para montar exatamente 300,00, porque todos os preços terminam em ,90.
- **Teto de 30%:** com FREESHIPPING, MEI × 1 (teto 5,97) e frete 5,97, o desconto é 5,97; com frete 5,98, o desconto continua 5,97.
- **Meio para o par:** com total do pedido de 100,10, o Pix daria 5,005 de desconto e o ajuste veio −5,00; com 100,50 (5,025) veio −5,02. Nos dois casos o arredondamento "meio para cima" daria −5,01 e −5,03.

Outros casos:
- **GOLD:** no motoboy paga 18,00 de frete; na econômica o frete sai 0,00; com 589,80 em produtos, `gift` vem `true`.
- **BUY3PAY2** sem 3 unidades de nenhum produto: desconto 0,00.
- **Cartão:** em 4x e 12x conferi a parcela com a fórmula Price em jshell (21,1886 → 21,19; 7,6263 → 7,63).
- **Boleto** com total acima de 1.000: `PAYMENT_METHOD_UNAVAILABLE`.

Conclusão: segue

**C4, as recusas.** Testei cada linha da tabela, uma alteração por vez, sobre um corpo que dá 200:
- **`INVALID_ORDER`:** carrinho vazio, sem `items`, item sem SKU, SKU repetido, quantidade 0, −1, ausente ou 1,5, sem CPF, sem e-mail, sem `customer`, corpo vazio e JSON mal formado.
- **Uma condição de cada:** `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` (BON-001 × 1 e MOC-001 × 2; MOC-001 × 1 passa).
- **Valores inválidos ou ausentes:**
  - `INVALID_CLUB_TIER`: inválido, ausente ou em minúsculas;
  - `INVALID_REGION`;
  - `INVALID_ZIP_CODE`: ausente, 7 dígitos ou com traço;
  - `INVALID_SHIPPING_METHOD`;
  - `INVALID_COUPON`: inexistente, `""` ou `welcome10`;
  - `INVALID_PAYMENT_METHOD`;
  - `INVALID_INSTALLMENTS`: Pix 2x, cartão 13x e 0x, boleto 2x.
- **`PAYMENT_METHOD_UNAVAILABLE`:** boleto acima de 1.000.

Todas essas recusas vieram com 422 e o corpo `{"error":"…"}`; o 503 vale para a TransLog (C5). `POST /orders` com erro devolve o mesmo código (`422 INVALID_CLUB_TIER`).

Dois problemas ao mesmo tempo, sempre com o de menor ordem vencendo:
- SKU repetido e desconhecido → `INVALID_ORDER`;
- desconhecido e fora de linha → `UNKNOWN_PRODUCT`;
- fora de linha e sem estoque → `PRODUCT_DISCONTINUED`;
- sem estoque e nível inválido → `INSUFFICIENT_STOCK`;
- nível e região → `INVALID_CLUB_TIER`;
- região e CEP → `INVALID_REGION`;
- CEP e entrega → `INVALID_ZIP_CODE`;
- entrega e cupom → `INVALID_SHIPPING_METHOD`;
- motoboy acima de 5 kg e cupom inválido → `SHIPPING_METHOD_UNAVAILABLE`;
- TransLog fora do ar e cupom inválido → `503 CARRIER_UNAVAILABLE`;
- cupom inválido e pagamento inválido → `INVALID_COUPON`;
- cupom não aplicável e pagamento inválido → `COUPON_NOT_APPLICABLE`;
- pagamento e parcelas → `INVALID_PAYMENT_METHOD`;
- boleto 2x acima de 1.000 → `INVALID_INSTALLMENTS`;
- sem e-mail e nível inválido → `INVALID_ORDER`.

Conclusão: segue

**C5, a TransLog.** Fiz um stub com o `HttpServer` do JDK que responde por CEP e serviço e registra as chamadas.

Traduções:
- **Preço e prazo:** `{"price_cents":3310,"eta_hours":41}` deu frete 33,10 e prazo de 2 dias no Sul, 5 no Norte (+3) e 4 no Nordeste (+2).
- **Horas em dias:** 48 h → 2; 49 h → 3; 1 h → 1; 0 h → 0.
- **Centavos:** `price_cents` 5 → 0,05.
- **Serviço:** a econômica pede `service=STD` e a expressa `EXP`, como mostra o log do stub.

Casos de erro:
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` deu `422 SHIPPING_METHOD_UNAVAILABLE` nas duas opções.
- **Viram `503 CARRIER_UNAVAILABLE`:**
  - outro `error`;
  - status 500 ou 404;
  - corpo inválido, vazio ou `{}`;
  - `price_cents` como texto;
  - `price_cents` 3310.7 ou `eta_hours` 41.5, o meu caso de "preço com mais de duas casas": o serviço trata como resposta inválida, ou seja, TransLog fora do ar;
  - atraso de 2.500 ms: a recusa vem em 2.019 ms.
- **Atraso de 1.500 ms:** responde normalmente.

TransLog fora do ar: subi uma instância com `TRANSLOG_URL=http://localhost:9`. A econômica e a expressa deram 503 (inclusive no `POST /orders`), e o motoboy e a retirada deram 200, tanto no resumo quanto no registro. Com o stub respondendo 500, o motoboy e a retirada também funcionaram.
Conclusão: segue

**C6, o ciclo do pedido.**
- **Registro:** antes do primeiro pedido, `GET /customers/50000000001` dava `404 CUSTOMER_NOT_FOUND`. O registro (`201`, `AWAITING_PAYMENT`, `trackingCode` null) traz um `summary` idêntico ao do `/checkout/summary` com os mesmos dados (comparei os dois JSON). O cliente passa a existir com saldo 0,00, a lista de avisos fica vazia e o estoque do CAM-001 continua 50.
- **Pagamento:** o estoque baixa de 50 para 48, o saldo do SILVER vai para 3,20 e surge um aviso `PAYMENT_APPROVED NEW`.
- **Envio e entrega:**
  - envio sem `trackingCode` → `422 INVALID_ORDER`;
  - envio com `BR123456789` → `SHIPPED` com o código, e um aviso `ORDER_SHIPPED`;
  - cancelar o pedido enviado → `409 OPERATION_NOT_ALLOWED`;
  - entregar → `DELIVERED`;
  - entregar de novo, cancelar ou enviar o entregue → 409.
- **Fora de ordem:** enviar ou entregar um pedido aguardando pagamento dá 409, e entregar um pedido pago também. Pagar um pedido cancelado com outro `transactionId` dá 409.
- **Cancelar um pedido pago** (GOLD, TEN × 1, crédito 12,50): o estoque vai de 10 para 9 e volta a 10, o saldo vai de 12,50 para 0,00 e surge um aviso `ORDER_CANCELLED`. Cancelar de novo dá 409. Depois disso, o WELCOME10 vale para esse CPF (desconto 7,99), porque o cancelado não conta como compra.
- **Cancelar um pedido não pago:** o estoque do FON-001 fica em 5, o saldo em 0,00, e surge só o aviso `ORDER_CANCELLED`.
- **Prazo de 48 h:**
  - com `paidAt` em `createdAt` + 48 h 00 min 01 s → `409 ORDER_EXPIRED`, o pedido vai para `CANCELLED`, o estoque não muda e surge um aviso `ORDER_CANCELLED`;
  - com 48 h exatas → `PAID`;
  - com um `paidAt` em `-03:00` equivalente a +47 h 59 min → `PAID`.
- **Sem estoque na hora do pagamento:** registrei dois pedidos do MOC-001 (estoque 1) e paguei os dois em sequência. O segundo deu `409 INSUFFICIENT_STOCK`, foi para `CANCELLED` e gerou um aviso; o estoque ficou em 0, nunca negativo.
- **Campos e inexistentes:**
  - sem `transactionId`, sem `paidAt` ou com `paidAt` inválido → `422 INVALID_ORDER`;
  - pedido inexistente, mesmo com o corpo vazio → `404 ORDER_NOT_FOUND`;
  - `GET` de um pedido inexistente → 404.
- **Avisos:**
  - `POST /notifications/{id}/read` → `READ`, e repetir mantém `READ`;
  - aviso inexistente → `404 NOTIFICATION_NOT_FOUND`;
  - avisos de um CPF desconhecido → `404 CUSTOMER_NOT_FOUND`;
  - a lista vem do mais antigo para o mais novo (`ORDER BY seq`, `NotificationRepository.java`).
- **Sem agendador:** não há `@Scheduled` no código; o pedido só muda com um aviso ou um cancelamento.

Conclusão: segue

**C7, o aviso de pagamento repetido.** Paguei um pedido do JAQ-001 com `TX-REP` e repeti o mesmo aviso, uma vez na 8081 e outra na 8082 (com outro `paidAt`). Comparei a resposta HTTP inteira, sem o cabeçalho `Date`: as três são idênticas. O estoque baixou só uma vez (3 → 2), o crédito entrou só uma vez (19,50) e há um único aviso `PAYMENT_APPROVED`.
- **Outro `transactionId` em pedido pago:** `409 OPERATION_NOT_ALLOWED`.
- **Repetir o aviso expirado:** devolve de novo `409 ORDER_EXPIRED`; outro `transactionId` nesse pedido dá 409 `OPERATION_NOT_ALLOWED`.
- **Repetir o aviso recusado por estoque:** devolve de novo `409 INSUFFICIENT_STOCK`.
- **Mesmo aviso simultâneo nas duas máquinas, 5 rodadas:** sempre 200/200, corpos idênticos, estoque baixando uma unidade e um único aviso.

A deduplicação usa a tabela `payment_notice` e é consultada antes da checagem de situação (`OrderService.java:81` e `:85`), sob `SELECT … FOR UPDATE` do pedido (`OrderRepository.java:75`).
Conclusão: segue

**C8, a última peça.** Subi duas instâncias (8081 e 8082) no mesmo banco. Em cada rodada registrei dois pedidos de MOC-001 × 1 (estoque 1) e disparei os dois pagamentos, um em cada máquina. Primeiro fiz 20 rodadas com `curl` em paralelo; depois 100 rodadas com um disparador Java que solta os dois pedidos juntos por uma `CyclicBarrier`, trocando as máquinas de lado na metade. Ao fim de cada rodada, cancelei o pedido pago para devolver a peça.
- **Resultado:** em todas as 120 rodadas, exatamente um pedido ficou `PAID`, o outro recebeu `409 INSUFFICIENT_STOCK` e foi para `CANCELLED`, e o estoque ficou em 0. O menor estoque visto foi 0.
- **Venceram as duas máquinas:** na segunda série de 50, a 8082 venceu uma vez.
- **Sem erro nem deadlock** nos logs.

O estoque é lido com `FOR UPDATE` em ordem de SKU (`ProductRepository.java:48`).
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar, guardei os estoques dos oito produtos, três pedidos (entregue, expirado, recusado por estoque) e o saldo e os avisos de três clientes, um deles com aviso `READ`. Derrubei todas as instâncias, subi de novo (2,4 s) e repeti as consultas: `diff` sem nenhuma diferença. Os estoques (CAM 48, MEI 97, JAQ 2, MOC 1…) batem com o que os pagamentos e cancelamentos fizeram, e o catálogo não foi recarregado.
Conclusão: segue

**C10, os testes do próprio agente.** Numa terceira cópia, rodei `mvn -q verify` sem as variáveis de banco (os testes usam H2). Terminou com código 0 em 14 s e 39 testes, sem falhas, erros ou testes pulados: CheckoutSummaryTest 11, CheckoutValidationTest 13, OrderLifecycleTest 13, JdbcUrlTest 2 (`target/surefire-reports`).
Conclusão: segue

## O que mudaria primeiro

1. **Gravar `created_at` como `TIMESTAMP WITH TIME ZONE`** (`schema.sql:20`), como já é o `paid_at`. Hoje o valor é gravado convertido para o fuso da sessão e lido como se fosse UTC (`OrderRepository.java:45` e `:154`). Com a JVM fora de UTC, o horário de registro fica errado e o prazo de 48 h é calculado errado; isso atinge diretamente uma regra de dinheiro e estoque.
2. **Guardar `final_total` como `NUMERIC(12,2)`, como os outros valores.** Hoje é `DOUBLE PRECISION` (`schema.sql:30`). Nos testes o valor voltou certo porque é arredondado na leitura, mas é o único valor em dinheiro guardado em ponto flutuante.

## Dúvidas

- **O fuso do servidor.** O enunciado não diz em que fuso o serviço roda; esta máquina e o banco estão em UTC, e aí tudo conferiu. Subi uma instância extra com `-Duser.timezone=America/Sao_Paulo`. O `POST /orders` devolveu `createdAt` `19:13:54Z`, mas o `GET` do mesmo pedido devolveu `16:13:54Z`, nas duas instâncias. Um pagamento em `createdAt` + 47 h foi recusado com `409 ORDER_EXPIRED`. Considerei isso fora das condições do enunciado, então não mudou nenhuma conclusão; mas é um defeito real se o serviço for implantado fora de UTC.
- **"Preço com mais de duas casas".** O anexo só define `price_cents` inteiro. Testei `3310.7` e `eta_hours` `41.5`: o serviço trata como "qualquer outra coisa" e devolve `503 CARRIER_UNAVAILABLE`, e aceita `3310.0`. Considerei essa leitura aceitável.
- **`"coupon": null`.** O serviço trata como cupom não informado (200, e o Pix ganha o desconto; `CheckoutRequestParser.java:40`). O enunciado diz "pode não vir; se vier, mesmo vazio…" e não fala de null; aceitei a leitura do agente.
- **Repetir o aviso depois de o pedido mudar.** Depois do envio, a repetição do aviso aceito devolve o pedido como está agora (`SHIPPED`, com rastreio), e não a resposta da primeira vez. Não foi "exatamente a mesma resposta", mas o enunciado também diz "200 e o pedido como está"; aceitei essa leitura.
- **GOLD com FREESHIPPING** em entrega coberta pelo clube: o frete sai 0,00 e o desconto do cupom 0,00. Pareceu coerente com "o desconto do cupom fica igual ao valor do frete".
- **`paidAt` anterior a `createdAt`:** não testei. O código aceita (a duração fica negativa), e o enunciado não cobre esse caso.

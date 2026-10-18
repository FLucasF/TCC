# comportamento

## Visão geral

O serviço compila, sobe em menos de um segundo com o banco vazio, cria as seis tabelas e grava o catálogo. Mas **toda chamada HTTP devolve `500`** com a página de erro do próprio Javalin ("It looks like you don't have an object mapper configured"). O projeto usa `ctx.json(...)` em todas as respostas (`src/main/java/com/loja/api/controllers/ApiController.java:73` e seguintes) sem colocar nenhum conversor de JSON que o Javalin reconheça. O `pom.xml` traz só o Gson, que nunca é registrado no Javalin (`src/main/java/com/loja/Main.java:111`), e o jar não tem nenhuma classe `fasterxml`. Então o site não recebe nenhum resumo, pedido, produto, saldo, aviso nem código de erro. Isso vale também para as recusas: o `status(422)` é trocado por 500 quando o `json()` falha.

Como as gravações acontecem antes da resposta, conferi pelo banco (por JDBC, só lendo) o que o serviço fez de fato. A conta está certa: os sete exemplos com valor foram gravados com os números do financeiro, e as fronteiras testadas conferem. O ciclo do pedido funciona em sequência.

Por baixo do erro de JSON, os problemas mais sérios, em ordem de gravidade:
1. **Nada é protegido entre pedidos simultâneos.** Seis pedidos pagaram as duas últimas unidades. Avisos repetidos em paralelo deram crédito e aviso várias vezes. Cinco `transactionId` diferentes foram todos aceitos.
2. **O prazo da TransLog sai de uma consulta errada**, com CEP `00000000` e 1 kg fixo.
3. **O pagamento entre 48h e 49h ainda é aceito.**
4. **Recusas que não acontecem**: cupom vazio aceito, boleto com total acima de R$ 1.000,00 aceito.

## Item a item

**C1, o serviço sobe.** Compilei uma cópia em `/tmp/run` com `mvn -q package -DskipTests` (18 s) e subi com `PORT=8081 TRANSLOG_URL=… java -jar target/order-service-1.0.0.jar` com o banco vazio. Uma segunda máquina subiu na 8082. O log mostra `Started Server … @590ms`. O serviço criou `products`, `customers`, `orders`, `order_items`, `notifications` e `payment_transactions` e gravou os oito produtos. Na segunda subida (C9) levou 584 ms e não regravou o catálogo. Não testei as duas máquinas subindo juntas num banco vazio (não há como esvaziá-lo de novo). A subida em si segue o pedido; o fato de tudo responder 500 é tratado nos itens seguintes.
Conclusão: segue

**C2, o catálogo.** `GET /products/CAM-001`, `GET /products/REL-001` (nas duas máquinas) e `GET /products/NOPE` devolvem `500` com a página do Javalin. O esperado era `200` com o produto e `404 PRODUCT_NOT_FOUND`. No banco, a tabela `products` tem os oito SKUs com nome, preço, peso e estoque do anexo, e `REL-001` com `discontinued = t`. Os dados estão lá, mas o site não consegue consultá-los.
Conclusão: não segue

**C3, o resumo.** `POST /checkout/summary` e `POST /orders` com os dados do Exemplo 1 devolvem `500`, sem nenhum campo do resumo.

Registrei os exemplos 1 a 7 (`POST /orders`, CPFs 91000000001…7, com a TransLog imitada respondendo o valor de cada exemplo) e li a tabela `orders`. Os sete conferem com o financeiro campo a campo (subtotal, cupom, frete, prazo, seguro, ajuste, total, parcelas, crédito e brinde). Por exemplo, o Ex. 2 deu `409.70 | 0.00 | 15.60 | 7 | 6.15 | 30.55 | 462.00 | 6 | 77.00 | 8.19 | f` e o Ex. 7 deu `1419.60 | 0.00 | 0.00 | 6 | 30.00 | -72.48 | 1377.12 | … | 70.98 | t`.

Os exemplos 8, 9 e 10 não gravaram pedido, ou seja, foram recusados, mas o código que o site recebe é o 500.

Fronteiras, todas lidas no banco:
- motoboy com exatamente 5,0 kg (`MEI-001` × 50): aceito; com 5,1 kg (× 51): recusado;
- MINUS50 com R$ 299,70: recusado; com R$ 319,60: aceito, desconto 50,00. Não existe carrinho de exatamente R$ 300,00 com este catálogo (ver Dúvidas);
- limite de 30%: Ex. 5 (71,91) e Ex. 6 (5,97);
- "meio para o par": seguro de 1% sobre R$ 438,50 deu 4,38 (o arredondamento comum daria 4,39); crédito GOLD sobre R$ 239,70 deu 11,98.

A conta está certa, mas o resumo que o site deveria receber não existe.
Conclusão: não segue

**C4, as recusas.** Todas as recusas chegam como `500`, sem o corpo `{ "error": … }`:
- recusas antes de registrar: SKU inexistente junto com região `MARS`, `BON-001` sem estoque junto com cupom `NOPE`, `PIX` com `installments: 2`, quantidade 0, Exemplo 8, Exemplo 10 (`ZIP_NOT_SERVED`);
- recusas nas operações de um pedido: pedido inexistente (`GET /orders/abc`), envio sem `trackingCode`, operações fora de ordem;
- recusas de cliente: `GET /customers/123`.

Por isso a ordem com dois problemas ao mesmo tempo não pode ser conferida: os dois casos testados só não gravaram pedido. Mesmo deixando o JSON de lado, o banco mostra recusas que não acontecem:
- **cupom vazio** (`"coupon": ""`): o pedido do CPF 93000000001 foi gravado (`AWAITING_PAYMENT`). O esperado era `INVALID_COUPON`. O texto vazio vira "sem cupom" em `ApiController.java:336`;
- **boleto com total acima de R$ 1.000,00**: produtos 979,70 + frete 33,10 + seguro 9,80 + tarifa deram `final_total = 1026.09`, e o pedido foi gravado. O esperado era `PAYMENT_METHOD_UNAVAILABLE`. O serviço compara o subtotal dos produtos, e não o total (`services/ValidationService.java:186`);
- **sem e-mail**: nenhum pedido gravado, mas o cliente 93000000002 foi criado. A recusa não vem da validação; vem da coluna `customer_email NOT NULL`.

Para confirmar o diagnóstico, montei uma terceira cópia com o Jackson adicionado. Ela não conta para a nota. Nela, região `MARS` deu `500 {"error":"INTERNAL_ERROR"}` em vez de `INVALID_REGION`, porque o serviço usa `Region.valueOf` (`ApiController.java:334`).
Conclusão: não segue

**C5, a TransLog.** Escrevi um `Stub.java` com `HttpServer` que registra as consultas recebidas.

O que confere:
- `price_cents` 3310, 1560, 2475, 2210 e 5080 viraram 33,10, 15,60, 24,75, 22,10 e 50,80;
- `eta_hours` 41 deu 2 dias, 48 deu 2 e 30 deu 2; 49 deu 3, mais 3 do Norte, total 6;
- com uma demora de 3 s, a chamada desistiu em 2029 ms e nada foi gravado; com 1,5 s, deu certo;
- resposta `500` e resposta em HTML não gravaram pedido;
- com a TransLog respondendo 500, motoboy e retirada gravaram o pedido (frete 18,00/prazo 0 e 0,00/1).

O que não confere:
- **o prazo vem de outra consulta.** O log do stub mostra `zip=01310100&weight_g=1799&service=EXP` repetida três vezes e depois `zip=00000000&weight_g=1000&service=EXP`. O prazo sai desta última (`domain/strategies/shipping/EconomyShipping.java:32`, `ExpressShipping.java:32`). Fiz a TransLog atender só o CEP 01310100 com `eta_hours: 100` (o certo seria 5 dias). A econômica gravou prazo 7 e a expressa prazo 2, que são valores fixos do código para quando a consulta falha (`EconomyShipping.java:36`, `ExpressShipping.java:36`). Nos exemplos o prazo conferiu só porque o stub respondia igual para qualquer CEP;
- **o peso vai truncado.** 2 × 0,30 + 1,20 kg virou `weight_g=1799` em vez de 1800 (`infrastructure/TransLogClient.java:27`);
- o `CARRIER_UNAVAILABLE 503` e o `SHIPPING_METHOD_UNAVAILABLE` não chegam ao site (500).
Conclusão: em parte

**C6, o ciclo do pedido.** Todas as respostas são `500`. Pelo banco:
- **estoque e crédito**: o registro não baixou o estoque (CAM 50 → 50). O pagamento baixou (→ 48), deu crédito SILVER de 3,20 e gerou `PAYMENT_APPROVED`;
- **ordem das operações**: envio antes do pagamento e entrega antes do envio não mudaram nada; o envio sem `trackingCode` também não. O envio gravou `SHIPPED`/`BR123456789` e gerou `ORDER_SHIPPED`. O cancelamento depois do envio não mudou nada, e a entrega gravou `DELIVERED`;
- **cancelar um pedido pago**: o estoque voltou (TEN 9 → 10), o crédito saiu (12,50 → 0,00) e saiu `ORDER_CANCELLED`. Cancelar de novo, ou pagar o cancelado com outro `transactionId`, não mudou nada;
- **cancelar um não pago**: nada foi devolvido e saiu `ORDER_CANCELLED`;
- **avisos**: `POST /notifications/1/read` marcou `READ` e continuou `READ` na segunda vez;
- **prazo de 48 horas**: com `paidAt` = registro + 48h, o pedido foi `PAID` (certo). Com **+48h30min, também `PAID`**; o esperado era `ORDER_EXPIRED` e `CANCELLED`. Só com +49h01 o pedido foi `CANCELLED`, com aviso. A conta corta as horas para número inteiro (`domain/entities/Order.java:102-103`);
- **WELCOME10**: recusado para o CPF com pedido entregue (Ex. 9) e aceito para o CPF que só tinha pedido cancelado.

Os efeitos conferem, menos a janela das 48h. As respostas, não.
Conclusão: em parte

**C7, o aviso de pagamento repetido.**

*Um depois do outro*: o mesmo `TX-S1` repetido na outra máquina não baixou o estoque de novo (48 → 45 → 45), e o crédito ficou em 11,98 com um `PAYMENT_APPROVED` só. Um `TX-S2` no pedido já pago não mudou nada. A resposta era para ser "exatamente a mesma" da primeira vez, mas é 500 nas duas. Pelo código, a repetição de um aviso recusado devolveria o pedido com 200 em vez da mesma recusa (`services/OrderService.java:65-66`); no banco, a repetição de um expirado não gerou aviso novo.

*Ao mesmo tempo*: cinco `TX-SAME` disparados juntos, divididos entre 8081 e 8082, em três rodadas. Esperado em cada rodada: MEI −2, crédito 0,40, um aviso.

| rodada | estoque MEI | crédito | avisos `PAYMENT_APPROVED` |
|---|---|---|---|
| 1 | 97 → 93 (−4) | 1,60 | 5 |
| 2 | 93 → 91 | 1,60 | 5 |
| 3 | 91 → 89 | 0,80 | 5 |

Cinco `transactionId` diferentes juntos (duas rodadas): os cinco foram aceitos (`payment_transactions` tem TX-D1…D5), com 5 avisos e crédito 0,80 em cada rodada. O esperado era um aceito e quatro `409 OPERATION_NOT_ALLOWED`. A causa é que o serviço consulta e só depois grava, sem trava nem transação (`OrderService.java:65`, `:84`, `:88`).
Conclusão: não segue

**C8, a última peça.** Pagamentos disparados juntos, alternando as duas máquinas:

| produto | pedidos | estoque | resultado | avisos |
|---|---|---|---|---|
| MOC-001 | 2 | 1 → 0 | 2 pagos | 2 `PAYMENT_APPROVED` |
| FON-001 | 6 | 2 → 1 | 6 pagos | 6 `PAYMENT_APPROVED` |
| JAQ-001 | 6 | 2 → 1 | 6 pagos | 6 `PAYMENT_APPROVED` |
| TEN-001 | 6 | 2 → 1 | 6 pagos | 6 `PAYMENT_APPROVED` |

Vendeu mais do que havia em todas as rodadas. O estoque só não ficou negativo porque as baixas se sobrescreveram (`domain/observers/StockObserver.java:22-30` lê, confere e grava `UPDATE products SET stock = ?`, `repositories/impl/ProductRepositoryImpl.java:48`). Ninguém recebeu `INSUFFICIENT_STOCK` nem `ORDER_CANCELLED`.

Em sequência o caminho existe: dois pedidos para a última `JAQ-001`. O primeiro ficou `PAID`, o segundo `CANCELLED`, com `ORDER_CANCELLED` e sem crédito; repetir esse aviso não gerou aviso novo. A resposta foi 500 em vez de `409 INSUFFICIENT_STOCK`.
Conclusão: não segue

**C9, os dados não se perdem.** Antes de derrubar: 60 pedidos, 61 avisos, estoques `CAM=45 FON=1 JAQ=0 MEI=85 MOC=0 TEN=1` e saldo 11,98 do CPF 96000000001. Derrubei as duas máquinas e subi de novo: os mesmos números, e o pedido entregue continua `DELIVERED`/`BR123456789`. O `GET /orders/{id}` depois da subida continua 500 (C2/C6).
Conclusão: segue

**C10, os testes do próprio agente.** `mvn verify` na cópia: `Tests run: 2, Failures: 0, Errors: 0`, `BUILD SUCCESS`. Os dois testes estão em `MoneyTest`.
Conclusão: segue

## O que mudaria primeiro

1. **Configurar o JSON do Javalin**: pôr o `jackson-databind` no `pom.xml` ou registrar o Gson como `jsonMapper`. Sem isso o site não recebe nenhum resultado. Numa cópia à parte, só com o Jackson adicionado, `GET /products/REL-001` passou a dar `200` e `GET /products/NOPE` a dar `404 PRODUCT_NOT_FOUND`. Junto com isso, tratar valores de enumeração inválidos e cupom vazio como os códigos da tabela, em vez de `INTERNAL_ERROR`.
2. **Fazer o pagamento numa transação só do banco, com trava.** Por exemplo: `SELECT … FOR UPDATE` do pedido, `UPDATE products SET stock = stock - ? WHERE sku = ? AND stock >= ?`, e o registro do `transactionId` antes dos efeitos, tudo junto. É isso que resolve C7 e C8, os dois pontos que o dono disse que "não pode acontecer". Depois, tirar o prazo da mesma consulta do frete (CEP e peso reais) e corrigir a janela das 48h.

## Dúvidas

- **Como julgar quando tudo responde 500 mas o banco grava certo.** Nos itens em que o objeto é a resposta (C2, C3, C4), contei como `não segue`. Nos itens em que o objeto é o efeito (C5, C6, C9), julguei pelo banco e registrei a falha da resposta como uma parte que não confere. No C1 considerei só a subida.
- **GOLD com FREESHIPPING na expressa** (CPF 92000000007): frete 0,00, mas cupom de 23,97 (30% de 79,90, pois o frete bruto era 33,10), total 56,73, abaixo dos produtos mais o seguro. O enunciado diz que o desconto "fica igual ao valor do frete" e que, no GOLD, "o frete sai zerado". Leio que o desconto deveria ser 0,00, mas o texto não decide o caso de forma explícita, então não mudei a conclusão do C3.
- **MINUS50 no limite exato.** Todos os preços terminam em ,90, e nenhuma combinação dá exatamente R$ 300,00. Usei 299,70 (recusado) e 319,60 (aceito).
- **TransLog com `price_cents: 3310.7`.** Virou 33,10, cortando a fração. O anexo não diz se isso é "qualquer outra coisa" (TransLog fora do ar) ou um valor a arredondar.
- **Registro com 48h exatas.** O `createdAt` tem frações de segundo e o `paidAt` não. Usei registro + 48h, cortado para o segundo, como "exatamente 48h".
- **Cópia com o Jackson.** Usei-a só para confirmar a causa dos 500 e o `INTERNAL_ERROR` da região inválida; nenhuma conclusão se apoia nela. A pasta de trabalho não foi alterada, e o serviço, a segunda máquina e o stub foram derrubados no fim.

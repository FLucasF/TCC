# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Ele sobe com o banco vazio em menos de 4 segundos e cria as tabelas e o catálogo por uma migração Flyway (`src/main/resources/db/migration/V1__schema.sql:57`). Os dez exemplos do financeiro dão os mesmos valores, campo a campo, e as fronteiras também conferem: o motoboy com 5,00 kg, o teto de 30% dos cupons e o arredondamento "meio para o par". A tabela "Quando dá erro" segue a ordem do enunciado, inclusive quando o pedido tem dois problemas.

Com duas máquinas no mesmo banco, nem o aviso de pagamento repetido nem a disputa pela última peça quebraram em nenhuma rodada. O pedido fica travado com `SELECT … FOR UPDATE` (`src/main/java/br/com/loja/order/OrderRepository.java:58`). O estoque é travado em ordem de SKU antes de baixar (`src/main/java/br/com/loja/catalog/ProductRepository.java:43`). O desfecho do primeiro aviso fica gravado no pedido (`payment_transaction_id`, `payment_outcome`), e o aviso repetido devolve esse desfecho (`src/main/java/br/com/loja/order/OrderService.java:87`).

Os dados sobrevivem ao reinício, e o `mvn verify` passa. Não encontrei um problema sério. Os pontos que dependem de leitura (um preço da TransLog com centavos fracionados, um aviso repetido com outro `paidAt`) estão em "Dúvidas".

## Item a item

**C1, o serviço sobe.** Compilei uma cópia em `/tmp/run/app` com `mvn -q package -DskipTests` e subi `java -jar pedidos-1.0.0.jar --server.port=8081` com o banco vazio. O log da primeira subida mostra `Schema history table … does not exist yet`, depois `Migrating schema "public" to version "1 - schema"` e `Started LojaApplication in 2.746 seconds`. A segunda máquina, na 8082, subiu com `Schema "public" is up to date. No migration necessary` em 2,55 s. Depois de um reinício, a subida levou 3,57 s.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Os oito vieram com os valores do anexo, por exemplo `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`. O `REL-001` veio com `"discontinued":true` e estoque 8, e o `BON-001` com estoque 0. Um SKU que não existe, `XXX-999`, deu `404 {"error":"PRODUCT_NOT_FOUND"}`. Pedir o `REL-001` no resumo deu `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Mandei os exemplos para o `POST /checkout/summary`, com o stub da TransLog dando a resposta de cada um.
- Exemplos 1 a 7: todos os campos conferem. Por exemplo, o 2 deu `subtotal 409.70, desconto 0.00, frete 15.60, prazo 7, seguro 6.15, ajuste 30.55, total 462.00, 6× de 77.00, crédito 8.19, brinde false`. O 7 deu `1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / 70.98 / gift true`.
- Exemplo 8: deu `422 COUPON_NOT_APPLICABLE`.
- Exemplo 9: registrei e paguei um pedido para o CPF `22200000009`, e o mesmo carrinho com `WELCOME10` deu `422 COUPON_NOT_APPLICABLE`. Com um CPF novo, o mesmo carrinho passa, com desconto de 15.98.
- Exemplo 10: deu `422 SHIPPING_METHOD_UNAVAILABLE`.

O resumo e o pedido registrado batem: os dados do exemplo 2 deram o mesmo `summary` no `POST /orders` e no `GET` pela outra máquina (`diff` vazio).

As fronteiras:
- **O motoboy:** com `MEI-001`×50 (5,00 kg) o frete é 18.00. Com ×51 (5,10 kg) vem `422 SHIPPING_METHOD_UNAVAILABLE`.
- **O MINUS50:** com 299.10 em produtos vem `COUPON_NOT_APPLICABLE`, e com 319.70 o desconto é de 50.00. Um subtotal de exatamente 300,00 não sai com este catálogo; o código usa `compareTo(MINIMUM) < 0`, que aceita os 300,00.
- **O teto de 30%:** no exemplo 5, o BUY3PAY2 daria 79.90 e ficou em 71.91. No exemplo 6, o FREESHIPPING daria 22.10 e ficou em 5.97.
- **"Meio para o par":** o crédito GOLD sobre 179.70 dá 8,985, e veio 8.98. Com arredondamento "meio para cima" seria 8.99.

Também conferi o cartão em 12× sobre 80.70 (parcela 7.63, total 91.56) e o Nordeste com 48 horas da TransLog (2 + 2 = 4 dias).
Conclusão: segue

**C4, as recusas.** Testei mais de 40 casos no resumo:
- **`INVALID_ORDER`:** o carrinho vazio ou ausente, o item sem SKU, o SKU repetido, a quantidade 0, −1 ou ausente, sem CPF, sem e-mail, sem `customer` e sem corpo.
- **Produto:** `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED` e `INSUFFICIENT_STOCK` (`BON-001`×1 e `MOC-001`×2).
- **Nível, região, CEP e entrega:** `INVALID_CLUB_TIER` (inexistente, ausente, `gold` em minúsculas), `INVALID_REGION` e `INVALID_ZIP_CODE` (7 dígitos, com traço, ausente). Também `INVALID_SHIPPING_METHOD`.
- **Cupom:** `INVALID_COUPON` para o inexistente, o `""` e o `welcome10`. O `"coupon": null` é tratado como "não veio".
- **Pagamento:** `INVALID_PAYMENT_METHOD`, `INVALID_INSTALLMENTS` (Pix 2×, boleto 2×, cartão 13× e 0×) e `PAYMENT_METHOD_UNAVAILABLE` (boleto com total acima de R$ 1.000).

Todos vieram com o status pedido (422, e 503 no caso da TransLog fora do ar).

Pedidos com dois problemas ao mesmo tempo, todos seguindo a ordem do enunciado:

| os dois problemas | o que voltou |
|---|---|
| SKU repetido + SKU desconhecido | `INVALID_ORDER` |
| desconhecido + fora de linha | `UNKNOWN_PRODUCT` |
| sem estoque + fora de linha | `PRODUCT_DISCONTINUED` |
| região inválida + nível inválido | `INVALID_CLUB_TIER` |
| CEP inválido + entrega inválida | `INVALID_ZIP_CODE` |
| motoboy acima de 5 kg + cupom inválido | `SHIPPING_METHOD_UNAVAILABLE` |
| cupom inválido + pagamento inválido | `INVALID_COUPON` |
| cupom não aplicável + pagamento inválido | `COUPON_NOT_APPLICABLE` |
| pagamento inválido + parcelas inválidas | `INVALID_PAYMENT_METHOD` |
| parcelas inválidas + boleto acima de R$ 1.000 | `INVALID_INSTALLMENTS` |

No `POST /orders`, as mesmas recusas se repetem, e um pedido recusado não cria o cliente: `GET /customers/44400000002` dá `404`.
Conclusão: segue

**C5, a TransLog.** O log do stub mostra as chamadas certas: `zip=01310100&weight_g=1800&service=EXP` no exemplo 1 e `service=STD` na econômica.

A tradução: 3310 centavos viram 33.10, 1 centavo vira 0.01 e 12345 centavos viram 123.45. Para o prazo, 24 h dão 1 dia, 25 h dão 2, 41 h dão 2 e 49 h dão 3; o código arredonda para cima com `(hours + 23) / 24` (`src/main/java/br/com/loja/shipping/TransLogGateway.java:63`). `{"error":"ZIP_NOT_SERVED"}` dá `422 SHIPPING_METHOD_UNAVAILABLE`.

Casos que viram `503 CARRIER_UNAVAILABLE`:
- HTTP 500, ou HTTP 404 mesmo com o corpo `ZIP_NOT_SERVED`;
- um corpo que não é JSON, ou `{}`;
- outro erro, `{"error":"OTHER"}`;
- `price_cents` como texto, `price_cents` 3310.5 ou 3310.456, `eta_hours` 40.5.

O tempo limite: com 1,7 s de demora o resumo sai normal, em 1,75 s. Com 2,3 s ou 5 s de demora vem `503` em 2,006 s, cortado pelo `timeout(TIMEOUT)` de 2 s (`TransLogGateway.java:36`).

Com o stub derrubado, a econômica e a expressa dão `503`. A retirada (`200`, frete 0.00, prazo 1) e o motoboy (`200`, frete 18.00, prazo 0) funcionam, e um `POST /orders` com motoboy registrou o pedido (`201`).
Conclusão: segue

**C6, o ciclo do pedido.**
- **O caminho todo:** registrei 2×`CAM-001` e o estoque continuou em 50. Paguei: o estoque foi para 48, o saldo para 3.20 (2% de 159.80) e saiu `PAYMENT_APPROVED`. O envio sem `trackingCode`, ou com ele vazio, deu `422 INVALID_ORDER`. O envio com `BR123456789` deu `SHIPPED` e o aviso `ORDER_SHIPPED`. A entrega deu `DELIVERED`.
- **Fora de ordem:** enviar ou entregar um pedido não pago, entregar um pedido pago, enviar de novo, cancelar depois de enviado ou entregue, entregar de novo e pagar com outro `transactionId` deram todos `409 OPERATION_NOT_ALLOWED`.
- **Cancelar um pedido pago (GOLD, 3×`CAM-001`):** o estoque foi de 48 para 45 no pagamento e voltou a 48 no cancelamento. O saldo foi de 11.98 a 0.00, e os avisos ficaram `PAYMENT_APPROVED` e `ORDER_CANCELLED`.
- **Cancelar um pedido não pago:** o estoque continuou em 48, o saldo em 0.00, e saiu `ORDER_CANCELLED`. Pagar depois deu `409 OPERATION_NOT_ALLOWED`. O `WELCOME10` volta a valer para quem só tem pedidos cancelados e não vale para quem tem um pedido entregue.
- **O prazo de 48 horas:** com `paidAt` igual a `createdAt` + 48 h o pagamento passou (`PAID`, estoque −1). Com + 48 h e 1 s veio `409 ORDER_EXPIRED`, o pedido ficou `CANCELLED` com o aviso `ORDER_CANCELLED`, e o estoque não mexeu.
- **Pedido que não existe:** os cinco endpoints deram `404 ORDER_NOT_FOUND`, inclusive o pagamento com o corpo vazio, o que confirma que essa conferência vem antes da do campo obrigatório. Com o pedido existindo, o pagamento sem `transactionId`, sem `paidAt` ou sem corpo deu `422 INVALID_ORDER`.
- **Os avisos:** saem do mais antigo para o mais novo. `POST /notifications/7/read` deu `READ`, e repetido (inclusive pela 8082) continua `READ`. Os ids `999999` e `abc` deram `404 NOTIFICATION_NOT_FOUND`. Um CPF que nunca comprou deu `404 CUSTOMER_NOT_FOUND` tanto no saldo quanto nos avisos.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Um depois do outro:** mandei o mesmo aviso `TX-E1` três vezes (8081, 8082, 8081). As três respostas foram idênticas byte a byte (`cmp`), todas `200`. O estoque baixou só 2 (de 99 para 97), o crédito entrou uma vez (0.80) e saiu um `PAYMENT_APPROVED`. Outro `transactionId` deu `409 OPERATION_NOT_ALLOWED`. Depois do envio, o `TX-E1` repetido devolve `200` com o pedido como está (`SHIPPED`). A repetição de uma recusa devolve a mesma recusa: `ORDER_EXPIRED` nas duas máquinas e `INSUFFICIENT_STOCK` mesmo depois de o estoque voltar.
- **Ao mesmo tempo:** em 5 rodadas, disparei 5 avisos iguais juntos, divididos entre a 8081 e a 8082. Em todas, o estoque baixou exatamente 3, o crédito ficou em 2.98, saiu um só `PAYMENT_APPROVED` e as cinco respostas foram `200` com o mesmo corpo (1 corpo distinto). Em outras 5 rodadas, 5 avisos com `transactionId` diferentes no mesmo pedido: em todas, 1 `200 PAID` e 4 `409 OPERATION_NOT_ALLOWED`, com baixa de estoque, crédito e aviso uma vez só.
Conclusão: segue

**C8, a última peça.** Com as duas máquinas, disparei os pagamentos juntos, alternando a máquina:
- **A última unidade:** `MOC-001` com estoque 1 e dois pedidos, em 5 rodadas.
- **Seis pedidos para duas unidades:** `JAQ-001` com estoque 2, em 5 rodadas.
- **Oito pedidos para uma unidade:** `MOC-001`, em 3 rodadas.
- **Pedidos com dois itens em ordens trocadas:** `JAQ+FON` e `FON+JAQ`, quatro pedidos para 2 jaquetas, em 3 rodadas.

Em todas as rodadas passaram tantos pagamentos quantas unidades havia, e o estoque terminou em 0, nunca negativo. Todos os perdedores receberam `409 INSUFFICIENT_STOCK`, ficaram `CANCELLED` e têm o aviso `ORDER_CANCELLED` (por exemplo, 4/4 e 7/7). Nos pedidos com dois itens, o `FON-001` baixou só para os ganhadores (de 5 para 3). Não houve deadlock nem exceção nos logs das duas máquinas (`grep -ci 'exception|deadlock|ERROR'` deu 0). Entre as rodadas, cancelei os pedidos ganhadores para devolver o estoque; isso confirma de novo que o cancelamento devolve as peças.
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar as duas máquinas, guardei os estoques (`CAM-001=46 MEI-001=72 MOC-001=1 JAQ-001=2 FON-001=5 TEN-001=10`), um pedido entregue e a lista de avisos de um cliente. Depois do reinício:
- os estoques são os mesmos, e o catálogo não foi inserido de novo;
- o `GET` do pedido e a lista de avisos são idênticos aos de antes (`cmp`), inclusive o aviso já `READ`;
- o saldo continua 3.20;
- o `WELCOME10` continua recusado para quem já comprou.
Conclusão: segue

**C10, os testes do próprio agente.** Numa cópia limpa, sem as variáveis do banco, rodei `mvn -q verify`, que terminou com saída 0. Rodam 9 testes: `LojaIntegrationTest` com 5 e `TransLogGatewayTest` com 4, sem falhas, erros nem testes pulados. Os testes usam H2, de escopo de teste no `pom.xml`.
Conclusão: segue

## O que mudaria primeiro

Nada do que testei falha, então não há uma correção de comportamento a fazer. Se fosse para mexer em algo, eu deixaria explícito o que o serviço faz quando a TransLog manda um preço com centavos fracionados: hoje isso vira `503 CARRIER_UNAVAILABLE`. Se o negócio preferir arredondar esse valor (meio para o par, como o resto do dinheiro), a mudança é pequena e fica no `TransLogGateway.java:58`. A segunda seria o aviso repetido com o mesmo `transactionId` e outro `paidAt` (veja "Dúvidas"), caso o banco possa mandar isso.

## Dúvidas

- **Preço com mais de duas casas.** O anexo diz que `price_cents` é o preço em centavos, mas não diz o que fazer se ele vier fracionado (3310.5). O serviço considera isso "qualquer outra coisa" e responde `503 CARRIER_UNAVAILABLE`. Isso está dentro do texto ("Se ela responder qualquer outra coisa … considere a TransLog fora do ar"), mas arredondar também seria uma leitura possível. Não mudou a conclusão do C5. O mesmo vale para `eta_hours` fracionado (40.5) e para `price_cents` como texto.
- **O aviso repetido com outro `paidAt`.** O enunciado define o aviso repetido como "mesmo pedido e mesmo `transactionId`". O serviço compara só o `transactionId`: um repetido com `paidAt` de 2030 devolveu `200` e o pedido pago, sem refazer nada. Segui essa definição.
- **O pagamento com `paidAt` malformado** (`"ontem"`) dá `422 INVALID_ORDER`. O enunciado só fala de campo ausente. Um `paidAt` com fuso (`-03:00`) é aceito.
- **Um pedido cancelado e enviado sem `trackingCode`** dá `422 INVALID_ORDER` antes de `409 OPERATION_NOT_ALLOWED`. É a ordem da tabela das operações (campo obrigatório antes da situação), então está certo.
- **Coisas que não deu para testar com este catálogo.** Um subtotal de exatamente R$ 300,00 não sai com estes preços; testei 299,10 e 319,70 e li a comparação no código. Também não testei um pedido que "envelhece" sem nenhum aviso: não há como avançar o relógio do serviço, mas o código só muda a situação nas operações (`OrderService.java:92`).
- **TransLog com `{"price_cents":0,"eta_hours":0}`** é aceita como frete 0.00 e prazo 0. O enunciado não trata esse caso.

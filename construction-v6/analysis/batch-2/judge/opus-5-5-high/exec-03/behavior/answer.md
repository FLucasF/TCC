# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Sobe sozinho com o banco vazio em menos de um segundo, cria as tabelas e coloca o catálogo. Os dez exemplos do financeiro dão os mesmos valores, campo a campo. Todas as recusas da tabela "Quando dá erro" devolvem o status e o código certos, e com dois problemas ao mesmo tempo vale a ordem do enunciado. A TransLog é traduzida corretamente: centavos para reais, horas para dias arredondando para cima, dias a mais no Norte e no Nordeste, CEP não atendido, tempo limite de 2 s e site fora do ar. O motoboy e a retirada continuam funcionando com a TransLog fora do ar.

Nas partes mais difíceis também não houve falha. Testei com duas máquinas e o mesmo banco:
- **Aviso de pagamento repetido:** cinco avisos iguais ao mesmo tempo devolveram cinco respostas idênticas, e o estoque, o crédito e o aviso `PAYMENT_APPROVED` saíram uma vez só. Com cinco `transactionId` diferentes, passou um e os outros quatro receberam `409 OPERATION_NOT_ALLOWED`.
- **Última peça:** em 19 rodadas de disputa pelo estoque, só passaram tantos pagamentos quantas unidades havia. O estoque nunca ficou negativo, e quem perdeu recebeu `409 INSUFFICIENT_STOCK`, teve o pedido `CANCELLED` e recebeu o aviso `ORDER_CANCELLED`.

Os dados sobrevivem ao reinício, e o `mvn verify` passa com 16 testes. Não achei nenhum problema de comportamento. O único ponto discutível é que, quando a TransLog manda `price_cents` com casas decimais, o serviço trata isso como TransLog fora do ar. O enunciado não decide esse caso (está em "Dúvidas").

## Item a item

**C1, o serviço sobe.** Compilei uma cópia em `/tmp/run/ws` com `mvn -q package -DskipTests` (14 s), que gerou `target/orders.jar`. Subi com `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT=8081` e `TRANSLOG_URL=http://localhost:9090`, e uma segunda máquina na 8082. Com o banco vazio, o log mostra `Started Server ... @696ms` e o serviço já respondia `GET /products/CAM-001`. No reinício, com os dados já lá, subiu em 765 ms. As tabelas e os índices são criados ao subir, sob um advisory lock, para as duas máquinas não disputarem (`src/main/java/shop/persistence/postgres/PostgresStore.java:61-75`). O catálogo entra com `ON CONFLICT DO NOTHING` (`PgProducts.java:182-183`), então não é reposto num reinício. Nenhuma exceção nos quatro logs.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs do anexo. Os oito voltaram `200` com nome, preço, peso, estoque e `discontinued` iguais ao anexo. Exemplos: `MOC-001` com 159.90, 0.90 kg e estoque 1; `BON-001` com estoque 0; `REL-001` com `"discontinued":true`. `GET /products/XXX` deu `404 {"error":"PRODUCT_NOT_FOUND"}`. O produto fora de linha também é recusado na compra: `REL-001` no carrinho dá `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Os dez exemplos foram feitos com `POST /checkout/summary`, com a TransLog falsa devolvendo o que cada exemplo diz:
- **Exemplos 1 a 7:** os onze campos batem com o enunciado. Por exemplo, no exemplo 2 veio `adjustment 30.55`, `finalTotal 462.00` e `6 × 77.00`, com crédito `8.19`. No exemplo 7 veio `insurance 30.00`, `paymentAdjustment -72.48`, `finalTotal 1377.12` e `gift true`.
- **Exemplos 8 e 10:** `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Exemplo 9:** registrei e paguei um pedido do mesmo CPF antes. Com `WELCOME10` deu `422 COUPON_NOT_APPLICABLE`; sem cupom, o mesmo carrinho dá `200`.

As fronteiras que testei:
- **Motoboy:** `MEI-001` × 50 (5,00 kg) dá `200` com frete 18.00. × 51 (5,10 kg) dá `422 SHIPPING_METHOD_UNAVAILABLE`.
- **MINUS50:** nenhum carrinho possível dá exatamente 300,00 (todos os preços terminam em ,90). Com 299,70 dá `422 COUPON_NOT_APPLICABLE`; com 318,40 dá desconto de 50.00.
- **Limite de 30%:** com `FREESHIPPING` em `MEI-001` × 1 (subtotal 19,90, 30% = 5,97), um frete de 5,97 dá desconto 5.97 e um frete de 5,98 também dá 5.97 (limitado).
- **Meio para o par:** `MEI-001` × 15 no Sul (298,50) dá seguro `2.98` (2,985 vai para 2,98). Um carrinho de 299,50 dá seguro `3.00` (2,995 vai para 3,00). São os dois exemplos do texto.
- **Outros pontos:** cartão em 12× (`23.85`, total 286.20) e em 4× (`66.27`) conferidos pela fórmula Price; `installments` ausente vale 1. O GOLD no motoboy paga 18.00; o GOLD com 499,80 de produtos não leva brinde. `BUY3PAY2` sem 3 unidades dá desconto 0 e tira o desconto do Pix. `WELCOME10` volta a valer para quem só tem um pedido cancelado ou aguardando pagamento.
- **Resumo e pedido:** o resumo e o `summary` do pedido registrado (`POST /orders`, exemplo 1) são idênticos.
Conclusão: segue

**C4, as recusas.** Testei as 15 linhas da tabela, cada uma com uma ou mais variantes, e todas deram o código e o status pedidos:
- **Linha 1 (`INVALID_ORDER`):** carrinho vazio ou ausente, item sem SKU, SKU repetido, quantidade 0, −1 ou ausente, CPF, e-mail ou `customer` ausentes, corpo que não é JSON ou vazio.
- **Linhas 2 a 4:** `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, e `INSUFFICIENT_STOCK` com `BON-001` × 1 e `MOC-001` × 2.
- **Linhas 5 a 8 (nível, região, CEP, entrega):** inexistentes e ausentes; no CEP, `"01310-100"` e 7 dígitos.
- **Linhas 9 e 10:** `SHIPPING_METHOD_UNAVAILABLE` e `503 CARRIER_UNAVAILABLE`.
- **Linhas 11 e 12:** `INVALID_COUPON` para `"BLACKFRIDAY"`, `""` e `"welcome10"`; `COUPON_NOT_APPLICABLE`.
- **Linha 13:** `INVALID_PAYMENT_METHOD`, inexistente e ausente.
- **Linha 14 (`INVALID_INSTALLMENTS`):** Pix 2×, boleto 2×, cartão 0× e 13×.
- **Linha 15:** boleto com total de 1.181,40 dá `PAYMENT_METHOD_UNAVAILABLE`; no cartão, o mesmo carrinho passa.

Em todas, o status foi 422, exceto o `CARRIER_UNAVAILABLE`, que foi 503.

Com dois problemas, sempre ganhou o de ordem menor:
- SKU repetido e inexistente: `INVALID_ORDER`
- fora de linha e inexistente: `UNKNOWN_PRODUCT`
- sem estoque e fora de linha: `PRODUCT_DISCONTINUED`
- sem estoque e nível errado: `INSUFFICIENT_STOCK`
- nível e região errados: `INVALID_CLUB_TIER`
- região e CEP errados: `INVALID_REGION`
- CEP e entrega errados: `INVALID_ZIP_CODE`
- entrega e cupom errados: `INVALID_SHIPPING_METHOD`
- motoboy acima de 5 kg e cupom errado: `SHIPPING_METHOD_UNAVAILABLE`
- TransLog fora do ar e cupom errado: `CARRIER_UNAVAILABLE`
- cupom e pagamento errados: `INVALID_COUPON`
- cupom não aplicável e pagamento errado: `COUPON_NOT_APPLICABLE`
- pagamento e parcelas errados: `INVALID_PAYMENT_METHOD`
- parcelas erradas e boleto acima de 1.000: `INVALID_INSTALLMENTS`

O `POST /orders` dá as mesmas recusas (testei quatro delas).

Nas operações de um pedido também vale a ordem: um pedido inexistente com o corpo vazio dá `404 ORDER_NOT_FOUND`, e não 422. O CPF que nunca comprou dá `404 CUSTOMER_NOT_FOUND`, tanto em `/customers/{cpf}` quanto nos avisos, e um aviso inexistente dá `404 NOTIFICATION_NOT_FOUND`.
Conclusão: segue

**C5, a TransLog.** O stub (`/tmp/run/stub/Stub.java`) registrou as chamadas: `GET /v2/quote?zip=01310100&weight_g=1800&service=EXP` no exemplo 1, `service=STD` na econômica, `weight_g=250` para um fone e `weight_g=550` para fone + 3 meias. A tradução confere:
- **Preço:** `price_cents` 1234 vira 12.34, e 5 vira 0.05.
- **Prazo:** `eta_hours` 24 dá 1 dia, 25 dá 2 dias e 0 dá 0. Com 25 h, o Norte dá 5 dias (+3), o Nordeste dá 4 (+2) e o Centro-Oeste dá 2. A retirada no Norte continua com 1 dia, e o motoboy no Nordeste com 0.
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` dá `422 SHIPPING_METHOD_UNAVAILABLE` na econômica e na expressa.
- **Tempo limite:** uma resposta em 1,7 s é aceita (`200`, em 1.707 s). Com 2,5 s ou 10 s de demora, o serviço corta em 2.006 s e devolve `503 CARRIER_UNAVAILABLE`, inclusive no `POST /orders`.
- **Respostas estranhas:** HTTP 500, HTML, corpo vazio, outro `error`, preço em texto ou `eta_hours` ausente dão `503 CARRIER_UNAVAILABLE`.
- **TransLog derrubada (conexão recusada):** ECONOMY e EXPRESS dão `503`. `STORE_PICKUP` no resumo e `COURIER` no `POST /orders` (`201`) continuam funcionando.
Conclusão: segue

**C6, o ciclo do pedido.**
- **Registro:** registrar `CAM-001` × 2 deixou o estoque em 50, sem baixar. O cliente passou a existir com `creditBalance 0.00`.
- **Pagamento:** o estoque foi de 50 para 48, o crédito entrou (`3.20`, os 2% do SILVER) e saiu o aviso `PAYMENT_APPROVED`.
- **Envio e entrega:** o envio sem `trackingCode` dá `422 INVALID_ORDER`; com ele, fica `SHIPPED` com `"trackingCode":"BR123456789"` e sai o aviso `ORDER_SHIPPED`. A entrega deixa `DELIVERED`.
- **Operações fora de ordem (todas `409 OPERATION_NOT_ALLOWED`):** enviar ou entregar um pedido não pago, entregar um pedido pago que não foi enviado, enviar duas vezes, cancelar um pedido enviado, entregar duas vezes, pagar um pedido entregue ou cancelado com outro `transactionId`, cancelar duas vezes.
- **Pagamento sem campos:** sem `transactionId`, sem `paidAt` ou com `paidAt` inválido dá `422 INVALID_ORDER`.
- **Cancelar um pedido não pago:** fica `CANCELLED`, o estoque continua em 48, o crédito em 0.00, e sai o aviso `ORDER_CANCELLED`.
- **Cancelar um pedido pago:** num pedido GOLD com crédito de 11.98, o estoque foi de 48 para 45 no pagamento e voltou a 48 no cancelamento; o saldo foi de 11.98 para 0.00; saiu o aviso `ORDER_CANCELLED`.
- **Prazo de 48 horas:** com `paidAt = createdAt + 172800 s` (exatamente 48 h), o pagamento é aceito e o estoque baixa. Com `+172801 s`, dá `409 ORDER_EXPIRED`, o pedido fica `CANCELLED`, o estoque não muda e sai o aviso `ORDER_CANCELLED`.
- **Avisos:** a lista vem do mais antigo para o mais novo. `POST /notifications/{id}/read` devolve o aviso como `READ`, e repetir o mesmo pedido na outra máquina mantém `READ`.
- **Primeira compra:** `WELCOME10` é recusado para um CPF cujo único pedido foi entregue.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Um depois do outro:** o mesmo `{"transactionId":"TX-R1",...}`, enviado três vezes (duas na 8081, uma na 8082), devolveu três respostas idênticas byte a byte (`200`, pedido `PAID`). O estoque baixou uma vez (48 para 46), o crédito ficou em 7.99 e saiu um único `PAYMENT_APPROVED`. Outro `transactionId` no pedido já pago deu `409 OPERATION_NOT_ALLOWED`. Depois do envio, repetir `TX-R1` devolve `200` com o pedido como está (`SHIPPED`). Isso também vale depois do reinício.
- **Repetição de uma recusa:** o aviso que expirou, repetido, devolve de novo `409 ORDER_EXPIRED`. O que perdeu por falta de estoque, repetido mesmo depois de o estoque voltar, devolve de novo `409 INSUFFICIENT_STOCK` e não paga o pedido.
- **Ao mesmo tempo, mesmo aviso:** fiz seis rodadas com cinco avisos iguais simultâneos, divididos entre 8081 e 8082 (`/tmp/run/race_same.sh`). Em todas, as cinco respostas foram idênticas e `200`, o estoque baixou uma vez só (por exemplo, 97 para 94 com 3 unidades), o crédito entrou uma vez (1.19) e saiu um único `PAYMENT_APPROVED`. Cinco repetições simultâneas de um aviso expirado deram cinco `409 ORDER_EXPIRED`, com um único `ORDER_CANCELLED` e o estoque sem mudar.
- **Ao mesmo tempo, avisos diferentes:** seis rodadas com cinco `transactionId` diferentes no mesmo pedido (`race_diff.sh`). Em todas, um `200` e quatro `409 OPERATION_NOT_ALLOWED`, o estoque baixou uma vez, o crédito entrou uma vez (1.99) e saiu um aviso. O motivo está no código: `SELECT ... FOR UPDATE` no pedido antes de consultar `payments` (`OrderService.java:36`), com a chave `(order_id, transaction_id)`.
Conclusão: segue

**C8, a última peça.** Usei `/tmp/run/race_stock.sh`. Os pagamentos foram disparados juntos e alternados entre 8081 e 8082. Depois de cada rodada, cancelei os pedidos pagos para devolver o estoque e repetir.
- **Dois pedidos para a última `MOC-001`, oito rodadas:** em todas, o estoque foi de 1 para 0, um pedido ficou `PAID` com `PAYMENT_APPROVED`, e o outro recebeu `409 {"error":"INSUFFICIENT_STOCK"}`, ficou `CANCELLED` e recebeu `ORDER_CANCELLED`.
- **Seis pedidos para duas unidades de `FON-001`, seis rodadas:** antes, baixei o estoque para 2 com um pedido pago. Em todas as rodadas, o estoque foi de 2 para 0, com 2 pagos e 4 recusados com `INSUFFICIENT_STOCK`, estados `CANCELLED` e 6/6 avisos certos.
- **Seis pedidos com dois produtos, cinco rodadas:** cada pedido tinha `FON-001` + `JAQ-001`, metade com os itens na ordem inversa, para provocar deadlock. O resultado foi o mesmo das rodadas anteriores (2 pagos, 4 recusados), sem nenhum deadlock ou erro 500 nos logs.
- **Estoque nunca negativo:** em nenhuma rodada o estoque ficou negativo. O código trava as linhas dos produtos em ordem de SKU com `FOR UPDATE` (`PgProducts.java:63-64`), e a coluna ainda tem `CHECK (stock >= 0)` (`PostgresStore.java:22`).
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar as duas máquinas, tirei uma foto do estado: o estoque dos seis produtos usados, o pedido `4e341286…` (`DELIVERED`, com rastreio, total 161.40), o pedido `f1b32293…` (`SHIPPED`), o saldo 7.99 de `60000000001` e os avisos de `50000000003` (um `READ` e um `NEW`). Subi de novo as duas e tirei outra foto. `diff before.txt after.txt` não mostrou diferença, e o catálogo não foi reposto (o `CAM-001` continuou com 46, não voltou a 50). O registro de idempotência também sobreviveu: repetir `TX-R1` depois do reinício ainda devolve `200`, sem baixar o estoque.
Conclusão: segue

**C10, os testes do próprio agente.** Rodei `mvn -q verify` numa segunda cópia (`/tmp/run/ws2`), sem as variáveis do banco. Terminou com `exit=0`. O surefire relata `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0` em `shop.CheckoutExamplesTest`.
Conclusão: segue

## O que mudaria primeiro

Não achei nada no comportamento que precise de correção. Se fosse para mexer em algo, seria em decidir com o dono da loja o que fazer quando a TransLog manda um `price_cents` fracionário ou um `eta_hours` fracionário. Hoje o serviço recusa a resposta inteira com `503 CARRIER_UNAVAILABLE` (`TransLogClient.java:30-37`). Isso é defensável, mas bloqueia a venda por causa de um detalhe de formato. O enunciado também admite arredondar.

## Dúvidas

- **Preço da TransLog com mais de duas casas:** o anexo só mostra `price_cents` inteiro e diz para tratar "qualquer outra coisa" como fora do ar. O serviço trata `3310.5` e `3310.567` (e também `eta_hours: 41.5`) como fora do ar e devolve `503 CARRIER_UNAVAILABLE`. Li isso como uma leitura possível do enunciado, e não como um erro. Por isso não mudou a conclusão do C5.
- **A fronteira de R$ 300,00 do MINUS50 não pôde ser testada no valor exato:** com o catálogo inicial, nenhum carrinho soma 300,00. Testei 299,70 (recusado) e 318,40 (aceito); o código compara com `< 300.00` (`Minus50.java:12`). Pelo mesmo motivo, também não testei o brinde do GOLD com exatamente R$ 500,00 nem o boleto com total de exatamente R$ 1.000,00.
- **Mesmo aviso de pagamento depois do envio:** repetir o aviso depois que o pedido foi enviado devolve `200` com o pedido `SHIPPED`, e não o corpo exato da primeira resposta (`PAID`). Li "se a primeira deu certo, `200` e o pedido como está" como pedindo justamente isso. Nos testes sem mudança de estado entre as repetições, as respostas foram idênticas byte a byte.

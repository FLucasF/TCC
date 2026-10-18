# comportamento

## Visão geral

O serviço sobe com o banco vazio, cria as seis tabelas e carrega o catálogo sozinho, em menos de um segundo. Testei duas instâncias (8081 e 8082) ligadas ao mesmo PostgreSQL e a uma TransLog simulada. Ele faz o que o enunciado pede em todos os itens.

- **O resumo:** os dez exemplos do financeiro conferem campo a campo. As fronteiras também conferem: 5,00 kg no motoboy, o limite de 30% do cupom e o "meio para o par" no Pix e no crédito do clube.
- **As recusas:** a tabela "Quando dá erro" é seguida, com a ordem certa nos pedidos com dois problemas.
- **A TransLog:** a tradução (centavos, horas arredondadas para cima, CEP não atendido, os 2 segundos) está certa. O motoboy e a retirada continuam funcionando com ela fora do ar.
- **A concorrência:** o aviso repetido e a última peça aguentaram a disputa entre as duas máquinas em todas as rodadas. O código trava o pedido e os produtos no banco (`FOR UPDATE`) e grava o resultado de cada pagamento numa tabela própria.
- **Persistência e testes:** os dados sobrevivem ao reinício, e o `mvn verify` passa com 11 testes.

Não achei nenhuma divergência. Só ficam leituras de casos que o enunciado não decide; estão em "Dúvidas".

## Item a item

**C1, o serviço sobe.** Copiei o `workspace/` para `/tmp/run/ws`. O `mvn -q package -DskipTests` gerou `target/order-service.jar` em cerca de 15 s. Com o banco vazio, `PORT=8081 TRANSLOG_URL=http://localhost:9999 java -jar target/order-service.jar` subiu sem erro. O log mostra `Started Server ... @743ms` e `Javalin started in 112ms`. A primeira chamada, `GET /products/CAM-001`, deu `200`. Quem cria as tabelas (com `CREATE TABLE IF NOT EXISTS`) e o catálogo é `src/main/java/shop/db/Schema.java:330-355`, sob um lock do PostgreSQL (`pg_advisory_lock`), e a segunda instância, na 8082, subiu sobre o mesmo banco sem conflito.
Conclusão: segue

**C2, o catálogo.** Fiz `GET /products/{sku}` nos oito SKUs. Todos voltaram `200` com os valores do anexo, por exemplo `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`. O `REL-001` veio com `"discontinued":true`. O `BON-001` veio com estoque 0. Um SKU inexistente deu `404 {"error":"PRODUCT_NOT_FOUND"}`. Pedir o `REL-001` no resumo dá `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Mandei os exemplos para `POST /checkout/summary`, com o stub da TransLog dando a resposta de cada um:
- **Exemplos 1 a 7:** todos os campos batem. Por exemplo, o exemplo 2 deu `{"productsSubtotal":409.70,"couponDiscount":0.00,"shippingCost":15.60,"deliveryDays":7,"insurance":6.15,"paymentAdjustment":30.55,"finalTotal":462.00,"installments":6,"installmentAmount":77.00,"nextPurchaseCredit":8.19,"gift":false}`. O exemplo 7 deu `paymentAdjustment -72.48`, `finalTotal 1377.12`, `gift true`. O stub registrou a consulta com o peso e o serviço certos: `zip=01310100&weight_g=1800&service=EXP` no exemplo 1.
- **Exemplo 8:** `422 COUPON_NOT_APPLICABLE`.
- **Exemplo 9:** antes de pagar o primeiro pedido do CPF, o mesmo resumo com `WELCOME10` dava desconto de 15,98. Depois de pagar um pedido desse CPF, deu `422 COUPON_NOT_APPLICABLE`. Um CPF que só tem pedido cancelado continua recebendo o `WELCOME10`.
- **Exemplo 10:** `422 SHIPPING_METHOD_UNAVAILABLE`.

Fronteiras:
- **Motoboy:** com `MEI-001`×50 (5,00 kg) foi aceito, frete 18,00 e prazo 0. Com ×51 (5,10 kg) deu `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Limite do MINUS50:** 299,10 em produtos deu `COUPON_NOT_APPLICABLE`, e 319,60 deu desconto de 50,00. O valor exato de 300,00 não dá para montar com este catálogo (ver "Dúvidas").
- **Limite de 30% do cupom:** FREESHIPPING no motoboy com 19,90 em produtos deu desconto de 5,97, e não 18,00. Com 159,80 em produtos deu 18,00. Os exemplos 5 e 6 também mostram o limite.
- **"Meio para o par":** um Pix com total de 20,10 deu desconto de 1,00 (`paymentAdjustment -1.00`, `finalTotal 19.10`); a conta dá 1,005, e o arredondamento comum daria 1,01. O crédito GOLD sobre 59,70 deu 2,98 (`nextPurchaseCredit 2.98`); a conta dá 2,985, e o arredondamento comum daria 2,99.
- **Outros casos:**
  - O BUY3PAY2 sem 3 unidades deu desconto 0,00 e tirou o desconto do Pix.
  - O GOLD no motoboy paga frete de 18,00.
  - O seguro do Nordeste parou no limite de 30,00.
  - O cartão em 4× e em 12× deu as parcelas 42,38 e 15,25, que conferem com a tabela Price.
  - O boleto acima de R$ 1.000,00 deu `PAYMENT_METHOD_UNAVAILABLE`.
Conclusão: segue

**C4, as recusas.** Passei por todos os casos da tabela no resumo. Todos devolveram o código e o `422` pedidos.
- **`INVALID_ORDER`:** carrinho vazio ou ausente, item sem SKU, SKU repetido, quantidade 0, −1 ou ausente, CPF, e-mail ou `customer` ausente, e corpo que não é JSON.
- **Produtos:** `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED` e `INSUFFICIENT_STOCK` (`BON-001`, e `CAM-001`×51).
- **Clube, região e CEP:** `INVALID_CLUB_TIER` (ausente, `PLATINUM`, `gold`), `INVALID_REGION` e `INVALID_ZIP_CODE` (ausente, 7 dígitos, com traço, número).
- **Entrega e cupom:** `INVALID_SHIPPING_METHOD` e `INVALID_COUPON` (vazio `""`, `welcome10`, `BLACKFRIDAY`).
- **Pagamento:** `INVALID_PAYMENT_METHOD` e `INVALID_INSTALLMENTS` (Pix 2× e 0×, cartão 13× e 0×, boleto 2×).

Com dois problemas, vale sempre o primeiro da tabela:

| problemas | resposta |
|---|---|
| CPF ausente + SKU desconhecido | `INVALID_ORDER` |
| desconhecido + fora de linha | `UNKNOWN_PRODUCT` |
| fora de linha + sem estoque | `PRODUCT_DISCONTINUED` |
| sem estoque + nível inválido | `INSUFFICIENT_STOCK` |
| nível + região | `INVALID_CLUB_TIER` |
| região + CEP | `INVALID_REGION` |
| CEP + entrega | `INVALID_ZIP_CODE` |
| entrega + cupom | `INVALID_SHIPPING_METHOD` |
| motoboy acima de 5 kg + cupom inválido | `SHIPPING_METHOD_UNAVAILABLE` |
| TransLog fora do ar + cupom inválido | `503 CARRIER_UNAVAILABLE` |
| cupom inválido + pagamento inválido | `INVALID_COUPON` |
| MINUS50 não aplicável + pagamento inválido | `COUPON_NOT_APPLICABLE` |
| pagamento + parcelas | `INVALID_PAYMENT_METHOD` |
| boleto 2× acima de R$ 1.000 | `INVALID_INSTALLMENTS` |

O `POST /orders` usa a mesma validação (`OrderService.java:51`): com nível e região inválidos, deu `422 INVALID_CLUB_TIER`.
Conclusão: segue

**C5, a TransLog.** Com o stub respondendo cada caso:
- **Conversão:** `{"price_cents":1234,"eta_hours":24}` com região NORTHEAST deu frete 12,34 e prazo 3 (1 + 2). `eta_hours` 25 deu prazo 4. `price_cents` 5 com 48 h deu 0,05 e 2 dias. 0 centavos com 0 h deu 0,00 e 0 dias. A `ECONOMY` consultou `service=STD` e a `EXPRESS`, `service=EXP`.
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` deu `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Fora do ar:** `503 CARRIER_UNAVAILABLE` em todas estas respostas: outro `error`, HTTP 500, corpo que não é JSON, `{}`, `price_cents` em texto, `price_cents` 1234.5 e `eta_hours` 24.5.
- **Tempo limite:** com atraso de 1,5 s, a resposta foi aceita em 1559 ms. Com atraso de 2,5 s, deu `503 CARRIER_UNAVAILABLE` em 2013 ms.
- **Site fora do ar:** com o stub desligado (conexão recusada), `ECONOMY` e `EXPRESS` deram `503`. `COURIER` e `STORE_PICKUP` deram `200`, e o `POST /orders` com `COURIER` registrou o pedido (`201`).
Conclusão: segue

**C6, o ciclo do pedido.** Usei o CPF 60000000001, nível SILVER, com `CAM-001`×2.
- **Registro:** o `POST /orders` deu `201`, `AWAITING_PAYMENT`, com o `summary` idêntico ao do `/checkout/summary` (comparei com `jq -S`). O estoque continuou 50 e o cliente foi criado com saldo 0,00.
- **Fora de ordem:** enviar ou entregar antes de pagar deu `409 OPERATION_NOT_ALLOWED`.
- **Campos faltando:** pagar sem `paidAt`, sem `transactionId` ou com `paidAt:"ontem"` deu `422 INVALID_ORDER`.
- **Pagamento:** deu `200 PAID`, o estoque foi de 50 para 48 e o saldo ficou em 3,20. Outro `transactionId` deu `409 OPERATION_NOT_ALLOWED`.
- **Envio e entrega:** entregar o pedido pago deu `409`, e enviar sem `trackingCode` deu `422`. O envio deu `200 SHIPPED` com `trackingCode:"BR123456789"`. Depois disso, enviar de novo e cancelar deram `409`. A entrega deu `200 DELIVERED`, e entregar de novo ou cancelar deram `409`.
- **Cancelamento de um pedido pago (GOLD):** o estoque voltou de 45/98 para 48/99, e o saldo, de 12,98 para 0,00. Cancelar de novo, pagar com outro `transactionId` ou enviar deram `409`.
- **Cancelamento de um pedido não pago:** o estoque e o saldo não mudaram.
- **Prazo de 48 horas:** um `paidAt` exatamente 48 h depois do `createdAt` foi aceito (`PAID`), e também em `-03:00`. Com 48 h e 1 s, deu `409 ORDER_EXPIRED`, o pedido ficou `CANCELLED`, e o estoque e o saldo não mudaram. Repetir esse aviso deu a mesma recusa, e outro `transactionId` deu `409 OPERATION_NOT_ALLOWED`.
- **Avisos:** a lista saiu em ordem, com `PAYMENT_APPROVED`, `ORDER_SHIPPED` e `ORDER_CANCELLED` (inclusive do expirado e do não pago). Marcar como lido deu `READ`, e marcar de novo continuou `200 READ`.
- **Não existe:** pedido inexistente nas cinco operações deu `404 ORDER_NOT_FOUND`. Cliente e avisos de um CPF sem pedido deram `404 CUSTOMER_NOT_FOUND`. Aviso inexistente deu `404 NOTIFICATION_NOT_FOUND`.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Um depois do outro:** o primeiro aviso foi para a 8081 e o mesmo aviso repetido, para a 8082. As duas respostas saíram byte a byte iguais (`200 PAID`). O estoque foi de 42 para 41 e ficou em 41. O crédito ficou em 1,60 e saiu um `PAYMENT_APPROVED` só. Outro `transactionId` deu `409 OPERATION_NOT_ALLOWED`. Depois de reiniciar, o mesmo aviso ainda devolveu `200` sem baixar o estoque.
- **Ao mesmo tempo, mesmo aviso:** em cinco rodadas, disparei cinco avisos iguais juntos, alternando 8081 e 8082. Em todas, o estoque baixou uma unidade só, as cinco respostas foram iguais (`200`, uma resposta distinta), o crédito ficou em 1,60 e houve um único `PAYMENT_APPROVED`.
- **Ao mesmo tempo, `transactionId` diferentes:** em cinco rodadas, com cinco avisos de `transactionId` diferentes no mesmo pedido, sempre houve 1 `200 PAID` e 4 `409 OPERATION_NOT_ALLOWED`, com o estoque baixando uma vez só.

O mecanismo é o lock do pedido seguido da consulta ao resultado gravado (`OrderService.java:67-76`, `Dao.java:212-231`).
Conclusão: segue

**C8, a última peça.** Disparei pagamentos simultâneos, alternando as duas máquinas, em nove rodadas:

| disputa | aceitos | estoque final |
|---|---|---|
| `MOC-001` (1 unidade), 2 pedidos; um deles também levava `MEI-001`×1 | 1 | 0 |
| `TEN-001` (10), 6 pedidos de 5 | 2 | 0 |
| `JAQ-001` (3), 6 pedidos de 1 | 3 | 0 |
| `FON-001` (5), 6 pedidos de 2 | 2 | 1 |
| `FON-001`, última unidade, 2 pedidos | 1 | 0 |
| `CAM-001` (31), 6 pedidos de 15 | 2 | 1 |
| `CAM-001`, última unidade, 4 pedidos | 1 | 0 |
| `MEI-001` (97), 6 pedidos de 40 | 2 | 17 |
| `MEI-001`, 2 pedidos de 17 | 1 | 0 |

- **Na primeira rodada,** o perdedor foi o pedido misto, e o `MEI-001` não baixou (ficou em 97): o cancelamento é atômico.
- **Uma rodada com ordens cruzadas,** 3×`[TEN 4, CAM 1]` e 3×`[CAM 1, TEN 4]`, deu 2 aceitos, `TEN` 10→2 e `CAM` 30→28, sem deadlock nos logs (os produtos são travados em ordem fixa, `Dao.java:60-74`).
- **Em todas as rodadas,** cada perdedor recebeu `409 {"error":"INSUFFICIENT_STOCK"}`, o pedido ficou `CANCELLED` e saiu um único `ORDER_CANCELLED`, sem crédito. Repetir o aviso de um perdedor devolveu a mesma recusa. O estoque nunca ficou negativo; a tabela ainda tem `CHECK (stock >= 0)` (`Schema.java:295`).
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar, tirei uma foto pela API: um pedido entregue, o estoque de seis produtos, o saldo de dois clientes e a lista de avisos com um `READ`. Derrubei as duas instâncias e subi as duas de novo. A foto saiu idêntica (`diff` vazio), e o aviso de pagamento repetido continuou sem efeito.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` na cópia, sem as variáveis do banco, terminou com código 0. Rodaram 11 testes, sem falha: 10 em `shop.ShopTest` (com H2 em memória) e 1 em `shop.TransLogClientTest`.
Conclusão: segue

## O que mudaria primeiro

Nada do que testei falha. A melhoria mais útil seria tornar explícita a recusa de `price_cents` com casas decimais. Hoje ela é tratada como "fora do ar" (`TransLogClient.java:252-255`), o que é uma leitura possível do "qualquer outra coisa" do anexo. Mas o motivo da recusa fica invisível para quem opera; um log com a resposta crua da TransLog ajudaria a diagnosticar.

## Dúvidas

- **`price_cents` não inteiro** (1234.5) dá `503 CARRIER_UNAVAILABLE`. O anexo diz que o preço vem em centavos e que "qualquer outra coisa" é fora do ar. Li a recusa como aceitável, e não como erro de conversão.
- **`"coupon": null`** é tratado como cupom não informado (resumo normal, com o desconto do Pix). O enunciado só fala de "não vir" e de "vazio"; considerei o `null` uma ausência.
- **`paidAt` em formato inválido** dá `422 INVALID_ORDER`, igual a um `paidAt` ausente. Achei razoável.
- **Aviso repetido depois que o pedido avançou:** o mesmo `transactionId` de um pagamento aceito, repetido depois da entrega, devolve `200` com o pedido no estado atual (`DELIVERED`). Li "o pedido como está" do anexo como o estado atual.
- **Fronteiras que não dá para montar:** todos os preços do catálogo terminam em ,90, então nenhuma soma de menos de 100 itens dá exatamente R$ 300,00 (MINUS50) ou R$ 500,00 (brinde). Testei logo abaixo e acima do limite do MINUS50; o brinde só foi visto acima do limite (exemplo 7).
- **Sem acesso direto ao banco:** não havia `psql` na máquina. Conferi o estoque, o saldo e os avisos só pela API.

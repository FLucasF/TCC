# comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio em cerca de 5 s, cria as tabelas e o catálogo e guarda tudo entre reinícios. O cálculo do resumo está certo: os exemplos 1 a 9 do financeiro conferem campo a campo, assim como as fronteiras que testei (motoboy com exatamente 5 kg, o teto de 30% do cupom, o arredondamento "meio para o par" em 2,985). O ciclo normal do pedido também funciona: pagar, enviar, entregar, cancelar com e sem devolução, avisos e marcar como lido.

Os problemas, do mais grave para o menos grave:

1. **A última peça é vendida mais de uma vez.** Com duas máquinas, dois pagamentos simultâneos da última mochila passaram os dois. Seis pedidos para duas unidades passaram os seis, nas três rodadas. O estoque não fica negativo, mas termina num valor errado, e cada cancelamento devolve unidades que nunca existiram.
2. **O pagamento recusado não cancela o pedido.** Isso vale para o pagamento fora do prazo (`ORDER_EXPIRED`) e para o pagamento sem estoque (`INSUFFICIENT_STOCK`). O pedido continua `AWAITING_PAYMENT`, nenhum aviso `ORDER_CANCELLED` é criado, e um segundo aviso com outro `transactionId` foi aceito e deixou o pedido `PAID`.
3. **A TransLog não atender o CEP vira "fora do ar".** O serviço devolve `503 CARRIER_UNAVAILABLE` em vez de `422 SHIPPING_METHOD_UNAVAILABLE`, então o exemplo 10 falha.
4. **Avisos de pagamento simultâneos dão 500.** O efeito acontece uma vez só, mas os perdedores recebem 500 em vez da resposta da primeira vez ou de `409`.
5. **Recusas menores fora do enunciado:**
   - um item sem SKU dá 500;
   - um produto fora de linha junto com um sem estoque devolve `INSUFFICIENT_STOCK`;
   - numa operação sobre um pedido que não existe, a falta de um campo é conferida antes do 404.

## Item a item

**C1, o serviço sobe.**
- Copiei o projeto para `/tmp/run/app`, compilei com `mvn -q package -DskipTests` (9 s) e subi o jar na 8081 e na 8082 com o banco vazio. Os logs dizem "Started LojaPedidosApplication in 4.709 seconds" e "4.522 seconds".
- O `src/main/resources/schema.sql` cria as tabelas com `IF NOT EXISTS` e insere o catálogo com `ON CONFLICT DO NOTHING`, então a segunda máquina e os reinícios não dão erro.

Conclusão: segue

**C2, o catálogo.**
- `GET /products/{sku}` devolveu os oito produtos do anexo com nome, preço, estoque e `discontinued` iguais ao anexo. `REL-001` veio com `"discontinued":true`.
- O peso vem com três casas (`"weightKg":0.300`), mas o número é o mesmo do anexo.
- Um SKU inexistente devolveu `404 {"error":"PRODUCT_NOT_FOUND"}`.

Conclusão: segue

**C3, o resumo.**

Chamei `POST /checkout/summary` com o stub da TransLog respondendo o que cada exemplo diz:
- **Exemplos 1 a 7:** todos os valores batem com o financeiro. Por exemplo, o 2 deu `finalTotal 462.00`, `installmentAmount 77.00`, `paymentAdjustment 30.55` e `nextPurchaseCredit 8.19`; o 7 deu `paymentAdjustment -72.48`, `finalTotal 1377.12` e `gift true`.
- **Exemplos 8 e 9:** `422 COUPON_NOT_APPLICABLE`, como pedido. Para o 9, criei e paguei antes um pedido do mesmo CPF.
- **Exemplo 10:** esperava `422 SHIPPING_METHOD_UNAVAILABLE` e veio `503 {"error":"CARRIER_UNAVAILABLE"}` (ver C5).

Fronteiras:
- motoboy com exatamente 5,0 kg (TEN×4 + MEI×2): aceito, frete 18,00; com 5,1 kg: `422 SHIPPING_METHOD_UNAVAILABLE`;
- MINUS50 com produtos de 299,10: `COUPON_NOT_APPLICABLE`; com 319,60: desconto 50,00. Exatamente 300,00 não sai com os preços do catálogo (ver Dúvidas);
- teto de 30%: BUY3PAY2 com MEI×3 dá 17,91 (30% de 59,70) em vez de 19,90; o exemplo 6 confirma o mesmo para o FREESHIPPING;
- "meio para o par": MEI×10 no Centro-Oeste dá seguro de 1,5% de 199,00 = 2,985, e veio `2.98`;
- outros casos conferidos: cartão em 4× (parcela 21,19), cartão em 12× com GOLD no motoboy (frete cobrado, seguro limitado a 30,00), boleto acima de 1.000 recusado, PIX com cupom de desconto zero sem o desconto do PIX.
- Resumo e pedido registrado dão os mesmos valores: comparei o exemplo 1 por `/checkout/summary` e por `POST /orders`.

Um desvio de formato: os zeros às vezes saem sem as duas casas (`"couponDiscount":0`, `"shippingCost":0`, `"paymentAdjustment":0`), e o enunciado pede "2 casas decimais".

Conclusão: em parte

**C4, as recusas.**

Rodei cerca de 45 combinações em `/checkout/summary`. Quase toda a tabela confere:
- carrinho vazio, SKU duplicado, quantidade 0, −1 ou ausente, CPF, e-mail ou cliente ausente: `422 INVALID_ORDER`;
- SKU desconhecido: `UNKNOWN_PRODUCT`; REL-001: `PRODUCT_DISCONTINUED`; BON-001 ou MOC×2: `INSUFFICIENT_STOCK`;
- os casos de nível, região, CEP (ausente, 7 dígitos, com traço) e forma de entrega devolvem os códigos certos;
- cupom inexistente, vazio ou minúsculo: `INVALID_COUPON`;
- forma de pagamento, parcelas (PIX 2×, boleto 2×, cartão 0× e 13×) e boleto acima de 1.000: os códigos certos.

Pedidos com dois problemas que respeitaram a ordem:

| problemas | veio |
|---|---|
| quantidade 0 + SKU desconhecido | `INVALID_ORDER` |
| SKU desconhecido + nível inválido | `UNKNOWN_PRODUCT` |
| nível + região | `INVALID_CLUB_TIER` |
| região + CEP | `INVALID_REGION` |
| motoboy > 5 kg + cupom inválido | `SHIPPING_METHOD_UNAVAILABLE` |
| cupom + pagamento | `INVALID_COUPON` |
| FREESHIPPING na retirada + PIX 2× | `COUPON_NOT_APPLICABLE` |
| boleto 2× + total > 1.000 | `INVALID_INSTALLMENTS` |

Falhas:
- **Item sem `sku`** (`"items":[{"quantity":1}]`): esperava `422 INVALID_ORDER`, veio `500 Internal Server Error`. Também acontece em `POST /orders`. O log mostra `NullPointerException: element cannot be mapped to a null key` em `src/main/java/com/loja/service/CheckoutService.java:113`.
- **Fora de linha + sem estoque** (REL-001 + BON-001, nas duas ordens): esperava `PRODUCT_DISCONTINUED` (ordem 3), veio `INSUFFICIENT_STOCK` (ordem 4). O laço confere fora de linha e estoque produto a produto (`CheckoutService.java:141` e `:151`), não regra a regra.
- **Pedido que não existe, com campos faltando** (`POST /orders/nope/payment '{}'` e `/shipment '{}'`): esperava `404 ORDER_NOT_FOUND`, veio `422 INVALID_ORDER` (`OrderService.java:118` confere antes da busca). Com o corpo completo, o 404 sai certo, e os 404 de cliente, avisos e aviso inexistente também conferem.

Conclusão: em parte

**C5, a TransLog.**

O que confere:
- **Tradução:** `price_cents 1999` vira frete `19.99`; 24 h viram 1 dia e 25 h viram 2. 48 h no Nordeste dão 4 dias (2 + 2), o exemplo 1 no Norte dá 5 (2 + 3) e 0 h na expressa dão 0 dias.
- **A chamada:** `zip=…&weight_g=1800&service=EXP` (ou `STD`), com o peso em gramas inteiras.
- **Respostas inválidas viram 503 `CARRIER_UNAVAILABLE`:** `{"error":"BOOM"}`, HTTP 500, HTTP 404, `{}`, texto puro, `price_cents` decimal ou como string.
- **Tempo limite:** com 2,5 s de demora veio 503 em 2.015 ms; com 1,5 s, a resposta foi aceita.
- **Com a TransLog totalmente fora:** COURIER e STORE_PICKUP continuam dando 200, inclusive `POST /orders` com COURIER (201). ECONOMY e EXPRESS dão 503.

Falha: `{"error":"ZIP_NOT_SERVED"}` dá `503 CARRIER_UNAVAILABLE` na econômica e na expressa, quando o certo é `422 SHIPPING_METHOD_UNAVAILABLE`. O cliente da TransLog devolve o mesmo "indisponível" para CEP não atendido e para fora do ar (`src/main/java/com/loja/external/TransLogClient.java:35-36` e `:53`), e o serviço traduz os dois como 503 (`CheckoutService.java:201`).

Observação: o serviço consulta a TransLog duas vezes por resumo (no log do stub, cada chamada aparece em dobro). Com 1,5 s de demora, o resumo levou 3.101 ms.

Conclusão: em parte

**C6, o ciclo do pedido.**

O que confere:
- **Registrar:** 201 com `AWAITING_PAYMENT`; o estoque de CAM-001 continuou 49.
- **Pagar:** 200 e `PAID`; o estoque baixou para 47 e o crédito SILVER de 3,20 entrou no saldo; saiu `PAYMENT_APPROVED`.
- **Enviar:** sem `trackingCode` dá 422 `INVALID_ORDER`; com o código, `SHIPPED`, o código de rastreio gravado e o aviso `ORDER_SHIPPED`.
- **Entregar:** `DELIVERED`.
- **Fora de ordem:** enviar ou entregar antes de pagar, entregar um pedido só pago, enviar duas vezes, cancelar depois de enviado ou de entregue, e pagar com outro `transactionId`: todos `409 OPERATION_NOT_ALLOWED`.
- **Cancelar pago (MEI×5):** estoque 95 → 100, crédito 1,99 → 0,00, aviso `ORDER_CANCELLED`.
- **Cancelar não pago:** estoque e saldo sem mudança, e um aviso `ORDER_CANCELLED`.
- **Pagar ou cancelar um pedido cancelado:** 409.
- **Primeira compra:** WELCOME10 vale para um CPF que só tem pedido cancelado.
- **Avisos:** `POST /notifications/2/read` duas vezes dá `READ` nas duas. Um id inexistente dá `404 NOTIFICATION_NOT_FOUND`.
- **Prazo:** pago a 48 h menos 0,4 s do registro, foi aceito.

Falha no prazo de 48 horas. Mandei `paidAt` a 48 h + 1,6 s do registro:
- veio `409 ORDER_EXPIRED`, como pedido;
- mas `GET /orders/{id}` continuou `AWAITING_PAYMENT` e a lista de avisos ficou vazia (`[]`);
- em seguida, um aviso com outro `transactionId` foi aceito, e o pedido ficou `PAID`.

O mesmo acontece com a falta de estoque no pagamento (ver C8). O código muda a situação e grava o aviso, mas logo em seguida lança uma `OrderException`, que é uma `RuntimeException`, dentro do `@Transactional`. Com isso a transação desfaz tudo (`src/main/java/com/loja/state/AwaitingPaymentState.java:31-33` e `:39-41`; `src/main/java/com/loja/service/OrderService.java:115`).

Formato: `createdAt` sai como `"2026-10-18T12:52:32.803342909"`, sem o `Z` e com frações de segundo, e não no formato `2026-10-14T15:30:00Z` do anexo.

Conclusão: em parte

**C7, o aviso de pagamento repetido.**

Um depois do outro (o primeiro na 8081, o repetido na 8082):
- as duas respostas são idênticas, byte a byte;
- o estoque foi 47 → 44 → 44 e o crédito ficou 11,98 → 11,98, com um só `PAYMENT_APPROVED`;
- outro `transactionId` no pedido pago dá `409 OPERATION_NOT_ALLOWED`;
- o aviso repetido de um pagamento recusado repete a mesma recusa (`409 ORDER_EXPIRED` duas vezes, `409 INSUFFICIENT_STOCK` duas vezes). Isso só acontece porque nada foi gravado na primeira vez (ver C6).

Ao mesmo tempo, com `curl --parallel` mandando cinco avisos (3 na 8081, 2 na 8082), cinco rodadas para cada caso:
- **Mesmo `transactionId`:** em todas as rodadas, o estoque baixou uma vez (delta 2), o crédito entrou uma vez (7,99) e saiu um só `PAYMENT_APPROVED`. Mas só uma resposta foi `200 PAID`; as outras quatro foram `500 Internal Server Error`, não a resposta da primeira vez.
- **`transactionId` diferentes:** em todas as rodadas, um só foi aceito, mas os outros quatro foram `500` em vez de `409 OPERATION_NOT_ALLOWED`.

O log mostra 20 vezes `duplicate key value violates unique constraint "order_payments_order_id_key"`. A restrição única impede o efeito em dobro, mas o erro não é tratado.

Conclusão: em parte

**C8, a última peça.**

Com as duas máquinas, um pagamento em cada, disparados juntos:
- **Dois pedidos para a última MOC-001** (estoque 1), quatro rodadas: nas quatro, os dois responderam `200 PAID`, os dois clientes receberam `PAYMENT_APPROVED` e o estoque terminou 0.
- **Seis pedidos para duas FON-001**, três rodadas: nas três, os seis responderam `200 PAID` e o estoque terminou **1**, não 0.

O estoque não ficou negativo, mas porque as baixas se sobrescrevem: o serviço perde atualizações. Ao cancelar os pedidos para restaurar, cada cancelamento devolveu uma unidade. O estoque da MOC-001 subiu 1 → 2 → 3 → 4 → 5 e o da FON-001 foi a 20, e continuou assim depois do reinício.

Causa provável (pela leitura, não verificada a fundo): `hasStockForOrder` carrega o produto sem trava (`OrderService.java:308`). Depois, o `findBySkuForUpdate` (`OrderService.java:225`) devolve a mesma instância já carregada, com o estoque antigo.

Quem perde não recebeu `409 INSUFFICIENT_STOCK` porque ninguém perdeu. No caso sequencial (pagar o segundo pedido depois que o primeiro levou a mochila), veio `409 INSUFFICIENT_STOCK`, mas o pedido ficou `AWAITING_PAYMENT`, sem `ORDER_CANCELLED`, e depois foi pago com outro `transactionId`.

Conclusão: não segue

**C9, os dados não se perdem.**
- Derrubei as duas máquinas e subi de novo (cerca de 5,3 s cada).
- O pedido entregue (`DELIVERED`, `BR123456789`), o estoque de CAM-001 (47), o saldo do cliente 91000000002 (3,20) e os dois avisos dele (um `READ`, outro `NEW`) ficaram idênticos: o `diff` entre antes e depois deu vazio.
- O catálogo não foi reinserido por cima, porque o estoque alterado da MOC-001 (5) continuou lá.

Conclusão: segue

**C10, os testes do próprio agente.**
- `mvn -q verify` na cópia, sem `DB_URL`, `DB_USER` e `DB_PASSWORD`, terminou com código 0.
- Roda 1 teste (`LojaPedidosApplicationTest.contextLoads`, com H2): "Tests run: 1, Failures: 0, Errors: 0".

Conclusão: segue

## O que mudaria primeiro

1. **Corrigir a baixa de estoque no pagamento.** Duas formas: travar e reler o produto antes de conferir (o `SELECT … FOR UPDATE` como primeira leitura, ou um `refresh`), ou baixar com um `UPDATE products SET stock = stock - :q WHERE sku = :sku AND stock >= :q` que confere as linhas afetadas. Sem isso, o serviço repete o problema que o enunciado mais quer evitar, e os cancelamentos ainda criam estoque do nada.
2. **Fazer a recusa do pagamento ser gravada.** O cancelamento por prazo ou por falta de estoque precisa ficar salvo, com o aviso e a resposta guardados para repetir. Por exemplo: registrar a tentativa e o cancelamento e devolver o erro sem desfazer a transação. Hoje um pedido expirado pode ser pago depois.

As duas correções seguintes são menores: separar `ZIP_NOT_SERVED` de "fora do ar" e tratar o conflito da restrição única do pagamento (devolver a resposta guardada ou `409`, em vez de 500).

## Dúvidas

- **48 horas exatas:** não deu para testar. O `createdAt` tem frações de segundo e o `paidAt` só tem segundos. Testei 48 h − 0,4 s (aceito) e 48 h + 1,6 s (recusado, mas sem cancelar).
- **Fronteiras de valor:** "exatamente R$ 300,00" do MINUS50 (e também 500,00 do brinde e 1.200,00 do teto do seguro no Norte) não sai com os preços do catálogo. Todo preço termina em ,90, e uma soma redonda exigiria um múltiplo de 10 itens cujos valores são todos pares. Usei os valores vizinhos (299,10 e 319,60).
- **O banco ficou alterado:** o estoque da MOC-001 está em 5 e o da FON-001 em 20 no banco compartilhado, por causa da venda dupla de C8. Não há como reverter pela API.

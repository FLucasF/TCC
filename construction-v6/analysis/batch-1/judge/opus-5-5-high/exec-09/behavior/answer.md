# o comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio em cerca de 5 s e já cria o catálogo. O cálculo do resumo está certo: os exemplos 1 a 9 do financeiro conferem campo a campo, e as fronteiras testadas também (motoboy com exatamente 5 kg, teto de 30%, "meio para o par"). O caminho feliz do pedido funciona: pagar, enviar, entregar, cancelar, estoque, crédito e avisos. Os dados sobrevivem ao reinício. Os problemas, do mais grave para o menos grave:

1. **Vende o que não tem.** Com duas máquinas, a última peça foi paga duas vezes em 19 de 20 rodadas, e o estoque ficou em 0 porque uma das baixas se perdeu.
2. **A recusa no pagamento não fica gravada.** O pedido expirado e o pedido sem estoque devolvem o 409 certo, mas continuam `AWAITING_PAYMENT` e não geram aviso. Por causa disso, o mesmo aviso repetido acabou pagando um pedido que tinha sido recusado por falta de estoque, e outro `transactionId` pagou um pedido expirado.
3. **`ZIP_NOT_SERVED` vira 503 `CARRIER_UNAVAILABLE`** em vez de 422 `SHIPPING_METHOD_UNAVAILABLE`, o que derruba o exemplo 10.
4. **Falhas menores nas recusas:** item sem SKU dá 500; produto fora de linha junto com produto sem estoque dá `INSUFFICIENT_STOCK`; pedido inexistente com campo faltando dá 422 em vez de 404.
5. **Formato:** alguns zeros saem como `0` em vez de `0.00`, e as datas saem sem `Z`.

## Item a item

**C1, o serviço sobe.** Compilei uma cópia com `mvn -q package -DskipTests` e subi com `TRANSLOG_URL` apontando para a imitação. No banco vazio, o log mostra `Started LojaPedidosApplication in 5.116 seconds`, e o primeiro `GET /products/CAM-001` respondeu 200. As tabelas e o catálogo vêm de `src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS` e `INSERT ... ON CONFLICT (sku) DO NOTHING`, `schema.sql:89`). A segunda máquina subiu em 3,8 s e um reinício em 4,2 s, sem erro.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Nome, preço, estoque e `discontinued` conferem com o anexo, e `REL-001` volta com `"discontinued":true`. Um SKU que não existe volta 404 `PRODUCT_NOT_FOUND`. Única diferença: o peso sai com três casas (`"weightKg":0.300`), mas o valor é o mesmo.
Conclusão: segue

**C3, o resumo.** Rodei os dez exemplos no `POST /checkout/summary` e no `POST /orders`, com a TransLog respondendo o que cada exemplo diz.
- **Exemplos 1 a 7:** todos os campos conferem, e o resumo e o pedido registrado dão os mesmos valores. Por exemplo, o 2 deu `finalTotal 462.00`, `installmentAmount 77.00`, `paymentAdjustment 30.55` e `nextPurchaseCredit 8.19`; o 7 deu `paymentAdjustment -72.48`, `finalTotal 1377.12` e `gift true`.
- **Exemplos 8 e 9:** 422 `COUPON_NOT_APPLICABLE`, como pedido. O 9 foi feito com um cliente que já tinha um pedido pago.
- **Exemplo 10:** esperava 422 `SHIPPING_METHOD_UNAVAILABLE` e veio **503 `CARRIER_UNAVAILABLE`** (detalhe em C5).

Fronteiras:
- **Motoboy:** com `MEI-001`×50 (5,00 kg) passa com frete 18.00; com ×51 (5,10 kg) dá 422 `SHIPPING_METHOD_UNAVAILABLE`.
- **MINUS50:** 299,10 em produtos é recusado; 319,60 dá desconto de 50.00. Exatamente 300,00 não dá para montar com os preços do catálogo.
- **Teto de 30%:** BUY3PAY2 com `MEI-001`×3 dá desconto 17.91 (30% de 59,70) em vez de 19,90.
- **"Meio para o par":** seguro de `MEI-001`×15 no Sul, 298,5 centavos, deu `2.98`; crédito GOLD de `MEI-001`×3, também 298,5 centavos, deu `2.98`.

Também conferem: 12× no cartão (`7.63` × 12 = `91.56`), WELCOME10 aceito para quem só tem pedido cancelado, GOLD no motoboy pagando 18.00 e boleto acima de 1.000 dando `PAYMENT_METHOD_UNAVAILABLE`.

Os valores conferem em número, mas alguns zeros saem sem as duas casas (`"couponDiscount":0`, `"shippingCost":0`, `"paymentAdjustment":0`). O `GET` do mesmo pedido devolve `0.00`, porque esses valores vêm de `BigDecimal.ZERO` (`CheckoutService.java:65`, `:216`; `PixPayment.java:25`; `StorePickupShipping.java:16`). O cálculo está todo certo; o que falha é um dos dez exemplos.
Conclusão: em parte

**C4, as recusas.** Testei todas as linhas da tabela no resumo e no registro. Saem certos, com 422: carrinho vazio ou ausente, SKU repetido, quantidade 0, −1 ou ausente, sem CPF, sem e-mail, SKU desconhecido, fora de linha, sem estoque, nível, região, CEP (ausente, 7 dígitos, com traço), forma de entrega, cupom inválido, vazio ou em minúsculas, forma de pagamento, e parcelas (Pix 2×, boleto 2×, cartão 0× e 13×). As operações de pedido também saem certas: 404 `ORDER_NOT_FOUND` nas cinco, 422 `INVALID_ORDER` sem `transactionId`, sem `paidAt` ou sem `trackingCode`, 409 para operação fora de ordem, e os 404 de cliente, de aviso e de avisos de CPF desconhecido.

Na ordem, conferem: sem SKU + sem CPF, desconhecido + nível, nível + região, região + CEP, CEP + entrega, motoboy acima de 5 kg + cupom, cupom inválido + pagamento, cupom não aplicável + pagamento, pagamento + parcelas, e parcelas + boleto acima de 1.000.

Falhas:
- **Item sem SKU** (`"items":[{"quantity":1}]`): esperava 422 `INVALID_ORDER` e veio **500**. O log mostra `NullPointerException: element cannot be mapped to a null key`, do `groupingBy` em `CheckoutService.java:113`.
- **Fora de linha + sem estoque** (`REL-001` e `BON-001`, nas duas ordens do carrinho): esperava `PRODUCT_DISCONTINUED` (ordem 3) e veio **`INSUFFICIENT_STOCK`**. As duas checagens rodam produto a produto no mesmo laço (`CheckoutService.java:140` e `:150`).
- **Pedido inexistente com campo faltando** (`POST /orders/<uuid inexistente>/payment` com `{}`, e o mesmo no `/shipment`): esperava 404 `ORDER_NOT_FOUND`, que vem antes na tabela, e veio **422 `INVALID_ORDER`**. Os campos são conferidos antes de buscar o pedido (`OrderService.java:117`, `:144`).
Conclusão: em parte

**C5, a TransLog.** A chamada vai certa: `GET /v2/quote?zip=01310100&weight_g=1800&service=STD` para `CAM-001`×6 na econômica, e `service=EXP` na expressa. A tradução confere:
- **Preço:** 1999 → 19.99, 123456 → 1234.56, 5 → 0.05.
- **Prazo:** 48 h → 2 dias, 49 h → 3, 0 h → 0, e 1 h no Nordeste → 1 + 2 = 3.
- **Respostas estranhas:** `price_cents` fracionário, `eta_hours` fracionário, preço como texto, `{}`, texto puro, HTTP 500 e HTTP 404 viram 503 `CARRIER_UNAVAILABLE`.
- **Tempo limite:** com atraso de 2,5 s, volta 503 em 2.020 ms. Com atraso de 1,5 s, a resposta certa veio em 3.101 ms, porque a TransLog é consultada duas vezes por resumo (`isAvailable` e depois `calculate`, nas duas `*Shipping`).
- **TransLog fora do ar** (processo parado): econômica e expressa dão 503; motoboy e retirada dão 200, e o registro com motoboy dá 201.

Falha: **`{"error":"ZIP_NOT_SERVED"}` vira 503 `CARRIER_UNAVAILABLE`** em vez de 422 `SHIPPING_METHOD_UNAVAILABLE`. O cliente devolve "não disponível" tanto para CEP não atendido quanto para fora do ar (`TransLogClient.java:35-36` e `:53`), e o serviço trata todo "não disponível" de econômica e expressa como transportadora fora (`CheckoutService.java:199-201`).
Conclusão: em parte

**C6, o ciclo do pedido.** O que funciona:
- **Estoque e crédito:** pedido SILVER com `CAM-001`×2 + `MEI-001`×3. No registro, o estoque continuou CAM 50 e MEI 99, e o saldo 0.00. No pagamento, foi para CAM 48, MEI 96 e saldo 4.39 (o crédito do resumo), com aviso `PAYMENT_APPROVED`.
- **Envio e entrega:** o envio deu `SHIPPED`, `trackingCode BR123456789` e aviso `ORDER_SHIPPED`; a entrega deu `DELIVERED`.
- **Fora de ordem:** entregar um pago, enviar duas vezes, cancelar um enviado, entregar ou cancelar um entregue, enviar ou entregar um aguardando, e pagar ou enviar um cancelado dão todos 409 `OPERATION_NOT_ALLOWED`.
- **Cancelar pago:** FON foi de 4 para 5, o saldo de 10.00 para 0.00, com aviso `ORDER_CANCELLED`.
- **Cancelar não pago:** nada muda, e gera aviso.
- **Avisos lidos:** `POST /notifications/7/read` duas vezes dá `READ` nas duas vezes.
- **Exatamente 48 h:** `paidAt` = `createdAt` + 48 h, com a mesma fração de segundo, foi aceito (200).

Falhas:
- **Pagamento 48 h + 1 s:** voltou 409 `ORDER_EXPIRED`, como pedido, mas o `GET` mostra o pedido ainda **`AWAITING_PAYMENT`**, e a lista de avisos do cliente está **vazia** (`[]`).
- **Sem estoque na hora do pagamento** (`MOC-001`, segundo pedido): voltou 409 `INSUFFICIENT_STOCK`, mas o pedido também **continua `AWAITING_PAYMENT`, sem aviso**.

A causa das duas é a mesma: o estado muda para `CANCELLED` e o aviso é gravado (`AwaitingPaymentState.java:29-33`, `:37-41`), mas em seguida vem a exceção, dentro de um `@Transactional` (`OrderService.java:115`), e tudo é desfeito.

Formato: o `createdAt` sai sem `Z` e com precisão diferente no `POST` e no `GET` (`"2026-10-18T05:17:50.78443272"` contra `"...50.784433"`), porque é `LocalDateTime` (`OrderService.java:86`, `OrderResponse.java:9`).
Conclusão: em parte

**C7, o aviso de pagamento repetido.** O que funciona:
- O mesmo aviso (`TX-R1`) enviado três vezes deu três respostas 200 idênticas byte a byte. O estoque baixou uma vez só (48 → 47), o crédito entrou uma vez só (4.00), e há um único `PAYMENT_APPROVED`.
- Repetido depois do envio, devolve 200 com o pedido como está (`SHIPPED`), sem mexer no estoque.
- Outro `transactionId` num pedido pago ou enviado dá 409 `OPERATION_NOT_ALLOWED`.

Falhas, todas consequências de a recusa não ficar gravada (C6):
- **Recusa por estoque:** o pedido Y foi recusado com `INSUFFICIENT_STOCK`. Cancelei o X, o estoque de MOC voltou a 1, e **o mesmo aviso `TX-Y` repetido deu 200 e `PAID`**, com o estoque indo a 0. Esperava a mesma recusa.
- **Recusa por prazo:** o pedido Z foi recusado com `ORDER_EXPIRED` (`TX-Z1`). Em seguida, **`TX-Z2` com `paidAt` atual deu 200 e `PAID`**, e o estoque de CAM baixou de 47 para 46. Esperava 409 `OPERATION_NOT_ALLOWED`, porque o pedido já devia estar cancelado.
- Depois disso, repetir o `TX-Z1` dá `OPERATION_NOT_ALLOWED`, e não a mesma `ORDER_EXPIRED` da primeira vez.

O repetido de um pagamento aceito está certo; o repetido de uma recusa, não.
Conclusão: em parte

**C8, a última peça.** Duas máquinas (8081 e 8082) no mesmo banco. Deixei `TEN-001` com estoque 1 e rodei 20 rodadas. Em cada uma, registrei um pedido de 1 unidade em cada máquina, disparei os dois pagamentos juntos (dois `curl` em paralelo, um por máquina) e, para repor, cancelei o vencedor.
- **Rodada 1:** A 200 `PAID`, B 409 `INSUFFICIENT_STOCK`, estoque 0. Certo.
- **Rodadas 2 a 20:** **A 200 `PAID` e B 200 `PAID`**, com o estoque em 0, não em −1.

Ou seja, duas vendas de uma peça, e uma das baixas se perdeu. Somando: 10 unidades de início, 9 do pedido de preparo + 19 pedidos B ainda pagos = 28 unidades vendidas, e o estoque mostra 0.

O estoque nunca fica negativo (há `CHECK (stock >= 0)` em `schema.sql:6`), mas só porque a baixa se perde, não porque o pagamento é recusado. A explicação provável: `hasStockForOrder` carrega o produto sem trava (`OrderService.java:308`). Depois, o `findBySkuForUpdate` (`:225`) espera a trava, mas reaproveita a cópia já carregada, com o estoque antigo, e grava `1 − 1 = 0` por cima da baixa da outra máquina. A falha principal do item acontece quase sempre.
Conclusão: não segue

**C9, os dados não se perdem.** Anotei estoques, um pedido `DELIVERED` com rastreio, o saldo de um cliente e duas listas de avisos (uma com aviso `READ`). Derrubei as duas máquinas, confirmei que as duas estavam fora e subi de novo. Tudo idêntico (`diff` vazio): os estoques não voltaram aos do anexo (`ON CONFLICT DO NOTHING`), o pedido continua `DELIVERED` com `BR123456789`, o saldo continua 4.39, e o aviso lido continua `READ`.
Conclusão: segue

**C10, os testes do próprio agente.** `mvn -q verify` numa cópia limpa terminou com `exit=0`. Roda **1 teste** (`LojaPedidosApplicationTest.contextLoads`, sobre H2): `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.
Conclusão: segue

## O que mudaria primeiro

1. **Gravar a recusa e travar o estoque de verdade.** Hoje, `ORDER_EXPIRED` e `INSUFFICIENT_STOCK` lançam exceção dentro da mesma transação que muda a situação para `CANCELLED`, e tudo é desfeito. Basta gravar o cancelamento (e o aviso) antes e devolver a recusa sem desfazer. Isso corrige C6, C7 e o pagamento de pedidos que já deviam estar cancelados. No mesmo trecho, conferir o estoque com a linha já travada (ou baixar com `UPDATE ... SET stock = stock - ? WHERE stock >= ?`), em vez de olhar uma cópia antiga. É isso que hoje deixa vender a mesma peça duas vezes (C8).
2. **Separar "CEP não atendido" de "TransLog fora do ar"** no `TransLogClient`, devolvendo 422 `SHIPPING_METHOD_UNAVAILABLE` no primeiro caso. Corrige o exemplo 10 e, consultando uma vez só, também acaba com a chamada dobrada.

## Dúvidas

- **Pagamento repetido depois de a situação mudar:** o enunciado diz "200 e o pedido como está". Li que, depois do envio, devolver o pedido `SHIPPED` está certo, mesmo não sendo o mesmo corpo da primeira resposta.
- **TransLog com `price_cents` ou `eta_hours` fracionários:** li como "qualquer outra coisa", logo fora do ar (503), que foi o que o serviço fez.
- **FREESHIPPING para cliente GOLD na econômica:** o serviço zera o frete pelo GOLD e o desconto do cupom fica 0. O enunciado não decide se o cupom é aceito ou recusado nesse caso; não contei contra.
- **`installments` e `coupon` vindos como `null`:** tratados como ausentes. Li assim ("pode não vir").
- **Id de aviso não numérico** (`/notifications/abc/read`): deu 400 genérico. O enunciado só fala de aviso que não existe; não contei contra.
- **Zeros sem casas e datas sem `Z`:** são desvios de formato do anexo, mas o valor é o mesmo. Registrei em C3 e C6 sem mudar as conclusões por eles.
- **MINUS50 exatamente em 300,00:** não dá para montar esse valor com os preços do catálogo, então testei a fronteira por 299,10 e 319,60.

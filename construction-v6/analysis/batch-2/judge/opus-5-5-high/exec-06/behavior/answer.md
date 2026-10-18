# comportamento

## Visão geral

O serviço sobe sozinho em menos de 1 s com o banco vazio. Ele cria as tabelas e o catálogo, e os dados sobrevivem ao reinício. A concorrência pedida pelo enunciado foi bem resolvida. A trava `SELECT … FOR UPDATE` no pedido e o `UPDATE … WHERE stock >= ?` fazem com que o aviso repetido simultâneo e a disputa pela última peça entre duas máquinas deem o resultado certo nos pedidos de um item só. Oito dos dez exemplos do financeiro conferem campo a campo. Os problemas, do mais grave ao menos grave:

1. **Cartão de 4× a 12× sempre dá `500 Server Error`.** É o exemplo 2: `BigDecimal.pow` com expoente negativo (`PaymentAdjustmentCalculator.java:27`). Toda compra parcelada com juros quebra.
2. **Um pedido de vários itens cancelado por falta de estoque no pagamento leva embora o estoque dos outros itens.** O serviço faz o commit depois de já ter baixado parte dos itens (`OrderService.java:123-129`). Vi isso tanto em sequência quanto numa disputa entre as duas máquinas.
3. **O resumo e o pedido divergem no WELCOME10.** O resumo trata todo cliente como primeira compra (`CheckoutService.java:62`, `conn == null ||`). O exemplo 9 dá `200` com desconto no resumo e `422` no pedido.
4. **O aviso de pagamento repetido depois de uma recusa devolve `200` e o pedido.** O enunciado pede a mesma recusa (`409 ORDER_EXPIRED` ou `409 INSUFFICIENT_STOCK`).
5. **Recusas fora da tabela.** Um valor inexistente de nível, região, entrega ou pagamento dá `500`. O cupom `""` é aceito. Várias recusas saem fora da ordem da tabela. O limite do boleto é aplicado ao valor final (com a tarifa), e não ao total do pedido.
6. **Cancelar um pedido já cancelado dá `200`** e gera outro aviso `ORDER_CANCELLED`.

## Item a item

**C1, o serviço sobe.** Copiei para `/tmp/svc`, rodei `mvn -q package -DskipTests` e subi com `PORT=8081 TRANSLOG_URL=… java -jar target/order-service-1.0.0.jar`. A primeira subida, com o banco vazio, respondeu em 835 ms. A segunda máquina (8082) e o reinício levaram uns 830 ms. As seis tabelas e o catálogo foram criados sozinhos (`Database.java:31` e seguintes, `ProductRepository.java:15-50`).

Testei também, à parte, as duas máquinas subindo **ao mesmo tempo** num banco vazio. Criei três bancos auxiliares no mesmo servidor e os apaguei no fim. Nas três tentativas, uma das duas morreu com `Failed to initialize database schema`, causado por `duplicate key value violates unique constraint "pg_type_typname_nsp_index"` / `type "products" already exists`. Ela sobe normalmente se for reiniciada. Como o enunciado não diz que as duas sobem juntas pela primeira vez, deixei isso em "Dúvidas".
Conclusão: segue

**C2, o catálogo.** `GET /products/{sku}` nos oito SKUs devolve nome, preço, estoque e `discontinued` iguais ao anexo. O `REL-001` vem com `"discontinued":true`, e pedi-lo dá `422 PRODUCT_DISCONTINUED`. Um SKU inexistente dá `404 PRODUCT_NOT_FOUND`. O único desvio de forma é o peso, que sai com três casas (`"weightKg":0.300`), mas o valor é o mesmo.
Conclusão: segue

**C3, o resumo.** Fiz `POST /checkout/summary` com o stub da TransLog respondendo o que cada exemplo diz:
- **Exemplos 1, 3, 4, 5, 6, 7, 8 e 10:** conferem campo a campo. Por exemplo, o 1 deu `409.70 / 40.97 / 33.10 / 5 / 10.24 / 0.00 / 412.07 / 1 / 412.07 / 0.00 / false`, e o 7 deu `1419.60 / 0 / 0 / 6 / 30.00 / -72.48 / 1377.12 / … / 70.98 / true`.
- **Exemplo 2** (`CREDIT_CARD` 6×): `Server Error [500]`. O log mostra `java.lang.ArithmeticException: Invalid operation at BigDecimal.pow … PaymentAdjustmentCalculator.java:27`. 4× e 12× também dão 500. Até 3× funciona: 2× deu `98.70`, 2 parcelas de `49.35`.
- **Exemplo 9:** o CPF `20000000001` já tinha um pedido pago e entregue. O resumo devolveu `200` com `couponDiscount 15.98`, quando o esperado era `422 COUPON_NOT_APPLICABLE`. O mesmo corpo em `POST /orders` dá `422 COUPON_NOT_APPLICABLE`. Ou seja, o resumo e o pedido não batem, que é justamente o problema que o enunciado diz já ter acontecido.

Fronteiras:
- **Motoboy:** com 5,0 kg exatos (`TEN-001`×4 + `MEI-001`×2) o resumo sai com frete 18.00. Com 5,1 kg dá `422 SHIPPING_METHOD_UNAVAILABLE`. Certo.
- **Limite de 30%:** os exemplos 5 e 6 dão 71.91 e 5.97, e o FREESHIPPING no motoboy sobre 19.90 deu 5.97. Certo.
- **"Meio para o par":** no Pix, com total de 90.10 o ajuste deu `-4.50` (4,505 → 4,50), e com total de 90.30 deu `-4.52` (4,515 → 4,52). Certo.
- **Boleto:** com um total do pedido de 999.00, e também de 1000.00, a resposta foi `422 PAYMENT_METHOD_UNAVAILABLE`, quando deveria aceitar. Só passou quando o *valor final* deu 1000.00. O código compara o `finalTotal`, que já inclui a tarifa de 3,49 (`CheckoutService.java:228`), e o enunciado fala no "total do pedido".
- **MINUS50:** 299.70 é recusado. O valor de exatamente 300,00 não se forma com o catálogo (ver "Dúvidas").

Detalhe de forma: os campos zerados de propósito saem como `0` e não `0.00` (`"couponDiscount":0`, `"shippingCost":0`).
Conclusão: em parte

**C4, as recusas.** Todos os casos de `INVALID_ORDER` dão `422 INVALID_ORDER`: carrinho vazio, item sem SKU, SKU repetido, quantidade 0, -1 ou ausente, CPF ou e-mail ausente. Também conferem:
- `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` (`BON-001`, e `MOC-001`×2);
- a ausência de nível, região, entrega ou pagamento, que dá o código certo;
- `INVALID_ZIP_CODE` com `"0131-100"` ou ausente;
- `INVALID_COUPON` com `XYZ` e com `welcome10`;
- `INVALID_INSTALLMENTS` com Pix 2× e com cartão 0× ou 13×;
- os 404 das operações e dos recursos.

Falhas:
- **Valor inexistente num enum:** `"clubTier":"PLATINUM"`, `"region":"MARS"`, `"shippingMethod":"DRONE"` e `"paymentMethod":"CASH"` dão `Server Error [500]`. É um `InvalidFormatException` do Jackson que ninguém trata. O esperado é o 422 da linha 5, 6, 8 ou 13.
- **Cupom vazio:** `"coupon":""` dá `200` e ainda leva o desconto do Pix (`-4.94`). Esperado: `422 INVALID_COUPON` (`CheckoutService.java:176`, `!isEmpty()`).
- **Dois problemas ao mesmo tempo:**
  - Conferem: fora de linha sem nível dá `PRODUCT_DISCONTINUED`; sem região e com CEP ruim dá `INVALID_REGION`; FREESHIPPING na retirada com boleto acima de 1000 dá `COUPON_NOT_APPLICABLE`.
  - Não conferem: motoboy acima de 5 kg com cupom `XYZ` deu `INVALID_COUPON`, quando o esperado é `SHIPPING_METHOD_UNAVAILABLE` (9 vem antes de 11). Motoboy acima de 5 kg sem forma de pagamento deu `INVALID_PAYMENT_METHOD`. Motoboy acima de 5 kg com Pix 2× deu `INVALID_INSTALLMENTS`. A TransLog fora do ar com cupom `XYZ` deu `INVALID_COUPON`, quando o esperado é `503 CARRIER_UNAVAILABLE`. MINUS50 inaplicável sem pagamento deu `INVALID_PAYMENT_METHOD`, e com Pix 2× deu `INVALID_INSTALLMENTS`, quando o esperado é `COUPON_NOT_APPLICABLE` (12 vem antes de 13 e 14). A causa é que a validação confere cupom, pagamento e parcelas antes de calcular o frete e a aplicabilidade do cupom (`CheckoutService.java:176-190`, depois as linhas 198-216).
- **Boleto:** o limite é aplicado ao valor final (C3).
- **JSON malformado** (`{oops`): `500`.
Conclusão: em parte

**C5, a TransLog.** O stub (`/tmp/stub/Stub.java`) registra cada chamada. As consultas saem como o anexo pede, por exemplo `/v2/quote?zip=01310100&weight_g=1800&service=EXP`, `weight_g=750&service=STD`. A tradução confere:
- 3310 centavos viram 33.10;
- 41 h viram 2 dias, 168 h viram 7, 150 h viram 7, 25 h viram 2, 24 h viram 1, 0 h vira 0;
- o Norte soma +3 (49 h deram 6 dias) e o Nordeste +2.

As respostas de falha também conferem:
- `{"error":"ZIP_NOT_SERVED"}` dá `422 SHIPPING_METHOD_UNAVAILABLE` (exemplo 10);
- uma resposta de 3 s dá `503 CARRIER_UNAVAILABLE` em 2014 ms, e uma de 1,5 s é aceita;
- HTTP 500, um corpo que não é JSON e um JSON sem `eta_hours` dão `503`;
- `POST /orders` com a TransLog lenta também dá `503`.

Com o stub derrubado, ECONOMY dá `503`, enquanto COURIER (`200`, frete 18.00) e STORE_PICKUP (resumo `200`, e `POST /orders` registrado) continuam funcionando.

Observação: cada resumo chama a TransLog de 2 a 4 vezes. O log mostrou 4 chamadas no exemplo 1 (`CheckoutService.java:72`, `:81`, `:207`, `:216`). Com a TransLog a 1,5 s por resposta, o resumo levou 3107 ms.
Conclusão: segue

**C6, o ciclo do pedido.** CPF `20000000001`, `CAM-001`×2, SILVER:
- O registro devolve `201` e `AWAITING_PAYMENT`, com o resumo idêntico ao do `/checkout/summary`. O estoque continua em 50.
- Enviar ou entregar antes de pagar dá `409 OPERATION_NOT_ALLOWED`. O pagamento sem `transactionId` ou sem `paidAt` dá `422 INVALID_ORDER`.
- O pagamento dá `PAID`, o estoque cai para 48 e o crédito vai a 3.20.
- Entregar antes de enviar dá 409, enviar sem código dá 422, e o envio dá `SHIPPED` com `trackingCode`. Cancelar depois de enviado dá 409. A entrega dá `DELIVERED`, e cancelar ou enviar depois disso dá 409.
- Os avisos `PAYMENT_APPROVED` e `ORDER_SHIPPED` aparecem em ordem. `POST /notifications/{id}/read` muda o aviso para `READ`, e repetir mantém `READ`.

Cancelamentos:
- Pedido não pago (TEN, GOLD): `CANCELLED`, o estoque continua em 10, o crédito em 0.00, e sai um `ORDER_CANCELLED`.
- Pedido pago (`TEN-001`×2): o estoque vai de 10 para 8 no pagamento e volta a 10 no cancelamento. O crédito vai de 24.99 para 0.00, e sai o aviso.

Prazo de 48 h:
- Com `paidAt` exatamente 48 h depois do `createdAt` gravado (até o microssegundo), o pagamento é aceito.
- Com 48 h e menos de 1 s a mais: `409 ORDER_EXPIRED`, o pedido fica `CANCELLED`, sai o aviso, e o estoque e o crédito não mudam.

O WELCOME10 é aceito para um CPF que só tem pedidos cancelados. Certo.

Falhas:
- **Vazamento de estoque.** Pedido `[FON-001×1, JAQ-001×1]`, com JAQ já zerado por outro pagamento: deu `409 INSUFFICIENT_STOCK` e o pedido ficou `CANCELLED`, mas o FON foi de **5 para 4**. A unidade baixada antes da falha foi gravada pelo `conn.commit()` da linha 129 do `OrderService.java`.
- **Cancelar um pedido já cancelado** dá `200`, e os avisos ficam `["ORDER_CANCELLED","ORDER_CANCELLED"]`. A trava só olha `SHIPPED`/`DELIVERED` (`OrderService.java:249`).
- **`paidAt` fora do formato** (`"ontem"`): `500`, porque o `Instant.parse` fica fora do tratamento (`OrderController.java:45`).
Conclusão: em parte

**C7, o aviso de pagamento repetido.**
- **Em sequência:** o mesmo `TX-D1` na 8081 e depois na 8082 deu respostas byte a byte idênticas. O CAM baixou 3 uma vez só (48 para 45), o crédito ficou em 11.98 e saiu um `PAYMENT_APPROVED` só. Outro `transactionId` com o pedido já `PAID` dá `409 OPERATION_NOT_ALLOWED`.
- **Ao mesmo tempo, mesmo aviso:** fiz três rodadas de 5 disparos divididos entre 8081 e 8082. Em todas, o estoque baixou uma vez (98→96, 96→94, 94→92), o crédito ficou em 1.99, saiu um `PAYMENT_APPROVED` só, as 5 respostas foram idênticas e todas `200`.
- **Ao mesmo tempo, 5 `transactionId` diferentes:** em três rodadas, foi um `200 PAID` e quatro `409 OPERATION_NOT_ALLOWED`, com o estoque baixando uma vez e um aviso só.
- **Falha:** quando a primeira tentativa foi recusada, a repetição não devolve a recusa. Depois de `409 ORDER_EXPIRED`, o mesmo `TX-C2` devolveu `200` e o pedido `CANCELLED`. Depois de `409 INSUFFICIENT_STOCK`, o mesmo `TX-E2` devolveu `200` e o pedido `CANCELLED`. Em 5 disparos simultâneos com `paidAt` expirado, saiu um `409 ORDER_EXPIRED` e quatro `200`. O motivo é que o código devolve o pedido sempre que a transação já está registrada (`OrderService.java:98-100`). O enunciado diz textualmente: "se a primeira foi recusada…, a mesma recusa".
Conclusão: em parte

**C8, a última peça.** Fiz sete rodadas com o serviço nas duas máquinas, criando antes os pedidos concorrentes e depois baixando o estoque com um pagamento até sobrar o número desejado:
- FON com 1 unidade, 2 pedidos;
- TEN com 2 unidades, 6 pedidos;
- CAM com 1, 2 pedidos;
- MEI com 2, 6 pedidos;
- CAM com 1, 4 pedidos;
- MEI com 2, 6 pedidos;
- TEN com 1, 6 pedidos.

Em todas, os `PAID` foram exatamente tantos quantas unidades havia, o estoque terminou em 0 e nunca negativo, e cada perdedor recebeu `409 INSUFFICIENT_STOCK`, ficou `CANCELLED` e ganhou um `ORDER_CANCELLED`. Por exemplo, na rodada do TEN com 6 pedidos e 2 unidades: 2 PAID, 4 `409`, 4 CANCELLED, 4 avisos.

**Falha com pedidos de vários itens.** Dois pedidos `[FON×1, JAQ×1]`, com FON 3 e JAQ 1, foram pagos juntos, um em cada máquina. O vencedor ficou `PAID` e o perdedor recebeu `409 INSUFFICIENT_STOCK`, mas o FON terminou em **1**, e não em 2. O perdedor levou uma unidade de FON consigo, pela mesma causa de C6.
Conclusão: em parte

**C9, os dados não se perdem.** Tirei uma foto antes e depois de derrubar e subir de novo: estoque dos oito SKUs, o pedido `2522c44c…` (status `DELIVERED`, rastreio, `createdAt`, resumo), o saldo `11.98` do CPF `20000000004` e os avisos com status (`READ`/`NEW`). O `diff` não mostrou diferença. O catálogo não foi recriado: os estoques zerados continuaram zerados.
Conclusão: segue

**C10, os testes do próprio agente.** Numa cópia limpa (`/tmp/svc2`), sem as variáveis do banco, rodei `mvn -q verify`: saiu com código 0. Rodam **2 testes**, `CheckoutServiceTest` (1) e `MoneyUtilsTest` (1), e nenhum falha.
Conclusão: segue

## O que mudaria primeiro

1. **Consertar o pagamento.** Ao faltar estoque, desfazer a transação inteira e só então gravar, numa transação nova, o cancelamento, o aviso e a recusa. Isso acaba com o vazamento de estoque em C6 e C8. Além disso, guardar a resposta da primeira tentativa junto com o `transactionId` e devolver essa resposta na repetição, inclusive quando for recusa (C7). As duas falhas mexem com estoque e dinheiro, e são as que o enunciado mais insistiu.
2. **Consertar o resumo.** Trocar o `pow(-n)` por `BigDecimal.ONE.divide(onePlusRate.pow(n), …)`, porque hoje todo parcelamento com juros dá 500. Também calcular a "primeira compra" com o banco no resumo, como já acontece no registro, para que resumo e pedido deem sempre o mesmo resultado.

## Dúvidas

- **Duas máquinas subindo juntas num banco vazio.** Uma das duas cai sempre, com o conflito no `CREATE TABLE IF NOT EXISTS`. O enunciado diz que o banco começa vazio e que há duas máquinas, mas não diz que elas sobem juntas pela primeira vez. Contei como observação e não mudei a conclusão de C1.
- **Fronteira do MINUS50 em 300,00.** O valor exato não se forma com o catálogo: todos os preços terminam em ,90, e chegar a 300,00 exigiria 100 unidades. Testei 299,70 (recusado) e 359,20 (aceito). O código compara com `>= 300.00`.
- **Boleto.** Li "total do pedido" como o total do passo 5, antes do ajuste do pagamento, que é o mesmo termo do texto e dos exemplos. O serviço usa o valor final com a tarifa. Contei como desvio.
- **Cancelar um pedido já cancelado.** O enunciado não diz explicitamente. Li "a operação não é permitida na situação atual" como cobrindo esse caso, porque não há o que cancelar. O serviço aceita e duplica o aviso.
- **TransLog com `price_cents` ou `eta_hours` fracionários** (3311.5 e 24.5). O serviço trunca e dá 33.11 e 1 dia. O anexo trata os dois como inteiros, então não contei.
- **FREESHIPPING para GOLD na econômica ou expressa.** O desconto sai 0, porque o frete já é zero. O enunciado não decide, e não contei.
- **Forma dos números e datas.** Zeros saem como `0` em vez de `0.00`, o `createdAt` sai com fração de segundo, o peso sai com 3 casas, e o pedido e o cliente trazem campos a mais (`customerCpf`, `email` etc.). Os valores estão certos, e tratei isso como forma, não como valor.

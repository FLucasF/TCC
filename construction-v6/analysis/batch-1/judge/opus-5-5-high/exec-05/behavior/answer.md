# comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio, carrega o catálogo e acerta todas as contas. Os dez exemplos do financeiro conferem campo a campo, e também as fronteiras testadas: motoboy com 5,00 kg, teto de 30% do cupom, arredondamento "meio para o par". O ciclo do pedido funciona em sequência, e os dados sobrevivem a um reinício. Os problemas, do mais grave ao menos grave:

1. **O aviso de pagamento repetido não tem nenhuma proteção quando chega em paralelo.** Cinco avisos iguais, divididos entre as duas máquinas, baixaram o estoque cinco vezes, deram o crédito cinco vezes e geraram cinco `PAYMENT_APPROVED`. Com cinco `transactionId` diferentes, os cinco foram aceitos.
2. **O pagamento recusado por falta de estoque perde estoque.** Num pedido com vários itens, os itens baixados antes do que faltou continuam baixados, mesmo com o pedido cancelado. Quando pedidos de vários itens disputam o mesmo estoque, o banco detecta um deadlock e o serviço responde `500`.
3. **O tempo limite de 2 segundos da TransLog não existe.** Uma resposta que demora 8 s é esperada e aceita.
4. **Recusas erradas.** Valores de enum inexistentes dão `400` do Spring em vez do código pedido. O cupom vazio é aceito. A ordem da tabela "Quando dá erro" não é respeitada.
5. **Um aviso repetido de um pagamento que foi recusado devolve `200`** em vez da mesma recusa.

## Item a item

**C1, o serviço sobe.** Compilei uma cópia (`mvn -q package -DskipTests`, 29 s) e subi o jar na 8081 e na 8082 contra o PostgreSQL vazio. As tabelas são criadas pelo Hibernate (`ddl-auto=update`, `src/main/resources/application.properties`) e o catálogo é carregado pelo `config/DataInitializer.java`. A primeira subida levou 4,0 s ("Started OrderServiceApplication in 4.031 seconds"), a segunda máquina 3,8 s. Não houve erro no log, só avisos de configuração.
Conclusão: segue

**C2, o catálogo.** `GET /products/{sku}` para os oito SKUs devolveu exatamente o anexo: nome, preço, peso e estoque. Exemplo: `{"sku":"BON-001",…,"price":59.90,"weightKg":0.15,"stock":0,"discontinued":false}`. O `REL-001` vem com `"discontinued":true`, e um SKU inexistente dá `404 {"error":"PRODUCT_NOT_FOUND"}`. Vender o `REL-001` é recusado com `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Montei cada exemplo com o stub da TransLog respondendo o que o exemplo diz:
- **Os 1 a 7** deram exatamente os valores do financeiro. Ex.: o 7 deu `1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / 1× 1377.12 / 70.98 / gift true`; o 2 deu `… 30.55 / 462.00 / 6× 77.00 / 8.19`.
- **O 8 e o 10** deram `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **O 9:** registrei e paguei um pedido do mesmo CPF; o resumo com `WELCOME10` deu `422 COUPON_NOT_APPLICABLE`.
- **O `weight_g` enviado à TransLog** estava certo: 1800, 900, 3600 etc., conferido no log do stub.

Fronteiras:
- **Motoboy:** 50 meias, exatamente 5,00 kg, é aceito (frete 18.00, prazo 0); 51 meias, 5,10 kg, dá `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Teto de 30%:** `BUY3PAY2` com 3 meias (59,70) dá desconto 17.91 e não 19.90.
- **`MINUS50`:** 299,70 é recusado e 319,60 dá 50.00. O valor exato de 300,00 não dá para montar com o catálogo (ver Dúvidas).
- **"Meio para o par":** 15 meias, GOLD, Sudeste (298,50) dão seguro 2.98 (2,985) e crédito 14.92 (14,925). Com meio para cima seriam 2,99 e 14,93.
- **Resumo e pedido:** o resumo de `POST /orders` é idêntico ao de `POST /checkout/summary` com os mesmos dados.

Conclusão: segue

**C4, as recusas.** Os casos isolados:
- **Corretos:** todos os `INVALID_ORDER` (carrinho vazio, sem SKU, SKU repetido, quantidade 0/−1/ausente, sem CPF, sem `customer`), `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` (`BON-001` e `MOC-001` ×2), os campos ausentes (tier, região, CEP, entrega, pagamento), o CEP com 7 dígitos ou com traço, `INVALID_INSTALLMENTS` (Pix 2×, boleto 2×, cartão 0× e 13×) e `PAYMENT_METHOD_UNAVAILABLE` (boleto em 1029,70).
- **Errados, `400` do Spring:** valores que não existem, como `"clubTier":"PLATINUM"`, `"region":"MARS"`, `"shippingMethod":"DRONE"`, `"coupon":"FAKE"`, `"coupon":"welcome10"` e `"paymentMethod":"BITCOIN"`. Voltam `400 {"timestamp":…,"status":400,"error":"Bad Request",…}` em vez de `422` com o código. A causa: os campos do DTO são enums (`dto/CheckoutRequest.java:11-16`) e só há handler para `BusinessException` (`exception/GlobalExceptionHandler.java:11`).
- **Cupom vazio:** `"coupon":""` volta `200` com o resumo, em vez de `422 INVALID_COUPON`.

Com dois problemas ao mesmo tempo, a ordem falha porque a `validateRequest` (`service/CheckoutCalculator.java:35`, linhas 104-131) confere tier, região, CEP, entrega, cupom, pagamento e parcelas antes dos produtos (linha 37):

| combinação | esperado | veio |
|---|---|---|
| SKU inexistente + sem tier | `UNKNOWN_PRODUCT` | `INVALID_CLUB_TIER` |
| sem estoque + CEP inválido | `INSUFFICIENT_STOCK` | `INVALID_ZIP_CODE` |
| motoboy 5,1 kg + sem pagamento | `SHIPPING_METHOD_UNAVAILABLE` | `INVALID_PAYMENT_METHOD` |
| retirada com `FREESHIPPING` + Pix 2× | `COUPON_NOT_APPLICABLE` | `INVALID_INSTALLMENTS` |
| CEP não atendido + cartão 13× | `SHIPPING_METHOD_UNAVAILABLE` | `INVALID_INSTALLMENTS` |

Entre fora de linha e sem estoque, o resultado depende da ordem de um `HashMap` (`CheckoutCalculator.java:177-187`). Com `REL-001` junto de `CAM-001`×100, `MEI-001`×101 ou `BON-001` veio `PRODUCT_DISCONTINUED`. Junto de `TEN-001`×11, `FON-001`×6, `JAQ-001`×4 ou `MOC-001`×2 veio `INSUFFICIENT_STOCK`.

Nas operações de pedido, `POST /orders/999999/payment` e `/shipment` com `{}` dão `422 INVALID_ORDER` em vez de `404 ORDER_NOT_FOUND`: os campos são conferidos antes de buscar o pedido (`service/OrderService.java:83` antes da 87). Os demais 404 estão certos: pedido, cliente, avisos de CPF desconhecido e aviso.
Conclusão: em parte

**C5, a TransLog.** O que funciona:
- **Tradução:** `1999c/24h` → 19.99 e 1 dia; `5c/25h` → 0.05 e 2 dias; `0/0` → 0.00 e 0; `123456c` → 1234.56.
- **Dias a mais:** Norte com 48 h → 5; Nordeste com 49 h → 5.
- **CEP não atendido:** `ZIP_NOT_SERVED` → `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Fora do ar:** outro `error`, HTTP 500, HTTP 404, corpo que não é JSON e `{}` dão `503 CARRIER_UNAVAILABLE`. Com o stub derrubado, a econômica dá `503` em 13 ms.
- **Motoboy e retirada sem a TransLog:** o resumo de motoboy e de retirada e o `POST /orders` de motoboy (`201`) funcionam.

O que falha:
- **Tempo limite:** com resposta em 3 s veio `200` após 3061 ms; com 8 s, `200` após 8054 ms. Esperava `503` em cerca de 2 s. O `timeoutSeconds` é recebido mas nunca usado no `RestClient` (`service/TransLogService.java:20-24`).
- **Valores fracionários:** são truncados, porque os campos são `Integer` (linhas 110 e 113). `price_cents: 1235.5` deu frete 12.35, quando meio para o par daria 12.36. `eta_hours: 48.5` deu 2 dias, quando para cima seriam 3.

Conclusão: em parte

**C6, o ciclo do pedido.** Testei com CPF novo, `CAM-001`×2, SILVER, Pix:
- **Registro:** o estoque fica em 49 → 49 e o cliente nasce com `creditBalance 0.00`.
- **Pagamento:** estoque 49 → 47, crédito 3.20, aviso `PAYMENT_APPROVED`.
- **Envio e entrega:** envio sem `trackingCode` dá `422 INVALID_ORDER`; com código dá `SHIPPED`, `trackingCode` gravado e aviso `ORDER_SHIPPED`; a entrega dá `DELIVERED`.
- **Fora de ordem:** enviar ou entregar antes de pagar, entregar antes de enviar, pagar de novo com outro tx, cancelar enviado, cancelar entregue, enviar de novo e entregar de novo dão todos `409 OPERATION_NOT_ALLOWED`.
- **Cancelar não pago:** `CANCELLED`, estoque e crédito inalterados, aviso `ORDER_CANCELLED`, e pagar depois dá `409`.
- **Cancelar pago:** estoque 45 → 47, crédito 6.40 → 3.20, aviso.
- **Prazo de 48 h:** `createdAt` 12:41:35.69 com `paidAt` 2026-10-20T12:41:35Z é aceito. Com `paidAt` às …:36Z (48 h e 0,25 s) vem `409 ORDER_EXPIRED`, o pedido vai a `CANCELLED`, sai aviso, e estoque e crédito não mudam. `paidAt` em 2030 também dá `ORDER_EXPIRED`.
- **`WELCOME10`:** volta a valer depois de o único pedido do CPF ser cancelado.
- **Avisos:** vêm do mais antigo para o mais novo. `POST /notifications/7/read` três vezes, numa e noutra máquina, devolve sempre `READ`.

Conclusão: segue

**C7, o aviso de pagamento repetido.** Um depois do outro:
- **Pagamento aceito:** o mesmo `TX-R1` três vezes, alternando as máquinas, devolveu respostas idênticas byte a byte. `FON-001` baixou de 5 para 4 uma vez só, e saíram um crédito (10.00) e um `PAYMENT_APPROVED`.
- **Outro tx em pedido pago:** `409 OPERATION_NOT_ALLOWED`.
- **Pagamento recusado:** repetir o aviso de um pedido expirado devolveu `200` com o pedido `CANCELLED`, em vez de `409 ORDER_EXPIRED`. O mesmo vale para o recusado por falta de estoque: `200` em vez de `409 INSUFFICIENT_STOCK`. O atalho de `OrderService.java:90-92` devolve sempre o pedido.

Ao mesmo tempo:
- **Mesmo tx:** cinco avisos iguais, divididos entre 8081 e 8082, em 6 rodadas com `MEI-001`×3 e GOLD. Em todas, o estoque baixou 15 em vez de 3 (99 → 84, 84 → 69, …), o crédito ficou em 14.90 em vez de 2.98 e saíram 5 `PAYMENT_APPROVED`. As cinco respostas foram `200`.
- **Tx diferentes:** cinco avisos com `transactionId` diferentes, em 4 rodadas com `CAM-001`×1. Em todas, os cinco foram aceitos (`200`), o estoque baixou 5 (46 → 41, …), o crédito ficou em 20.00 em vez de 4.00 e saíram 5 avisos.

A causa: o pedido é lido sem trava e sem versão (`OrderService.java:87`; `entity/Order.java` não tem `@Version`), então todas as transações veem `AWAITING_PAYMENT`.
Conclusão: não segue

**C8, a última peça.** Disputa com pedidos de um item só, sempre tudo junto e dividido entre as duas máquinas:
- **Última unidade:** com `JAQ-001` em 1, dois pedidos em 5 rodadas.
- **Seis para duas:** com `FON-001` em 2, seis pedidos em 5 rodadas.
- **Resultado:** em todas as rodadas passou exatamente o número de unidades. O estoque foi a 0 e nunca ficou negativo. Cada perdedor recebeu `409 INSUFFICIENT_STOCK`, ficou `CANCELLED` e ganhou o aviso `ORDER_CANCELLED`.

Com pedidos de vários itens, as duas coisas falham:
- **Perdedor perde estoque:** o pedido X `[CAM-001, MOC-001]` foi pago depois de outro pedido levar a última mochila. Veio `409 INSUFFICIENT_STOCK` e o X ficou `CANCELLED`, mas `CAM-001` caiu de 47 para 46 e não voltou. O `@Transactional(noRollbackFor = BusinessException.class)` (`OrderService.java:81`) faz o commit da baixa parcial feita em `reserveStock` (linha 210).
- **Deadlock:** seis pedidos `[CAM, FON]` e `[FON, CAM]` disputando 2 fones, em 3 rodadas. Em cada uma, 2 passaram e não houve estoque negativo, mas 3 ou 4 pedidos receberam `HTTP 500`. O log mostra `PSQLException: ERROR: deadlock detected` e `CannotAcquireLockException`. Esses pedidos ficaram `AWAITING_PAYMENT`, sem `INSUFFICIENT_STOCK`, sem cancelamento e sem aviso.

Conclusão: em parte

**C9, os dados não se perdem.** Tirei uma foto antes de derrubar as duas máquinas: o estoque dos seis produtos, o pedido 3 (`DELIVERED`), o saldo de dois clientes e a lista de avisos com um `READ`. Subi as duas de novo, o catálogo não foi recarregado por cima, e a foto saiu idêntica (`diff` vazio), consultada também pela 8082.
Conclusão: segue

**C10, os testes do próprio agente.** `mvn -q verify` numa cópia limpa terminou com `exit 0`. Rodaram 14 testes, sem falha nem erro: `CheckoutCalculatorTest` 7, `OrderIntegrationTest` 6, `OrderServiceApplicationTests` 1. O banco de teste é um H2 em memória.
Conclusão: segue

## O que mudaria primeiro

1. **Tornar o pagamento atômico por pedido.** Ler o pedido com `SELECT … FOR UPDATE` (ou `@Version`), guardar a resposta da primeira vez para o par pedido + `transactionId` (inclusive as recusas) e travar os produtos sempre na mesma ordem (por SKU). Isso resolve o C7, que hoje multiplica estoque, crédito e avisos, e os deadlocks do C8.
2. **Separar a recusa por falta de estoque da baixa.** Conferir todos os itens antes de baixar qualquer um, ou fazer o rollback da baixa e gravar o cancelamento numa transação à parte. Hoje um pagamento recusado some com estoque de verdade. Em seguida: o tempo limite no `RestClient` e um handler que transforme os enums inválidos e o cupom vazio no `422` com o código da tabela, na ordem dela.

## Dúvidas

- **Brinde do GOLD no motoboy:** GOLD no motoboy com produtos acima de R$ 500 (`JAQ-001`×2, 779,80) sai com `gift:false` (`CheckoutCalculator.java:416`). Li a frase do motoboy como restrita ao frete, então o brinde deveria sair, mas o texto admite as duas leituras. Não contei na conclusão.
- **Cancelar duas vezes:** cancelar um pedido já cancelado devolve `200` sem novo aviso (`OrderService.java:178`). O enunciado não diz se isso é `409` ou se é aceito sem efeito.
- **Fronteiras impossíveis:** as fronteiras exatas de R$ 300,00 (`MINUS50`) e R$ 500,00 (brinde) não dá para montar com o catálogo, porque todos os preços terminam em ,90. Testei os dois lados mais próximos.
- **48 horas exatas:** o `createdAt` tem frações de segundo e o `paidAt` não, então a fronteira só foi testada a menos de 1 s de cada lado. O `createdAt` da resposta do `POST /orders` vem com nanossegundos e o das consultas seguintes com microssegundos (…`.158249724Z` vs …`.158250Z`). Nenhum dos dois está no formato do anexo, sem frações.
- **Versões:** o `pom.xml` usa Spring Boot 3.3.5, e não o 4.1.1 do enunciado. Não afeta nada do que testei.
- **Valores fracionários da TransLog:** o anexo só mostra inteiros. Li o arredondamento geral do dinheiro e o "arredondado para cima" do prazo como valendo também para eles.

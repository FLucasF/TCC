# comportamento

## Visão geral

Não encontrei nenhuma divergência em relação ao `prompt.md` em tudo o que testei. O serviço sobe em menos de um segundo com o banco vazio. Ele cria as tabelas e carrega o catálogo sozinho. Os nove exemplos do financeiro que dá para conferir só pelo resumo batem campo a campo. O exemplo 9 também bate, depois de criar um pedido pago para o CPF. As fronteiras de peso, cupom, limite de 30% e arredondamento "meio para o par" conferem. Todas as linhas da tabela "Quando dá erro" devolvem o status e o código pedidos, e a ordem vale nos pedidos com dois problemas. O mapeamento da TransLog está certo: centavos para reais, horas para dias arredondando para cima, CEP não atendido e o tempo limite de 2 s. O motoboy e a retirada continuam funcionando com a TransLog fora do ar. Rodei a parte concorrente com duas máquinas e o mesmo banco: avisos repetidos ao mesmo tempo, avisos com `transactionId` diferentes e disputa pela última peça. Em 6 rodadas de cada cenário de estoque, nunca vendeu além do que havia, nunca deixou estoque negativo e nunca deu crédito ou aviso em dobro. Os dados sobrevivem a um reinício e o `mvn verify` passa com 10 testes. Não há problema sério a apontar. Os poucos pontos que o enunciado não decide estão em "Dúvidas".

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/run`, rodei `mvn -q package -DskipTests` e subi com `PORT=8081 TRANSLOG_URL=http://localhost:9090 java -jar target/orders-service.jar`, com o banco vazio. O log mostra o pool Hikari conectado e `Started Server ... @731ms`, e o `GET /products/CAM-001` respondeu 200 logo em seguida. A segunda máquina, na 8082 com o mesmo banco, subiu em `@847ms` sem conflito. A criação das tabelas fica protegida por `pg_advisory_lock` (`src/main/java/shop/JdbcStore.java:104-108`), e a carga do catálogo usa `ON CONFLICT DO NOTHING` (`JdbcStore.java:114`). No reinício (`@708ms`), os estoques alterados não voltaram aos valores iniciais.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs do anexo. Todos vieram com o nome, o preço, o peso, o estoque e o `discontinued` do anexo, por exemplo `{"sku":"REL-001","name":"Relógio digital","price":129.90,"weightKg":0.200,"stock":8,"discontinued":true}` e `BON-001` com `stock: 0`. Um SKU inexistente devolve `404 {"error":"PRODUCT_NOT_FOUND"}`. Pedir o REL-001 no resumo dá `422 PRODUCT_DISCONTINUED`. O peso vem com três casas (`0.300`); ver "Dúvidas".
Conclusão: segue

**C3, o resumo.** Meu stub da TransLog respondia o que cada exemplo diz, e todos os pedidos usaram o CEP `01310100`.
- **Exemplos.** Os exemplos 1 a 7 deram exatamente os valores do enunciado, campo a campo. Por exemplo, o E2 veio `409.70 / 0.00 / 15.60 / 7 / 6.15 / 30.55 / 462.00 / 6 / 77.00 / 8.19 / false`, e o E7 veio `1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / 1 / 1377.12 / 70.98 / true`. O E8 e o E10 deram `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`. O E9 deu `422 COUPON_NOT_APPLICABLE` para o CPF 40000000001, que já tinha um pedido pago e entregue. O log do stub confirma as chamadas certas à TransLog, por exemplo `zip=01310100&weight_g=1800&service=EXP` no E1 e `weight_g=100&service=STD` no E6.
- **Fronteira do motoboy.** MEI-001 × 50 pesa exatamente 5,000 kg e veio com `200`, frete 18.00. Com × 51 (5,1 kg) veio `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Fronteira do MINUS50.** Com produtos de 299,10 veio `422 COUPON_NOT_APPLICABLE`; com CAM-001 × 4 (319,60) veio desconto de 50.00. Não dá para montar exatamente 300,00 com este catálogo, porque todo preço termina em ,90 (ver "Dúvidas").
- **Limite de 30%.** O E5 (BUY3PAY2 daria 79,90 e fica em 71,91) e o E6 (FREESHIPPING daria 22,10 e fica em 5,97) caem no limite e batem.
- **Meio para o par.** Com MEI-001 × 15 (298,50), região SOUTH e clube GOLD, o seguro de 1% dá 2,985 e virou `2.98` (o arredondamento comum daria 2,99). O crédito de 5% dá 14,925 e virou `14.92`. O Pix sobre 301,48 dá 15,074 e virou ajuste `-15.07`.
- **Outros casos.** O GOLD no motoboy paga os 18,00, e o crédito 3,995 vira `4.00`. FREESHIPPING com GOLD dá cupom 0,00. BUY3PAY2 sem nenhum trio dá desconto 0,00 e o Pix não desconta. O cartão em 4× deu `21.19 × 4` e em 12× deu `7.63 × 12`, que conferem com a tabela Price. Por fim, o resumo de `POST /checkout/summary` e o `summary` do pedido de `POST /orders` com os mesmos dados saíram idênticos (comparei os dois JSONs com `jq -S`).
Conclusão: segue

**C4, as recusas.** Testei cada linha da tabela, em `POST /checkout/summary` e também em `POST /orders`.
- **1:** carrinho vazio, `items` ausente, item sem SKU, SKU repetido, quantidade 0, -1 ou ausente, CPF, e-mail ou `customer` ausentes, e corpo que não é JSON. Todos deram `422 INVALID_ORDER`.
- **2 a 4:** `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, e `INSUFFICIENT_STOCK` para BON-001 × 1 e MOC-001 × 2, tudo com 422.
- **5 a 8:** nível, região e forma de entrega inválidos ou ausentes, e CEP com 7 dígitos, com traço ou ausente. Cada um deu seu código com 422.
- **9 e 10:** `SHIPPING_METHOD_UNAVAILABLE` 422 e `CARRIER_UNAVAILABLE` 503.
- **11:** `INVALID_COUPON` para `BOGUS`, para `welcome10` em minúsculas e para o texto vazio `""`.
- **12 a 15:** `COUPON_NOT_APPLICABLE`, `INVALID_PAYMENT_METHOD` (inválida ou ausente), `INVALID_INSTALLMENTS` (Pix 2×, boleto 2×, cartão 0× e 13×) e `PAYMENT_METHOD_UNAVAILABLE` (boleto em JAQ-001 × 3, total acima de 1.000). Um boleto de 972,48 foi aceito.
- **Dois problemas ao mesmo tempo:**
  - fora de linha + nível inválido → `PRODUCT_DISCONTINUED`;
  - fora de linha + SKU desconhecido → `UNKNOWN_PRODUCT`;
  - BON sem estoque antes do REL fora de linha no carrinho → `PRODUCT_DISCONTINUED`, ou seja, a regra 3 vale para o carrinho inteiro antes da 4;
  - região + CEP inválidos → `INVALID_REGION`;
  - CEP + entrega inválidos → `INVALID_ZIP_CODE`;
  - motoboy acima de 5 kg + cupom inválido → `SHIPPING_METHOD_UNAVAILABLE`;
  - TransLog com 500 + cupom inválido → `503 CARRIER_UNAVAILABLE`;
  - cupom inválido + pagamento inválido → `INVALID_COUPON`;
  - boleto 2× + total acima de 1.000 → `INVALID_INSTALLMENTS`.

  Nas operações de pedido, o pedido inexistente dá `404 ORDER_NOT_FOUND` em consulta, pagamento, envio, entrega e cancelamento, mesmo com o corpo vazio, o que mostra que a regra 404 vem antes do 422.
Conclusão: segue

**C5, a TransLog.** O stub respondia conforme o CEP.
- `{"price_cents":12345,"eta_hours":25}` em NORTH/ECONOMY deu frete `123.45` e prazo `5` (2 dias + 3).
- 48 h em NORTHEAST deu prazo `4` (2 + 2), e 49 h no E7 deu 3 + 3 = 6.
- `0 c / 0 h` deu frete `0.00` e prazo `0`.
- Um campo extra na resposta é ignorado.
- `{"error":"ZIP_NOT_SERVED"}` deu `422 SHIPPING_METHOD_UNAVAILABLE`.
- Resposta atrasada 3 s deu `503 CARRIER_UNAVAILABLE` em **2,005 s**. Atrasada 1,5 s foi aceita em 1,51 s.
- HTTP 500, corpo HTML, `{"error":"OTHER"}`, `price_cents` em texto e `price_cents: 3310.5` deram `503 CARRIER_UNAVAILABLE`.

Subi uma terceira máquina com `TRANSLOG_URL=http://localhost:9999`, onde nada responde. Nela, a ECONOMY e a EXPRESS deram `503 CARRIER_UNAVAILABLE`. O COURIER deu `200` com frete 18.00 e prazo 0, a STORE_PICKUP deu `200` com prazo 1, e `POST /orders` com motoboy registrou o pedido (`201`).
Conclusão: segue

**C6, o ciclo do pedido.**
- **Pedido pago, enviado e entregue** (CPF 40000000001, SILVER, CAM × 2). O registro deu `201 AWAITING_PAYMENT`, o estoque continuou em 50 e o cliente foi criado com `creditBalance: 0.00`. Enviar ou entregar antes de pagar deu `409 OPERATION_NOT_ALLOWED`. Pagar sem `paidAt`, sem `transactionId` ou com `paidAt` inválido deu `422 INVALID_ORDER`. O pagamento deu `200 PAID`, o estoque foi de 50 para 48 e o crédito para 3.20. Entregar um pedido pago deu 409. Enviar sem `trackingCode` deu 422, e com ele deu `SHIPPED` com o código. Enviar de novo deu 409, cancelar o enviado deu 409, entregar deu `DELIVERED`, e entregar ou cancelar de novo deu 409. Os avisos foram `PAYMENT_APPROVED` e `ORDER_SHIPPED`, ambos `NEW`.
- **Cancelar pedido não pago.** Deu `CANCELLED`, o TEN-001 ficou em 10 → 10, o crédito ficou em 0.00 e saiu o aviso `ORDER_CANCELLED`. Cancelar de novo, ou pagar o cancelado com outro `transactionId`, deu 409. O WELCOME10 continuou valendo para esse CPF, porque o pedido cancelado não conta como compra.
- **Cancelar pedido pago.** TEN × 2: o estoque foi 10 → 8 → 10, o crédito 0.00 → 10.00 → 0.00, e saíram os avisos `PAYMENT_APPROVED` e `ORDER_CANCELLED`.
- **Prazo de 48 horas.** `paidAt` igual a `createdAt + 48h` foi aceito (`PAID`, MEI 100 → 99). Com `+48h 1s` veio `409 ORDER_EXPIRED`: o pedido foi para `CANCELLED`, o estoque e o crédito não mudaram e saiu `ORDER_CANCELLED`.
- **Avisos.** `POST /notifications/3/read` deu `READ`, e repetir manteve `READ`. Aviso inexistente (ou id `abc`) deu `404 NOTIFICATION_NOT_FOUND`. O saldo e os avisos de um CPF desconhecido deram `404 CUSTOMER_NOT_FOUND`. A lista de avisos veio em ordem crescente de id.

Não consegui testar que um pedido parado há mais de 48 h continua `AWAITING_PAYMENT`, porque não dá para mexer no relógio do serviço. O código não tem nada agendado que cancele pedidos.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Um depois do outro.** Mandei o mesmo aviso primeiro na 8081 e depois na 8082. As duas respostas saíram byte a byte iguais (`200`, pedido `PAID`). O FON-001 baixou uma vez só (5 → 4) e o crédito ficou em 10.00. Outro `transactionId` no pedido já pago deu `409 OPERATION_NOT_ALLOWED`. O mesmo aviso repetido depois do envio deu `200` com o pedido como está (`SHIPPED`), sem novo aviso.
- **Recusas repetidas.** Repetir o aviso recusado por estoque deu de novo `409 INSUFFICIENT_STOCK`, mesmo depois de eu devolver uma unidade, e não baixou nada. Repetir o aviso expirado, mesmo com um `paidAt` dentro do prazo, deu de novo `409 ORDER_EXPIRED`.
- **Ao mesmo tempo, mesmo aviso.** Em 5 rodadas, disparei 5 avisos iguais juntos, divididos entre 8081 e 8082 (CAM × 3, GOLD). Todas as rodadas deram 5 × `200` com o corpo idêntico, CAM baixando exatamente 3, crédito `11.98` uma vez e um único `PAYMENT_APPROVED`. Cinco avisos expirados iguais e juntos deram 5 × `409 ORDER_EXPIRED` e um único `ORDER_CANCELLED`.
- **Ao mesmo tempo, `transactionId` diferentes.** Em 5 rodadas de 5 avisos, todas deram 1 × `200` e 4 × `409 OPERATION_NOT_ALLOWED`, com MEI baixando 2, crédito `1.99` uma vez e um único `PAYMENT_APPROVED`.
Conclusão: segue

**C8, a última peça.** Usei as duas máquinas e o mesmo banco, alternando os pagamentos entre 8081 e 8082. Depois de cada rodada, cancelei os vencedores para devolver o estoque.
- **Dois pedidos para a última unidade** (MOC-001, estoque 1), 6 rodadas. Todas tiveram 1 aceito e o estoque terminou em 0. O perdedor recebeu `409 {"error":"INSUFFICIENT_STOCK"}`, o pedido dele ficou `CANCELLED` e o aviso dele foi exatamente `["ORDER_CANCELLED"]`.
- **Seis pedidos para duas unidades** (JAQ-001, estoque 2), 6 rodadas. Todas tiveram 2 aceitos, 4 perdedores corretos e o estoque terminou em 0.
- **Pedidos com dois produtos em ordens trocadas** (FON + JAQ e JAQ + FON), 4 pedidos com JAQ = 2, 3 rodadas. Todas deram 2 × 200 e 2 × 409, com FON 4 → 2 e JAQ 2 → 0, sem deadlock.

Nenhum `Exception` ou `INTERNAL_ERROR` apareceu nos logs das duas máquinas.
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar, tirei uma foto do pedido entregue, do estoque de seis produtos, e do saldo e dos avisos de três clientes. Derrubei as duas máquinas, subi de novo e comparei: os três arquivos ficaram idênticos (`cmp`). O pedido continuou `DELIVERED` com `BR123456789`, o crédito em 3.20, e o aviso que marquei como lido continuou `READ`.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` na cópia terminou com `exit=0`. Rodaram 10 testes: `CheckoutExamplesTest` 2, `OrderLifecycleTest` 6 e `TransLogGatewayTest` 2, sem falhas nem erros.
Conclusão: segue

## O que mudaria primeiro

Nada que o enunciado peça ficou faltando. Se fosse mudar algo, seria proteger o pagamento contra `paidAt` anterior ao registro do pedido: hoje um `paidAt` de 2020 num pedido criado em 2026 é aceito como `PAID`. Um aviso desses é quase certamente um erro do banco, e aceitá-lo baixa o estoque e dá crédito. O enunciado, porém, não fala desse caso.

## Dúvidas

- **`paidAt` anterior ao registro.** O enunciado só fala do limite de 48 horas para trás. O serviço aceita um `paidAt` de 2020 num pedido criado em 2026. Não contei isso contra.
- **TransLog com `price_cents` fracionário (`3310.5`) ou em texto.** O serviço trata como "qualquer outra coisa" e devolve `503 CARRIER_UNAVAILABLE`. O anexo diz que o valor é em centavos, um número inteiro, então considerei essa leitura defensável.
- **Fronteiras exatas que não dá para montar.** Com este catálogo não se chega a produtos de exatamente 300,00 (MINUS50), 500,00 (brinde) nem a um total de exatamente 1.000,00 (boleto). Todo preço termina em ,90, e a soma de dez ou vinte itens não fecha nesses valores. Testei os dois lados de cada limite e li no código que as comparações são `< 300` (`Coupons.java:68`), `> 500` (`ClubTier.java:32`) e `<= 1000` (`Payments.java:57`), que é o que o texto diz.
- **`"coupon": null`** é tratado como cupom ausente: não dá `INVALID_COUPON` e o Pix dá desconto. O texto vazio `""` é recusado, como pede o enunciado.
- **Peso com três casas.** O `weightKg` vem com três casas (`0.300`). O enunciado fixa duas casas só para valores em dinheiro, então não contei contra.
- **Pedido parado há mais de 48 horas.** Não consegui testar pelo serviço que ele continua `AWAITING_PAYMENT` (C6), porque o relógio não é controlável. Me apoiei na ausência de qualquer cancelamento agendado no código.

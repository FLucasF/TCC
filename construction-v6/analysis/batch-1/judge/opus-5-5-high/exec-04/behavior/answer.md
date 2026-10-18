# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Subiu em cerca de 3 s com o banco vazio, criou as tabelas e o catálogo sozinho e deu os dez exemplos do financeiro campo a campo. Também acertou as fronteiras de cálculo, as 15 recusas do resumo na ordem pedida e a tradução da TransLog, incluindo o tempo limite de 2 s. O ciclo do pedido funcionou inteiro, e os dados continuaram lá depois de reiniciar.

O ponto mais forte é a concorrência com duas máquinas e o mesmo banco. Isso inclui o aviso repetido (o mesmo `transactionId` ou ids diferentes) e a disputa pela última peça, com até seis pedidos por rodada e itens em ordens trocadas, num total de 45 rodadas. Em todas, o estoque baixou uma vez só por pedido aceito e nunca ficou negativo. Os perdedores receberam `409 INSUFFICIENT_STOCK`, ficaram `CANCELLED` e ganharam o aviso `ORDER_CANCELLED`. Os logs não mostraram nenhum deadlock.

Não achei falha em nenhum item. Os únicos pontos discutíveis estão em "Dúvidas": como tratar um `price_cents` fracionário vindo da TransLog, e o limite do MINUS50, que o catálogo não deixa testar exatamente.

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/run/app`, rodei `mvn -q package -DskipTests` (7 s) e subi com `java -jar … --server.port=8081`, com `TRANSLOG_URL` apontando para o meu stub. O log mostra `Current version of schema "public": << Empty Schema >>`, depois `Successfully applied 1 migration … v1`, depois `Started PedidosApplication in 2.568 seconds (process running for 2.986)`. A segunda máquina, na 8082 e com o mesmo banco, mostrou `Schema "public" is up to date` e subiu em 2,66 s. Não testei as duas subindo juntas sobre o banco vazio, porque o banco não pode ser zerado.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Os oito vieram iguais ao anexo, por exemplo `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`. O `REL-001` veio com `"discontinued":true`, e o `BON-001` com `"stock":0`. Um SKU inexistente devolveu `404 {"error":"PRODUCT_NOT_FOUND"}`. Pedir o `REL-001` devolveu `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Mandei `POST /checkout/summary` com o stub respondendo o que cada exemplo diz:
- **Exemplos 1 a 7:** todos os campos bateram. Exemplo 1: 409.70 / 40.97 / 33.10 / 5 / 10.24 / 0.00 / 412.07 / 1× 412.07 / 0.00 / false. Exemplo 2: … 30.55 / 462.00 / 6× 77.00 / 8.19. Exemplo 7: 1419.60 / 0.00 / 0.00 / 6 / 30.00 / −72.48 / 1377.12 / 70.98 / true.
- **Exemplos 8 e 10:** `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Exemplo 9:** registrei e paguei um pedido para o CPF 20000000009; depois disso o WELCOME10 deu `422 COUPON_NOT_APPLICABLE`. Para comparar, um CPF que só tinha um pedido cancelado teve o WELCOME10 aceito (15.98).
- **O resumo é o mesmo do pedido:** registrei os exemplos 1 e 2. O `summary` do `POST /orders`, feito na 8082, e o do `GET /orders/{id}`, feito na 8081, são idênticos ao do resumo prévio.
- **Peso do motoboy no limite:** `MEI-001`×50 (5,00 kg) e `TEN-001`×4 + `MEI-001`×2 (5,00 kg) foram aceitos com frete 18.00. `MEI-001`×51 (5,10 kg) deu `SHIPPING_METHOD_UNAVAILABLE`.
- **Meio para o par:** `MEI-001`×15, Sudeste, dá 298,50 × 1% = 2,985, e o seguro veio **2.98**. No Pix sobre um total de 100,50, 5% = 5,025, e o ajuste veio **−5.02**.
- **Teto de 30%:** BUY3PAY2 com `CAM`×3 + `MEI`×3 daria 99,80; veio **89.82**, que é 30% de 299,40. Os exemplos 5 e 6 também mostram o teto.
- **Limite de cupom:** o MINUS50 recusou 299,70 e aceitou 318,40 com 50.00 (o valor exato de 300,00 não é possível; ver Dúvidas).
- **Limite do boleto:** com total de exatamente 1000,00, o boleto foi aceito (final 1003.49). Com 1000,01, veio `PAYMENT_METHOD_UNAVAILABLE`.
- **Outros casos:** cartão em 12× deu 23.85 por parcela e em 4× deu 66.27, os dois conferidos à mão pela tabela Price. Cupom com desconto zero (BUY3PAY2 sem trinca) tirou o desconto do Pix. GOLD no motoboy pagou os 18.00. GOLD com FREESHIPPING deu frete 0 e cupom 0. Sem `installments`, o pedido foi tratado como 1×.

Conclusão: segue

**C4, as recusas.** Testei 50 casos e todos voltaram com o status e o código esperados:
- **Ordem 1, `INVALID_ORDER` 422:** carrinho vazio ou ausente; item sem SKU ou com SKU vazio; SKU repetido; quantidade 0, −1 ou ausente; CPF, e-mail ou `customer` ausente; JSON malformado; corpo vazio.
- **Ordens 2 a 15:** cada código apareceu na sua situação. Por exemplo: `UNKNOWN_PRODUCT`; `PRODUCT_DISCONTINUED`; `INSUFFICIENT_STOCK` para `BON-001`×1 e `MOC-001`×2; nível `PLATINUM`, `gold` em minúsculas ou ausente; região `WEST`; CEP com 7 dígitos, com traço, com letra ou numérico; `DRONE`; cupom `""`, `welcome10` ou `BLACKFRIDAY`; `CASH`; Pix 2×, boleto 2×, cartão 0× e 13×; boleto acima de R$ 1.000.
- **Dois problemas juntos:** a ordem do enunciado sempre valeu. Quantidade 0 com SKU inexistente deu `INVALID_ORDER`. Produto fora de linha com SKU inexistente deu `UNKNOWN_PRODUCT`. Sem estoque com fora de linha deu `PRODUCT_DISCONTINUED`. Sem estoque com nível, região, entrega e pagamento inválidos deu `INSUFFICIENT_STOCK`. Região e nível inválidos deram `INVALID_CLUB_TIER`. CEP e entrega inválidos deram `INVALID_ZIP_CODE`. Motoboy acima de 5 kg com cupom inválido deu `SHIPPING_METHOD_UNAVAILABLE`. TransLog fora do ar com cupom inválido deu `503 CARRIER_UNAVAILABLE`. Cupom e pagamento inválidos deram `INVALID_COUPON`. FREESHIPPING na retirada com Pix 2× deu `COUPON_NOT_APPLICABLE`. Boleto 2× acima de R$ 1.000 deu `INVALID_INSTALLMENTS`. No `POST /orders`, as recusas foram iguais.
- **Operações de um pedido:** um id inexistente deu `404 ORDER_NOT_FOUND` no pagamento, no envio, na entrega, no cancelamento e na consulta. Faltando `transactionId`, `paidAt` ou `trackingCode`, deu `422 INVALID_ORDER`, antes do 409, mesmo com o pedido na situação errada. Cliente e aviso inexistentes deram `CUSTOMER_NOT_FOUND` e `NOTIFICATION_NOT_FOUND`, e os avisos de um CPF desconhecido também deram `CUSTOMER_NOT_FOUND`.

Conclusão: segue

**C5, a TransLog.** O log do stub mostra a chamada `zip=01310100&weight_g=1800&service=EXP` no exemplo 1 e `service=STD` na econômica; 0,10 kg virou `weight_g=100`.
- **Tradução:** 1234 centavos viraram 12.34, e 5 centavos viraram 0.05. As horas viraram dias arredondando para cima: 24 h deu 1 dia, 25 h deu 2, 1 h deu 1, 0 h deu 0 e 48 h deu 2. As regiões somaram os dias certos: Nordeste +2 (72 h → 5), Norte +3 (→ 6), Centro-Oeste sem acréscimo (→ 3).
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` deu `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Fora do ar, `503 CARRIER_UNAVAILABLE`:** outro `error`; status 500 ou 404; corpo que não é JSON; `{}`; demora de 2,3 s (respondeu em 2011 ms); demora de 10 s (respondeu em 2010 ms). Com demora de 1,7 s, o resumo saiu normal.
- **Site fora do ar de verdade (stub derrubado):** `COURIER` e `STORE_PICKUP` funcionaram, no resumo e no registro (`201`). `ECONOMY` e `EXPRESS` deram 503 em cerca de 15 ms.

Conclusão: segue

**C6, o ciclo do pedido.**
- **Registrar e pagar:** registrar `CAM-001`×2 não mexeu no estoque (50 → 50). O pagamento baixou para 48, o saldo foi de 0.00 para 3.20 (o crédito do resumo) e saiu o aviso `PAYMENT_APPROVED:NEW`.
- **Envio e entrega:** o envio sem `trackingCode`, ou com o código vazio, deu 422. Entregar antes de enviar deu 409. O envio com código deu `SHIPPED` com `"trackingCode":"BR123456789"` e o aviso `ORDER_SHIPPED`. Enviar de novo, cancelar o enviado e pagar o enviado com outro tx deram 409. A entrega deu `DELIVERED`; entregar ou cancelar depois deu 409.
- **Cancelar um pedido pago:** o estoque voltou (45 → 48), o saldo voltou (7.99 → 3.20) e saiu o aviso `ORDER_CANCELLED`.
- **Cancelar um pedido não pago:** estoque e saldo não mudaram, e saiu o aviso `ORDER_CANCELLED`. Pagar ou cancelar de novo um pedido cancelado deu 409. Enviar ou entregar um pedido não pago deu 409.
- **Cancelamentos ao mesmo tempo:** cinco cancelamentos de um pedido pago, divididos entre as duas máquinas, deram um 200 e quatro `409 OPERATION_NOT_ALLOWED`. O estoque voltou uma vez só (8 → 10) e o crédito saiu uma vez só (10.00 → 0.00).
- **Prazo de 48 h:** com `paidAt` em `createdAt`+48h exatas, o pedido virou `PAID`. O mesmo vale com fuso `-03:00` equivalente. Com +48h e 1 s, ou +48h e 0,5 s, veio `409 ORDER_EXPIRED`: o pedido ficou `CANCELLED`, o estoque não mudou e saiu o aviso `ORDER_CANCELLED`. Um pedido sem aviso de pagamento continua `AWAITING_PAYMENT`.
- **Avisos:** a lista vem do mais antigo para o mais novo. `POST /notifications/{id}/read` deu `READ`, e repetir na outra máquina continuou `READ`. `paidAt` inválido ("ontem") deu 422.

Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Um depois do outro:** mandei o mesmo `TX-S1` três vezes, alternando as máquinas. As três respostas foram byte a byte iguais, o estoque baixou uma vez (99 → 97), o crédito entrou uma vez (0.80) e saiu um único `PAYMENT_APPROVED`. Outro tx no pedido pago deu `409 OPERATION_NOT_ALLOWED`. Repetir o aviso de uma recusa devolveu a mesma recusa: `ORDER_EXPIRED` repetido deu `ORDER_EXPIRED`, mesmo com um `paidAt` diferente, e `INSUFFICIENT_STOCK` repetido deu `INSUFFICIENT_STOCK`, sem aviso novo. A repetição continuou funcionando depois de reiniciar o serviço.
- **Ao mesmo tempo, o mesmo tx:** 10 rodadas de 5 avisos simultâneos, alternando 8081 e 8082. Em todas: cinco 200 com respostas idênticas, estoque −1, crédito 0.40 e um `PAYMENT_APPROVED`. Cinco avisos simultâneos de um pedido vencido deram cinco `ORDER_EXPIRED` e um único `ORDER_CANCELLED`.
- **Ao mesmo tempo, txs diferentes:** 10 rodadas de 5 avisos simultâneos. Em todas: um 200 e quatro `409 OPERATION_NOT_ALLOWED`, estoque −1, um `PAYMENT_APPROVED` e crédito 0.40.

Conclusão: segue

**C8, a última peça.** Todos os pagamentos de cada rodada foram disparados juntos, alternando as duas máquinas:
- **Dois pedidos para a última unidade** (`MOC-001`, estoque 1): 8 rodadas. Em todas, 1 → 0, um pagamento passou e o outro recebeu `409 INSUFFICIENT_STOCK`.
- **Seis pedidos para duas unidades** (`JAQ-001`, estoque 2): 8 rodadas. Em todas, 2 → 0, dois passaram e quatro receberam `INSUFFICIENT_STOCK`.
- **Seis pedidos de `TEN`×1 + `JAQ`×2 para duas unidades de JAQ:** 4 rodadas, um aceito e cinco recusados.
- **Seis pedidos FON+TEN com os itens em ordens trocadas** (FON estoque 3): 5 rodadas, três aceitos e três recusados, sem deadlock nos logs.

Em todas as rodadas, cada perdedor ficou `CANCELLED` e recebeu exatamente o aviso `["ORDER_CANCELLED"]`. Nenhum estoque ficou negativo. Entre uma rodada e outra, cancelei os pedidos aceitos e o estoque voltou ao valor inicial.
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar, guardei o estoque dos oito produtos, o pedido `09a85229…` (`DELIVERED`, com código de rastreio e resumo), o pedido `608a00c4…` (`PAID`), o saldo e os avisos de dois clientes, incluindo um aviso já `READ`. Derrubei as duas máquinas e subi de novo. O `diff` das duas fotos saiu idêntico.
Conclusão: segue

**C10, os testes do próprio agente.** Numa cópia do projeto, `mvn -q verify` terminou com exit 0. Rodei de novo sem `DB_URL`, `DB_USER`, `DB_PASSWORD` e `TRANSLOG_URL` e também passou: os testes usam H2 em memória. Rodam 20 testes (`PedidosIntegrationTest`: 20 run, 0 failures, 0 errors, 0 skipped).
Conclusão: segue

## O que mudaria primeiro

1. **Decidir o que fazer com um `price_cents` fracionário, ou escrito como `3310.0`.** Hoje esses valores viram `503 CARRIER_UNAVAILABLE` (`TranslogClient.java:56-62`). Se a TransLog mandar o número nesse formato num dia normal, a econômica e a expressa somem do site sem motivo. Seria melhor aceitar `3310.0` e arredondar centavos fracionários "meio para o par", como o enunciado manda fazer com todo valor em dinheiro. Ou então deixar a recusa registrada como decisão.
2. Nada mais muda o resultado. Uma melhoria menor seria um teste de duas máquinas subindo juntas sobre o banco vazio. O Flyway com PostgreSQL deve resolver isso com o lock dele, mas não consegui testar.

## Dúvidas

- **`price_cents` com casas decimais.** O enunciado manda tratar como fora do ar qualquer resposta "diferente", mas também manda arredondar todo valor em dinheiro. O serviço escolheu recusar `3310.5`, `3310.0`, `"3310"` e `eta_hours: 40.5` como fora do ar. Achei a leitura defensável e não mudei a conclusão do C5 por isso.
- **O limite do MINUS50.** "A partir de R$ 300,00" não pode ser testado no valor exato: todos os preços terminam em ,90, e nenhuma combinação do catálogo soma 300,00. Testei 299,70 (recusado) e 318,40 (aceito). O código compara com `>= 0` (`coupon/Minus50Coupon.java:16`). Usei o boleto a R$ 1.000,00 exatos como fronteira extra.
- **O aviso repetido devolve o pedido "como está".** Depois de enviado ou cancelado, a repetição de um aviso que deu certo devolve 200 com o pedido `SHIPPED` ou `CANCELLED`, e não o `PAID` da primeira vez. Esse é o texto do enunciado ("200 e o pedido como está"), e foi assim que li.
- **O que conta como "o mesmo aviso".** O serviço identifica o aviso por pedido + `transactionId` e ignora o `paidAt`: uma repetição com outro `paidAt` recebe a resposta original. O enunciado define o aviso repetido exatamente assim, então não tratei isso como desvio.
- **Fração de segundo no prazo.** O `createdAt` é gravado truncado no segundo, e o prazo de 48 h conta a partir dele. Na prática, isso deixa a janela até menos de 1 s mais curta do que a hora real do registro. O enunciado não desce a esse nível.

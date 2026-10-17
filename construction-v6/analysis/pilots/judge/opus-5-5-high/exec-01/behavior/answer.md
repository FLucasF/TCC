# comportamento

## Visão geral

O serviço faz o que o enunciado pede em todos os itens que testei. Ele sobe em cerca de 2,3 s com o banco vazio, cria as tabelas e o catálogo, e não recarrega o catálogo por cima do estoque quando reinicia. Os dez exemplos do financeiro batem campo a campo, e também as fronteiras alcançáveis. A tabela de erros vale caso a caso e na ordem pedida, também com dois problemas ao mesmo tempo. A TransLog é traduzida como o anexo descreve, com o tempo limite de 2 s respeitado. O ciclo do pedido está certo: o estoque baixa só no pagamento, o cancelamento devolve estoque e crédito só quando o pedido estava pago, 48 h passa e 48 h + 1 s não. O aviso de pagamento repetido devolve a mesma resposta, byte a byte. Com duas instâncias e o mesmo banco, em 90 disputas simultâneas nada foi vendido a mais, e nenhuma apresentou problema. Os dados sobrevivem ao reinício, e os 39 testes do agente passam sem banco. Não achei falha. Três pontos ficam em aberto porque o enunciado não decide:
- um `paidAt` anterior ao registro é aceito;
- um `price_cents` fracionário é tratado como "TransLog fora do ar";
- o limite exato de R$ 300,00 do MINUS50 não pôde ser testado, porque nenhum carrinho do catálogo soma esse valor.

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/svc`, rodei `mvn -q package -DskipTests` (8 s) e subi com `java -jar … --server.port=8081`, apontando `TRANSLOG_URL` para o meu stub. Os dados do banco vêm de `DB_URL`, `DB_USER` e `DB_PASSWORD`. O log diz `Started OrdersApplication in 2.293 seconds`; o primeiro `GET /products/CAM-001` respondeu cerca de 3,2 s depois do comando. As tabelas e o catálogo saem de `src/main/resources/schema.sql` (`CREATE TABLE IF NOT EXISTS`) e `data.sql` (`INSERT … WHERE NOT EXISTS`). A segunda instância (8082) subiu em 3,5 s contra o mesmo banco já populado, sem erro e sem mexer no estoque.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Os oito vieram com nome, preço, peso, estoque e `discontinued` iguais ao anexo. Exemplos: `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`, `BON-001` com `stock 0`, e `REL-001` com `"discontinued":true`. Um SKU inexistente devolve `404 {"error":"PRODUCT_NOT_FOUND"}`. O produto fora de linha é recusado na compra com `PRODUCT_DISCONTINUED` (ver C4).
Conclusão: segue

**C3, o resumo.** Mandei `POST /checkout/summary` com o stub respondendo o que cada exemplo diz.
- **Exemplos 1 a 7:** todos os campos iguais ao enunciado. No 1: `409.70 / 40.97 / 33.10 / 5 / 10.24 / 0.00 / 412.07 / 1 / 412.07 / 0.00 / false`. No 2: ajuste `30.55`, final `462.00`, 6× de `77.00`, crédito `8.19`. No 7: frete `0.00`, prazo 6, seguro `30.00`, ajuste `-72.48`, final `1377.12`, crédito `70.98`, brinde `true`.
- **Exemplos 8 e 10:** `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Exemplo 9:** o CPF 10000000009 recebeu um pedido pago. Depois disso o WELCOME10 deu `422 COUPON_NOT_APPLICABLE`, tanto no resumo quanto em `POST /orders`. Antes do pagamento, e depois de cancelar o pedido pago, o mesmo cupom voltou a valer (desconto `15.98`).
- **Peso do motoboy:** MEI×50 pesa 5,00 kg e é aceito (frete `18.00`, prazo 0). TEN×4 + MEI×2 (5,00 kg) também é aceito. MEI×51 (5,10 kg) dá `SHIPPING_METHOD_UNAVAILABLE`.
- **Teto de 30% no FREESHIPPING** (MEI×1, teto `5.97`): com frete de 596 centavos o desconto foi `5.96`; com 597, `5.97`; com 598, `5.97` e frete `5.98`.
- **MINUS50:** com 299,10 em produtos dá `COUPON_NOT_APPLICABLE`; com 319,00 dá desconto `50.00`. Exatamente 300,00 não existe no catálogo: todo preço termina em ,90, e uma busca exaustiva não achou combinação. Por isso o limite exato não foi testado.
- **Limite do boleto:** o total do pedido exatamente em `1000.00` (JAQ×2 + frete 21240 centavos) foi aceito, com final `1003.49`. Com `1000.01` veio `PAYMENT_METHOD_UNAVAILABLE`.
- **Meio para o par:** crédito GOLD de MEI×3, 2,985 → `2.98`. Pix sobre um total de 38,10, 1,905 → ajuste `-1.90`.
- **Brinde:** 499,80 → `false`; 519,70 → `true`.
- **Outros:** o GOLD paga o motoboy (`18.00`). Cartão 4× sobre 80,70 dá `21.19 × 4 = 84.76`, e 12× dá `7.63 × 12 = 91.56`, ambos conferidos com a fórmula. BUY3PAY2 com desconto zero tira o desconto do Pix (ajuste `0.00`). Nordeste com a expressa soma 2 dias; Norte na retirada não soma nada.
- **Resumo e pedido iguais:** o resumo e o `summary` de `POST /orders` com os mesmos dados saíram idênticos (comparação com `jq -S`).
Conclusão: segue

**C4, as recusas.** Fiz os 15 casos isolados. Todos devolveram o código e o status da tabela:
- **INVALID_ORDER:** carrinho vazio ou ausente, item sem SKU, SKU duplicado, quantidade 0, -1 ou ausente, sem CPF, sem e-mail, sem `customer`, corpo vazio;
- **produtos:** `UNKNOWN_PRODUCT`; `PRODUCT_DISCONTINUED` (REL-001); `INSUFFICIENT_STOCK` (BON×1, MOC×2);
- **dados da compra:** nível, região e forma de entrega inválidos ou ausentes; CEP ausente, com 7 dígitos ou com traço;
- **cupom:** cupom `""`, `welcome10` em minúsculas e `BLACKFRIDAY` dão `INVALID_COUPON`. `coupon: null` é tratado como "não veio";
- **pagamento:** forma inválida ou ausente; PIX 2×, boleto 2×, cartão 0× e 13× dão `INVALID_INSTALLMENTS`; boleto acima de R$ 1.000 dá `PAYMENT_METHOD_UNAVAILABLE`.

Todos são 422, menos `CARRIER_UNAVAILABLE`, que é 503.

Com dois problemas ao mesmo tempo, sempre venceu o primeiro na ordem da tabela:

| combinação | veio |
|---|---|
| sem CPF + SKU inexistente | INVALID_ORDER |
| fora de linha + inexistente | UNKNOWN_PRODUCT |
| sem estoque + fora de linha | PRODUCT_DISCONTINUED |
| sem estoque + nível inválido | INSUFFICIENT_STOCK |
| região + nível inválidos | INVALID_CLUB_TIER |
| CEP + região inválidos | INVALID_REGION |
| entrega + CEP inválidos | INVALID_ZIP_CODE |
| motoboy acima de 5 kg + cupom inválido | SHIPPING_METHOD_UNAVAILABLE |
| ZIP_NOT_SERVED + cupom inválido | SHIPPING_METHOD_UNAVAILABLE |
| TransLog com 500 + cupom inválido | CARRIER_UNAVAILABLE (503) |
| cupom + pagamento inválidos | INVALID_COUPON |
| MINUS50 não aplicável + pagamento inválido | COUPON_NOT_APPLICABLE |
| parcelas inválidas + boleto acima de 1.000 | INVALID_INSTALLMENTS |

Em `POST /orders` as recusas são as mesmas, e um pedido recusado não cria o cliente (`GET /customers/10000000003` → 404).
Nas operações de um pedido, a ordem também vale. Pedido inexistente com corpo vazio dá 404 `ORDER_NOT_FOUND`. Pedido cancelado com o aviso de pagamento sem campos dá 422 `INVALID_ORDER`, antes do 409.
Conclusão: segue

**C5, a TransLog.** O stub registrou as consultas, por exemplo `GET /v2/quote?zip=01310100&weight_g=1800&service=EXP` no exemplo 1 e `service=STD` na econômica. O peso vai em gramas e o serviço certo é pedido.
- **Tradução:** 1234 centavos → `12.34`, 1 centavo → `0.01`. As horas viram dias arredondando para cima: 24 h → 1, 25 h → 2, 41 h → 2, 49 h → 3, 150 h → 7.
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` → 422 `SHIPPING_METHOD_UNAVAILABLE`.
- **Respostas tratadas como "fora do ar" (503 `CARRIER_UNAVAILABLE`):** HTTP 404, HTTP 500, corpo que não é JSON, outro código de erro, falta do `eta_hours`, `price_cents` em texto.
- **Tempo limite:** atrasos de 1,5 s e 1,9 s respondem normalmente (1559 ms e 1960 ms). Atrasos de 2,5 s e 10 s dão 503 em cerca de 2010 ms, então o limite vale para a resposta inteira (`TranslogCarrier.java:73`).
- **Preço com mais de duas casas:** `price_cents: 3310.5` dá 503, e `3310.0` dá `33.10`. O anexo diz que "qualquer outra coisa" é TransLog fora do ar, então recusar é uma leitura defensável. Arredondar para centavos também seria, e o enunciado não decide entre as duas.
- **TransLog derrubada:** com o processo do stub parado, a econômica e a expressa dão 503. A retirada (`0.00`, 1 dia) e o motoboy (`18.00`, 0 dia) seguem funcionando, no resumo e em `POST /orders` (201). Durante um atraso de 10 s, a retirada e o motoboy responderam em cerca de 8 ms. Um `POST /orders` com a econômica e a TransLog fora não registra nada.
Conclusão: segue

**C6, o ciclo do pedido.** Usei o CPF 20000000001 com CAM×2 + MEI×1.
- **Registrar:** 201, `AWAITING_PAYMENT`, estoque inalterado (CAM 50, MEI 100). O cliente nasce com `creditBalance 0.00` e sem avisos.
- **Fora de ordem antes do pagamento:** enviar e entregar dão 409 `OPERATION_NOT_ALLOWED`. Um aviso sem `paidAt` ou sem `transactionId` dá 422 `INVALID_ORDER`.
- **Pagar:** 200 `PAID`. O estoque fica CAM 48 / MEI 99, o crédito `3.59` (2% de 179,70) e aparece o aviso `PAYMENT_APPROVED NEW`.
- **Do pago em diante:** entregar sem ter enviado dá 409. Enviar sem `trackingCode` dá 422. Enviar dá 200 `SHIPPED` com `trackingCode "BR123456789"` e o aviso `ORDER_SHIPPED`. Cancelar o enviado dá 409. Entregar dá 200 `DELIVERED`. Entregar de novo, cancelar e pagar com outro tx dão 409.
- **Pago e cancelado:** TEN foi de 10 a 8 e voltou a 10. O crédito foi de `24.99` a `0.00`, e saiu o aviso `ORDER_CANCELLED`.
- **Não pago e cancelado:** TEN ficou em 10, o crédito ficou igual, e o aviso `ORDER_CANCELLED` saiu do mesmo jeito.
- **Prazo de 48 h:** `paidAt` igual ao registro + 48 h foi aceito (`PAID`), também em fuso `-03:00`. Com + 48 h 1 s veio 409 `ORDER_EXPIRED`, o pedido foi para `CANCELLED`, o estoque não mudou e saiu o aviso. A regra está em `OrderService.java:87` (`compareTo(...) > 0`).
- **Sem estoque na hora de pagar:** com FON em 5, o pedido FON×3 foi pago. Depois, o pedido CAM×1 + FON×3 veio 409 `INSUFFICIENT_STOCK`, foi cancelado e não baixou nem o CAM (tudo ou nada; CAM continuou 48).
- **Avisos:** a lista sai do mais antigo para o mais novo. `POST /notifications/{id}/read` duas vezes devolve `READ` nas duas.
- **Observação:** um `paidAt` anterior ao registro (`2020-01-01`) foi aceito. O enunciado não trata esse caso, então não conto como falha.
Conclusão: segue

**C7, o aviso de pagamento repetido.** O mesmo `TX-R` foi mandado três vezes, a terceira com outro `paidAt`. Veio 200 nas três, e os corpos 1, 2 e 3 são idênticos byte a byte (`cmp`). O estoque baixou uma vez só (CAM 48 → 45), o crédito entrou uma vez (`4.79`) e saiu um único `PAYMENT_APPROVED`. Outro `transactionId` no pedido pago deu 409 `OPERATION_NOT_ALLOWED`. A repetição de uma recusa devolve a mesma recusa: `ORDER_EXPIRED` e `INSUFFICIENT_STOCK` nos dois casos. Um aviso repetido depois do envio, ou depois do cancelamento, devolve 200 e "o pedido como está" (`SHIPPED`, `CANCELLED`), sem mexer em nada. Repetir depois de reiniciar o serviço também funcionou (C9). O mesmo aviso disparado ao mesmo tempo nas duas máquinas foi testado 30 vezes: 200/200 com corpos iguais, estoque −2 e crédito +0,80 uma vez só, um só aviso.
Conclusão: segue

**C8, a última peça.** Subi duas instâncias (8081 e 8082) no mesmo banco. Primeiro, 25 rodadas com `curl` em paralelo. Depois, 30 rodadas com um cliente Java que solta as duas chamadas na mesma `CyclicBarrier`: dois pedidos de MOC-001×1 com estoque 1, cada pagamento numa máquina.
- **Última peça:** sempre um `200 PAID` e um `409 INSUFFICIENT_STOCK` com o pedido `CANCELLED`, estoque 0, nunca negativo. A máquina A venceu 25 vezes e a B, 5, então houve disputa de verdade. Depois de cada rodada eu cancelava o vencedor e o estoque voltava a 1.
- **Itens cruzados:** 30 rodadas com [JAQ, FON] contra [FON, JAQ], com estoque para um só pedido. Sempre um 200 e um 409, sem travamento, estoque 0/0.

O mecanismo é `SELECT … FOR UPDATE` no pedido (`OrderRepository.java:75`) e nos produtos, em ordem de SKU (`ProductRepository.java:48`). O banco tem ainda `CHECK (stock >= 0)` (`schema.sql:6`).
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar, guardei os estoques dos oito produtos, o pedido `b23c7bd2…` (`DELIVERED`, com rastreio), o saldo do CPF 20000000002 (`32.98`) e a lista de avisos dele (9 `NEW`, 1 `READ`, com md5). Derrubei as duas instâncias e subi de novo. A comparação deu `diff` vazio: tudo idêntico, e o catálogo não foi recarregado (CAM continua em 45, não em 50).
Conclusão: segue

**C10, os testes do próprio agente.** Rodei `mvn -q verify` numa cópia limpa (`/tmp/svc-verify`) sem `DB_URL`, `DB_USER` e `DB_PASSWORD`. Saiu com `exit=0`. Rodaram 39 testes, com 0 falhas, 0 erros e 0 ignorados: `CheckoutSummaryTest` 11, `CheckoutValidationTest` 13, `OrderLifecycleTest` 13, `JdbcUrlTest` 2.
Conclusão: segue

## O que mudaria primeiro

1. **Pedir ao dono uma decisão sobre o `paidAt` anterior ao registro.** Hoje esse aviso é aceito (`OrderService.java:87` só olha o atraso). Se o banco nunca manda isso, tudo bem. Se mandar, um aviso com data errada aprova o pagamento sem questionar.
2. **Deixar explícito o que fazer com um `price_cents` fracionário.** Hoje ele vira 503 (`TranslogCarrier.java:105`). É coerente com "qualquer outra coisa é fora do ar", mas derruba a econômica e a expressa por um detalhe de formato. Vale confirmar com o dono se ele prefere arredondar para centavos.

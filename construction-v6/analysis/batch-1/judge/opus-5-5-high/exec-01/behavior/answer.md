# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Ele sobe sozinho com o banco vazio em cerca de 2,3 s: o Flyway cria as tabelas e carrega o catálogo. Os dez exemplos do financeiro dão os valores certos, campo a campo, e o pedido registrado traz exatamente o mesmo resumo da prévia. As recusas da tabela "Quando dá erro" vêm com o status e o código certos e respeitam a ordem do enunciado em todas as 14 combinações de dois problemas que montei. A tradução da TransLog e o tempo limite de 2 s estão corretos, e o motoboy e a retirada continuam funcionando com a TransLog fora do ar. O ciclo do pedido, o aviso de pagamento repetido e a disputa pela última peça entre duas máquinas também se comportam como o enunciado descreve, e nada se perde num reinício. Não achei nenhuma falha. Há só pontos que o enunciado não decide (um frete negativo vindo da TransLog é aceito, por exemplo), que deixei em "Dúvidas".

## Item a item

**C1, o serviço sobe.** Compilei uma cópia com `mvn -q package -DskipTests` e subi com `java -jar target/pedidos-1.0.0.jar --server.port=8081`, com o banco vazio. No log: `Migrating schema "public" to version "1 - schema"` e `Started LojaApplication in 2.345 seconds`. A segunda máquina (8082), e depois o reinício, encontraram `Schema "public" is up to date` e subiram em cerca de 3 s. Nenhum erro nos logs durante todos os testes.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs do anexo. Os oito vêm com nome, preço, peso, estoque e `discontinued` iguais aos do anexo, por exemplo `{"sku":"REL-001","name":"Relógio digital","price":129.90,"weightKg":0.20,"stock":8,"discontinued":true}`. O dinheiro e o peso vêm com 2 casas. `GET /products/XXX-999` devolve `404 {"error":"PRODUCT_NOT_FOUND"}`. Um pedido com `REL-001` é recusado com `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Configurei o stub da TransLog com a resposta de cada exemplo e chamei `POST /checkout/summary`.
- **Os dez exemplos:** os sete calculados conferem campo a campo. O exemplo 1 deu `409.70 / 40.97 / 33.10 / 5 / 10.24 / 0.00 / 412.07 / 1 / 412.07 / 0.00 / false`; o 2 deu `… 30.55 / 462.00 / 6 / 77.00 / 8.19`; o 7 deu `1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / … / 70.98 / true`. O 3, o 4, o 5 e o 6 também conferem.
- **Exemplos 8 e 10:** `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Exemplo 9:** antes de qualquer pedido o WELCOME10 vale (desconto de 15,98), e continua valendo com um pedido só aguardando pagamento. Depois de pagar um pedido desse CPF, o resumo e o `POST /orders` dão `422 COUPON_NOT_APPLICABLE`. Para um CPF que só tem pedidos cancelados, o cupom volta a valer (7,99).
- **Resumo igual ao pedido:** registrei os exemplos 1, 2, 3, 4 e 7 com `POST /orders` e comparei o `summary` com a prévia: iguais nos cinco, todos com 201.

Fronteiras:
- **Motoboy:** `MEI-001 ×50`, exatamente 5,00 kg, é aceito com frete 18,00 e prazo 0. Com ×51 (5,10 kg) vem `422 SHIPPING_METHOD_UNAVAILABLE`. O GOLD no motoboy paga os 18,00.
- **Meio para o par:** `MEI-001 ×15` no Sul dá seguro de 2,98, porque 2,985 vai para 2,98 (o arredondamento comum daria 2,99).
- **Teto de 30%:** o exemplo 5 (71,91) e o exemplo 6 (5,97) aplicam o teto. O FREESHIPPING com frete de exatamente 5,97, que é 30% de 19,90, dá desconto de 5,97; com frete de 5,96, dá 5,96.
- **Boleto:** total de exatamente R$ 1.000,00 (frete 799,01) é aceito, com final 1003,49. Com 1.000,01 vem `422 PAYMENT_METHOD_UNAVAILABLE`.
- **MINUS50 e brinde:** com subtotal de 299,10 o MINUS50 é recusado; com 319,60 vale 50,00. O GOLD com 499,00 não ganha brinde; com 519,20 ganha.
- **Outros:** BUY3PAY2 sem trio vale com desconto 0, e o Pix não dá desconto quando há cupom. O cartão em 12× dá parcela de 23,85.
Conclusão: segue

**C4, as recusas.** Testei em `/checkout/summary` cada linha da tabela, todas com 422:
- `INVALID_ORDER`: corpo vazio, carrinho vazio, item sem SKU, SKU repetido, quantidade 0, negativa ou ausente, sem CPF, sem e-mail, sem `customer`.
- `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, e `INSUFFICIENT_STOCK` com `BON-001 ×1` e `MOC-001 ×2`.
- `INVALID_CLUB_TIER` (inválido, ausente, `gold` minúsculo), `INVALID_REGION`, `INVALID_ZIP_CODE` (ausente, 7 dígitos, com traço), `INVALID_SHIPPING_METHOD`.
- `INVALID_COUPON` para `""`, `welcome10` e `BLACKFRIDAY`. O cupom `null` é tratado como ausente.
- `INVALID_PAYMENT_METHOD`, e `INVALID_INSTALLMENTS` para Pix 2×, boleto 2× e cartão 0× e 13×.

Combinações de dois problemas, iguais em `/checkout/summary` e em `/orders`; em todas veio o primeiro da ordem:
- SKU repetido + SKU inexistente → `INVALID_ORDER`
- fora de linha + inexistente → `UNKNOWN_PRODUCT`
- sem estoque + fora de linha → `PRODUCT_DISCONTINUED`
- sem estoque + nível inválido → `INSUFFICIENT_STOCK`
- nível + região → `INVALID_CLUB_TIER`
- região + CEP → `INVALID_REGION`
- CEP + entrega → `INVALID_ZIP_CODE`
- entrega + cupom → `INVALID_SHIPPING_METHOD`
- motoboy acima de 5 kg + cupom inválido → `SHIPPING_METHOD_UNAVAILABLE`
- TransLog com 500 + cupom inválido → `503 CARRIER_UNAVAILABLE`
- cupom inválido + pagamento inválido → `INVALID_COUPON`
- FREESHIPPING na retirada + pagamento inválido → `COUPON_NOT_APPLICABLE`
- pagamento inválido + parcelas inválidas → `INVALID_PAYMENT_METHOD`
- boleto 2× acima de R$ 1.000 → `INVALID_INSTALLMENTS`

Nas operações de pedido, um pedido inexistente dá `404 ORDER_NOT_FOUND` nas cinco operações. Faltar campo dá 422 antes do 409. Um cliente ou aviso inexistente dá 404 com `CUSTOMER_NOT_FOUND` ou `NOTIFICATION_NOT_FOUND`.
Conclusão: segue

**C5, a TransLog.** O log do stub mostra a consulta certa: `zip=01310100&weight_g=1800&service=EXP` no exemplo 1, e `service=STD` na econômica.
- **Prazo em dias** (`eta_hours` → dias): 0→0, 1→1, 23→1, 24→1, 25→2, 47→2, 48→2, 49→3, 72→3. O Norte soma 3 e o Nordeste soma 2. A retirada e o motoboy não somam nada no Norte (1 e 0).
- **Preço:** `price_cents` 1234 vira 12,34 e 1 vira 0,01.
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` dá `422 SHIPPING_METHOD_UNAVAILABLE`.
- **TransLog fora do ar:** tudo isto deu `503 CARRIER_UNAVAILABLE` — status 404, 500, 302 e 204; corpo que não é JSON; corpo vazio; `{"error":"RATE_LIMITED"}`; campo faltando; `price_cents` como texto ou fracionário (3310.5); `eta_hours` fracionário.
- **Tempo limite:** com atraso de 1,5 s e de 1,9 s, a resposta vem (1521 ms e 1960 ms). Com 2,1 s, 3 s e 10 s, vem `503` em cerca de 2015 ms.
- **Site fora do ar:** com o stub derrubado, o motoboy e a retirada respondem 200 no resumo, e o `POST /orders` com motoboy dá 201. A econômica e a expressa dão 503 no resumo e no registro.
Conclusão: segue

**C6, o ciclo do pedido.**
- **Caminho completo** (pedido A, SILVER, `CAM-001 ×2` + `MEI-001 ×3`): ao registrar, o estoque continua 50/99, o saldo é 0,00 e a lista de avisos vem vazia. No pagamento, o estoque vai a 48/96, o saldo a 4,39 (o crédito do resumo) e aparece o aviso `PAYMENT_APPROVED`. O envio sem `trackingCode` ou com código vazio dá 422. Com o código, o pedido fica `SHIPPED` com `trackingCode` e aparece `ORDER_SHIPPED`. Depois vem `DELIVERED`.
- **Fora de ordem:** enviar ou entregar um pedido aguardando pagamento, entregar um pago, cancelar um enviado ou entregue, enviar ou entregar de novo, e pagar com outro `transactionId` um pedido já enviado: todos dão `409 OPERATION_NOT_ALLOWED`.
- **Cancelar um pedido pago** (B, GOLD): o estoque de FON vai de 4 para 5, o saldo de 10,00 para 0,00, e aparece `ORDER_CANCELLED`.
- **Cancelar um pedido não pago** (C): o estoque e o saldo não mudam, e aparece `ORDER_CANCELLED`. Pagar esse pedido cancelado depois dá 409.
- **Prazo de 48 h:** um `paidAt` igual a `createdAt` + 48 h exatas é aceito (`PAID`). Com + 48 h + 1 s vem `409 ORDER_EXPIRED`, o pedido fica `CANCELLED`, o estoque e o saldo não mudam, e aparece `ORDER_CANCELLED`.
- **Falta de estoque no pagamento:** com `JAQ-001` (estoque 3) registrei dois pedidos, um de ×2 e outro de `MEI ×1 + JAQ ×2`. O primeiro pagamento passa (JAQ fica com 1). O segundo dá `409 INSUFFICIENT_STOCK`, o pedido fica `CANCELLED` e a meia não baixa: a baixa é tudo ou nada.
- **Avisos:** a lista vem em ordem crescente de id. `POST /notifications/10/read` devolve `READ`, e repetir continua `READ`.
- **Pedido inexistente:** `GET /orders/nope` dá `404 ORDER_NOT_FOUND`.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Repetição em sequência:** paguei o pedido H com `TX-H`. Repeti 3 vezes e o corpo veio byte a byte igual, com 200. O estoque de TEN ficou em 9, o saldo em 12,50 e só há 1 aviso.
- **Depois do envio:** a repetição devolve 200 e o pedido como está (`SHIPPED`), sem mudar nada.
- **Recusa repetida:** a repetição de uma recusa devolve a mesma recusa (`TX-E` → `ORDER_EXPIRED`; `TX-G2` → `INSUFFICIENT_STOCK`), mesmo com outro `paidAt`.
- **Outro `transactionId`:** num pedido pago, num expirado ou num cancelado, dá `409 OPERATION_NOT_ALLOWED`.
- **Repetição simultânea:** mandei o mesmo aviso 4 vezes ao mesmo tempo, 2 por máquina, em 5 rodadas. Veio uma resposta só (um único hash), o estoque baixou 1, o crédito entrou uma vez e há 1 aviso.
- **Depois do reinício:** a repetição continua igual.
Conclusão: segue

**C8, a última peça.** Testei com duas máquinas (8081 e 8082) no mesmo banco, usando `MOC-001` (estoque 1). Em cada rodada registrei os pedidos, disparei os pagamentos juntos e, no fim, cancelei o vencedor para o estoque voltar a 1.
- **Um contra um, 15 rodadas:** sempre 1 `200 PAID` e 1 `409 INSUFFICIENT_STOCK`, com estoque final 0. A 8081 ganhou todas, o que indica pouca sobreposição real entre os pedidos.
- **Oito ao mesmo tempo, 12 rodadas:** dois `curl --parallel`, cada um com 4 pagamentos para uma máquina. Sempre exatamente 1 com 200 e 7 com 409, estoque 0, e o vencedor variou (a 8082 ganhou uma vez).
- **`JAQ-001 ×2` contra `×2`, estoque 3, 5 rodadas:** sempre `PAID` e `INSUFFICIENT_STOCK`, com estoque 1.

O estoque nunca ficou negativo, e nenhuma exceção ou 500 apareceu nos logs.
Conclusão: segue

**C9, os dados não se perdem.** Antes do reinício guardei os pedidos A, B, E e H, o estoque de seis produtos, e o saldo e os avisos de quatro clientes (inclusive um aviso já lido). Derrubei as duas máquinas, subi a 8081 de novo e repeti as consultas: `diff` sem nenhuma diferença. O `DELIVERED`, o `SHIPPED` e os `CANCELLED` continuam lá, os saldos também (4,39, 12,50, 0,80), e o aviso `READ` continua lido. A repetição de `TX-H` e de `TX-E` depois do reinício devolve a mesma resposta de antes.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` numa cópia sai com código 0. Rodam 9 testes, sem falhas nem pulados: 5 em `LojaIntegrationTest` (com H2 em memória) e 4 em `TransLogGatewayTest`.
Conclusão: segue

## O que mudaria primeiro

1. **Validar melhor a resposta da TransLog.** Um `price_cents` negativo (-100) foi aceito e virou frete de -1,00 no resumo, o que baixa o total do pedido. Tratar preço negativo como resposta inválida (TransLog fora do ar) seria mais seguro, já que o enunciado manda tratar "qualquer outra coisa" assim.
2. **Nenhuma falha de comportamento exige correção.** O resto do que testei confere com o enunciado.

## Dúvidas

- **"Um preço com mais de duas casas".** O `price_cents` é em centavos, então "mais de duas casas" só aparece como centavos fracionários (`3310.5`). O serviço trata isso como TransLog fora do ar (503). Li como certo: não é o formato documentado, e o enunciado manda tratar "qualquer outra coisa" como fora do ar. Mas o enunciado não diz isso de forma explícita.
- **Frete negativo ou zero vindo da TransLog** (`price_cents: -100` ou `0`) é aceito. O enunciado não cobre o caso, então não mudei a conclusão do C5.
- **As fronteiras exatas de R$ 300,00 (MINUS50) e R$ 500,00 (brinde) não podem ser testadas.** Todos os preços terminam em ,90, então nenhuma combinação do catálogo dá esses valores. Testei os vizinhos (299,10 e 319,60; 499,00 e 519,20). Pela leitura do código, o limite do MINUS50 é inclusivo e o do brinde é exclusivo ("passar de"), como o enunciado diz, mas isso não foi visto rodando.
- **As 48 h contam a partir do `createdAt`, que o serviço guarda sem frações de segundo.** Usei o `createdAt` devolvido como referência das 48 h exatas.
- **O estoque conferido no resumo e no registro é o estoque daquele momento**, sem contar pedidos que aguardam pagamento. Isso segue o enunciado ("o estoque só baixa quando o pagamento é confirmado").

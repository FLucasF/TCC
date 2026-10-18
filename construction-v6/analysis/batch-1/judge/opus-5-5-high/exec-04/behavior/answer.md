# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Ele sobe com o banco vazio em cerca de 3 s; a migração Flyway cria as tabelas e carrega o catálogo. Os dez exemplos do financeiro dão os mesmos valores, campo a campo, e o pedido registrado traz o mesmo resumo da prévia. As cinco fronteiras testadas conferem: motoboy com exatamente 5 kg, boleto com total de exatamente R$ 1.000,00, frete igual a 30% no FREESHIPPING, 48 h exatas no pagamento e "meio para o par" em dois lugares. As 15 linhas da tabela "Quando dá erro" respeitam a ordem quando há dois problemas ao mesmo tempo, tanto no resumo quanto no registro. A TransLog é traduzida como o anexo pede, inclusive o tempo limite de 2 s e o CEP não atendido. O ciclo do pedido, os avisos, o aviso de pagamento repetido (também ao mesmo tempo nas duas máquinas) e a disputa pela última peça entre duas máquinas funcionam: em 25 rodadas, só um pagamento passou de cada vez, e cada máquina venceu em alguma rodada. Os dados sobrevivem ao reinício, e o `mvn verify` passa sem banco. Não achei falha. O que sobra são leituras de pontos que o enunciado não decide: o `"coupon": null`, o `price_cents` não inteiro e o arredondamento do peso em gramas. Estão em "Dúvidas".

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/run`, compilei (`mvn -q package -DskipTests`, 6 s) e subi com `java -jar target/pedidos-1.0.0.jar --server.port=8081` sobre o banco vazio. O log mostra `Current version of schema "public": << Empty Schema >>`, `Successfully applied 1 migration ... v1` e `Started PedidosApplication in 2.286 seconds`. O primeiro `GET /products/CAM-001` respondeu cerca de 3,1 s depois do comando. As tabelas e a carga do catálogo vêm de `src/main/resources/db/migration/V1__init.sql`. A segunda máquina (8082) e o reinício encontraram `Schema "public" is up to date` e subiram em cerca de 2,2 s.
Conclusão: segue

**C2, o catálogo.** `GET /products/{sku}` para os oito SKUs devolveu `200` com os valores do anexo, em todos os campos. Por exemplo: `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`. O BON-001 vem com `stock` 0. O REL-001 vem com `"discontinued":true`, e no resumo ele é recusado com `PRODUCT_DISCONTINUED`. Um SKU inexistente (`XXX-999`) devolve `404 {"error":"PRODUCT_NOT_FOUND"}`.
Conclusão: segue

**C3, o resumo.** Passei os dez exemplos por `POST /checkout/summary`, com o stub respondendo o que cada um diz:
- **E1:** 409.70 / 40.97 / 33.10 / 5 / 10.24 / 0.00 / 412.07 / 1× 412.07 / 0.00 / false.
- **E2:** 15.60 / 7 / 6.15 / 30.55 / 462.00 / 6× 77.00 / 8.19.
- **E3:** 50.00 / 18.00 / 0 / 8.00 / 3.49 / 379.29 / 19.99.
- **E4:** 299.10 / 39.80 / 0.00 / 1 / 2.99 / 262.29 / 3× 87.43 / 5.98.
- **E5:** 71.91 / 24.75 / 2 / 2.40 / 194.94.
- **E6:** 5.97 / 22.10 / 7 / 0.20 / 3.49 / 39.72.
- **E7:** 1419.60 / 0.00 / 6 / 30.00 / −72.48 / 1377.12 / 70.98 / `gift:true`.
- **E8 e E10:** `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **E9:** com o pedido do CPF só registrado, o WELCOME10 ainda vale. Depois de pagá-lo, vem `422 COUPON_NOT_APPLICABLE`.

O stub registrou a consulta do E1 como `/v2/quote?zip=01310100&weight_g=1800&service=EXP`. O `POST /orders` do E1 devolveu `201` com o mesmo `summary` da prévia.

Fronteiras:
- **Motoboy:** 50 meias (5,00 kg) dão frete 18.00. Com 51 (5,10 kg) vem `SHIPPING_METHOD_UNAVAILABLE`.
- **Boleto:** com frete de 192.30, o total do pedido fica em exatamente 1000.00 e é aceito (`finalTotal` 1003.49). Com 192.31 vem `PAYMENT_METHOD_UNAVAILABLE`.
- **Limite de 30%:** no FREESHIPPING com frete 5.97 (= 30% de 19.90) o desconto é 5.97, com 5.96 é 5.96. No E5 e no E6 o desconto já é cortado no limite.
- **MINUS50:** com 299.10 vem `COUPON_NOT_APPLICABLE`; com 318.70 o desconto é 50.00.
- **"Meio para o par":** no Pix sobre 20.10, a conta dá 1.005 e o ajuste sai −1.00, e não −1.01. O crédito GOLD sobre 239.70 dá 11.985 e sai 11.98 (`GET /customers/90000000200`). Sobre 20.30, 1.015 sai 1.02.
- **Cartão:** 4× e 12× ficam com juros (4× de 21.19, 12× de 7.63, ambos sobre 80.70). Não recalculei esses dois à parte: a calculadora `bc` não estava instalada. A fórmula já está conferida pelo 6× do E2.
Conclusão: segue

**C4, as recusas.** Cada linha da tabela devolveu o código e o status pedidos:
- **1, `INVALID_ORDER`:** carrinho vazio, item sem SKU, SKU repetido, quantidade 0, −1 ou ausente, CPF ou e-mail ausente, sem `customer`, corpo vazio e JSON quebrado, todos com 422.
- **2 a 4:** `UNKNOWN_PRODUCT` (XXX-1), `PRODUCT_DISCONTINUED` (REL-001) e `INSUFFICIENT_STOCK` (BON-001 ×1, MOC-001 ×2).
- **5 a 8:** `INVALID_CLUB_TIER` (PLATINUM, ausente, `gold`), `INVALID_REGION`, `INVALID_ZIP_CODE` (7 dígitos, `01310-100`, ausente) e `INVALID_SHIPPING_METHOD`.
- **9 e 10:** `SHIPPING_METHOD_UNAVAILABLE` e `CARRIER_UNAVAILABLE` com 503.
- **11 e 12:** `INVALID_COUPON` (XPTO, `""`, `welcome10`) e `COUPON_NOT_APPLICABLE`.
- **13 a 15:** `INVALID_PAYMENT_METHOD`, `INVALID_INSTALLMENTS` (PIX 2×, boleto 2×, cartão 0× e 13×) e `PAYMENT_METHOD_UNAVAILABLE`.

Com dois problemas, vence sempre o primeiro da ordem, no resumo e em `POST /orders`:
- quantidade 0 com SKU desconhecido dá `INVALID_ORDER`; fora de linha com desconhecido dá `UNKNOWN_PRODUCT`; sem estoque com fora de linha dá `PRODUCT_DISCONTINUED`;
- sem estoque com nível inválido dá `INSUFFICIENT_STOCK`; nível com região inválidos dá `INVALID_CLUB_TIER`; região com CEP dá `INVALID_REGION`; CEP com entrega dá `INVALID_ZIP_CODE`;
- motoboy acima de 5 kg com cupom inválido dá `SHIPPING_METHOD_UNAVAILABLE`; TransLog fora do ar com cupom e pagamento inválidos dá 503 `CARRIER_UNAVAILABLE`; CEP não atendido com cupom inválido dá `SHIPPING_METHOD_UNAVAILABLE`;
- cupom inválido com pagamento inválido dá `INVALID_COUPON`; pagamento inválido com 99 parcelas dá `INVALID_PAYMENT_METHOD`; boleto 2× acima de R$ 1.000 dá `INVALID_INSTALLMENTS`.

Sem `installments`, o cartão sai em 1×.
Conclusão: segue

**C5, a TransLog.** O stub é um `HttpServer` do JDK, com a resposta, o status e o atraso controlados por arquivo:
- **Tradução:** `price_cents` 3310 vira 33.10. Com o Nordeste (+2 dias), `eta_hours` 0, 1, 48 e 49 dão 2, 3, 4 e 5 dias; no Sul, 24 h dão 1 dia e 25 h dão 2.
- **Serviço e peso:** a econômica manda `service=STD`, a expressa `service=EXP`, e o `weight_g` é inteiro.
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` devolve `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Fora do ar:** todas estas respostas devolvem `503 CARRIER_UNAVAILABLE`: status 500 ou 404, `ZIP_NOT_SERVED` com 400, outro `error`, corpo que não é JSON, `{}` e preço negativo.
- **Tempo limite:** com atraso de 1,5 s a cotação passa (a chamada levou 1560 ms). Com 2,5 s ou 10 s vem 503 em cerca de 2020 ms, o que confirma o limite de 2 s em `src/main/java/br/com/loja/pedidos/shipping/TranslogClient.java:24` e `:48`.
- **Site fora do ar:** com o stub derrubado, ECONOMY e EXPRESS dão 503, enquanto COURIER (18.00 / 0 dia) e STORE_PICKUP (0.00 / 1 dia) dão 200, e um `POST /orders` com COURIER dá 201.
Conclusão: segue

**C6, o ciclo do pedido.**
- **Estoque e crédito:** com a CAM em 50, registrar 2 unidades deixa o estoque em 50; pagar deixa em 48 e o crédito do SILVER vai de 0.00 a 3.20.
- **Pedido aguardando:** entregar e enviar dão `409 OPERATION_NOT_ALLOWED`.
- **Pedido pago:** entregar dá 409. Enviar sem `trackingCode` ou com ele vazio dá `422 INVALID_ORDER`; com o código, vem 200, `SHIPPED` e `trackingCode` preenchido.
- **Pedido enviado:** cancelar dá 409, e outro pagamento também. Entregar dá 200 `DELIVERED`.
- **Pedido entregue:** enviar, cancelar ou entregar de novo dão 409.
- **Cancelar um pago:** o estoque volta de 46 para 48 e o crédito de 6.40 para 3.20. Cancelar de novo dá 409.
- **Cancelar um não pago:** estoque (48) e crédito (3.20) ficam iguais. Pagar ou enviar depois dá 409.
- **Prazo de 48 h:**
  - com `paidAt` exatamente `createdAt` + 48 h, o pagamento passa (`PAID`), também quando o mesmo instante vem escrito em `-03:00`;
  - com +48 h 1 s vem `409 ORDER_EXPIRED`, o pedido fica `CANCELLED`, o estoque da MEI não muda e sai um `ORDER_CANCELLED`.
- **Falta de estoque no pagamento:** com o FON em 4, pagar FON ×5 + MEI ×1 dá `409 INSUFFICIENT_STOCK` e o pedido vai para `CANCELLED`. A MEI não baixa: não houve baixa parcial.
- **Operações de um pedido que não existe:** as quatro operações e o `GET` dão `404 ORDER_NOT_FOUND`, também sem corpo.
- **Campos do aviso de pagamento:** sem `transactionId`, sem `paidAt`, com `paidAt` "ontem" ou sem corpo, vem `422 INVALID_ORDER`.
- **Avisos:** chegam na ordem certa (`PAYMENT_APPROVED`, `ORDER_SHIPPED`, `ORDER_CANCELLED`), com `id`, `type`, `orderId` e `status` `NEW`.
- **Marcar como lido:** `POST /notifications/{id}/read` devolve `READ`, e de novo continua `READ`. Um aviso inexistente dá `404 NOTIFICATION_NOT_FOUND`.
- **Clientes:** um CPF desconhecido dá `CUSTOMER_NOT_FOUND`, no saldo e nos avisos. Um CPF que só pediu resumo, ou cujo pedido foi recusado, não vira cliente.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Mesmo aviso:** três `POST /orders/{id}/payment` iguais (`TX-R1`) devolveram respostas idênticas byte a byte. O estoque baixou uma vez (48 para 45), o crédito entrou uma vez (11.98) e houve um só `PAYMENT_APPROVED`. O mesmo `transactionId` com outro `paidAt` também devolve o pedido.
- **Outro `transactionId`:** no pedido pago dá `409 OPERATION_NOT_ALLOWED`.
- **Recusas repetidas:** um expirado repetido devolve de novo `409 ORDER_EXPIRED`, com um único `ORDER_CANCELLED`; outro tx nesse pedido dá `OPERATION_NOT_ALLOWED`. Um `INSUFFICIENT_STOCK` repetido devolve a mesma recusa, mesmo depois de a peça voltar ao estoque, e o estoque não muda.
- **Nas duas máquinas ao mesmo tempo:** o mesmo aviso (`TX-DUP`) deu 200 nas duas, com corpos idênticos, uma baixa só (CAM 45 para 44) e um único `PAYMENT_APPROVED`.
Conclusão: segue

**C8, a última peça.** Subi a segunda máquina na 8082, com o mesmo banco. Em cada rodada registrei dois pedidos de MOC-001 ×1 (estoque 1), um em cada máquina, e disparei os dois pagamentos no mesmo instante. Os tempos medidos mostram as duas chamadas em curso ao mesmo tempo (início a 1–3 ms, fim a 12–21 ms). Depois, cancelei o pago para a peça voltar.
- **25 rodadas** (15 numa ordem de disparo, 10 na ordem inversa): sempre um 200 e um `409 INSUFFICIENT_STOCK`, com o estoque em 0, nunca negativo. A 8082 venceu duas vezes.
- **Seis pagamentos simultâneos** de JAQ-001 ×2, com estoque 3, alternando as máquinas: um `PAID` e cinco `INSUFFICIENT_STOCK`; o estoque terminou em 1.
- **Logs:** nenhum `ERROR` nas duas máquinas.

O código explica o resultado. Há `SELECT ... FOR UPDATE` nas linhas do produto, em ordem de SKU (`src/main/java/br/com/loja/pedidos/catalog/ProductRepository.java:289-292`, `src/main/java/br/com/loja/pedidos/order/OrderService.java:127-131`), a baixa é condicional (`ProductRepository.java:295`) e há um `CHECK (stock >= 0)` (`V1__init.sql:6`).
Conclusão: segue

**C9, os dados não se perdem.** Antes do reinício, fotografei:
- um pedido `DELIVERED`;
- os estoques: CAM 44, MOC 1, JAQ 1, FON 4, MEI 97;
- o saldo de 3.20 do CPF 90000000100;
- os cinco avisos do CPF 90000000101, um deles `READ`.

Derrubei as duas máquinas (o log mostra o `Shutdown completed`) e subi de novo. A mesma foto saiu idêntica (`diff` vazio), e a migração não rodou de novo (`Schema "public" is up to date`).
Conclusão: segue

**C10, os testes do próprio agente.** Numa cópia limpa (`/tmp/verify`), sem `DB_URL`, `DB_USER`, `DB_PASSWORD` e `TRANSLOG_URL`, o `mvn -q verify` terminou com código 0 em 8 s. Rodaram 20 testes, numa classe só (`PedidosIntegrationTest`): 0 falhas, 0 erros, 0 pulados (`target/surefire-reports/TEST-br.com.loja.pedidos.PedidosIntegrationTest.xml`). Os testes usam H2.
Conclusão: segue

## O que mudaria primeiro

1. **Decidir o `"coupon": null`.** Hoje ele conta como "não informado" (`src/main/java/br/com/loja/pedidos/checkout/CheckoutRequest.java:171`) e ganha o desconto do Pix. Se o site mandar `null` quando o campo está vazio, tudo bem; se mandar `null` para um cupom apagado à mão, o cliente ganha o Pix sem querer. Vale alinhar com o site e fixar num teste.
2. **Criar o cliente na mesma transação do pedido.** `OrderService.register` cria o cliente fora da transação que grava o pedido (`src/main/java/br/com/loja/pedidos/order/OrderService.java:58-62`). Se o insert do pedido falhar, fica um cliente sem pedido, ao contrário de "o cliente passa a existir quando registra o primeiro pedido". Não consegui provocar isso pela API: só vi pela leitura do código.

## Dúvidas

- **`"coupon": null`:** o enunciado diz "pode não vir. Se vier, mesmo vazio, precisa ser um dos cupons". O serviço trata `null` como ausente: o resumo sai 200 e com o desconto do Pix. Li `null` como "não veio" e não descontei.
- **`price_cents` não inteiro:** com 3310.5, com 3310.0 ou como texto, o serviço responde 503 `CARRIER_UNAVAILABLE` (`TranslogClient.java:73-81`). Li como "qualquer outra coisa", que o anexo manda tratar como fora do ar. Recusar até o 3310.0 é uma leitura estrita, mas defensável.
- **`weight_g`:** o serviço arredonda o peso para cima em gramas (`src/main/java/br/com/loja/pedidos/shipping/CarrierShippingOption.java:110`), e o enunciado não diz como arredondar. Com os pesos do catálogo (múltiplos de 10 g), isso nunca muda o valor.
- **Fronteiras de R$ 300,00 (MINUS50) e R$ 500,00 (brinde):** não dá para cair exatamente nelas com este catálogo. Todos os preços terminam em ,90, então só somas de 10, 20… unidades dão centavos ,00, e nenhuma combinação dá esses valores. Testei 299,10 e 318,70. A fronteira do limite de 30%, ao contrário, foi testada exatamente.
- **Aviso repetido de um pedido pago e depois cancelado:** devolve 200 com o pedido como está agora, já `CANCELLED`. Li isso como "200 e o pedido como está", conforme o anexo.
- **GOLD + FREESHIPPING na expressa:** o frete já sai zerado, o desconto fica 0,00 e o cupom é aceito (e, por ter cupom, o Pix não desconta). O enunciado não trata esse caso; não contei como erro.

# comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio, carrega o catálogo certo e faz bem a conta do resumo: com a TransLog fora da jogada, os exemplos e as fronteiras conferem campo a campo. O ciclo do pedido funciona no caminho feliz e os dados sobrevivem a um reinício. Os problemas, do mais grave para o menos grave:

1. **A integração com a TransLog não funciona no formato do enunciado.** O cliente lê `priceCents`/`etaHours`, mas a TransLog manda `price_cents`/`eta_hours`. Toda cotação vira `SHIPPING_METHOD_UNAVAILABLE`, e a econômica e a expressa nunca funcionam. A TransLog fora do ar também vira `422`, e não `503`. O tempo limite de 2 s não existe.
2. **As recusas do pagamento são desfeitas pelo rollback.** O pedido vencido e o que perde o estoque recebem a recusa certa, mas continuam `AWAITING_PAYMENT`, sem o aviso `ORDER_CANCELLED`. O vencido pode até ser pago depois, com outro `transactionId`.
3. **O prazo de 48 h é contado em horas inteiras.** 48 h 59 min ainda passa.
4. **A concorrência não corrompe nada, mas responde mal.** Avisos simultâneos resultam em um `200` e quatro `500`. A memória dos avisos repetidos fica só na memória de cada máquina, e por isso a mesma recusa repetida na outra máquina virou aprovação.
5. **Valores fora da lista recebem um `400` genérico do Spring, sem o código pedido.** Isso vale para nível, região, entrega, cupom (inclusive o vazio) e forma de pagamento. Falta de estoque no resumo sai `409`, e não `422`.

## Item a item

**C1, o serviço sobe.** Compilei a cópia com `mvn -q package -DskipTests` e subi com `java -jar … --server.port=8081` contra o banco vazio. O log diz `Started OrderServiceApplication in 5.052 seconds`, e o `GET /products/CAM-001` respondeu logo depois. As tabelas foram criadas pelo `ddl-auto=update` (`src/main/resources/application.properties:8`). A segunda máquina, na 8082 e com o mesmo banco, subiu em 4,8 s sem semear o catálogo de novo.
Conclusão: segue

**C2, o catálogo.** Fiz `GET /products/{sku}` nos oito produtos. SKU, nome, preço, peso, estoque e `discontinued` vieram iguais ao anexo, por exemplo `{"sku":"BON-001",…,"price":59.90,"weightKg":0.15,"stock":0,"discontinued":false}`. O `REL-001` vem com `"discontinued":true`, e no resumo ele dá `422 PRODUCT_DISCONTINUED`. Um SKU inexistente dá `404 {"error":"PRODUCT_NOT_FOUND"}`.
Conclusão: segue

**C3, o resumo.** Rodei `POST /checkout/summary` com o stub da TransLog respondendo exatamente o JSON de cada exemplo:
- **Os exemplos 3, 4 e 8, que não passam pela TransLog, conferem.**
  - Ex3 deu `399.80 / 50.00 / 18.00 / 0 / 8.00 / 3.49 / 379.29 / 1× 379.29 / 19.99 / false`.
  - Ex4 deu `299.10 / 39.80 / 0.00 / 1 / 2.99 / 0.00 / 262.29 / 3× 87.43 / 5.98`.
  - Ex8 deu `422 COUPON_NOT_APPLICABLE`.
  - O `POST /orders` dos exemplos 3 e 4 registrou o mesmo resumo do `/checkout/summary`, campo a campo.
- **Os exemplos 1, 2, 5, 6, 7 e 9 deram `422 {"error":"SHIPPING_METHOD_UNAVAILABLE"}`.** Era esperado o resumo, ou `COUPON_NOT_APPLICABLE` no Ex9. O exemplo 10 dá o `SHIPPING_METHOD_UNAVAILABLE` esperado, mas por acaso: qualquer resposta da TransLog dá isso (ver C5).
- **A conta em si está certa.** Para ver o resto da conta, fiz o stub responder com os campos em camelCase, um formato que o enunciado não prevê. Aí os seis exemplos batem todos os campos (Ex1 `412.07`, Ex2 `6× 77.00` com ajuste `30.55`, Ex5 `71.91`, Ex6 `39.72`, Ex7 `-72.48 / 1377.12 / 70.98 / gift true`, Ex9 `COUPON_NOT_APPLICABLE`).
- **As fronteiras conferem.**
  - Motoboy com exatamente 5,00 kg (50 `MEI-001`): aceito. Com 5,10 kg: `SHIPPING_METHOD_UNAVAILABLE`.
  - Limite de 30%: `BUY3PAY2` com 3 `JAQ-001` dá `350.91`, que é 30% de 1169,70.
  - "Meio para o par": 15 `MEI-001` (298,50) dão seguro `2.98` (o 2,985 do enunciado) e crédito GOLD `14.92` (de 14,925).
  - `MINUS50`: 299,70 dá `COUPON_NOT_APPLICABLE`, 319,60 dá `50.00`.
  - Pix com `BUY3PAY2` de desconto zero não dá o desconto do Pix (ajuste `0.00`).
  - Teto do seguro no Norte em `30.00`, e 12× por `120.91`, conferido pela fórmula.

Pelo caminho que o site realmente usa, seis dos dez exemplos falham. A causa é uma só, a leitura da TransLog (C5). O cálculo e as fronteiras conferem em tudo o que foi testado.
Conclusão: em parte

**C4, as recusas.** Recebem o código e o status pedidos:
- todos os casos de `INVALID_ORDER` (carrinho vazio ou ausente, item sem SKU, SKU repetido, quantidade 0, −1 ou ausente, CPF, e-mail ou `customer` ausente);
- `UNKNOWN_PRODUCT` e `PRODUCT_DISCONTINUED`;
- nível, região, entrega e pagamento **ausentes**;
- CEP ausente ou `"123"`;
- motoboy acima de 5 kg;
- `INVALID_INSTALLMENTS` (Pix 2×, cartão 0× e 13×);
- boleto acima de R$ 1.000 (`PAYMENT_METHOD_UNAVAILABLE`).

Não conferem:
- **Valor que não existe** em `clubTier` (`PLATINUM`), `region` (`XX`), `shippingMethod` (`DRONE`), `coupon` (`XYZ`, `""`, `welcome10`) e `paymentMethod` (`CASH`). Todos dão `400 {"timestamp":…,"status":400,"error":"Bad Request",…}`. Os campos são enums no DTO (`src/main/java/com/loja/dto/OrderRequest.java:10-15`), a desserialização falha antes da regra e o `GlobalExceptionHandler` não trata esse erro.
- **Falta de estoque no resumo e no registro** (`BON-001` ×1, `TEN-001` ×11) dá `409 INSUFFICIENT_STOCK`, não `422`. O mesmo código serve às duas tabelas (`src/main/java/com/loja/exception/ErrorCode.java:9`).
- **`CARRIER_UNAVAILABLE` nunca sai** (ver C5).
- **Dois problemas ao mesmo tempo:**
  - SKU repetido + SKU desconhecido dá `INVALID_ORDER`; certo.
  - SKU desconhecido + região ausente dá `UNKNOWN_PRODUCT`; certo.
  - CEP inválido + pagamento ausente dá `INVALID_ZIP_CODE`; certo.
  - SKU desconhecido + região `XX` dá `400`; esperado `UNKNOWN_PRODUCT`.
  - Fora de linha + cupom `XYZ` dá `400`; esperado `PRODUCT_DISCONTINUED`.
  - Motoboy acima de 5 kg + cupom `XYZ` dá `400`; esperado `SHIPPING_METHOD_UNAVAILABLE`.
  - `BON-001` sem estoque + nível ausente dá `409`; esperado `422 INSUFFICIENT_STOCK`.

Nas operações do pedido:
- `ORDER_NOT_FOUND` (404) funciona no `GET`, no pagamento, na entrega e no cancelamento.
- A ordem falha com o pedido inexistente e o campo faltando: `POST /orders/999999/payment` com `{}` e `POST /orders/999999/shipment` com `{}` dão `422 INVALID_ORDER`; esperado `404 ORDER_NOT_FOUND`. A causa é que o campo é conferido antes de buscar o pedido (`src/main/java/com/loja/service/OrderService.java:62`, `:144`).
- Pagamento sem corpo dá `400` genérico.

`CUSTOMER_NOT_FOUND`, inclusive nos avisos de um CPF desconhecido, e `NOTIFICATION_NOT_FOUND` conferem.

A ordem da tabela vale quando os campos vêm ausentes. Mas a regra 4 sai com o status errado, a 10 nunca sai, e as regras 5, 6, 8, 11 e 13 dão `400` genérico com valor inválido.
Conclusão: em parte

**C5, a TransLog.** A chamada sai certa: o stub registrou `GET /v2/quote?zip=01310100&weight_g=1800&service=EXP`, e `service=STD` na econômica. Os problemas:
- **A resposta do enunciado não é lida.** Com `{"price_cents":3310,"eta_hours":41}` vem `422 SHIPPING_METHOD_UNAVAILABLE`. O DTO interno tem `priceCents`/`etaHours` sem mapeamento para os nomes da TransLog (`src/main/java/com/loja/integration/TransLogClient.java:44-45`), então o preço chega nulo.
- **Fora do ar** (stub devolvendo `500`, ou stub parado) também dá `422 SHIPPING_METHOD_UNAVAILABLE`, não `503 CARRIER_UNAVAILABLE`. O `isAvailable` engole a exceção e devolve `false` (`src/main/java/com/loja/service/shipping/TransLogShipping.java:30`).
- **Sem tempo limite.** Com o stub atrasando 10 s, o resumo voltou **200 depois de 20 s**: são duas consultas, uma em `isAvailable` e outra em `calculateShipping`, sem limite nenhum. O parâmetro `timeoutMs` é recebido e não é usado (`TransLogClient.java:16`).
- **O que funciona:** com a resposta em camelCase, a conversão de centavos para reais e de horas para dias, arredondando para cima, mais os dias extras de Norte e Nordeste, confere (Ex1 prazo 5, Ex7 prazo 6). Com a TransLog parada, motoboy e retirada continuam respondendo `200`.
Conclusão: não segue

**C6, o ciclo do pedido.**
- **O que confere.**
  - Registrar não baixa o estoque (CAM 50 → 50). Pagar baixa (50 → 48), põe o crédito no saldo (`3.20`) e gera `PAYMENT_APPROVED`.
  - Enviar com `trackingCode` gera `ORDER_SHIPPED`. Depois disso, entregar dá `DELIVERED`.
  - Cancelar um pago devolve o estoque (45 → 48) e o crédito (7,99 → 3,20) e avisa `ORDER_CANCELLED`. Cancelar um não pago não mexe em nada e avisa.
  - Fora de ordem dá `409 OPERATION_NOT_ALLOWED`: entregar pago, enviar duas vezes, cancelar enviado ou entregue, enviar não pago ou cancelado, pagar cancelado.
  - Marcar aviso como lido dá `READ`; repetir continua `READ`; a lista sai do mais antigo para o mais novo.
  - Exatamente 48 h depois do `createdAt` ainda é aceito. `WELCOME10` volta a valer quando o único pedido do CPF foi cancelado.
- **O prazo é truncado em horas.** `paidAt` em 48 h 1 s e em 48 h 59 min foram **aceitos (`200 PAID`)**, porque a conta é `elapsed.toHours() > 48` (`OrderService.java:97`). Só com 49 h 1 min veio `409 ORDER_EXPIRED`.
- **A recusa por prazo não fica gravada.** Depois desse `409 ORDER_EXPIRED`, o `GET` mostrou o pedido ainda `AWAITING_PAYMENT`, e nenhum `ORDER_CANCELLED` apareceu: o id do aviso pulou de 10 para 12, sinal do rollback. Um pagamento seguinte com outro `transactionId` foi **aceito** e baixou o estoque. A causa é que o cancelamento e o aviso são gravados e depois a exceção derruba a transação inteira (`OrderService.java:98-106`).
- **Cancelar um pedido já cancelado** dá `200` e gera um segundo `ORDER_CANCELLED` (avisos 5 e 6 do pedido 3).
Conclusão: em parte

**C7, o aviso de pagamento repetido.**
- **Um depois do outro.**
  - Quando a primeira vez deu certo, o mesmo `TX-…` repetido, na mesma máquina e na outra, devolve `200` com o mesmo corpo. O estoque baixa uma vez só (FON 5 → 4), o crédito fica `10.00` e sai um único `PAYMENT_APPROVED`.
  - Outro `transactionId` num pedido pago dá `409 OPERATION_NOT_ALLOWED`.
  - O problema está na recusa repetida. O pedido 14 recebeu `409 INSUFFICIENT_STOCK` na 8081. Depois o estoque voltou (cancelei outro pedido), e **o mesmo aviso, repetido na 8082, deu `200 PAID` e baixou o estoque**. A mesma repetição na 8081 continuou respondendo `409 INSUFFICIENT_STOCK` para um pedido já pago. A memória dos avisos é um `HashMap` em cada processo (`OrderService.java:29`): não é dividida entre as máquinas e some no reinício.
- **Ao mesmo tempo, mesmo `transactionId`** (5 por rodada, alternando 8081 e 8082, 5 rodadas).
  - Os efeitos saem certos: em todas as rodadas o estoque baixou uma vez (−2), o crédito entrou uma vez (`7.99`) e saiu um único `PAYMENT_APPROVED`, graças ao `@Version` do pedido (`src/main/java/com/loja/domain/Order.java:42`).
  - As respostas não: em cada rodada, **1 × `200 PAID` e 4 × `500 Internal Server Error`**, com `ObjectOptimisticLockingFailureException` nos logs. Esperado: as cinco iguais à primeira.
- **Ao mesmo tempo, `transactionId` diferentes** (5 rodadas): só um foi aceito, como deve, e o estoque e o crédito saíram certos. Os outros quatro deram `500`, e não `409 OPERATION_NOT_ALLOWED`.

O essencial (não baixar o estoque nem dar o crédito duas vezes) foi atendido em todos os testes. As respostas à concorrência e a repetição de uma recusa não foram.
Conclusão: em parte

**C8, a última peça.** Fiz sete rodadas com as duas máquinas: três de 2 pedidos para 1 unidade e quatro de 6 pedidos para 2 unidades (`MEI-001` e `CAM-001`).
- **O estoque fica certo.** Em todas, passaram exatamente tantos pagamentos quantas unidades havia, o estoque terminou em 0 e nunca ficou negativo (o produto é lido com `PESSIMISTIC_WRITE`).
- **Quem perde recebe a resposta certa**, `409 INSUFFICIENT_STOCK`.
- **O pedido de quem perde não é cancelado.** Ele continua `AWAITING_PAYMENT` (por exemplo, `statuses=[PAID AWAI AWAI AWAI PAID AWAI]`), e **nenhum `ORDER_CANCELLED`** foi gerado (`cancel-notifs=0` em todas as rodadas). É o mesmo rollback do C6 (`OrderService.java:111-120`). O mesmo aconteceu num teste em sequência, numa máquina só: o pedido 14 perdeu a `MOC-001` e ficou aguardando, sem aviso.
Conclusão: em parte

**C9, os dados não se perdem.** Tirei uma foto antes, derrubei as duas máquinas, subi a 8081 de novo e comparei. Bateram:
- o pedido 2 `DELIVERED`, com `trackingCode` `BR123` e total `153.33`;
- o pedido 14 `PAID`;
- os estoques (CAM 28, MOC 0);
- o saldo do cliente 40000000001 (`3.20`);
- os seis avisos com os status `READ`/`NEW`.

O `diff` saiu vazio, e o catálogo não foi semeado de novo (MEI continuou 95). Só a memória dos avisos repetidos se perde, e isso está no C7.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` da cópia saiu com código 0. Rodaram 6 testes, 3 em `MoneyTest` e 3 em `CheckoutServiceTest`, todos passando, com H2 em memória.
Conclusão: segue

## O que mudaria primeiro

1. **Consertar o cliente da TransLog.** Mapear `price_cents`/`eta_hours` (`@JsonProperty`), configurar o tempo limite de 2 s e deixar a falha de comunicação chegar como `CARRIER_UNAVAILABLE`, em vez de engolida no `isAvailable`. É uma correção pequena, que destrava a econômica e a expressa, seis dos dez exemplos do financeiro e a regra 10 da tabela.
2. **Gravar as recusas do pagamento e a idempotência no banco.** O cancelamento por prazo ou por falta de estoque, e o aviso, precisam ser gravados mesmo quando a resposta é um erro: com `noRollbackFor`, ou devolvendo o resultado em vez de lançar a exceção dentro da transação. O resultado do primeiro aviso de cada `transactionId` deveria ficar numa tabela, e o pedido deveria ser lido com trava. Isso resolve o pedido vencido que continua pagável, o perdedor sem cancelamento e sem aviso, os `500` da concorrência e a repetição que muda de resposta entre as máquinas. A comparação do prazo deveria ser com `Duration` inteira, não com `toHours()`.

## Dúvidas

- **Cancelar um pedido já cancelado.** O enunciado diz que o cliente pode cancelar "enquanto o pedido não foi enviado" e não fala do pedido já cancelado. Li a repetição como operação fora de ordem (`409`). Seja qual for a leitura, o aviso duplicado contraria o "cada cancelamento gera um aviso". Contei isso no C6, junto com os outros problemas do item.
- **`MINUS50` com exatamente R$ 300,00.** Não dá para montar esse valor com os preços do catálogo (todos terminam em ,90). Testei 299,70 e 319,60.
- **Variante em camelCase da TransLog.** Usei essa resposta, que não é o formato do enunciado, só para ver se o resto da conta estava certo. Não a contei como acerto da integração.
- **GOLD com `FREESHIPPING` na econômica ou na expressa** (frete já zerado pelo clube). O código recusa, porque exige frete positivo (`src/main/java/com/loja/service/coupon/FreeShippingCoupon.java:17`). O enunciado não decide esse caso, e não o testei pela API.
- **O formato do `createdAt`.** Na resposta do registro ele vem com nanossegundos (`…00.680552443Z`), e nas consultas seguintes com microssegundos (`…00.680552Z`). O enunciado dá o formato sem fração. Não pesou na conclusão.
- **O teste das 48 h exatas** usou o `createdAt` da resposta truncado em microssegundos. Pode ter ficado até 1 µs antes das 48 h; isso não muda o resultado.

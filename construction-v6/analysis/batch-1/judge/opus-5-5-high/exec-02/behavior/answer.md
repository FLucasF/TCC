# Comportamento

## Visão geral

O serviço compila e sobe em cerca de 4 s com o banco vazio. Cria as tabelas e o catálogo sozinho, e o caminho mais simples funciona: registrar, pagar, enviar, entregar e cancelar, com o estoque baixando no pagamento e voltando no cancelamento. Os problemas, do mais grave ao menos grave:

1. **Os dados não sobrevivem.** O `ddl-auto: create-drop` (`src/main/resources/application.yml:11`) apaga tudo a cada reinício. Pior: ao subir a segunda máquina, ela apaga os pedidos da primeira.
2. **Não há proteção nenhuma contra concorrência.** Seis pagamentos disputando duas unidades passaram todos, quatro vezes seguidas. Avisos de pagamento repetidos e simultâneos geram vários `PAYMENT_APPROVED`, e cinco `transactionId` diferentes no mesmo pedido são todos aceitos.
3. **Seis dos dez exemplos do financeiro não batem:**
   - o cartão de 4× a 12× sempre devolve 500;
   - o desconto do Pix é dado mesmo com cupom;
   - o GOLD não paga o motoboy;
   - o FREESHIPPING é sempre recusado;
   - o WELCOME10 não confere a primeira compra.
4. **As recusas que mudam o pedido são desfeitas.** O pedido expirado e o pedido sem estoque no pagamento devolvem o código certo, mas o rollback desfaz o cancelamento e o aviso. O prazo de 48 h também aceita até 48 h 59 min.
5. **A tabela de erros só funciona em parte.** Valores de enum inválidos dão `400` do Spring, `CARRIER_UNAVAILABLE` nunca aparece, o boleto acima de R$ 1.000,00 é aceito e a ordem de conferência não é a do enunciado.

## Item a item

**C1, o serviço sobe.** A cópia foi compilada com `mvn -q package -DskipTests` (35 s) e subiu com `java -jar target/order-service-1.0.0.jar --server.port=8081`. O log diz `Started OrderServiceApplication in 3.862 seconds`. As tabelas são criadas pelo Hibernate (`application.yml:11`) e o catálogo é gravado pelo `DataInitializer.java:18-21`, que só semeia quando a tabela está vazia. O problema de usar `create-drop` aparece em C9. Uma observação: o `pom.xml:19` usa Spring Boot 3.3.0, e não o 4.1.1 que o enunciado pede.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Todos voltaram `200` com nome, preço, peso e estoque iguais aos do anexo; por exemplo, `{"sku":"REL-001",...,"stock":8,"discontinued":true}` e `BON-001` com `stock: 0`. Um SKU inexistente deu `404 {"error":"PRODUCT_NOT_FOUND"}`.
Conclusão: segue

**C3, o resumo.** Usei `POST /checkout/summary` com o stub da TransLog respondendo o que cada exemplo diz. O stub registrou `weight_g=1800&service=EXP` no exemplo 1, que está certo.

| exemplo | resultado | o que veio |
|---|---|---|
| 1 | ✗ | `paymentAdjustment -20.60` e `finalTotal 391.47`; esperado 0,00 e 412,07. O Pix foi aplicado mesmo com cupom (`CheckoutService.java:343-344`). |
| 2 | ✗ | `500 Internal Server Error`. O log mostra `ArithmeticException: Invalid operation at BigDecimal.pow`, por causa do `pow(-numInstallments)` em `CheckoutService.java:353-355`. Cartão em 4× e 12× também deu 500, então todo parcelamento com juros quebra. |
| 3 | ✗ | `shippingCost 0` e `finalTotal 361.29`; esperado 18,00 e 379,29. O GOLD fica sem frete também no motoboy (`CheckoutService.java:56-60`). |
| 4 | ✓ | Confere. |
| 5 | ✗ | Ajuste `-9.75` e final `185.19`; esperado 0,00 e 194,94. Mesmo problema do Pix com cupom. O teto de 30% do cupom deu certo (71,91). |
| 6 | ✗ | `422 COUPON_NOT_APPLICABLE`. O FREESHIPPING lança erro sempre (`CheckoutService.java:227-228`). Com a expressa também deu 422. |
| 7 | ✓ | Confere, inclusive o brinde, o seguro no limite (30,00) e o prazo 6. |
| 8 | ✓ | `422 COUPON_NOT_APPLICABLE`, como esperado. |
| 9 | ✗ | `200`, com `couponDiscount 15.98`, para um CPF que já tinha pedido pago e entregue. O WELCOME10 não consulta o histórico (`CheckoutService.java:220-221`). |
| 10 | ✓ | `422 SHIPPING_METHOD_UNAVAILABLE`, como esperado. |

As fronteiras conferem:
- **Meio para o par, no Pix:** MEI×1, retirada, Pix, SOUTHEAST → ajuste `-1.00`, final `19.10` (1,005 → 1,00).
- **Meio para o par, no seguro:** MEI×10, MIDWEST → seguro `2.98` (2,985 → 2,98).
- **Motoboy no limite:** MEI×50 (5,00 kg) → `200` com frete 18,00; MEI×51 → `422 SHIPPING_METHOD_UNAVAILABLE`.
- **MINUS50:** com 299,10 → `422 COUPON_NOT_APPLICABLE`; com 319,00 → desconto 50,00.

Além disso, os valores zerados saem como `0` e não `0.00` (por exemplo, `"couponDiscount":0`), contra o formato de duas casas do anexo.

São quatro exemplos certos em dez, e o parcelamento com juros não funciona nunca.
Conclusão: não segue

**C4, as recusas.** Os casos simples, um problema por vez, devolvem o código e o status certos:
- carrinho vazio, SKU duplicado, quantidade 0 e e-mail ausente → `422 INVALID_ORDER`;
- `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED` e `INSUFFICIENT_STOCK` (BON-001);
- nível, entrega e pagamento ausentes;
- CEP com 7 dígitos;
- cupom inexistente e cupom em minúsculas;
- Pix em 2×, cartão em 13×;
- `ORDER_NOT_FOUND`, `CUSTOMER_NOT_FOUND` e `NOTIFICATION_NOT_FOUND`.

O que falha:
- **Enum inválido:** `clubTier:"PLATINUM"`, `region:"MARS"`, `shippingMethod:"DRONE"` e `paymentMethod:"CASH"` dão `400` com o corpo padrão do Spring (`{"timestamp":...,"status":400,"error":"Bad Request"}`). Os campos são enums no DTO (`CheckoutSummaryRequest.java:15-20`), e o Jackson recusa antes do serviço.
- **Cupom vazio:** `coupon:""` → `200`. O texto vazio é tratado como "sem cupom" (`CheckoutService.java:196`).
- **Boleto acima do limite:** JAQ×3 no boleto, com total de 1.184,89 → `200`. Não existe a verificação de R$ 1.000,00.
- **TransLog fora do ar:** devolve 422, nunca `503 CARRIER_UNAVAILABLE` (detalhes em C5).
- **Ordem de conferência:** cinco dos seis pedidos com dois problemas devolveram o código errado.

| pedido com dois problemas | esperado | veio |
|---|---|---|
| SKU inexistente + Pix em 2× | `UNKNOWN_PRODUCT` | `INVALID_INSTALLMENTS` |
| produto fora de linha + nível ausente | `PRODUCT_DISCONTINUED` | `INVALID_CLUB_TIER` |
| cupom inexistente + Pix em 2× | `INVALID_COUPON` | `INVALID_INSTALLMENTS` |
| motoboy acima de 5 kg + cupom inexistente | `SHIPPING_METHOD_UNAVAILABLE` | `INVALID_COUPON` |
| sem estoque + CEP inválido | `INSUFFICIENT_STOCK` | `INVALID_ZIP_CODE` |

O motivo é que `validateRequest` (`CheckoutService.java:122-152`) confere nível, região, CEP, entrega, pagamento e parcelas antes dos produtos, e o cupom antes da entrega (`CheckoutService.java:41-42` contra `:50-63`).
Conclusão: em parte

**C5, a TransLog.** A tradução da resposta está certa:
- 3310 centavos → 33,10;
- 41 h → 2 dias, 48 h → 2, 49 h → 3 (+3 no Norte = 6), 1 h → 1;
- os dias extras do Norte e do Nordeste;
- `weight_g` em gramas e `STD`/`EXP` corretos;
- `ZIP_NOT_SERVED` → `422 SHIPPING_METHOD_UNAVAILABLE`;
- o tempo limite é respeitado: o stub demorou 3 s e a chamada voltou em 2,013 s.

Com a TransLog fora do ar, o motoboy (`200`, frete 18,00) e a retirada (`200`) continuam funcionando.

Mas toda falha da TransLog vira `422 SHIPPING_METHOD_UNAVAILABLE`: o `500`, o corpo inválido, outro `error` e o tempo esgotado. O cliente devolve `Optional.empty()` em todos os casos (`TransLogClient.java:45-49,61-63`), e o serviço traduz isso para 422 (`CheckoutService.java:279-281`). `CARRIER_UNAVAILABLE` não existe no código. O GOLD na expressa também recebe 422 com a TransLog fora, porque a cotação é feita mesmo sem cobrar frete.
Conclusão: em parte

**C6, o ciclo do pedido.** O que confere:
- registro → `201 AWAITING_PAYMENT`, sem mexer no estoque de CAM (50 → 50);
- envio e entrega de pedido não pago → `409 OPERATION_NOT_ALLOWED`;
- pagamento sem `paidAt` → `422 INVALID_ORDER`;
- pagamento → `PAID`, estoque 50 → 48, saldo 3,20;
- envio sem `trackingCode` → `422`; envio com código → `SHIPPED`; cancelamento depois de enviado → `409`; entrega → `DELIVERED`; segunda entrega → `409`;
- cancelamento de um pedido pago: MEI 97 → 100, saldo 1,19 → 0,00 e aviso `ORDER_CANCELLED`;
- cancelamento de um não pago: estoque e saldo intactos, com aviso;
- marcar um aviso como lido → `READ`, e repetir mantém `READ`.

O que falha:
- **O cliente não existe depois de registrar o pedido.** `GET /customers/{cpf}` e `/notifications` devolvem `404 CUSTOMER_NOT_FOUND`. O cliente só é criado no pagamento (`OrderService.java:137-150`).
- **O prazo de 48 h aceita até 48 h 59 min.** Com `paidAt` = registro + 48 h 30 min veio `200 PAID`, porque a conta usa horas inteiras (`OrderService.java:86`). Com + 49 h veio `409 ORDER_EXPIRED`, mas o pedido continuou `AWAITING_PAYMENT` e não saiu aviso: o `save(CANCELLED)` é desfeito pelo rollback da exceção (`OrderService.java:88-91`). Depois disso, outro `transactionId` com `paidAt` dentro do prazo foi aceito (`200 PAID`).
- **Faltar estoque no pagamento não cancela o pedido.** Com MOC já esgotado, o segundo pagamento deu `409 INSUFFICIENT_STOCK`, mas o pedido ficou `AWAITING_PAYMENT`, o cliente não recebeu aviso (`404 CUSTOMER_NOT_FOUND`), e o pagamento com outro TX passou depois que o estoque voltou (`OrderService.java:113-117`, mesmo rollback).
- **Um pedido cancelado pode ser cancelado de novo.** Veio `200` e um segundo aviso `ORDER_CANCELLED` (`OrderService.java:214` só bloqueia `SHIPPED` e `DELIVERED`).
- **`trackingCode` some da resposta antes do envio,** em vez de vir `null` (`application.yml:22`, `non_null`).
Conclusão: em parte

**C7, o aviso de pagamento repetido.** Os testes em sequência conferem: o mesmo `TX-R1` duas vezes deu respostas idênticas byte a byte (`cmp`), o estoque baixou uma vez (48 → 47), o crédito ficou em 1,60 e saiu um `PAYMENT_APPROVED` só. Outro TX num pedido pago → `409 OPERATION_NOT_ALLOWED`. Mas o mesmo TX sem `paidAt` num pedido pago devolve `200` em vez de `422`, porque a verificação de campos vem depois da de situação (`OrderService.java:71-82`).

Ao mesmo tempo, com cinco avisos divididos entre a 8081 e a 8082, nada confere:
- **Cliente novo:** o estoque e o crédito saíram certos, mas as respostas foram misturadas: `4×200 + 1×500` numa rodada e `1×200 + 4×500` nas outras. O 500 é `duplicate key value violates unique constraint "customers_pkey"`, ou seja, só a chave do cliente recém-criado barrou as duplicatas.
- **Cliente que já existia:** quatro rodadas com `5×200` e cinco `PAYMENT_APPROVED` para o mesmo pedido. O estoque (−2) e o crédito (+0,80) saíram certos apenas porque cada transação leu o mesmo valor e gravou o mesmo resultado (lost update). Não há lock nem `@Version` (`OrderService.java:63,133-134`).
- **Cinco `transactionId` diferentes juntos:** três rodadas com `5×200` e cinco avisos. Um dos TX "aceitos", reenviado depois, deu `409`.

A parte em sequência funciona; a parte simultânea falha em tudo o que o item pede.
Conclusão: em parte

**C8, a última peça.** Disparei pagamentos simultâneos, alternando 8081 e 8082, cada pedido de um cliente diferente.

| rodada | disputa | pagamentos `200` | estoque | `ORDER_CANCELLED` |
|---|---|---|---|---|
| MOC-001 | 2 pedidos, 1 unidade | 2 | 1 → 0 | — |
| TEN-001 | 6 pedidos, 2 unidades | 6 de 6, todos `PAID` | 2 → 0 | 0 |
| FON-001 | 6 pedidos, 2 unidades | 6 de 6 | 2 → 1 | 0 |
| JAQ-001 | 6 pedidos, 2 unidades | 6 de 6 | 2 → 1 | 0 |
| CAM-001 | 6 pedidos, 2 unidades | 6 de 6 | 2 → 1 | 0 |

Ninguém recebeu `409 INSUFFICIENT_STOCK`. O estoque não ficou negativo, mas só por causa do lost update: ele chega a ficar com 1 unidade depois de vender 6 de 2. A causa é ler, conferir e gravar o estoque sem lock (`OrderService.java:112-113,133-134`, `Product.java` sem `@Version`).
Conclusão: não segue

**C9, os dados não se perdem.** Registrei e entreguei um pedido (`33006713…`), com CAM em 47 e o cliente `50000000001` com saldo 3,20. Depois de derrubar (SIGTERM) e subir de novo: `GET /orders/33006713…` → `404 ORDER_NOT_FOUND`, CAM voltou a 50 e o cliente deu `404 CUSTOMER_NOT_FOUND`.

Pior: com a 8081 no ar, registrei o pedido `ea052c53…` e subi a 8082 no mesmo banco. Logo depois, o pedido sumiu nas duas máquinas (`404`). A causa é `ddl-auto: create-drop` (`application.yml:11`): toda subida apaga e recria as tabelas.
Conclusão: não segue

**C10, os testes do próprio agente.** Rodei `mvn -q verify` numa cópia, sem `DB_URL`, `DB_USER` e `DB_PASSWORD`: saída 0 e 10 testes, todos verdes (`OrderServiceApplicationTest` 1, `CheckoutServiceTest` 4, `CurrencyUtilsTest` 5). O contexto sobe em H2 com o perfil `test`.
Conclusão: segue

## O que mudaria primeiro

1. **Guardar os dados de verdade e travar o estoque.** Trocar o `create-drop` por um esquema versionado (Flyway, ou pelo menos `update`), para que nem um reinício nem a segunda máquina apaguem nada. No pagamento, travar o pedido e os produtos (`SELECT … FOR UPDATE`, ou um `UPDATE … SET stock = stock - ? WHERE stock >= ?` atômico) e só aceitar a transição se o pedido ainda estiver `AWAITING_PAYMENT`. Isso resolve de uma vez C7, C8 e C9, que são as falhas que o enunciado aponta como prejuízo real (a venda do que não tinha e o pagamento contado duas vezes).
2. **Corrigir as regras do resumo e os cancelamentos.**
   - Juros do cartão com uma potência que funcione (`1 / (1+i)^n`).
   - Pix sem desconto quando há cupom.
   - GOLD pagando o motoboy.
   - FREESHIPPING e o WELCOME10 de primeira compra implementados.
   - O limite do boleto.
   - O 503 da TransLog e a ordem da tabela de erros.
   - Gravar o cancelamento por prazo ou por falta de estoque fora da transação que é desfeita, para que o pedido fique `CANCELLED` e o aviso saia.

## Dúvidas

- **Preço fracionário da TransLog.** Com `price_cents: 3310.5`, o serviço truncou para 33,10. O enunciado não diz o que fazer com centavos fracionários. Não considerei isso falha, só registro o que vi.
- **Cancelar um pedido já cancelado.** O enunciado não lista esse caso explicitamente. Li "o cliente pode cancelar enquanto o pedido não foi enviado" junto com "as operações fora de ordem são recusadas" como pedindo `409`, e contei isso em C6 como um problema menor. O peso dele na conclusão é pequeno.
- **Estoque e crédito certos na concorrência de C7.** Os números saíram certos, mas por lost update e não por proteção. Contei esses números como observados (eles conferem), mas a conclusão vem dos avisos multiplicados, dos 500 e dos TX diferentes aceitos.
- **Formato dos valores zerados.** `0` em vez de `0.00` é diferente do formato do anexo, mas o número é o mesmo. Não deixei isso decidir nenhum exemplo de C3.

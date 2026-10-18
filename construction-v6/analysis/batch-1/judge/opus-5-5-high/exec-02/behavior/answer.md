# comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio em cerca de 3,5 s, carrega o catálogo certo e acerta o caminho feliz do ciclo do pedido: o estoque baixa só no pagamento, o cancelamento devolve estoque e crédito, os avisos funcionam e o aviso repetido devolve a mesma resposta. Fora disso, os problemas são graves. Em ordem de gravidade:

1. **Nada fica guardado.** O `ddl-auto: create-drop` apaga todos os dados quando o serviço reinicia. Pior: quando a segunda máquina sobe, ela apaga os dados da primeira.
2. **A última peça é vendida duas vezes.** Com duas máquinas, isso aconteceu em 20 de 20 rodadas.
3. **Só 4 dos 10 exemplos do financeiro conferem.** O Pix dá desconto mesmo com cupom; o GOLD não paga o motoboy; o FREESHIPPING é sempre recusado; o WELCOME10 não confere a primeira compra; e todo cartão de 4× a 12× devolve 500.
4. **A TransLog fora do ar nunca vira `CARRIER_UNAVAILABLE`.** Volta como `SHIPPING_METHOD_UNAVAILABLE` (422).
5. **O pedido expirado ou sem estoque não é cancelado.** A transação desfaz a mudança, então o pedido continua `AWAITING_PAYMENT` e o cliente não recebe aviso.
6. **A ordem dos erros não é a do enunciado**, e valores de código inexistentes devolvem 400 do Spring em vez do 422 pedido.

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/run/app`, compilei com `mvn -q package -DskipTests` (35 s) e subi com `TRANSLOG_URL=http://localhost:9090 java -jar target/order-service-1.0.0.jar --server.port=8081`. O log mostra `Started OrderServiceApplication in 3.462 seconds`. O Hibernate criou as quatro tabelas (`customers`, `notifications`, `orders`, `products`; os avisos "table … does not exist, skipping" vêm do drop inicial) e o catálogo foi carregado (`src/main/java/com/loja/orderservice/config/DataInitializer.java:487-559`). O fato de ele apagar o banco ao subir é tratado em C8 e C9.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Nome, preço, peso, estoque e `discontinued` batem com o anexo, por exemplo `{"sku":"REL-001",…,"price":129.90,"weightKg":0.20,"stock":8,"discontinued":true}`. Um resumo com `REL-001` devolve `422 {"error":"PRODUCT_DISCONTINUED"}`, e `GET /products/XXX-999` devolve `404 PRODUCT_NOT_FOUND`.
Conclusão: segue

**C3, o resumo.** Rodei os dez exemplos em `POST /checkout/summary`, com o stub respondendo o que cada exemplo diz:
- **E4, E7, E8 e E10 conferem** campo a campo.
- **E1 e E5 (cupom + PIX):** a resposta traz `paymentAdjustment` −20,60 e total 391,47 (o certo é 0,00 e 412,07) e, no E5, −9,75 e total 185,19 (o certo é 0,00 e 194,94). O desconto do Pix é aplicado sem olhar o cupom (`src/main/java/com/loja/orderservice/service/CheckoutService.java:343-344`).
- **E2 (cartão 6×):** `500 Internal Server Error`. No log aparece `ArithmeticException: Invalid operation at BigDecimal.pow`, porque o código chama `pow(-numInstallments)` (`CheckoutService.java:354`). O mesmo 500 acontece em 4× e em 12×.
- **E3 (GOLD + COURIER):** o frete vem 0 e o total 361,29; o certo é 18,00 e 379,29. O ramo do GOLD nunca cobra frete (`CheckoutService.java:56-60`).
- **E6 (FREESHIPPING):** volta `422 COUPON_NOT_APPLICABLE` em vez do resumo. Esse cupom sempre lança erro (`CheckoutService.java:227-228`). A TransLog nem chega a ser consultada.
- **E9:** registrei e paguei um pedido para o CPF 10000000009 e depois pedi WELCOME10. A resposta foi `200` com desconto de 15,98 (o certo é `422 COUPON_NOT_APPLICABLE`). O código não confere a primeira compra (`CheckoutService.java:220-221`). `POST /orders` com o mesmo corpo também registra o pedido (201).

Fronteiras:
- Motoboy com exatamente 5,00 kg (MEI×50) é aceito com frete 18,00; com 5,10 kg vem `SHIPPING_METHOD_UNAVAILABLE`. Confere.
- MINUS50 com 299,40 é recusado e com 319,30 dá 50,00. Confere.
- O limite de 30% confere: BUY3PAY2 com CAM×3 dá 71,91, que é 30% de 239,70.
- "Meio para o par" confere: seguro MIDWEST sobre 199,00 dá 2,98 (2,985) e NORTH sobre 119,40 também dá 2,98.

O resumo e o pedido registrado deram os mesmos valores (E9). Um detalhe de formato: os zeros saem como `0` e não `0.00` (por exemplo `"couponDiscount":0`, de `CheckoutService.java:48,215`).

Com 6 dos 10 exemplos errados e o cartão com juros fora do ar, o principal do item falha.
Conclusão: não segue

**C4, as recusas.** Casos simples:
- **Conferem:** carrinho vazio, item sem SKU, SKU repetido, quantidade 0 ou ausente, CPF ausente e `customer` ausente dão `422 INVALID_ORDER`. Também conferem `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` (BON-001), nível, pagamento ou CEP ausentes, cupom inexistente ou em minúsculas (`INVALID_COUPON`), e PIX 2×, cartão 13× e 0× (`INVALID_INSTALLMENTS`).
- **Valor inexistente:** `"clubTier":"PLATINUM"`, `"region":"MARS"`, `"shippingMethod":"DRONE"` e `"paymentMethod":"CASH"` voltam `400` com o corpo padrão do Spring (`{"timestamp":…,"status":400,"error":"Bad Request"}`) em vez de 422 com o código. Os campos são enums no DTO e o Jackson recusa antes do serviço (`src/main/java/com/loja/orderservice/dto/CheckoutSummaryRequest.java:300-305`).
- **Cupom vazio:** `"coupon":""` é aceito (200) em vez de `INVALID_COUPON` (`CheckoutService.java:196`).
- **Boleto acima de R$ 1.000:** JAQ×3 com BANK_SLIP (total 1.184,89) volta 200 em vez de `PAYMENT_METHOD_UNAVAILABLE`. A regra não existe no código.

Pedidos com dois problemas (o primeiro da ordem é o esperado):

| problemas | esperado | veio |
|---|---|---|
| SKU desconhecido + pagamento ausente | UNKNOWN_PRODUCT | INVALID_PAYMENT_METHOD |
| sem estoque + CEP inválido | INSUFFICIENT_STOCK | INVALID_ZIP_CODE |
| cupom inexistente + PIX 2× | INVALID_COUPON | INVALID_INSTALLMENTS |
| motoboy >5 kg + FREESHIPPING | SHIPPING_METHOD_UNAVAILABLE | COUPON_NOT_APPLICABLE |
| CEP não atendido + cupom inexistente | SHIPPING_METHOD_UNAVAILABLE | INVALID_COUPON |
| MINUS50 abaixo de 300 + pagamento ausente | COUPON_NOT_APPLICABLE | INVALID_PAYMENT_METHOD |
| cupom inexistente + nível ausente | INVALID_CLUB_TIER | INVALID_CLUB_TIER ✓ |

A causa é que `validateRequest` confere nível, região, CEP, entrega, pagamento e parcelas antes de buscar os produtos (`CheckoutService.java:39-41,122-152`), e o cupom é conferido antes do frete (`CheckoutService.java:42-46` contra `:52`). `CARRIER_UNAVAILABLE` nunca é devolvido (ver C5).
Conclusão: em parte

**C5, a TransLog.** A tradução confere:
- Com `{"price_cents":999,"eta_hours":48}` vêm frete 9,99 e 2 dias; com 49 h, 3 dias; com 1 h no NORTHEAST, 3 dias (1 + 2).
- O stub recebeu `weight_g=1800&service=EXP` no E1 e `service=STD` no E2.
- `ZIP_NOT_SERVED` vira `422 SHIPPING_METHOD_UNAVAILABLE` (E10).
- Uma resposta que demora 3 s é cortada em 2,0 s.

O "fora do ar" não confere. A resposta HTTP 500, `{"error":"INTERNAL"}`, o corpo `not json`, a demora de 3 s e o stub desligado voltam todos `422 SHIPPING_METHOD_UNAVAILABLE` em vez de `503 CARRIER_UNAVAILABLE`. O cliente transforma toda falha em `Optional.empty()` (`src/main/java/com/loja/orderservice/integration/TransLogClient.java:452-454`), e o serviço trata isso como CEP não atendido (`CheckoutService.java:279-281`). Já `{}` com 200 dá `500` (NPE em `TransLogClient.java:443`).

Com o stub desligado, o motoboy (frete 18,00, prazo 0) e a retirada (prazo 1) continuam funcionando no resumo e no registro (201).
Conclusão: em parte

**C6, o ciclo do pedido.** O que confere:
- Pedido TEN×2: o estoque fica 10 depois do registro e cai para 8 depois do pagamento. O crédito SILVER de 10,00 entra no saldo.
- Enviar ou entregar antes de pagar, entregar um pedido pago, cancelar um enviado e entregar duas vezes dão `409 OPERATION_NOT_ALLOWED`. Envio sem `trackingCode` e pagamento sem `transactionId` dão `422 INVALID_ORDER`.
- Cancelar um pedido pago (GOLD, CAM×2): estoque 50→48→50, saldo 7,99→0,00, aviso `ORDER_CANCELLED`. Cancelar um não pago não mexe no estoque e gera aviso.
- Os avisos `PAYMENT_APPROVED`, `ORDER_SHIPPED` e `ORDER_CANCELLED` aparecem em ordem. `POST /notifications/{id}/read` dá `READ` e repetir continua `READ`.

O que falha:
- **O prazo de 48 h é contado em horas inteiras** (`src/main/java/com/loja/orderservice/service/OrderService.java:86-88`). `paidAt` = registro + 48 h 2 s e registro + 48 h 59 min são aceitos (PAID, estoque baixa). Só com 49 h 1 s vem `409 ORDER_EXPIRED`.
- **O pedido expirado não é cancelado.** Depois do `ORDER_EXPIRED`, `GET /orders/{id}` mostra `AWAITING_PAYMENT` e não há aviso (`CUSTOMER_NOT_FOUND`). A exceção é lançada dentro do `@Transactional` e desfaz a mudança (`OrderService.java:61,88-91`). Um pagamento posterior com outro `transactionId` foi aceito (PAID).
- **Falta de estoque no pagamento também não cancela.** Paguei FON×5 e depois FON×1: o segundo dá `409 INSUFFICIENT_STOCK`, mas o pedido continua `AWAITING_PAYMENT` e sem aviso (`OrderService.java:113-117`).
- **O cliente não passa a existir no registro.** Depois de `POST /orders`, `GET /customers/50000000001` e `/notifications` dão `404 CUSTOMER_NOT_FOUND`. O cliente só é criado no pagamento (`OrderService.java:137-150`).
- **Cancelar um pedido já cancelado** dá 200 e gera um segundo `ORDER_CANCELLED` (`OrderService.java:214-216`).
- **Detalhes de formato:** `trackingCode` é omitido em vez de `null` (`src/main/resources/application.yml:21`, `non_null`), e `createdAt` vem com nanossegundos no registro e microssegundos na consulta (`.636813629Z` contra `.636814Z`).
Conclusão: em parte

**C7, o aviso de pagamento repetido.** Para CAM×3 SILVER, enviei `TX-777` duas vezes. As respostas são idênticas byte a byte, o estoque vai 50→47→47, o saldo fica 4,79 e há um só `PAYMENT_APPROVED`. Outro `transactionId` em pedido pago ou cancelado dá `409 OPERATION_NOT_ALLOWED`.

A repetição de uma recusa não confere. Só a primeira resposta de sucesso é lembrada (o `transactionId` só é gravado quando dá certo, `OrderService.java:71-75,123`); uma recusa é recalculada a cada chamada:
- O `TX-FB`, recusado por falta de estoque, foi repetido depois de cancelar o pedido que havia esgotado o FON-001. Veio `200 PAID` e o estoque baixou de 5 para 4. O esperado era a mesma `INSUFFICIENT_STOCK`.
- O `ORDER_EXPIRED` repetido só coincide porque a conta dá o mesmo resultado de novo.
Conclusão: em parte

**C8, a última peça.** Subi a segunda máquina na 8082. Primeiro achado: ao subir, ela apagou o banco da primeira. Um pedido criado na 8081 antes passou a dar `ORDER_NOT_FOUND` nas duas máquinas.

Depois fiz 20 rodadas, cada uma com MOC-001 em estoque exatamente 1: dois pedidos registrados e os dois pagamentos disparados juntos, um em cada máquina. Nas 20, as duas respostas foram `200 PAID` e o estoque terminou em 0: duas peças vendidas com uma em estoque. Numa rodada anterior, também com estoque 1, uma passou e a outra levou `409 INSUFFICIENT_STOCK`.

O estoque nunca ficou negativo, mas só porque uma escrita apaga a outra. O código lê, confere e grava o estoque sem trava nem versão (`OrderService.java:112-135`; `src/main/java/com/loja/orderservice/domain/entity/Product.java` não tem `@Version`).
Conclusão: não segue

**C9, os dados não se perdem.** Antes de derrubar: pedido `4a551dd7…` PAID, CAM-001 com 47, FON-001 com 4, cliente 70000000001 com saldo 4,79. Derrubei com `kill` e subi de novo. Depois: `GET /orders/4a551dd7…` dá `404 ORDER_NOT_FOUND`, o cliente dá `404 CUSTOMER_NOT_FOUND`, o estoque volta a CAM 50 e FON 5, e os avisos sumiram. A causa é `ddl-auto: create-drop` (`src/main/resources/application.yml:11`).
Conclusão: não segue

**C10, os testes do próprio agente.** `mvn -q verify` na cópia termina com exit 0. Rodam 10 testes, todos passam: `OrderServiceApplicationTest` 1, `CheckoutServiceTest` 4, `CurrencyUtilsTest` 5.
Conclusão: segue

## O que mudaria primeiro

1. **Guardar os dados e travar o estoque.** Trocar o `create-drop` por um esquema que não apaga nada (Flyway ou, no mínimo, `update`). Fazer a baixa do estoque numa operação atômica no banco (`UPDATE … SET stock = stock - ? WHERE sku = ? AND stock >= ?`, conferindo as linhas afetadas) ou com trava pessimista. Sem isso, o serviço não roda em duas máquinas nem sobrevive a um reinício, exatamente os dois problemas que o lojista relatou.
2. **Corrigir a conta do resumo e as recusas no pagamento:**
   - Price com `pow` positivo e divisão;
   - Pix só sem cupom;
   - GOLD pagando o motoboy;
   - FREESHIPPING e WELCOME10 implementados;
   - boleto acima de R$ 1.000 recusado;
   - TransLog fora do ar como 503;
   - gravar o cancelamento por expiração ou falta de estoque fora da transação que é desfeita (ou sem lançar exceção dentro dela), lembrando a recusa para a repetição.

## Dúvidas

- **MINUS50 em exatamente R$ 300,00:** não dá para montar com este catálogo. Todos os preços terminam em ,90, e 300,00 exigiria uma combinação que não existe. Testei 299,40 (recusa) e 319,30 (aceita).
- **`price_cents` com casas decimais** (3310.75): o serviço trunca para 33,10. O anexo diz que o preço vem em centavos inteiros e não diz o que fazer com fração. Não contei contra.
- **Zeros como `0` em vez de `0.00`:** o valor numérico é o mesmo; tratei como detalhe de formato, não como erro de valor.
- **Mesmo `transactionId` sem `paidAt`** num pedido já pago devolve 200, porque a checagem de repetição vem antes da checagem de campos. Pela ordem da tabela, seria `422 INVALID_ORDER`. Não decidi contra, porque o enunciado também pede que a repetição devolva a mesma resposta.
- **Cancelar um pedido já cancelado:** o enunciado não diz explicitamente. Li como operação não permitida, e o aviso duplicado reforça que é um problema.
- **Fora desta dimensão:** o `pom.xml` usa Spring Boot 3.3.0, e não o 4.1.1 pedido.

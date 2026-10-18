# comportamento

## Visão geral

O serviço sobe com o banco vazio em menos de um segundo, cria as tabelas e carrega o catálogo. O ciclo do pedido funciona e nada se perde ao reiniciar. A disputa pela última peça, com pedidos de um item só, é bem resolvida pelo banco (`UPDATE … WHERE stock >= ?`): em todas as rodadas passaram exatamente tantos pagamentos quantas unidades havia. O estoque nunca ficou negativo, e quem perdeu recebeu `409 INSUFFICIENT_STOCK`, teve o pedido cancelado e foi avisado.

Os problemas, do mais grave para o menos grave:
1. **O cartão de 4x a 12x nunca funciona.** Sempre devolve `500`, porque `BigDecimal.pow(-n)` lança exceção. O Exemplo 2 falha no resumo e no registro.
2. **O resumo recusa o WELCOME10 para qualquer cliente**, porque roda sem conexão e trata todo cliente como "não é primeira compra". Assim o resumo e o pedido registrado dão resultados diferentes, justamente o que o enunciado proíbe. O Exemplo 1 falha no resumo.
3. **Pedido de dois itens perde estoque.** Quando o pedido é cancelado por falta do segundo item, a baixa do primeiro é gravada mesmo assim. Pagamentos simultâneos de pedidos com os itens em ordem invertida travam no banco (deadlock) e devolvem `500`.
4. **O aviso de pagamento repetido só devolve a mesma resposta na mesma máquina e em sequência**, porque a memória do aviso é um `HashMap` local. Na outra máquina, junto com o primeiro ou depois de reiniciar, vem `409 OPERATION_NOT_ALLOWED`. O estoque e o crédito, porém, nunca baixam nem entram duas vezes.
5. **Pagamento entre 48h e 49h é aceito**, porque a conta trunca as horas.
6. **Valores de código que não existem devolvem `500`** em vez dos `422` da tabela: cupom, nível, região, entrega e pagamento. `INVALID_COUPON` nunca aparece. A ordem cupom × pagamento também está trocada.
7. **O Pix dá 5% de desconto quando o cupom informado deu desconto zero**, o que o enunciado proíbe.

## Item a item

**C1, o serviço sobe.** Copiei o projeto para `/tmp/run/app`, rodei `mvn -q package -DskipTests` (14 s) e subi com `PORT=8081 TRANSLOG_URL=http://localhost:9090 java -jar target/order-service-1.0.0.jar`, com o banco vazio. Subiu sem erro: o Jetty estava no ar em cerca de 0,64 s (`Started Server … @641ms` no log), e as tabelas e o catálogo foram criados sozinhos (`config/Database.java:34-39`, `CREATE TABLE IF NOT EXISTS` e carga do catálogo). O primeiro pedido recebeu `id` 1. A segunda instância, na 8082 com o mesmo banco, subiu normalmente. Ao reiniciar, a primeira resposta veio em 763 ms.
Conclusão: segue

**C2, o catálogo.** `GET /products/{sku}` para os oito SKUs devolve nome, preço, estoque e `discontinued` iguais ao anexo. `REL-001` aparece com `"discontinued":true`. Um SKU que não existe dá `404 {"error":"PRODUCT_NOT_FOUND"}`. O peso vem com três casas (`"weightKg":0.300`), o mesmo número que 0.30.
Conclusão: segue

**C3, o resumo.** Usei `POST /checkout/summary` com o stub da TransLog respondendo o que cada exemplo diz.
- **Exemplos 3, 4, 5, 6, 7, 8 e 10:** conferem campo a campo. Por exemplo, o Ex7 deu `1419.60 / 0 / 0 / 6 / 30.00 / -72.48 / 1377.12 / 70.98 / gift true`. O stub registrou os pesos certos: `weight_g=1800`, `3600`, `100`.
- **Exemplo 1:** veio `422 COUPON_NOT_APPLICABLE`, esperado `200` com total 412,07. O controller chama `calculateSummary(request, null)` (`controller/CheckoutController.java:20`), e com `conn == null` o serviço trata todo cliente como "não é primeira compra" (`service/CheckoutService.java:64-68`). O mesmo carrinho em `POST /orders` deu `201` com todos os valores do exemplo. Ou seja, o resumo e o pedido registrado divergem.
- **Exemplo 2:** veio `500 INTERNAL_ERROR` tanto no resumo quanto em `/orders`. Qualquer cartão de 4x a 12x dá `500` (testei 4x e 12x). A causa é `onePlusRate.pow(-installments)` (`service/PaymentService.java:64`): `BigDecimal.pow` não aceita expoente negativo, e conferi no `jshell` que lança `ArithmeticException: Invalid operation`.
- **Exemplo 9:** depois de pagar o pedido 1 do CPF 10000000101, `/orders` com WELCOME10 dá `422 COUPON_NOT_APPLICABLE`, que confere. No resumo o resultado também confere, mas só porque ele recusa o WELCOME10 para todo mundo.
- **Fronteiras:**
  - Motoboy com exatamente 5,0 kg (`MEI-001`×50) foi aceito, frete 18,00. Com 5,1 kg veio `SHIPPING_METHOD_UNAVAILABLE`. Confere.
  - Teto de 30%: os Ex5 e Ex6 confirmam. O FREESHIPPING no motoboy com `MEI-001`×1 deu cupom 5,97 (30% de 19,90), e com `CAM-001`×1 deu 18,00 (abaixo do teto). Confere.
  - Arredondamento meio para o par: `MEI-001`×15, GOLD, Sul, deu seguro 2,98 (2,985) e crédito 14,92 (14,925). Confere.
  - MINUS50: 300,00 exatos não é alcançável com os preços do catálogo. Com 299,10 veio `COUPON_NOT_APPLICABLE`, e com 318,70 o desconto foi de 50,00. Confere.
  - Pix com cupom informado e desconto zero: `CAM-001`×2, retirada, `BUY3PAY2`, Pix. Veio `paymentAdjustment: -8.07`, esperado 0,00 ("mesmo que o cupom tenha dado desconto zero"). A causa é a condição `couponDiscount > 0` em `service/PaymentService.java:37`. O mesmo acontece com FREESHIPPING para um cliente GOLD.

Oito dos dez exemplos e quase todas as fronteiras conferem. Mas um exemplo falha só no resumo, um falha sempre, e toda a faixa de 4x a 12x no cartão está quebrada.
Conclusão: em parte

**C4, as recusas.** Para cada linha da tabela, mudei um campo de um pedido válido.
- **Conferem:**
  - `INVALID_ORDER` nas onze variações: carrinho vazio, sem SKU, SKU repetido, quantidade 0, negativa ou ausente, sem CPF, sem e-mail, sem `customer`, sem `items`.
  - `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` (422).
  - `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_SHIPPING_METHOD` e `INVALID_PAYMENT_METHOD` quando o campo está **ausente**.
  - `INVALID_ZIP_CODE` (ausente, com traço, com 7 dígitos), `SHIPPING_METHOD_UNAVAILABLE`, `CARRIER_UNAVAILABLE` 503.
  - `COUPON_NOT_APPLICABLE`, `INVALID_INSTALLMENTS` (Pix 2x, boleto 2x, cartão 0x e 13x), `PAYMENT_METHOD_UNAVAILABLE`.
- **Não conferem:** um valor que **não existe** devolve `500 {"error":"INTERNAL_ERROR"}` em vez de 422:
  - `clubTier:"PLATINUM"`, `region:"MARS"`, `shippingMethod:"DRONE"`, `paymentMethod:"CASH"`;
  - `coupon:"XYZ"`, `coupon:""` e `coupon:"welcome10"`.

  Os campos são enums do Jackson (`dto/CheckoutRequest.java:17`), e o erro de leitura cai no `catch (Exception e)` → 500 (`controller/CheckoutController.java:24`). `INVALID_COUPON` nunca é devolvido.
- **Dois problemas ao mesmo tempo:**
  - Produto desconhecido + região ausente → `UNKNOWN_PRODUCT`. Confere.
  - Nível ausente + região ausente → `INVALID_CLUB_TIER`. Confere.
  - Boleto 2x + total acima de R$ 1.000 → `INVALID_INSTALLMENTS`. Confere.
  - TransLog fora do ar + MINUS50 não aplicável → `CARRIER_UNAVAILABLE`. Confere.
  - MINUS50 não aplicável + Pix 2x → veio `INVALID_INSTALLMENTS`, esperado `COUPON_NOT_APPLICABLE` (12 antes de 14). O mesmo com a forma de pagamento ausente (`INVALID_PAYMENT_METHOD`) e com WELCOME10 não aplicável + cartão 13x em `/orders`. A causa é que a forma de pagamento é conferida em `validateRequest` antes do cupom, que só é conferido em `CheckoutService.java:77`.
  - Motoboy acima de 5 kg + cupom `"XYZ"` → veio `500`, esperado `SHIPPING_METHOD_UNAVAILABLE`.
  - `[REL-001, XXX]` → veio `PRODUCT_DISCONTINUED`, esperado `UNKNOWN_PRODUCT` (veja em Dúvidas).
- **Operações de um pedido:** pedido inexistente com o corpo incompleto (`POST /orders/999999/payment` sem `transactionId`, `/shipment` sem `trackingCode`) dá `422 INVALID_ORDER`, quando a tabela manda `404 ORDER_NOT_FOUND` primeiro. A causa é que o controller confere os campos antes de buscar o pedido (`controller/OrderController.java:47`, `:67`). Os demais `404` (pedido, cliente, aviso, avisos de CPF sem pedido) conferem.
Conclusão: em parte

**C5, a TransLog.** Usei o stub em `localhost:9090`.
- **Tradução:**
  - `1999 / 24h` → 19,99 e 1 dia; `1999 / 25h` → 2 dias; `5 / 48h` no Norte → 0,05 e 5 dias; `12345 / 49h` no Nordeste → 123,45 e 5 dias.
  - O serviço pede `service=STD` na econômica e `EXP` na expressa, com `weight_g` inteiro (300, 350, 1800).
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` com 200 → `422 SHIPPING_METHOD_UNAVAILABLE` (Ex10).
- **TransLog fora do ar, todos os casos dão `503 CARRIER_UNAVAILABLE`:**
  - status 500;
  - 404 com o corpo de CEP não atendido;
  - 200 com `{}`, 200 com outro erro, 200 com HTML;
  - atraso de 2,5 s e de 10 s, os dois cortados em cerca de 2,02 s;
  - stub derrubado.

  Com atraso de 1,5 s o frete passa normalmente.
- **Motoboy e retirada com o stub derrubado:** `200` no resumo e `201` no registro.
- **Desvio:** `{"price_cents":"abc",…}` foi aceito como frete 0,00, e `-500` como frete −5,00. O `asInt()` (`service/ShippingService.java:73`) converte silenciosamente em vez de tratar como "qualquer outra coisa" = fora do ar.

O essencial confere; só a resposta malformada com 200 escapa.
Conclusão: em parte

**C6, o ciclo do pedido.**
- **Estoque e crédito:** pedido `CAM-001`×2 SILVER. O estoque continua 48 depois do registro e vai a 46 no pagamento. O crédito vai de 0,00 a 3,20.
- **Envio e entrega:**
  - Enviar ou entregar um pedido não pago, entregar um pedido pago, enviar de novo, cancelar enviado ou entregue e entregar de novo: todos dão `409 OPERATION_NOT_ALLOWED`.
  - Envio sem `trackingCode` → `422 INVALID_ORDER`.
  - O envio grava `trackingCode` e gera `ORDER_SHIPPED`.
- **Cancelamento:**
  - Pedido pago `CAM-001`×3 cancelado: estoque 43 → 46 e crédito 4,79 → 0,00, com `ORDER_CANCELLED`.
  - Pedido não pago cancelado: o estoque e o crédito não mudam, e sai `ORDER_CANCELLED`.
  - Pagar um pedido cancelado com outro `transactionId` → `409`.
- **Avisos:** `POST /notifications/4/read` deu `READ`, e repetir continua `READ`. Um aviso inexistente dá `404`. A lista vem em ordem de `id`.
- **Prazo de 48h:** com exatamente 48h (e 47h) o pagamento é aceito, e com 49h01s vem `409 ORDER_EXPIRED`, pedido `CANCELLED`, aviso emitido e estoque intocado. Mas **48h01min e 48h59min foram aceitos** (pedidos 7 e 8 foram para `PAID` e baixaram estoque). A causa é `Duration.toHours() > 48` (`service/OrderService.java:107`), que trunca.
- **Detalhes menores:** um `paidAt` em formato inválido ou o corpo vazio dão `500`.
Conclusão: em parte

**C7, o aviso de pagamento repetido.**
- **Um depois do outro, mesma máquina:** a segunda resposta é byte a byte igual à primeira (`200 PAID`), e o estoque baixou uma vez só (96 → 92). A recusa repetida (expirado) também volta igual: `409 ORDER_EXPIRED` duas vezes.
- **Um depois do outro, na outra máquina:** o mesmo aviso na 8082 deu `409 OPERATION_NOT_ALLOWED`, tanto para o pago quanto para o expirado. Depois de reiniciar, `TX-R11` repetido também deu `409`. A causa é que a memória do aviso é um `HashMap` em memória (`service/OrderService.java:31`, `:85`), não está no banco.
- **Outro `transactionId` num pedido já pago:** `409 OPERATION_NOT_ALLOWED`. Confere.
- **Ao mesmo tempo:** cinco avisos iguais divididos 3/2 entre as máquinas, em 4 rodadas. Em todas, o estoque baixou uma vez só, o crédito entrou uma vez só (0,80) e saiu um `PAYMENT_APPROVED` só. Mas só uma resposta foi `200`; as outras quatro foram `409 OPERATION_NOT_ALLOWED`, em vez da resposta da primeira vez.
- **Cinco `transactionId` diferentes juntos:** em 3 rodadas, um `200` e quatro `409 OPERATION_NOT_ALLOWED`, com um só pagamento aplicado. Confere.

O que importa para o dinheiro e o estoque está protegido (pelo `SELECT … FOR UPDATE`), mas a resposta repetida só é a mesma na mesma máquina e em sequência.
Conclusão: em parte

**C8, a última peça.**
- **Dois pedidos para a última `MOC-001`:** um em cada máquina, 6 rodadas, restaurando a unidade com o cancelamento do vencedor. Em todas passou 1, o estoque foi 1 → 0, e o perdedor recebeu `409 INSUFFICIENT_STOCK`, ficou `CANCELLED` e recebeu `ORDER_CANCELLED`.
- **Seis pedidos para duas unidades:** `JAQ-001` com 2 unidades, 3 rodadas, e `MEI-001`×30 com estoque 78, 2 rodadas. Sempre passaram exatamente 2, o estoque foi a 0 (ou 18), e os quatro perdedores ficaram com `409`, `CANCELLED` e aviso. O estoque nunca ficou negativo.
- **Pedido com dois itens, cancelado por falta do segundo:** pedido 63 com `CAM-001`×1 + `MOC-001`×1, depois que outro pedido levou a última `MOC`. Veio `409 INSUFFICIENT_STOCK` e `CANCELLED`, mas a camiseta baixou de 46 para 45 e não voltou. A baixa do primeiro item é gravada junto com o cancelamento: o `catch` faz `conn.commit()` sem desfazer as baixas anteriores (`service/OrderService.java:120-131`).
- **Pagamentos simultâneos de pedidos com os itens em ordem invertida** (`[CAM, MEI]` e `[MEI, CAM]`, nas duas máquinas, 6 rodadas, estoque de sobra): 8 de 24 pagamentos deram `500 INTERNAL_ERROR`. O contador `pg_stat_database.deadlocks` do banco `shop` marcou 8, ou seja, deadlock. Esses pedidos ficaram aguardando pagamento, sem estrago.

A regra principal se mantém, mas há um vazamento real de estoque e falhas sob concorrência com vários itens.
Conclusão: em parte

**C9, os dados não se perdem.** Antes de derrubar as duas instâncias, fotografei:
- o pedido 3 (`DELIVERED`, `BR123456789`, 170,43) e o pedido 11 (`PAID`);
- o crédito do CPF 10000000701 (1,59);
- os avisos de dois clientes, inclusive um `READ`;
- o estoque de seis produtos.

Depois de subir de novo, a foto saiu idêntica (`diff` vazio). Só a memória do aviso repetido se perde (veja C7).
Conclusão: segue

**C10, os testes do próprio agente.** `mvn verify` na cópia: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`, todos em `com.shop.BasicTest`.
Conclusão: segue

## O que mudaria primeiro

1. **Corrigir o cartão parcelado e o resumo do WELCOME10.** Trocar `pow(-n)` por `BigDecimal.ONE.divide(onePlusRate.pow(n), MathContext.DECIMAL128)` e fazer o resumo consultar o banco como o registro faz. São dois erros pequenos que derrubam dois dos dez exemplos do financeiro e fazem o resumo divergir do pedido.
2. **Gravar a identidade do aviso de pagamento no banco e tornar o pagamento atômico de verdade.** Guardar, por `(pedido, transactionId)`, o resultado da primeira vez, com chave única na mesma transação, e devolvê-lo em qualquer máquina e depois de reiniciar. Na falta de estoque, fazer `rollback` das baixas antes de cancelar, e baixar os itens em ordem fixa de SKU para evitar o deadlock.

## Dúvidas

- **`[REL-001, XXX]`:** o serviço confere item por item e devolve `PRODUCT_DISCONTINUED`. Li "conferir nesta ordem e devolver o primeiro problema encontrado" como a ordem das linhas da tabela, valendo para o carrinho inteiro, e por isso esperei `UNKNOWN_PRODUCT`. Contei como divergência leve, mas outra leitura é possível.
- **Cancelar um pedido já cancelado:** o serviço devolve `200` com o pedido, sem novo aviso. O enunciado não diz se isso é `409`. Não contei.
- **Aviso repetido de um pagamento que deu certo, chegando depois de o pedido ter sido cancelado:** na mesma máquina, o serviço devolve o pedido guardado com `"status":"PAID"`, embora ele esteja `CANCELLED`. "200 e o pedido como está" pode querer dizer o pedido de agora. Não contei.
- **`price_cents` com casas decimais** (`1234.7`): foi truncado para 12,34. O anexo diz que o preço vem em centavos e não prevê esse caso. Não contei; contei só o texto e o negativo, em C5.
- **Formato dos números:** zeros saem como `0` em vez de `0.00`, e `createdAt` sai com microssegundos (`…:01.800686Z`). Os valores são numericamente os mesmos, e não contei.
- **Fronteira de R$ 300,00 do MINUS50:** os preços do catálogo não permitem somar exatamente 300,00. Testei 299,10 e 318,70.

# comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio. O catálogo está certo e os cálculos estão muito bem feitos: os dez exemplos do financeiro conferem campo a campo, e o resumo é igual ao pedido registrado. O ciclo básico funciona (pagar, enviar, entregar, cancelar com devolução), e a última peça disputada entre duas máquinas nunca foi vendida duas vezes. Os problemas, do mais grave para o menos grave:

1. **Aviso repetido ao mesmo tempo.** Quando o mesmo aviso de pagamento chega ao mesmo tempo nas duas máquinas, o estoque baixa duas vezes, o crédito entra em dobro e saem dois avisos. Aconteceu em 10 de 10 tentativas.
2. **Estoque perdido na recusa.** Um pagamento recusado por falta de estoque deixa baixado o estoque dos outros itens do pedido, e essas unidades se perdem.
3. **Sem tempo limite na TransLog.** O limite de 2 segundos não existe: uma TransLog que demora 15 s é esperada e aceita.
4. **Valores inválidos viram 400.** Nível, região, entrega, cupom ou pagamento com valor que não existe voltam `400` com a página de erro padrão do Spring. Por isso `INVALID_COUPON` nunca aparece, nem para o cupom vazio. A ordem de checagem da tabela também não é seguida.
5. **Recusa repetida vira 200.** A repetição de um aviso que foi recusado devolve `200` em vez da mesma recusa.
6. **Brinde no motoboy.** O GOLD no motoboy perde o brinde.
7. **Travamento entre pagamentos.** Dois pagamentos simultâneos com os mesmos produtos em ordem invertida travam um ao outro no banco (deadlock), e um deles volta `500`.

## Item a item

**C1, o serviço sobe.** Subi o jar numa cópia, com `DB_URL`/`DB_USER`/`DB_PASSWORD` do ambiente e `TRANSLOG_URL` apontando para o meu stub. O banco estava vazio e o serviço criou as tabelas sozinho (`spring.jpa.hibernate.ddl-auto=update`, `src/main/resources/application.properties:8`). O log diz `Started OrderServiceApplication in 3.725 seconds`, e a segunda instância na 8082 subiu em 3,6 s. Antes disso, o build com `mvn -q package -DskipTests` levou 29 s.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Os oito voltam `200` com nome, preço, peso e estoque iguais aos do anexo, por exemplo `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`. O `REL-001` vem com `"discontinued":true`. Um SKU que não existe (`XXX-999`) devolve `404 {"error":"PRODUCT_NOT_FOUND"}`. Depois do reinício, o catálogo não foi recriado por cima do estoque já movimentado (`config/DataInitializer.java:21`).
Conclusão: segue

**C3, o resumo.** Mandei `POST /checkout/summary` com os exemplos 1 a 8 e 10, com o stub respondendo o que cada um diz. Todos deram exatamente os valores do enunciado, por exemplo:
- exemplo 2: `409.70/0.00/15.60/7/6.15/30.55/462.00/6/77.00/8.19/false`;
- exemplo 7: `1419.60/0.00/0.00/6/30.00/-72.48/1377.12/1/1377.12/70.98/true`.

O exemplo 8 voltou `422 COUPON_NOT_APPLICABLE`. Para o exemplo 9, paguei antes um pedido do mesmo CPF; aí o `WELCOME10` deu `422 COUPON_NOT_APPLICABLE`, e antes do pagamento ele era aceito. O exemplo 10 deu `422 SHIPPING_METHOD_UNAVAILABLE`. O resumo e o pedido registrado (`GET /orders/2`) deram os mesmos campos, comparados com `diff`.

Nas fronteiras:
- **Motoboy.** Com 5,00 kg (MEI×50) e com 4,80+0,20 kg passa; com 5,10 kg volta `SHIPPING_METHOD_UNAVAILABLE`.
- **Meio para o par.** MEI×10 no `MIDWEST` dá 1,5% de 199,00 = 2,985, e o seguro veio `2.98`.
- **Teto de 30%.** `BUY3PAY2` com MEI×3 volta `17.91`; `FREESHIPPING` com frete 33,10 sobre 79,90 volta `23.97`; sem o teto, o frete inteiro (`33.10`).
- **Seguro no Norte.** O limite segura em `30.00`.
- **Parcelas.** 4× e 12× batem com a tabela Price.
- **Pix com cupom de desconto zero.** Não dá o desconto do Pix (ajuste `0.00`).

Uma divergência: GOLD + `COURIER` + JAQ×2 (779,80) volta frete `18.00`, mas `"gift":false`. Na retirada, o mesmo carrinho volta `"gift":true`. O código tira o brinde no motoboy (`service/CheckoutCalculator.java:416`), mas o enunciado só tira o frete grátis.
Conclusão: em parte

**C4, as recusas.**

Os casos de problema único estão quase todos certos:
- todos os casos de `INVALID_ORDER` (carrinho vazio, sem SKU, SKU repetido, quantidade 0/−1/ausente, CPF/e-mail/cliente ausente) dão `422 INVALID_ORDER`;
- também dão o código certo: `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` (BON-001 e quantidade acima do estoque), nível/região/CEP/entrega/pagamento ausentes, CEP `1234` e `0131010a`, `INVALID_INSTALLMENTS` (Pix 2×, cartão 0× e 13×), `PAYMENT_METHOD_UNAVAILABLE` e `CARRIER_UNAVAILABLE 503`.

Já um valor que não existe nos códigos devolve `400 {"timestamp":…,"status":400,"error":"Bad Request","path":"/checkout/summary"}`, fora do formato `{ "error": "CODE" }`. Isso vale para `"clubTier":"PLATINUM"`, `"region":"MARS"`, `"shippingMethod":"DRONE"`, `"paymentMethod":"CASH"`, `"coupon":"XYZ"`, `"coupon":""` e `"coupon":"welcome10"`. A causa: esses campos são enums desserializados pelo Jackson (`dto/CheckoutRequest.java:215-220`), e o tratador só conhece `BusinessException` (`exception/GlobalExceptionHandler.java:11`). Com isso, `INVALID_COUPON` nunca é devolvido.

A ordem de checagem também falha, porque `validateRequest` confere nível, região, CEP, entrega, cupom, pagamento e parcelas antes de olhar os produtos (`service/CheckoutCalculator.java:35-40`, `83-134`). Resultados com dois problemas ao mesmo tempo:

| os dois problemas | esperado | veio |
|---|---|---|
| SKU inexistente + região ausente | `UNKNOWN_PRODUCT` | `INVALID_REGION` |
| SKU inexistente + nível ausente | `UNKNOWN_PRODUCT` | `INVALID_CLUB_TIER` |
| `REL-001` + CEP ausente | `PRODUCT_DISCONTINUED` | `INVALID_ZIP_CODE` |
| `BON-001` + Pix 3× | `INSUFFICIENT_STOCK` | `INVALID_INSTALLMENTS` |
| motoboy 5,1 kg + Pix 2× | `SHIPPING_METHOD_UNAVAILABLE` | `INVALID_INSTALLMENTS` |
| retirada + `FREESHIPPING` + Pix 2× | `COUPON_NOT_APPLICABLE` | `INVALID_INSTALLMENTS` |
| TransLog fora + pagamento ausente | `CARRIER_UNAVAILABLE` | `INVALID_PAYMENT_METHOD` |

Algumas combinações seguem a ordem: discontinued + sem estoque, nível + região, região + CEP. Nas operações de pedido, `POST /orders/999999/payment` com `{}` dá `422 INVALID_ORDER`, quando o esperado era `404 ORDER_NOT_FOUND`. O mesmo acontece no envio (`service/OrderService.java:83-88`, `134-139`). Os casos simples (`404 ORDER_NOT_FOUND`, `409 OPERATION_NOT_ALLOWED`, `CUSTOMER_NOT_FOUND`, `NOTIFICATION_NOT_FOUND`) estão certos.
Conclusão: em parte

**C5, a TransLog.**

A tradução está certa:
- 3310/41 → 33,10/2; 1560/168 → 7; 3315/48 → 33,15/2; 999/49 → 9,99/3; 1000/24 → 1; 1000/1 → 1;
- os dias a mais saem certos: Norte +3 (exemplo 1, prazo 5) e Nordeste +2 (41 h → 4);
- a consulta vai como pedido: `/v2/quote?zip=01310100&weight_g=1800&service=EXP` e `service=STD` na econômica, conforme o log do stub;
- `ZIP_NOT_SERVED` → `422 SHIPPING_METHOD_UNAVAILABLE`;
- dá `503 CARRIER_UNAVAILABLE` para: outro `error`, corpo que não é JSON, `{}`, HTTP 503, e o stub derrubado (conexão recusada).

Com a TransLog fora do ar, `COURIER` (resumo) e `STORE_PICKUP` (pedido registrado, `201`) continuaram funcionando.

**O tempo limite não existe.** Com o stub atrasando 3 s, a resposta foi `200` em 3060 ms; com 15 s, foi `200` em 15059 ms. O esperado era `503` por volta de 2 s. O parâmetro `translog.timeout.seconds` é lido, mas não é usado no `RestClient` (`service/TransLogService.java:18-25`).
Conclusão: em parte

**C6, o ciclo do pedido.**

O que funcionou (pedido 4, CAM×2, SILVER):
- **Estoque.** Fica 50 depois de registrar e vai a 48 depois do pagamento.
- **Crédito.** Vai a 3,20.
- **Operações fora de ordem.** Enviar ou entregar antes do pagamento, entregar antes de enviar, enviar de novo, entregar de novo, cancelar depois de enviado ou entregue e pagar com outro `transactionId` depois de pago: todos dão `409 OPERATION_NOT_ALLOWED`. O envio sem `trackingCode` dá `422`.
- **Cancelamento de pedido pago.** O pedido 5 (TEN×2, GOLD) cancelado depois de pago devolveu o estoque (8→10) e o crédito (24,99→0,00) e gerou `ORDER_CANCELLED`.
- **Cancelamento sem pagamento.** O cancelamento sem pagamento não mexeu no estoque e gerou aviso.
- **Prazo de 48 h.** `paidAt` exatamente `createdAt`+48 h (com os microssegundos) → `PAID`. Com +48 h 1 s → `409 ORDER_EXPIRED`, pedido `CANCELLED`, aviso gerado e estoque intacto.
- **Avisos.** `GET …/notifications` traz os avisos em ordem. `POST /notifications/4/read` duas vezes dá `READ` nas duas.
- **WELCOME10.** Um CPF só com pedidos cancelados ainda conta como primeira compra.

O defeito: **um pagamento recusado por falta de estoque baixa o estoque dos outros itens e não devolve.** Paguei o pedido 9 (MOC×1, a última unidade). Depois o pedido 10, `[CAM×1, MOC×1]`, deu `409 INSUFFICIENT_STOCK` e foi para `CANCELLED`, mas o CAM caiu de 48 para 47 e assim ficou. A transação é `noRollbackFor = BusinessException` (`service/OrderService.java:81`) e `reserveStock` já salvou a baixa do CAM antes de achar a falta do MOC (`service/OrderService.java:108-116`, `199-215`). Cancelar o pedido de novo não devolve a unidade, porque o cancelamento de um pedido já cancelado só devolve o pedido (`service/OrderService.java:178-180`).

A ordem 404/422 nas operações já está descrita em C4.
Conclusão: em parte

**C7, o aviso de pagamento repetido.**

O que funcionou:
- **Repetição de um aviso aceito.** Devolve `200` com o pedido como está; o estoque não baixa de novo (CAM ficou 48) e o crédito não dobra (3,20).
- **Outro `transactionId`.** Num pedido pago ou cancelado, dá `409 OPERATION_NOT_ALLOWED`.

O que falhou:
- **Repetição de um aviso recusado.** Devolve outra coisa. Pedido 8 expirado: o primeiro `TX-E2` deu `409 ORDER_EXPIRED`, e o mesmo aviso repetido deu `200` com o pedido `CANCELLED`. Pedido 10 sem estoque: o primeiro deu `409 INSUFFICIENT_STOCK`, a repetição deu `200`. O código só guarda o `transactionId` e, quando ele bate, devolve o pedido (`service/OrderService.java:90-93`).
- **Mesmo aviso ao mesmo tempo nas duas máquinas.** Mandei o mesmo `TX-DUP` simultaneamente para a 8081 e a 8082, em 10 rodadas, cada uma com MEI×1 e SILVER. Em todas as 10, as duas respostas foram `200`, o estoque baixou 2 em vez de 1, o crédito ficou `0.80` em vez de `0.40` e saíram dois `PAYMENT_APPROVED`. O pedido é lido sem trava (`service/OrderService.java:87`), e as duas transações veem `AWAITING_PAYMENT`.

Conclusão: em parte

**C8, a última peça.** Fiz 25 rodadas com o MOC-001 em estoque 1. Em cada uma, criei dois pedidos de MOC×1 e paguei ao mesmo tempo, um pela 8081 e outro pela 8082; depois cancelei o vencedor para repor a peça. Resultado: 23 vezes a 8081 passou e a 8082 levou `409 INSUFFICIENT_STOCK`, 2 vezes o contrário. Nunca passaram os dois, e o estoque ficou 0 depois de cada disputa, nunca negativo. A trava pessimista na linha do produto funciona (`repository/ProductRepository.java`, `findBySkuForUpdate`).

Fiz também um teste à parte, fora da pergunta do item: dois pedidos com os mesmos dois produtos em ordem invertida (`[CAM, MEI]` e `[MEI, CAM]`), pagos ao mesmo tempo, com estoque de sobra. Em 10 de 10 rodadas, um deles voltou `500`, e o log mostra `ERROR: deadlock detected`. O pedido recusado ficou `AWAITING_PAYMENT`, sem dano no estoque. Esse problema está na visão geral e não muda a conclusão deste item.
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar, guardei o estoque de 4 produtos, o pedido 4 (`DELIVERED`), os saldos de dois clientes e os avisos de um deles, um já marcado `READ`. Derrubei as duas instâncias e subi a 8081 de novo. O `diff` saiu idêntico. O pedido seguinte recebeu o id 71, continuando a sequência.
Conclusão: segue

**C10, os testes do próprio agente.** Na cópia, `mvn -q verify` terminou com código 0 em 10 s. Rodaram 14 testes, todos passando: 1 em `OrderServiceApplicationTests`, 7 em `CheckoutCalculatorTest` e 6 em `OrderIntegrationTest`, com H2 em memória. O número bate com o `pit.txt`.
Conclusão: segue

## O que mudaria primeiro

1. **Tornar o pagamento atômico.**
   - Ler o pedido com `SELECT … FOR UPDATE` em `processPayment`, para que dois avisos simultâneos não paguem duas vezes.
   - Conferir todo o estoque antes de baixar qualquer item, ou desfazer a baixa e gravar o cancelamento numa transação separada.
   - Guardar o resultado da primeira resposta (sucesso, `ORDER_EXPIRED` ou `INSUFFICIENT_STOCK`) para repeti-lo.
   - Travar os produtos sempre na mesma ordem (por SKU), para acabar com o deadlock.

   Esses são os defeitos que causam prejuízo de verdade: estoque e crédito errados.
2. **Conferir a entrada do jeito que o enunciado pede.**
   - Receber os códigos como texto e validá-los na ordem exata da tabela, antes de qualquer cálculo, sempre respondendo `{ "error": … }`.
   - Configurar o tempo limite de 2 s no `RestClient`.

   Hoje o site recebe `400` genérico para valores inválidos e fica preso enquanto a TransLog demorar.

## Dúvidas

- **Brinde no motoboy.** Li a frase "no motoboy o GOLD paga o frete normalmente" como uma exceção só ao frete, então o brinde continua valendo, e contei a falta dele como erro em C3.
- **GOLD + `FREESHIPPING`.** Na expressa, o desconto sai `0.00`, porque o frete do GOLD já é zero e o cupom fica igual ao frete mostrado. Achei coerente com "o desconto do cupom fica igual ao valor do frete" e não contei como erro.
- **Cancelar um pedido já cancelado.** Devolve `200` com o pedido, sem novo aviso. O enunciado não diz se isso é `OPERATION_NOT_ALLOWED`; não contei como erro.
- **A TransLog com números estranhos.** `price_cents` com casas decimais (3310.75) é truncado em silêncio (33,10), e um preço negativo é aceito. O anexo não diz o que fazer; não contei como erro.
- **Quantidade quebrada.** `quantity: 1.5` é truncada para 1 e aceita. O enunciado pede inteiros, mas não diz se isso é `INVALID_ORDER`; não contei como erro.
- **Formato do `createdAt`.** Vem com fração de segundo: nanossegundos no `201` (`…23.265287107Z`) e microssegundos nas consultas. É ISO válido, mas foge do formato mostrado (`2026-10-14T15:30:00Z`) e muda entre as respostas; não contei como erro.
- **Fronteiras que não deu para testar.**
  - O limite exato de R$ 300,00 do `MINUS50` e o de R$ 1.000,00 do boleto: com os preços do catálogo, todos terminados em ,90, nenhum carrinho soma exatamente esses valores. Testei abaixo e acima.
  - "Continua aguardando depois das 48 h sem aviso": não deu para envelhecer um pedido. Testei só pelo `paidAt`.
- **Versão do Spring Boot.** O `pom.xml` usa Spring Boot 3.3.5, e o enunciado pede 4.1.1. Isso não afeta o comportamento testado, mas é um desvio do pedido técnico.

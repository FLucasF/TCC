# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Ele sobe em menos de 3 s com o banco vazio: o Flyway cria as tabelas e carrega o catálogo. Os sete exemplos do financeiro com valores batem campo a campo, e os três de recusa devolvem o erro e o status certos. As fronteiras também conferem: 5,00 kg no motoboy, o teto de 30% exato, o boleto com total de exatamente R$ 1.000,00 e o "meio para o par". Também conferem os 15 erros da tabela, inclusive quando há dois problemas ao mesmo tempo, e a tradução da TransLog, com tempo limite de 2 s medido. O ciclo do pedido funciona: o estoque só baixa no pagamento, o cancelamento devolve, 48 h exatas são aceitas e 48 h + 1 s não. Com duas máquinas e o mesmo banco, o aviso de pagamento repetido e a disputa pela última peça se comportaram como o enunciado manda em todas as rodadas. O mecanismo é uma trava na linha do pedido (`OrderRepository.java:52`, `SELECT … FOR UPDATE`) e travas nas linhas dos produtos em ordem fixa de SKU (`StockService.java:24-26`, `ProductRepository.java:27`). Os dados sobrevivem ao reinício, e o `mvn verify` passa sem banco. Não achei falha de comportamento. Os pontos que ficam são leituras que o enunciado não decide (preço fracionário da TransLog, a resposta repetida depois de um cancelamento) e um risco teórico de deadlock que não consegui provocar.

## Item a item

**C1, o serviço sobe.** Compilei uma cópia em `/tmp/run/app` e subi com `java -jar … --server.port=8081` contra o banco vazio. O log mostra `Current version of schema "public": << Empty Schema >>`, depois `Migrating schema "public" to version "1 - schema"` e `"2 - catalog seed"`, e por fim `Started PedidosApplication in 2.779 seconds`. Do comando até a primeira resposta HTTP foram cerca de 3,6 s. A segunda máquina (8082) subiu em 2,45 s, sobre o mesmo banco.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Todos voltaram com os valores do anexo: por exemplo, `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`. `BON-001` vem com `stock 0`, e `REL-001` vem com `"discontinued":true`. Um SKU inexistente dá `404 {"error":"PRODUCT_NOT_FOUND"}`. Num pedido, o `REL-001` dá `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Os exemplos 1 a 7, chamados em `POST /checkout/summary` com o stub respondendo como cada exemplo diz, deram exatamente os valores do financeiro. O exemplo 1, por exemplo, deu `409.70 / 40.97 / 33.10 / 5 / 10.24 / 0.00 / 412.07 / 1 / 412.07 / 0.00 / false`. O exemplo 7 deu `1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / … / 70.98 / true`. Os exemplos 8 e 10 dão `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`. O exemplo 9 dá `422 COUPON_NOT_APPLICABLE`, testado depois de o CPF ter um pedido pago e entregue. O resumo e o pedido registrado deram o mesmo JSON: comparei o `summary` do `POST /orders` com o resumo (`cmp`). As fronteiras:
- **Motoboy:** `MEI-001` × 50 (5,00 kg) aceita, com frete 18,00 e prazo 0. 5,05 kg dá `SHIPPING_METHOD_UNAVAILABLE`.
- **Teto de 30%:** com o FREESHIPPING sobre um subtotal de 19,90, o frete de 5,96 vira desconto de 5,96; o de 5,97 (exatamente 30%) vira 5,97; o de 5,98 fica em 5,97. BUY3PAY2 com `MEI-001` × 3 fica em 17,91.
- **Boleto:** com total de exatamente 1000,00 é aceito (final 1003,49); com 1000,01 dá `PAYMENT_METHOD_UNAVAILABLE`.
- **Meio para o par:** o Pix sobre 100,10 dá ajuste −5,00 (5,005 → 5,00). O seguro do Norte sobre 549,80 dá 13,74 (13,745 → 13,74). O Pix sobre 80,70 dá −4,04 (4,035 → 4,04).
- **Cartão em 12×:** dá parcela 39,10. Conferi à parte: 39,1047.

Não consegui testar o MINUS50 em R$ 300,00 exatos (ver Dúvidas). Testei 299,10, que é recusado, e 339,50, que é aceito.
Conclusão: segue

**C4, as recusas.** Com uma base válida, mudei um campo de cada vez em `POST /checkout/summary`. Cada caso da tabela devolveu o código e o status pedidos:
- carrinho vazio, sem `items`, item sem SKU, SKU repetido, quantidade 0, −1 ou ausente, sem CPF, e-mail ou `customer`: `INVALID_ORDER`;
- `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED` e `INSUFFICIENT_STOCK` (`BON-001`, e `MOC-001` × 2);
- nível, região, CEP (ausente, 7 dígitos, com traço) e entrega, inválidos ou ausentes;
- cupom inexistente, vazio ou em minúsculas: `INVALID_COUPON`;
- pagamento inválido ou ausente; Pix 2× ou 0×, cartão 13× ou 0×, boleto 2×: `INVALID_INSTALLMENTS`.

Todos esses dão 422. A TransLog fora do ar dá `503 CARRIER_UNAVAILABLE`.

Testei pares de problemas, e sempre vence o de ordem menor: vazio + nível inválido dá `INVALID_ORDER`; desconhecido + fora de linha dá `UNKNOWN_PRODUCT`; nível + região dá `INVALID_CLUB_TIER`; motoboy acima de 5 kg + cupom inválido dá `SHIPPING_METHOD_UNAVAILABLE`; TransLog fora + cupom inválido dá `503 CARRIER_UNAVAILABLE`; entrega inválida + TransLog fora dá `INVALID_SHIPPING_METHOD`; cupom inválido + pagamento inválido dá `INVALID_COUPON`; parcelas + boleto acima de 1000 dá `INVALID_INSTALLMENTS`. O `POST /orders` recusa igual (vazio dá `INVALID_ORDER`, cupom `""` dá `INVALID_COUPON`). Sem corpo ou com JSON quebrado, a resposta é `422 INVALID_ORDER`.
Conclusão: segue

**C5, a TransLog.** O log do stub mostra a consulta certa: `zip=01310100&weight_g=1800&service=EXP` no exemplo 1, e `service=STD` na econômica. A tradução confere:
- **Prazo:** 41 h → 2 dias, 25 h → 2, 48 h → 2, 49 h no Norte → 3 + 3 = 6, 72 h no Nordeste → 3 + 2 = 5, 1 h → 1.
- **Preço:** 1999 centavos → 19,99, 1 → 0,01.
- **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` com 200 dá `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Respostas tratadas como site fora do ar (`503 CARRIER_UNAVAILABLE`):** status 500, corpo que não é JSON, outro `error`, `ZIP_NOT_SERVED` vindo com status 404, campo ausente, número em texto e `eta_hours` fracionário.
- **Tempo limite:** com atraso de 1,8 s a resposta é 200; com 2,3 s e com 10 s, a resposta é 503 em 2,005 s.
- **Site derrubado:** com o stub parado, a econômica e a expressa dão 503, e o motoboy (frete 18,00, prazo 0) e a retirada (0,00, prazo 1) respondem 200. Com a TransLog fora, também consegui registrar um pedido de motoboy (`201`).

Um `price_cents` fracionário (3310.5) também vira 503 (`HttpTransLogClient.java:68`); essa leitura está em Dúvidas.
Conclusão: segue

**C6, o ciclo do pedido.** Registrei `CAM-001` × 2, SILVER: `201 AWAITING_PAYMENT`, `trackingCode null`, `createdAt` no formato `2026-10-18T12:43:02Z`. O cliente passou a existir com `creditBalance 0.00`, e o estoque continuou em 50.

O pagamento seguiu o enunciado:
- Envio e entrega antes do pagamento dão `409 OPERATION_NOT_ALLOWED`.
- Pagamento sem `transactionId`, sem `paidAt`, sem corpo ou com `paidAt` "ontem" dá `422 INVALID_ORDER`. Num pedido inexistente, dá `404 ORDER_NOT_FOUND`.
- Pagamento com `paidAt` = `createdAt` + 48 h exatas dá `200 PAID`. O estoque foi de 50 para 48, e o crédito ficou em 3,20.

O envio e a entrega também:
- Outro `transactionId` dá `409 OPERATION_NOT_ALLOWED`. Entregar sem enviar também dá 409.
- Envio sem `trackingCode` dá `422 INVALID_ORDER`. Com `trackingCode`, dá `SHIPPED` com `BR123456789`.
- Enviar de novo, cancelar o enviado, entregar de novo e cancelar o entregue dão 409.
- A entrega dá `DELIVERED`.

No cancelamento e nos avisos:
- **Cancelar um pago** (`FON-001` + `MEI-001` × 3, GOLD): o estoque volta (FON 4 → 5, MEI 97 → 100), o crédito volta de 12,98 para 0,00 e sai `ORDER_CANCELLED`.
- **Cancelar um não pago:** dá `CANCELLED`, sem mexer no estoque nem no crédito, e sai `ORDER_CANCELLED`.
- **Pagar com `paidAt` = `createdAt` + 48 h + 1 s:** dá `409 ORDER_EXPIRED`, e o pedido fica `CANCELLED`, com aviso.
- **Listagem dos avisos:** a lista vem do mais antigo para o mais novo, toda `NEW`.
- **Marcar como lido:** dá `READ`, e de novo continua `READ`.
- **404:** aviso inexistente dá `NOTIFICATION_NOT_FOUND`. Cliente e avisos de um CPF sem pedido dão `CUSTOMER_NOT_FOUND`. `GET`, envio, entrega e cancelamento de um pedido inexistente dão `ORDER_NOT_FOUND`.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Um depois do outro:** mandei o mesmo aviso na 8081, depois na 8082, depois com outro `paidAt`. As três respostas foram byte a byte iguais (`200` e o pedido `PAID`). O estoque baixou uma vez só (MEI 99 → 97, pedido de 2), entrou um crédito só (0,80) e saiu um único `PAYMENT_APPROVED`. Outro `transactionId` dá `409 OPERATION_NOT_ALLOWED`. O aviso repetido de uma recusa devolve a mesma recusa: `ORDER_EXPIRED` duas vezes.
- **Ao mesmo tempo, mesmo aviso:** em 18 rodadas, mandei cinco avisos iguais juntos, três na 8081 e dois na 8082. Em todas, as cinco respostas foram `200`, com MD5 idêntico. O estoque baixou exatamente 1 de cada SKU, entrou um crédito só (4,99) e saiu um único `PAYMENT_APPROVED`.
- **Ao mesmo tempo, `transactionId` diferentes:** em 8 rodadas de cinco avisos no mesmo pedido, sempre 1 × `200` e 4 × `409 OPERATION_NOT_ALLOWED`, com uma baixa, um crédito e um aviso.
- **Repetição de uma recusa ao mesmo tempo:** cinco avisos iguais de um pedido sem estoque deram 5 × `409 INSUFFICIENT_STOCK`. Cinco de um pedido expirado deram 5 × `409 ORDER_EXPIRED`. Nos dois casos saiu um único `ORDER_CANCELLED`.

O mecanismo é a trava na linha do pedido antes da consulta a `payment_notices` (`OrderService.java:70`).
Conclusão: segue

**C8, a última peça.** Usei as duas máquinas, com os pagamentos alternados entre 8081 e 8082 e disparados juntos:
- **Dois pedidos para a última `MOC-001`, 10 rodadas** (eu cancelava o vencedor entre uma rodada e outra, para o estoque voltar a 1): sempre 1 × `200` e 1 × `409 INSUFFICIENT_STOCK`. O estoque foi de 1 para 0 e nunca ficou negativo. O perdedor ficou `CANCELLED`, com `["ORDER_CANCELLED"]` e crédito 0,00, e o vencedor ficou `PAID`, com `["PAYMENT_APPROVED"]`.
- **Seis pedidos para duas `JAQ-001`, 10 rodadas:** sempre 2 aceitos e 4 `INSUFFICIENT_STOCK`, com o estoque indo de 2 para 0. Dez pedidos para duas, em 3 rodadas, deram 2 aceitos e 8 recusados.
- **Carrinhos com os mesmos dois produtos em ordem inversa** (`[JAQ, MOC]` e `[MOC, JAQ]`), 6 rodadas: 1 aceito, 1 `INSUFFICIENT_STOCK`, nenhum deadlock.
- **Cancelamento de um pedido pago `[TEN, FON]` junto com o pagamento de um `[FON, TEN]`**, 20 rodadas: todos deram 200, sem erro no log, e o estoque voltou ao início (TEN 10, FON 5).
Conclusão: segue

**C9, os dados não se perdem.** Tirei uma foto de antes: um pedido `DELIVERED` com rastreio e total 190,40; os estoques CAM 22, JAQ 2, MEI 71 e MOC 1; o crédito 3,20; cinco avisos, um deles `READ`; e um pedido `PAID`. Derrubei as duas máquinas e subi uma de novo. O log mostrou `Schema "public" is up to date`, e o `diff` entre antes e depois deu igual. O aviso de pagamento repetido do pedido pago, mandado depois do reinício, devolveu `200 PAID` sem baixar o estoque (JAQ continuou em 2). Ou seja, o registro dos avisos de pagamento também ficou guardado.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` passou na cópia (`exit=0`): `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0` em `PedidosApiTest`. Passou também sem `DB_URL`, `DB_USER` e `DB_PASSWORD` no ambiente, porque o teste aponta para H2 em memória (`PedidosApiTest.java:61`).
Conclusão: segue

## O que mudaria primeiro

1. **Liberar o estoque na mesma ordem fixa de SKU em que ele é travado no pagamento.** O cancelamento de um pedido pago devolve o estoque na ordem dos itens do pedido (`StockService.java:34`), enquanto o pagamento trava os produtos em ordem de SKU. Um cancelamento e um pagamento com os mesmos produtos em ordem inversa podem, em tese, se travar um ao outro, e o PostgreSQL derrubaria um deles com erro 500. Não consegui provocar isso em 20 rodadas, mas é o único caminho concorrente sem a ordem fixa.
2. **Decidir de forma explícita o `price_cents` fracionário.** Hoje ele é tratado como "fora do ar" (`HttpTransLogClient.java:68`). Se a TransLog um dia mandar meio centavo, todas as cotações caem em 503; arredondar "meio para o par", como o resto do dinheiro, seria a outra saída (ver Dúvidas).

## Dúvidas

- **Um `price_cents` com casas decimais** (3310.5 ou 33.105) vira `503 CARRIER_UNAVAILABLE`. O anexo diz que "qualquer outra coisa" é TransLog fora do ar, mas também diz que todo valor em dinheiro se arredonda para centavos. Considerei aceitável a leitura do agente, de que o contrato é um número inteiro de centavos. Isso não muda a conclusão do C5.
- **O aviso repetido de um pagamento aceito, depois que o pedido foi cancelado,** devolve `200` com o pedido como está agora, `CANCELLED`, e não com o corpo `PAID` da primeira vez. O anexo diz "se a primeira deu certo, 200 e o pedido como está", e foi essa a leitura que usei. Uma leitura mais estrita de "exatamente a mesma resposta" pediria o corpo antigo. Nos testes do C7, o pedido não mudava entre as repetições, e as respostas foram idênticas.
- **Os limites exatos de R$ 300,00 (MINUS50), R$ 500,00 (brinde GOLD) e do teto de R$ 30,00 do seguro não são alcançáveis com o catálogo.** Todos os preços terminam em ,90, e dá para mostrar que nenhuma soma chega a esses valores redondos. Testei os vizinhos (299,10 recusado, 339,50 aceito, 549,80 com brinde) e li o código, que usa `< 300` e `> 500`. Usei como fronteiras exatas o teto de 30%, o boleto em R$ 1.000,00 e o motoboy em 5,00 kg, que dão para montar com o preço que eu controlo na TransLog.
- **Um cliente GOLD na econômica ou na expressa, com a TransLog fora, recebe 503,** mesmo que o frete dele fosse sair zerado. É coerente com a tabela (a ordem 10 vem antes do cálculo), e o prazo também depende da TransLog.

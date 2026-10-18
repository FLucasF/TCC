# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Subiu sozinho com o banco vazio em cerca de 2,2 s e criou as tabelas e o catálogo. Os dez exemplos do financeiro bateram campo a campo, e as fronteiras também: o motoboy em 5,00 kg, o boleto com total de exatamente R$ 1.000,00, o limite de 30% do cupom e o "meio para o par". Todas as recusas da tabela vieram com o status e o código certos, e a ordem valeu nos 16 pedidos que testei com dois ou três problemas ao mesmo tempo. A TransLog foi traduzida como o anexo pede, inclusive o tempo limite de 2 s. O ciclo do pedido, o aviso de pagamento repetido (também com 10 repetições ao mesmo tempo), a última peça disputada por duas máquinas (30 rodadas) e o reinício funcionaram sem nenhuma falha. Não achei nenhum problema de comportamento. Os poucos pontos que ficaram sem teste são casos que o catálogo ou o relógio não deixam montar; estão em "Dúvidas".

## Item a item

**C1, o serviço sobe.** Compilei uma cópia em `/tmp/svc` com `mvn -q package -DskipTests` e subi o jar na 8081 com `TRANSLOG_URL` apontando para o meu stub. O log mostra `Current version of schema "public": << Empty Schema >>`, depois as migrações `1 - schema` e `2 - catalog seed`, e `Started PedidosApplication in 2.25 seconds`. Do comando até a primeira resposta HTTP passaram cerca de 3,1 s. A segunda máquina (8082) encontrou o banco já criado (`Schema "public" is up to date`) e subiu em 2,17 s.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs do anexo. Nome, preço, peso, estoque e `discontinued` saíram iguais ao anexo, com duas casas (`{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`). O `REL-001` veio com `"discontinued":true`. Um SKU que não existe devolveu `404 {"error":"PRODUCT_NOT_FOUND"}`.
Conclusão: segue

**C3, o resumo.** Rodei os exemplos 1 a 8 e 10 em `POST /checkout/summary`, cada um com um CPF novo e o stub respondendo o que o exemplo diz. Todos saíram exatamente como o financeiro conferiu. Por exemplo, o 2 deu `409.70 / 0.00 / 15.60 / 7 / 6.15 / 30.55 / 462.00 / 6× 77.00 / 8.19 / false`, e o 7 deu `1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / 70.98 / gift true`. Os exemplos 8 e 10 voltaram `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.

O exemplo 9: antes de qualquer pedido, o mesmo carrinho com WELCOME10 dava desconto de 15.98. Com um pedido só aguardando pagamento, o desconto continuava. Depois de pagar esse pedido, o resumo e o registro passaram a recusar com `422 COUPON_NOT_APPLICABLE`. Com outro CPF, cujo único pedido foi pago e depois cancelado, o WELCOME10 voltou a valer.

As fronteiras:
- **Motoboy:** `MEI-001×50` (5,00 kg) e `TEN-001×4 + MEI-001×2` (5,00 kg) foram aceitos com frete 18.00 e prazo 0. `MEI-001×51` (5,10 kg) voltou `SHIPPING_METHOD_UNAVAILABLE`.
- **Limite de 30% do cupom:** com FREESHIPPING sobre `CAM-001×1` (30% = 23.97), um frete de 23.97 deu desconto de 23.97 e um frete de 23.98 deu desconto de 23.97, ou seja, travou no teto.
- **Meio para o par:** o seguro do Centro-Oeste sobre 199.00 é 2,985 e saiu `2.98`. O desconto Pix sobre o total 40.10 é 2,005 e saiu `-2.00`. Pelo arredondamento comum, os dois teriam saído 2,99 e 2,01.
- **Boleto:** com o total do pedido em exatamente 1000.00, foi aceito (`finalTotal 1003.49`). Com 1000.01, voltou `PAYMENT_METHOD_UNAVAILABLE`.
- **MINUS50:** com produtos de 299.10, foi recusado; com 319.70, deu desconto de 50.00.
- **Clube GOLD:** com FREESHIPPING o frete sai zerado e o desconto fica 0.00. No motoboy, o GOLD paga os 18.00 e o FREESHIPPING desconta 18.00.
- **Cartão em 12×:** sobre 80.70 deu parcela de 7.63 e total de 91.56, o que confere com a tabela Price.

No registro, o `summary` do pedido saiu idêntico ao do resumo (comparei os dois JSONs com `jq -S`).
Conclusão: segue

**C4, as recusas.** Chamei `POST /checkout/summary` com 39 casos isolados, cada um com um problema só. Todos voltaram 422 com o código da tabela, exceto a TransLog fora do ar, que voltou 503 como a tabela pede. Os casos: carrinho vazio, sem `items`, item sem SKU, SKU repetido, quantidade 0, negativa ou ausente, sem CPF, sem e-mail, sem `customer` e corpo vazio dão `INVALID_ORDER`. `XXX-001` dá `UNKNOWN_PRODUCT`, `REL-001` dá `PRODUCT_DISCONTINUED`, `BON-001×1` e `JAQ-001×4` dão `INSUFFICIENT_STOCK`. Nível ausente, `PLATINUM` ou `gold` dão `INVALID_CLUB_TIER`. Região ausente ou `MARS` dão `INVALID_REGION`. CEP com 7 dígitos, com traço ou ausente dá `INVALID_ZIP_CODE`. Entrega `DRONE` ou ausente dá `INVALID_SHIPPING_METHOD`. CEP não atendido dá `SHIPPING_METHOD_UNAVAILABLE`, e a TransLog com erro 500 dá `503 CARRIER_UNAVAILABLE`. Cupom inexistente, vazio ou `welcome10` dá `INVALID_COUPON`. MINUS50 abaixo de 300 dá `COUPON_NOT_APPLICABLE`. Pagamento `CASH` ou ausente dá `INVALID_PAYMENT_METHOD`. PIX em 2×, boleto em 2×, cartão em 13× ou em 0× dão `INVALID_INSTALLMENTS`.

Testei 16 combinações de dois ou três problemas, e o primeiro da ordem sempre venceu:
- SKU repetido e desconhecido dá `INVALID_ORDER`.
- `[REL, XXX]` dá `UNKNOWN_PRODUCT`, mesmo com o fora de linha vindo primeiro no carrinho.
- `[BON, REL]` dá `PRODUCT_DISCONTINUED`.
- Sem estoque e nível inválido dá `INSUFFICIENT_STOCK`.
- Nível e região inválidos dá `INVALID_CLUB_TIER`.
- Região e CEP inválidos dá `INVALID_REGION`.
- CEP e entrega inválidos dá `INVALID_ZIP_CODE`.
- Motoboy acima de 5 kg com cupom inválido dá `SHIPPING_METHOD_UNAVAILABLE`.
- TransLog fora do ar com cupom e pagamento inválidos dá `503 CARRIER_UNAVAILABLE`.
- Cupom inválido e pagamento inválido dá `INVALID_COUPON`.
- Cupom não aplicável e pagamento inválido dá `COUPON_NOT_APPLICABLE`.
- Pagamento inválido com 99 parcelas dá `INVALID_PAYMENT_METHOD`.
- Boleto de 1000.01 em 2× dá `INVALID_INSTALLMENTS`.

Em `POST /orders` a ordem foi a mesma nos quatro casos que repeti, e nenhuma recusa criou o cliente (`GET /customers/31000000002` deu `404 CUSTOMER_NOT_FOUND`).

Nas operações do pedido: um id que não existe deu `404 ORDER_NOT_FOUND` em pagamento, envio, entrega, cancelamento e consulta, mesmo com o corpo sem campos. Faltar `transactionId`, `paidAt` ou `trackingCode` deu `422 INVALID_ORDER`, também num pedido já cancelado, ou seja, o 422 vem antes do 409, como pede a ordem.
Conclusão: segue

**C5, a TransLog.** O stub registrou as chamadas, por exemplo `GET /v2/quote?zip=01310100&weight_g=1800&service=EXP` no exemplo 1 e `weight_g=100&service=STD` no exemplo 6. O peso foi em gramas inteiras e os serviços foram STD e EXP, como o anexo pede.

A tradução saiu certa:
- **Preço:** 3310 centavos viraram 33.10, 1 virou 0.01 e 123456 virou 1234.56.
- **Prazo em dias, arredondado para cima:** 41 h viraram 2 dias, 48 h viraram 2, 49 h viraram 3 e 0 h virou 0.
- **Dias a mais da região:** 25 h no Nordeste deram 4 dias, e no Norte deram 5. A retirada e o motoboy no Norte e no Nordeste ficaram com 1 e 0 dias, sem acréscimo.

A resposta `{"error":"ZIP_NOT_SERVED"}` voltou `422 SHIPPING_METHOD_UNAVAILABLE`. Todas as outras respostas viraram `503 CARRIER_UNAVAILABLE`: status 500, 404 com corpo válido, 200 com outro erro, 200 que não é JSON, `price_cents` faltando, preço como texto e `price_cents` 3310.5.

O tempo limite: uma resposta que demorou 1,5 s foi aceita (`HTTP200 em 1.506s`). Uma que demorou 3 s virou `503` em `2.007s`.

Com o stub derrubado, o motoboy (`200`, frete 18.00) e a retirada (`200`) continuaram funcionando, inclusive no registro (`201 AWAITING_PAYMENT`). A econômica deu `503 CARRIER_UNAVAILABLE`.
Conclusão: segue

**C6, o ciclo do pedido.**
- **Registro:** registrei `CAM-001×2 + MEI-001×3` como SILVER. Voltou `201 AWAITING_PAYMENT`, `trackingCode: null`, o estoque não mudou (CAM 50, MEI 99) e o saldo ficou `0.00`. O `GET /orders/{id}` devolveu o mesmo JSON do registro.
- **Pagamento:** voltou `200 PAID`, o estoque baixou (CAM 48, MEI 96), o saldo foi para `4.39` e apareceu o aviso `PAYMENT_APPROVED` como `NEW`.
- **Envio e entrega:** entregar antes de enviar deu `409 OPERATION_NOT_ALLOWED`. O envio deu `200 SHIPPED` com `trackingCode` e gerou o aviso `ORDER_SHIPPED`. Depois do envio, cancelar, enviar de novo e pagar com outro `transactionId` deram `409`. A entrega deu `200 DELIVERED`, e entregar ou cancelar de novo deram `409`.
- **Cancelamento de um pedido pago:** um pedido GOLD de `FON-001×1` pago tirou o FON de 5 para 4 e deu crédito de 10.00. Cancelado, devolveu o estoque (5), zerou o saldo (0.00) e gerou `ORDER_CANCELLED`. Cancelar de novo ou pagar o cancelado deu `409`.
- **Cancelamento de um pedido não pago:** voltou `CANCELLED` com o estoque e o saldo sem mudar, e também gerou `ORDER_CANCELLED`. Enviar ou entregar um pedido não pago deu `409`.
- **Prazo de 48 horas:** um `paidAt` de exatamente `createdAt + 48h` foi aceito (`PAID`). Com `+48h+1s` voltou `409 ORDER_EXPIRED`, o pedido foi para `CANCELLED`, o aviso `ORDER_CANCELLED` foi gerado e o estoque ficou igual.
- **Sem estoque na hora do pagamento:** dois pedidos de `JAQ-001×2` com estoque 3. O primeiro pagou (estoque 1). O segundo voltou `409 INSUFFICIENT_STOCK`, foi para `CANCELLED` com aviso e o estoque continuou em 1.
- **Avisos:** a lista sai do mais antigo para o mais novo. Marcar um aviso como lido duas vezes deu `200 ... "status":"READ"` nas duas. Um aviso que não existe deu `404 NOTIFICATION_NOT_FOUND`, e um CPF sem pedidos deu `404 CUSTOMER_NOT_FOUND` tanto no saldo quanto nos avisos.
Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Repetição simples:** mandei o mesmo aviso três vezes, com o mesmo `transactionId` e o mesmo `paidAt`, num pedido de `CAM-001×3` GOLD. As três respostas saíram byte a byte iguais. O estoque baixou uma vez só (48 → 45), o crédito entrou uma vez (11.98) e houve um único `PAYMENT_APPROVED`.
- **Mesmo `transactionId` com outro `paidAt`:** um `paidAt` além das 48 h também voltou 200, sem efeito, porque o aviso é identificado pelo pedido e pelo `transactionId`.
- **Outro `transactionId`:** num pedido pago, deu `409 OPERATION_NOT_ALLOWED`.
- **Repetição depois do envio:** voltou `200` com o pedido como está (`SHIPPED`, `BR1`).
- **Repetição de uma recusa:** o pedido expirado repetiu `409 ORDER_EXPIRED` e o que ficou sem estoque repetiu `409 INSUFFICIENT_STOCK`. Nesses dois pedidos, outro `transactionId` deu `409 OPERATION_NOT_ALLOWED`.
- **Dez repetições ao mesmo tempo:** voltaram 10 vezes `200`. O estoque baixou 3 só uma vez (45 → 42), o crédito entrou uma vez (23.96 no total) e saiu um único aviso novo.
- **Depois do reinício:** a repetição continuou devolvendo 200 sem efeito.
Conclusão: segue

**C8, a última peça.** Subi as duas máquinas, 8081 e 8082, no mesmo banco. Em cada rodada registrei vários pedidos da quantidade inteira do estoque e disparei os pagamentos juntos, alternando as máquinas. No fim de cada rodada cancelei o pago para devolver o estoque.
- `MOC-001` (estoque 1), 2 pedidos, 15 rodadas: sempre `1×200 + 1×409`, estoque em 0 e um só `PAID`.
- `MOC-001`, 6 pedidos, 10 rodadas: sempre `1×200 + 5×409`.
- `FON-001` (estoque 5, pedidos de 5 unidades), 4 pedidos, 5 rodadas: sempre `1×200 + 3×409`.

Nenhuma das 30 rodadas falhou, e o estoque nunca ficou negativo (o menor valor foi 0). Também testei dois pedidos com os itens em ordem oposta (`[CAM, MEI]` e `[MEI, CAM]`) pagos juntos nas duas máquinas, 5 vezes: os dois passaram, sem impasse entre as travas, e os logs não têm `ERROR` nem `deadlock`. O mesmo pedido pago ao mesmo tempo nas duas máquinas, com `transactionId` diferentes, deu `200` e `409`, e o estoque baixou uma vez só.
Conclusão: segue

**C9, os dados não se perdem.** Antes de derrubar, guardei o estoque dos oito produtos, um pedido `DELIVERED` completo (com `summary` e `trackingCode`), dois saldos e uma lista de avisos com um aviso `READ`. Derrubei as duas máquinas, subi a 8081 de novo (`Schema "public" is up to date`) e repeti as consultas. O `diff` deu idêntico, inclusive as duas casas dos valores do `summary`.
Conclusão: segue

**C10, os testes do próprio agente.** Rodei `mvn -q verify` numa cópia separada (`/tmp/svc2`), com `DB_URL`, `DB_USER` e `DB_PASSWORD` retiradas do ambiente para imitar a máquina sem banco. Saiu com código 0: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0` em `PedidosApiTest`, usando H2 em memória.
Conclusão: segue

## O que mudaria primeiro

Nada que eu tenha testado falha, então não há correção de comportamento a fazer. Mudaria só uma coisa, e mais por garantia do que por defeito: a TransLog fica fora do ar quando `price_cents` ou `eta_hours` não vêm inteiros (`shipping/HttpTransLogClient.java:65-68`). Isso segue a letra do anexo ("qualquer outra coisa"). Ainda assim, valeria confirmar com a loja se uma resposta como `3310.5` deve mesmo bloquear a venda, porque hoje ela derruba a econômica e a expressa para todos os clientes.

## Dúvidas

- **Limite de um cupom e brinde do GOLD:** com os preços do catálogo, todos terminados em ,90, não existe carrinho que some exatamente R$ 300,00, que é o limite do MINUS50, nem R$ 500,00, o limite do brinde. Testei os valores vizinhos (299,10 recusado e 319,70 aceito). Para o "valor exatamente no limite de um cupom" usei o teto de 30% do FREESHIPPING, cujo frete eu controlo pelo stub (23,97 contra 23,98).
- **Preço com mais de duas casas:** um `price_cents` grande (123456) foi traduzido certo. Um `price_cents` fracionado (3310.5) foi tratado como TransLog fora do ar (503). O anexo diz que o preço vem em centavos, então li um valor fracionado como "qualquer outra coisa" e não contei como falha.
- **Pedido parado depois das 48 horas sem nenhum aviso:** não dá para simular sem mexer no relógio do serviço. Testei só o lado do pagamento, que usa o `paidAt` do aviso. O código não tem nenhuma rotina agendada que mude a situação sozinha (`order/OrderService.java`), o que bate com o enunciado.
- **`paidAt` em outros formatos:** `paidAt` com fração de segundo ou com fuso (`-03:00`) foi aceito, e um texto inválido (`"ontem"`) deu `422 INVALID_ORDER`. O enunciado só mostra o formato com `Z`, então não contei nada disso contra nem a favor.
- **Repetição depois que o pedido avançou:** li o "pedido como está" do enunciado como o pedido na situação atual. Por isso aceitei como certo que a repetição de um aviso aceito, depois do envio, volte `SHIPPED`, e não a resposta da primeira vez byte a byte.

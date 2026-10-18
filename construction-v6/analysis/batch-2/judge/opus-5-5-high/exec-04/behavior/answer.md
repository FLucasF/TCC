# comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio, cria as tabelas, carrega o catálogo e guarda tudo entre reinícios. O cálculo do resumo acerta oito dos dez exemplos do financeiro. A tradução da TransLog funciona: centavos para reais, horas para dias arredondando para cima, CEP não atendido, tempo limite de 2 s, site fora do ar.

Os problemas, do mais grave ao menos grave:

1. **Concorrência.** Não há transação nem trava no pagamento. Com duas máquinas, a última peça é vendida duas ou seis vezes e o estoque fica negativo (MOC −1, JAQ −4, FON −4, TEN −1). O mesmo aviso disparado junto baixa o estoque e dá o crédito duas ou três vezes. Cinco avisos com `transactionId` diferentes, juntos, são todos aceitos.
2. **Respostas 500.** Toda operação que devolve um pedido ou um aviso responde `500 INTERNAL_ERROR`, porque falta o módulo `jsr310` para serializar o `Instant`. Isso vale para registrar, consultar, pagar, enviar, entregar, cancelar, listar avisos e marcar como lido. O efeito no banco acontece, mas o site nunca recebe o pedido.
3. **Repetição sequencial.** O aviso de pagamento repetido, mandado depois do primeiro, devolve `409 OPERATION_NOT_ALLOWED` em vez da resposta da primeira vez.
4. **Valores e erros.** Os juros do cartão saem errados (Ex. 2). Os dias a mais de Norte e Nordeste entram também no motoboy (Ex. 3). Valores fora da lista (nível, região, entrega, pagamento) dão 500 em vez de 422. O cupom vazio é aceito. A ordem das recusas não segue a tabela em dois pontos.

## Item a item

**C1, o serviço sobe.** Compilei uma cópia com `mvn -q package -DskipTests`, que gerou `target/order-service.jar`. Subi com `PORT=8081 TRANSLOG_URL=http://localhost:9000 java -jar …` sobre o banco vazio. No log, o pool de conexões abre às 17:11:26.690, aparece "Database schema created" às 26.965, "Initial products seeded" às 26.977 e "Javalin started" às 27.167: cerca de 0,5 s. O `GET /products/CAM-001` respondeu na primeira tentativa. A segunda máquina (8082) subiu sobre o mesmo banco sem repetir a carga do catálogo. Um detalhe: sem `logback.xml`, o log sai inteiro em DEBUG do Jetty.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` nos oito SKUs. Nome, preço, peso, estoque e `discontinued` vieram iguais ao anexo, e `REL-001` veio com `"discontinued":true`. Um SKU inexistente deu `404 {"error":"PRODUCT_NOT_FOUND"}`. Nos pedidos, `REL-001` é recusado com `PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Rodei os dez exemplos em `POST /checkout/summary`, com o stub da TransLog respondendo o que cada exemplo diz.

- **Os que conferem, campo a campo:** Ex. 1, 4, 5, 6 e 7. Ex. 8 e 10 dão `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`. Ex. 9 deu `422 COUPON_NOT_APPLICABLE` depois que paguei antes um pedido do mesmo CPF.
- **Ex. 2 está errado:** veio ajuste 30.79, total 462.24 e 6× de 77.04, quando o esperado é 30.55, 462.00 e 77.00. A causa está em `CheckoutService.java:197`: o código arredonda `total × taxa` para centavos (8.585855 vira 8.59) antes de dividir pelo fator da tabela Price. Recalculei: a parcela exata é 76.9990.
- **Ex. 3 está errado:** veio prazo 2, quando o esperado é 0. Em `CheckoutService.java:153`, os dias a mais da região são somados em qualquer forma de entrega, inclusive motoboy e retirada.

Fronteiras:

- **Motoboy com exatamente 5 kg:** aceito em cinco combinações, entre elas `MEI-001`×50 e `TEN-001`×4 + `MEI-001`×2. Com 5,1 kg (`MEI-001`×51) dá `SHIPPING_METHOD_UNAVAILABLE`. Segue.
- **MINUS50:** com 298,50 dá `COUPON_NOT_APPLICABLE`; com 309,60 o desconto é 50,00. O valor exato de 300,00 não é alcançável com os preços do catálogo.
- **Limite de 30%:** BUY3PAY2 com `MEI-001`×3 deu 17,91, não 19,90. FREESHIPPING com frete 23,97, exatamente 30% de 79,90, deu 23,97; com frete 23,98, ficou em 23,97. Segue.
- **"Meio para o par":** o seguro de 298,50 a 1% (2,985) deu 2,98; o de 99,50 (0,995) deu 1,00; o Pix de 80,70 a 5% (4,035) deu −4,04. Segue.
- **Boleto:** com o total do pedido exatamente em 1.000,00 (frete 212,40), o serviço recusa com `PAYMENT_METHOD_UNAVAILABLE`, porque compara o valor final já com a tarifa de 3,49 (`OrderRoutes.java:46`). Já um total de 996,51, que dá valor final de 1.000,00, é aceito. O enunciado fala em "total do pedido", que o próprio texto define como o valor antes do ajuste.
- **Formato dos zeros:** os campos zerados saem como `0` e não `0.00` (por exemplo `"couponDiscount":0`), contra o "2 casas decimais" do anexo.
- **Resumo e pedido:** o pedido registrado com os dados do Ex. 1 gravou os mesmos valores do resumo (consulta direta no banco, pedido 2).

Conclusão: em parte

**C4, as recusas.** Testei cada linha da tabela em `/checkout/summary` e algumas em `/orders`.

Conferem:
- carrinho vazio ou ausente, item sem SKU, SKU repetido, quantidade 0, negativa ou ausente, CPF, e-mail ou cliente ausente: `422 INVALID_ORDER`;
- `UNKNOWN_PRODUCT`, `PRODUCT_DISCONTINUED`, `INSUFFICIENT_STOCK` (`BON-001`×1, `MOC-001`×2);
- nível, região e forma de entrega ausentes;
- CEP com 7 dígitos, com traço ou ausente;
- motoboy acima de 5 kg, CEP não atendido, `503 CARRIER_UNAVAILABLE`;
- cupom `XYZ` e `welcome10` (minúsculo): `INVALID_COUPON`;
- forma de pagamento ausente;
- Pix 2×, boleto 3×, cartão 0× e 13×: `INVALID_INSTALLMENTS`;
- boleto acima de 1.000.

Não conferem:
- **Valores fora da lista dão 500.** `"clubTier":"PLATINUM"`, `"region":"MARS"`, `"shippingMethod":"DRONE"` e `"paymentMethod":"CASH"` dão `500 INTERNAL_ERROR`. O Jackson falha ao converter o enum dentro de `bodyAsClass`, antes de qualquer validação, e cai no `catch` genérico.
- **Cupom vazio é aceito.** `"coupon":""` deu 200 com desconto Pix. `Validation.java:82` e `OrderRoutes.java:35` tratam o vazio como "não informado", e o enunciado diz o contrário.

Dois problemas ao mesmo tempo:
- **Conferem:** SKU inexistente + nível ausente dá `UNKNOWN_PRODUCT`; CEP inválido + entrega ausente dá `INVALID_ZIP_CODE`; motoboy acima de 5 kg + cupom `XYZ` dá `SHIPPING_METHOD_UNAVAILABLE`; TransLog fora do ar + cupom `XYZ` dá `503 CARRIER_UNAVAILABLE`; cupom `XYZ` + Pix 2× dá `INVALID_COUPON`.
- **Não conferem:**
  - FREESHIPPING na retirada + Pix 2× deu `INVALID_INSTALLMENTS`, e MINUS50 abaixo de 300 + cartão 13× também. Era `COUPON_NOT_APPLICABLE` nos dois, porque a ordem 12 vem antes da 14. MINUS50 abaixo de 300 + pagamento ausente deu `INVALID_PAYMENT_METHOD`, também no lugar de `COUPON_NOT_APPLICABLE`. O motivo é que `validateCoupon` só roda depois de `validateCheckout`, que já confere o pagamento.
  - `BON-001` (sem estoque) antes de `REL-001` (fora de linha) no carrinho deu `INSUFFICIENT_STOCK` em vez de `PRODUCT_DISCONTINUED`. A conferência é feita item a item (`Validation.java:41-44`), e não regra a regra.
  - SKU inexistente + `PLATINUM` deu 500.

Conclusão: em parte

**C5, a TransLog.**
- **O que confere:**
  - **A chamada:** o stub recebeu `zip=01310100&weight_g=1800&service=EXP` no Ex. 1 e `service=STD` na econômica.
  - **O prazo:** `eta_hours` de 1, 24, 25, 41, 48, 49 e 168 viraram 1, 1, 2, 2, 2, 3 e 7 dias. O Nordeste somou +2 (41 h deu 4 dias) e o Norte +3 (Ex. 1).
  - **O preço:** `price_cents` de 1, 99, 100 e 3310 viraram 0.01, 0.99, 1.00 e 33.10.
  - **CEP não atendido:** `{"error":"ZIP_NOT_SERVED"}` deu `422 SHIPPING_METHOD_UNAVAILABLE`, também em `/orders`.
  - **Fora do ar:** uma resposta de 3 s deu `503 CARRIER_UNAVAILABLE` em 2,005 s. Um HTTP 500, um corpo que não é JSON e o stub derrubado (porta fechada) também deram 503.
  - **Motoboy e retirada sem a TransLog:** com ela fora do ar, os dois deram 200 no resumo. A retirada também gravou o pedido, embora a resposta tenha sido 500, pelo defeito de serialização descrito no C6.
- **O que não confere:** um preço com mais de duas casas foi truncado, não arredondado. `price_cents: 3310.6` deu frete 33.10, e o esperado é 33.11, porque `TransLogClient.java:56` usa `asLong()`.
- **Observação:** cada resumo chama a TransLog três ou quatro vezes (validação, frete, prazo e, com FREESHIPPING, de novo); o log do stub mostra 21 chamadas para 7 resumos. Se a TransLog oscilar entre essas chamadas, o frete ou o prazo pode sair zerado sem erro, porque o código troca a falha por zero em `CheckoutService.java`, no `catch` de `calculateShippingCostRaw` e de `calculateDeliveryDays`.

Conclusão: em parte

**C6, o ciclo do pedido.** Quase toda operação que dá certo responde `500 INTERNAL_ERROR`: `POST /orders`, `GET /orders/{id}`, pagamento, envio, entrega, cancelamento, `GET /customers/{cpf}/notifications` e `POST /notifications/{id}/read`. O log mostra `InvalidDefinitionException: Java 8 date/time type java.time.Instant not supported … (through reference chain: com.clothing.model.Order["createdAt"])`. `Order.java:11` e `Notification.java:13` usam `Instant`, e o `pom.xml` não traz o `jackson-datatype-jsr310`. Por isso conferi os efeitos por `GET /products`, por `GET /customers` (que funciona) e por leitura direta no banco.

- **O que confere:**
  - **Estoque só no pagamento:** `MEI-001` ficou em 99 depois de registrar e foi a 95 depois de pagar (×4). O crédito SILVER de 1,59 entrou no saldo e saiu um `PAYMENT_APPROVED`.
  - **Cancelamento de pedido pago:** `FON-001` foi de 5 a 4 no pagamento e voltou a 5. O crédito GOLD de 10,00 saiu do saldo (10,00 para 0,00) e saiu um `ORDER_CANCELLED`.
  - **Cancelamento de pedido não pago:** o estoque e o saldo não mudaram, e saiu um `ORDER_CANCELLED`.
  - **Prazo de 48 h:** com 48 h exatas, o pagamento foi aceito (`PAID`). Com 48 h + 2 s e com 50 h, deu `409 ORDER_EXPIRED`, o pedido foi para `CANCELLED`, saiu `ORDER_CANCELLED` e o estoque não baixou.
  - **Operações fora de ordem:** entregar ou enviar um pedido aguardando, entregar um pago, enviar de novo, cancelar um enviado ou entregue, pagar ou enviar um cancelado: todos `409 OPERATION_NOT_ALLOWED`.
  - **Campos que faltam:** sem `transactionId`, sem `paidAt` ou sem `trackingCode`, a resposta é 422.
  - **Envio:** o envio gravou o `tracking_code` `BR123456789`.
  - **Marcar como lido:** o aviso 2 foi para `READ` no banco, embora a resposta tenha sido 500. Um aviso inexistente deu `404 NOTIFICATION_NOT_FOUND`.
- **O que não confere:**
  - O site não recebe nenhum pedido nem a lista de avisos (500).
  - Pagamento e envio num pedido inexistente com o corpo vazio deram `422 INVALID_ORDER`, quando a tabela pede `404 ORDER_NOT_FOUND` primeiro (`OrderRoutes.java:123` e `:188` validam o corpo antes de buscar o pedido).
  - Um pedido já cancelado pode ser cancelado de novo, e isso gera um segundo `ORDER_CANCELLED` (pedido 5). O envio não gera `ORDER_SHIPPED`.
  - `GET /orders/abc` dá 500.

Conclusão: em parte

**C7, o aviso de pagamento repetido.**
- **Um depois do outro:** depois de um pagamento aceito, o mesmo aviso repetido deu `409 OPERATION_NOT_ALLOWED`, não a resposta da primeira vez. O status é conferido em `OrderRoutes.java:136`, antes da checagem de repetição em `:142`, então essa checagem nunca é alcançada depois que o pedido muda de situação. O estoque, o crédito e os avisos não se repetiram (pedidos 7 e 8). Depois de um `ORDER_EXPIRED`, a repetição deu `OPERATION_NOT_ALLOWED`, e devia repetir `ORDER_EXPIRED` (pedidos 9 e 10). Depois de um `INSUFFICIENT_STOCK`, também (pedido 37). Outro `transactionId` num pedido pago deu `409 OPERATION_NOT_ALLOWED`, como pede o enunciado.
- **Ao mesmo tempo, mesmo aviso:** disparei cinco cópias, alternando 8081 e 8082, em três rodadas, com `MEI-001`×3 e GOLD (crédito de 2,98 por pedido). O estoque baixou 6, 9 e 6 em vez de 3. O crédito entrou 5,96, 8,94 e 5,96 em vez de 2,98. Saíram 2, 3 e 2 avisos `PAYMENT_APPROVED`. As respostas vieram misturadas entre 500 e 409. A tabela `payment_transactions` tem um registro só, mas a violação de unicidade é engolida (`OrderRepository.java:165-166`) e o processamento continua.
- **Ao mesmo tempo, `transactionId` diferentes:** em três rodadas com `MEI-001`×2, os cinco foram aceitos todas as vezes. O estoque baixou 10 em vez de 2, o crédito entrou 4,00 em vez de 0,80, saíram cinco `PAYMENT_APPROVED`, e nenhuma resposta foi 409.

Conclusão: não segue

**C8, a última peça.**
- **Em sequência:** dois pedidos de `CAM-001`, um de 49 e outro de 50 unidades, com estoque 50. O primeiro pagamento passou (o estoque foi a 1). O segundo deu `409 INSUFFICIENT_STOCK`, o pedido foi para `CANCELLED` e saiu `ORDER_CANCELLED`. Segue.
- **Juntos, um pagamento em cada máquina:**

  | produto | unidades | pedidos | pagamentos aceitos | estoque depois |
  |---|---|---|---|---|
  | `MOC-001` | 1 | 2 | 2 | −1 |
  | `JAQ-001` | 2 | 6 | 6 | −4 |
  | `FON-001` | 2 | 6 | 6 | −4 |
  | `TEN-001` | 1 | 2 | 2 | −1 |

  Ninguém recebeu `INSUFFICIENT_STOCK`. O código lê o estoque (`OrderRoutes.java:153`) e depois faz `stock = stock - ?` sem condição (`OrderRepository.java:198`), cada passo numa conexão própria em autocommit e sem `CHECK (stock >= 0)`.

Conclusão: não segue

**C9, os dados não se perdem.** Derrubei as duas máquinas e subi uma de novo. Os 37 pedidos continuaram lá, com o pedido 4 `DELIVERED` e rastreio `BR123456789`. Os 53 avisos também, com o aviso 2 ainda `READ`. Os estoques ficaram iguais aos observados antes (`CAM-001`=1, `FON-001`=−4, `JAQ-001`=−4, `MOC-001`=−1, `TEN-001`=−1), e o saldo do CPF 66600000001 continuou em 1,59. O catálogo não foi recarregado: nenhuma linha "Initial products seeded" no segundo log.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` numa cópia nova, sem as variáveis do banco, terminou com código 0. Roda um teste só (`CheckoutServiceTest.testMoneyRounding`), segundo `TEST-com.clothing.service.CheckoutServiceTest.xml`.
Conclusão: segue

## O que mudaria primeiro

1. **Fazer o pagamento numa transação só, com trava.** Um `SELECT … FOR UPDATE` no pedido; a baixa como `UPDATE products SET stock = stock - ? WHERE sku = ? AND stock >= ?`, conferindo as linhas afetadas; um `CHECK (stock >= 0)`; e a idempotência gravada junto, devolvendo a resposta guardada quando o mesmo `transactionId` chega de novo. Hoje é isso que deixa o estoque negativo e o crédito duplicado com duas máquinas, exatamente o que o enunciado diz que não pode acontecer.
2. **Registrar o `JavaTimeModule` no Jackson do Javalin e tratar enums inválidos como 422.** É uma mudança pequena que faz voltarem a funcionar as respostas de pedidos e avisos, hoje todas 500, que o site precisa.

## Dúvidas

- **Como observei os efeitos.** Como as respostas vinham 500, conferi estoque e saldo pelas rotas que funcionam e a situação dos pedidos e avisos lendo o banco direto, com uma conexão JDBC só de leitura. O site não teria esse acesso, e por isso conto o 500 como falha mesmo com o efeito certo no banco.
- **Limite de 48 h com frações de segundo.** O `createdAt` tem microssegundos e o `paidAt` vem em segundos. O código trunca a diferença para segundos (`CheckoutService.java:225`), então 48 h + 0,2 s foi aceito. O enunciado não fala em frações; testei a fronteira com 48 h exatas (aceito) e com 48 h + 2 s (recusado).
- **Limite do boleto.** Li "total do pedido" como o valor antes do ajuste de pagamento, porque o próprio enunciado chama assim o passo 5. Com essa leitura, o serviço recusa errado o caso de exatamente 1.000,00.
- **Avisos não exigidos.** O enunciado não diz com todas as letras que o envio gera `ORDER_SHIPPED` (só lista o tipo), nem que cancelar um pedido já cancelado é recusado. Contei esses dois pontos como observações, não como falhas decisivas do C6.
- **Formato dos zeros.** `0` em vez de `0.00` é o mesmo número em JSON. Registrei no C3 como desvio do formato pedido, mas não foi isso que decidiu a conclusão.
- **Preparação do C8.** Para ter "seis pedidos para duas unidades", baixei antes o estoque de `JAQ-001` e `FON-001` com pagamentos feitos em sequência, que funcionaram normalmente.

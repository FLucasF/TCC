# comportamento

## Visão geral

O serviço faz o que o enunciado pede em tudo o que testei. Ele sobe em menos de um segundo com o banco vazio, cria as tabelas e carrega o catálogo sozinho. Os dez exemplos do financeiro batem campo a campo, e as fronteiras também batem: motoboy com 5,00 kg, teto de 30% do cupom, boleto com total de exatamente R$ 1.000,00 e o arredondamento "meio para o par". As 42 recusas da tabela "Quando dá erro" saem com o status e o código certos, inclusive em 13 pedidos com dois problemas ao mesmo tempo. A TransLog é traduzida como o anexo manda, com o tempo limite de 2 s, e o motoboy e a retirada continuam funcionando quando ela está fora do ar.

O ciclo do pedido, o aviso de pagamento repetido e a disputa pela última peça aguentaram as duas máquinas ao mesmo tempo sem nenhuma falha. Em 10 rodadas de avisos simultâneos e 13 disputas de estoque não houve venda a mais, crédito em dobro nem aviso em dobro. Os dados sobrevivem ao reinício e o `mvn verify` passa com 11 testes. Não encontrei nenhum problema de comportamento. As poucas leituras que tive de escolher estão em "Dúvidas".

## Item a item

**C1, o serviço sobe.** Compilei uma cópia em `/tmp/run/svc` com `mvn -q package -DskipTests` (13 s). Subi com `PORT=8081 TRANSLOG_URL=http://localhost:9099 java -jar target/order-service.jar`. A primeira resposta de `GET /products/CAM-001` veio 0,86 s depois do disparo; o log mostra o Jetty pronto em 739 ms. As tabelas são criadas com `CREATE TABLE IF NOT EXISTS`, sob um `pg_advisory_lock` (`src/main/java/shop/persistence/Schema.java:68`). O catálogo é carregado com `ON CONFLICT DO NOTHING` (`Schema.java:81`). A segunda máquina (8082) subiu em seguida no mesmo banco, sem erro no log.
Conclusão: segue

**C2, o catálogo.** `GET /products/{sku}` para os oito SKUs devolveu exatamente os valores do anexo. Por exemplo, `{"sku":"CAM-001","name":"Camiseta básica","price":79.90,"weightKg":0.30,"stock":50,"discontinued":false}`, `BON-001` com `stock` 0 e `REL-001` com `"discontinued":true`. Um SKU inexistente deu `404 {"error":"PRODUCT_NOT_FOUND"}`. Um pedido com `REL-001` foi recusado com `422 PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.** Para cada exemplo, o stub da TransLog respondeu com o preço e o prazo dados no enunciado, e chamei `POST /checkout/summary`.
- **Exemplos 1 a 7:** todos os campos bateram. Exemplo 1: 409.70 / 40.97 / 33.10 / 5 / 10.24 / 0.00 / 412.07 / 1× 412.07 / 0.00 / false. Exemplo 2: …/30.55/462.00/6× 77.00/8.19. Exemplo 7: 1419.60 / 0.00 / 0.00 / 6 / 30.00 / -72.48 / 1377.12 / 70.98 / `gift:true`. Os outros também bateram.
- **Exemplos 8 e 10:** `422 COUPON_NOT_APPLICABLE` e `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Exemplo 9:** o mesmo pedido com `WELCOME10` dá 200 antes de qualquer compra do CPF e `422 COUPON_NOT_APPLICABLE` depois de um pedido pago. Com só um pedido pago e depois cancelado, o cupom volta a valer.
- **O resumo e o pedido registrado batem:** o resumo do exemplo 1 na 8081 e o `POST /orders` na 8082 deram o mesmo JSON, comparado com `jq -S`.

Fronteiras:
- **Peso do motoboy:** com 5,00 kg (`MEI-001`×50, e também `TEN-001`×4 + `MEI-001`×2) o motoboy é aceito, com frete 18.00. Com 5,10 kg vem `SHIPPING_METHOD_UNAVAILABLE`.
- **Teto de 30% do cupom:** FREESHIPPING com frete 23.97, que é exatamente 30% de 79.90, dá desconto 23.97. Com frete 23.98 o desconto fica limitado em 23.97.
- **Limite do boleto:** boleto com total do pedido de exatamente 1000.00 é aceito (`finalTotal` 1003.49). Com 1000.01 vem `PAYMENT_METHOD_UNAVAILABLE`.
- **MINUS50:** com 299.10 em produtos vem `COUPON_NOT_APPLICABLE`; com 319.00 o desconto é 50.00.
- **"Meio para o par":** `MEI-001`×15 (298.50) no Sul dá seguro 2.98 (2,985 vai para 2,98). O Pix dá -15.07 e o crédito GOLD dá 14.92 (14,925 vai para 14,92). O crédito GOLD sobre 59.70 deu 2.98 (2,985 vai para 2,98).
- **Outros:** o teto de seguro do Nordeste ficou em 28.39 (abaixo do limite); o cartão em 12×, 136.84 × 12 = 1642.08; o GOLD no motoboy paga os 18.00; BUY3PAY2 sem trinca dá desconto 0 e tira o desconto do Pix.

Conclusão: segue

**C4, as recusas.** Alterei um pedido válido com `jq`, um problema por vez:
- **Item 1, `INVALID_ORDER` 422:** carrinho vazio, sem `items`, item sem SKU, SKU repetido, quantidade 0, -1 ou ausente, CPF, e-mail ou `customer` ausente.
- **Itens 2 a 4:** `UNKNOWN_PRODUCT` 422, `PRODUCT_DISCONTINUED` 422, `INSUFFICIENT_STOCK` 422 (`BON-001`×1, `JAQ-001`×4).
- **Itens 5 a 8:** os campos inválidos ou ausentes deram `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_ZIP_CODE` (ausente, 7 dígitos, com traço) e `INVALID_SHIPPING_METHOD`, todos 422.
- **Item 9:** motoboy acima de 5 kg e CEP não atendido, `SHIPPING_METHOD_UNAVAILABLE` 422.
- **Item 10:** a TransLog com 500, com 3 s de atraso, com corpo que não é JSON e com outro `error` deu `CARRIER_UNAVAILABLE` 503.
- **Itens 11 e 12:** cupom desconhecido, `""` e `"welcome10"` deram `INVALID_COUPON`; MINUS50 abaixo do mínimo deu `COUPON_NOT_APPLICABLE`.
- **Itens 13 a 15:** `INVALID_PAYMENT_METHOD`; `INVALID_INSTALLMENTS` para Pix 2×, boleto 2×, cartão 0× e 13×; `installments` ausente vale 1; boleto acima de R$ 1.000 dá `PAYMENT_METHOD_UNAVAILABLE`.

Com dois problemas, vale sempre o de menor número na tabela:

| combinação | resposta |
|---|---|
| SKU repetido + desconhecido | `INVALID_ORDER` |
| fora de linha + desconhecido (nesta ordem no carrinho) | `UNKNOWN_PRODUCT` |
| fora de linha + estoque | `PRODUCT_DISCONTINUED` |
| estoque + nível | `INSUFFICIENT_STOCK` |
| nível + região | `INVALID_CLUB_TIER` |
| região + CEP | `INVALID_REGION` |
| CEP + entrega | `INVALID_ZIP_CODE` |
| motoboy acima de 5 kg + cupom | `SHIPPING_METHOD_UNAVAILABLE` |
| TransLog fora + cupom | `CARRIER_UNAVAILABLE` 503 |
| cupom + pagamento | `INVALID_COUPON` |
| cupom não aplicável + pagamento | `COUPON_NOT_APPLICABLE` |
| parcelas + boleto acima de R$ 1.000 | `INVALID_INSTALLMENTS` |

O `POST /orders` segue a mesma ordem; JSON quebrado dá `INVALID_ORDER`. Nas operações de um pedido:
- `ORDER_NOT_FOUND` 404 em consulta, pagamento, envio, entrega e cancelamento, e ele vem antes do campo faltando (`POST /orders/nope/payment` com `{}` dá 404).
- `INVALID_ORDER` 422 sem `transactionId` ou `paidAt`, e sem `trackingCode` (inclusive num pedido não pago: o campo faltando vem antes do 409, como na tabela).
- `CUSTOMER_NOT_FOUND` 404 no saldo e nos avisos de um CPF sem pedido; `NOTIFICATION_NOT_FOUND` 404.

Conclusão: segue

**C5, a TransLog.** O stub registrou as chamadas, por exemplo `zip=01310100&weight_g=1800&service=EXP` (CAM×2 + TEN = 1,8 kg) e `service=STD` na econômica. As horas viram dias arredondando para cima: 0h dá 0 dias, 25h dá 2, 41h dá 2, 48h dá 2, 49h dá 3. Somam-se +3 dias no Norte e +2 no Nordeste, e nada no Sul. O motoboy e a retirada não ganham dias a mais no Norte e no Nordeste.

Os centavos viram reais (1234 dá 12.34). `ZIP_NOT_SERVED` com 200 dá 422; o mesmo corpo com 404 dá 503. Com 1,5 s de atraso, o resumo responde 200 em 1,51 s. Com 2,5 s de atraso, responde 503 em 2,005 s, porque o limite é de 2 s (`src/main/java/shop/shipping/TransLogGateway.java:16`).

Com uma terceira instância apontando `TRANSLOG_URL` para uma porta fechada:
- `ECONOMY` e `EXPRESS` dão `503 CARRIER_UNAVAILABLE`;
- `COURIER` dá 200 com frete 18.00 e prazo 0;
- `STORE_PICKUP` dá 200 com frete 0.00 e prazo 1;
- `POST /orders` com motoboy dá 201.

Conclusão: segue

**C6, o ciclo do pedido.** CPF 90000000601, `CAM-001` com estoque 50, nível SILVER:
1. **Registro:** depois do registro o estoque continua 50 e o saldo é 0.00.
2. **Fora de ordem antes de pagar:** enviar ou entregar um pedido não pago dá 409 `OPERATION_NOT_ALLOWED`.
3. **Pagamento:** dá 200 `PAID`, o estoque vai a 48 e o saldo a 3.20.
4. **Fora de ordem depois de pagar:** entregar um pedido pago dá 409; enviar com `trackingCode:""` dá 422.
5. **Envio e entrega:** o envio dá `SHIPPED` com `BR123456789`. Depois, cancelar dá 409, pagar com outro `transactionId` dá 409 e enviar de novo dá 409. A entrega dá `DELIVERED`; cancelar ou entregar de novo dá 409.

Os outros caminhos:
- **Pedido pago e cancelado:** o pagamento baixa o estoque de 48 para 45 e o saldo vai a 7.99. O cancelamento volta o estoque a 48 e o saldo a 3.20. Cancelar de novo dá 409.
- **Pedido não pago e cancelado:** dá `CANCELLED`, sem mexer no estoque nem no saldo. Pagar depois dá 409.
- **Prazo de 48 horas:** com `paidAt` = `createdAt` + 48h01s vem `409 ORDER_EXPIRED`, o pedido fica `CANCELLED` e o estoque não muda. Com exatamente 48h vem 200 `PAID`. Um `paidAt` ilegível dá 422.
- **Avisos:** a lista sai do mais antigo para o mais novo, com os tipos `PAYMENT_APPROVED`, `ORDER_SHIPPED` e `ORDER_CANCELLED`. Cada cancelamento, inclusive o por expiração, gera um aviso. `POST /notifications/{id}/read` dá `READ`, e repetir na outra máquina continua `READ`, com 200.

Conclusão: segue

**C7, o aviso de pagamento repetido.**
- **Um depois do outro:** o mesmo aviso, mandado uma vez na 8081, outra na 8082 e outra na 8081, devolveu três respostas idênticas, byte a byte. O estoque baixou uma vez só (5 para 4), o crédito ficou em 10.00 e houve um único `PAYMENT_APPROVED`. Outro `transactionId` num pedido `PAID` dá 409 `OPERATION_NOT_ALLOWED`. Repetir o aviso de um pedido expirado devolve de novo `409 ORDER_EXPIRED`, sem novo aviso. Repetir o aviso do primeiro pedido, que já está `DELIVERED`, devolve 200 com o pedido como está (ver "Dúvidas").
- **Ao mesmo tempo:** em 5 rodadas, disparei o mesmo aviso 5 vezes de uma vez, 3 na 8081 e 2 na 8082. Em todas: 5 respostas 200 idênticas, o estoque baixou 3 (uma vez), o crédito foi 2.98 (uma vez) e houve um único `PAYMENT_APPROVED`. Em outras 5 rodadas, disparei 5 `transactionId` diferentes juntos. Em todas: exatamente um 200 e quatro `409 OPERATION_NOT_ALLOWED`, o estoque baixou uma vez e houve um único aviso.
- **Por que funciona:** a linha do pedido é travada com `FOR UPDATE` antes de consultar o aviso repetido (`src/main/java/shop/order/OrderService.java:66` e `:78`), e o aviso é guardado na mesma transação.

Conclusão: segue

**C8, a última peça.** Fiz 13 disputas, cada uma com pagamentos simultâneos alternados entre a 8081 e a 8082:

| disputa | rodadas |
|---|---|
| `MOC-001`, 2 pedidos para 1 unidade | 1 |
| `JAQ-001`, 6 pedidos para 2 | 1 |
| `CAM-001`, 2 pedidos para 1 | 3 |
| `CAM-001`, 6 pedidos para 2 | 3 |
| `MEI-001`, 6 pedidos para 2 | 2 |
| `MEI-001`, 6 pedidos para 1 | 2 |
| `TEN-001` + `FON-001`, 6 pedidos com os dois itens em ordens trocadas, para 2 de cada | 1 |

Em todas, o número de 200 foi igual ao de unidades e o estoque terminou em 0, nunca negativo. Todo perdedor recebeu `409 INSUFFICIENT_STOCK` e ficou `CANCELLED`. As 52 recusas e cancelamentos do CPF de teste têm exatamente um `ORDER_CANCELLED` por pedido: são 42 perdedores e 10 cancelamentos que usei para repor o estoque. Repetir na outra máquina o aviso de um perdedor devolve a mesma recusa, `409 INSUFFICIENT_STOCK`. Não apareceu deadlock nem exceção nos logs.

Por que funciona: os produtos são travados em ordem de SKU (`OrderService.java:153`), e há um `CHECK (stock >= 0)` no banco (`Schema.java:24`).
Conclusão: segue

**C9, os dados não se perdem.** Tirei uma foto do estado antes de derrubar as duas máquinas e outra depois de subir de novo. A foto inclui:
- o saldo de três clientes (4.80, 10.00 e 0.00);
- um hash das listas de avisos, com o tipo e o `NEW`/`READ` de cada um;
- os oito produtos, vários já com estoque 0;
- um pedido `DELIVERED`.

O `diff` saiu vazio. O catálogo não foi recarregado por cima: o estoque continuou 0 onde estava 0. O aviso marcado como lido continuou `READ`.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` da cópia saiu com código 0, em 6 s. O `target/surefire-reports/TEST-shop.ServiceTest.xml` mostra 11 testes, 0 falhas, 0 erros e 0 pulados.
Conclusão: segue

## O que mudaria primeiro

Nada do que testei falhou, então não há correção de comportamento a pedir. Se fosse para reforçar algo, seria:
1. Cobrir no próprio projeto a subida de duas máquinas ao mesmo tempo num banco vazio. O código protege isso com o `pg_advisory_lock`, mas eu subi as duas uma depois da outra.
2. Cobrir uma TransLog que não aceita a conexão (e não só uma que demora a responder), para confirmar que o limite de 2 s vale também na conexão.

## Dúvidas

- **Aviso repetido depois de o pedido andar.** O guia pede "exatamente a mesma resposta da primeira vez"; o enunciado diz "se a primeira deu certo, `200` e o pedido como está". O serviço segue o enunciado: repetir o aviso de um pedido já `DELIVERED` devolve 200 com o status `DELIVERED`. Enquanto o pedido não muda, as respostas são idênticas byte a byte. Tomei como certo.
- **`price_cents` não inteiro.** O stub mandou 3310.5 (e também `"3310"` como texto), e o serviço tratou como TransLog fora do ar (503). Isso bate com o "qualquer outra coisa" do anexo, mas o enunciado não trata esse caso de propósito.
- **Fronteiras inalcançáveis.** Com os preços do catálogo, não existe carrinho de exatamente R$ 300,00 (MINUS50) nem de R$ 500,00 (brinde). Testei logo abaixo e logo acima (299.10 e 319.00), e usei como fronteiras exatas o teto de 30% e o boleto de R$ 1.000,00, que dá para acertar pelo frete do stub.
- **Banco vazio.** Não havia `psql` no ambiente, então não confirmei diretamente que o banco estava vazio antes da primeira subida. Parti do que o guia diz. O catálogo saiu com o estoque original, o que é coerente com um banco vazio.
- **Um erro meu, já corrigido.** Na primeira contagem do C8, um erro no meu script pôs todos os pedidos no mesmo CPF, e a contagem de avisos por cliente saiu errada. Refiz a conferência por pedido: 52 de 52 têm exatamente um `ORDER_CANCELLED`. O resultado das disputas de estoque não é afetado.

# comportamento

## Visão geral

O serviço calcula o resumo da compra, mas não registra pedido nenhum. Todo `POST /orders` devolve `500 Server Error`. O log mostra `Can't infer the SQL type to use for an instance of java.time.Instant`, em `src/main/java/com/loja/services/OrderService.java:31` (a gravação de `created_at`). A leitura de um pedido também quebra, em `OrderService.java:103` (`conversion to class java.time.Instant from timestamp not supported`). Por isso nada do ciclo do pedido funciona: pagamento, envio, entrega, cancelamento, avisos, o aviso de pagamento repetido e a disputa pela última peça. Além disso, o serviço não sobe da primeira vez com o banco vazio: cai em `Database.java:117`, com `Cannot commit when autoCommit is enabled`, e só sobe na segunda tentativa.

O resumo está mais perto do pedido. Oito dos nove exemplos testáveis do financeiro conferem campo a campo, e a tabela de erros funciona caso a caso. Mas há erros sistemáticos de dinheiro:
- O arredondamento é feito em `double`, então os casos exatos de "meio para o par" saem errados.
- O `paymentAdjustment` sai sempre 0,00 nos juros do cartão.
- No cartão até 3×, o valor final é recalculado como parcela × número de parcelas.
- O peso no limite do motoboy é recusado.

Também há erros de ordem nas recusas, e falhas da TransLog que viram frete zero com `200`.

## Item a item

**C1, o serviço sobe.** Na primeira subida com o banco vazio (`PORT=8081 TRANSLOG_URL=http://localhost:9999 java -jar target/order-service-1.0.0.jar`), o processo cria as tabelas e insere o catálogo, mas termina com `Exception in thread "main" org.postgresql.util.PSQLException: Cannot commit when autoCommit is enabled` em `com.loja.database.Database.loadInitialCatalog(Database.java:117)`. Esse `commit()` só roda quando `products` tem zero linhas, o que confirma que o banco estava vazio. Como os `INSERT` já tinham entrado em modo auto-commit, a segunda subida pula o catálogo e funciona: respondeu em cerca de 0,6 s, e a segunda máquina na 8082 também subiu. Ele cria o que precisa, mas não sobe sozinho com o banco vazio: precisa de uma segunda tentativa.
Conclusão: em parte

**C2, o catálogo.** `GET /products/{sku}` para os oito SKUs devolve nome, preço, peso, estoque e `discontinued` iguais ao anexo. O `REL-001` vem com `"discontinued":true`, e um SKU inexistente devolve `404 {"error":"PRODUCT_NOT_FOUND"}`. Os valores saem como `79.9` e `0.3`, sem as duas casas: veja "Dúvidas".
Conclusão: segue

**C3, o resumo.** Usei `POST /checkout/summary`, com a TransLog imitada respondendo o que cada exemplo diz.
- **Exemplos 1, 3, 4, 5, 6, 7, 8 e 10:** conferem todos os campos. Por exemplo, o 7 deu `1419.6 / 0.0 / 0.0 / 6 / 30.0 / -72.48 / 1377.12 / 70.98 / gift true`; o 8 deu `422 COUPON_NOT_APPLICABLE`; o 10 deu `422 SHIPPING_METHOD_UNAVAILABLE`.
- **Exemplo 2:** `finalTotal` 462,00 e as 6 parcelas de 77,00 conferem, mas `paymentAdjustment` veio `0.0` em vez de 30,55. A parcela com juros (`CheckoutService.java:254-258`) não atualiza o ajuste.
- **Exemplo 9:** não deu para testar, porque ele exige um pedido pago, e nenhum pedido pode ser registrado (C6).

Para as fronteiras, comparei o serviço com um cálculo de referência em `BigDecimal` em 897 resumos de retirada na loja (todas as regiões; PIX, cartão 2×, 3×, 5×, 12× e boleto). 493 divergem:
- **Ajuste com juros:** 317 trazem `paymentAdjustment` 0,00. Por exemplo, CAM-001 ×1 no Sudeste em 5×: esperado 4,90, veio 0,0.
- **Meio para o par:**
  - CAM-001 ×1, `MIDWEST`, PIX: o desconto de 81,10 × 5% = 4,055 deveria ser 4,06; veio `-4.05`, e o final 77,05 em vez de 77,04.
  - Subtotal 219,50, `SOUTHEAST`: o seguro de 2,195 deveria ser 2,20; veio `2.19`.
  - Subtotal 329,80, `NORTH`: o seguro de 8,245 deveria ser 8,24; veio `8.25`.
  - A causa é `new BigDecimal(double)` em `utils/MoneyUtils.java:7`.
- **Cartão até 3×:** CAM-001 ×1, `MIDWEST`, 3×: o total é 81,10, mas veio `finalTotal 81.09`, porque o código multiplica a parcela arredondada (`CheckoutService.java:259-261`). O enunciado diz que o final é o próprio total.
- **Motoboy no limite:** MEI-001 ×50 (5,0 kg) passa. JAQ-001 ×3 + TEN-001 ×2 + MEI-001 ×2, também 5,0 kg, foi recusado com `422 SHIPPING_METHOD_UNAVAILABLE`, porque a soma em `double` dá um pouco mais que 5 (`CheckoutService.java:122-127`).
- **Limite de 30%:** confere. Exemplo 5 dá 71,91; exemplo 6 dá 5,97.
- **MINUS50:** um subtotal de 299,10 é recusado (`COUPON_NOT_APPLICABLE`) e um de 379,30 aceito. Os preços do catálogo não permitem um subtotal de exatamente 300,00.

Os exemplos quase todos conferem, mas as fronteiras de arredondamento, o ajuste dos juros e o limite do motoboy falham.
Conclusão: em parte

**C4, as recusas.**
- **Casos isolados:** cada linha da tabela, testada sozinha, devolve o código e o status certos. Cobri: carrinho vazio, item sem SKU, SKU repetido, quantidade 0, negativa ou ausente, CPF, e-mail ou cliente ausente, `XXX-001`, `REL-001`, `BON-001`, `MOC-001` ×2, nível, região, CEP (ausente, 7 dígitos, com traço), entrega, motoboy acima de 5 kg, cupom `XYZ` e `welcome10`, MINUS50 abaixo de 300, pagamento, parcelas (PIX 2×, boleto 3×, cartão 0× e 13×) e boleto em 1.169,70.
- **Três falhas:**
  - **Cupom vazio:** `"coupon":""` devolve `200` com o resumo em vez de `422 INVALID_COUPON` (`CheckoutService.java:101` ignora o texto vazio).
  - **Limite do boleto:** TEN×2 + JAQ×1 + CAM×1 + MEI×1, retirada, `SOUTH`, tem total do pedido 999,40 (o serviço calcula 999,39), mas foi recusado com `PAYMENT_METHOD_UNAVAILABLE`. O código compara o valor final já com a tarifa de 3,49 (`CheckoutService.java:191-192`), e não o total do pedido.
  - **Ordem com dois problemas:**
    - `[REL-001, XXX-001]` devolve `PRODUCT_DISCONTINUED` em vez de `UNKNOWN_PRODUCT`.
    - `[BON-001, XXX-001]` devolve `INSUFFICIENT_STOCK` em vez de `UNKNOWN_PRODUCT`.
    - `[BON-001, REL-001]` devolve `INSUFFICIENT_STOCK` em vez de `PRODUCT_DISCONTINUED`.

    Nesses três, a conferência é feita item a item, não regra a regra (`CheckoutService.java:38-49`).
    - Cupom `XYZ` + pagamento `BITCOIN` devolve `INVALID_PAYMENT_METHOD` em vez de `INVALID_COUPON`.
    - MINUS50 abaixo de 300 + PIX 2× devolve `INVALID_INSTALLMENTS` em vez de `COUPON_NOT_APPLICABLE`.

    Nesses dois, o cupom é conferido depois do pagamento (`CheckoutService.java:87-108`).

    Nível e região inválidos juntos devolvem `INVALID_CLUB_TIER`, o que está certo.
- **Operações de pedido:** só deu para testar o pedido inexistente. Pagamento, envio, entrega, cancelamento e consulta devolvem `404 ORDER_NOT_FOUND`. Aviso inexistente devolve `404 NOTIFICATION_NOT_FOUND`, e CPF sem pedido em `/notifications` devolve `404 CUSTOMER_NOT_FOUND`.
Conclusão: em parte

**C5, a TransLog.**
- **O que confere:**
  - `price_cents` 3315 vira 33,15 e 1 vira 0,01.
  - `eta_hours` 24 dá 1 dia; 25 e 48 dão 2 dias.
  - O Norte soma 3 dias (exemplos 1 e 7) e o Nordeste não afeta o motoboy (exemplo 3).
  - `ZIP_NOT_SERVED` dá `422 SHIPPING_METHOD_UNAVAILABLE`.
  - Uma resposta que demora 2,6 s dá `503 CARRIER_UNAVAILABLE`, em cerca de 2,0 s; uma que demora 1,5 s é aceita.
  - Status 500, status 404, texto que não é JSON e `{}` dão 503.
  - Com a TransLog derrubada, motoboy (`200`, 18,00, 0 dias) e retirada (`200`, 0,00, 1 dia) funcionam, e a expressa dá `503`.
- **O que falha:**
  - **Peso:** CAM-001 ×3 (0,9 kg) foi consultado com `weight_g=899`, por causa do truncamento em `(int)(totalWeight*1000)` (`CheckoutService.java:145` e `363`).
  - **Erro desconhecido:** `{"error":"SOMETHING_ELSE"}`, que pelo anexo é "qualquer outra coisa", devolveu `200` com `shippingCost 0.0` e `deliveryDays 0` em vez de 503 (`CheckoutService.java:151` e `370-371`).
  - **Consultas repetidas:** o serviço consulta a TransLog de 2 a 5 vezes por resumo (5 com FREESHIPPING + boleto). Quando a primeira resposta é boa e a segunda é `500` ou `ZIP_NOT_SERVED`, o resumo sai `200` com frete 0,00 e prazo 0.
Conclusão: em parte

**C6, o ciclo do pedido.** `POST /orders` devolve `500 Server Error` em toda tentativa: retirada com PIX, motoboy com cartão 2×, expressa com WELCOME10 e boleto, nas duas máquinas. O log aponta `OrderService.java:31`. Como teste complementar, inseri à mão um pedido no banco. `GET /orders/manual-1` e `POST /orders/manual-1/payment` também devolveram `500` (`conversion to class java.time.Instant from timestamp not supported`, em `OrderService.java:103`), e o estoque do `MOC-001` continuou 1. Depois apaguei o pedido.

Efeito colateral: cada tentativa de registro deixa um cliente criado sem pedido. `GET /customers/20000000009` devolve `200 {"creditBalance":0.0}`, e o banco ficou com 4 clientes e 0 pedidos. Isso acontece porque o cliente é gravado antes do pedido, fora de uma transação (`OrderService.java:15`).

Registrar, pagar, enviar, entregar, cancelar, o prazo de 48 h e os avisos não funcionam. A lista de avisos sai sempre `[]`.
Conclusão: não segue

**C7, o aviso de pagamento repetido.** Não deu para testar: nenhum pedido pode ser registrado, e mesmo um pedido inserido à mão faz o pagamento dar `500` antes de qualquer lógica (C6). Pela leitura do código, sem teste:
- `PaymentService.java:50-78` lê a situação e depois atualiza o estoque, o crédito e a situação em comandos separados, sem transação nem trava.
- Uma recusa (pedido expirado ou sem estoque) não guarda o `transactionId`, então um aviso repetido depois dela receberia `OPERATION_NOT_ALLOWED`, e não a mesma recusa.
Conclusão: não se aplica

**C8, a última peça.** Não deu para testar, pelo mesmo motivo do C7. Pela leitura do código, sem teste: a conferência de estoque (`PaymentService.java:68-71`) e a baixa (`ProductService.java:21`, `stock = stock + ?`) são separadas, sem trava, e a tabela não tem restrição que impeça estoque negativo. Duas máquinas poderiam vender a mesma peça.
Conclusão: não se aplica

**C9, os dados não se perdem.** Derrubei o serviço e subi de novo, na 8081 e também na 8082. O catálogo (`CAM-001` com estoque 50) e o cliente `30000000006` continuam lá. Mas não há pedidos, baixas de estoque nem avisos para conferir, porque nenhum pode ser criado. A pergunta do item não pode ser feita.
Conclusão: não se aplica

**C10, os testes do próprio agente.** `mvn -q verify` na cópia termina com código 0. Rodam 3 testes (`com.loja.CheckoutServiceTest`: `Tests run: 3, Failures: 0, Errors: 0`).
Conclusão: segue

## O que mudaria primeiro

1. **Fazer o pedido existir.** Gravar e ler `created_at` (e os demais `Instant`) com um tipo que o driver aceita: `Timestamp`, ou `OffsetDateTime` com coluna `timestamptz`. Também é preciso garantir que o Gson serialize `createdAt`. Hoje esse defeito sozinho derruba metade do enunciado (C6 a C9). Em seguida, remover o `conn.commit()` de `Database.java:117`, para o serviço subir na primeira vez.
2. **Fazer o dinheiro em `BigDecimal` do começo ao fim**, com `BigDecimal.valueOf` ou valores vindos do banco como `BigDecimal`, e corrigir a etapa de pagamento: `paymentAdjustment` com os juros e o valor final sem juros igual ao total. Isso resolve as 493 divergências do C3, o limite do motoboy e o peso enviado à TransLog. Junto com isso, consultar a TransLog uma vez só por pedido e tratar qualquer resposta inesperada como fora do ar.

## Dúvidas

- **Duas casas decimais.** O enunciado pede valores "com ponto e 2 casas decimais (79.90)". O serviço devolve `79.9`, `0.0`, `33.1`. Em JSON esses números têm o mesmo valor, e um cliente que lê número não vê diferença. Não mudei a conclusão do C2, mas um site que compara o texto veria.
- **Preço com mais de duas casas.** `price_cents` 3310.5 foi truncado em silêncio para 33,10, e `eta_hours` 40.5 também foi truncado (o prazo deu 2 dias, igual ao certo). O anexo diz que `price_cents` é em centavos e manda tratar "qualquer outra coisa" como fora do ar. Não está claro se um preço fracionado é "outra coisa" (503) ou deve ser arredondado. Não pesou na conclusão do C5.
- **FREESHIPPING com cliente GOLD.** Na expressa, GOLD + FREESHIPPING deu `shippingCost 0.0` (o clube zera o frete), mas `couponDiscount 23.97` (30% dos produtos). Na prática o cupom virou desconto nos produtos. Li "o desconto do cupom fica igual ao valor do frete" como o frete do resumo, que seria zero. Mas o enunciado põe o cupom (etapa 2) antes do frete (etapa 3) e não trata o caso, então não pesou na conclusão.
- **Limite do boleto.** Li "o total do pedido passa de R$ 1.000,00" das observações do financeiro como o total da etapa 5, sem a tarifa. A tabela de erros diz só "boleto acima de R$ 1.000,00". Com essa leitura, a recusa de um total de 999,40 é um erro; foi o único ponto dessa natureza que pesou no C4.
- **Itens que o próprio serviço bloqueia.** Marquei C7 e C8 como `não se aplica`, e não `não segue`, seguindo a regra de "não deu para testar". O que digo sobre eles vem da leitura do código, não de teste. No C9, a única coisa testável (o catálogo e o cliente depois de reiniciar) persistiu.

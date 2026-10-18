# comportamento

## Visão geral

O serviço sobe sozinho com o banco vazio e carrega o catálogo certo. Todas as contas que dão para testar sem a TransLog batem com os exemplos, até o centavo. Os dados persistem. A disputa pela última peça está bem resolvida: o produto fica travado no banco durante o pagamento.

Os problemas, do mais grave para o menos grave:

1. **A econômica e a expressa nunca funcionam.** O serviço não lê a resposta da TransLog no formato do anexo (`price_cents`, `eta_hours`). Toda cotação vira `SHIPPING_METHOD_UNAVAILABLE`, e os exemplos 1, 2, 5, 6, 7 e 9 falham. Também não há o tempo limite de 2 segundos, e a TransLog fora do ar dá 422 no lugar do 503.
2. **As recusas no pagamento são desfeitas.** Quando o pagamento é recusado por prazo vencido ou por falta de estoque, o serviço responde a recusa, mas o banco desfaz o cancelamento. O pedido continua `AWAITING_PAYMENT`, sem aviso, e depois aceita outro pagamento.
3. **O prazo de 48 horas é contado em horas inteiras.** Um pagamento com 48h30 de atraso passa.
4. **A lembrança dos avisos repetidos fica na memória.** Ela vive em um processo só: o mesmo aviso, repetido na outra máquina, pode pagar um pedido que tinha sido recusado.
5. **Algumas recusas saem com o status ou o formato errado.** Valores inválidos em campos de código dão 400 com a página padrão do Spring, e a falta de estoque no resumo dá 409 no lugar de 422.

## Item a item

**C1, o serviço sobe.** Fiz uma cópia em `/tmp/run`, compilei com `mvn -q package -DskipTests` (8 s) e subi com `TRANSLOG_URL=http://localhost:9090 java -jar target/order-service-1.0.0.jar --server.port=8081`. O log mostra `Started OrderServiceApplication in 3.8 seconds`. As tabelas são criadas pelo `spring.jpa.hibernate.ddl-auto=update` (`src/main/resources/application.properties:8`) e o catálogo é carregado pelo `DataInitializer`. A segunda máquina, na 8082, subiu em 3,9 s sem duplicar o catálogo. Segue o que o enunciado pede.
Conclusão: segue

**C2, o catálogo.** Chamei `GET /products/{sku}` para os oito SKUs. Nome, preço, peso, estoque e `discontinued` saíram iguais ao anexo, e o `REL-001` aparece com `"discontinued":true`. Um SKU inexistente (`XXX-999`) devolveu 404 `PRODUCT_NOT_FOUND`. Pedir o `REL-001` no resumo dá 422 `PRODUCT_DISCONTINUED`.
Conclusão: segue

**C3, o resumo.**
- **Os exemplos.** Os exemplos 3, 4 e 8, que não passam pela TransLog, dão exatamente os valores do financeiro, campo a campo. O 3, por exemplo, deu `399.80/50.00/18.00/0/8.00/3.49/379.29/1/379.29/19.99/false`. Os exemplos 1, 2, 5, 6, 7, 9 e 10 dependem da TransLog. Com a TransLog falsa respondendo como o anexo manda (`{"price_cents": 3310, "eta_hours": 41}`), o 1 deu 422 `{"error":"SHIPPING_METHOD_UNAVAILABLE"}`, e o mesmo aconteceu com o 2, o 5, o 6 e o 7.
- **A causa.** Em `src/main/java/com/loja/integration/TransLogClient.java:44-46`, os campos se chamam `priceCents`/`etaHours`, e não há nenhuma conversão de nomes. Assim o preço chega nulo e `TransLogShipping.isAvailable` devolve `false` (`service/shipping/TransLogShipping.java:28`).
- **Diagnóstico, fora do anexo.** Para separar o defeito de leitura das contas, fiz a TransLog responder em camelCase (`priceCents`). Nesse caso, os exemplos 1, 2, 5, 6 e 7 dão exatamente os valores do enunciado: o 7, por exemplo, deu `1419.60/0.00/0.00/6/30.00/-72.48/1377.12/…/70.98/true`. As contas estão certas; o que falha é a leitura da resposta.
- **Exemplo 9.** Com um pedido pago do mesmo CPF, deu `COUPON_NOT_APPLICABLE` tanto no diagnóstico quanto com retirada na loja. No formato do anexo, sai `SHIPPING_METHOD_UNAVAILABLE`, que vem antes na ordem.
- **Exemplo 10.** Deu 422 `SHIPPING_METHOD_UNAVAILABLE`, como o enunciado pede, mas só porque toda resposta da TransLog dá esse erro.
- **Fronteiras:**
  - motoboy com exatamente 5,00 kg (`MEI-001`×50): aceito; com 5,10 kg: 422 `SHIPPING_METHOD_UNAVAILABLE`;
  - arredondamento meio para o par (`MEI-001`×6, Norte): o seguro de 2,985 virou `2.98`;
  - teto de 30% no `BUY3PAY2` (`CAM-001`×3): o desconto deu `71.91`, que é 30% de 239,70;
  - `MINUS50` com 299,10: recusado; com 319,60: aceito. Um subtotal de exatamente 300,00 não existe com este catálogo;
  - `BUY3PAY2` sem nenhum produto com 3 unidades: desconto 0,00;
  - GOLD no motoboy: paga o frete de 18,00.

As fronteiras conferem. O que falha é o principal do item: seis dos dez exemplos dependem da TransLog e não saem.
Conclusão: em parte

**C4, as recusas.** Testei o `POST /checkout/summary` e conferi alguns casos no `POST /orders`.
- **O que confere.** Todos os casos de `INVALID_ORDER` saem 422 com esse código: carrinho vazio ou ausente, item sem SKU, SKU repetido, quantidade 0, −1 ou ausente, CPF, e-mail ou cliente ausente. Também conferem:
  - `UNKNOWN_PRODUCT` e `PRODUCT_DISCONTINUED`;
  - `INVALID_CLUB_TIER`, `INVALID_REGION`, `INVALID_SHIPPING_METHOD` e `INVALID_PAYMENT_METHOD` quando o campo está ausente;
  - `INVALID_ZIP_CODE` para `123`, `01310-100` e ausente;
  - `INVALID_INSTALLMENTS` para Pix 2×, boleto 2×, cartão 0× e 13×;
  - `PAYMENT_METHOD_UNAVAILABLE` para boleto acima de R$ 1.000;
  - o motoboy acima de 5 kg.
- **O que não confere:**
  - **Falta de estoque** (`BON-001`, ou `MOC-001`×2): sai `{"error":"INSUFFICIENT_STOCK"}` com **409**; a tabela pede 422. A causa é `exception/ErrorCode.java:9`, e vale também para o `POST /orders`.
  - **Valores inexistentes** (`clubTier:"PLATINUM"`, `region:"MARS"`, `shippingMethod:"DRONE"`, `paymentMethod:"CASH"`, `coupon:"XYZ"`, `coupon:""`, `coupon:"welcome10"`): todos saem **400** com o corpo padrão do Spring (`{"timestamp":…,"status":400,"error":"Bad Request",…}`), e não com o código da tabela. Os campos são enums (`dto/OrderRequest.java:9-16`), então o JSON quebra antes da validação.
  - **TransLog fora do ar**: sai 422 no lugar de 503 `CARRIER_UNAVAILABLE` (ver o C5).
- **Dois problemas ao mesmo tempo:**
  - SKU inexistente + região ausente → `UNKNOWN_PRODUCT` (certo);
  - fora de linha + sem estoque → `PRODUCT_DISCONTINUED` (certo);
  - quantidade 0 + SKU inexistente → `INVALID_ORDER` (certo);
  - cupom não aplicável + Pix 2× → `COUPON_NOT_APPLICABLE` (certo);
  - fora de linha + `region:"MARS"` → 400 (errado: devia ser `PRODUCT_DISCONTINUED`);
  - CEP inválido + cupom `XYZ` → 400 (errado: devia ser `INVALID_ZIP_CODE`).

A ordem vale quando os campos vêm ausentes, mas qualquer valor inválido num campo de código passa na frente de tudo.
Conclusão: em parte

**C5, a TransLog.** A chamada sai certa: a TransLog falsa recebeu `GET /v2/quote?zip=01310100&weight_g=1800&service=EXP`. O resto falha:
- **Resposta válida** (`price_cents`): 422 `SHIPPING_METHOD_UNAVAILABLE` (ver o C3). Só por isso não dá para conferir a tradução de centavos e horas no formato do anexo. No diagnóstico em camelCase, ela confere: 3310 vira 33,10; 41 h viram 2 dias; 48 h, 2; 49 h, 3; 168 h, 7; e o Norte soma 3 dias.
- **CEP não atendido** (`ZIP_NOT_SERVED`): 422 `SHIPPING_METHOD_UNAVAILABLE`, como pedido.
- **TransLog com erro** (resposta 500, corpo que não é JSON, 503 ou porta fechada): sai sempre **422** `SHIPPING_METHOD_UNAVAILABLE`. O esperado é 503 `CARRIER_UNAVAILABLE`. A causa é o `catch (Exception e) { return false; }` em `service/shipping/TransLogShipping.java:30-32`, que esconde a exceção de indisponibilidade.
- **Tempo limite.** Com a TransLog demorando 5 s, a resposta levou 5,05 s (formato do anexo) e 10,09 s (diagnóstico, que chama a TransLog duas vezes, em `isAvailable` e em `calculateShipping`). O `translog.timeout` é recebido e nunca aplicado (`integration/TransLogClient.java:16-21`). Com 1,5 s de demora, o resumo levou 3,1 s.
- **Motoboy e retirada com a TransLog fora do ar** (porta fechada e 503): continuam respondendo 200 com os valores certos.
Conclusão: não segue

**C6, o ciclo do pedido.**
- **O caminho feliz.** `CPF 50000000001`, `MEI-001`×3, retirada, SILVER. O resumo e o pedido registrado deram os mesmos valores. O estoque continuou 99 depois do registro e foi a 96 depois do pagamento, e o crédito foi a 1,19.
- **Envio e entrega.** Enviar antes de pagar, entregar antes de enviar, cancelar um pedido enviado ou entregue, e enviar ou entregar de novo: todos dão 409 `OPERATION_NOT_ALLOWED`. O envio sem `trackingCode` dá 422 `INVALID_ORDER`, e o código de rastreio aparece depois do envio.
- **Os avisos.** Saíram `PAYMENT_APPROVED` e `ORDER_SHIPPED`, nessa ordem. Marcar como lido devolve `READ`, e marcar de novo continua `READ`. Os 404 (`NOTIFICATION_NOT_FOUND`, `CUSTOMER_NOT_FOUND`, `ORDER_NOT_FOUND`) conferem.
- **Cancelar um pedido pago** (`50000000012`, `CAM-001`×2): o estoque foi de 50 a 48 e voltou a 50; o crédito foi de 3,20 a 0,00; veio o aviso `ORDER_CANCELLED`.
- **Cancelar um pedido não pago** (`50000000003`): o estoque e o saldo não mudam, e vem o aviso.
- **Prazo de 48 horas:**
  - exatamente 48h: aceito (certo);
  - **48h + 1 s: aceito, e o estoque baixou** (errado);
  - **48h30: aceito** (errado);
  - 72h: 409 `ORDER_EXPIRED`, **mas `GET /orders/13` continua `AWAITING_PAYMENT`, sem aviso de cancelamento.** Depois disso, outro `transactionId` dentro do prazo pagou esse mesmo pedido.

  A causa do limite é `elapsed.toHours() > 48`, que joga fora os minutos (`service/OrderService.java:91`). A causa do pedido que não cancela é o `throw` dentro do `@Transactional`, que desfaz o `order.cancel()` e o aviso (`service/OrderService.java:92-101`).
- **Falta de estoque no pagamento** (`MOC-001`, com uma unidade e dois pedidos): o segundo pagamento deu 409 `INSUFFICIENT_STOCK`, mas o pedido continuou `AWAITING_PAYMENT`, sem aviso. Depois que o estoque voltou, outro `transactionId` pagou o mesmo pedido.
- **Cancelar de novo um pedido já cancelado:** 200 e um **segundo** aviso `ORDER_CANCELLED` (ver Dúvidas).
- **Ordem das verificações:** `POST /orders/999999/payment` com `{}` deu 422 `INVALID_ORDER`, quando a tabela pede `ORDER_NOT_FOUND` primeiro. O envio tem a mesma inversão.

O caminho normal funciona, mas dois pontos que o item pede falham: a recusa depois do prazo e o cancelamento automático.
Conclusão: em parte

**C7, o aviso de pagamento repetido.**
- **O que confere:**
  - o mesmo aviso repetido num pedido pago (`TX-R1`, pedido 16) devolveu uma resposta byte a byte igual à primeira; o estoque ficou em 4, o crédito em 10,00, e havia um só `PAYMENT_APPROVED`;
  - depois do envio, e também depois do reinício, a repetição devolve 200 com o pedido como está;
  - outro `transactionId` num pedido pago dá 409 `OPERATION_NOT_ALLOWED`;
  - uma recusa repetida na mesma máquina devolve a mesma recusa.
- **O que falha:** a lembrança das recusas fica num `HashMap` na memória de um processo (`service/OrderService.java:30`). O pedido 101, por exemplo, foi recusado por falta de estoque na 8081 (`TX-X`, 409 `INSUFFICIENT_STOCK`). Liberei o estoque e repeti **o mesmo aviso na 8082**: veio 200 `PAID`, e o estoque baixou de 1 para 0. Com o prazo vencido, a repetição na outra máquina ou depois do reinício devolveu `ORDER_EXPIRED` de novo, mas só porque o pedido nunca chegou a ser cancelado.

O caso mais comum confere, mas a repetição de uma recusa não garante a mesma resposta com duas máquinas, que é como o serviço roda.
Conclusão: em parte

**C8, a última peça.** Fiz 24 rodadas com `MOC-001` em estoque 1, dois pedidos e os dois pagamentos disparados juntos, um em cada máquina, alternando qual saía primeiro. Depois fiz quatro rajadas válidas de 10 pagamentos simultâneos, 5 em cada máquina. Em todas, exatamente **um** passou e os outros deram 409 `INSUFFICIENT_STOCK`, e o estoque nunca ficou abaixo de 0. Os tempos de resposta se sobrepõem (cerca de 10 ms e 13 ms), o que é coerente com uma máquina esperando a trava da outra (`repository/ProductRepository.java:15-17`, `PESSIMISTIC_WRITE`). O vencedor foi sempre a 8081 nos pares, e a 8081 também nas rajadas. Isso não fere o enunciado, mas sugere que a 8082 estava só um pouco mais lenta.
Conclusão: segue

**C9, os dados não se perdem.** Registrei o estado de 115 pedidos (situação, rastreio, valor final), do estoque dos 8 produtos e do saldo e dos avisos de 5 CPFs. Derrubei as duas máquinas, subi a 8081 de novo (4,0 s) e registrei tudo outra vez. O `diff` não mostrou nenhuma diferença, e o catálogo não foi recarregado.
Conclusão: segue

**C10, os testes do próprio agente.** O `mvn -q verify` na cópia passou (saída 0). Rodam **6 testes**: 3 em `MoneyTest` e 3 em `CheckoutServiceTest`, com 0 falhas e o banco H2 em memória.
Conclusão: segue

## O que mudaria primeiro

1. **Ler a TransLog do jeito do anexo.** Os campos precisam ser lidos como `price_cents`/`eta_hours` (com `@JsonProperty` ou uma regra de nomes), com o tempo limite de 2 s aplicado no cliente e a TransLog chamada uma vez só. A indisponibilidade tem de chegar ao controller como 503, sem virar `false`. Com isso, as duas entregas principais da loja voltam a funcionar, e o diagnóstico indica que as contas já estão certas.
2. **Gravar as recusas do pagamento de verdade.** O cancelamento e o aviso das recusas de prazo e de estoque precisam ser salvos antes de responder com o erro (fora da transação desfeita, ou sem lançar exceção dentro dela). O resultado de cada `transactionId` tem de ficar guardado no banco, não num `HashMap`. Junto com a troca de `toHours()` por uma comparação exata de 48 horas, isso faz um pedido vencido ou sem estoque ser cancelado de fato. A repetição passa a dar a mesma resposta em qualquer máquina, e nada mais é pago por engano.

## Dúvidas

- **A TransLog em camelCase** não está no anexo. Usei esse formato só para separar a leitura das contas, e os itens foram decididos pelo formato do anexo.
- **Cancelar um pedido já cancelado:** o enunciado diz que dá para cancelar "enquanto o pedido não foi enviado", sem tratar esse caso. Li que um pedido cancelado não pode ser cancelado de novo (`OPERATION_NOT_ALLOWED`). O serviço aceita e gera um segundo aviso. Não mudei a conclusão do C6 por isso.
- **O formato de `createdAt`:** o `POST /orders` devolve `createdAt` com nanossegundos (`…21.177134333Z`) e as consultas seguintes, com microssegundos (`…21.177134Z`). O enunciado mostra `2026-10-14T15:30:00Z`. Considerei a data legível e não contei isso como falha, mas o mesmo pedido aparece com dois textos diferentes.
- **`MINUS50` exatamente em 300,00:** não dá para montar com este catálogo (todos os preços terminam em ,90). Testei o mais perto possível de cada lado (299,10 e 319,60).
- **`FREESHIPPING` para um GOLD na econômica ou na expressa**, quando o frete já vem zerado: o enunciado não decide. Pelo código, daria `COUPON_NOT_APPLICABLE`, mas não deu para testar por causa da TransLog.

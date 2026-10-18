Oi! Tenho uma loja online de roupas e acessórios e preciso do serviço que cuida dos pedidos: calcula o resumo da compra, registra o pedido, acompanha o pagamento e o envio, e controla o estoque. Não sou programador, mas ando estudando o básico de programação por curiosidade. Vou explicar como o negócio funciona e o que eu preciso que aconteça, e no fim deixei a parte técnica que consegui montar estudando e pesquisando. A pasta está vazia, então é montar tudo do começo.

## Como funciona a compra

O cliente monta o carrinho com os produtos do catálogo, informa o CPF e o e-mail, escolhe como quer receber, pode colocar um cupom e escolhe como vai pagar. Antes de confirmar, o site mostra um resumo com quanto deu cada parte e o valor final. Quando o cliente confirma, o pedido é registrado e fica esperando o pagamento. O resumo que o site mostra antes de confirmar e o do pedido registrado precisam dar exatamente os mesmos valores: num sistema antigo, já aconteceu de o resumo mostrar um valor e o pedido registrar outro.

O valor final é calculado nesta ordem:

1. Soma dos produtos (preço de cada item × quantidade).
2. Desconto do cupom, se o cliente usou um.
3. Frete, de acordo com a forma de entrega escolhida.
4. Seguro do envio, de acordo com a região do cliente.
5. Total do pedido = produtos − desconto do cupom + frete + seguro.
6. Ajuste da forma de pagamento sobre esse total.

Todo valor em dinheiro é arredondado para centavos em cada etapa, usando o arredondamento "meio para o par" (exemplo: 2,995 vira 3,00 e 2,985 vira 2,98).

## Catálogo e estoque

Os produtos ficam num catálogo, cada um com um código (o SKU), nome, preço, peso e a quantidade em estoque. O catálogo de hoje está no anexo e precisa estar no sistema assim que o serviço começar a funcionar.

Alguns produtos saem de linha: continuam no catálogo, mas não podem mais ser vendidos.

O estoque só baixa quando o pagamento é confirmado, não quando o cliente monta o carrinho. Já aconteceu de dois clientes pagarem a última peça quase no mesmo segundo e a gente vender o que não tinha: isso não pode acontecer. O estoque nunca pode ficar negativo.

## Entrega

Hoje temos estas opções de entrega:

| Opção | Quem calcula | Prazo |
|---|---|---|
| `ECONOMY` | a transportadora TransLog | a TransLog informa |
| `EXPRESS` | a transportadora TransLog | a TransLog informa |
| `STORE_PICKUP` | grátis | 1 dia |
| `COURIER` | R$ 18,00 | 0 dias (mesmo dia) |

O peso do pedido é a soma do peso de cada item × quantidade, sem arredondar.

O motoboy só leva pedidos de até 5 kg. Acima disso, essa opção não está disponível.

Os pedidos para o Norte e o Nordeste saem de um centro de distribuição que demora mais para despachar, então a econômica e a expressa têm dias a mais nessas regiões: Norte, 3 dias a mais; Nordeste, 2 dias a mais. Nas outras regiões, e na retirada e no motoboy, o prazo é o da opção.

Na econômica e na expressa, quem diz o valor do frete e o prazo é a TransLog, pelo site dela (como consultar está no anexo). Às vezes a TransLog não atende algum CEP, e às vezes o site dela sai do ar. Pode ser que um dia a gente troque de transportadora.

Estamos fechando parceria com transportadoras novas o tempo todo, e quase toda semana entra uma opção nova de entrega, cada uma com seu jeito de cobrar, seu prazo e suas limitações.

## Cupons

O pessoal do marketing adora inventar promoção. Os cupons que valem hoje são estes:

- **WELCOME10**: 10% de desconto no valor dos produtos, só na primeira compra do cliente. Conta como primeira compra quando o CPF ainda não tem nenhum pedido pago, enviado ou entregue (um pedido cancelado não conta).
- **MINUS50**: R$ 50,00 de desconto nos produtos, só para compras a partir de R$ 300,00 em produtos.
- **FREESHIPPING**: o cliente não paga o frete. No resumo, o frete aparece normalmente e o desconto do cupom fica igual ao valor do frete. Não vale para retirada na loja, que já não tem frete.
- **BUY3PAY2**: a cada 3 unidades de um mesmo produto do carrinho, uma sai de graça. Se o carrinho não tiver 3 unidades de nenhum produto, o cupom vale, mas o desconto é zero.

Nenhum cupom pode dar desconto maior que 30% do valor dos produtos, nem o FREESHIPPING: se a conta do cupom passar disso, o desconto fica em 30% do valor dos produtos, arredondado para centavos.

Só dá para usar um cupom por pedido. O código do cupom é sempre em letras maiúsculas, exatamente como está acima. Por enquanto os cupons ficam fixos no sistema, não precisa de cadastro.

## Clube da loja

Todo cliente tem um nível no clube, e o nível muda o que a pessoa recebe:

- **BRONZE**: é só o cadastro, não ganha nada.
- **SILVER**: ganha 2% do valor dos produtos de volta, em crédito para a próxima compra.
- **GOLD**: ganha 5% dos produtos de volta em crédito, não paga frete, e se os produtos passarem de R$ 500,00 a gente manda um brinde junto. O motoboy é terceirizado, então no motoboy o GOLD paga o frete normalmente.

O crédito não abate nada nesta compra: ele entra no saldo do cliente quando o pagamento é confirmado (o valor é o crédito que saiu no resumo do pedido) e fica guardado para as próximas. Quando o GOLD não paga frete, o frete sai zerado no resumo.

Estamos estudando criar mais níveis, e cada um vai ter seu conjunto de vantagens.

## Depois que o cliente confirma

O pedido passa por estas situações:

- **AWAITING_PAYMENT**: o pedido foi registrado e o banco ainda não avisou o pagamento.
- **PAID**: o banco avisou que o pagamento caiu.
- **SHIPPED**: o pacote saiu, com o código de rastreio.
- **DELIVERED**: o cliente recebeu.
- **CANCELLED**: o pedido não vai mais acontecer.

Só dá para enviar um pedido pago, e só dá para entregar um pedido enviado. O cliente pode cancelar enquanto o pedido não foi enviado; depois de enviado, não dá mais. Se o pagamento chegar mais de 48 horas depois do registro do pedido, a gente não aceita: o pedido é cancelado. Com exatamente 48 horas, ainda aceita. O pedido só muda de situação quando chega um aviso de pagamento ou um pedido de cancelamento: enquanto nada chega, ele continua aguardando o pagamento, mesmo depois das 48 horas.

Estamos pensando em criar uma situação "em separação", entre o pago e o enviado, para o pessoal do estoque.

Quando o pagamento é confirmado, três coisas precisam acontecer: baixar o estoque dos produtos, colocar o crédito do clube no saldo do cliente e avisar o cliente de que o pagamento foi aprovado. Por enquanto, o aviso é só registrar a mensagem numa lista que o atendimento consulta; o e-mail de verdade vem depois. Cada aviso começa como novo, e o atendimento marca como lido depois de conferir. O marketing já pediu para, no futuro, também mandar SMS e dar pontos num parceiro de cashback quando o pagamento cair.

Se, na hora do pagamento, não houver estoque para todos os itens, o pedido é cancelado e o cliente é avisado.

O banco às vezes avisa o mesmo pagamento duas vezes. O mesmo aviso repetido não pode baixar o estoque duas vezes, nem dar o crédito duas vezes.

Quando um pedido pago é cancelado, o estoque volta, o crédito do clube sai do saldo do cliente e o cliente é avisado. Quando um pedido ainda não pago é cancelado, não há nada para devolver. Todo cancelamento, de qualquer jeito que aconteça, gera um aviso para o cliente.

O cliente passa a existir no sistema quando registra o primeiro pedido, com saldo de crédito zero.

Os pedidos, o estoque e o saldo dos clientes precisam ficar guardados: se o serviço reiniciar, nada pode se perder.

## O que o atendimento mais responde

Juntei aqui as dúvidas que os clientes mais mandam no WhatsApp, pode ajudar a entender o negócio:

- **"Qual o prazo de entrega?"** Depende da opção escolhida, aparece no resumo.
- **"Pagando no Pix tem desconto?"** Tem sim, 5% de desconto no total do pedido, mas só para quem não informou cupom (mesmo que o cupom tenha dado desconto zero).
- **"Posso trocar se não servir?"** Pode, em até 7 dias, mas isso é resolvido pelo atendimento, não entra no sistema.
- **"Dá para parcelar?"** No cartão de crédito, em até 3x sem juros. De 4x até 12x tem juros de 1,99% ao mês.
- **"O boleto tem alguma taxa?"** Tem uma tarifa de R$ 3,49 que o banco cobra, somada ao total.
- **"Pix e boleto parcelam?"** Não, são sempre à vista.
- **"Aceitam outras moedas?"** Não, só reais.

## Observações do financeiro

- O cálculo dos juros do cartão é o mesmo do crediário (tabela Price): **parcela = total × taxa ÷ (1 − (1 + taxa)^−número de parcelas)**. A parcela é arredondada para centavos e o valor final é a parcela × número de parcelas.
- Sem juros (até 3x), o valor final é o próprio total do pedido e a parcela é o total dividido pelo número de parcelas, arredondado para centavos.
- No Pix, o desconto é 5% do total do pedido, arredondado para centavos.
- Todo pedido vai com seguro contra extravio e roubo, e a seguradora cobra por região do cliente:

  | Região | Porcentagem | Limite do seguro |
  |---|---|---|
  | Sudeste (`SOUTHEAST`) | 1% | sem limite |
  | Sul (`SOUTH`) | 1% | sem limite |
  | Centro-Oeste (`MIDWEST`) | 1,5% | sem limite |
  | Norte (`NORTH`) | 2,5% | R$ 30,00 |
  | Nordeste (`NORTHEAST`) | 2% | R$ 30,00 |

  O seguro é a porcentagem sobre o valor dos produtos, sem desconto e sem frete, arredondada para centavos; quando a região tem limite e a conta passa dele, o seguro fica no limite.
- O crédito do clube é sobre o valor dos produtos, sem desconto e sem frete, arredondado para centavos.
- Não aceitamos boleto quando o total do pedido passa de R$ 1.000,00.

---

## Anexo: como o site vai chamar o serviço

Dei uma pesquisada e consegui montar assim. O pessoal que vai fazer o site pediu a parte técnica em inglês, então os nomes que o site e o serviço trocam estão todos em inglês. Por favor, siga exatamente estes nomes e formatos. Todos os valores em dinheiro vêm e vão como número com ponto e 2 casas decimais (79.90); quantidades e parcelas, como número inteiro; datas e horas no formato `2026-10-14T15:30:00Z`.

### Os códigos

| no negócio | os códigos |
|---|---|
| níveis do clube | `BRONZE`, `SILVER` (prata) e `GOLD` (ouro) |
| regiões | `SOUTHEAST` (Sudeste), `SOUTH` (Sul), `MIDWEST` (Centro-Oeste), `NORTH` (Norte) e `NORTHEAST` (Nordeste) |
| entregas | `ECONOMY` (econômica), `EXPRESS` (expressa), `STORE_PICKUP` (retirada na loja) e `COURIER` (motoboy) |
| cupons | `WELCOME10`, `MINUS50`, `FREESHIPPING` e `BUY3PAY2` |
| formas de pagamento | `PIX`, `CREDIT_CARD` (cartão de crédito) e `BANK_SLIP` (boleto) |
| situações do pedido | `AWAITING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED` e `CANCELLED` |

### O catálogo inicial

| sku | name | price | weightKg | stock | discontinued |
|---|---|---|---|---|---|
| `CAM-001` | Camiseta básica | 79.90 | 0.30 | 50 | false |
| `TEN-001` | Tênis corrida | 249.90 | 1.20 | 10 | false |
| `FON-001` | Fone sem fio | 199.90 | 0.25 | 5 | false |
| `MEI-001` | Meia esportiva | 19.90 | 0.10 | 100 | false |
| `JAQ-001` | Jaqueta corta-vento | 389.90 | 0.80 | 3 | false |
| `MOC-001` | Mochila urbana | 159.90 | 0.90 | 1 | false |
| `BON-001` | Boné aba reta | 59.90 | 0.15 | 0 | false |
| `REL-001` | Relógio digital | 129.90 | 0.20 | 8 | true |

### Os dados da compra

O resumo e o registro do pedido recebem os mesmos dados, em JSON:

| campo | o que é | exemplo |
|---|---|---|
| `customer` | quem está comprando: `cpf` (11 números, sem pontos) e `email`. O serviço só confere se vieram; o formato, o site já conferiu | `{ "cpf": "12345678909", "email": "ana@exemplo.com" }` |
| `items` | a lista do carrinho. Cada item tem `sku` e `quantity` | `[{ "sku": "CAM-001", "quantity": 2 }]` |
| `zipCode` | o CEP de entrega, 8 números, sem traço | `"01310100"` |
| `region` | onde o cliente mora | `SOUTHEAST` |
| `shippingMethod` | a forma de entrega | `EXPRESS` |
| `coupon` | o código do cupom; pode não vir. Se vier, mesmo vazio, precisa ser um dos cupons que valem | `WELCOME10` |
| `paymentMethod` | a forma de pagamento | `PIX` |
| `installments` | em quantas vezes; pode não vir, e nesse caso é 1 | 1 |
| `clubTier` | o nível do cliente no clube | `GOLD` |

### O que o serviço oferece

| o que faz | como o site chama | o que volta quando dá certo |
|---|---|---|
| calcular o resumo, sem registrar nada | `POST /checkout/summary` com os dados da compra | `200` e o resumo |
| registrar o pedido | `POST /orders` com os dados da compra | `201` e o pedido |
| consultar um pedido | `GET /orders/{id}` | `200` e o pedido |
| o banco avisa o pagamento | `POST /orders/{id}/payment` com `{ "transactionId": "TX-123", "paidAt": "2026-10-14T16:00:00Z" }` | `200` e o pedido |
| enviar | `POST /orders/{id}/shipment` com `{ "trackingCode": "BR123456789" }` | `200` e o pedido |
| entregar | `POST /orders/{id}/delivery` | `200` e o pedido |
| cancelar | `POST /orders/{id}/cancellation` | `200` e o pedido |
| consultar um produto | `GET /products/{sku}` | `200` e o produto |
| consultar o saldo do cliente | `GET /customers/{cpf}` | `200` e `{ "cpf": "…", "creditBalance": 20.48 }` |
| ver os avisos de um cliente | `GET /customers/{cpf}/notifications` | `200` e a lista de avisos, do mais antigo para o mais novo |
| marcar um aviso como lido | `POST /notifications/{id}/read` | `200` e o aviso (se já estava lido, continua lido) |

**O resumo** tem os campos: `productsSubtotal` (a soma dos produtos), `couponDiscount` (o desconto do cupom), `shippingCost` (o frete), `deliveryDays` (o prazo de entrega, em dias), `insurance` (o seguro), `paymentAdjustment` (o ajuste da forma de pagamento: o valor final menos o total do pedido; negativo quando é desconto, positivo quando é tarifa ou juros, zero quando não muda nada), `finalTotal` (o valor final), `installments` (em quantas vezes), `installmentAmount` (o valor de cada parcela), `nextPurchaseCredit` (o crédito para a próxima compra) e `gift` (se vai brinde: `true` ou `false`).

**O pedido** tem os campos: `id`, `status` (a situação), `createdAt` (quando foi registrado), `summary` (o resumo calculado quando o pedido foi registrado, que não muda mais), `items` (cada um com `sku` e `quantity`) e `trackingCode` (o código de rastreio; `null` até o envio).

**O produto** tem os campos: `sku`, `name`, `price`, `weightKg`, `stock` e `discontinued` (`true` ou `false`).

**Cada aviso** tem os campos `id`, `type` (`PAYMENT_APPROVED`, `ORDER_SHIPPED` ou `ORDER_CANCELLED`), `orderId` e `status` (`NEW` ou `READ`).

O mesmo aviso de pagamento repetido (mesmo pedido e mesmo `transactionId`) devolve exatamente a mesma resposta da primeira vez, sem fazer nada de novo: se a primeira deu certo, `200` e o pedido como está; se a primeira foi recusada (pedido expirado ou sem estoque), a mesma recusa. Um aviso de pagamento com outro `transactionId`, num pedido que já não está aguardando o pagamento, é uma operação não permitida.

### A TransLog

O endereço do site da TransLog chega ao serviço pela variável de ambiente `TRANSLOG_URL`. A documentação dela também é em inglês; o que importa para nós é isto. Para saber o frete e o prazo:

`GET {TRANSLOG_URL}/v2/quote?zip=01310100&weight_g=1800&service=EXP`

- `zip`: o CEP, 8 números;
- `weight_g`: o peso do pedido em gramas, número inteiro;
- `service`: `STD` para a nossa econômica e `EXP` para a nossa expressa.

Quando a TransLog atende, ela responde `200` com `{ "price_cents": 3310, "eta_hours": 41 }`: o preço em centavos e o prazo em horas. Para nós, o frete é o `price_cents` em reais (3310 centavos são R$ 33,10) e o prazo em dias é o `eta_hours` dividido por 24, arredondado para cima (41 horas são 2 dias).

Quando a TransLog não atende aquele CEP, ela também responde `200`, mas com `{ "error": "ZIP_NOT_SERVED" }`. Se ela responder qualquer outra coisa, ou não responder em até 2 segundos, considere a TransLog fora do ar.

### Quando dá erro

Quando não dá para fazer o que foi pedido, o serviço recusa e devolve só o código do problema, assim: `{ "error": "CODE" }`, com a situação HTTP da tabela.

**No resumo e no registro do pedido**, conferir nesta ordem e devolver o primeiro problema encontrado:

| Ordem | Situação | Código | HTTP |
|---|---|---|---|
| 1 | Carrinho vazio, item sem SKU, o mesmo SKU duas vezes no carrinho, ou quantidade zero, negativa ou ausente; CPF ou e-mail ausente | `INVALID_ORDER` | 422 |
| 2 | Algum SKU que não existe no catálogo | `UNKNOWN_PRODUCT` | 422 |
| 3 | Algum produto fora de linha | `PRODUCT_DISCONTINUED` | 422 |
| 4 | Algum produto sem estoque suficiente para a quantidade pedida | `INSUFFICIENT_STOCK` | 422 |
| 5 | Nível do clube que não existe ou não informado | `INVALID_CLUB_TIER` | 422 |
| 6 | Região que não existe ou não informada | `INVALID_REGION` | 422 |
| 7 | CEP ausente ou fora do formato | `INVALID_ZIP_CODE` | 422 |
| 8 | Forma de entrega que não existe ou não informada | `INVALID_SHIPPING_METHOD` | 422 |
| 9 | Forma de entrega existe, mas não atende o pedido (motoboy acima de 5 kg, ou CEP que a TransLog não atende) | `SHIPPING_METHOD_UNAVAILABLE` | 422 |
| 10 | A forma de entrega depende da TransLog e ela está fora do ar | `CARRIER_UNAVAILABLE` | 503 |
| 11 | Cupom informado que não existe (inclusive o texto vazio) | `INVALID_COUPON` | 422 |
| 12 | Cupom existe, mas o pedido não cumpre a condição | `COUPON_NOT_APPLICABLE` | 422 |
| 13 | Forma de pagamento que não existe ou não informada | `INVALID_PAYMENT_METHOD` | 422 |
| 14 | Número de parcelas não permitido para a forma de pagamento (Pix e boleto só 1; cartão de 1 a 12) | `INVALID_INSTALLMENTS` | 422 |
| 15 | Forma de pagamento existe, mas não atende o pedido (boleto acima de R$ 1.000,00) | `PAYMENT_METHOD_UNAVAILABLE` | 422 |

**Nas operações de um pedido** (pagamento, envio, entrega, cancelamento e consulta), conferir nesta ordem:

| Situação | Código | HTTP |
|---|---|---|
| Pedido que não existe | `ORDER_NOT_FOUND` | 404 |
| Faltou um campo obrigatório: `transactionId` ou `paidAt` no aviso de pagamento, `trackingCode` no envio | `INVALID_ORDER` | 422 |
| A operação não é permitida na situação atual do pedido | `OPERATION_NOT_ALLOWED` | 409 |
| Pagamento chegou mais de 48 horas depois do registro (o pedido vai para `CANCELLED`) | `ORDER_EXPIRED` | 409 |
| Na hora do pagamento, faltou estoque (o pedido vai para `CANCELLED`) | `INSUFFICIENT_STOCK` | 409 |

Produto, cliente ou aviso que não existe: `404` com `PRODUCT_NOT_FOUND`, `CUSTOMER_NOT_FOUND` ou `NOTIFICATION_NOT_FOUND`. Os avisos de um CPF que nunca registrou pedido também são `CUSTOMER_NOT_FOUND`.

### Exemplos conferidos pelo financeiro

Nos exemplos, o CEP é sempre `01310100`, e o cliente está fazendo a primeira compra, a não ser quando o exemplo diz o contrário.

**Exemplo 1**: Camiseta básica (`CAM-001`) × 2 + Tênis corrida (`TEN-001`) × 1, `EXPRESS` (a TransLog respondeu `{ "price_cents": 3310, "eta_hours": 41 }`), cupom `WELCOME10`, `PIX`, clube `BRONZE`, região `NORTH`
→ subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 5 · seguro 10,24 · ajuste 0,00 · total final 412,07 · 1× de 412,07 · crédito 0,00 · brinde não

**Exemplo 2**: Camiseta básica (`CAM-001`) × 2 + Tênis corrida (`TEN-001`) × 1, `ECONOMY` (a TransLog respondeu `{ "price_cents": 1560, "eta_hours": 168 }`), sem cupom, `CREDIT_CARD` em 6×, clube `SILVER`, região `MIDWEST`
→ subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · seguro 6,15 · ajuste 30,55 · total final 462,00 · 6× de 77,00 · crédito 8,19 · brinde não

**Exemplo 3**: Fone sem fio (`FON-001`) × 2, `COURIER`, cupom `MINUS50`, `BANK_SLIP`, clube `GOLD`, região `NORTHEAST`
→ subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · seguro 8,00 · ajuste 3,49 · total final 379,29 · 1× de 379,29 · crédito 19,99 · brinde não

**Exemplo 4**: Meia esportiva (`MEI-001`) × 7 + Camiseta básica (`CAM-001`) × 2, `STORE_PICKUP`, cupom `BUY3PAY2`, `CREDIT_CARD` em 3×, clube `SILVER`, região `SOUTH`
→ subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · seguro 2,99 · ajuste 0,00 · total final 262,29 · 3× de 87,43 · crédito 5,98 · brinde não

**Exemplo 5**: Camiseta básica (`CAM-001`) × 3, `EXPRESS` (a TransLog respondeu `{ "price_cents": 2475, "eta_hours": 30 }`), cupom `BUY3PAY2`, `PIX`, clube `BRONZE`, região `SOUTHEAST`
→ subtotal 239,70 · cupom 71,91 · frete 24,75 · prazo 2 · seguro 2,40 · ajuste 0,00 · total final 194,94 · 1× de 194,94 · crédito 0,00 · brinde não

**Exemplo 6**: Meia esportiva (`MEI-001`) × 1, `ECONOMY` (a TransLog respondeu `{ "price_cents": 2210, "eta_hours": 150 }`), cupom `FREESHIPPING`, `BANK_SLIP`, clube `BRONZE`, região `SOUTH`
→ subtotal 19,90 · cupom 5,97 · frete 22,10 · prazo 7 · seguro 0,20 · ajuste 3,49 · total final 39,72 · 1× de 39,72 · crédito 0,00 · brinde não

**Exemplo 7**: Jaqueta corta-vento (`JAQ-001`) × 3 + Tênis corrida (`TEN-001`) × 1, `EXPRESS` (a TransLog respondeu `{ "price_cents": 5080, "eta_hours": 49 }`), sem cupom, `PIX`, clube `GOLD`, região `NORTH`
→ subtotal 1.419,60 · cupom 0,00 · frete 0,00 · prazo 6 · seguro 30,00 · ajuste −72,48 · total final 1.377,12 · 1× de 1.377,12 · crédito 70,98 · brinde sim

**Exemplo 8**: Camiseta básica (`CAM-001`) × 1, `STORE_PICKUP`, cupom `FREESHIPPING`, `PIX`, clube `SILVER`, região `SOUTHEAST`
→ recusado: `{ "error": "COUPON_NOT_APPLICABLE" }`, HTTP 422

**Exemplo 9**: Camiseta básica (`CAM-001`) × 2, `EXPRESS` (a TransLog respondeu `{ "price_cents": 2900, "eta_hours": 36 }`), cupom `WELCOME10` (o cliente já tem um pedido pago), `CREDIT_CARD` em 2×, clube `BRONZE`, região `SOUTH`
→ recusado: `{ "error": "COUPON_NOT_APPLICABLE" }`, HTTP 422

**Exemplo 10**: Tênis corrida (`TEN-001`) × 1, `ECONOMY` (a TransLog respondeu `{ "error": "ZIP_NOT_SERVED" }`), sem cupom, `PIX`, clube `BRONZE`, região `NORTH`
→ recusado: `{ "error": "SHIPPING_METHOD_UNAVAILABLE" }`, HTTP 422

---

## O que pesquisei da parte técnica

- Use Java 21 e Javalin 6.7.0, sem Spring.
- O serviço atende na porta que chega pela variável de ambiente `PORT` (se ela não vier, na 8080).
- O `mvn package` precisa gerar um jar que sobe sozinho com `java -jar`.
- Use PostgreSQL 18 para guardar os dados. O endereço, o usuário e a senha do banco chegam pelas variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`. O banco começa vazio: o serviço precisa criar o que precisa ao subir.
- Em produção, o serviço roda em duas máquinas ao mesmo tempo, atrás de um balanceador, as duas usando o mesmo banco.
- O projeto precisa funcionar com `mvn verify`. O computador que roda o `mvn verify` não tem banco de dados nem Docker; as dependências do Maven são baixadas normalmente.

Quando terminar, responda apenas "CONCLUÍDO".

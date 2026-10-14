Oi! Tenho uma loja online de roupas e acessórios e preciso do serviço que cuida dos pedidos: calcula o resumo da compra, registra o pedido, acompanha o pagamento e o envio, e controla o estoque. Não sou programador, mas ando estudando o básico de programação por curiosidade. Vou explicar como o negócio funciona e o que eu preciso que aconteça, e no fim deixei a parte técnica que consegui montar estudando e pesquisando. A pasta está vazia, então é montar tudo do começo.

## Como funciona a compra

O cliente monta o carrinho com os produtos do catálogo, informa o CPF e o e-mail, escolhe como quer receber, pode colocar um cupom e escolhe como vai pagar. Antes de confirmar, o site mostra um resumo com quanto deu cada parte e o valor final. Quando o cliente confirma, o pedido é registrado e fica esperando o pagamento.

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
| `ECONOMICA` | a transportadora TransLog | a TransLog informa |
| `EXPRESSA` | a transportadora TransLog | a TransLog informa |
| `RETIRADA_LOJA` | grátis | 1 dia |
| `MOTOBOY` | R$ 18,00 | 0 dias (mesmo dia) |

O peso do pedido é a soma do peso de cada item × quantidade, sem arredondar.

O motoboy só leva pedidos de até 5 kg. Acima disso, essa opção não está disponível.

Na econômica e na expressa, quem diz o valor do frete e o prazo é a TransLog, pelo site dela (como consultar está no anexo). Às vezes a TransLog não atende algum CEP, e às vezes o site dela sai do ar. Estamos conversando com outra transportadora, a RapidoLog, e pode ser que a gente troque ou passe a usar as duas.

Estamos fechando parceria com transportadoras novas o tempo todo, e quase toda semana entra uma opção nova de entrega, cada uma com seu jeito de cobrar, seu prazo e suas limitações.

## Cupons

O pessoal do marketing adora inventar promoção. Os cupons que valem hoje são estes:

- **BEMVINDO10**: 10% de desconto no valor dos produtos, só na primeira compra do cliente. Conta como primeira compra quando o CPF ainda não tem nenhum pedido pago, enviado ou entregue (um pedido cancelado não conta).
- **MENOS50**: R$ 50,00 de desconto nos produtos, só para compras a partir de R$ 300,00 em produtos.
- **FRETEGRATIS**: o cliente não paga o frete. No resumo, o frete aparece normalmente e o desconto do cupom fica igual ao valor do frete. Não vale para retirada na loja, que já não tem frete.
- **LEVE3PAGUE2**: a cada 3 unidades de um mesmo produto do carrinho, uma sai de graça.

Só dá para usar um cupom por pedido. O código do cupom é sempre em letras maiúsculas, exatamente como está acima. Por enquanto os cupons ficam fixos no sistema, não precisa de cadastro.

## Clube da loja

Todo cliente tem um nível no clube, e o nível muda o que a pessoa recebe:

- **BRONZE**: é só o cadastro, não ganha nada.
- **PRATA**: ganha 2% do valor dos produtos de volta, em crédito para a próxima compra.
- **OURO**: ganha 5% dos produtos de volta em crédito, não paga frete, e se os produtos passarem de R$ 500,00 a gente manda um brinde junto. O motoboy é terceirizado, então no motoboy o OURO paga o frete normalmente.

O crédito não abate nada nesta compra: ele entra no saldo do cliente quando o pagamento é confirmado e fica guardado para as próximas. Quando o OURO não paga frete, o frete sai zerado no resumo.

Estamos estudando criar mais níveis, e cada um vai ter seu conjunto de vantagens.

## Depois que o cliente confirma

O pedido passa por estas situações:

- **AGUARDANDO_PAGAMENTO**: o pedido foi registrado e o banco ainda não avisou o pagamento.
- **PAGO**: o banco avisou que o pagamento caiu.
- **ENVIADO**: o pacote saiu, com o código de rastreio.
- **ENTREGUE**: o cliente recebeu.
- **CANCELADO**: o pedido não vai mais acontecer.

Só dá para enviar um pedido pago, e só dá para entregar um pedido enviado. O cliente pode cancelar enquanto o pedido não foi enviado; depois de enviado, não dá mais. Se o pagamento chegar mais de 48 horas depois do registro do pedido, a gente não aceita: o pedido é cancelado. Com exatamente 48 horas, ainda aceita.

Estamos pensando em criar uma situação "em separação", entre o pago e o enviado, para o pessoal do estoque.

Quando o pagamento é confirmado, três coisas precisam acontecer: baixar o estoque dos produtos, colocar o crédito do clube no saldo do cliente e avisar o cliente de que o pagamento foi aprovado. Por enquanto, o aviso é só registrar a mensagem numa lista que o atendimento consulta; o e-mail de verdade vem depois. O marketing já pediu para, no futuro, também mandar SMS e dar pontos num parceiro de cashback quando o pagamento cair.

Se, na hora do pagamento, não houver estoque para todos os itens, o pedido é cancelado e o cliente é avisado.

O banco às vezes avisa o mesmo pagamento duas vezes. O mesmo aviso repetido não pode baixar o estoque duas vezes, nem dar o crédito duas vezes.

Quando um pedido pago é cancelado, o estoque volta, o crédito do clube sai do saldo do cliente e o cliente é avisado. Quando um pedido ainda não pago é cancelado, não há nada para devolver. Todo cancelamento, de qualquer jeito que aconteça, gera um aviso para o cliente.

O cliente passa a existir no sistema quando registra o primeiro pedido, com saldo de crédito zero.

Os pedidos, o estoque e o saldo dos clientes precisam ficar guardados: se o serviço reiniciar, nada pode se perder.

## O que o atendimento mais responde

Juntei aqui as dúvidas que os clientes mais mandam no WhatsApp, pode ajudar a entender o negócio:

- **"Qual o prazo de entrega?"** Depende da opção escolhida, aparece no resumo.
- **"Pagando no Pix tem desconto?"** Tem sim, 5% de desconto no total do pedido, mas só para quem não usou cupom.
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
  | Sudeste | 1% | sem limite |
  | Sul | 1% | sem limite |
  | Centro-Oeste | 1,5% | sem limite |
  | Norte | 2,5% | R$ 30,00 |
  | Nordeste | 2% | R$ 30,00 |

  O seguro é a porcentagem sobre o valor dos produtos, sem desconto e sem frete, arredondada para centavos; quando a região tem limite e a conta passa dele, o seguro fica no limite.
- O crédito do clube é sobre o valor dos produtos, sem desconto e sem frete, arredondado para centavos.
- Não aceitamos boleto quando o total do pedido passa de R$ 1.000,00.

---

## Anexo: como o site vai chamar o serviço

Dei uma pesquisada e consegui montar assim. Por favor, siga exatamente estes nomes e formatos. Todos os valores em dinheiro vêm e vão como número com ponto e 2 casas decimais (79.90); quantidades e parcelas, como número inteiro; datas e horas no formato `2026-10-14T15:30:00Z`.

### O catálogo inicial

| sku | nome | preço | peso (kg) | estoque | fora de linha |
|---|---|---|---|---|---|
| `CAM-001` | Camiseta básica | 79.90 | 0.30 | 50 | não |
| `TEN-001` | Tênis corrida | 249.90 | 1.20 | 10 | não |
| `FON-001` | Fone sem fio | 199.90 | 0.25 | 5 | não |
| `MEI-001` | Meia esportiva | 19.90 | 0.10 | 100 | não |
| `JAQ-001` | Jaqueta corta-vento | 389.90 | 0.80 | 3 | não |
| `MOC-001` | Mochila urbana | 159.90 | 0.90 | 1 | não |
| `BON-001` | Boné aba reta | 59.90 | 0.15 | 0 | não |
| `REL-001` | Relógio digital | 129.90 | 0.20 | 8 | sim |

### Os dados da compra

O resumo e o registro do pedido recebem os mesmos dados, em JSON:

| campo | o que é | exemplo |
|---|---|---|
| `cliente` | quem está comprando: `cpf` (11 números, sem pontos) e `email` | `{ "cpf": "12345678909", "email": "ana@exemplo.com" }` |
| `itens` | a lista do carrinho. Cada item tem `sku` e `quantidade` | `[{ "sku": "CAM-001", "quantidade": 2 }]` |
| `cep` | o CEP de entrega, 8 números, sem traço | `"01310100"` |
| `regiao` | onde o cliente mora: `SUDESTE`, `SUL`, `CENTRO_OESTE`, `NORTE` ou `NORDESTE` | `SUDESTE` |
| `modalidadeEntrega` | `ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA` ou `MOTOBOY` | `EXPRESSA` |
| `cupom` | o código do cupom; pode não vir | `BEMVINDO10` |
| `formaPagamento` | `PIX`, `CARTAO` ou `BOLETO` | `PIX` |
| `parcelas` | em quantas vezes; pode não vir, e nesse caso é 1 | 1 |
| `nivelClube` | `BRONZE`, `PRATA` ou `OURO` | `OURO` |

### O que o serviço oferece

| o que faz | como o site chama | o que volta quando dá certo |
|---|---|---|
| calcular o resumo, sem registrar nada | `POST /checkout/resumo` com os dados da compra | `200` e o resumo |
| registrar o pedido | `POST /pedidos` com os dados da compra | `201` e o pedido |
| consultar um pedido | `GET /pedidos/{id}` | `200` e o pedido |
| o banco avisa o pagamento | `POST /pedidos/{id}/pagamento` com `{ "idTransacao": "TX-123", "pagoEm": "2026-10-14T16:00:00Z" }` | `200` e o pedido |
| enviar | `POST /pedidos/{id}/envio` com `{ "codigoRastreio": "BR123456789" }` | `200` e o pedido |
| entregar | `POST /pedidos/{id}/entrega` | `200` e o pedido |
| cancelar | `POST /pedidos/{id}/cancelamento` | `200` e o pedido |
| consultar um produto | `GET /produtos/{sku}` | `200` e o produto |
| consultar o saldo do cliente | `GET /clientes/{cpf}` | `200` e `{ "cpf": "…", "saldoCredito": 20.48 }` |
| ver os avisos de um cliente | `GET /clientes/{cpf}/avisos` | `200` e a lista de avisos, do mais antigo para o mais novo |

**O resumo** tem os campos: `subtotalProdutos`, `descontoCupom`, `frete`, `prazoEntregaDias`, `seguro`, `ajustePagamento` (o valor final menos o total do pedido: negativo quando é desconto, positivo quando é tarifa ou juros, zero quando não muda nada), `totalFinal`, `parcelas`, `valorParcela`, `creditoProximaCompra` e `brinde` (`true` ou `false`).

**O pedido** tem os campos: `id`, `situacao`, `criadoEm`, `resumo` (o resumo calculado quando o pedido foi registrado, que não muda mais), `itens` (cada um com `sku` e `quantidade`) e `codigoRastreio` (vazio até o envio).

**O produto** tem os campos: `sku`, `nome`, `preco`, `pesoKg`, `estoque` e `foraDeLinha` (`true` ou `false`).

**Cada aviso** tem os campos `tipo` (`PAGAMENTO_APROVADO`, `PEDIDO_ENVIADO` ou `PEDIDO_CANCELADO`) e `pedidoId`.

O mesmo aviso de pagamento repetido (mesmo pedido e mesmo `idTransacao`) devolve `200` e o pedido como está, sem fazer nada de novo.

### A TransLog

O endereço do site da TransLog chega ao serviço pela variável de ambiente `TRANSLOG_URL`. Para saber o frete e o prazo:

`GET {TRANSLOG_URL}/cotacao?cep=01310100&pesoKg=1.80&servico=EXPRESSA`

Quando a TransLog atende, ela responde `200` com `{ "valor": 33.10, "prazoDias": 2 }`. Quando não atende aquele CEP, responde `404`. Se ela responder qualquer outra coisa, ou não responder em até 2 segundos, considere a TransLog fora do ar.

### Quando dá erro

Quando não dá para fazer o que foi pedido, o serviço recusa e devolve só o código do problema, assim: `{ "erro": "CODIGO" }`, com a situação HTTP da tabela.

**No resumo e no registro do pedido**, conferir nesta ordem e devolver o primeiro problema encontrado:

| Ordem | Situação | Código | HTTP |
|---|---|---|---|
| 1 | Carrinho vazio, item sem SKU, o mesmo SKU duas vezes no carrinho, ou quantidade zero, negativa ou ausente; CPF ou e-mail ausente | `PEDIDO_INVALIDO` | 422 |
| 2 | Algum SKU que não existe no catálogo | `PRODUTO_INEXISTENTE` | 422 |
| 3 | Algum produto fora de linha | `PRODUTO_FORA_DE_LINHA` | 422 |
| 4 | Algum produto sem estoque suficiente para a quantidade pedida | `ESTOQUE_INSUFICIENTE` | 422 |
| 5 | Nível do clube que não existe ou não informado | `NIVEL_CLUBE_INVALIDO` | 422 |
| 6 | Região que não existe ou não informada | `REGIAO_INVALIDA` | 422 |
| 7 | CEP ausente ou fora do formato | `CEP_INVALIDO` | 422 |
| 8 | Opção de entrega que não existe ou não informada | `MODALIDADE_INVALIDA` | 422 |
| 9 | Opção de entrega existe, mas não atende o pedido (motoboy acima de 5 kg, ou CEP que a TransLog não atende) | `MODALIDADE_INDISPONIVEL` | 422 |
| 10 | A TransLog está fora do ar | `TRANSPORTADORA_INDISPONIVEL` | 503 |
| 11 | Cupom informado que não existe | `CUPOM_INVALIDO` | 422 |
| 12 | Cupom existe, mas o pedido não cumpre a condição | `CUPOM_NAO_APLICAVEL` | 422 |
| 13 | Forma de pagamento que não existe ou não informada | `FORMA_PAGAMENTO_INVALIDA` | 422 |
| 14 | Número de parcelas não permitido para a forma de pagamento (Pix e boleto só 1; cartão de 1 a 12) | `PARCELAMENTO_INVALIDO` | 422 |
| 15 | Forma de pagamento existe, mas não atende o pedido (boleto acima de R$ 1.000,00) | `FORMA_PAGAMENTO_INDISPONIVEL` | 422 |

**Nas operações de um pedido** (pagamento, envio, entrega, cancelamento e consulta), conferir nesta ordem:

| Situação | Código | HTTP |
|---|---|---|
| Pedido que não existe | `PEDIDO_NAO_ENCONTRADO` | 404 |
| A operação não é permitida na situação atual do pedido | `OPERACAO_NAO_PERMITIDA` | 409 |
| Pagamento chegou depois de 48 horas do registro (o pedido vai para `CANCELADO`) | `PEDIDO_EXPIRADO` | 409 |
| Na hora do pagamento, faltou estoque (o pedido vai para `CANCELADO`) | `ESTOQUE_INSUFICIENTE` | 409 |

Produto ou cliente que não existe nas consultas: `404` com `PRODUTO_NAO_ENCONTRADO` ou `CLIENTE_NAO_ENCONTRADO`.

### Exemplos conferidos pelo financeiro

[A PREENCHER: os exemplos saem da calculadora de referência e são conferidos pelo check-prompt, como no V5. Previstos: um por regra cruzada (OURO no motoboy, FRETEGRATIS na retirada, Pix com cupom, BEMVINDO10 na segunda compra, seguro no teto), um de juros no cartão e um ciclo completo de pedido: registrar, pagar, enviar, entregar.]

---

## O que pesquisei da parte técnica

- Use Java 21 e Spring Boot 4.1.1.
- Use PostgreSQL 18 para guardar os dados. O endereço, o usuário e a senha do banco chegam pelas variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`. O banco começa vazio: o serviço precisa criar o que precisa ao subir.
- O projeto precisa funcionar com `mvn verify`. O computador que roda o `mvn verify` não tem banco de dados nem internet.

Quando terminar, responda apenas "CONCLUÍDO".

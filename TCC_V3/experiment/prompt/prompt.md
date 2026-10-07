Oi! Tenho uma loja online de roupas e acessórios e preciso do serviço que calcula o resumo da compra na hora de finalizar o pedido. Não sou programador, mas ando estudando o básico de programação por curiosidade. Vou explicar como o negócio funciona e o que eu preciso que aconteça, e no fim deixei a parte técnica que consegui montar estudando e pesquisando. A pasta está vazia, então é montar tudo do começo.

## Como funciona a compra

O cliente monta o carrinho com os produtos, escolhe como quer receber, pode colocar um cupom e escolhe como vai pagar. Antes de confirmar, o site mostra um resumo com quanto deu cada parte e o valor final. É esse resumo que o serviço precisa calcular.

O valor final é calculado nesta ordem:

1. Soma dos produtos (preço de cada item × quantidade).
2. Desconto do cupom, se o cliente usou um.
3. Frete, de acordo com a forma de entrega escolhida.
4. Seguro do envio, de acordo com a região do cliente.
5. Total do pedido = produtos − desconto do cupom + frete + seguro.
6. Ajuste da forma de pagamento sobre esse total.

Todo valor em dinheiro é arredondado para centavos em cada etapa, usando o arredondamento "meio para o par" (exemplo: 2,995 vira 3,00 e 2,985 vira 2,98).

## Entrega

Hoje temos estas opções de entrega:

| Opção | Quanto custa | Prazo |
|---|---|---|
| `ECONOMICA` | R$ 12,00 + R$ 2,00 por kg do pedido | 7 dias |
| `EXPRESSA` | R$ 25,00 + R$ 4,50 por kg do pedido | 2 dias |
| `RETIRADA_LOJA` | Grátis | 1 dia |
| `MOTOBOY` | R$ 18,00 | 0 dias (mesmo dia) |

O peso do pedido é a soma do peso de cada item × quantidade, sem arredondar.

O motoboy só leva pedidos de até 5 kg. Acima disso, essa opção não está disponível.

Estamos fechando parceria com transportadoras novas o tempo todo. Quase toda semana entra uma opção nova de entrega, cada uma com seu jeito de cobrar, seu prazo e suas limitações.

## Cupons

O pessoal do marketing adora inventar promoção. Os cupons que valem hoje são estes:

- **BEMVINDO10**: 10% de desconto no valor dos produtos.
- **MENOS50**: R$ 50,00 de desconto nos produtos, só para compras a partir de R$ 300,00 em produtos.
- **FRETEGRATIS**: o cliente não paga o frete. No resumo, o frete aparece normalmente e o desconto do cupom fica igual ao valor do frete.
- **LEVE3PAGUE2**: a cada 3 unidades de um mesmo item do carrinho, uma sai de graça.

Só dá para usar um cupom por pedido. O código do cupom é sempre em letras maiúsculas, exatamente como está acima. Por enquanto os cupons ficam fixos no sistema, não precisa de cadastro.

## Clube da loja

Todo cliente tem um nível no clube, e o nível muda o que a pessoa recebe:

- **BRONZE**: é só o cadastro, não ganha nada.
- **PRATA**: ganha 2% do valor dos produtos de volta, em crédito para a próxima compra.
- **OURO**: ganha 5% dos produtos de volta em crédito, **não paga frete nunca**, e se os produtos passarem de R$ 500,00 a gente manda um brinde junto.

O crédito não abate nada nesta compra, fica guardado para a próxima. Quando o OURO não paga frete, o frete sai zerado no resumo.

Estamos estudando criar mais níveis, e cada um vai ter seu conjunto de vantagens.

## O que o atendimento mais responde

Juntei aqui as dúvidas que os clientes mais mandam no WhatsApp, pode ajudar a entender o negócio:

- **"Qual o prazo de entrega?"** Depende da opção escolhida, aparece no resumo.
- **"Pagando no Pix tem desconto?"** Tem sim, 5% de desconto no total do pedido.
- **"Posso trocar se não servir?"** Pode, em até 7 dias, mas isso é resolvido pelo atendimento, não entra no cálculo.
- **"Dá para parcelar?"** No cartão de crédito, em até 3x sem juros. De 4x até 12x tem juros de 1,99% ao mês.
- **"O boleto tem alguma taxa?"** Tem uma tarifa de R$ 3,49 que o banco cobra, somada ao total.
- **"Pix e boleto parcelam?"** Não, são sempre à vista.
- **"Aceitam outras moedas?"** Não, só reais.

## Observações do financeiro

- O cálculo dos juros do cartão é o mesmo do crediário (tabela Price): **parcela = total × taxa ÷ (1 − (1 + taxa)^−número de parcelas)**. A parcela é arredondada para centavos e o valor final é a parcela × número de parcelas.
- Sem juros (até 3x), o valor final é o próprio total do pedido e a parcela é o total dividido pelo número de parcelas, arredondado para centavos.
- No Pix, o desconto é 5% do total do pedido, arredondado para centavos.
- Todo pedido vai com seguro contra extravio e roubo, e a seguradora cobra por região do cliente: Sudeste 1%, Sul 1%, Centro-Oeste 1,5%, Norte 2,5% e Nordeste 2%. **É só a porcentagem que muda, a conta é a mesma em todas**: a porcentagem sobre o valor dos produtos, sem desconto e sem frete, arredondada para centavos.
- O crédito do clube é sobre o valor dos produtos, sem desconto e sem frete, arredondado para centavos.
- Não aceitamos boleto quando o total do pedido passa de R$ 1.000,00.

---

## Anexo: como o site vai chamar o serviço

Dei uma pesquisada e consegui montar assim. Por favor, siga exatamente estes nomes e formatos.

### O que o site envia

O site envia os dados da compra para o endereço `/checkout/resumo`, neste formato:

```json
{
  "itens": [
    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "parcelas": 1,
  "nivelClube": "OURO",
  "regiao": "SUDESTE"
}
```

- `cupom` pode não vir, quando o cliente não usou cupom.
- `parcelas` pode não vir; nesse caso, é 1.
- Formas de pagamento: `PIX`, `CARTAO`, `BOLETO`.
- Níveis do clube: `BRONZE`, `PRATA`, `OURO`.
- Regiões: `SUDESTE`, `SUL`, `CENTRO_OESTE`, `NORTE`, `NORDESTE`.

### O que o serviço devolve

Quando dá certo, o serviço devolve o resumo assim:

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 0.00,
  "prazoEntregaDias": 2,
  "seguro": 4.10,
  "ajustePagamento": -18.64,
  "totalFinal": 354.19,
  "parcelas": 1,
  "valorParcela": 354.19,
  "creditoProximaCompra": 20.48,
  "brinde": false
}
```

- `ajustePagamento` = `totalFinal` − total do pedido (negativo quando é desconto, positivo quando é tarifa ou juros, zero quando não muda nada).
- Todos os valores em dinheiro com 2 casas decimais.

### Quando dá erro

Quando não dá para calcular, o serviço recusa o pedido e devolve só o código do problema, assim: `{ "erro": "CODIGO" }`. Conferir nesta ordem e devolver o primeiro problema encontrado:

| Ordem | Situação | Código |
|---|---|---|
| 1 | Carrinho vazio, ou algum item com preço, quantidade ou peso zero/negativo/ausente | `PEDIDO_INVALIDO` |
| 2 | Nível do clube que não existe ou não informado | `NIVEL_CLUBE_INVALIDO` |
| 3 | Região que não existe ou não informada | `REGIAO_INVALIDA` |
| 4 | Opção de entrega que não existe ou não informada | `MODALIDADE_INVALIDA` |
| 5 | Opção de entrega existe, mas não atende o pedido (ex.: motoboy acima de 5 kg) | `MODALIDADE_INDISPONIVEL` |
| 6 | Cupom informado que não existe | `CUPOM_INVALIDO` |
| 7 | Cupom existe, mas o pedido não cumpre a condição (ex.: MENOS50 abaixo de R$ 300,00) | `CUPOM_NAO_APLICAVEL` |
| 8 | Forma de pagamento que não existe ou não informada | `FORMA_PAGAMENTO_INVALIDA` |
| 9 | Número de parcelas não permitido para a forma de pagamento (Pix e boleto só 1; cartão de 1 a 12) | `PARCELAMENTO_INVALIDO` |
| 10 | Forma de pagamento existe, mas não atende o pedido (ex.: boleto acima de R$ 1.000,00) | `FORMA_PAGAMENTO_INDISPONIVEL` |

### Exemplos conferidos pelo financeiro

**Exemplo 1**: Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), `EXPRESSA`, cupom `BEMVINDO10`, `PIX`, clube `BRONZE`, região `NORTE`
→ subtotal 409,70 · cupom 40,97 · frete 33,10 · prazo 2 · seguro 10,24 · ajuste −20,60 · total final 391,47 · 1× de 391,47 · crédito 0,00 · brinde não

**Exemplo 2**: mesmos itens, `ECONOMICA`, sem cupom, `CARTAO` em 6×, clube `PRATA`, região `CENTRO_OESTE`
→ subtotal 409,70 · cupom 0,00 · frete 15,60 · prazo 7 · seguro 6,15 · ajuste 30,55 · total final 462,00 · 6× de 77,00 · crédito 8,19 · brinde não

**Exemplo 3**: Fone 199,90 × 2 (0,25 kg), `MOTOBOY`, cupom `MENOS50`, `BOLETO`, clube `BRONZE`, região `NORDESTE`
→ subtotal 399,80 · cupom 50,00 · frete 18,00 · prazo 0 · seguro 8,00 · ajuste 3,49 · total final 379,29 · 1× de 379,29 · crédito 0,00 · brinde não

**Exemplo 4**: Meia 19,90 × 7 (0,10 kg) + Camiseta 79,90 × 2 (0,30 kg), `RETIRADA_LOJA`, cupom `LEVE3PAGUE2`, `CARTAO` em 3×, clube `PRATA`, região `SUL`
→ subtotal 299,10 · cupom 39,80 · frete 0,00 · prazo 1 · seguro 2,99 · ajuste 0,00 · total final 262,29 · 3× de 87,43 · crédito 5,98 · brinde não

**Exemplo 5**: Camiseta 79,90 × 2 (0,30 kg) + Tênis 249,90 × 1 (1,20 kg), `EXPRESSA`, sem cupom, `PIX`, clube `OURO`, região `SUDESTE`
→ subtotal 409,70 · cupom 0,00 · frete 0,00 (OURO não paga) · prazo 2 · seguro 4,10 · ajuste −20,69 · total final 393,11 · 1× de 393,11 · crédito 20,48 · brinde não

---

## O que pesquisei da parte técnica

- Não use banco de dados.
- Use Java 21 e Spring Boot 4.1.1.
- O projeto precisa funcionar com `mvn verify`.

Quando terminar, responda apenas "CONCLUÍDO".

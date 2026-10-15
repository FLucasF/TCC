# Serviço de Cálculo de Resumo de Compra

Serviço REST que calcula o resumo de compra para a loja online de roupas e acessórios, incluindo cálculo de descontos, frete e formas de pagamento.

## Compilação e Testes

```bash
mvn verify
```

## Executar a Aplicação

```bash
mvn spring-boot:run
```

A aplicação iniciará na porta 8080.

## Endpoint

### `POST /checkout/resumo`

Calcula o resumo de um pedido.

#### Request

```json
{
  "itens": [
    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "parcelas": 1
}
```

- `itens`: Array com os produtos do carrinho (obrigatório)
  - `nome`: Descrição do produto
  - `precoUnitario`: Preço unitário em reais
  - `quantidade`: Quantidade de unidades
  - `pesoKg`: Peso em kg
- `modalidadeEntrega`: Opção de entrega (obrigatório)
  - `ECONOMICA`: R$ 12,00 + R$ 2,00/kg (7 dias)
  - `EXPRESSA`: R$ 25,00 + R$ 4,50/kg (2 dias)
  - `RETIRADA_LOJA`: Grátis (1 dia)
  - `MOTOBOY`: R$ 18,00 (0 dias, máximo 5 kg)
- `cupom`: Cupom de desconto (opcional)
  - `BEMVINDO10`: 10% desconto nos produtos
  - `MENOS50`: R$ 50,00 desconto (mínimo R$ 300,00 em produtos)
  - `FRETEGRATIS`: Frete grátis
  - `LEVE3PAGUE2`: A cada 3 unidades de um item, 1 sai de graça
- `formaPagamento`: Forma de pagamento (obrigatório)
  - `PIX`: 5% desconto
  - `CARTAO`: Parcelado até 12x (sem juros até 3x, 1,99% a.m. de 4x a 12x)
  - `BOLETO`: R$ 3,49 taxa (máximo R$ 1.000,00)
- `parcelas`: Número de parcelas (opcional, padrão 1)

#### Response de Sucesso (200)

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 33.10,
  "prazoEntregaDias": 2,
  "ajustePagamento": -20.09,
  "totalFinal": 381.74,
  "parcelas": 1,
  "valorParcela": 381.74
}
```

#### Response de Erro (400)

```json
{
  "erro": "PEDIDO_INVALIDO"
}
```

Possíveis códigos de erro:
- `PEDIDO_INVALIDO`: Carrinho vazio ou item com valores inválidos
- `MODALIDADE_INVALIDA`: Opção de entrega inválida ou ausente
- `MODALIDADE_INDISPONIVEL`: Opção não disponível para o pedido (ex.: motoboy > 5 kg)
- `CUPOM_INVALIDO`: Cupom não existe
- `CUPOM_NAO_APLICAVEL`: Cupom não atende os critérios (ex.: MENOS50 < R$ 300,00)
- `FORMA_PAGAMENTO_INVALIDA`: Forma de pagamento inválida ou ausente
- `PARCELAMENTO_INVALIDO`: Parcelamento não permitido (Pix/Boleto só 1x, Cartão 1-12x)
- `FORMA_PAGAMENTO_INDISPONIVEL`: Forma não disponível para o pedido (ex.: Boleto > R$ 1.000,00)

## Exemplo com cURL

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H "Content-Type: application/json" \
  -d '{
    "itens": [
      {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
      {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
    ],
    "modalidadeEntrega": "EXPRESSA",
    "cupom": "BEMVINDO10",
    "formaPagamento": "PIX",
    "parcelas": 1
  }'
```

## Detalhes de Cálculo

### Arredondamento
Todos os valores em dinheiro são arredondados para centavos usando o arredondamento "meio para o par" (HALF_EVEN).

### Ordem de Cálculo
1. Subtotal: soma dos preços multiplicados pelas quantidades
2. Desconto do cupom (aplicado ao subtotal ou ao frete)
3. Frete: baseado na modalidade e peso total do pedido
4. Total antes do ajuste: subtotal - cupom + frete
5. Ajuste de pagamento: desconto ou tarifa/juros da forma de pagamento
6. Total final: total antes do ajuste + ajuste

### Juros do Cartão
Utiliza a fórmula Price: `parcela = total × taxa ÷ (1 − (1 + taxa)^−número_parcelas)`
- Taxa: 1,99% ao mês
- Sem juros: até 3x
- Com juros: 4x até 12x

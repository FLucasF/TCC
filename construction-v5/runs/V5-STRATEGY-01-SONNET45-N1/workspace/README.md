# Serviço de Checkout

Serviço REST para cálculo do resumo de compra de uma loja online.

## Executar o projeto

```bash
mvn verify
```

## Endpoint

**POST** `/checkout/resumo`

### Exemplo de requisição

```json
{
  "itens": [
    {
      "nome": "Camiseta",
      "precoUnitario": 79.90,
      "quantidade": 2,
      "pesoKg": 0.30
    }
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "parcelas": 1,
  "nivelClube": "BRONZE",
  "regiao": "SUDESTE"
}
```

### Resposta de sucesso

```json
{
  "subtotalProdutos": 159.80,
  "descontoCupom": 15.98,
  "frete": 27.40,
  "prazoEntregaDias": 2,
  "seguro": 1.60,
  "ajustePagamento": -8.59,
  "totalFinal": 163.23,
  "parcelas": 1,
  "valorParcela": 163.23,
  "creditoProximaCompra": 0.00,
  "brinde": false
}
```

### Resposta de erro

```json
{
  "erro": "CUPOM_INVALIDO"
}
```

## Códigos de erro

- `PEDIDO_INVALIDO`: Carrinho vazio ou item com valores inválidos
- `NIVEL_CLUBE_INVALIDO`: Nível do clube inexistente
- `REGIAO_INVALIDA`: Região inexistente
- `MODALIDADE_INVALIDA`: Modalidade de entrega inexistente
- `MODALIDADE_INDISPONIVEL`: Modalidade não atende o pedido
- `CUPOM_INVALIDO`: Cupom inexistente
- `CUPOM_NAO_APLICAVEL`: Cupom não se aplica ao pedido
- `FORMA_PAGAMENTO_INVALIDA`: Forma de pagamento inexistente
- `PARCELAMENTO_INVALIDO`: Número de parcelas inválido
- `FORMA_PAGAMENTO_INDISPONIVEL`: Forma de pagamento não disponível

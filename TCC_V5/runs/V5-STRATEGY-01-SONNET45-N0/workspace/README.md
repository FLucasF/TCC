# Serviço de Checkout - Loja Online

Serviço REST para calcular o resumo de compra da loja online.

## Requisitos

- Java 21
- Maven 3.6+

## Executar

```bash
mvn verify
```

## Endpoint

**POST** `/checkout/resumo`

### Exemplo de Request

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
  "modalidadeEntrega": "ECONOMICA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "parcelas": 1,
  "nivelClube": "BRONZE",
  "regiao": "SUDESTE"
}
```

### Exemplo de Response (Sucesso)

```json
{
  "subtotalProdutos": 159.80,
  "descontoCupom": 15.98,
  "frete": 13.20,
  "prazoEntregaDias": 7,
  "seguro": 1.60,
  "ajustePagamento": -7.33,
  "totalFinal": 139.29,
  "parcelas": 1,
  "valorParcela": 139.29,
  "creditoProximaCompra": 0.00,
  "brinde": false
}
```

### Exemplo de Response (Erro)

```json
{
  "erro": "PEDIDO_INVALIDO"
}
```

## Testes

O projeto inclui 11 testes automatizados que validam:
- Os 5 exemplos fornecidos pelo financeiro
- As principais validações de erro

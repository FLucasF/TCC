# Checkout Resumo

Serviço de cálculo de resumo de compra para loja online de roupas e acessórios.

## Build

```bash
mvn clean verify
```

## Executar

```bash
java -jar target/checkout-resumo-1.0.0.jar
```

A aplicação inicia na porta 8080.

## API

### POST /checkout/resumo

Calcula o resumo da compra com todos os descontos, fretes e ajustes de pagamento.

#### Request

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
  "parcelas": 1
}
```

#### Response (200)

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

#### Response (400)

```json
{
  "erro": "PEDIDO_INVALIDO"
}
```

## Recursos

- ✅ 4 tipos de cupons (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2)
- ✅ 4 modalidades de entrega (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY)
- ✅ 3 formas de pagamento (PIX, CARTAO, BOLETO)
- ✅ Parcelamento em cartão (1-12x com juros conforme necessário)
- ✅ Arredondamento "meio para o par" em todas as operações
- ✅ Validações de negócio em ordem de precedência
- ✅ 17 testes unitários cobrindo todos os cenários

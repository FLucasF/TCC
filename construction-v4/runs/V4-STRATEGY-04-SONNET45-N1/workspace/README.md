# Serviço de Checkout

Serviço REST para cálculo de resumo de compra de loja online.

## Requisitos

- Java 21
- Maven 3.6+

## Executar

### Compilar e testar

```bash
mvn verify
```

### Rodar a aplicação

```bash
mvn spring-boot:run
```

O serviço estará disponível em `http://localhost:8080`

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
    },
    {
      "nome": "Tênis",
      "precoUnitario": 249.90,
      "quantidade": 1,
      "pesoKg": 1.20
    }
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "nivelClube": "BRONZE",
  "regiao": "NORTE"
}
```

### Exemplo de resposta

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 33.10,
  "prazoEntregaDias": 2,
  "seguro": 10.24,
  "ajustePagamento": -20.60,
  "totalFinal": 391.47,
  "parcelas": 1,
  "valorParcela": 391.47,
  "creditoProximaCompra": 0.00,
  "brinde": false
}
```

### Testar com curl

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H "Content-Type: application/json" \
  -d '{
    "itens": [
      {
        "nome": "Camiseta",
        "precoUnitario": 79.90,
        "quantidade": 2,
        "pesoKg": 0.30
      }
    ],
    "modalidadeEntrega": "EXPRESSA",
    "formaPagamento": "PIX",
    "nivelClube": "BRONZE",
    "regiao": "SUDESTE"
  }'
```

# Serviço de Checkout

Serviço REST para cálculo de resumo de compra de uma loja online.

## Requisitos

- Java 21
- Maven

## Executar o projeto

```bash
mvn spring-boot:run
```

O serviço ficará disponível em `http://localhost:8080`

## Executar os testes

```bash
mvn test
```

## Build e verificação

```bash
mvn verify
```

## Endpoint

### POST /checkout/resumo

Calcula o resumo da compra com todos os valores e descontos aplicados.

**Request:**
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
  "regiao": "NORTE"
}
```

**Response (sucesso):**
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

**Response (erro):**
```json
{
  "erro": "PEDIDO_INVALIDO"
}
```

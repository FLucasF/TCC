# Serviço de Checkout

Serviço REST para cálculo de resumo de compras de uma loja online.

## Requisitos

- Java 21
- Maven

## Como executar

```bash
mvn spring-boot:run
```

O serviço será iniciado na porta 8080.

## Como usar

Enviar uma requisição POST para `/checkout/resumo` com o corpo JSON contendo os dados do pedido.

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

### Exemplo de resposta (sucesso)

```json
{
  "subtotalProdutos": 159.80,
  "descontoCupom": 15.98,
  "frete": 27.20,
  "prazoEntregaDias": 2,
  "seguro": 1.60,
  "ajustePagamento": -8.53,
  "totalFinal": 162.09,
  "parcelas": 1,
  "valorParcela": 162.09,
  "creditoProximaCompra": 0.00,
  "brinde": false
}
```

### Exemplo de resposta (erro)

```json
{
  "erro": "CUPOM_INVALIDO"
}
```

## Executar testes

```bash
mvn verify
```

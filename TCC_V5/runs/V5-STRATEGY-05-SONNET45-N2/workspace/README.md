# Serviço de Checkout - Loja Online

Serviço REST para cálculo de resumo de compra.

## Requisitos

- Java 21
- Maven 3.x

## Executar o projeto

```bash
mvn spring-boot:run
```

O serviço estará disponível em: http://localhost:8080

## Executar testes

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
  "frete": 27.70,
  "prazoEntregaDias": 2,
  "seguro": 1.60,
  "ajustePagamento": -8.66,
  "totalFinal": 164.46,
  "parcelas": 1,
  "valorParcela": 164.46,
  "creditoProximaCompra": 0.00,
  "brinde": false
}
```

### Resposta de erro

```json
{
  "erro": "CODIGO_ERRO"
}
```

## Códigos de erro

- `PEDIDO_INVALIDO` - Carrinho vazio ou item com valores inválidos
- `NIVEL_CLUBE_INVALIDO` - Nível do clube não existe
- `REGIAO_INVALIDA` - Região não existe
- `MODALIDADE_INVALIDA` - Modalidade de entrega não existe
- `MODALIDADE_INDISPONIVEL` - Modalidade existe mas não atende o pedido
- `CUPOM_INVALIDO` - Cupom não existe
- `CUPOM_NAO_APLICAVEL` - Cupom existe mas pedido não cumpre condições
- `FORMA_PAGAMENTO_INVALIDA` - Forma de pagamento não existe
- `PARCELAMENTO_INVALIDO` - Número de parcelas não permitido
- `FORMA_PAGAMENTO_INDISPONIVEL` - Forma de pagamento existe mas não atende o pedido

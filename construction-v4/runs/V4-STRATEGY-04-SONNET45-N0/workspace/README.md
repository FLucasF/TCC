# Serviço de Checkout - Loja Online

Serviço REST para calcular o resumo de compras, incluindo produtos, cupons, frete, seguro e formas de pagamento.

## Tecnologias

- Java 21
- Spring Boot 4.1.1
- Maven

## Como executar

### Compilar e testar

```bash
mvn verify
```

### Executar o serviço

```bash
mvn spring-boot:run
```

O serviço estará disponível em: `http://localhost:8080`

## Endpoint

### POST /checkout/resumo

Calcula o resumo da compra.

**Exemplo de requisição:**

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
  "parcelas": 1,
  "nivelClube": "BRONZE",
  "regiao": "NORTE"
}
```

**Exemplo de resposta (sucesso):**

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

**Exemplo de resposta (erro):**

```json
{
  "erro": "CUPOM_INVALIDO"
}
```

## Regras de negócio

### Modalidades de entrega
- `ECONOMICA`: R$ 12,00 + R$ 2,00 por kg (7 dias)
- `EXPRESSA`: R$ 25,00 + R$ 4,50 por kg (2 dias)
- `RETIRADA_LOJA`: Grátis (1 dia)
- `MOTOBOY`: R$ 18,00 (mesmo dia, máximo 5 kg)

### Cupons disponíveis
- `BEMVINDO10`: 10% de desconto nos produtos
- `MENOS50`: R$ 50,00 de desconto (mínimo R$ 300,00 em produtos)
- `FRETEGRATIS`: Frete grátis
- `LEVE3PAGUE2`: A cada 3 unidades do mesmo item, 1 sai grátis

### Níveis do clube
- `BRONZE`: Sem benefícios
- `PRATA`: 2% de crédito para próxima compra
- `OURO`: 5% de crédito, frete grátis, brinde se produtos > R$ 500,00

### Formas de pagamento
- `PIX`: 5% de desconto no total
- `BOLETO`: Tarifa de R$ 3,49 (máximo R$ 1.000,00)
- `CARTAO`: Até 3x sem juros, 4-12x com juros de 1,99% ao mês

### Regiões e seguro
- `SUDESTE`: 1%
- `SUL`: 1%
- `CENTRO_OESTE`: 1,5%
- `NORTE`: 2,5%
- `NORDESTE`: 2%

## Códigos de erro

- `PEDIDO_INVALIDO`: Carrinho vazio ou item inválido
- `NIVEL_CLUBE_INVALIDO`: Nível do clube inválido
- `REGIAO_INVALIDA`: Região inválida
- `MODALIDADE_INVALIDA`: Modalidade de entrega inválida
- `MODALIDADE_INDISPONIVEL`: Modalidade não atende o pedido
- `CUPOM_INVALIDO`: Cupom não existe
- `CUPOM_NAO_APLICAVEL`: Cupom não atende os requisitos
- `FORMA_PAGAMENTO_INVALIDA`: Forma de pagamento inválida
- `PARCELAMENTO_INVALIDO`: Número de parcelas não permitido
- `FORMA_PAGAMENTO_INDISPONIVEL`: Forma de pagamento não atende o pedido

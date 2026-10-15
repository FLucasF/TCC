# Serviço de Checkout

Serviço de cálculo de resumo de compra para loja online de roupas e acessórios.

## Funcionalidades

- Cálculo de subtotal dos produtos
- Aplicação de cupons (BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2)
- Cálculo de frete (ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY)
- Cálculo de impostos por região
- Cálculo de ajustes por forma de pagamento (Pix, Cartão, Boleto)
- Parcelamento com juros para cartão
- Crédito do clube para próxima compra
- Brinde para clientes Ouro

## Build e Execução

### Pré-requisitos
- Java 21
- Maven 3.8+

### Compilar
```bash
mvn clean compile
```

### Executar testes
```bash
mvn test
```

### Gerar pacote
```bash
mvn package
```

### Rodar a aplicação
```bash
java -jar target/checkout-service-1.0.0.jar
```

A aplicação iniciará na porta `8080`.

## Endpoint

### POST /checkout/resumo

Calcula o resumo de compra antes de finalizar o pedido.

#### Requisição

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

**Campos opcionais:**
- `cupom`: null ou vazio para sem cupom
- `parcelas`: padrão é 1 se não informado

**Valores permitidos:**
- `modalidadeEntrega`: ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY
- `formaPagamento`: PIX, CARTAO, BOLETO
- `nivelClube`: BRONZE, PRATA, OURO
- `regiao`: SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE
- `cupom`: BEMVINDO10, MENOS50, FRETEGRATIS, LEVE3PAGUE2

#### Resposta (200 OK)

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 33.10,
  "prazoEntregaDias": 2,
  "imposto": 49.16,
  "ajustePagamento": -20.09,
  "totalFinal": 381.74,
  "parcelas": 1,
  "valorParcela": 381.74,
  "creditoProximaCompra": 20.48,
  "brinde": false
}
```

#### Resposta (400 Bad Request)

```json
{
  "erro": "CUPOM_INVALIDO"
}
```

**Códigos de erro:**
- `PEDIDO_INVALIDO`: carrinho vazio ou item inválido
- `NIVEL_CLUBE_INVALIDO`: nível do clube inválido ou não informado
- `REGIAO_INVALIDA`: região inválida ou não informada
- `MODALIDADE_INVALIDA`: modalidade de entrega inválida ou não informada
- `MODALIDADE_INDISPONIVEL`: modalidade não suporta o peso do pedido
- `CUPOM_INVALIDO`: cupom inexistente
- `CUPOM_NAO_APLICAVEL`: cupom não atende os critérios
- `FORMA_PAGAMENTO_INVALIDA`: forma de pagamento inválida
- `PARCELAMENTO_INVALIDO`: número de parcelas não permitido
- `FORMA_PAGAMENTO_INDISPONIVEL`: forma de pagamento não aceita (ex: boleto > R$1000)

## Regras de Negócio

### Cupons
- **BEMVINDO10**: 10% de desconto nos produtos
- **MENOS50**: R$ 50,00 de desconto (mínimo R$ 300,00 em produtos)
- **FRETEGRATIS**: frete grátis
- **LEVE3PAGUE2**: a cada 3 unidades iguais, 1 sai grátis

### Entregas
- **ECONOMICA**: R$ 12,00 + R$ 2,00/kg (7 dias)
- **EXPRESSA**: R$ 25,00 + R$ 4,50/kg (2 dias)
- **RETIRADA_LOJA**: Grátis (1 dia)
- **MOTOBOY**: R$ 18,00 (mesmo dia, máximo 5kg)

### Clube
- **BRONZE**: sem benefícios
- **PRATA**: 2% de crédito na próxima compra
- **OURO**: 5% de crédito, frete grátis, brinde acima de R$ 500

### Impostos por Região
- Sudeste: 12%
- Sul: 11%
- Centro-Oeste: 9%
- Norte: 7%
- Nordeste: 7%

### Formas de Pagamento
- **PIX**: 5% de desconto
- **CARTAO**: até 3x sem juros, 4x-12x com 1,99% ao mês
- **BOLETO**: taxa de R$ 3,49 (não aceito acima de R$ 1.000)

## Testes

O projeto contém 21 testes cobrindo:
- Exemplos conferidos pelo financeiro
- Validações de entrada
- Cálculos de cupons
- Cálculos de frete
- Parcelamentos
- Imposto por região

Execute com:
```bash
mvn test
```

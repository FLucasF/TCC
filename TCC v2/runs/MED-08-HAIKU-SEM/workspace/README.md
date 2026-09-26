# Checkout Resumo - Serviço de Cálculo de Resumo de Compra

Serviço Spring Boot que calcula o resumo da compra para uma loja online de roupas e acessórios.

## Compilação e Testes

```bash
mvn verify
```

O projeto compila com Java 21 e Spring Boot 4.1.1, passando todos os testes unitários.

## Executar a Aplicação

```bash
java -jar target/checkout-resumo-1.0.0.jar
```

A aplicação será inicializada na porta 8080.

## Endpoint

`POST /checkout/resumo`

### Request

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

### Response (Sucesso - 200)

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

### Response (Erro - 400)

```json
{
  "erro": "PEDIDO_INVALIDO"
}
```

## Funcionalidades

### Modalidades de Entrega

- **ECONOMICA**: R$ 12,00 + R$ 2,00 por kg (7 dias)
- **EXPRESSA**: R$ 25,00 + R$ 4,50 por kg (2 dias)
- **RETIRADA_LOJA**: Grátis (1 dia)
- **MOTOBOY**: R$ 18,00 - máximo 5 kg (mesmo dia)

### Cupons Válidos

- **BEMVINDO10**: 10% de desconto nos produtos
- **MENOS50**: R$ 50,00 de desconto (mínimo R$ 300,00 de compra)
- **FRETEGRATIS**: Frete gratuito
- **LEVE3PAGUE2**: A cada 3 unidades do mesmo item, uma sai de graça

### Formas de Pagamento

- **PIX**: 5% de desconto no total
- **CARTAO**: 
  - Sem juros: até 3x
  - Com juros de 1,99% ao mês: 4x a 12x (fórmula Price)
- **BOLETO**: Taxa de R$ 3,49 (máximo R$ 1.000,00)

### Arredondamento

Todos os valores em reais são arredondados para centavos usando o método "meio para o par" (HALF_EVEN) em cada etapa do cálculo.

## Código de Erros

- `PEDIDO_INVALIDO`: Carrinho vazio ou item com preço/quantidade/peso inválido
- `MODALIDADE_INVALIDA`: Opção de entrega não existe
- `MODALIDADE_INDISPONIVEL`: Opção de entrega não atende o pedido (ex: Motoboy > 5kg)
- `CUPOM_INVALIDO`: Cupom não existe
- `CUPOM_NAO_APLICAVEL`: Cupom existe mas condição não é atendida
- `FORMA_PAGAMENTO_INVALIDA`: Forma de pagamento não existe
- `PARCELAMENTO_INVALIDO`: Número de parcelas não permitido
- `FORMA_PAGAMENTO_INDISPONIVEL`: Forma de pagamento não atende o pedido (ex: Boleto > R$ 1.000)

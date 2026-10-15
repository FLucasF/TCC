# Checkout Service - Loja Online de Roupas e Acessórios

Serviço REST que calcula o resumo de uma compra na loja online, incluindo subtotal, descontos, frete, impostos e ajustes de pagamento.

## Requisitos

- Java 21
- Maven 3.8+
- Spring Boot 4.1.1

## Construindo o Projeto

```bash
mvn clean verify
```

Este comando compila, executa os testes e empacota a aplicação.

## Executando a Aplicação

```bash
mvn spring-boot:run
```

A API ficará disponível em `http://localhost:8080/checkout/resumo`.

## Endpoint

### POST /checkout/resumo

Calcula o resumo de uma compra.

**Requisição:**
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
  "nivelClube": "BRONZE",
  "regiao": "SUDESTE"
}
```

**Resposta de Sucesso (200):**
```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 33.10,
  "prazoEntregaDias": 2,
  "imposto": 44.25,
  "ajustePagamento": -22.30,
  "totalFinal": 423.78,
  "parcelas": 1,
  "valorParcela": 423.78,
  "creditoProximaCompra": 0.00,
  "brinde": false
}
```

**Resposta de Erro (400):**
```json
{
  "erro": "PEDIDO_INVALIDO"
}
```

## Funcionalidades

### Modalidades de Entrega

- **ECONOMICA**: R$ 12,00 + R$ 2,00/kg (7 dias)
- **EXPRESSA**: R$ 25,00 + R$ 4,50/kg (2 dias)
- **RETIRADA_LOJA**: Grátis (1 dia)
- **MOTOBOY**: R$ 18,00 (0 dias, máx 5kg)

### Cupons Disponíveis

- **BEMVINDO10**: 10% de desconto nos produtos
- **MENOS50**: R$ 50,00 de desconto (mínimo R$ 300,00)
- **FRETEGRATIS**: Frete grátis
- **LEVE3PAGUE2**: A cada 3 unidades de um item, 1 sai grátis

### Níveis do Clube

- **BRONZE**: Sem benefício
- **PRATA**: 2% de crédito para próxima compra
- **OURO**: 5% de crédito + frete grátis + brinde se produtos > R$ 500,00

### Formas de Pagamento

- **PIX**: 5% de desconto no total
- **CARTAO**: Sem juros até 3x, juros de 1,99% a.m. de 4x a 12x
- **BOLETO**: Taxa de R$ 3,49 (máximo R$ 1.000,00)

### Impostos por Região

- **SUDESTE**: 12%
- **SUL**: 11%
- **CENTRO_OESTE**: 9%
- **NORTE**: 7%
- **NORDESTE**: 7%

## Arredondamento

Todo valor em dinheiro é arredondado para centavos usando o arredondamento "meio para o par" (HALF_EVEN):
- 2,995 → 3,00
- 2,985 → 2,98

## Validações de Erro

Os erros são retornados em ordem de prioridade:

1. `PEDIDO_INVALIDO` - Carrinho vazio ou item com dados inválidos
2. `NIVEL_CLUBE_INVALIDO` - Nível de clube não existe
3. `REGIAO_INVALIDA` - Região não existe
4. `MODALIDADE_INVALIDA` - Modalidade de entrega não existe
5. `MODALIDADE_INDISPONIVEL` - Modalidade não atende o pedido
6. `CUPOM_INVALIDO` - Cupom não existe
7. `CUPOM_NAO_APLICAVEL` - Cupom não atende a condição
8. `FORMA_PAGAMENTO_INVALIDA` - Forma de pagamento não existe
9. `PARCELAMENTO_INVALIDO` - Número de parcelas não permitido
10. `FORMA_PAGAMENTO_INDISPONIVEL` - Forma de pagamento não atende o pedido

## Testes

Execute os testes unitários com:

```bash
mvn test
```

Os testes cobrem todos os exemplos fornecidos e validações de erro.

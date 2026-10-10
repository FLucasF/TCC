# Serviço de Checkout

Serviço REST para cálculo do resumo de compra da loja online.

## Tecnologias

- Java 21
- Spring Boot 4.1.1
- Maven

## Como executar

### Compilar e testar

```bash
mvn verify
```

### Executar a aplicação

```bash
mvn spring-boot:run
```

O serviço estará disponível em `http://localhost:8080`

## Endpoint

### POST /checkout/resumo

Calcula o resumo da compra.

**Exemplo de requisição:**

```json
{
  "itens": [
    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
    {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
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

## Estrutura do projeto

O projeto segue as orientações do `CLAUDE.md`, separando o que varia do que é fixo:

- **domain/modalidade**: cada modalidade de entrega em sua própria classe
- **domain/cupom**: cada cupom com sua própria lógica de desconto e condições
- **domain/clube**: cada nível do clube com suas vantagens específicas
- **domain/pagamento**: cada forma de pagamento com seu ajuste próprio
- **domain/Regiao**: enum com as porcentagens de seguro por região

## Testes

O projeto inclui 16 testes automatizados:
- 5 testes validando os exemplos fornecidos
- 11 testes validando os códigos de erro

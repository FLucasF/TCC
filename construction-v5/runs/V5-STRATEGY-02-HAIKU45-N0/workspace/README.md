# Serviço de Cálculo de Resumo de Compra

Serviço REST em Java 21 com Spring Boot 4.1.1 que calcula o resumo da compra para uma loja online de roupas e acessórios.

## Como Usar

### Compilar e testar
```bash
mvn clean verify
```

### Executar a aplicação
```bash
mvn spring-boot:run
```

A aplicação inicia na porta 8080.

### Endpoint

**POST** `/checkout/resumo`

Calcula o resumo da compra com todos os descontos, taxas e ajustes.

#### Requisição (JSON)
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
  "parcelas": null,
  "nivelClube": "BRONZE",
  "regiao": "NORTE"
}
```

#### Resposta (JSON)
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

## Funcionalidades

- **Modalidades de Entrega**: ECONOMICA, EXPRESSA, RETIRADA_LOJA, MOTOBOY
- **Cupons**: BEMVINDO10 (10% desconto), MENOS50 (R$50 desconto), FRETEGRATIS (frete grátis), LEVE3PAGUE2 (1 grátis a cada 3)
- **Níveis de Clube**: BRONZE, PRATA, OURO (com benefícios especiais)
- **Formas de Pagamento**: PIX (5% desconto), CARTAO (com parcelas e juros), BOLETO (taxa de R$3,49)
- **Regiões para Seguro**: SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE

## Validações

O serviço valida em ordem:
1. Carrinho válido (itens obrigatórios)
2. Nível do clube válido
3. Região válida
4. Modalidade de entrega válida
5. Modalidade disponível para o pedido
6. Cupom válido
7. Cupom aplicável
8. Forma de pagamento válida
9. Parcelas válidas
10. Forma de pagamento disponível

Erros retornam: `{"erro": "CODIGO_ERRO"}`

## Estrutura do Projeto

```
src/main/java/com/loja/checkout/
├── CheckoutApplication.java          # Inicialização Spring Boot
├── controller/
│   └── CheckoutController.java        # Endpoint REST
├── service/
│   └── CheckoutService.java           # Lógica de cálculo
├── dto/
│   ├── CheckoutRequest.java
│   ├── CheckoutResponse.java
│   └── ItemRequest.java
└── model/
    ├── ModalidadeEntrega.java
    ├── NivelClube.java
    ├── Regiao.java
    ├── FormaPagamento.java
    └── Cupom.java
```

## Testes

18 testes unitários verificam:
- 5 exemplos completos fornecidos pelo financeiro
- Validações de erro (pedido inválido, cupom inválido, etc.)
- Casos especiais (frete grátis, brinde, parcelamento)

Executar: `mvn test`

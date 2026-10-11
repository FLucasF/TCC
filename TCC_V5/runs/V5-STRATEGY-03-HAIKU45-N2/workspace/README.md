# Serviço de Cálculo de Resumo de Compra

Serviço REST em Java 21 e Spring Boot 4.1.1 para calcular o resumo de compra em uma loja online de roupas e acessórios.

## Compilação e Execução

```bash
# Compilar e executar testes
mvn verify

# Executar a aplicação
java -jar target/checkout-resumo-1.0.0.jar
```

## API

### Endpoint: POST `/checkout/resumo`

**Requisição:**

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

**Resposta de Sucesso:**

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

**Resposta de Erro:**

```json
{
  "erro": "CUPOM_INVALIDO"
}
```

## Estrutura do Projeto

```
src/main/java/com/loja/
├── CheckoutApplication.java          # Classe principal
├── controller/
│   └── CheckoutController.java        # Controller REST
├── service/
│   └── ResumoCheckoutService.java     # Lógica de cálculo
├── model/
│   ├── ItemPedido.java
│   ├── RequisicaoResumo.java
│   ├── RespostaResumo.java
│   └── RespostaErro.java
├── entrega/
│   ├── Entrega.java                   # Interface
│   ├── EntregaEconomica.java
│   ├── EntregaExpressa.java
│   ├── EntregaRetiradaLoja.java
│   ├── EntregaMotoboy.java
│   └── FabricaEntrega.java
├── cupom/
│   ├── Cupom.java                     # Interface
│   ├── CupomBemvindo10.java
│   ├── CupomMenos50.java
│   ├── CupomFreteGratis.java
│   ├── CupomLeve3Pague2.java
│   └── FabricaCupom.java
├── pagamento/
│   ├── FormaPagamento.java            # Interface
│   ├── FormaPagamentoPix.java
│   ├── FormaPagamentoBoleto.java
│   ├── FormaPagamentoCartao.java
│   └── FabricaFormaPagamento.java
├── clube/
│   └── NivelClube.java                # Enum
├── regiao/
│   └── Regiao.java                    # Enum
└── util/
    └── Arredondamento.java            # Utilitário
```

## Testes

O projeto inclui 21 testes unitários que cobrem:

- Os 5 exemplos fornecidos no enunciado
- Validações de erros em todas as 10 situações especificadas
- Casos extremos (carrinho vazio, peso máximo de motoboy, cupom não aplicável, etc.)
- Cálculos especiais (brinde para OURO, frete grátis, juros do cartão)

Todos os testes passam com `mvn verify`.

## Funcionalidades Implementadas

✓ Cálculo de subtotal de produtos
✓ 4 tipos de entrega com preços variáveis
✓ 4 tipos de cupom com lógicas diferentes
✓ 3 níveis de clube com vantagens específicas
✓ 3 formas de pagamento (PIX, Cartão, Boleto)
✓ Cálculo de juros do cartão (tabela Price)
✓ Cálculo de seguro por região
✓ Crédito para próxima compra
✓ Brinde automático para OURO acima de R$ 500
✓ Arredondamento "meio para o par" em todas as etapas
✓ Validação completa com 10 tipos de erro

## Observações de Implementação

- Cada tipo variável (entrega, cupom, forma de pagamento, clube, região) está isolado em sua própria classe
- As fábricas (FabricaEntrega, FabricaCupom, FabricaFormaPagamento) instanciam a implementação correta
- Validações ocorrem em ordem específica, retornando o primeiro erro encontrado
- Todos os valores monetários usam BigDecimal com 2 casas decimais
- Sem uso de banco de dados, conforme solicitado

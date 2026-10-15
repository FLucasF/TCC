# Resumo da compra

Servico que calcula o resumo do pedido no fechamento da compra.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Rodar

    mvn verify          # compila e roda os testes
    mvn spring-boot:run # sobe o servico na porta 8080

Pedido de exemplo:

    curl -X POST localhost:8080/checkout/resumo -H 'Content-Type: application/json' -d '{
      "itens": [
        {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
        {"nome": "Tenis",    "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
      ],
      "modalidadeEntrega": "EXPRESSA",
      "cupom": "BEMVINDO10",
      "formaPagamento": "PIX",
      "parcelas": 1,
      "nivelClube": "OURO",
      "regiao": "SUDESTE"
    }'

Pedido recusado responde `{"erro":"CODIGO"}` com status 422.

## Onde mexer

O roteiro do calculo (produtos, cupom, frete, seguro, total, pagamento) e o
mesmo para toda compra e esta em `CalculadoraResumo`. O que muda de caso para
caso tem uma classe propria, e a escolha e pelo codigo que o site envia
(`Catalogo`), nunca por uma sequencia de condicoes.

| O que | Onde | Para acrescentar um caso novo |
|---|---|---|
| Entrega | `entrega/` | nova classe `ModalidadeEntrega` com `@Component` |
| Cupom | `cupom/` | nova classe `Cupom` com `@Component` |
| Nivel do clube | `clube/` | nova classe `NivelClube` com `@Component` |
| Forma de pagamento | `pagamento/` | nova classe `FormaPagamento` com `@Component` |
| Regiao (seguro) | `dominio/Regiao.java` | a conta e a mesma, so o percentual muda: uma linha no enum |

Nada em `CalculadoraResumo` muda quando entra uma transportadora, uma promocao,
um nivel do clube ou uma forma de pagamento nova.

Dinheiro e sempre `BigDecimal` arredondado para centavos meio para o par
(`dominio/Dinheiro`); percentual sobre valor fica em `dominio/Percentual`.

## Testes

- `ExemplosDoFinanceiroTest` — os 5 exemplos conferidos pelo financeiro e o do anexo, pelo HTTP.
- `RecusasTest` — os 10 codigos de erro e a ordem de conferencia.
- `CalculadoraResumoTest` — frete gratis, brinde, juros do cartao, modalidade nova.
- `ArredondamentoTest` — o arredondamento meio para o par.

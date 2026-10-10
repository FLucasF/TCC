# Resumo da compra

Serviço que calcula o resumo do pedido no fechamento da compra.

## Como rodar

```bash
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{
    "itens": [
      { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
      { "nome": "Tenis",    "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
    ],
    "modalidadeEntrega": "EXPRESSA",
    "cupom": "BEMVINDO10",
    "formaPagamento": "PIX",
    "parcelas": 1,
    "nivelClube": "OURO",
    "regiao": "SUDESTE"
  }'
```

Pedido recusado volta com o código do problema e status HTTP 422:
`{ "erro": "MODALIDADE_INDISPONIVEL" }`

## Onde mexer quando o negócio muda

A sequência do cálculo é a mesma para todo pedido e mora em
`servico/CalculadoraResumo.java`. O que muda de caso para caso mora cada um no
seu arquivo, e acrescentar um caso novo é acrescentar uma constante:

| Mudança no negócio | Arquivo |
|---|---|
| Nova transportadora / opção de entrega | `dominio/ModalidadeEntrega.java` |
| Nova promoção / cupom | `dominio/Cupom.java` |
| Novo nível do clube | `dominio/NivelClube.java` |
| Nova região ou percentual do seguro | `dominio/Regiao.java` |
| Nova forma de pagamento | `dominio/FormaPagamento.java` |

O arredondamento em centavos (meio para o par) fica em `dominio/Dinheiro.java`.

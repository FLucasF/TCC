# Resumo da compra

Serviço que calcula o resumo do pedido no fechamento da compra.

## Como rodar

```
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

## Como chamar

```
POST /checkout/resumo
```

```json
{
  "itens": [
    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "parcelas": 1,
  "nivelClube": "BRONZE",
  "regiao": "NORTE"
}
```

Quando dá certo, responde `200` com o resumo. Quando o pedido é recusado,
responde `422` com `{"erro": "CODIGO"}`.

## Onde fica cada regra

| O que muda de caso para caso | Onde mora |
|---|---|
| frete, prazo e limitações de cada forma de entrega | `dominio/ModalidadeEntrega` |
| condição e desconto de cada cupom | `dominio/Cupom` |
| vantagens de cada nível do clube | `dominio/NivelClube` |
| parcelamento, juros, tarifas e descontos de cada forma de pagamento | `dominio/FormaPagamento` |
| alíquota do seguro de cada região | `dominio/Regiao` |

O que é igual em todo pedido — a ordem do cálculo e a ordem em que o pedido é
recusado — fica em `aplicacao/CalculadoraResumo`. O arredondamento em centavos,
meio para o par, fica em `dominio/Dinheiro`.

Para entrar com uma entrega, um cupom ou um nível novo, basta acrescentar uma
constante no lugar correspondente.

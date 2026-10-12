# Resumo da compra (checkout)

Serviço que calcula o resumo do pedido antes de o cliente confirmar a compra.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{
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
      }'
```

Quando o pedido é recusado, a resposta é só o código do problema
(`{"erro": "CUPOM_NAO_APLICAVEL"}`), com status HTTP 422; pedido aceito
responde 200.

## Onde mexer quando o negócio muda

Cada coisa que varia de pedido para pedido mora numa classe só dela. Para
incluir uma opção nova basta criar a classe: nada mais precisa ser alterado,
porque o serviço acha as opções pelo código que o site manda.

| O que mudou | Onde mexer |
|---|---|
| Transportadora/opção de entrega nova | nova classe em `entrega/` implementando `ModalidadeEntrega` |
| Promoção nova | nova classe em `cupom/` implementando `Cupom` |
| Nível do clube novo | nova classe em `clube/` implementando `NivelClube` |
| Forma de pagamento nova | nova classe em `pagamento/` implementando `FormaPagamento` |
| Porcentagem do seguro por região | `seguro/Regiao.java` |
| Ordem das somas do resumo | `resumo/CalculadoraResumo.java` |
| Arredondamento do dinheiro | `comum/Dinheiro.java` |

Os limites de cada opção ficam junto dela: o peso máximo do motoboy está em
`entrega/Motoboy.java`, o mínimo do MENOS50 em `cupom/Menos50.java`, o teto
do boleto em `pagamento/Boleto.java`.

## Testes

- `CalculadoraResumoTest` — os cinco exemplos conferidos pelo financeiro,
  mais os casos de cada cupom, nível e forma de pagamento.
- `PedidoRecusadoTest` — cada código de recusa e a ordem de conferência
  (quando o pedido tem dois problemas, aparece o primeiro da lista).
- `CheckoutApiTest` — o formato do JSON que entra e sai.
- `DinheiroTest` — o arredondamento meio para o par.

# Resumo da compra (checkout)

Serviço que calcula o resumo do pedido antes de o cliente confirmar a compra.
Não usa banco de dados: os cupons, as opções de entrega, os níveis do clube e
as formas de pagamento estão no próprio código.

## Como rodar

```
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

## Como chamar

```
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{
        "itens": [
          { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
          { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
        ],
        "modalidadeEntrega": "EXPRESSA",
        "cupom": "BEMVINDO10",
        "formaPagamento": "PIX",
        "parcelas": 1,
        "nivelClube": "OURO",
        "regiao": "SUDESTE"
      }'
```

Quando o pedido é recusado, a resposta é só o código do problema, por exemplo
`{"erro":"CUPOM_NAO_APLICAVEL"}`.

## Onde mexer quando o negócio mudar

| Mudança | Onde |
|---|---|
| Nova transportadora | uma classe nova em `entrega`, implementando `ModalidadeEntrega` |
| Nova promoção | uma classe nova em `cupom`, implementando `Cupom` |
| Novo nível do clube | uma classe nova em `clube`, implementando `NivelClube` |
| Nova forma de pagamento | uma classe nova em `pagamento`, implementando `FormaPagamento` |
| Alíquota do seguro de uma região | o enum `dominio/Regiao` |

Basta criar a classe com `@Component`: ela entra no catálogo sozinha, sem
alterar o cálculo. O desenho está em `.claude/desenho.md`.

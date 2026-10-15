# Serviço de resumo da compra

Calcula o resumo que o site mostra antes de o cliente confirmar o pedido.

## Rodar

```bash
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

## Chamar

`POST /checkout/resumo`, com o corpo em JSON:

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
  "nivelClube": "OURO",
  "regiao": "SUDESTE"
}
```

Quando dá certo, responde `200` com o resumo. Quando o pedido é recusado,
responde `422` com `{"erro": "CODIGO"}`.

## Onde mexer

- Transportadora nova: uma constante em `ModalidadeEntrega`.
- Promoção nova: uma constante em `Cupom`.
- Nível novo do clube: uma constante em `NivelClube`.
- Forma de pagamento nova: uma constante em `FormaPagamento`.

O desenho está em `.claude/desenho.md`.

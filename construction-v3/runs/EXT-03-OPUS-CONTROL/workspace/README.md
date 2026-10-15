# Checkout da loja — resumo da compra

Serviço que calcula o resumo mostrado antes de o cliente confirmar o pedido.
Java 21, Spring Boot 4.1.1, sem banco de dados.

```bash
mvn verify          # compila e roda os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

## A chamada

`POST /checkout/resumo`

```json
{
  "itens": [
    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
    { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": null,
  "formaPagamento": "PIX",
  "parcelas": 1,
  "nivelClube": "OURO",
  "regiao": "SUDESTE"
}
```

Resposta (200):

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 0.00,
  "frete": 0.00,
  "prazoEntregaDias": 2,
  "imposto": 49.16,
  "ajustePagamento": -22.94,
  "totalFinal": 435.92,
  "parcelas": 1,
  "valorParcela": 435.92,
  "creditoProximaCompra": 20.48,
  "brinde": false
}
```

Erro (400): `{ "erro": "CODIGO" }`, sempre o primeiro da ordem combinada
(`PEDIDO_INVALIDO`, `NIVEL_CLUBE_INVALIDO`, `REGIAO_INVALIDA`, `MODALIDADE_INVALIDA`,
`MODALIDADE_INDISPONIVEL`, `CUPOM_INVALIDO`, `CUPOM_NAO_APLICAVEL`,
`FORMA_PAGAMENTO_INVALIDA`, `PARCELAMENTO_INVALIDO`, `FORMA_PAGAMENTO_INDISPONIVEL`).

## Ordem do cálculo

1. `subtotalProdutos` = soma de preço × quantidade de cada item
2. `descontoCupom`
3. `frete` (zerado quando o nível do clube não paga frete)
4. `imposto` = alíquota da região sobre (produtos − cupom)
5. total do pedido = produtos − cupom + frete + imposto
6. `ajustePagamento` sobre o total do pedido → `totalFinal`

Todo valor em dinheiro é arredondado em centavos a cada etapa, "meio para o par"
(`Dinheiro.centavos`).

## Onde mexer quando entra coisa nova

Cada regra de negócio que muda com frequência é uma classe anotada com `@Component`,
recolhida automaticamente por um catálogo. Nada mais precisa ser alterado.

| O que entrou | Implemente | Exemplo |
|---|---|---|
| Transportadora nova | `entrega.ModalidadeEntrega` | `EntregaExpressa` |
| Promoção nova | `cupom.Cupom` | `CupomMenos50` |
| Nível novo do clube | `clube.NivelClube` | `NivelOuro` |
| Forma de pagamento nova | `pagamento.FormaPagamento` | `PagamentoPix` |

As regiões ficam no enum `dominio.Regiao` — só a alíquota muda de uma para a outra.

## Decisões tomadas

- Os exemplos 1 a 4 do financeiro foram conferidos sem a parte do imposto, mas a região
  é obrigatória na chamada. Esses exemplos são testados nos valores que declaram
  (produtos, cupom, frete, prazo) e o ajuste de pagamento de cada um está coberto em
  `PagamentoTest`, aplicado sobre o total do pedido que eles produzem.
- `LEVE3PAGUE2` só vale se o carrinho tiver pelo menos 3 unidades de algum item; caso
  contrário a resposta é `CUPOM_NAO_APLICAVEL`.
- `cupom` vazio ou ausente é tratado como compra sem cupom.
- O limite de R$ 1.000,00 do boleto olha produtos − cupom + frete, sem o imposto,
  como combinado; o ajuste de R$ 3,49 incide sobre o total do pedido com imposto.

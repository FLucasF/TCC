# Checkout - resumo da compra

Serviço que calcula o resumo mostrado ao cliente antes de confirmar o pedido.
Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Rodar

```bash
mvn verify        # compila e roda os testes
mvn spring-boot:run
```

## Chamada

`POST /checkout/resumo`

```json
{
  "itens": [
    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
  ],
  "modalidadeEntrega": "EXPRESSA",
  "cupom": "BEMVINDO10",
  "formaPagamento": "PIX",
  "parcelas": 1
}
```

Resposta 200:

```json
{
  "subtotalProdutos": 409.70,
  "descontoCupom": 40.97,
  "frete": 33.10,
  "prazoEntregaDias": 2,
  "ajustePagamento": -20.09,
  "totalFinal": 381.74,
  "parcelas": 1,
  "valorParcela": 381.74
}
```

Erro 400: `{ "erro": "CODIGO" }`.

## Ordem do cálculo

1. `subtotalProdutos` = soma de preço × quantidade
2. `descontoCupom`
3. `frete` (pelo peso do pedido, que é a soma de peso × quantidade sem arredondar)
4. total do pedido = produtos − cupom + frete
5. `ajustePagamento` sobre o total do pedido; `totalFinal` é o resultado

Todo valor em dinheiro fecha em centavos a cada etapa com arredondamento
"meio para o par" (`HALF_EVEN`).

## Como cadastrar coisas novas

O catálogo é montado sozinho pelo Spring — não é preciso mexer no cálculo.

- **Transportadora nova:** crie uma classe `@Component` em `entrega/`
  implementando `ModalidadeEntrega` (código, prazo, frete e, se houver limite,
  `atende`). Quem cobra fixo + por quilo pode estender `FretePorPeso`.
- **Cupom novo:** classe `@Component` em `cupom/` implementando `Cupom`
  (código, desconto e, se houver condição, `aplicavel`).
- **Forma de pagamento nova:** classe `@Component` em `pagamento/`
  implementando `FormaPagamento`.

## Regras de hoje

Entrega: `ECONOMICA` (R$ 12,00 + R$ 2,00/kg, 7 dias), `EXPRESSA`
(R$ 25,00 + R$ 4,50/kg, 2 dias), `RETIRADA_LOJA` (grátis, 1 dia),
`MOTOBOY` (R$ 18,00, mesmo dia, até 5 kg).

Cupons (um por pedido, código em maiúsculas): `BEMVINDO10` (10% dos produtos),
`MENOS50` (R$ 50,00 a partir de R$ 300,00 em produtos), `FRETEGRATIS`
(desconto igual ao frete), `LEVE3PAGUE2` (a cada 3 unidades do mesmo item, 1 grátis).

Pagamento: `PIX` (5% de desconto, à vista), `BOLETO` (tarifa de R$ 3,49, à vista,
só até R$ 1.000,00 de pedido), `CARTAO` (até 3x sem juros, de 4x a 12x pela
tabela Price a 1,99% ao mês).

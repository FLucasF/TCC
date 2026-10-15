# Serviço de resumo do checkout

Calcula o resumo da compra (produtos, cupom, frete, total e ajuste da forma de pagamento)
que o site mostra antes do cliente confirmar o pedido.

Java 21 · Spring Boot 4.1.1 · sem banco de dados.

## Como rodar

```bash
mvn verify        # compila e roda todos os testes
mvn spring-boot:run   # sobe o serviço em http://localhost:8080
```

## A chamada

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

Resposta (200):

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

Erro (400): `{ "erro": "CUPOM_NAO_APLICAVEL" }`.

## Como o cálculo é feito

`CalculadoraResumo` segue a ordem combinada: soma dos produtos → desconto do cupom →
frete → total do pedido (produtos − cupom + frete) → ajuste da forma de pagamento.
Todo valor em dinheiro passa por `Dinheiro.valor(...)`, que arredonda para centavos
com "meio para o par".

Os erros são verificados na ordem da tabela combinada com o site: carrinho, entrega
(existe / atende), cupom (existe / aplicável), pagamento (existe / parcelas / atende).

## Onde mexer quando o negócio mudar

Cada regra do negócio é uma classe pequena e independente. Basta criar a classe nova
com `@Component` — o catálogo correspondente a encontra sozinho, sem alterar mais nada.

| Mudança | O que fazer |
|---|---|
| Transportadora nova | Classe em `entrega` implementando `ModalidadeEntrega` (ou estendendo `FretePorPeso`, para cobrança fixa + por kg). Use `atende(...)` para limites de peso e afins. |
| Promoção nova | Classe em `cupom` implementando `Cupom`. Use `aplicavel(...)` para a condição e `calcularDesconto(...)` para o valor. |
| Forma de pagamento nova | Classe em `pagamento` implementando `FormaPagamento`. |

## O que está cadastrado hoje

- **Entrega**: `ECONOMICA` (R$ 12,00 + R$ 2,00/kg, 7 dias), `EXPRESSA` (R$ 25,00 + R$ 4,50/kg, 2 dias),
  `RETIRADA_LOJA` (grátis, 1 dia), `MOTOBOY` (R$ 18,00, mesmo dia, até 5 kg).
- **Cupons**: `BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2` (um por pedido).
- **Pagamento**: `PIX` (5% de desconto, à vista), `BOLETO` (tarifa de R$ 3,49, à vista,
  só até R$ 1.000,00 de pedido), `CARTAO` (até 3x sem juros, de 4x a 12x com 1,99% ao mês
  pela tabela Price).

# Serviço de resumo da compra (checkout)

Calcula o resumo mostrado ao cliente antes de confirmar o pedido: produtos, desconto do
cupom, frete, prazo, ajuste da forma de pagamento e valor final.

Java 21 + Spring Boot 4.1.1, sem banco de dados.

## Como rodar

```bash
mvn verify          # compila e roda todos os testes
mvn spring-boot:run # sobe o serviço em http://localhost:8080
```

Exemplo de chamada:

```bash
curl -X POST http://localhost:8080/checkout/resumo \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}],
       "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}'
```

## Ordem do cálculo

1. `subtotalProdutos` = soma de (preço × quantidade) de cada item
2. `descontoCupom` conforme o cupom informado
3. `frete` conforme a modalidade de entrega (peso do pedido = soma de peso × quantidade, sem arredondar)
4. total do pedido = produtos − cupom + frete
5. `ajustePagamento` da forma de pagamento sobre o total do pedido; `totalFinal` = total do pedido + ajuste

Todo valor em dinheiro é arredondado para centavos em cada etapa com "meio para o par"
(`RoundingMode.HALF_EVEN`), centralizado em `dominio/Dinheiro`. Os cálculos usam
`BigDecimal` do começo ao fim, nunca `double`.

## Onde mexer quando o negócio muda

O serviço não conhece as regras de cada opção: ele consulta catálogos que se montam
sozinhos a partir dos beans do Spring.

| Mudança | O que fazer |
|---|---|
| Nova transportadora / opção de entrega | criar uma classe em `entrega/` implementando `ModalidadeEntrega` (ou estendendo `FreteBasePorPeso`) e anotar com `@Component` |
| Nova promoção | criar uma classe em `cupom/` implementando `Cupom` e anotar com `@Component` |
| Nova forma de pagamento | criar uma classe em `pagamento/` implementando `FormaPagamento` e anotar com `@Component` |

Nenhum desses casos exige alterar `CheckoutService`, o controller ou os catálogos.
Cada opção carrega seu próprio preço, prazo, limitação (`atende(...)`) e forma de cálculo.

## Regras em vigor hoje

**Entrega** — `ECONOMICA` R$ 12,00 + R$ 2,00/kg (7 dias) · `EXPRESSA` R$ 25,00 + R$ 4,50/kg
(2 dias) · `RETIRADA_LOJA` grátis (1 dia) · `MOTOBOY` R$ 18,00 (mesmo dia, até 5 kg).

**Cupons** (um por pedido, código sempre em maiúsculas) — `BEMVINDO10` 10% dos produtos ·
`MENOS50` R$ 50,00 a partir de R$ 300,00 em produtos · `FRETEGRATIS` desconto igual ao frete ·
`LEVE3PAGUE2` a cada 3 unidades do mesmo item, uma sai de graça.

**Pagamento** — `PIX` 5% de desconto, à vista · `BOLETO` tarifa de R$ 3,49, à vista, só até
R$ 1.000,00 de total do pedido · `CARTAO` 1x a 3x sem juros e 4x a 12x com 1,99% ao mês pela
tabela Price (`parcela = total × taxa ÷ (1 − (1 + taxa)^−parcelas)`, arredondada; valor final =
parcela × parcelas).

## Erros

Sempre `400` com `{ "erro": "CODIGO" }`, verificados nesta ordem: `PEDIDO_INVALIDO`,
`MODALIDADE_INVALIDA`, `MODALIDADE_INDISPONIVEL`, `CUPOM_INVALIDO`, `CUPOM_NAO_APLICAVEL`,
`FORMA_PAGAMENTO_INVALIDA`, `PARCELAMENTO_INVALIDO`, `FORMA_PAGAMENTO_INDISPONIVEL`.

## Decisões tomadas onde o combinado não dizia

- **`LEVE3PAGUE2` sem nenhum item com 3 unidades** devolve `CUPOM_NAO_APLICAVEL`: o pedido não
  cumpre a condição da promoção. (A alternativa seria aceitar o cupom com desconto R$ 0,00.)
- **`FRETEGRATIS` com `RETIRADA_LOJA`** é aceito, com desconto R$ 0,00: esse cupom não tem
  condição de valor mínimo, e o frete já era zero.
- **`cupom` vazio ou só com espaços** é tratado como "cliente não usou cupom".
- **Corpo da requisição ausente ou mal formado** (ex.: `quantidade` que não é inteiro) devolve
  `PEDIDO_INVALIDO`.
- **Limites são inclusivos**: motoboy aceita exatamente 5 kg, `MENOS50` vale com exatamente
  R$ 300,00 em produtos, boleto aceita total de exatamente R$ 1.000,00.
- O limite do boleto usa o total do pedido **depois** do cupom e do frete, como descrito pelo
  financeiro.

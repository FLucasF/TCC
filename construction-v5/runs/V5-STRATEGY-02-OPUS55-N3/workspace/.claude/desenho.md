# Desenho — resumo do checkout

## O que muda de caso para caso (o enunciado diz que varia)
- **Modalidade de entrega**: cada uma tem seu jeito de cobrar, prazo e limitação (e entram novas toda semana).
- **Cupom**: cada um tem sua regra de desconto e, às vezes, sua condição (marketing inventa novos).
- **Nível do clube**: cada um tem seu conjunto de vantagens (frete, crédito, brinde; novos níveis previstos).
- **Forma de pagamento**: cada uma tem seu ajuste (desconto Pix, tarifa boleto, juros cartão),
  suas parcelas permitidas e sua restrição (boleto até R$ 1.000).

## O que é igual
- Ordem do cálculo e ordem das validações (fluxo único no `CheckoutService`).
- Subtotal e peso do carrinho; arredondamento HALF_EVEN para centavos em cada etapa.
- Seguro: a conta é a mesma para todas as regiões, só a porcentagem muda → **enum `Regiao`
  com a porcentagem como dado**, sem estratégia por região.

## Estrutura
- Uma interface por eixo que varia (`ModalidadeEntrega`, `Cupom`, `NivelClube`, `FormaPagamento`),
  uma classe por caso (bean Spring). Todas expõem `codigo()`.
- `Catalogo<T>` indexa os beans pelo código; escolher o caso é uma busca no mapa, não uma
  cadeia de `if`/`switch`. Caso novo = classe nova, sem tocar no fluxo.
- Assinaturas pensadas no caso mais exigente:
  - entrega: `atende(Carrinho)` (motoboy precisa do peso), `frete(Carrinho)`, `prazoDias()`.
  - cupom: `aplicavel(Carrinho)` (MENOS50) e `desconto(Carrinho, frete)` (LEVE3PAGUE2 precisa dos
    itens, FRETEGRATIS precisa do frete já com a vantagem do clube).
  - clube: `frete(freteDaModalidade)`, `credito(Carrinho)`, `brinde(Carrinho)`.
  - pagamento: `parcelasPermitidas(n)`, `atende(totalPedido)`, `calcular(totalPedido, n)` → total final e parcela.
- Ausência de cupom = objeto `Cupom.NENHUM` (desconto zero), fora do catálogo.
- Erros: `CheckoutException(codigo)` → HTTP 422 `{ "erro": "CODIGO" }`. Corpo ilegível → `PEDIDO_INVALIDO`.
- Request com campos de texto para os códigos (valor desconhecido vira o erro de negócio, não erro do Jackson).

## Decisões após revisão
- LEVE3PAGUE2 conta as unidades por linha do carrinho ("cada item do carrinho").
- Corpo ilegível ou número quebrado em campo inteiro → 422 `PEDIDO_INVALIDO`, como os demais erros.

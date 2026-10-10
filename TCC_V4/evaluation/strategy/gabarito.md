---
padrao: strategy
enunciado: experiment/prompt/prompt.md
enunciado_hash: 8c70bb30493dbfbbde8b2d4be857669993335b0e4d6114c8c0b777ee00c69984
lotes: V4-STRATEGY, V4-EXPLOR
---

# Gabarito: enunciado do Strategy (V4)

> **CONGELADO em 09/10/2026**, junto com a [régua](../regua.md) (versão 4, enxuta). Vale só para o
> enunciado e os lotes do cabeçalho, que é lido por script: o `verify.mjs` confere que
> o `enunciado_hash` é o `prompt_hash` gravado no `meta.json` de cada execução dos
> lotes. A versão anterior (enunciado do V3, com o imposto, lote EXT) está no
> histórico do git.

Com a régua enxuta, o gabarito só precisa dizer **os pontos e os casos** de cada um.
Os nomes são os códigos que o site manda, e são a tabela que o Semgrep usa
(`evaluation/tools/semgrep/pontos.mjs`).

| ponto | tipo | casos | quem lê |
|---|---|---|---|
| **P1** entrega | positivo | `ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY` | o Semgrep (secundário) |
| **P2** cupom | positivo | `BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2` | o Semgrep (secundário) |
| **P3** pagamento | positivo | `PIX`, `CARTAO`, `BOLETO` | o Semgrep (secundário) |
| **P4** clube | positivo (**o foco**) | `BRONZE`, `PRATA`, `OURO` | o Semgrep e o Lucas |
| **P5** seguro por região | **controle negativo** | `SUDESTE`, `SUL`, `CENTRO_OESTE`, `NORTE`, `NORDESTE` | o Semgrep e o Lucas |

## Por que o P4 é o foco

O `OURO` mexe em três partes do resumo (o crédito, o frete e o brinde), e o frete é
calculado em outro lugar, na entrega. Quem escreve o clube olhando só o `BRONZE` e a
`PRATA` tende a resolver o Ouro com um `if (nivel == OURO)` no cálculo do frete: o
desenho espalhado que o harness quer evitar. É o ponto em que o padrão mais faz
diferença, e por isso é o da hipótese principal.

## Por que o P5 é o controle negativo

O enunciado diz: *"É só a porcentagem que muda, a conta é a mesma em todas."* A
resposta proporcional é a variação como dado (um `enum` só com as porcentagens, um mapa
de números) ou um `switch` que só devolve a porcentagem. Uma classe, um objeto ou uma
constante de enum com corpo por região é o **exagero** que este ponto mede.

## O que não é ponto de variação

Para não virar ponto por engano na leitura:

- **A ordem de precedência dos erros** (10 códigos) é uma sequência fixa, e não uma
  variação por caso.
- **A moeda.** O enunciado diz "só reais".
- **A troca de produto.** O enunciado diz que "não entra no cálculo".

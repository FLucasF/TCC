# `analysis/`: as tabelas que saem de um lote

Os arquivos que os scripts **escrevem** depois que as execuções rodaram, um conjunto
por lote (o prefixo, como `V5-STRATEGY`). Nada aqui é escrito à mão, e quase tudo se
refaz a partir de `runs/`. A regra do git é por isso: o que se recalcula fica fora; o
que é registro de um instrumento, antes de ser comparado com outra coisa, entra.

## O que aparece aqui, na ordem do `COMO-RODAR-V5.md`

| arquivo | quem escreve | o que tem | no git? |
|---|---|---|---|
| `resultados.csv` | `infra/scripts/aggregate.mjs` | custo e processo: uma linha por execução, tirada do `meta.json` (tokens, tempo de API, turnos, ferramentas, término, build) | **não**: refaz-se dos `meta.json` |
| `acceptance-<lote>.csv` | `infra/scripts/acceptance.sh` | a correção: uma linha por execução, com o status da suíte (passou tudo, errou casos, sem pom, não compilou, não subiu), as contas e as recusas | **não**: refaz-se dos `runs/<id>/acceptance.txt`, que estão no git |
| `semgrep-<lote>.csv` | `evaluation/tools/semgrep/detect.sh` | o desenho: uma linha por **pacote cego** (o código, não o `run_id`), com as respostas da régua do P1 ao P5, a evidência e os avisos; no cabeçalho, o hash das regras, do classificador e da imagem | **sim**, e **antes** da leitura humana: é o registro do instrumento, que não se refaz a partir de nada que esteja no git |
| `conferencia-<lote>.md` | `evaluation/tools/compare.mjs` | a leitura do Lucas × o Semgrep, pergunta por pergunta, e qual pergunta vale para o lote (90%: 40 de 44 no V5) | sim |
| `notas-<lote>.csv` | `evaluation/tools/nota.mjs` | a nota de 0 a 100 de cada execução (pesos A, B e C), com as marcas (trava, "sem nota") | **sim** |
| `hipoteses-<lote>.md` e `.json` | `evaluation/tools/hipoteses.mjs` | as tabelas de pares e o veredito de cada hipótese do `OBJETIVO` (§4) | sim |

As métricas de qualidade (CK e SonarQube) não ficam aqui: vão para
`evaluation/metrics/<lote>/metricas.csv`.

## O que há hoje no `TCC_V3`

Só saídas de **testes da bancada**, fora de qualquer análise: `acceptance-TESTE-*.csv`,
`semgrep-TESTE-*.csv` (o mapa tem dois, o `semgrep-TESTE-MAPA-01.csv` e um `-b`, os dois
commitados em 09/10 no `d4af173`; o motivo do segundo não foi registrado) e
`testes-2026-09-30.md`, as notas das primeiras rodadas `TESTE-*` (State e Strategy, n = 1
por célula). Os resultados do V4 e do ensaio da análise estão no
`TCC_V4/analysis/`.

## Vai para o `TCC_V5`?

Vai **este README** (congelado). A pasta nasce vazia e se enche ao longo dos passos 2 a
4 do `COMO-RODAR-V5.md`.

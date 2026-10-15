# `experiment/`: o que define o experimento

O que o agente recebe e como o lote é montado: o enunciado, os harnesses de cada nível,
a skill de terceiros, e o desenho e a ordem do lote. Mudar qualquer arquivo daqui muda
**o que** está sendo medido; por isso tudo o que o V5 usa está congelado
(`CONGELADO-V5.sha256`), e os harnesses e o enunciado são identificados por hash no
`meta.json` de cada execução.

## O que tem aqui

| item | o que é | vai para o `TCC_V5`? |
|---|---|---|
| `prompt/prompt.md` | **o enunciado**, o mesmo nos quatro níveis (`8c70bb30…`): o dono de uma loja que estuda o básico de programação descreve as regras do checkout e, num anexo, o contrato da API em linguagem simples. Sem palavra de arquitetura. O `README.md` da pasta explica como ele foi escrito | sim (congelado) |
| `harnesses/` | **os níveis**: `N1/` (um `CLAUDE.md`), `N2/` (o N1 mais a skill `gof-patterns` em `.claude/skills/`) e `N3/` (o N2 mais o subagente `revisor` em `.claude/agents/`). O N0 não tem pasta: é o workspace vazio. O `run-one.sh` copia a pasta do nível para o workspace e grava o hash da árvore. O `README.md` da pasta conta a escada e a validação da bancada | sim (congelados) |
| `third-party/gof-patterns/` | de onde veio a skill do N2 e do N3 (`ORIGEM.md`): pública, intacta, escrita sem conhecer as tarefas | sim (congelado) |
| `desenho-v5.json` | **o desenho do V5**, lido pelo `run-levels.sh`, pelo `draw-order.mjs`, pelo `verify.mjs` e pelo `hipoteses.mjs` (campos abaixo) | sim (congelado) |
| `ordem-v5.csv` | **a ordem dos 55 quartetos**, sorteada pelo `infra/scripts/draw-order.mjs` com a semente 20261020: `posicao`, `rodada`, `prefixo`, `apelido`, `modelo` e o `comando` pronto. Cada rodada tem uma réplica de cada modelo, e as rodadas vão em sequência | sim (congelada) |
| `desenho-v4.json`, `desenho-v4-exploratorio.json`, `ordem-v4.csv`, `ordem-v4-exploratorio.csv` | o desenho e as ordens do V4 (descartado antes da análise; ver `DECISOES.md`, 10/10) | não: ficam no `TCC_V4`, congelados lá |

## Os campos do `desenho-v5.json`

| campo | o que é | quem usa |
|---|---|---|
| `prefixo` | o nome do lote, `V5-STRATEGY`; o `run_id` é `<prefixo>-<réplica com 2 dígitos>-<APELIDO>-<nível>` | todos |
| `replicas` | 5 | `draw-order.mjs`, `verify.mjs`, `hipoteses.mjs` |
| `niveis` | N0 a N3: o braço (`CONTROL` ou `HARNESS`) e o hash da árvore do harness (16 primeiros caracteres) | `verify.mjs` confere cada `meta.json` |
| `modelos` | apelido → ID completo (`OPUS55` → `claude-opus-5-5`); o apelido entra no `run_id`, e o ID é o que o Claude Code recebe | `run-levels.sh`, `verify.mjs` |
| `simultaneos_minutos` | os 4 níveis de um quarteto têm de começar dentro de 5 minutos | `verify.mjs` |
| `effort` | `medium` para todos | `run-levels.sh`, `verify.mjs` |
| `image_id`, `claude_code_version` | a imagem `v5` e o Claude Code 2.1.288 que toda execução tem de ter usado | `verify.mjs` |
| `suite`, `gabarito`, `resultados` | onde estão a suíte, o gabarito e o CSV de custo | `verify.mjs` |

## O que nunca muda no meio de um lote

O enunciado, a pasta de um nível, o desenho e a ordem. Uma mudança depois do
congelamento é emenda datada no `OBJETIVO` e no `DECISOES.md`, nunca uma edição
silenciosa: o hash de cada execução denunciaria a troca.

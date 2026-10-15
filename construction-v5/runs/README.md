# `runs/`: uma pasta por execução

Cada execução de um agente vira uma pasta aqui, criada pelo `infra/scripts/run-one.sh`
e **nunca reaproveitada**: se a pasta existe, o script recusa rodar de novo com o mesmo
nome. É a matéria-prima de tudo: a análise lê daqui, e quase tudo em `analysis/` se
refaz a partir destas pastas.

## O nome de uma execução

| época | formato | exemplo |
|---|---|---|
| V4 e V5 (a escada de níveis) | `<lote>-<réplica com 2 dígitos>-<APELIDO>-<nível>` | `V5-STRATEGY-03-OPUS55-N2` |
| testes da bancada | `TESTE-<o que testa>-<NN>-<APELIDO>-<nível>` ou `TESTE-<o que testa>-<NN>-<APELIDO>` | `TESTE-V5-01-OPUS55-N3`, `TESTE-IDS-02-HAIKU55` |
| V3 (dois braços, antes da escada) | `<lote>-<NN>-<MODELO>-<CONTROL ou HARNESS>` | `EXT-02-SONNET-HARNESS` |

O apelido do modelo é o do desenho (`experiment/desenho-v5.json`); o nível diz o
harness (N0 é o braço `CONTROL`, com o workspace vazio).

## O que tem dentro de cada pasta

| arquivo | quem escreve | o que é |
|---|---|---|
| `workspace/` | o agente | **o código que ele deixou**: o projeto Spring Boot (às vezes numa subpasta, como `checkout/`) e, nos níveis N1 a N3, a pasta do harness que o `run-one.sh` copiou antes (`CLAUDE.md`, `.claude/`) |
| `claude-output.jsonl` | o Claude Code | a transcrição inteira, um evento JSON por linha (o início da sessão, cada mensagem, cada ferramenta, o resultado) |
| `stderr.txt` | o container | a saída de erro, com as três linhas `[pre]` que provam o isolamento (a versão do Claude Code, o `~/.claude` vazio, nenhum `CLAUDE.md` fora do workspace) |
| `build.txt` | o `run-one.sh` | o `mvn verify` rodado num **segundo** container, sem o token, no projeto do `pom.xml` mais raso |
| `meta.json` | o `extract-meta.mjs` | o resumo da execução (campos abaixo); a fonte de tudo o que o `aggregate.mjs` e o `verify.mjs` leem |
| `acceptance.txt` | o `acceptance.sh` | a suíte de aceitação: os hashes da suíte e do enunciado, o status e a saída completa dos 21 casos. Uma execução nunca é medida duas vezes |
| `git-do-agente/` | à mão, antes do commit | só quando o agente rodou `git init`: o `.git` dele sai do workspace, senão o git do TCC guardaria o workspace como ponteiro, sem o código (aconteceu no V4) |

### Os campos do `meta.json`

| campo | o que tem |
|---|---|
| `run_id`, `condition`, `replicate` | quem é a execução |
| `model_requested`, `model_init`, `models_observed` | o modelo pedido, o que o Claude Code iniciou e os que de fato responderam (o `verify.mjs` confere que são o mesmo) |
| `environment` | a imagem e o ID dela, a versão do Claude Code, o hash do enunciado e do harness, as linhas `[pre]` |
| `parameters` | o effort e o modo de permissão |
| `timing` | início, fim, duração de relógio e **duração de API** (a medida limpa, porque o relógio sofre com 4 containers juntos) |
| `outcome` | o término (`completed`, `error`...), turnos, ferramentas usadas por nome, subagentes, a mensagem final e se o build passou |
| `tokens` | entrada, saída, cache, raciocínio e o custo estimado |
| `foundation` | o projeto: onde está o `pom.xml`, as versões de Java e Spring Boot, os starters e se as versões pedidas foram obedecidas |
| `isolation_init` | o que o Claude Code achou ao iniciar: ferramentas, skills (a `gof-patterns` só no N2 e no N3), subagentes (o `revisor` só no N3), MCPs, plugins |
| `valid`, `invalid_reason` | sempre vazios: nenhum script decide se uma execução vale |

## `logs/`

Um `.log` por execução lançada pelo `run-levels.sh` (ou pelo antigo `rodada.sh`): a
saída do terminal do `run-one.sh` daquela execução. `logs-teste-v5/` guarda os logs dos
testes de 10/10 na imagem `v5`, lançados direto pelo `run-one.sh`.

## Os lotes que existem no `TCC_V3`

Nenhum entra na análise do V5; todos serviram para construir e validar a bancada.

| prefixo | o que foi |
|---|---|
| `SMOKE-*` | as primeiras execuções, para ver a bancada de pé |
| `BATCH-01` a `03` | **o piloto** (3 modelos × 2 braços × 3 réplicas, o enunciado de três pontos em `history/pilot/`): mostrou o efeito de teto |
| `EXT-01` a `03` | **o experimento do V3** (o enunciado de `history/prompt-v3/`): validou a bancada e serviu de ensaio da régua e das métricas |
| `TESTE-STRATEGY-*`, `TESTE-P4-*`, `TESTE-STATE-*` | testes de enunciado (Strategy, o ponto P4, o padrão State) |
| `TESTE-BANCADA-*` | a validação de que cada nível chega ao agente (o enunciado barato de `history/bench-test/`); a `-03` é a da imagem `v5` |
| `TESTE-NIVEIS-01` | os quatro níveis rodando juntos pela primeira vez |
| `TESTE-MAPA-01` | o mapa no N0, uma execução por modelo, antes do V4 |
| `TESTE-DIFICIL-01` | o enunciado mais difícil nos três modelos mais fortes (não tirou do teto) |
| `TESTE-ENSAIO-01` | o ensaio do quarteto com o enunciado barato |
| `TESTE-IDS-01`, `-02` | cada modelo responde com o ID pedido (a `-02` na imagem `v5`, os 11 do V5) |
| `TESTE-V5-01` | o quarteto de ensaio do Opus 5.5 na imagem `v5`, com o enunciado real |

## Regras

- **Nada é refeito por cima.** Um quarteto cortado pela cota vai inteiro para
  `runs-descartadas/`, na raiz do TCC (registrado no `DECISOES.md`), e roda de novo.
- **Antes de cada commit**, procurar `.git` dentro dos workspaces
  (`find runs -maxdepth 3 -name .git`) e conferir que nenhum `.env` entra.

## Vai para o `TCC_V5`?

Vai **este README** (congelado). No `TCC_V5`, `runs/` nasce vazia e recebe só as 220
execuções `V5-STRATEGY-*`.

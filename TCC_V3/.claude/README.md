# `.claude/`: a configuração do Claude Code **desta bancada**

Esta pasta configura o Claude Code quando ele é usado **para construir e operar a
bancada** (as sessões em que o Lucas e o Claude escrevem os scripts, rodam os lotes e
fazem a análise). Ela **não** tem nada a ver com os agentes do experimento.

| arquivo | o que faz |
|---|---|
| `settings.json` | as configurações de projeto do Claude Code. Hoje só tem `claudeMdExcludes`: faz o Claude Code **ignorar o `CLAUDE.md` da raiz `J:\TCC`** quando trabalha aqui dentro, para as instruções de lá não entrarem nas sessões desta bancada |

## A diferença que importa: esta pasta × o `.claude` dos harnesses

| | onde | quem lê | para quê |
|---|---|---|---|
| **esta pasta** | `TCC_V3/.claude/` | o Claude Code das sessões de trabalho, na máquina do Lucas | configurar quem constrói a bancada |
| **o `.claude` de um nível** | `experiment/harnesses/N2/.claude/`, `N3/.claude/` | o agente do experimento, **dentro do container**, quando o nível é copiado para o workspace | a skill (`gof-patterns`) e o subagente revisor (`revisor`), que **são** o tratamento do N2 e do N3 |

Os agentes do experimento nunca veem esta pasta: cada execução roda num container
descartável, com o workspace vazio (N0) ou só com a pasta do nível (N1 a N3), e as
linhas `[pre]` do `stderr.txt` de cada execução provam que o `~/.claude` do container
está vazio e que não há `CLAUDE.md` fora do workspace (ver `infra/scripts/README.md`).

## Vai para o `TCC_V5`?

**Não.** É configuração da bancada de trabalho, não parte do experimento congelado.

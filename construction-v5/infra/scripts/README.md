# Scripts da bancada (`infra/scripts/`)

Os scripts que **rodam** o experimento e **conferem** o que ele produziu. Os que
**avaliam** o código dos agentes (Semgrep, nota, hipóteses, métricas, leitura) ficam
em `evaluation/tools/`, com o README deles. Cada script tem, no topo, um bloco
**COMO LER** com as partes dele na ordem em que rodam; este README é o mapa.

Todos rodam da raiz do TCC, no Git Bash (os `.sh`) ou com o Node (os `.mjs`), sem
dependência além do Docker e do Node 24.

## O caminho de uma execução

```
run-levels.sh  ──4x──>  run-one.sh  ──>  extract-meta.mjs  ──>  runs/<id>/meta.json
(um quarteto)           (um container)    (resume a transcrição)
                                                              │
acceptance.sh  ──────────────────────────────────────────────>│ runs/<id>/acceptance.txt
aggregate.mjs  ──> analysis/resultados.csv (custo e processo) │
verify.mjs     ──> confere que tudo isso bate com o desenho <─┘
```

## Os scripts

| script | o que faz | como faz |
|---|---|---|
| `run-one.sh` | roda **uma** execução: um agente, um modelo, um nível | (1) confere tudo antes de gastar cota: o ID completo do modelo (alias é recusado), o `.env` com o token da assinatura e sem variável que troque cobrança ou provedor, a imagem, a pasta do harness; (2) cria `runs/<id>/workspace`, vazio no N0 ou com a pasta do nível copiada, e calcula o hash dela; (3) sobe um container descartável da imagem `experimento-harness:v5` e roda `claude -p` com o enunciado pela entrada padrão, sem pedir permissão e sem restrição de ferramenta, gravando a transcrição (`claude-output.jsonl`); três linhas `[pre]` provam o isolamento (versão do Claude Code, `~/.claude` vazio, nenhum `CLAUDE.md` fora do workspace); (4) num **outro** container, sem o token, roda `mvn verify` no projeto do `pom.xml` mais raso; (5) chama o `extract-meta.mjs` |
| `run-levels.sh` | roda um **quarteto**: os níveis N0 a N3 do mesmo modelo, **ao mesmo tempo** | lê o apelido do modelo (`OPUS55`) no desenho (`experiment/desenho-v*.json`, ou o arquivo em `DESENHO=`) e tira de lá o ID completo e o effort; recusa `PROMPT_FILE` e `IMAGE` trocados no lote; confere que nenhuma das 4 execuções existe; lança 4 `run-one.sh` em paralelo e espera todas. Simultâneo de propósito: horário, fila e carga do servidor ficam iguais nos 4 níveis, e a análise compara níveis vizinhos do mesmo quarteto |
| `extract-meta.mjs` | escreve o `runs/<id>/meta.json`, o resumo de uma execução | lê a transcrição: o evento `init` (modelo, ferramentas, skills, subagentes que o Claude Code achou) e os `result` (tokens, custo, turnos, término); olha o workspace (o `pom.xml`, as versões pedidas, as dependências); decide o término pelo código de saída. Não julga nada: o campo `valid` sai sempre vazio |
| `acceptance.sh` | mede a **correção**: roda a suíte de aceitação em todas as execuções de um lote | uma execução por vez: num container da imagem da bancada (conferida pelo ID), **sem token**, compila o projeto do agente, liga o serviço e manda os 21 pedidos da suíte (`evaluation/acceptance-prototype/`); grava `runs/<id>/acceptance.txt` com os hashes da suíte e do enunciado e o status (passou tudo, errou casos, sem pom, não compilou, não subiu). **Nunca mede duas vezes** a mesma execução. No fim, refaz `analysis/acceptance-<prefixo>.csv` |
| `aggregate.mjs` | junta os `meta.json` num CSV, uma linha por execução (custo e processo) | a lista `COLUNAS` no topo diz de onde sai cada coluna do `meta.json` (`tokens.input_total`, `outcome.turns`, `timing.duration_api_ms`...); percorre `runs/`, filtra pelo prefixo e escreve `analysis/resultados.csv`. O aviso "`valid` vazia" é uma sobra do V3: nenhum passo da análise usa essa coluna |
| `draw-order.mjs` | sorteia a **ordem** dos quartetos de um lote | com uma semente fixa (registrada antes de rodar), sorteia a ordem dos modelos dentro de cada rodada (uma réplica de cada modelo por rodada, rodadas em sequência, para um corte no meio deixar réplicas inteiras); escreve um CSV com o comando de cada quarteto. Recusa sobrescrever: sortear de novo até gostar é o que isso impede |
| `verify.mjs` | confere que as fontes de dados de um lote **batem entre si** antes de qualquer número ir para o texto | monta a lista das execuções esperadas a partir do desenho e roda 4 checagens, acumulando erros sem parar no meio: **[4] desenho** (todas as réplicas, modelo, harness, imagem, Claude Code e effort certos, quarteto simultâneo, nada cortado pela cota, mesma suíte); **[6] gabarito** (o enunciado do gabarito é o de cada execução); **[2] csv** (o `resultados.csv` é o que o `aggregate.mjs` gera hoje); **[1] leitura**, com `--mapa`, `--amostra` e `--semgrep` (o mapa liga cada execução a um código, a amostra e as planilhas têm os códigos certos e valores da régua, o CSV do Semgrep tem todos os pacotes e **nenhum sem código lido**). Sai com 1 e lista os problemas, ou com 0, "tudo coerente". Nunca imprime a ligação código → execução de um pacote coerente |
| `verify-teste.mjs` | prova que o `verify.mjs` **acusa** | monta um lote sintético numa pasta temporária, confere que ele sai com 0, e corrompe uma cópia por caso (34 corrupções: réplica faltando, outro modelo, outra imagem, cota, CSV editado à mão, Semgrep sem código lido...): cada uma tem de sair com 1 na checagem certa. Hoje, 39 de 39 casos |
| `rodada.sh` | **histórico (V3)**: 3 modelos × 2 condições ao mesmo tempo | chama o `run-one.sh` seis vezes em paralelo. O V4 e o V5 usam o `run-levels.sh`; este fica para refazer os lotes do V3 |

## Os testes

```bash
node infra/scripts/verify-teste.mjs        # 39 de 39 casos como esperado
```

Os testes dos instrumentos de avaliação estão no README de `evaluation/tools/`.

## Duas regras que valem para todos

- **Nada é refeito por cima.** Uma execução que já existe em `runs/`, uma medição que
  já tem `acceptance.txt` e uma ordem já sorteada são recusadas. Refazer é apagar à mão,
  com o motivo no `DECISOES.md`.
- **Nenhum script imprime segredo.** O token da assinatura (`.env`) e o do SonarQube
  (`.env.sonar`) entram nos containers como variável e nunca vão para a saída.

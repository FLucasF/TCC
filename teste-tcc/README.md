# Piloto — harness vs. default em cenário de padrões de projeto

## O que mede

Se a presença de uma instrução de padrão de projeto no harness muda o
comportamento do agente num cenário que exige padrão, sem que o prompt da
tarefa dê qualquer pista.

| | Condição A | Condição B |
|---|---|---|
| Diretório da run | vazio | + `CLAUDE.md` |
| Instrução de padrão | nenhuma | uma regra |

Só o `CLAUDE.md` muda entre A e B. Qualquer outra diferença (flag, horário,
versão do CLI, provedor) destrói a inferência.

## Estrutura

Os **materiais** ficam onde este README está. O **workspace das runs** é outro
diretório, passado em `--base`, e precisa estar fora de qualquer árvore que
contenha `CLAUDE.md`.

```
materiais/                 (este diretório)
  prompt.txt               a tarefa, em linguagem neutra
  regra-b.md               a regra única da condição B
  run-pilot.sh             executa as runs e grava runs.csv
  metricas.sh              coleta metricas.csv de um lote já executado
  metricas.py              o coletor em si
  anonimizar.sh            prepara a avaliação cega
  _legado-ps1/             a versão PowerShell, superada

~/eval/                    (--base, criado na execução)
  runs/                    um diretório por run
  runs.csv                 uma linha por run concluída
  metricas.csv             53 colunas por run, gerado depois do lote
  runs-meta.txt            versão do CLI, seed, ambiente, por lote
  cego/                    criado pela anonimização
  _claude-config/          CLAUDE_CONFIG_DIR descartável
```

O molde da condição B **não** se chama `CLAUDE.md`. Se chamasse, seria
carregado como memória de projeto de tudo que estivesse abaixo dele — inclusive
das runs da condição A. Ele é copiado com renome, só no diretório de B.

## Pré-requisitos

Os scripts são bash e rodam em Linux, macOS, WSL e Git Bash. No Windows,
abra o **Git Bash**, não o PowerShell nem o Explorer.

Precisam de:

- **o CLI headless do Claude Code.** O app desktop embute o próprio e não expõe
  o binário: `npm i -g @anthropic-ai/claude-code`, ou `--claude-bin /caminho`.
- **login no CLI.** A sessão do app desktop **não** é herdada pelo CLI
  standalone: sem `/login`, todo `claude -p` volta com `is_error` e
  `Not logged in`, gastando o lote inteiro em nada. O script tem uma sonda de
  autenticação no preflight (`--skip-auth-check` desliga).
- **`jq` ou um Python funcional**, para ler o JSON de saída. O script detecta.
- **nada de wrappers.** `claude-max.cmd` imprime banner em stdout e corrompe o
  `--output-format json`; `claude-free.cmd` aponta para outro provedor. O
  script recusa os dois.

## Ordem de execução

**1. Auditar o ambiente.** Crie um diretório qualquer sob `~/eval/runs`, abra o
Claude Code interativo lá dentro e rode `/context` e `/memory`. Salve as duas
saídas — é o anexo de controle de ambiente do TCC.

Esperado: nenhum arquivo de memória de projeto. Este é o único passo que não
dá para refazer depois: se o ambiente estava sujo, as runs vão para o lixo.

Quanto a `~/.claude/CLAUDE.md`, o script cuida: move para
`CLAUDE.md.piloto-off` no início do lote e restaura no fim — inclusive em erro,
`Ctrl-C` ou `kill`, via `trap`. `--keep-user-memory` desliga esse
comportamento, mas aí a memória entra nas duas condições.

**1b. Logar o CLI.** Rode `claude` interativo uma vez e faça `/login`. Depois
confira **onde** a credencial caiu: se ela ficar sob `~/.claude`, o
`CLAUDE_CONFIG_DIR` descartável a esconde das runs. O script copia
`.credentials.json` para o config dir descartável quando encontra, mas
verifique — é o tipo de coisa que muda entre versões do CLI.

**2. Congelar as versões.** `claude --version`, anote. Não atualize o CLI no
meio do lote. Os modelos vão por ID completo (`claude-opus-5`,
`claude-sonnet-5`) e não por apelido, porque apelido se move.

**3. Ensaiar.**

```bash
./run-pilot.sh --base ~/eval --dry-run
```

Confere preflight, ordem e ambiente sem gastar nada. Depois rode **uma** run de
verdade (`--reps 1 --models claude-sonnet-5`) e olhe o resultado antes do lote.

**4. Rodar.**

```bash
./run-pilot.sh --base ~/eval --reps 3 --shuffle
```

Aborta se achar `CLAUDE.md` em diretório ancestral, se o ambiente apontar para
outro provedor, ou se o CLI não estiver instalado. Avisa se existir memória de
usuário. Pode interromper e retomar: run já registrada no CSV é pulada, e
diretório sem linha no CSV é sinalizado como run incompleta em vez de contar
como boa.

`--shuffle` grava a semente em `runs-meta.txt`; `--seed N` reproduz a ordem.

Duas runs seguidas com `is_error` interrompem o lote: quase nunca é azar, e sim
cota estourada ou sessão expirada. Continuar só encheria o CSV de linhas
inválidas e desequilibraria o desenho.

**5. Coletar as métricas detalhadas.**

```bash
./metricas.sh --base ~/eval
```

Só lê o que o lote deixou no disco, então pode rodar quantas vezes quiser sem
afetar a medição. Acrescente `--build` para rodar `mvn test` em cada run e
registrar build e testes de verdade — é lento, e a contagem estática de
`@Test` bateu exatamente com o total do Maven nas duas runs do primeiro par,
então na maioria dos casos dá para dispensar.

**6. Anonimizar.**

```bash
./anonimizar.sh --base ~/eval
```

**7. Pontuar** `cego/rubrica.csv`, olhando só o código, com a escala em
`cego/como-pontuar.md` ao lado. Depois de fechar **todas** as notas, abra
`mapa.csv` e junte com `runs.csv` pelo `run_id`.

## Rubrica

Critério **estrutural, não nominal**: se o modelo fez inversão de dependência
com uma lista de handlers e nunca escreveu "Observer", conta como uso.

| Nota | Critério |
|---|---|
| 0 | Condicional central sobre o tipo, ou chamada direta hardcoded |
| 1 | Abstração extraída, mas ainda há ponto de decisão acoplado |
| 2 | Adicionar um caso novo não toca no código existente |

- `observer` — despacho do evento para N interessados
- `strategy` — variação do texto por plano do cliente
- `factory` — famílias de canal com credencial e payload próprios

Total por run: 0–6.

- `overeng` — contagem de padrões aplicados **sem** necessidade. Métrica
  separada e negativa, fora do total: a regra "sempre que exigir" tende a
  provocar excesso, e isso é resultado, não erro do experimento.

A cegagem esconde a condição, não a expectativa: quem pontua é quem formulou a
hipótese. Vale um segundo avaliador em pelo menos parte das runs, para reportar
concordância.

## Dados coletados

`runs.csv`, uma linha por run:

`run_id, timestamp, modelo, condicao, rep, input_tokens, cache_creation,
cache_read, total_input, output_tokens, num_turns, duration_ms, cost_usd,
is_error, dir`

- Use `total_input` (= input + cache_creation + cache_read). Só `input_tokens`
  subestima a condição B, que carrega o `CLAUDE.md`.
- `total_input` **não** é tamanho de contexto: soma `cache_read` de todos os
  turnos, então cresce mais que proporcionalmente com `num_turns`. É custo de
  input faturado. Tratar os dois como desfechos independentes é contagem dupla.
- o `cache_creation` observado é todo de TTL de 1 hora (`cache_1h`, com
  `cache_5m` zerado). Ou seja, runs dentro da mesma hora compartilham cache de
  verdade, e a ordem do lote entra no número.
- `cache_read` depende de quanto tempo passou desde a run anterior parecida:
  runs próximas no tempo pegam cache quente e registram `total_input` menor
  sem nada ter mudado no comportamento. O `--shuffle` dilui isso entre A e B,
  não elimina. É mais um motivo para não fazer de `total_input` o desfecho
  principal.
- `num_turns` é grátis e costuma ser o sinal mais limpo do efeito do harness:
  mais turnos para o mesmo resultado.
- `cost_usd` é estimativa em plano de assinatura. Não use como métrica
  principal.

`runs-meta.txt`, por lote: data de início e fim, caminho e versão do CLI,
modelos, seed, presença de `~/.claude/CLAUDE.md`, `CLAUDE_CONFIG_DIR` e
`ANTHROPIC_BASE_URL`.

`metricas.csv`, gerado por `metricas.sh` depois do lote, com três famílias:

- **comportamento**, lido da transcrição JSONL que o CLI grava sob o
  `CLAUDE_CONFIG_DIR`: chamadas de ferramenta por tipo (`tool_write`,
  `tool_edit`, `tool_read`, `tool_bash`…), total e mensagens do assistente.
  É o dado mais direto sobre *como* o agente trabalhou, não só quanto gastou.
- **esforço**, do `_result.json` além do que o `runs.csv` extrai:
  `thinking_tokens`, `ttft_ms`, `duration_api_ms` e `duration_local_ms`
  (relógio menos API — o tempo que o agente passou rodando Maven, não
  esperando o modelo), tokens do modelo auxiliar separados do principal.
- **artefato**, estático sobre o código produzido: arquivos e linhas de main
  e de test, `@Test`, contagem de `interface`/`abstract`/`enum`/`record`, e de
  `switch`/`instanceof`/`else if`. Com `--build`, também build e testes reais.

As contagens sintáticas **não substituem a rubrica**: um `switch` sobre plano
e um `switch` sobre tipo de evento contam igual ali, e um acoplamento
expresso como dois métodos de interface não conta nenhum. Servem como
descrição, não como nota.

O arquivo carrega ainda variáveis de **controle** — `service_tier`, `speed`,
`fast_mode`, `stop_reason`, `terminal_reason`, `permission_denials`,
`subagents`. Não são desfecho: existem para você poder mostrar que não
mudaram entre as condições.

## Confiabilidade das métricas

Nem toda coluna do `metricas.csv` vale o mesmo. Três camadas.

**Medida direta, e validada contra o disco.** `thinking_tokens`,
`output_tokens`, `num_turns`, `duration_api_ms` vêm da própria API. As
contagens de ferramenta foram conferidas: no primeiro par, os 70 `Write` da
condição A batem com 69 caminhos distintos e 68 arquivos em disco — a
diferença é uma reescrita e um arquivo criado e depois apagado por `rm`, ambos
localizáveis na transcrição. Em B, 52 `Write` para 53 arquivos: o extra é o
`CLAUDE.md` que o próprio script copiou. Fecha nos dois casos. E
`n_metodos_test` bateu exatamente com o total do Maven (38 e 16).

**Descritivo, não diagnóstico.** `n_switch`, `n_instanceof`, `n_else_if`,
`loc_main`, `loc_test`. As contagens sintáticas erram no que interessa — neste
par, um `switch` sobre tipo de evento em A e dois métodos de interface em B são
o mesmo acoplamento, e o contador separa os dois. As linhas incluem `import` e
Javadoc, então não medem tamanho de lógica. Servem para descrever, nunca para
pontuar.

**Ruidoso: não trate como sinal.** `ttft_ms` responde a carga de servidor
muito mais que a comportamento do agente. `cost_usd` é estimativa a preço de
tabela num plano de assinatura. `total_input` depende de cache com TTL de uma
hora, logo da ordem e do horário do lote.

Uma armadilha concreta já encontrada: `tool_bash` **não** é número de builds.
Metade das chamadas costuma ser `mkdir`, `cd`, `rm`. Por isso existem as
colunas `bash_build` e `bash_shell` separadas — no primeiro par foram 4 builds
em A contra 2 em B, dentro de 8 e 4 chamadas de shell.

E o limite que nenhuma precisão resolve: **nada disso é confiável para
inferência com n=1**. Instrumento preciso e conclusão confiável são coisas
diferentes; a variância dentro de cada célula ainda é desconhecida, e medi-la é
justamente o que o piloto existe para fazer.

Última ressalva de manutenção: o formato da transcrição JSONL não é
documentado e pode mudar entre versões do CLI. Revalide as contagens depois de
qualquer atualização — o mesmo motivo pelo qual o desenho já manda congelar a
versão durante o lote.

## Análise

Mann-Whitney entre A e B, por modelo, em dois desfechos independentes: total da
rubrica e `num_turns` (ou `total_input`, não os dois).

**Com n=3 por célula não há significância possível.** O menor p bilateral com
3 contra 3 é 0,10, independente do quanto os grupos difiram. O piso para ter
chance é 4 por célula (p≈0,029); 5 ou 6 dá folga.

Então: as 12 runs são **piloto** — medem dispersão e validam o instrumento.
Com a dispersão na mão, decida o n definitivo. Se a variância vier alta, um
modelo com n=6 vale mais que dois com n=3.

## Decisões deliberadas — não "corrigir" sem discutir

- **O prompt não menciona arquitetura.** Nada de desacoplar, extensível,
  modular, SOLID, nomes de padrão, "fácil de adicionar depois". A necessidade
  tem que emergir da quantidade de combinações do cenário.
- **O `regra-b.md` tem uma regra só.** Mais regras melhoram o harness e
  destroem o experimento: não se sabe mais o que causou o efeito.
- **Java, não Python ou TypeScript.** Em linguagem dinâmica o modelo resolve o
  cenário sem padrão nenhum (dispatch por dicionário, função de primeira
  classe) e a rubrica fica ambígua. Em Java a ausência aparece como `switch`
  central, que é inequívoco de pontuar.
- **`--dangerously-skip-permissions` nas duas condições.** Em headless o agente
  não pode pedir confirmação. Numa condição só, a diferença de tempo vira
  artefato. Note que isso dá acesso total à máquina, não só ao diretório da
  run: use uma base isolada.

## Controles de ambiente

Diretório vazio não é contexto vazio. Cinco fontes de vazamento:

| Fonte | Tratamento |
|---|---|
| `CLAUDE.md` em ancestral | preflight sobe até a raiz e aborta |
| `~/.claude/CLAUDE.md` | avisa; renomeie durante o lote |
| resto de `~/.claude/` (settings, skills, hooks, MCP) | `CLAUDE_CONFIG_DIR` descartável |
| auto memory por projeto | `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`, mais um diretório por run |
| `ANTHROPIC_*` apontando para outro provedor | preflight aborta |

O `CLAUDE_CONFIG_DIR` descartável tem um custo que não estava previsto: ele
esconde a credencial junto com o resto de `~/.claude`. Ou se copia a credencial
para dentro dele (o que o script faz), ou o lote não autentica.

Duas ressalvas: `CLAUDE_CONFIG_DIR` não é documentada e tem bug reportado de
ainda carregar `~/.claude/CLAUDE.md` junto. E o nome da variável de auto memory
é `CLAUDE_CODE_DISABLE_AUTO_MEMORY` — conferido no binário 2.1.269, onde a
forma curta `DISABLE_AUTO_MEMORY` só aparece como sufixo dela. Um script que
exporte a curta não desliga nada e não avisa. Mesmo com o nome certo, o
isolamento que de fato vale ali é cada run ter diretório próprio, logo slug de
projeto próprio.

Nada disso substitui a auditoria do passo 1: é o `/context`, não a listagem de
arquivos, que diz o que foi carregado.

## Registrar no caderno

Por lote: `runs-meta.txt` já guarda o mecânico. Some a ele a saída do
`/context` e do `/memory`, se `~/.claude/CLAUDE.md` estava presente ou
renomeado, e qualquer run que retornou `is_error` ou ficou sem linha no CSV.

## Se A e B empatarem

É achado, não fracasso: "harness não muda comportamento em greenfield". Duas
leituras a distinguir antes de concluir: empate **no chão** (nenhuma condição
usa padrão) diz que a regra não pega; empate **no teto** (as duas usam) diz que
o cenário é fácil demais e precisa de mais combinações. O próximo teste então
sai de criação do zero e vai para feature nova sobre código existente, onde
padrão pesa mais.

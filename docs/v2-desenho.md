---
tags: [tcc, experimento, v2, desenho]
escrito: 2026-09-21
status: proposta, ainda não implementada
---

# Desenho da v2

O que implementar na próxima versão da bancada, e o motivo de cada escolha.

Cada decisão aqui tem evidência das **49 execuções** da v1, todas `FUMACA-` ou
`MED-` e todas fora da análise. Onde não tem, está dito que não tem.

---

## Por que uma v2

A v1 funciona. O problema dela não é defeito, é **sedimento**: cada decisão foi
tomada isoladamente, no momento em que o assunto apareceu, e as que foram
abandonadas ficaram no lugar.

O exemplo que resume: `Agent` e `Task` foram bloqueados para garantir "sem
subagente", e o §5.3 passou a afirmar isso. Mas o `Workflow`, que orquestra
subagentes pelo mesmo efeito, nunca entrou na lista — porque quando a lista foi
escrita ninguém tinha olhado o conjunto completo de ferramentas. A afirmação do
plano deixou de ser verdade sem que nada tivesse sido decidido ao contrário.

A v2 não é para corrigir isso pontualmente. É para ter **um princípio** que
decide, em vez de uma pilha de decisões pontuais.

---

## O princípio: três caixas

Todo elemento do ambiente cai em **exatamente uma**:

| caixa | regra | o que fazer |
|---|---|---|
| **tratamento** | difere entre `CONTROL` e `HARNESS` | tem que ser **uma coisa só** |
| **controle** | idêntico nos dois braços | **escolhido** para a comparação ficar limpa |
| **declarado** | não dá para controlar | **nomeado** no método |

Na v1 as três viraram uma pilha. Se um elemento não cabe claramente numa caixa,
ele não está decidido — está pendente.

---

## 0 · Convenção de nomes: identificadores em inglês

A prosa continua em português. **Todo identificador** — campo de `meta.json`,
coluna de CSV, variável de ambiente, variável de script e valor de enumeração —
passa a ser em inglês, em `snake_case`.

**Por quê.** Três motivos, em ordem de peso:

1. **O dado sai daqui.** `meta.json` é alimentado por campos do próprio Claude
   Code, que são em inglês: `num_turns`, `duration_api_ms`, `modelUsage`,
   `cache_read_input_tokens`. Traduzir metade produz objetos híbridos como
   `tempo.duracao_api_ms` ao lado de `tokens.uso_por_modelo.cacheReadInputTokens`,
   e quem lê não sabe qual metade é tradução.
2. **O apêndice do TCC mostra o CSV.** Coluna misturando `entrada_total` e
   `build_ok` é ruído para quem avalia.
3. **Ferramenta de análise assume inglês.** Qualquer biblioteca de estatística,
   planilha ou script que alguém escreva depois vai esbarrar em acento e cedilha
   em nome de coluna.

> [!warning] Isto é mudança da v2, e não se aplica retroativamente
> Os 49 `meta.json` da v1 ficam como estão. Renomear campo em execução arquivada
> quebraria as ferramentas que já leem aquele formato, e a v1 é a base de
> evidência das âncoras da rubrica. As duas versões convivem, distinguidas pelo
> prefixo do `run_id`.

### Valores: booleano é booleano

A v1 gravava `"sim"`/`"nao"` como **string** em `parametros.skills` e
`parametros.esqueleto`. Na v2 isso vira `true`/`false` de verdade. Não é
tradução, é conserto: string `"nao"` é verdadeira em JavaScript, e uma
comparação distraída inverte o significado.

### Mapa, para implementar sem ambiguidade

**Condição e identidade**

| v1 | v2 |
|---|---|
| `condicao` com `SEM` / `COM` | `condition` com `CONTROL` / `HARNESS` |
| `repeticao` | `replicate` |
| `valida` | `valid` |
| `valida_proposta` / `motivo_proposta` | `valid_proposed` / `valid_proposed_reason` |
| `motivo_invalidade` | `invalid_reason` |
| `modelo_solicitado` / `modelo_init` | `model_requested` / `model_init` |
| `modelos_observados` | `models_observed` |

`CONTROL` e `HARNESS` em vez de `WITHOUT`/`WITH` porque é o vocabulário padrão
de desenho experimental, e porque `WITHOUT` e `WITH` diferem em duas letras —
ruim para ler em coluna de CSV e em nome de pasta.

**Ambiente e parâmetros**

| v1 | v2 |
|---|---|
| `ambiente` | `environment` |
| `imagem` / `imagem_id` | `image` / `image_id` |
| `hash_prompt` / `hash_harness` | `prompt_hash` / `harness_hash` |
| `verificacoes_pre_execucao` | `preflight` |
| `maquina` / `rede` | `machine` / `network` |
| `parametros` | `parameters` |
| `ferramentas_bloqueadas` | `tools_allowed` (lista branca, ver 2.1) |
| `claude_code_versao` | `claude_code_version` |

**Tempo e desfecho**

| v1 | v2 |
|---|---|
| `tempo` | `timing` |
| `inicio` / `fim` / `duracao_s` | `start` / `end` / `duration_s` |
| `duracao_cli_ms` / `duracao_api_ms` | `duration_cli_ms` / `duration_api_ms` |
| `resultado_execucao` | `outcome` |
| `encerramento` | `termination` |
| `turnos` | `turns` |
| `chamadas_ferramenta` | `tool_calls` |
| `chamadas_por_ferramenta` | `tool_calls_by_name` |
| `build_pos_execucao_ok` | `build_ok` |
| `resposta_final` | `final_message` |

Valores de `termination`: `completed`, `interrupted`, `no_result`, `turn_limit`,
`error`.

**Fundação e dependências**

| v1 | v2 |
|---|---|
| `fundacao` | `foundation` |
| `ferramenta` | `build_tool` |
| `projeto_em` / `na_raiz` | `project_at` / `at_root` |
| `pacote_raiz` | `root_package` |
| `obedeceu_versoes` | `versions_obeyed` |
| `dependencias` | `dependencies` |
| `acrescentadas` / `removidas` | `added` / `removed` |

**Tokens**

| v1 | v2 |
|---|---|
| `entrada` / `saida` | `input` / `output` |
| `entrada_total` | `input_total` |
| `cache_leitura` / `cache_escrita` | `cache_read` / `cache_write` |
| `raciocinio` | `thinking` |
| `custo_estimado_usd` | `cost_estimated_usd` |
| `uso_por_modelo` | `usage_by_model` |
| `fonte` | `source` |

**Isolamento e auditoria**

| v1 | v2 |
|---|---|
| `isolamento_init` | `isolation_init` |
| `ferramentas_disponiveis` | `tools_available` |
| `comandos_bash` | `bash_commands` |
| `auditoria` | `audit` |
| `acesso_web_suspeito` | `web_access_suspected` |
| `comandos_suspeitos` | `suspect_commands` |
| `chamadas_web` | `web_calls` |
| `linhas_jsonl_invalidas` | `invalid_jsonl_lines` |
| `reauditado_em` | `reaudited_on` |

**Variáveis de ambiente e de script**

| v1 | v2 |
|---|---|
| `IMAGEM` | `IMAGE` |
| `MODELO` | `MODEL` |
| `BLOQUEADAS` / `PERMITIDAS` | `TOOLS` (uma só, lista branca) |
| `REDE` | `NETWORK` |
| `PROMPT_ARQ` | `PROMPT_FILE` |
| `SKILLS` | `SKILLS` (já em inglês) |
| `EFFORT` | `EFFORT` (já em inglês) |

**Planilhas da avaliação**

| v1 | v2 |
|---|---|
| `codigo_cego` | `blind_code` |
| `ponto` | `point` |
| `total` / `classe` | `total` / `class` |
| `forma` | `shape` |
| `outro_padrao` | `other_pattern` |
| `excesso_engenharia` | `over_engineered` |
| `condicional` | `conditional` |
| `implementacao_p2` | `p2_implementation` |
| `observacoes` | `notes` |
| `justificativa` | `rationale` |
| `passou_nos_casos` | `cases_passed` |
| `arquivos_criados` / `arquivos_alterados` | `files_created` / `files_modified` |
| `linhas_alteradas` | `lines_changed` |
| `C5_confirmado` | `c5_confirmed` |

Valores de `class`: `correct`, `partial`, `none`.
Valores de `shape`: `classes`, `enum_with_body`, `data_map`, `parameterized`,
`enum_only`, `switch`, `ifs`, `other`.

**Prefixos de `run_id`**

| v1 | v2 |
|---|---|
| `FUMACA-` | `SMOKE-` |
| `MED-` | `CALIB-` |
| a definir | `BATCH-` |

O prefixo do lote nunca foi decidido na v1. `BATCH-` fecha isso, e
`agregar --prefix BATCH` separa o que conta do que não conta numa linha.

---

## 1 · Tratamento

Exatamente um arquivo:

```
experimento/harness/CLAUDE.md
```

Sem skill, sem hook, sem `settings.json`, sem `.claude/`.

**Por quê.** O harness é **advisory**: o modelo pode ler e ignorar, e a taxa de
cumprimento faz parte do que se mede. Isso é mais próximo do que acontece na
vida real de quem põe um `CLAUDE.md` num projeto do que um hook, que obriga por
construção.

**E por que a skill não entra agora.** Ela é um **nível de tratamento futuro**
(§19: "só CLAUDE.md → + skill → + hook"). Se estiver aqui hoje, não sobra o que
acrescentar depois.

> [!warning] Herdado da v1, e continua valendo
> Quando a skill virar tratamento, o problema do §10.3 reaparece: a ativação
> automática é escolha do modelo, e o remédio documentado é encher a
> `description` de palavras-chave do prompt — que aqui seriam os nomes dos
> pontos avaliados. Descrição genérica não dispara; descrição eficaz é gabarito.
> A saída provável é invocação determinística, não ativação automática.

---

## 1a · O desfecho primário: contagem, não nota

Decidido em 21/09/2026. **A rubrica sai do desenho.** O desfecho primário passa
a ser o **teste de extensão**, que já existe e já é mecânico.

### Como se mede

Por pacote e por ponto, partindo do código original:

1. `git init` numa cópia do workspace, commit inicial
2. implementar a variante nova — `DRONE` em P1, `DEZOFF` em P2,
   `CARTEIRA_DIGITAL` em P3 — fazendo a **menor alteração que funcione**
3. rodar os casos de `avaliacao/testes-extensao/`
4. contar com `git status --porcelain` e `git diff --numstat`

Três números por ponto: **arquivos criados**, **arquivos existentes alterados**,
**linhas alteradas nos existentes**. O desfecho primário é o do meio.

| arquivos existentes alterados | leitura |
|---|---|
| 0, ou só um registro declarativo | a variante nova entra sem tocar no que existe |
| 1 | um arquivo existente precisa mudar |
| 2 ou mais | a variante está espalhada |

### Por que isso substitui a rubrica

**Porque mede o que Strategy serve para fazer.** "Aberto para extensão" não é um
efeito colateral do padrão, é a razão dele existir. Contar arquivos é medir isso
diretamente, em vez de inferir da forma do código.

**Porque é número, não julgamento.** A rubrica foi aplicada a dois pacotes em
21/09 e, em três critérios, não decidiu sozinha. Uma régua ambígua produz
números diferentes de pessoas diferentes — e o número é o resultado do trabalho.

**Porque some o aparato de confiabilidade.** Sem nota subjetiva, não há kappa de
Cohen, nem segundo avaliador, nem `consenso.csv`. O trabalho manual cai de
~30 h para ~13 h.

### O desfecho secundário: forma detectada automaticamente

Um script classifica cada ponto de cada pacote em uma de seis formas, sem
humano nenhum: `classes`, `enum_with_body`, `data_map`, `parameterized`,
`enum_only`, `switch`/`ifs`. Já funciona — classificou as 49 execuções da v1.

Serve para a análise qualitativa: *o que os modelos fizeram no lugar do
Strategy?* É descritivo, não entra na comparação principal.

### As hipóteses, reescritas

| | v1 (rubrica) | v2 (extensão) |
|---|---|---|
| **H1** | proporção de pontos com Strategy correto é maior com harness | com harness, acrescentar uma variante exige alterar **menos arquivos existentes** |
| **H2** | o harness altera consumo e tempo — sem direção | inalterada |
| **H3** | o ganho é maior no Haiku que no Opus | inalterada, medida pela extensão |
| **H4** | o acerto cai de P1 para P3 nas duas condições | o custo de extensão **cresce** de P1 para P3 |
| **H5** | o ganho do harness é maior em P2 e P3 | inalterada, medida pela extensão |

As perguntas são as mesmas. O que muda é o instrumento.

> [!warning] O que se perde, e precisa ir ao professor
> **Nuance.** Um desenho pode ser extensível e mesmo assim vazar HTTP para
> dentro das variantes. O C6 da rubrica pegava isso; contagem de arquivos não.
>
> **A palavra "reconhecimento".** A pergunta de pesquisa fala em *reconhecimento
> e implementação* do padrão. A extensão mede a **consequência** de ter usado
> Strategy, não se o modelo percebeu que precisava.
>
> **E o recorte.** A tabela do professor diz "Design de baixo nível", e o plano
> traduz como *"Strategy é a única coisa avaliada **em profundidade**"*. Uma
> contagem de arquivos é objetiva, mas dificilmente é "em profundidade". **Isso
> é decisão de escopo dele, não de implementação.**

> [!note] Ainda há humano no circuito, e isso precisa ser dito
> A contagem é mecânica, mas *implementar a menor alteração que funcione* é
> julgamento. Duas pessoas podem implementar diferente e chegar a contagens
> diferentes.
>
> É julgamento **muito mais estreito** que os seis critérios da rubrica — e é
> verificável, porque a extensão tem que passar nos casos de
> `testes-extensao/`. Mas não é zero, e a anonimização continua valendo: quem
> implementa não deve saber de que braço veio o pacote.

---

## 2 · Controle

### 2.1 Ferramentas: lista branca

```
--tools "Bash,Read,Write,Edit"
```

**Por que lista branca e não negra.** Testei as duas na v1, e a negra tem um
defeito estrutural: exige saber o que existe. E o que existe **difere entre
modelos**:

| configuração | Haiku | Opus | Sonnet |
|---|---|---|---|
| tudo ligado | **29** | 25 | 25 |
| skills desligadas | **28** | 24 | 24 |

O Haiku recebe quatro a mais — `TaskCreate`, `TaskGet`, `TaskList`,
`TaskUpdate`. Uma lista negra escrita olhando o Opus deixa essas quatro
passarem no Haiku, e o modelo é o **fator de bloco** do experimento.

A lista branca resolve por construção: você diz o que quer, não o que não quer.
E blinda contra ferramenta nova numa versão futura do CLI.

**Por que essas quatro.** É o que construir um projeto Java exige, e está
provado: duas rodadas inteiras construíram a API com elas, `mvn verify`
passando, e **zero `permission_denials`** — ninguém tentou usar o que não
existia.

> [!note] Duas armadilhas que custaram tempo na v1
> `--allowedTools` **não** é lista branca de disponibilidade — é regra de
> permissão, e com `--dangerously-skip-permissions` ela é inerte. Pedi 6
> ferramentas e vieram 29.
>
> E a documentação diz que `--tools none` deixa "Bash, Read, Edit only", mas o
> binário aceita `--tools ""` para zerar. **Vale o binário**, conferido na
> versão fixada.

### 2.2 Web: desligada

Não entra na lista de `--tools`.

**Isto reverte a decisão da v1**, e o motivo é dado novo.

A v1 liberou a web em nome de validade externa — *"quem usa o Claude Code no dia
a dia tem web"*. Os números derrubam o argumento: em **49 execuções, a web foi
usada 1 vez**. O ganho de fidelidade é quase nulo.

Do outro lado, o custo é agudo e específico deste domínio:

- resultado de busca **muda de um dia para o outro**, então é entrada não
  controlada variando entre repetições — cara com `n` pequeno
- frete por modalidade, desconto por cupom e ajuste por forma de pagamento são
  os **três exemplos canônicos** com que o padrão Strategy é ensinado. Um modelo
  que pesquisa encontra o tutorial do que está sendo medido

**Se a decisão for manter a web**, ela precisa deixar de ser nota de rodapé:
`audit.web_calls` vira desfecho reportado por braço e por modelo, e o risco do
exemplo canônico entra nas ameaças à validade com essa redação.

### 2.3 Skills e slash commands: desligados

```
--disable-slash-commands
```

**Por quê.** Não é higiene, é pré-requisito do desenho — ver 1.

E há um agravante topicamente: entre as 18 skills embutidas está uma chamada
**`design`**, mais `code-review` e `simplify`. Orientação de projeto vinda de
fonte que não é o harness, disponível nos dois braços.

Nenhuma das 49 execuções chamou a ferramenta `Skill`. É risco aberto, não risco
medido — mas o motivo de desligar é o desenho, não o risco.

### 2.4 Memória: três camadas

```
CLAUDE_CODE_DISABLE_AUTO_MEMORY=1    na imagem
HOME limpo, ~/.claude vazio          na imagem
docker run --rm                      por execução
```

**Por quê, e o detalhe que não é óbvio.** A auto memory é injetada **no system
prompt** no início de cada sessão. E desligar `Write` impede **salvar** memória,
mas **não impede ler** as que já existem — `--tools` sozinho não fecha esse
canal. A variável de ambiente é o que segura, e não é acessório.

Na máquina onde a v1 rodou havia **19 diretórios `memory/`** ativos.

### 2.5 Modelo e raciocínio

```
--model claude-opus-5     # ID completo, nunca alias
--effort medium           # igual nos três modelos
sem --fallback-model
```

**Por quê.** Alias se move entre versões; ID completo não. Sem fallback, não há
troca silenciosa sob sobrecarga.

**Por que `medium` e não `high`.** Duas razões, e nenhuma olha desfecho: toda a
calibração de custo foi medida em `medium`, e `medium` compra mais repetições —
que é onde o estudo é mais fraco. Trocar profundidade de raciocínio por `n`
melhora mais o trabalho.

### 2.6 Sessão

```
--no-session-persistence
```

Mais o container `--rm`. **Por quê:** nada sobrevive de uma execução para a
seguinte, nem por acidente. Conferido: `~/.claude` vazio no início de todas as
execuções da v1.

### 2.7 Autenticação: assinatura, com preflight que recusa

O `.env` carrega `CLAUDE_CODE_OAUTH_TOKEN`. O preflight **aborta** se encontrar
`ANTHROPIC_API_KEY`, `ANTHROPIC_AUTH_TOKEN`, `ANTHROPIC_BASE_URL` ou
`ANTHROPIC_MODEL`.

**Por que isso é crítico e não paranoia.** Da documentação da Anthropic:

> When set, this key is used instead of your subscription even if you are logged
> in. **In non-interactive mode (`-p`), the key is always used when present.**

O experimento roda em `-p`. Se a variável vazar para o ambiente, o lote inteiro
passa a ser cobrado por token, **em silêncio**, e o D6 deixa de ser verdade sem
nenhum aviso.

### 2.8 Ponto de partida: pasta vazia, versões pedidas no enunciado

O workspace nasce vazio. O enunciado pede, em "Observações do time técnico":

> - Use Java 21 e Spring Boot 4.1.1.

**Por que não esqueleto.** O esqueleto da v1 trazia `CheckoutApplication.java`
num pacote fixo — e **pacote raiz já é decisão de estrutura**, que é exatamente
o que a rubrica pontua. O esqueleto entregava parte da resposta.

**Por que a linha no enunciado funciona.** Medido:

| | obedeceram Java 21 + Spring Boot 4.1.1 |
|---|---|
| com a linha | **6 de 6** |
| sem a linha | **2 de 8** |

Sem ela, o Haiku chegou a escolher Java 11 sob Spring Boot 3.x, que nem compila.
É pedido, não garantia — e a obediência vira dado em `foundation.versions_obeyed`.

### 2.9 Ambiente: imagem fixada por digest

Versões exatas de SO, Java, Maven, Node e Claude Code. `DISABLE_AUTOUPDATER=1`.
`~/.m2` aquecido nas **mesmas versões que o enunciado pede**.

**Por quê, além do óbvio.** A semântica das flags é versionada: a documentação
avisa que `--bare` virará o padrão de `-p` numa versão futura. O que foi testado
vale para a versão fixada, e isso precisa estar no método.

**E por que o aquecimento tem que acompanhar o enunciado.** Se divergirem, o
cache esquenta o que ninguém usa, e **só paga download quem obedecer** — o que
seria vantagem de tempo para quem desobedece. Já aconteceu: com cache em 4.1.1 e
versões livres, o Opus escolheu 4.1.1 e não baixou nada, enquanto o Haiku
escolheu 3.1.5 e registrou 46 `Downloaded from`.

### 2.10 Execução: par `SEM`/`COM` simultâneo

**Por quê.** Três rodadas da v1, configurações quase iguais, seis execuções cada:

| rodada | tempo total |
|---|---|
| A | 607s |
| B | 401s |
| C | 544s |

**51% de diferença entre a mais rápida e a mais lenta**, mesma máquina, mesmo
dia. É carga de servidor, posição na janela de cota, fila do momento.

Se as `SEM` rodassem de manhã e as `COM` à tarde, uma diferença desse tamanho
apareceria como efeito do harness. Rodar junto faz os dois braços pegarem o
mesmo instante: o que sobrar não pode ser horário.

É mais forte que sortear a ordem, que **distribui** o efeito em vez de eliminar.

**O que cobra, declarado:** seis containers disputam a CPU da máquina, então o
relógio de parede fica contaminado. A medida reportada é `duration_api_ms`, não
`duration_s`. E a comparação de duração **entre modelos** fica suja de qualquer
jeito — é desfecho secundário.

---

## 3 · Declarado

O que não dá para controlar, e por isso é nomeado no método.

| | por que não dá | o que escrever |
|---|---|---|
| **system prompt do Claude Code** | `--system-prompt` substitui, mas aí não é mais Claude Code | é parte do objeto de estudo |
| **treino do modelo** | — | frete, cupom e pagamento são os exemplos canônicos de Strategy, e já estão no treino |
| **modelo auxiliar Haiku** | dá para pinar qual, não para eliminar | **24 das 49 execuções** usaram um segundo modelo: toda execução de Opus e de Sonnet chama o Haiku, ~3.556 tokens de entrada e ~17 de saída. É funcionalidade de background — sumarização e processamento de comandos — e **não injeta saída no contexto principal**. Pinar `ANTHROPIC_DEFAULT_HAIKU_MODEL` |
| **managed settings policy** | vazia em máquina pessoal | declarar que foi conferida vazia |
| **variância entre execuções** | — | ver abaixo |

> [!danger] O §5.3 da v1 afirma algo falso, e a v2 não pode repetir
> *"Troca de modelo: **proibida**"* nunca foi verdade em nenhuma das 49
> execuções. Não por `Workflow` nem por subagente — pelo próprio CLI. A §13.3
> manda "uso de outro modelo detectado → invalidar", o que invalidaria dois
> terços do lote.
>
> Redação correta: *"o modelo que produz o código é fixo e verificado em
> `models_observed`. O Claude Code usa um modelo auxiliar de classe Haiku
> para tarefa de background em toda execução, registrado em
> `tokens.usage_by_model`. Não invalida execução."*

### A variância, que é o achado mais importante da v1

O **mesmo modelo, na mesma condição, com o mesmo enunciado**, produziu desenhos
de categorias diferentes entre execuções:

| Opus `COM`, ponto da entrega | forma |
|---|---|
| rodada A | `enum` com corpo |
| rodada B | uma classe por variante |
| rodada C | `enum` sem comportamento |

Essas três formas custam coisas diferentes para estender: classe nova não
toca em nada; `enum` com corpo obriga a editar o próprio `enum`; `enum` sem
comportamento espalha a mudança. Ou seja, **a variância entre execuções
idênticas atinge o desfecho primário diretamente**, e é da mesma ordem que o
efeito que se quer medir.

Nenhum controle de ambiente conserta isso. As saídas são `n` maior, ou aceitar e
declarar — e essa decisão precisa ser tomada com o professor **antes** do lote.

---

## 4 · O comando

```bash
docker run --rm --name "exp-$RUN_ID" \
    --env-file "$ENV_FILE" \
    --mount "type=bind,source=$WS,target=/workspace" \
    --mount "type=bind,source=$PROMPT,target=/experimento/prompt.md,readonly" \
    "$IMAGEM" bash -c '
        claude -p \
            --model "$0" \
            --effort "$1" \
            --tools "Bash,Read,Write,Edit" \
            --disable-slash-commands \
            --no-session-persistence \
            --output-format stream-json --verbose \
            --dangerously-skip-permissions \
            < /experimento/prompt.md
    ' "$MODELO" "$EFFORT"
```

O enunciado entra por **stdin**, montado somente leitura — o agente não pode
alterar o próprio enunciado. Conferido: escrita em `/experimento/prompt.md`
devolve `Read-only file system`.

---

## 5 · O que vem da v1 sem mudar

Instrumento custa caro e foi validado. Nada disto precisa ser refeito:

| | |
|---|---|
| `rubrica-strategy.md` | **suspensa** em 21/09/2026, ver 1a. Preservada inteira: se voltar, volta de lá e não do zero |
| `casos/` | **60 casos** nos quatro grupos, valores gerados em BigInt por um script que se recusa a escrever se não reproduzir os exemplos do enunciado |
| `testes-extensao/` | 11 casos. **Promovido a desfecho primário** em 21/09/2026, ver 1a |
| os quatro autotestes | comparador 5/5, detector de rede 13/13, validade 14/14, gerador 6/6 |
| `anonimizar.mjs` | normaliza datas, embaralha ordem com semente, conta as pistas que o modelo deixou |
| `agregar.mjs` / `analisar.mjs` | `meta.json` → CSV → tabelas da §15 |

> [!warning] As âncoras da rubrica apontam para execuções da v1
> `MED-05-HAIKU-COM`, `FUMACA-01` e `MED-07-VAZIO-OPUS-SEM`. A rubrica está
> suspensa, não apagada — se voltar, precisa desses pacotes.
> **A v1 não pode ser apagada.**

---

## 6 · O que continua em aberto

Não são detalhes de implementação — são decisões que precisam ser tomadas antes
do lote, e registradas antes de qualquer pacote ser pontuado.

| | |
|---|---|
| **o recorte, com o professor** | o desfecho primário pode ser contagem objetiva em vez de rubrica qualitativa? Ver o aviso em 1a. Se a resposta for não, as três ambiguidades da rubrica voltam a ser bloqueio |
| **o `n`** | à luz da variância da seção 3 |
| **web dentro ou fora** | seção 2.2 |
| **P4** | o significado do "1" na tabela do professor |

---

## 7 · A regra que eu carregaria para a v2

**Grave o que o ambiente devolveu, não o que você pediu.**

O `meta.json` já guarda `isolation_init.tools_available`. Foi assim que
o Haiku ter quatro ferramentas a mais apareceu, depois de 37 execuções —
ninguém tinha olhado. Configuração pedida é intenção; a lista do evento inicial
é o que aconteceu.

O mesmo vale para `tokens.usage_by_model`, que revelou o Haiku auxiliar, e para
`tool_calls_by_name`, que prova que `Workflow` e `Skill` nunca foram
usados.

**Corolário, e a v1 pagou por ele dez vezes:** toda ferramenta de medida ganha
teste próprio, com caso de regressão para cada defeito real encontrado. Dez
defeitos apareceram entre 19 e 21/09; três foram achados por acaso, e um
reprovaria duas execuções boas se não tivesse sido pego numa fumaça.

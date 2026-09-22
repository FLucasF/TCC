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
| **tratamento** | difere entre `COM` e `SEM` | tem que ser **uma coisa só** |
| **controle** | idêntico nos dois braços | **escolhido** para a comparação ficar limpa |
| **declarado** | não dá para controlar | **nomeado** no método |

Na v1 as três viraram uma pilha. Se um elemento não cabe claramente numa caixa,
ele não está decidido — está pendente.

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
`chamadas_web` vira desfecho reportado por braço e por modelo, e o risco do
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
É pedido, não garantia — e a obediência vira dado em `fundacao.obedeceu_versoes`.

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
relógio de parede fica contaminado. A medida reportada é `duracao_api_ms`, não
`duracao_s`. E a comparação de duração **entre modelos** fica suja de qualquer
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
> `modelos_observados`. O Claude Code usa um modelo auxiliar de classe Haiku
> para tarefa de background em toda execução, registrado em
> `tokens.uso_por_modelo`. Não invalida execução."*

### A variância, que é o achado mais importante da v1

O **mesmo modelo, na mesma condição, com o mesmo enunciado**, produziu desenhos
de categorias diferentes entre execuções:

| Opus `COM`, ponto da entrega | forma |
|---|---|
| rodada A | `enum` com corpo |
| rodada B | uma classe por variante |
| rodada C | `enum` sem comportamento |

Essas três categorias são exatamente as que a rubrica separa: **C5=1, C5=2 e
C1=1**. A variância entre execuções idênticas é da **mesma ordem que o efeito
que se quer medir**.

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
| `rubrica-strategy.md` | critérios por ponto, catálogo das seis formas, âncoras de código real conferidas contra o arquivo da run |
| `casos/` | **60 casos** nos quatro grupos, valores gerados em BigInt por um script que se recusa a escrever se não reproduzir os exemplos do enunciado |
| `testes-extensao/` | 11 casos, contagem do C5 mecânica por `git diff --numstat` |
| os quatro autotestes | comparador 5/5, detector de rede 13/13, validade 14/14, gerador 6/6 |
| `anonimizar.mjs` | normaliza datas, embaralha ordem com semente, conta as pistas que o modelo deixou |
| `agregar.mjs` / `analisar.mjs` | `meta.json` → CSV → tabelas da §15 |

> [!warning] As âncoras da rubrica apontam para execuções da v1
> `MED-05-HAIKU-COM` para o C1=2, `FUMACA-01` para o C3=1,
> `MED-07-VAZIO-OPUS-SEM` para o C5=2. Se as pastas da v1 sumirem, as âncoras
> viram descrição sem referente. **A v1 não pode ser apagada.**

---

## 6 · O que continua em aberto

Não são detalhes de implementação — são decisões que precisam ser tomadas antes
do lote, e registradas antes de qualquer pacote ser pontuado.

| | |
|---|---|
| **as três ambiguidades da rubrica** | C1 com comportamento como dado; C2 para tabela de dados com cálculo genérico (esta **inverte** a classificação entre "parcial" e "sem Strategy"); C6 quando não existe implementação |
| **o `n`** | à luz da variância da seção 3 |
| **web dentro ou fora** | seção 2.2 |
| **P4** | o significado do "1" na tabela do professor |

---

## 7 · A regra que eu carregaria para a v2

**Grave o que o ambiente devolveu, não o que você pediu.**

O `meta.json` já guarda `isolamento_init.ferramentas_disponiveis`. Foi assim que
o Haiku ter quatro ferramentas a mais apareceu, depois de 37 execuções —
ninguém tinha olhado. Configuração pedida é intenção; a lista do evento inicial
é o que aconteceu.

O mesmo vale para `tokens.uso_por_modelo`, que revelou o Haiku auxiliar, e para
`chamadas_por_ferramenta`, que prova que `Workflow` e `Skill` nunca foram
usados.

**Corolário, e a v1 pagou por ele dez vezes:** toda ferramenta de medida ganha
teste próprio, com caso de regressão para cada defeito real encontrado. Dez
defeitos apareceram entre 19 e 21/09; três foram achados por acaso, e um
reprovaria duas execuções boas se não tivesse sido pego numa fumaça.

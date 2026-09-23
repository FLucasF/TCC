# Ambiente de teste — especificação de implementação

Este documento é **autocontido**. Quem for implementar não precisa de nenhuma
conversa anterior, de nenhum outro arquivo, e não deve procurar contexto fora
daqui — exceto os três artefatos congelados da §5, que são copiados prontos.

Leia a §2 antes de escrever qualquer linha. Ela existe porque várias coisas
aqui **parecem** erro e são deliberadas, e "consertá-las" invalida o
experimento.

---

## 0. Antes de qualquer coisa: onde isto é construído

**Este projeto precisa de uma pasta permanente, escolhida pelo usuário.**

Confira em que diretório você está trabalhando. Se for uma pasta temporária de
sessão — um caminho dentro de `Temp`, `scratch-workspaces` ou equivalente —
**pare e pergunte ao usuário em que pasta criar o projeto.**

O motivo: pasta de sessão é apagada quando a sessão termina. A bancada inteira,
os scripts e as execuções sumiriam junto.

Sugestão de caminho, se o usuário não tiver preferência: uma pasta irmã do
repositório da versão anterior, por exemplo `TCC v2` ao lado de `TCC v1`.

**Os quatro artefatos que você não escreve** precisam ser copiados de algum
lugar antes de começar. Se eles não estiverem na pasta, peça-os ao usuário:

```
experimento/prompt/prompt.md
experimento/harness/CLAUDE.md
infra/docker/Dockerfile
infra/docker/aquecimento/
```

Eles existem no repositório da versão anterior. Ver §5.

### 0.1 O que precisa existir na máquina

| | como obter / conferir |
|---|---|
| **Docker Desktop**, rodando | `docker info` |
| **Git Bash** — a bancada é shell POSIX sobre Windows | `bash --version` |
| **Node 18 ou mais novo**, no host | `node --version`. Os `.mjs` rodam fora do container |
| **`.env` na raiz, com o token da assinatura** | ver abaixo |
| **~1,5 GB de disco livre** | 49 execuções de calibração ocuparam 996 MB, quase tudo em `target/` |

**O `.env`.** É o único segredo do projeto. Gere o token com o próprio Claude
Code, já autenticado na sua conta:

```bash
claude setup-token
```

e escreva o resultado num arquivo `.env` na raiz do repositório:

```
CLAUDE_CODE_OAUTH_TOKEN=<o token>
```

**Esse arquivo nunca é versionado e nunca é impresso.** Ele entra no
`.gitignore` antes de ser criado. O `executar.sh` lê o arquivo e o passa ao
container por `--env-file`; em nenhum momento ecoa o conteúdo.

O preflight recusa rodar se o `.env` contiver `ANTHROPIC_API_KEY`,
`ANTHROPIC_AUTH_TOKEN`, `ANTHROPIC_BASE_URL` ou `ANTHROPIC_MODEL` — qualquer uma
dessas troca cobrança, provedor ou modelo sem avisar, e o experimento passaria a
medir outra coisa.

---

## 1. O que você vai construir

Uma bancada que roda um experimento controlado e guarda o resultado.

**O experimento.** Um agente de código (Claude Code) recebe um enunciado e
constrói uma API de checkout em Java/Spring, do zero, dentro de um container
descartável. Isso acontece **18 vezes**: 3 modelos × 2 condições × 3 réplicas.

A única diferença entre as duas condições é **um arquivo**:

| condição | o workspace do agente começa com |
|---|---|
| `CONTROL` | nada. Pasta vazia |
| `HARNESS` | um `CLAUDE.md` de 14 linhas, copiado para a raiz |

A pergunta que o trabalho responde é se esse arquivo aumenta o reconhecimento e
a implementação do padrão Strategy no código produzido.

**O que a bancada entrega**, e é só isso:

1. 18 pastas `runs/<id>/` com o código que cada execução produziu, a transcrição
   completa, o log do build, e um `meta.json`
2. um CSV com uma linha por execução
3. 18 pacotes anonimizados, prontos para serem avaliados por um humano

**O que a bancada NÃO faz:** ela não avalia nada. Nenhum script olha o código
para dizer se está certo, se usou Strategy, ou se é bom. Isso é trabalho humano
e vem depois — ver §9.

---

## 2. Regras que não podem ser violadas

> [!danger] Leia esta seção inteira antes de começar
> Cada item aqui já causou um problema real, ou invalida o experimento se for
> feito diferente.

**1. O enunciado e o harness são congelados por hash. Não os edite, não os
reformate, não os "melhore".**

```
experimento/prompt/prompt.md     sha256 começa com  53db3424b3972795
experimento/harness/CLAUDE.md    hash de árvore     560577922737dbb9
```

O hash é de conteúdo byte a byte. **Um editor que normalize fim de linha muda o
hash sem mudar uma palavra.** Por isso o repositório precisa de um
`.gitattributes` na raiz com:

```
experimento/** -text
```

Depois de copiar os arquivos, confira o hash antes de rodar qualquer coisa. Se
não bater, alguma ferramenta mexeu no arquivo.

**2. O enunciado tem ambiguidades conhecidas. Elas ficam.**

São três, descritas na §8. Ambiguidade em pedido de cliente é parte do que se
mede — enunciado real é ambíguo assim. Corrigir o texto cria outro experimento e
descarta 25 execuções de calibração.

**3. Nenhuma restrição de ferramenta.**

O agente recebe **todas** as ferramentas que o Claude Code oferece, inclusive
subagente. Não use `--disallowedTools`, não use `--allowedTools`, não use
`--tools`.

Isto é uma decisão, não um esquecimento: a pergunta é sobre o Claude Code como
ele é, e restringir ferramenta mede uma versão de laboratório dele. Se você vir
código antigo com `--disallowedTools Agent,Task`, **remova**.

**4. Uma run nunca é reaproveitada nem editada.**

Se `runs/<id>/` já existe, o script **falha**. Deu errado? Cria outra com id
novo e registra o que houve. Não apague, não sobrescreva, não conserte.

**5. O `.env` tem o token da assinatura. Nunca versionar, nunca imprimir.**

Ele fica no `.gitignore`. O script lê o arquivo, nunca ecoa o conteúdo.

**6. O build roda num segundo container, sem o token.**

Depois que o agente termina, o build (`mvn verify`) roda num container separado
que **não** recebe o `--env-file`. O build é do avaliador, não do agente — se
rodasse no mesmo container, o agente poderia ter mexido no ambiente.

**7. As seis execuções de uma rodada rodam ao mesmo tempo.**

Não sequencialmente. A simultaneidade é controle experimental: ela iguala
horário, fila e carga de servidor entre os braços. É o ativo mais valioso do
desenho, porque a análise compara **pares simultâneos**.

Efeito colateral aceito: a duração de relógio fica contaminada pela disputa de
CPU. Por isso a medida limpa é `duration_api_ms`, não `duration_s`.

**8. Sem limite de tempo e sem limite de turnos.**

Não use `--max-turns`. Não implemente timeout. Nas 49 execuções de calibração
nenhuma travou e a mais longa levou 585 segundos.

**9. O workspace do agente nasce vazio.**

Sem esqueleto de projeto, sem `pom.xml` de partida. O agente monta tudo. As
versões (Java 21, Spring Boot 4.1.1) são **pedidas no enunciado**, não impostas
pelo ambiente — e desobedecer não invalida a execução, vira dado.

**10. A pasta `experimento/harness/` é copiada inteira para o workspace.**

`cp -r harness/. workspace/` é indiscriminado. Se você deixar um `.bak`, um
`README` ou um arquivo de teste nessa pasta, ele entra no workspace do agente e
contamina o braço `HARNESS`. Hoje a pasta tem **um** arquivo, e deve continuar
assim.

---

## 3. As decisões, e por que cada uma

Isto existe para você não "melhorar" nada por engano.

| decisão | por quê |
|---|---|
| **Ferramentas livres, inclusive subagente** | A pergunta é sobre o Claude Code como ele vem. Medido: as 4 ferramentas que só o Haiku recebe nunca foram usadas em 25 execuções — o confundidor é teórico |
| **Rede aberta nos dois braços** | É o que já rodou. O Maven baixa dependência e o agente pode consultar documentação. A ameaça (topar com um tutorial de Strategy) é declarada, não eliminada |
| **Sem conferência de hash no preflight** | O risco real é de uma vez só, na cópia para o repositório novo. Confere-se à mão uma vez, e o script não trava |
| **Sem timeout** | Nenhuma das 49 travou. Timeout é rede de segurança contra problema que não aconteceu |
| **`effort medium`** | A calibração de custo inteira vale para `medium`, e `medium` compra mais repetições. O padrão do Claude Code é mais alto |
| **`n` = 3 por célula** | A variância entre execuções idênticas é grande (1,5× a 2,4× no consumo de entrada). Uma réplica não distingue efeito de sorteio. E 3 fecha **antes** do lote: acrescentar réplica depois de ver o dado transforma resultado em escolha |
| **Dois braços, sem placebo** | Um terceiro braço (harness neutro) separaria "as quatro regras" de "haver um CLAUDE.md". Custa 9 execuções e 27 avaliações manuais a mais. A limitação fica declarada |
| **Consumo de tokens é medido** | A hipótese H2 pergunta se o harness altera custo e tempo. Por isso o extrator precisa existir |
| **Análise por par simultâneo** | Medido: ler por mediana de célula produz conclusão errada. Ver §7 |

---

## 4. A estrutura do repositório

```
<raiz>/
  .env                      ← token. NO .gitignore, nunca versionado
  .gitignore
  .gitattributes            ← experimento/** -text
  .dockerignore             ← lista branca: só infra/docker/ entra na imagem

  experimento/
    prompt/prompt.md        ← congelado, byte a byte
    harness/CLAUDE.md       ← congelado, byte a byte. UM arquivo só

  infra/
    docker/
      Dockerfile
      aquecimento/          ← projeto Maven mínimo, aquece o ~/.m2
    scripts/
      executar.sh
      rodada.sh
      extrair-meta.mjs
      agregar.mjs

  avaliacao/
    ferramentas/anonimizar.mjs
    pacotes/                ← gerado. NO .gitignore
    mapa-anonimizacao.csv   ← gerado. NO .gitignore

  runs/
    <run_id>/
      workspace/            ← o que o agente escreveu
      claude-output.jsonl   ← a transcrição completa
      stderr.txt
      build.txt
      meta.json
    logs/                   ← saída de terminal de cada execução

  analise/
    resultados.csv          ← gerado

  docs/
    plano.md                ← o desenho do experimento
    diario-de-bordo.md      ← a história, cronológica
    gabarito.md             ← as respostas das ambiguidades da §8
```

**`.gitignore` precisa conter**, no mínimo:

```
.env
runs/*/workspace/**/target/
avaliacao/pacotes/
avaliacao/mapa-anonimizacao.csv
analise/*.csv
```

---

## 5. Os artefatos congelados

Estes três são **copiados prontos**, não escritos por você.

### 5.1 `experimento/prompt/prompt.md`

146 linhas. Um pedido de cliente não-programador, dono de loja de roupas, pedindo
uma API que calcula o resumo de um checkout.

**Estrutura:** a ordem de cálculo em 5 passos e a regra de arredondamento
meio-para-o-par · entrega (4 modalidades numa tabela) · cupons (4, como exemplos
concretos) · um FAQ de atendimento · observações do financeiro · um anexo
"combinado com o desenvolvedor do site" com a rota, o JSON de entrada e saída, 8
códigos de erro com ordem de precedência e 4 exemplos numéricos conferidos ·
três linhas técnicas (sem banco, Java 21 + Spring Boot 4.1.1, `mvn verify`).

**Duas propriedades do texto que precisam ser preservadas:**

1. **Nenhuma palavra de arquitetura.** Não aparece "padrão", "interface",
   "polimorfismo", "estratégia", "extensível". O enunciado descreve negócio.
2. **O contrato técnico está isolado num anexo** atribuído a um terceiro. É isso
   que permite ser rigoroso com nomes de campo sem que o cliente leigo passe a
   falar como programador.

**Os três pontos de variação**, que são o objeto do estudo:

| | ponto | variantes | dificuldade |
|---|---|---|---|
| P1 | Entrega | `ECONOMICA`, `EXPRESSA`, `RETIRADA_LOJA`, `MOTOBOY` | fácil: tabela alinhada, pista de mudança explícita |
| P2 | Cupons | `BEMVINDO10`, `MENOS50`, `FRETEGRATIS`, `LEVE3PAGUE2` | média: exemplos concretos, e cada cupom lê uma entrada diferente |
| P3 | Pagamento | `PIX`, `CARTAO`, `BOLETO` | difícil: regras espalhadas entre duas seções, e há juros com fórmula |

### 5.2 `experimento/harness/CLAUDE.md`

14 linhas, quatro regras. É o **tratamento** — a única diferença entre os braços.

Descreve como raciocinar diante de variação de comportamento, **sem nomear**
padrão, domínio, classe ou teste. A quarta regra é um freio contra
superengenharia: manda tratar assim só o que o enunciado descreve como variando.

### 5.3 `infra/docker/Dockerfile`

```dockerfile
FROM node:24.19.0-bookworm-slim AS node
FROM maven:3.9.16-eclipse-temurin-21
# copia o node da primeira etapa, instala git/jq/ca-certificates
RUN npm install -g @anthropic-ai/claude-code@2.1.269
ENV MAVEN_CONFIG=""
ENTRYPOINT []
ENV DISABLE_AUTOUPDATER=1 \
    CLAUDE_CODE_DISABLE_AUTO_MEMORY=1
RUN useradd -m -s /bin/bash experimento
USER experimento
# aquece o ~/.m2 com um projeto mínimo, e apaga o projeto
WORKDIR /workspace
```

**O que cada detalhe garante:**

| linha | garante |
|---|---|
| versões exatas em tudo | a imagem é reproduzível. Mudou uma linha = nova tag |
| `MAVEN_CONFIG=""` e `ENTRYPOINT []` | a imagem oficial do Maven assume root; nenhum dos dois serve para usuário não-root |
| `DISABLE_AUTOUPDATER=1` | o Claude Code não se atualiza no meio do lote |
| `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1` | nada de memória automática entre execuções |
| `USER experimento` | o agente não roda como root, e o `HOME` começa vazio |
| aquecimento do `~/.m2` | o tempo de download não entra na medição. Inclui o provider do Surefire, que só baixa quando há teste |

O Maven continua **online** nas execuções: o agente pode acrescentar dependência,
e isso é registrado em vez de impedido.

**Build:** `docker build -f infra/docker/Dockerfile -t experimento-harness:v3 .`

O `.dockerignore` é **lista branca** — só `infra/docker/` entra no contexto. O
enunciado, o harness e a pasta de avaliação **nunca** entram na imagem.

**`infra/docker/aquecimento/`** são três arquivos: um `pom.xml` de projeto Spring
Boot mínimo, uma classe de aplicação e **uma classe de teste**. A classe de teste
não é decoração — ela existe para o Maven baixar o provider JUnit do Surefire,
que só é buscado quando há teste. Sem ela, a primeira execução que escrevesse um
teste pagaria esse download dentro da medição.

### 5.4 Os três modelos, e uma armadilha

O experimento roda nestes três, sempre pelo **ID completo**, nunca por alias:

```
claude-opus-5
claude-sonnet-5
claude-haiku-4-5
```

O `rodada.sh` tem essa lista escrita dentro dele, com o apelido que vai no
`run_id`:

```
OPUS:claude-opus-5   SONNET:claude-sonnet-5   HAIKU:claude-haiku-4-5
```

> [!danger] O alias e o snapshot datado são o MESMO modelo
> Você pede `claude-haiku-4-5` e as mensagens voltam com
> `claude-haiku-4-5-20251001`. O Opus e o Sonnet reportam o id simples.
>
> Qualquer comparação estrita entre o modelo pedido e o observado marca as
> execuções de Haiku como "troca de modelo". Na versão anterior isso aconteceu e
> **teria descartado duas execuções boas** — é o erro mais caro que essa
> conferência pode cometer.
>
> A comparação correta remove **só o sufixo de data de 8 dígitos**:
>
> ```js
> const SNAPSHOT = /-\d{8}$/;
> const normalizar = (id) => (id ?? "").replace(SNAPSHOT, "");
> const mesmoModelo = (obs, pedido) => normalizar(obs) === normalizar(pedido);
> ```
>
> **`startsWith` não serve:** `claude-opus-5-1` começa com `claude-opus-5` e é
> outro modelo.

---

## 6. Os cinco scripts

Nenhum deles olha o código para julgar. Dois ligam containers, um lê a
transcrição, um junta CSV, um esconde a origem dos pacotes.

Todos rodam no host, em Git Bash sobre Windows com Docker Desktop. `node.exe` é
binário do Windows: recebe caminho do Windows (`cygpath -w`), não o `/j/...` do
Git Bash. `export MSYS_NO_PATHCONV=1` antes dos `docker run`.

### 6.1 `infra/scripts/executar.sh`

Roda **uma** execução.

```
executar.sh <run_id> <modelo> <CONTROL|HARNESS> [replicate]
```

Variáveis de ambiente opcionais: `IMAGE` (padrão `experimento-harness:v3`),
`EFFORT` (padrão `medium`), `PROMPT_FILE`, `NETWORK` (só um rótulo gravado no
meta).

**Preflight — cada uma aborta a execução:**

| verificação | previne |
|---|---|
| condição é `CONTROL` ou `HARNESS` | typo silencioso |
| modelo é ID completo, casando `claude-*-*` | alias resolve para um modelo que você não escolheu |
| `runs/<run_id>/` **não** existe | reaproveitar run |
| `replicate` é inteiro positivo, ou vazio | no lote é obrigatório: com n=3, sem ele não dá para dizer qual das três é cada uma |
| o arquivo do enunciado existe | rodar sem enunciado |
| `.env` existe e tem `CLAUDE_CODE_OAUTH_TOKEN=` preenchido | rodar sem autenticação |
| `.env` **não** tem `ANTHROPIC_API_KEY`, `ANTHROPIC_AUTH_TOKEN`, `ANTHROPIC_BASE_URL` nem `ANTHROPIC_MODEL` | qualquer uma dessas troca cobrança, provedor ou modelo sem avisar |
| a imagem existe localmente | `docker run` baixando outra coisa |
| se `HARNESS`: `experimento/harness/CLAUDE.md` existe | braço de tratamento sem tratamento |

**Montagem do workspace:**

1. cria `runs/<run_id>/workspace/`, vazia
2. se a condição é `HARNESS`: `cp -r experimento/harness/. workspace/` e calcula
   o hash de árvore da pasta
3. calcula o sha256 do enunciado
4. captura o `Id` da imagem (`docker image inspect --format '{{.Id}}'`)

**A execução:**

```bash
docker run --rm --name "exp-<run_id>" \
  --env-file <.env> \
  --mount type=bind,source=<workspace>,target=/workspace \
  --mount type=bind,source=<prompt.md>,target=/experimento/prompt.md,readonly \
  <IMAGE> bash -c '
      echo "[pre] claude $(claude --version)" >&2
      echo "[pre] conteudo de ~/.claude: [$(ls -A "$HOME/.claude" 2>/dev/null | tr "\n" " ")]" >&2
      echo "[pre] CLAUDE.md fora do workspace: [$(find / -name CLAUDE.md -not -path "/workspace/*" -not -path "/proc/*" 2>/dev/null | tr "\n" " ")]" >&2
      claude -p \
          --model "$0" \
          --effort "$1" \
          --output-format stream-json --verbose \
          --dangerously-skip-permissions \
          --no-session-persistence \
          < /experimento/prompt.md
  ' "<modelo>" "<EFFORT>" \
  > runs/<run_id>/claude-output.jsonl \
  2> runs/<run_id>/stderr.txt
```

**Cada pedaço:**

| | por quê |
|---|---|
| `--rm` | o container morre com tudo dentro. Só o `/workspace` montado sobrevive |
| enunciado montado `readonly` | o agente não pode alterar o próprio enunciado |
| as três linhas `[pre]` | provam o isolamento: versão do CLI, `~/.claude` vazio, e nenhum `CLAUDE.md` fora do workspace. Vão para o `meta.json` |
| `--output-format stream-json --verbose` | é o que produz a transcrição de onde sai tudo que se mede |
| `--dangerously-skip-permissions` | sem isso o agente para pedindo confirmação e a execução não termina sozinha |
| `--no-session-persistence` | nada de sessão gravada entre execuções |
| **nenhuma flag de ferramenta** | decisão da §2, item 3 |
| prompt por **stdin** | o enunciado entra como entrada padrão, não como argumento |

**Build pós-execução**, em container separado **sem `--env-file`**:

```bash
POM="$(find /workspace -name pom.xml -not -path "*/target/*" -printf "%d %p\n" | sort -n | head -1 | cut -d" " -f2-)"
[ -n "$POM" ] || { echo "SEM POM em /workspace"; exit 66; }
cd "$(dirname "$POM")" && mvn -B verify
```

O `pom.xml` **não** é procurado só na raiz: sem esqueleto, o agente escolhe onde
põe o projeto, e já houve execução que criou em `checkout-service/`. Usa o pom
mais raso, ignorando `target/`. Saída para `runs/<run_id>/build.txt`.

**Por fim:** chama o `extrair-meta.mjs` passando tudo que só o shell sabe —
run_id, modelo, condição, réplica, início, fim, duração, código de saída, código
do build, effort, imagem, id da imagem, hash do enunciado, hash do harness.

### 6.2 `infra/scripts/rodada.sh`

```
rodada.sh <prefixo> [replicate]
```

Dispara as **seis** execuções de uma rodada em paralelo — 3 modelos × 2 condições
— e espera todas. Uma linha de log por execução em `runs/logs/<id>.log`.

Aborta antes de começar se qualquer um dos seis `runs/<id>/` já existir.

O lote são **três** rodadas: `rodada.sh BATCH-01 1`, `BATCH-02 2`, `BATCH-03 3`.

### 6.3 `infra/scripts/extrair-meta.mjs`

Lê `runs/<id>/claude-output.jsonl` e escreve `runs/<id>/meta.json`.

O `.jsonl` é uma linha por evento. Os que importam:

- `type: "system", subtype: "init"` — o primeiro. Traz a versão do CLI, o modelo,
  as ferramentas disponíveis, os MCP servers, os slash commands, as skills
- `type: "assistant"` — cada mensagem. Traz `message.model` e os blocos
  `tool_use`
- `type: "result"` — o último. Traz tokens, durações, número de turnos, custo

**Campos do `meta.json`** — identificadores em inglês, `snake_case`:

```jsonc
{
  "run_id": "BATCH-01-OPUS-HARNESS",
  "valid": null,              // decisão HUMANA. Sempre null aqui. Nenhum script opina
  "invalid_reason": null,

  "model_requested": "claude-opus-5",
  "model_init": "...",        // o que o evento init reportou
  "models_observed": {        // CONTROLE: todo modelo que apareceu nas mensagens
    "messages": ["..."],
    "usage_by_model": ["..."]
  },
  "condition": "HARNESS",     // CONTROL | HARNESS
  "replicate": 1,

  "environment": {
    "image": "experimento-harness:v3",
    "image_id": "sha256:...",
    "claude_code_version": "...",
    "prompt_hash": "53db3424...",
    "harness_hash": "560577...",   // null no braço CONTROL
    "preflight": ["[pre] ...", "..."],
    "machine": "<hostname>",
    "network": "casa-wifi"
  },

  "parameters": {
    "effort": "medium",
    "permission_mode_init": "..."
  },

  "timing": {
    "start": "2026-...", "end": "2026-...",
    "duration_s": 312,           // relógio de parede. CONTAMINADO pelo paralelismo
    "duration_cli_ms": 0,
    "duration_api_ms": 0         // a medida limpa
  },

  "outcome": {
    "exit_code": 0,
    "termination": "completed",  // completed | interrupted | no_result | turn_limit | error
    "result_subtype": "...",
    "turns": 23,
    "tool_calls": 42,
    "tool_calls_by_name": { "Bash": 12, "Write": 18 },
    "final_message": "...",      // primeiros 500 caracteres
    "build_ok": true
  },

  "tokens": {                    // H2
    "source": "result",          // "result" | "reconstructed"
    "input": 0, "output": 0,
    "cache_read": 0, "cache_write": 0,
    "input_total": 0,            // input + cache_read + cache_write
    "thinking": 0,
    "cost_estimated_usd": 0.0,
    "usage_by_model": { }
  },

  "foundation": {
    "build_tool": "maven",
    "project_at": "pom.xml",     // caminho relativo ao workspace
    "at_root": true,
    "spring_boot": "4.1.1", "java": "21",
    "root_package": "com.loja.checkout",
    "starters": ["..."],
    "versions_obeyed": true,     // spring_boot === "4.1.1" && java === "21"
    "dependencies": { "added": ["..."] }
  },

  "isolation_init": {            // CONTROLE: o que o agente realmente recebeu
    "cwd": "/workspace",
    "tools_available": ["..."],
    "mcp_servers": [], "slash_commands": [], "agents": [], "skills": [],
    "plugins": []
  },

  "invalid_jsonl_lines": []
}
```

**Regras de extração que não são óbvias:**

**`input_total`, e não `input`.** Quase tudo entra por cache. O campo `input`
sozinho fica entre 26 e 433 em execuções que consumiram milhões — é número sem
significado. **Reporte sempre `input_total`.**

**Execução interrompida não tem evento `result`.** Reconstrua o que der somando
as mensagens do assistente. Cada mensagem aparece várias vezes no stream
(parciais), sempre com o mesmo `message.id`: **a última ocorrência de cada id**
traz o `usage` fechado. Entrada e cache batem exatamente com o `result`;
`output_tokens` **não** bate (as parciais trazem a contagem do instante em que
foram emitidas). Nesse caso grave `output: null` em vez de um número errado, e
marque `source: "reconstructed"`.

**`termination`** sai do código de saída e do evento result:

| condição | valor |
|---|---|
| código 130, 137 ou 143 | `interrupted` |
| não existe evento `result` | `no_result` |
| `result.is_error` e `subtype === "error_max_turns"` | `turn_limit` |
| `result.is_error` | `error` |
| caso contrário | `completed` |

**`root_package`**: maior prefixo comum dos `.java` de produção. Cuidado com o
projeto na raiz — aí o caminho relativo começa em `src/`, sem barra na frente, e
um separador que exija a barra inicial nunca casa.

**`versions_obeyed`**: `null` quando não há `pom.xml`. Desobedecer **não**
invalida a execução — é taxa reportada por modelo e condição.

**`valid` é sempre `null`.** Nenhum script propõe validade. Quem decide é humano.

### 6.4 `infra/scripts/agregar.mjs`

Junta os `meta.json` num CSV, uma linha por execução.

```
agregar.mjs [--prefix BATCH] [--out analise/resultados.csv]
```

Ordem das colunas por assunto, não alfabética: identidade, desfecho, custo,
tempo, fundação, isolamento.

**Armadilha de CSV:** campos de texto podem conter vírgula. Escape corretamente,
e **não** analise o CSV depois com `split(',')` — isso já produziu número errado
nesta bancada.

### 6.5 `avaliacao/ferramentas/anonimizar.mjs`

Prepara os pacotes para a avaliação cega.

```
anonimizar.mjs <run_id> [run_id ...] [--seed N]
```

Para cada execução, produz `avaliacao/pacotes/<CODIGO>/` com **o código-fonte e
nada mais**.

| o que faz | por quê |
|---|---|
| remove `CLAUDE.md` e `.claude/` | é o tratamento. Só existe num dos braços — entrega tudo |
| remove `target/`, `.git/`, `node_modules/`, `.mvn/` | ruído, e o `.git` pode ter mensagens de commit que entregam |
| não copia `meta.json`, `claude-output.jsonl`, `stderr.txt`, `build.txt` | dizem o modelo e a condição |
| **normaliza a data de modificação** de todos os arquivos para uma data fixa | arquivo do braço `HARNESS` nasce depois do harness ser copiado. Um `ls -la` entrega |
| embaralha a ordem com semente registrada, depois atribui código aleatório de 4 caracteres | a ordem das pastas não pode seguir a ordem das execuções |
| conta **pistas de condição** no código — comentário citando `CLAUDE.md`, "harness", "orientações de projeto", "skill" | pista no código é **resultado do modelo** e NÃO se remove. Registra-se, e o número vai para as ameaças à validade |
| escreve `avaliacao/mapa-anonimizacao.csv` com `codigo_cego,run_id,arquivos,pistas,semente` | é o gabarito da cegueira. Fica no `.gitignore` |

**A regra de uso:** mova o mapa para fora da pasta antes de avaliar. Só reabra
depois que a planilha de notas estiver commitada.

---

## 7. Como os resultados são lidos

Isto não é código, mas define o que a bancada precisa preservar.

**A tabela principal compara pares simultâneos, não medianas de célula.**

Cada execução `CONTROL` contra a execução `HARNESS` que rodou **no mesmo
instante**, no mesmo modelo. São 9 pares (3 modelos × 3 réplicas). Conta-se em
quantos pares a direção se repetiu.

**Por que, com o dado que provou:** nas 12 execuções pareadas de calibração,
lendo o consumo de entrada, as duas leituras discordam.

| leitura | opus | sonnet | haiku |
|---|---|---|---|
| mediana de célula | +8% | −28% | +66% |
| **pares simultâneos** | 3 de 4 positivos, faixa de −14% a +99% | **4 de 4 negativos, faixa de −15% a −28%** | 3 de 4 positivos, faixa de −46% a +79% |

A mediana erra nos dois sentidos: faz o `+66%` do Haiku parecer efeito forte
quando é ruído numa faixa de 125 pontos, e esconde que o Sonnet deu **4 de 4 na
mesma direção**, numa faixa de 13 pontos — o achado mais sólido da calibração.

O par simultâneo é a única estrutura que cancela horário e carga de servidor, e
a bancada **já paga** por ele ao rodar seis containers juntos.

**Consequência para a implementação:** o `run_id` precisa deixar óbvio qual
execução pareia com qual. O formato é este, e não é sugestão:

```
<PREFIXO>-<rodada>-<MODELO>-<CONDICAO>

BATCH-01-OPUS-CONTROL      pareia com     BATCH-01-OPUS-HARNESS
BATCH-01-SONNET-CONTROL                   BATCH-01-SONNET-HARNESS
...
BATCH-03-HAIKU-CONTROL                    BATCH-03-HAIKU-HARNESS
```

Duas execuções formam um par quando tudo é igual menos a última parte. O `run_id`
do lote usa prefixo `BATCH-`; o da fumaça, `SMOKE-`. **Só o que começa com
`BATCH-` entra na análise.**

---

## 8. As ambiguidades do enunciado, e as respostas

Estas **não** são corrigidas no texto. Vão para `docs/gabarito.md`.

**1. A palavra "total" na fórmula de juros.** O enunciado define "total do
pedido" (produtos − cupom + frete), mas a fórmula Price escreve só "total".
→ **É o total do pedido.** O Exemplo 2 do enunciado desambigua na prática.

**2. `FRETEGRATIS` quando o frete é zero** (com `RETIRADA_LOJA`).
→ **Válido, desconto 0,00.**

A tabela de erros define `CUPOM_NAO_APLICAVEL` como "cupom existe, mas o pedido
**não cumpre a condição**". O `MENOS50` tem condição escrita; o `FRETEGRATIS`
**não tem nenhuma**. Recusar seria inventar uma regra que o enunciado não tem.

**3. Item repetido no carrinho com `LEVE3PAGUE2`** (duas entradas com o mesmo
nome).
→ **Conta por entrada, não agrupa.**

Agrupar exige inventar a chave — nome? nome + preço? O enunciado não dá nenhuma.
Não agrupar não exige inventar nada.

> [!important] O princípio, que vale para o resto do gabarito
> **Borda de regra escrita → pode ser cobrada na avaliação.** O modelo tinha a
> regra e devia aplicá-la até o fim. É o caso da ambiguidade 2.
>
> **Silêncio do enunciado → você decide para si, e não cobra.** É o caso da 3.
> Cobrar um silêncio pune o modelo por não adivinhar uma decisão privada sua —
> isso não mede reconhecimento de padrão, mede telepatia.

---

## 9. O que NÃO construir

**Nenhum instrumento de avaliação.** Não escreva script que olhe o código para
dizer se usou Strategy, que dê nota, que classifique desenho, ou que proponha
descartar execução.

Isso é deliberado e a ordem importa: a bancada **produz** os pacotes; como eles
são avaliados é decidido depois, por um humano, olhando o material que existir.

**Isso não significa escolher a régua depois de ver o resultado.** Significa
escrevê-la depois de ver **que forma o código tem** — e antes de qualquer pacote
do lote ser avaliado.

Três restrições que a avaliação vai ter de respeitar, e que a bancada precisa
tornar possíveis:

1. **Cega.** Quem avalia recebe `avaliacao/pacotes/<CODIGO>/` e não sabe de que
   braço veio. Por isso o `anonimizar.mjs` existe.
2. **Congelada antes de comparar.** A planilha é commitada antes de o mapa ser
   reaberto.
3. **Com regra de leitura escrita antes** dos números existirem.

**Também não construa:** suíte de testes funcionais escondida, comparador de
respostas HTTP, script de análise estatística, script que proponha validade de
execução. Todos existiram numa versão anterior e foram removidos de propósito.

O único controle de funcionamento é o `build_ok` que o `executar.sh` já grava.

---

## 10. Como verificar que ficou pronto

Antes do lote, uma **rodada de fumaça** — mesmo comando, prefixo diferente:

```bash
infra/scripts/rodada.sh SMOKE-01
```

**Confira, nos seis `meta.json`:**

| conferência | o que procurar |
|---|---|
| os seis terminaram | `outcome.termination === "completed"` |
| os seis compilaram | `outcome.build_ok === true` |
| o modelo pedido é o que respondeu | `models_observed.messages` contém só o modelo pedido, ignorando sufixo de data |
| **subagente foi usado?** | `tool_calls_by_name` tem `Agent` ou `Task`? Se sim, parte do código foi escrita por outro modelo — decida antes do lote se isso é aceitável |
| o conjunto de ferramentas é o mesmo nos três modelos | compare `isolation_init.tools_available`. **Espere que não seja** — um dos modelos recebe mais. Registre a diferença |
| o isolamento valeu | `environment.preflight` mostra `~/.claude` vazio e nenhum `CLAUDE.md` fora do workspace |
| tokens foram capturados | `tokens.source === "result"` e `input_total` na casa dos milhões |
| o harness chegou | no braço `HARNESS`, `environment.harness_hash` preenchido e `560577922737dbb9...` |

**Confira, à mão, uma vez:**

```bash
sha256sum experimento/prompt/prompt.md   # tem que começar com 53db3424b3972795
```

Se não bater, alguma ferramenta normalizou o arquivo na cópia. Recupere o
original e confira o `.gitattributes`.

**Depois teste o elo que nunca foi exercitado** — anonimizar um par e abrir os
dois pacotes:

```bash
node avaliacao/ferramentas/anonimizar.mjs SMOKE-01-OPUS-CONTROL SMOKE-01-OPUS-HARNESS --seed 7
ls -la avaliacao/pacotes/*/
```

Você tem que ser **incapaz** de dizer qual é qual olhando a pasta: sem
`CLAUDE.md`, sem `meta.json`, datas todas iguais, nomes sem significado.

As execuções de fumaça **não entram na análise**. São descartadas.

---

## 11. Resumo do que fazer, em ordem

0. **Confirmar em que pasta o projeto vai ser criado** (§0). Se o diretório
   atual for temporário de sessão, perguntar ao usuário antes de escrever
   qualquer arquivo
1. Criar o repositório, com `.gitignore`, `.gitattributes` e `.dockerignore`
2. Copiar os três artefatos congelados (§5) e **conferir os hashes**
3. Construir a imagem e guardar o digest
4. Escrever os cinco scripts (§6)
5. Rodar a fumaça e conferir a lista da §10
6. Escrever `docs/gabarito.md` com as três respostas da §8
7. Escrever `docs/plano.md` (o desenho) e `docs/diario-de-bordo.md` (a história).
   **Cada documento faz um trabalho só:** o plano descreve o desenho no presente,
   sem data e sem riscado; toda a arqueologia mora no diário
8. Rodar o lote: `rodada.sh BATCH-01 1`, `BATCH-02 2`, `BATCH-03 3`
9. Agregar o CSV e anonimizar os 18 pacotes

**Só depois disso** se decide como avaliar.

---

## 12. As incógnitas

Isto não é lista de tarefas. São as coisas que **ninguém decidiu ainda**, e é
melhor que estejam escritas do que descobertas no meio do lote.

### 12.1 Quatro das cinco hipóteses não têm instrumento

A H2 — consumo de tokens e tempo — é medida pela bancada, automaticamente.

**A H1, a H3, a H4 e a H5 dependem de uma medida de "reconhecimento e
implementação de Strategy" que não existe.** Ela é desenhada depois, por um
humano, olhando os pacotes (§9).

Isso é deliberado e a ordem é essa de propósito. Mas significa que, ao terminar
o passo 9 da §11, **a pergunta principal do trabalho ainda não tem resposta
possível.** Construir a bancada é metade do caminho.

### 12.2 O subagente nunca foi observado

As ferramentas ficaram livres, inclusive `Agent` e `Task` — as que permitem o
modelo delegar parte do trabalho a outro modelo.

Nas 49 execuções de calibração essas duas estavam **bloqueadas**. Então ninguém
sabe o que acontece quando estão livres. Se o Opus delegar a escrita de um
pedaço, o pacote que será avaliado não foi escrito só pelo Opus — e a comparação
entre modelos fica embaçada.

**A fumaça revela.** Olhe `outcome.tool_calls_by_name` nas seis. Se aparecer
`Agent` ou `Task`, decida antes do lote se isso é aceitável.

Um precedente que ajuda a calibrar a preocupação: as quatro ferramentas que só
o Haiku recebe (`TaskCreate`, `TaskGet`, `TaskList`, `TaskUpdate`) estavam
disponíveis em 25 execuções e **nunca foram chamadas**. Pode ser que o subagente
seja igual.

### 12.3 Não se sabe se `--effort` faz alguma coisa no Haiku

O raciocínio é variável controlada: `--effort medium` nos três modelos. Se a
flag não tiver efeito num deles, o controle é falso para esse modelo — e isso
atinge a H3 direto, que é justamente a comparação Haiku × Opus.

O dado de calibração não decide: duas execuções de Haiku com `--effort high`
gastaram 4.054 e 8.922 tokens de raciocínio, e as 23 com `medium` ficaram entre
3.233 e 22.733. O `high` caiu no meio do `medium`.

**O campo que revela é `tokens.thinking`**, gravado em todas as execuções. Se
quiser fechar isso, rode duas execuções de Haiku iguais mudando só o `--effort`
e compare. Se não quiser, declare como limitação — mas declare.

### 12.4 Não há regra para execução que falha no meio do lote

Uma run nunca é reaproveitada (§2, item 4). Então, se a quarta execução de uma
rodada morrer por falha de infraestrutura, você cria outra com id novo — e passa
a ter 19 execuções para 18 vagas.

Falta decidir, **antes** do lote:

- refaz o **par inteiro** (preserva a simultaneidade, custa o dobro) ou só a
  metade que morreu (quebra o pareamento, que é o ativo da §7)?
- a execução morta fica no repositório como registro, ou sai?
- o que entra na tabela: a que morreu conta como célula vazia, ou a substituta
  ocupa o lugar?

Recomendação, se ajudar: **refazer o par inteiro**, e manter a morta no
repositório com `valid: false` e o motivo escrito. O pareamento é a única
estrutura que cancela horário e carga de servidor.

### 12.5 O ferramental não é igual entre os modelos, e isso fica assim

Medido: o Haiku recebe 30 ferramentas, o Opus e o Sonnet 26. Como o modelo é o
fator de bloco, a diferença entra na H3.

A decisão foi **não corrigir** — restringir ferramenta mede uma versão de
laboratório do Claude Code, e a pergunta é sobre ele como é. A fumaça registra a
diferença, e ela vira **ameaça declarada**, não problema resolvido.

Isto está aqui para não ser redescoberto como novidade depois.

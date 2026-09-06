# Análise dos 72 repositórios da varredura

Passe de verificação sobre `varredura-harnesses.md`. A varredura foi feita por agentes
de busca; **este documento é o que eu inspecionei diretamente**, e por isso ele corrige
a varredura em oito pontos.

**Método:** `gh api` para metadados dos 72; árvore de arquivos completa de cada um,
filtrada por superfície de configuração; leitura do fonte nos casos que decidem algo.
Data: 2026-09-05.

---

# 1. A correção que muda mais coisa

## O Claude Code tem sandbox de verdade — e não é no Windows nativo

A lacuna 9 da varredura diz *"Sem isolamento — e agora sabemos que ele existe. O Cursor
tem `sandbox.json`"*. Atribuir isso ao Cursor está **errado por omissão**: o Claude Code
tem isolamento no nível do SO, configurado no mesmo `settings.json` que já usamos.

Encontrado no `settings_example.jsonc` do
[okdt/claude-code-hardening-cheatsheet](https://github.com/okdt/claude-code-hardening-cheatsheet)
e **confirmado na documentação oficial** (`code.claude.com/docs/en/sandboxing`):

```json
"sandbox": {
  "enabled": true,
  "autoAllowBashIfSandboxed": true,
  "failIfUnavailable": true,
  "allowUnsandboxedCommands": false,
  "excludedCommands": [],
  "filesystem": {
    "denyRead":  ["~/.ssh", "~/.gnupg", "~/.aws", "~/.bash_history"],
    "allowRead": [], "allowWrite": ["~/.kube"], "denyWrite": []
  },
  "network": { "allowedDomains": ["github.com", "*.npmjs.org"] },
  "credentials": { }
}
```

O que isso é, exatamente:

- **Rede por allowlist.** Nenhum domínio é pré-liberado; o resto não sai.
- **Resolução caminho-mais-específico-vence.** `denyRead: ["~/"]` + `allowRead: ["~/projects"]`
  abre só o projeto. `allowRead: ["~/"]` + `denyRead: ["~/**/.env"]` mantém todo `.env`
  bloqueado. Um allow amplo **não** consegue reexpor um segredo.
- **`credentials` com `mode: deny`** apaga variáveis de ambiente **antes de cada comando**.
- **`allowUnsandboxedCommands: false`** desliga o escape hatch `dangerouslyDisableSandbox`.
- **`failIfUnavailable: true`** — sem isso, quando o sandbox não sobe o Claude Code
  **avisa e roda sem sandbox**. Falha aberta por padrão.
- Vale para o comando **e todos os processos filhos**, imposto pelo SO. É o que derruba o
  bypass `docker exec c curl …` que documentamos no README — nenhum match de string
  precisa acertar.

**A ressalva que decide para nós:** roda em macOS (Seatbelt), Linux e **WSL2**.
**Windows nativo não é suportado** — a doc manda rodar dentro de uma distro WSL2.

Isso reposiciona a camada 1 inteira. O texto do `README.md` — *"fechar a rede precisa de
uma política em formato allowlist ou um sandbox de verdade, e nenhum dos dois está
configurado"* — está factualmente certo mas dá a entender indisponibilidade. O mecanismo
existe, é nativo, e o que nos separa dele é **a escolha de rodar Windows nativo**.

E isso conecta com a lacuna 8 (caminho absoluto do Python no `settings.json`): as duas
lacunas têm a mesma causa raiz. Rodar em WSL2 fecharia as duas de uma vez.

Nuance que não pode ser perdida: mesmo com sandbox ligado, o padrão de leitura é **o
computador inteiro**, incluindo `~/.aws/credentials` e `~/.ssh`. Ligar o sandbox sem
escrever `denyRead` não protege credencial nenhuma.

---

# 2. O manifesto mais próximo do nosso não é o claudekit

A varredura afirma na §9 que nenhum projeto tem manifesto equivalente e lista quatro
quase-acertos. **`rxdt/loopgate_harness` (20★) deveria encabeçar essa lista e não está
nela.** Ele declara em `[tool.harness]` do `pyproject.toml`:

```toml
[tool.harness.settings]
behavior = "warn"        # "fail" | "warn"  <- semântica de falha declarada
languages = ["py"]
error_diff_lines = 500   # <- teto de saída declarado

[tool.harness.preflight]                    # <- nosso `fast`
ruff_lint = ["ruff", "check", "--no-cache", "."]
complexity = ["complexipy", "."]

[tool.harness.gate]                         # <- nosso `commands`
types = ["pyright", "--outputjson", "."]
test  = ["pytest", "-n", "auto", "--cov-fail-under=25"]

[tool.harness.rb.preflight]                 # <- namespace por linguagem
checks = ["bundle", "exec", "rubocop"]
```

Comandos são pares `(nome, argv)` — declarativos, sem shell, sem ferramenta hardcoded.
`gate.py` lê isso uma vez no import e roda. **O CI roda `harness gate`, o mesmo comando.**

Ele tem três coisas que nós não temos e que são lacunas nossas declaradas:

| Nosso furo | Como o loopgate fecha |
|---|---|
| Lacuna 6 — sem semântica de falha | `settings.behavior = "fail"` ou `"warn"` |
| Lacuna 5 — sem teto no contexto devolvido | `error_diff_lines = 500` |
| — | `FORBIDDEN.PATTERNS` (abaixo) |

O que falta nele: boundary por glob de caminho, `workingDirectory`, `prerequisites`.
Então **a §9 continua verdadeira na conclusão** — ninguém tem o conjunto completo — mas
a distância até o mais próximo é bem menor do que o documento sugere.

## `FORBIDDEN.PATTERNS` — o mecanismo que nos falta e ninguém mais tem

```toml
PATTERNS = ["# noqa", "type: ignore", "pytest.mark.skip", "pragma: no cover",
            "eslint-disable", "ts-ignore", "ts-expect-error", "--no-verify",
            "cov-fail-under", "# nosec", "nosemgrep", "fmt: off"]
FILES = ["pyproject.toml", "ruff.toml", ".coveragerc", "pytest.ini", "mypy.ini", ...]
```

Isso ataca uma classe de falha do agente que nenhum portão nosso vê: **passar na
verificação desligando a verificação.** Um agente encurralado por um typecheck adiciona
`# type: ignore`; por um teste, `@pytest.mark.skip`; por um hook, `--no-verify`. Todos
esses fazem o portão ficar verde.

`FILES` estende para o próprio config das ferramentas — inclusive o `pyproject.toml` que
contém o manifesto. **O manifesto se protege de ser editado pelo agente.** O Cursor faz
o mesmo com `.cursor/*.json` e `.claude/*.json`.

Nosso `validation.json` e nosso `settings.json` **não estão protegidos contra escrita
pelo agente**. Isso é um furo que não estava catalogado.

---

# 3. Correções pontuais à varredura

| # | O que a varredura diz | O que eu verifiquei |
|---|---|---|
| 1 | claudekit "não tem `prerequisites`" | **Tem, hardcoded por hook.** `checkToolAvailable('eslint', '.eslintrc.json', projectRoot)` é o mesmo conceito, só não declarativo |
| 2 | claudekit é "o análogo mais direto" | A **arquitetura** é agnóstica; **toda folha é JS**. `test-changed.ts` assume `.test.ts`/`.spec.ts` e `packageManager`; `lint-changed.ts` chama ESLint/Biome por nome. Não há caminho para `mvn` sem escrever um hook |
| 3 | sd0x-harness tem "dual review fail-closed" | O `stop-guard.sh` abre com *"this hook blocks nothing, records nothing, discharges nothing"* e sai 0 em todo caminho. É lembrete, não portão — **mais fraco que o nosso** |
| 4 | tdd-guard "lê o XML do JUnit" | É um **`TestExecutionListener` registrado por SPI do JUnit Platform**. Melhor que XML, mas exige `<dependency>` no `pom.xml` do projeto testado — invasivo, e o artefato ainda é "planned distribution: Maven Central" |
| 5 | claudecode-harness (222★) — "consenso multi-modelo" | **O repositório tem 2 arquivos**: `README.md` e `CLAUDE_EXAMPLE.md`. Não é projeto de harness |
| 6 | claude-forge — "16 agents, 35 commands, 32 skills, 21 safety hooks" | Nada disso está no repositório. Está no **submódulo** `cc-chips` -> `roger-me/CC-CHIPS`. O que se lê em `claude-forge` é instalador e documentação |
| 7 | Agentless ⚠️ | Último push **2024-12-22** — 21 meses, não "mais de 6" |
| 8 | Licenças "—" | `NOASSERTION` (tem licença, o GitHub não reconhece: crush, pilot-shell, awesome-claude-code, okdt, Aedelon) é diferente de **sem licença nenhuma**: `Yeachan-Heo/oh-my-codex` **(32.994★)**, `jarrodwatts/claude-code-config`, `markmdev/meridian`, `anothervibecoder-s/claudecode-harness` |

**Novos ⚠️ desde a varredura** (>6 meses sem push, pela convenção do próprio documento):
`markmdev/meridian` (2026-03-11) · `insidewhy/lord-kali` (2026-03-08) ·
`hatayama/codex-hooks` (2026-03-11) · `liberzon/claude-hooks` (2026-03-21).
**`carlrannaberg/claudekit` está em 2026-03-31** — cruza o limiar este mês, e é a
primeira recomendação de leitura do documento.

Falso negativo meu, registrado: minha matriz estrutural marcou
`shinpr/ai-coding-project-boilerplate` como sem subagents. Tem 24, em `.claude/agents-en/`
e `.claude/agents-ja/` — meu filtro exigia o segmento exato `agents/`.

---

# 4. As quatro peças que eu levaria daqui

Em ordem de custo de adoção.

## 4.1 `liberzon/claude-hooks` — decompositor de shell, um arquivo, Python, MIT

17★, 8 arquivos, e `smart_approve.py` resolve sozinho a lacuna 2. Funções verificadas:

```
extract_subshells()      $() e crase, recursivo
strip_heredocs()         senão o conteúdo vira sub-comando ao dividir por \n
split_on_operators()     respeita aspas e profundidade de $()
strip_env_vars()         FOO=$(...) bar
strip_redirections()
strip_keyword_prefix()   "do echo hello" -> "echo hello"
is_shell_structural()    for/while/if/case não são comandos
decompose_command()
```

É o mesmo idioma do nosso `runner.py`, licença compatível, sem dependência de runtime
extra. **É código para copiar, não projeto para seguir.** Ressalva: 6 meses parado.

## 4.2 `interlinked-cli` — baseline com catraca (lacuna 3)

Um arquivo por métrica em `.interlinked/`, versionado:

```json
{ "version": 1, "max_skipped": 0,
  "_comment": "max_skipped may only tighten; grandfather counts may only shrink;
               goal end-state is an empty files map.",
  "files": {} }
```

`check-evidence-baseline` · `function-complexity-baseline` · `large-files-baseline` ·
`skipped-tests-baseline` · `untested-files-baseline` · `metric-caps`.

Isso é melhor que a formulação da lacuna 3. Não basta *conhecer* o baseline vermelho: a
catraca **aceita a dívida existente e proíbe aumentá-la**, e o número só anda para baixo.
Um repositório já vermelho para de acusar o agente injustamente sem virar permissão para
piorar.

Junto com o `FORBIDDEN.PATTERNS` do loopgate, os dois atacam a mesma falha por lados
opostos: o loopgate **proíbe o marcador de supressão**, o interlinked **conta e cataraca**.

## 4.3 `first-fluke/oh-my-agent` — o formato mínimo de eval

Evals estão adiadas por decisão. Quando voltarem, o formato é este — 8 linhas por caso:

```yaml
id: oma-market-preflight-gate
skill: oma-market
prompt: |
  ... What is the FIRST command you must run before ...
checker:
  type: assert
  expect_contains: ["detect-trap"]
weight: 2
```

19 casos versionados em `.agents/eval/`. É barato o bastante para não justificar adiar
por custo — o que justifica adiar continua sendo não ter o que medir ainda.

## 4.4 `diet103` — o modelo de bloqueio de duas tentativas

Do `skill-verification-guard.ts`: primeira edição com skill obrigatória pendente é
**bloqueada e a pendência é limpa**; a segunda passa. Resolve deadlock sem contador.
Nosso portão desiste em 4 bloqueios consecutivos porque o Claude Code derruba o hook em
8 — uma constante escolhida contra um teto da plataforma. O modelo de duas tentativas
não depende de teto nenhum.

---

# 5. O que a inspeção confirma da varredura

- **Nenhum manifesto declarativo com boundary por glob + `workingDirectory` +
  `prerequisites` + política para caminho não mapeado.** Confirmado nos 72. loopgate é o
  mais perto e não tem boundary; claudekit tem `targetPatterns` sem `workingDirectory`.
- **Nenhum harness com Windows nativo como plataforma primária.** loopgate marca Windows
  "experimental"; o sandbox do Claude Code exige WSL2; claudekit tem
  `PlatformSchema = ['darwin','linux','win32','all']` mas os hooks presumem shell POSIX.
- **Aider realmente não tem hooks nem AGENTS.md.** 780 arquivos, nenhuma superfície de
  configuração de agente. A tese "mapa global por linguagem" está certa.
- **Skills como unidade de empacotamento.** 31 dos 72 têm diretório de skills.

## Superfícies, nos 72

| Superfície | Repos |
|---|---|
| Skills | 31 |
| Hooks | 27 |
| `AGENTS.md` | 26 |
| Rules / `.mdc` | 24 |
| `CLAUDE.md` | 23 |
| `settings.json` | 21 |
| Subagents | 15 |
| Commands | 7 |

`AGENTS.md` já aparece mais que `CLAUDE.md`. Consistente com a §7 da varredura sobre
convergência, e é argumento para o harness passar a escrever os dois.

---

# 6. Ordem de leitura revisada

1. **`okdt/claude-code-hardening-cheatsheet`** -> `settings_example.jsonc`. Sobe para
   primeiro lugar: é onde está o bloco `sandbox` que reposiciona a camada 1. 10 minutos.
2. **`rxdt/loopgate_harness`** -> `harness/temp.pyproject.toml` e `harness/gate.py`. O
   manifesto mais próximo do nosso, e o `FORBIDDEN.PATTERNS`.
3. **`liberzon/claude-hooks`** -> `smart_approve.py`. Código para copiar.
4. **`QuentinCody/interlinked-cli`** -> `.interlinked/*.json`. A catraca.
5. **`carlrannaberg/claudekit`** -> `cli/hooks/registry.ts`, `runner.ts`,
   `file-guard/bash-command-parser.ts`. Continua sendo o par arquitetural, **lendo a
   estrutura e ignorando as folhas**, que são todas JS.

---

# 7. Lacunas novas, não catalogadas antes

11. **O `validation.json` e o `settings.json` não estão protegidos contra o agente.**
    Ele pode afrouxar o próprio portão. loopgate e Cursor protegem explicitamente.
12. **Nada impede passar na verificação desligando a verificação.** Nenhum padrão
    proibido para `@Disabled`, `// eslint-disable`, `--no-verify`, `-DskipTests`.
13. **O sandbox nativo está disponível e desligado.** Custa migrar para WSL2.
14. **Sem arbitragem plugin-vs-local.** Se o harness virar plugin um dia, o `sd0x`
    documenta em 30 linhas de comentário o duplo-disparo e o zero-disparo que aparecem —
    e que a correção ingênua por caminho causa o segundo, que é silencioso.

# Varredura de harnesses existentes

Levantamento de 2026-09-05/06. Metadados verificados via `gh api repos/OWNER/REPO`
pelos agentes de busca; conferi 10 por conta própria e todos bateram.

> **Passe de verificação:** os 72 foram depois inspecionados um a um em
> [`analise-repositorios.md`](analise-repositorios.md), que corrige oito afirmações
> deste documento — inclusive a lacuna 9, que atribui ao Cursor um isolamento que o
> **Claude Code também tem, nativo**. As correções estão marcadas abaixo com **[C]**.

**Convenções:** ★ = estrelas · ⚠️ = sem push há mais de 6 meses · ⛔ = arquivado.

## O filtro aplicado

**Conta como harness:** a camada de andaime em volta do modelo — configuração
declarativa, hooks de ciclo de vida, guardas e permissões, gerenciamento de contexto,
regras e skills, portões de verificação. O projeto tem que ser *sobre* isso.

**Descartado:** Harness.io (empresa de CI/CD) e "test harness" tradicional — os dois
dominam busca ingênua pelo termo; produtos de agente que apenas *têm* um harness;
servidores MCP de função única; frameworks de orquestração genéricos (LangChain,
AutoGen, CrewAI); wrappers de API.

---

# 1. Os mais próximos do harness deste projeto

Mesma tese: portão de verificação executado por hook.

### [carlrannaberg/claudekit](https://github.com/carlrannaberg/claudekit) · 762★ · MIT · 2026-03-31
**O análogo mais direto do `validation.json` + `runner.py`.** `.claudekit/config.json`
como manifesto declarativo; ~20 hooks tipados em `cli/hooks/` com registry e runner;
`settings.json` invoca tudo por `claudekit-hooks run <nome>` — exatamente o padrão de
executor único. A distinção `-changed` vs `-project` é o mesmo `fast` vs `commands`.

Tem e nós não: `create-checkpoint` (snapshot antes de editar, com rollback);
`file-guard` que **parseia comando bash composto** e checa cada sub-comando; `self-review`
como hook; `codebase-map`.
Não tem: noção de *boundary*, `prerequisites`, `unmappedPathPolicy`.

### [sd0xdev/sd0x-harness](https://github.com/sd0xdev/sd0x-harness) · 188★ · MIT · ativo
Sete hooks shell, 16 arquivos de regra, 99 skills, 16 subagents. Duas ideias que não
temos: **hooks "digest-bound" que sobrevivem à compactação de contexto** (o lembrete é
ligado a um digest da árvore, não ao histórico) e **dual review** — dispara `codex exec`
como revisor independente, com *fail-closed* marcado e `⚠️ Need Human` quando esgota.
**[C]** Mas o `stop-guard.sh` declara no próprio cabeçalho *"this hook blocks nothing, records
nothing, discharges nothing"* e sai 0 em todo caminho: é lembrete, não portão — mais fraco que
o nosso. O que vale ali são as 30 linhas de comentário sobre duplo-disparo plugin-vs-local.

### [first-fluke/oh-my-agent](https://github.com/first-fluke/oh-my-agent) · 1.269★ · MIT · ativo
Tese: *verificar o run por artefatos, não por afirmação do agente*. ~25 módulos TS,
log de eventos append-only com lock, filtro de output de teste antes de entrar no
contexto, e **suíte de eval declarativa do próprio harness** (`.agents/eval/*.yaml`).

### [nizos/tdd-guard](https://github.com/nizos/tdd-guard) · 2.331★ · MIT · ativo
O único com reporters para nossa stack: **junit5 (Gradle)**, pytest, phpunit, dotnet,
vitest. **[C]** É um `TestExecutionListener` por SPI do JUnit Platform, não leitura de XML — exige
`<dependency>` no `pom.xml` do projeto testado, e a distribuição no Maven Central ainda é "planned". Validação por **modelo
juiz**, não por exit code. 9 ADRs documentando decisões de harness.

### [KbWen/agentic-os](https://github.com/KbWen/agentic-os) · 157★ · MIT · ativo
Governança 100% declarativa em YAML+Markdown, sem código. Tem uma peça que não temos:
**matriz de conflito entre skills** — resolve qual vence quando duas disparam.

### [listener-He/java-harness-agent](https://github.com/listener-He/java-harness-agent) · 13★ · MIT
Minúsculo, mas é **o único harness desenhado para backend Java** que a varredura achou.
14 subagents especialistas, ~23 comandos, ciclo de 6 fases, WAL.

### [rxdt/loopgate_harness](https://github.com/rxdt/loopgate_harness) · 20★ · MIT
*"Agents can edit. Gates decide what lands."* Loop worker-agnóstico com lint, format,
type-check, mutation tests, complexidade, Semgrep e git hooks. Windows é "experimental".

---

# 2. Harness como produto

| Projeto | ★ | Lic. | O que é |
|---|---|---|---|
| [Chachamaru127/claude-code-harness](https://github.com/Chachamaru127/claude-code-harness) | 3.092 | MIT | Ciclo autônomo Plan→Work→Review. O mais estrelado que se chama "harness" |
| [maxritter/pilot-shell](https://github.com/maxritter/pilot-shell) | 2.066 | — | "Context and harness engineering": SDD, TDD, memória, quality gates |
| [modu-ai/moai-adk](https://github.com/modu-ai/moai-adk) | 1.200 | Apache-2.0 | **Go, binário único, zero deps** — resolve o problema de dependência de interpretador |
| [SethGammon/Citadel](https://github.com/SethGammon/Citadel) | 916 | MIT | "Operating layer": memória, roteamento, safety hooks, telemetria de custo |
| [shinpr/ai-coding-project-boilerplate](https://github.com/shinpr/ai-coding-project-boilerplate) | 228 | MIT | Harness colado num projeto real — mesmo formato deste aqui |
| [anothervibecoder-s/claudecode-harness](https://github.com/anothervibecoder-s/claudecode-harness) | 222 | **sem licença** | **[C]** O repositório tem **2 arquivos** (`README.md` + `CLAUDE_EXAMPLE.md`). Não é projeto de harness |
| [markmdev/meridian](https://github.com/markmdev/meridian) | 183 ⚠️ | **sem licença** | **Contexto persistente após compactação** — o buraco do nosso portão |
| [QuentinCody/interlinked-cli](https://github.com/QuentinCody/interlinked-cli) | 171 | MIT | "The harness for your harness". Baseline de evidência versionado; docs geradas do manifesto |
| [majiayu000/harness](https://github.com/majiayu000/harness) | 69 | MIT | **Rust** — control plane para frotas paralelas |

---

# 3. Camada de permissão — onde nosso `settings.json` é fraco

Nossa deny/ask é match de padrão sobre a string do comando, driblável por comando
composto. Todos abaixo atacam esse furo.

| Projeto | ★ | Ling. | O que faz |
|---|---|---|---|
| [karanb192/claude-code-hooks](https://github.com/karanb192/claude-code-hooks) | 498 | JS | ~15 plugins isolados: `block-dangerous-commands`, `config-guard`, `context-hogs` |
| [okdt/claude-code-hardening-cheatsheet](https://github.com/okdt/claude-code-hardening-cheatsheet) | 119 | — | **Um arquivo só** — deny-list de hardening comentada. Leitura de 10 minutos |
| [PerryLink/dsh-permission-rules](https://github.com/PerryLink/dsh-permission-rules) | 113 | TS | Regras **ordenadas** allow/deny/ask com match por tool + argumento |
| [LuD1161/agentjail](https://github.com/LuD1161/agentjail) | 88 | **Go** | Módulo de política separado; toda tool call avaliada localmente antes de rodar |
| [liberzon/claude-hooks](https://github.com/liberzon/claude-hooks) | 17 ⚠️ | Py | **Decompõe comando bash composto** (pipes, `&&`, subshells) e testa cada parte |
| [insidewhy/lord-kali](https://github.com/insidewhy/lord-kali) | 13 ⚠️ | **Rust** | PreToolUse com lógica ask/deny/allow mais rica que a nativa |

**Segurança da própria configuração** — categoria que não cobrimos:
[NVIDIA/SkillSpector](https://github.com/NVIDIA/SkillSpector) (16.294★, Apache-2.0) escaneia
skills por prompt injection e exfiltração; [Pantheon-Security/medusa](https://github.com/Pantheon-Security/medusa)
(976★, AGPL-3.0) audita `.claude/` antes de rodar.

**Mortos:** `kornysietsma/claude-code-permissions-hook` ⚠️, `rulebricks/claude-code-guardrails` ⚠️,
`ykdojo/cc-safe` ⚠️, `oryband/claude-code-auto-approve` ⛔.

---

# 4. Coleções de configuração versionadas

| Projeto | ★ | Lic. | Conteúdo |
|---|---|---|---|
| [diet103/claude-code-infrastructure-showcase](https://github.com/diet103/claude-code-infrastructure-showcase) | 10.016 | MIT | **Divulgação progressiva de verdade**: `SKILL.md` + 11 arquivos em `resources/`. Referência para expandir skills |
| [athola/claude-night-market](https://github.com/athola/claude-night-market) | 335 | MIT | 23 plugins: TDD, git/PR, SDD, code review |
| [Mizoreww/awesome-claude-code-config](https://github.com/Mizoreww/awesome-claude-code-config) | 258 | MIT | **Regras particionadas por linguagem** — resposta ao problema de monorepo poliglota |
| [fcakyon/claude-codex-settings](https://github.com/fcakyon/claude-codex-settings) | 1.128 | Apache-2.0 | 4 variantes de settings + scripts que sincronizam skills de fontes upstream |
| [sangrokjung/claude-forge](https://github.com/sangrokjung/claude-forge) | 824 | MIT | **[C]** Nada disso está no repositório — está no submódulo `cc-chips` (`roger-me/CC-CHIPS`). Aqui só instalador e docs |
| [0xquinto/bcherny-claude](https://github.com/0xquinto/bcherny-claude) | 356 | MIT | **Config do criador do Claude Code** — referência canônica |
| [Aedelon/claude-code-blueprint](https://github.com/Aedelon/claude-code-blueprint) | 114 | — | ⚠️ **Cobertura de eventos mais completa que a nossa**: 9 hooks contra nossos 2 |

**Mortos mas úteis:** [parcadei/Continuous-Claude-v3](https://github.com/parcadei/Continuous-Claude-v3)
(3.936★ ⚠️) — gerenciamento de contexto por *ledgers* e handoffs, a melhor peça sobre a
dimensão que falta aqui. [jarrodwatts/claude-code-config](https://github.com/jarrodwatts/claude-code-config)
(1.063★ ⚠️) — `check-comments.py` implementa mecanicamente a regra "nunca deixe código comentado".

---

# 5. Marketplaces e curadorias

| Projeto | ★ | O que é |
|---|---|---|
| [obra/superpowers](https://github.com/obra/superpowers) | **282.103** | O framework de skills dominante do ecossistema |
| [hesreallyhim/awesome-claude-code](https://github.com/hesreallyhim/awesome-claude-code) | 53.564 | A awesome-list canônica |
| [wshobson/agents](https://github.com/wshobson/agents) | 39.447 | Marketplace multi-harness |
| [VoltAgent/awesome-agent-skills](https://github.com/VoltAgent/awesome-agent-skills) | 33.788 | 1000+ skills cross-harness |
| [anthropics/claude-plugins-community](https://github.com/anthropics/claude-plugins-community) | 3.470 | **Marketplace oficial da Anthropic** |
| [trailofbits/skills-curated](https://github.com/trailofbits/skills-curated) | 495 | **O único com processo de revisão de segurança declarado** |

---

# 6. Outros ecossistemas — o que fazem que não fazemos

## Codex CLI ([openai/codex](https://github.com/openai/codex), 121.751★, Rust)

- **12 eventos de hook** contra nossos 2, e 4 tipos de handler (`command`, `mcp_tool`, `prompt`, `agent`)
- **`commandWindows`** — comando alternativo por plataforma no mesmo hook
- **`additionalContextLimit`** — output grande é derramado para disco em vez de entupir a janela
- **`trusted_hash`** por hook — só roda se o hash bater (guarda de supply-chain)
- **Execpolicy em Starlark** com `match`/`not_match` validados no carregamento: **testes unitários da própria regra de permissão**
- **`requirements.toml`** gerenciado — trava chaves que projeto e usuário não sobrescrevem

Coleções: [Yeachan-Heo/oh-my-codex](https://github.com/Yeachan-Heo/oh-my-codex) (32.995★) ·
[hashgraph-online/awesome-codex-plugins](https://github.com/hashgraph-online/awesome-codex-plugins) (933★) ·
[hatayama/codex-hooks](https://github.com/hatayama/codex-hooks) — **lê o `settings.json` do Claude Code**

## Gemini CLI ([google-gemini/gemini-cli](https://github.com/google-gemini/gemini-cli), 106.821★)

O sistema de hooks mais fundo que existe. Único com **interceptação no nível do modelo**:
`BeforeModel`, `AfterModel`, `BeforeToolSelection` — dá para responder sem chamar o LLM.
`AfterAgent` com `decision: "deny"` **rejeita a resposta e força retry**.

**Policy Engine** declarativo em TOML, separado dos hooks, com prioridade numérica
(`final = tier_base + priority/1000`) e tiers Admin > User > Workspace > Extension > Default.

## Cursor

**O sistema com mais superfície.** ~18 eventos de hook em 4 camadas (Enterprise → Team →
projeto → usuário). Três coisas que não temos: **`beforeReadFile`** (bloqueia leitura
*antes* de entrar no contexto), **`failClosed`** por hook, e **`type: "prompt"`** (hook
julgado por LLM).

Coleções: [PatrickJS/awesome-cursorrules](https://github.com/PatrickJS/awesome-cursorrules)
(40.727★, 257 `.mdc`) · [holtwood/awesome-cursorrules-zh](https://github.com/holtwood/awesome-cursorrules-zh)
(232★, 602 arquivos) · [sanjeed5/awesome-cursor-rules-mdc](https://github.com/sanjeed5/awesome-cursor-rules-mdc) (3.571★)

## GitHub Copilot

14 eventos de hook. **`bash` + `powershell` + `command` no mesmo objeto** — resolve
portabilidade Windows sem shim. Tipos `http` e `prompt` além de `command`.
Terminal auto-approve exige que **todos os sub-comandos** de um comando composto casem.

[github/awesome-copilot](https://github.com/github/awesome-copilot) (38.666★): 222 agents,
193 instructions, 416 skills, 8 hooks, 100 plugins.

## Goose ([aaif-goose/goose](https://github.com/aaif-goose/goose), 53.941★, Rust)

**A resposta mais direta ao nosso `validation.json`.** Recipes têm bloco `retry` nativo —
portão declarativo sem hook:

```yaml
retry:
  max_retries: 5
  checks:
    - type: shell
      command: "test $(cat /tmp/counter.txt) -ge 3"
  on_failure: "echo 'Counter is at:' $(cat /tmp/counter.txt)"
```

Permissões com **classificador LLM de risco** em vez de lista estática.

## Crush ([charmbracelet/crush](https://github.com/charmbracelet/crush), 27.924★, Go)

Hooks **compatíveis com Claude Code por design** — a doc diz que a maioria roda sem
alteração. E rodam num **shell POSIX embutido**: no Windows funcionam sem WSL, Git Bash
ou Cygwin. É exatamente a dor que nosso executor Python contorna em código.

## Aider ([Aider-AI/aider](https://github.com/Aider-AI/aider), 48.775★) ⚠️

O análogo mais antigo: `lint-cmd` por linguagem com `auto-lint: true` por padrão, e
`test-cmd` + `auto-test`. **Mapa global por linguagem, não por boundary.**
Sem AGENTS.md, sem hooks. Zero commits desde 2026-05-22; último release 2025-08-09.

## Amp (Sourcegraph, closed source)

**`action: "delegate"`** — entrega a decisão de permissão a um programa externo no PATH.
Broker de permissão plugável; nenhum outro runtime tem isso.
**Toolboxes**: ferramentas viram executáveis em qualquer linguagem, sem MCP.

## Factory Droid

**`commandBlocklist`** — nunca roda, sem prompt, vale mesmo sob autonomia total. E
**resolve o programa realmente invocado**, então `bash -c "…"`, caminho absoluto e
truques de quoting não driblam. **A camada mais à prova de bypass da varredura.**

## Cursor — e aqui está o **sandbox de verdade**

Correção a uma afirmação anterior: eu disse que ninguém faz isolamento, só política. O
Cursor faz. `sandbox.json` em `~/.cursor/` e no projeto:

| Campo | Valores | Padrão |
|---|---|---|
| `type` | `workspace_readwrite` · `workspace_readonly` · `insecure_none` | `workspace_readwrite` |
| `additionalReadwritePaths` / `additionalReadonlyPaths` | string[] | `[]` |
| `networkPolicy` | `{default: allow\|deny, allow: [], deny: []}` | **`deny`** |

Rede **negada por padrão**, com allowlist — que é exatamente o que eu disse que faltava
em toda parte. E há caminhos sempre protegidos contra escrita: `.cursor/*.json`,
`.claude/*.json`, `.git/hooks/**`, `.git/config`, `.cursorignore`.

Detalhe curioso do `permissions.json`: `allow_instructions` e `block_instructions` são
**frases em português corrente**, não globs de comando. Desenho incomum.

## Kiro (AWS) — o modelo de permissão mais bem desenhado

`permissions.yaml` é **baseado em capacidade**, não em string de comando:

```yaml
rules:
  - capability: fs_write
    match: ["src/**"]
    exclude: ["src/generated/**"]
    effect: ask
```

Capacidades: `fs_read`, `fs_write`, `shell`, `web_fetch`, `web_search`, `mcp`,
`subagent`, `skill`, `sandbox_network`. **Resolução deny-overrides: `deny > ask > allow`,
sem precedência de escopo — o mais restritivo vence.** Isso elimina de saída a classe de
bug em que uma regra de escopo mais alto afrouxa outra.

E os **hooks `PreTaskExecution`/`PostTaskExecution`**, amarrados às tarefas de um spec,
são o mais próximo de um portão declarativo entre os quatro runtimes desta seção.

## Zed — lê o arquivo de todo mundo

Verificado no fonte (`RULES_FILE_NAMES` em `crates/prompt_store/src/prompts.rs`),
primeiro que casar vence:

```
.rules · .cursorrules · .windsurfrules · .clinerules
.github/copilot-instructions.md · AGENT.md · AGENTS.md · CLAUDE.md · GEMINI.md
```

`agent.tool_permissions` faz permissão **por regex, por ferramenta**, com `always_allow` /
`always_deny` / `always_confirm`. Sem sandbox, sem hooks.

**Negativo genuíno:** busca exaustiva não achou **nenhuma** coleção de configuração
específica de Zed. Coerente com ele ler o formato dos outros — não há formato próprio
para colecionar.

## Convergência para Skills

Dois runtimes migraram no mesmo sentido em 2026:

- **Cursor 2.4+** está movendo regras e comandos para Skills, com um `/migrate-to-skills` embutido
- **Zed v1.4.0** substituiu regras sob demanda e a Rules Library por Skills; regras
  sempre-ativas viraram "Instructions"

Skills como unidade de empacotamento parece estar virando o padrão de fato — e é onde
este harness já está.

## Outros

- **Roo Code** ⛔ arquivado 2026-05-15; sucessor [Zoo-Code](https://github.com/Zoo-Code-Org/Zoo-Code)
- **Windsurf** absorvido pelo Devin; `docs.windsurf.com` redireciona (307). `.devin/rules/`
  tem precedência sobre `.windsurf/rules/`. Limite de **12.000 caracteres** por arquivo de regra
- **Continue.dev** — hooks **compatíveis com Claude Code**, 17 eventos, mas **totalmente
  não documentados**; `hub.continue.dev` não resolve mais

**Coleções para Kiro:** [mikeartee/kiro-steering-docs](https://github.com/mikeartee/kiro-steering-docs)
(29★, MIT) é o melhor corpus de steering apesar das poucas estrelas ·
[aws-samples/sample-kiro-steering-studio](https://github.com/aws-samples/sample-kiro-steering-studio)
(6★, MIT-0) é amostra oficial da AWS.

**Coleções para Cursor, com uma ressalva de formato:**
[PatrickJS/awesome-cursorrules](https://github.com/PatrickJS/awesome-cursorrules) tem 257
diretórios, mas no **formato legado `.cursorrules`**;
[sanjeed5/awesome-cursor-rules-mdc](https://github.com/sanjeed5/awesome-cursor-rules-mdc)
(3.571★) tem 243 arquivos no formato **`.mdc` atual** — é a citação correta para o formato moderno.

---

# 7. Sincronização entre runtimes

| Projeto | ★ | O que faz |
|---|---|---|
| [intellectronica/ruler](https://github.com/intellectronica/ruler) | 2.910 | `.ruler/*.md` como fonte única. **34 agentes mapeados com path exato** — o melhor mapa de portabilidade que existe |
| [dyoshikawa/rulesync](https://github.com/dyoshikawa/rulesync) | 1.387 | **41 ferramentas × 9 features** numa matriz. Tem `convert --from cursor --to copilot` |
| [caliber-ai-org/ai-setup](https://github.com/caliber-ai-org/ai-setup) | 1.262 | Sincroniza skills/MCP entre Claude Code, Cursor e Codex |
| [agent-sh/agnix](https://github.com/agent-sh/agnix) | 404 | **Linter + LSP para configuração de agente**: 455 regras, autofix, GitHub Action |
| [athola/skrills](https://github.com/athola/skrills) | 69 | **Rust** — valida e sincroniza entre Codex, Copilot e Claude Code |

**[agentsmd/agents.md](https://github.com/agentsmd/agents.md)** (24.154★) — o padrão AGENTS.md,
sob a Agentic AI Foundation da Linux Foundation, 60k+ projetos. **Não existe spec formal
nem schema**: é Markdown livre, e a única regra real é precedência por proximidade na
árvore. Portátil justamente porque não declara nada — hooks, permissões e verificação
ficam fora dele por construção.

---

# 8. Pesquisa — o harness como objeto de estudo

## Os canônicos

| Projeto | ★ | Paper | Contribuição |
|---|---|---|---|
| [SWE-agent](https://github.com/SWE-agent/SWE-agent) | 20.240 | [2405.15793](https://arxiv.org/abs/2405.15793) | A ACI. **18,0% com interface vs 11,0% shell puro**, mesmo modelo |
| [mini-swe-agent](https://github.com/SWE-agent/mini-swe-agent) | 6.970 | — | ~100 linhas, só bash. **>74% no SWE-bench Verified**. É o *controle experimental* da literatura |
| [Agentless](https://github.com/OpenAutoCoder/Agentless) | 2.109 ⚠️ (push 2024-12-22) | [2407.01489](https://arxiv.org/abs/2407.01489) | Pipeline fixo. **32,00% a US$0,70/instância** |
| [OpenHands](https://github.com/OpenHands/OpenHands) | 86.274 | — | Runtime e sandbox |
| [SWE-ReX](https://github.com/SWE-agent/SWE-ReX) | 583 | — | Separa o runtime de execução do loop. Subestimado |

## Estudos que MEDEM o efeito do harness

- **[2602.14690](https://arxiv.org/abs/2602.14690)** — *Harness Engineering for Agentic AI
  Coding Tools*, estudo empírico de **2.853 repositórios**. Achado: arquivos de contexto
  dominam e frequentemente são o único mecanismo; Skills e Subagents têm adoção limitada.
  **É a justificativa empírica pronta para um harness declarativo.**
- **[2607.22585](https://arxiv.org/abs/2607.22585)** — *The Scaffold Effect*: 2 modelos × 3
  harnesses × 50 tarefas. **Até 40× de diferença em tokens por tarefa**, com pass-rate
  variando só 0–8 p.p. Conclusão metodológica: reporte **tokens**, não só taxa de sucesso.
- **[2608.26218](https://arxiv.org/abs/2608.26218)** — *Same Model, Different Harness*: só
  truncar resultados de ferramenta antigos levou **28% → 49%**. Uma decisão de gestão de
  contexto, mesmo modelo.
- **[2609.00006](https://arxiv.org/abs/2609.00006)** — anatomia de **11 sistemas de produção**:
  7 subsistemas, 29 padrões, e um scaffold mínimo viável de 90 linhas. Achado forte:
  *nenhum* usa framework agêntico genérico nem embeddings para busca de código.
- **[2605.13357](https://arxiv.org/abs/2605.13357)** — propõe a escada **H0–H3** que torna a
  contribuição do harness empiricamente separável da do modelo.
- **[2604.13346](https://arxiv.org/abs/2604.13346)** + [ScaleML/AgentSPEX](https://github.com/ScaleML/AgentSPEX)
  (95★) — DSL declarativa em YAML para pipelines, contra "workflow acoplado a código".
  **O trabalho mais próximo da tese deste harness.**
- **[2503.18666](https://arxiv.org/abs/2503.18666)** — AgentSpec (ICSE 2026): DSL de
  enforcement em runtime, `triggers/predicates/enforcement`. >90% de ações inseguras
  bloqueadas. É nosso sistema de hooks formalizado e revisado por pares.

## Harness auto-evolutivo

- **[china-qijizhifeng/agentic-harness-engineering](https://github.com/china-qijizhifeng/agentic-harness-engineering)**
  (868★) — [2604.25850](https://arxiv.org/abs/2604.25850). Trata o harness como **arquivos
  versionáveis** e destila trajetórias em corpus de evidência. Terminal-Bench 2: 69,7 → **77,0%**.
- **[live-swe-agent](https://github.com/OpenAutoCoder/live-swe-agent)** (456★) — evolui o
  próprio scaffold em runtime. **75,4%** Verified.

## Surveys

[ai-boost/awesome-harness-engineering](https://github.com/ai-boost/awesome-harness-engineering) (4.015★) ·
[lopopolo/harness-engineering](https://github.com/lopopolo/harness-engineering) (2.669★, CC-BY-4.0) ·
[Gloriaameng/Awesome-Agent-Harness](https://github.com/Gloriaameng/Awesome-Agent-Harness) (348★, modelo H=(E,T,C,S,L,V)) ·
[RUCAIBox/awesome-agent-harness](https://github.com/RUCAIBox/awesome-agent-harness) (191★, 500+ papers)

**Retirado — não citar:** [2604.05013](https://arxiv.org/abs/2604.05013) *"Scaling Coding
Agents via Atomic Skills"* foi retirado pelo autor por erros nos dados.

---

# 9. O que a varredura NÃO encontrou

Estes negativos foram confirmados por **dois agentes independentes**:

**Nenhum projeto tem um manifesto declarativo equivalente ao `validation.json`** — um
arquivo que declara *boundaries por glob de caminho*, com `workingDirectory`, comandos
rápidos vs. completos, `prerequisites` de binário, filtro ao arquivo tocado e política
para caminho não mapeado.

Os mais próximos, e onde param:
- **claudekit** — tem `targetPatterns` e separação changed/project; **não tem** boundary nem prerequisites
- **Goose** — tem `retry.checks[]` executado sem hook; é **por recipe**, não por boundary
- **Aider** — tem `lint-cmd`/`test-cmd`; é **mapa global por linguagem**
- **interlinked-cli** — tem checks e baseline de evidência; **não tem** boundary de monorepo

Também não encontrado:
- Harness que combine **Windows como plataforma primária + monorepo Java/TS + portão por boundary**
- **Padrão cross-runtime para hooks, permissões ou verificação.** O rulesync tenta, mas é
  normalização de uma ferramenta, não padrão acordado — e sua coluna `checks` é *code
  review*, não verificação executável
- **Spec formal do AGENTS.md** — não existe
- Coleção substancial de configs para Windsurf, Zed ou Continue.dev

---

# 10. Lacunas deste harness que a varredura expõe

Convergentes entre as varreduras:

1. **Usamos 2 de ~9 eventos de hook.** Sem `PreToolUse` não há guarda *antes* da ação.
2. **A deny list é driblável por comando composto.** `liberzon/claude-hooks` decompõe;
   `claudekit` parseia; Factory Droid resolve o programa realmente invocado.
3. **O portão não tem *self-baseline*.** Um repositório já vermelho acusa o agente
   injustamente — `tsetse012/claude-verify-gate` resolve isso.
4. **Nada sobrevive à compactação de contexto.** `sd0x-harness` e `meridian` tratam
   explicitamente.
5. **Sem limite no contexto que o hook devolve.** Codex tem `additionalContextLimit`; um
   `mvn test` verboso vai inteiro para a janela.
6. **Sem semântica de falha declarada.** Cursor tem `failClosed` por hook; Cline tem
   `fail_closed` + `failureMode` + `retries`.
7. **Sem lint da própria configuração** — `agnix` tem 455 regras para isso.
8. **Dependência de caminho absoluto do interpretador Python** no `settings.json`.
   `moai-adk` resolve com binário único em Go; Codex com `commandWindows`; Copilot com
   `bash`+`powershell` no mesmo objeto.
9. **[C] Sem isolamento — e o Claude Code tem um, nativo.** Correção: o bloco `sandbox` do
   nosso próprio `settings.json` faz rede por allowlist, `denyRead` de credencial e
   `allowUnsandboxedCommands: false`, imposto pelo SO em todo processo filho — mas **não roda
   em Windows nativo, só WSL2**. Detalhes em `analise-repositorios.md` §1. O Cursor tem `sandbox.json` com
   `networkPolicy` **negando rede por padrão** e caminhos de configuração protegidos contra
   escrita. Nossa camada de permissão é política declarativa; a dele é isolamento. Isso
   confirma a correção registrada em `decisoes-do-harness.md` §8: marcar Permission como
   "parcial" estava certo, e agora há referência concreta do que seria "completo".
10. **Permissão por string de comando, não por capacidade.** O modelo do Kiro
    (`capability: fs_write` + `match`/`exclude` + resolução deny-overrides) é
    estruturalmente melhor: não depende de acertar a grafia do comando, e o mais
    restritivo sempre vence sem regra de precedência de escopo.

---

# 11. Leitura recomendada, na ordem

1. **[carlrannaberg/claudekit](https://github.com/carlrannaberg/claudekit)** — o mesmo
   design, mais maduro. Ler `cli/hooks/registry.ts`, `runner.ts`, `file-guard/bash-command-parser.ts`
2. **[okdt/claude-code-hardening-cheatsheet](https://github.com/okdt/claude-code-hardening-cheatsheet)** —
   arquivo único, comparação direta com nossa deny list, 10 minutos
3. **[nizos/tdd-guard](https://github.com/nizos/tdd-guard)** — `reporters/junit5/` substitui
   parse de stdout do Maven por leitura do XML
4. **[2602.14690](https://arxiv.org/abs/2602.14690)** — a justificativa empírica de 2.853 repos
5. **[2607.22585](https://arxiv.org/abs/2607.22585)** — se for comparar harnesses, reporte
   tokens por tarefa resolvida, não só taxa de sucesso

> **Deprecated em 2026-09-06.** Executado por inteiro nesse dia (traço no runner, política da
> auto memory, promoção dos fatos). Duas afirmações daqui caíram: o store da auto memory é por
> repositório git, não por diretório; e a recomendação de desligá-la não foi adotada — ficou
> ligada, com política. Estado atual em `decisoes-do-harness.md` (§3, §8, §9) e no README do
> harness. Vale como registro, não como guia.

# Memória no harness — briefing para análise

**Público deste documento:** um agente de código sem nenhum contexto prévio deste
projeto. Tudo que é necessário está aqui.

**O que se espera como saída:** um plano de implementação. Não implemente a partir
deste documento. Se discordar de algo aqui, diga qual item e por quê — várias
afirmações abaixo são recomendações, não fatos, e estão marcadas como tal.

**Convenção de marcação usada em todo o documento:**

- **[FATO]** — verificado por leitura direta de arquivo ou medição. Confiável.
- **[INFERÊNCIA]** — conclusão tirada de fatos. Pode estar errada; o raciocínio está
  explícito para ser contestado.
- **[RECOMENDAÇÃO]** — opinião de projeto. Discuta.
- **[DECISÃO PENDENTE]** — depende do Lucas. Não resolva por conta própria.

**Escopo:** apenas memória. Existem outros problemas conhecidos no harness
(portabilidade dos hooks, cap de regras estourado, boundary sem check rápido) que
estão **fora deste documento de propósito**. Não os inclua no plano.

---

# 1. O que é o harness

**[FATO]** Fica em `J:\TCC\harness`. É um projeto de TCC. Não é um framework nem uma
biblioteca: é configuração que um agente de código (Claude Code) lê ao abrir a pasta.
O software do usuário mora **dentro** dela, em `apps/`, para que a raiz do harness
seja o diretório de trabalho.

**[FATO]** Estrutura relevante:

```
J:\TCC\harness\
├── CLAUDE.md                     regras always-on (13 parágrafos, cap declarado de 10)
├── README.md                     documenta a doutrina e o que falta
├── .claude/
│   ├── settings.json             permissões (deny/ask) + 2 hooks
│   ├── validation.json           manifesto de boundaries → comandos do projeto
│   ├── validation.example.json   documentação de todos os campos
│   └── skills/
│       ├── api-change/           SKILL.md
│       ├── commit/               SKILL.md
│       ├── duplication-check/    SKILL.md
│       ├── error-handling/       SKILL.md
│       └── testing/              SKILL.md
├── verify/
│   ├── runner.py                 11.484 bytes — lê o manifesto e roda os comandos
│   ├── test_runner.py            7.074 bytes — testes do runner
│   └── .state.json               47 bytes — estado de coordenação entre os hooks
└── apps/
    ├── backend/                  Java/Maven
    ├── frontend/                 Node
    ├── dados/                    sem boundary declarado
    └── docs/                     sem boundary declarado
```

**[FATO]** `.claude/settings.json` registra dois hooks, ambos chamando `verify/runner.py`:

| Evento | Argumento | Timeout | Quando |
|---|---|---|---|
| `PostToolUse` (matcher `Edit\|Write\|NotebookEdit`) | `--hook` | 120 s | após toda edição de arquivo |
| `Stop` | `--gate` | 600 s | antes de aceitar que o turno terminou |

**[FATO]** `.claude/validation.json` declara três boundaries:

| id | paths | workingDirectory | fast | commands |
|---|---|---|---|---|
| `backend` | `apps/backend/src/**` | `apps/backend` | `npx --yes jscpd src/main --min-tokens 100 --reporters ai` (`reportOnly: true`) | `mvn -B -q test` |
| `frontend` | `apps/frontend/src/**` | `apps/frontend` | `npm run typecheck` | `npm run build` |
| `harness` | `*.md`, `.claude/**`, `verify/**` | `.` | — | `[]` (nenhum) |

Com `"unmappedPathPolicy": "report"`.

**[FATO]** Constantes e tipos já definidos no topo de `verify/runner.py`, que o plano
deve reusar em vez de redefinir:

```python
HARNESS_ROOT = Path(__file__).resolve().parent.parent
MANIFEST     = HARNESS_ROOT / ".claude" / "validation.json"
STATE        = HARNESS_ROOT / "verify" / ".state.json"

Phase   = Literal["fast", "full"]
Outcome = Literal["UNMAPPED", "NO_CHECKS", "BLOCKED", "PASS", "FAIL", "NOTE"]

MAX_OUTPUT_LINES      = 40
COMMAND_TIMEOUT_S     = 600
MAX_CONSECUTIVE_BLOCKS = 4
```

**[FATO]** O docstring de `runner.py` declara uma restrição de projeto que o plano
deve respeitar: *"Pure module: no SDK dependency, no network. The hooks in
.claude/settings.json call it; tests call verify() directly."*

---

# 2. A doutrina do harness

Isto governa toda decisão abaixo. **[FATO]**, extraído do `README.md` e do `CLAUDE.md`.

## 2.1 Três camadas, organizadas por quem aplica

| Camada | Quem aplica | Confiabilidade |
|---|---|---|
| **Permissões** | a plataforma, fora do modelo | determinística — vale em toda chamada |
| **Verificação** | `verify/runner.py`, chamado pelos dois hooks | determinística por edição; o portão de conclusão falha aberto |
| **Guia** | o modelo, lendo | probabilística |

**A regra de alocação:** uma prática só desce de camada quando a camada acima não
consegue expressá-la. O README diz textualmente: *"o que uma ferramenta consegue
checar não pertence à prosa — pertence ao linter do projeto, e o resultado volta como
fato em vez de lembrete."*

## 2.2 O critério de admissão do `CLAUDE.md`

Uma regra só entra se **contradiz um comportamento padrão do modelo**. Práticas que
um modelo capaz já segue sozinho estão deliberadamente ausentes: repeti-las custa
contexto e enfraquece as regras que ficam. Existe um cap declarado de 10 regras.

## 2.3 Prioridades de projeto, em ordem

1. custo de token
2. confiabilidade e determinismo
3. facilidade de manutenção
4. latência

**Consequência para memória:** um mecanismo que grava é barato; um mecanismo que
carrega conteúdo em contexto compete com a prioridade nº 1 e precisa se justificar.

## 2.4 Modo de uso

**[FATO]** O harness é para uso interativo, com humano no loop. Não é para agente
autônomo rodando horas sem supervisão. Mecanismos cujo valor depende de autonomia
longa estão fora de escopo.

---

# 3. Restrições da plataforma

**[FATO]**, verificado na documentação oficial do Claude Code. Um plano que ignore
qualquer item desta seção é inviável.

1. **A compactação de contexto não é substituível.** Existem hooks `PreCompact` e
   `PostCompact` para observar, e `autoCompactWindow` para ajustar o limiar. Não há
   ponto de extensão para trocar o algoritmo nem para forçar descarte do histórico a
   cada N chamadas de ferramenta.
2. **A memória automática da plataforma** ("auto memory") grava em
   `~/.claude/projects/<cwd-codificado>/memory/`, com um `MEMORY.md` de índice
   (carregado até 200 linhas ou 25 KB) mais arquivos por tema carregados sob demanda.
   Liga e desliga por `autoMemoryEnabled` em `settings.json`, ou pela variável de
   ambiente `CLAUDE_CODE_DISABLE_AUTO_MEMORY=1`.
3. **O store de auto memory é indexado pelo diretório de trabalho da sessão.**
   Diretórios diferentes produzem stores diferentes que não se enxergam.
4. **Precedência de settings**, da mais forte para a mais fraca: managed (organização)
   → CLI → `.claude/settings.local.json` → `.claude/settings.json` →
   `~/.claude/settings.json`.
5. **Subagents** (`.claude/agents/*.md`) aceitam `tools:` no frontmatter como
   **allowlist determinística** — o cliente bloqueia antes de o modelo tentar.
6. **`.claude/rules/*.md`** aceita `paths:` no frontmatter para carregamento
   condicional por glob.
7. Existem ~25 eventos de hook, entre eles `SessionStart`, `SessionEnd`,
   `PreToolUse`, `PostToolUse`, `PostToolUseFailure`, `Stop`, `SubagentStop`,
   `PreCompact`, `PostCompact`.

---

# 4. Vocabulário: o que "memória" significa aqui

**[FATO]** Definições da §3.1 do survey *Externalization in LLM Agents*
(arXiv:2604.08224). Use estes quatro termos com estes sentidos no plano.

| Dimensão | Definição | Exemplo neste projeto |
|---|---|---|
| **Working context** | estado vivo da tarefa em andamento: arquivos abertos, planos parciais, checkpoints. Perde valor rápido; some quando o contexto reseta | — |
| **Episodic experience** | o que aconteceu em execuções passadas: chamadas, falhas, resultados, duração | o que o runner produz e descarta |
| **Semantic knowledge** | abstrações que sobrevivem a qualquer episódio: convenções do projeto, heurísticas, fatos estáveis | `CLAUDE.md`, as 5 skills |
| **Personalized** | informação estável sobre o usuário: preferências, hábitos, restrições recorrentes | preferência por linguagens tipadas |

**[FATO]** O survey (§3.1) é explícito sobre não misturar a quarta com as outras:

> "[Memória personalizada] não deve ser colapsada no store geral de automelhoria do
> agente, porque traços específicos do usuário obedecem a regras diferentes de
> retenção, recuperação e privacidade."

**[FATO]** O survey também separa memória de skill (§3.1, final):

> "Regularidades procedurais repetidas podem aparecer primeiro como padrões em traços
> episódicos, mas deixam de ser memória propriamente dita quando o harness as promove
> a orientação reutilizável explícita. Nesse ponto pertencem à camada de skill, não à
> de memória."

Ou seja: **memória é a base de evidência; skill é o procedimento destilado dela.** As
5 skills existentes são camada de skill e não devem ser contadas como memória.

---

# 5. Estado atual da memória no harness

**[INFERÊNCIA]** a partir dos fatos da seção 1, aplicando o vocabulário da seção 4:

| Dimensão | Estado | Quem escreve | Revisado? |
|---|---|---|---|
| Semantic | `CLAUDE.md` + 5 skills | o Lucas | sim |
| **Episodic** | **ausente** — o runner tem os dados e os descarta no `return` | ninguém | — |
| Working context | ausente | — | — |
| Personalized | auto memory da plataforma | o modelo | **não** |

**[FATO]** Três linhas já escritas no `README.md` do harness, antes desta análise,
descrevem o mesmo buraco de três ângulos:

- Skills: *"Missing the paper's third attribute: revision driven by observed failure"*
- Observability: *"**Absent.** `verify/.state.json` is coordination state, not a log:
  no history, no traces, no metrics"*
- Adiado — Session logging: *"O runner já sabe o boundary, comando, resultado e
  duração, e joga tudo fora. Adicionar um JSONL agora é uma função, não um mecanismo."*

**[FATO]** O survey (§7.1) explica por que são o mesmo buraco:

> **Skill → memória: execution recording.** "Toda execução de skill gera traços,
> falhas intermediárias e refinamentos que de outro modo desapareceriam com a janela
> de contexto ativa. Infraestrutura de observabilidade e log captura essas
> trajetórias como evidência durável, permitindo ao sistema validar quais skills
> permanecem confiáveis e quais devem ser revisadas, divididas ou restringidas."
>
> "Esse fluxo é o que torna a camada de skills autocorretiva em vez de meramente
> auto-expansiva. **Sem esse registro, o harness não tem base empírica para
> manutenção de skills**, e o caminho de destilação de memória para skill opera sobre
> evidência cada vez mais velha."

**[INFERÊNCIA]** Uma única implementação — o runner passar a gravar — fecha as três
linhas. É a Mudança 1.

---

# 6. Evidência medida: a auto memory da plataforma

**[FATO]** Medição feita em 2026-09-06 sobre uma cópia de
`C:\Users\Lucas\.claude\projects\`.

Existem **13 stores**. Três têm conteúdo; dez estão vazios.

| Store | Arquivos | Bytes |
|---|---|---|
| `J--TCC/memory` | 5 | 10.354 |
| `J--TCC-Nutri--o/memory` | 3 | 7.135 |
| `J--TCC-zapBotMessage/memory` | 3 | 6.424 |
| **`J--TCC-harness/memory`** | **0** | — (diretório criado em 30/08) |
| os outros 9 | 0 | — |

**[FATO]** O store do harness está vazio. O store que contém memória *sobre* o
harness é `J--TCC/`, o diretório pai, porque foi ali que a sessão foi aberta.

**[INFERÊNCIA]** O harness manda abrir `J:\TCC\harness`; o trabalho de fato acontece
ora ali, ora no diretório pai. Como o store é indexado pelo diretório de trabalho
(restrição 3 da seção 3), a memória se fragmenta em stores irmãos que não se
enxergam, e nenhum é autoritativo.

**[FATO] Custo de contexto:** o `MEMORY.md` do maior store tem **652 bytes**. Apenas o
índice carrega por padrão. Contra a prioridade nº 1, isso é desprezível. Qualquer
argumento contra a auto memory baseado em custo de contexto está errado e não deve
entrar no plano.

**[FATO] Formato dos arquivos:** frontmatter YAML com `name`, `description` e
`metadata` (contendo `node_type: memory`, `type: project` ou `type: user`,
`originSessionId`, `modified`), corpo em prosa com seções `**Why:**` e `**How to
apply:**`, e `[[wikilinks]]` entre arquivos. É um formato bem construído.

**[FATO]** O `type: project` / `type: user` distingue as dimensões da seção 4 no
metadado. Mas os arquivos dos dois tipos convivem no mesmo diretório, com a mesma
política de retenção.

**[INFERÊNCIA]** A taxonomia existe; a separação de escopo não. É exatamente o que a
§3.1 do survey adverte que não se deve fazer, observado num sistema em produção.

**[FATO] O conteúdo problemático.** O arquivo
`J--TCC/memory/harness-e-o-projeto-tcc-e-secundario.md`, modificado em 2026-09-05,
contém:

> "Em 2026-09-05 o Lucas definiu que o projeto é criar e usar um harness de agente de
> código — e disse, literalmente, 'foda-se o TCC'. O critério de sucesso é o harness
> funcionar (...), não a defensabilidade da banca. (...) Crítica de originalidade,
> consistência de rótulos E/D/P, ameaças à validade e ordem evals-antes-do-revisor
> são irrelevantes."
>
> "**How to apply:** ao revisar a spec ou planejar, ordene por 'isso me morde
> construindo ou usando?' e não por 'a banca pergunta isso?'."

**[FATO]** Em 2026-09-06, um dia depois, o Lucas trabalhou várias horas exatamente
sobre defensabilidade acadêmica: o que diferencia o projeto de trabalhos
relacionados, como declarar exclusões com justificativa, como tornar o cap de regras
verificável para a banca.

**[INFERÊNCIA]** O registro está desatualizado e contradiz o comportamento atual.
Qualquer sessão futura aberta em `J:\TCC` vai ler essa diretiva e desprezar
precisamente o trabalho que ele está pedindo.

**[INFERÊNCIA] — e este é o achado analítico principal:** o arquivo não guarda um
**fato**, guarda uma **instrução permanente**. Um fato desatualizado é inofensivo:
envelhece e alguém corrige. Uma diretiva não tem prazo de validade — governa o
comportamento até ser removida à mão, e não existe nada no mecanismo que a expire.
O survey nomeia a cascata (§7.1, *system-level dynamics*):

> "Uma entrada de memória envenenada pode levar a uma skill defeituosa, cujos traços
> de execução contaminam ainda mais a memória — uma cascata que o controle de
> qualidade de nenhum módulo isolado consegue interromper sem intervenção em nível de
> harness."

---

# 7. Mudança 1 — o runner grava traço episódico

**A única mudança deste documento que envolve código.**

## 7.1 Justificativa

**Pelos artigos.** Survey §7.1: é o *execution recording*, sem o qual a camada de
skills cresce mas não se corrige. Survey §6.2.4: observabilidade é "o mecanismo pelo
qual o harness aprende com a própria operação". InfiAgent §3.2: o que a execução
produz vira artefato persistente em vez de histórico de prompt.

**Pelo harness.** Os dados já existem dentro do runner e são descartados no retorno.
O próprio README declara isso como adiado, com o sinal *"adicionar um JSONL agora é
uma função, não um mecanismo"*.

**Pela doutrina.** Camada 2, determinística: escrito pelo runner, não pelo modelo.
Essa é a propriedade decisiva — seria o único componente de memória do harness cujo
conteúdo não depende de o modelo decidir registrar.

## 7.2 Especificação

**Arquivo:** `verify/.trace.jsonl`. Prefixo com ponto, ao lado de `.state.json`, que
já segue essa convenção. Deve entrar no `.gitignore`: é dado de execução por máquina.

**Formato:** JSON Lines. Um objeto JSON por linha, UTF-8, terminado em `\n`. Sem
array externo, sem vírgulas entre linhas.

**Quando gravar:** um registro por **tentativa de execução de comando**. Desfechos que
não executam comando (`UNMAPPED`, `NO_CHECKS`) também geram registro, com
`command: null`.

**Campos** — todos obrigatórios salvo indicação:

| Campo | Tipo | Conteúdo |
|---|---|---|
| `schema` | string | versão do formato, ex. `"harness.trace.v1"`. Permite ao leitor evoluir sem quebrar |
| `ts` | string | timestamp ISO-8601 com timezone |
| `boundary` | string \| null | id do boundary; `null` quando `UNMAPPED` |
| `phase` | string \| null | o `Phase` já existente: `"fast"` ou `"full"` |
| `command` | string \| null | o comando como declarado no manifesto |
| `outcome` | string | o `Outcome` já existente, sem valores novos |
| `duration_s` | number \| null | duração medida; `null` se não executou |
| `exit_code` | number \| null | código de saída; `null` se não executou |
| `touched` | array de string | caminhos que dispararam esta verificação |
| `output_head` | string (opcional) | **somente em `FAIL`**, respeitando `MAX_OUTPUT_LINES` |

**Escrita:** modo append, com `flush` por registro, para que uma interrupção não
comprometa o histórico anterior. Apenas biblioteca padrão (`json`, `pathlib`,
`datetime`) — o docstring do módulo proíbe dependência externa e rede.

**Teto de tamanho:** declare uma constante no topo do módulo, ao lado das existentes,
com um número máximo de registros. Ao ultrapassar, descarte a partir do mais antigo.
**Nunca descarte o registro que está sendo gravado.** Crescimento ilimitado dentro da
pasta do usuário é defeito, não recurso.

**Falha ao gravar não pode falhar a verificação.** Se a escrita do traço levantar
exceção, o resultado da verificação não muda. Mas atenção à regra do `CLAUDE.md`
*"Never swallow an error silently"*: capture a exceção, não a descarte — emita um
`NOTE` com a causa. Memória é best-effort; silêncio não é.

## 7.3 Testes a acrescentar em `verify/test_runner.py`

1. Um registro é gravado por evento de verificação, com os campos e tipos acima.
2. O arquivo permanece JSONL válido depois de múltiplas execuções (toda linha parseia
   isoladamente).
3. `FAIL` inclui `output_head`; os outros desfechos, não.
4. `UNMAPPED` e `NO_CHECKS` gravam com `command: null`.
5. O teto descarta do mais antigo e preserva o registro recém-escrito.
6. Uma falha de escrita do traço não altera o resultado retornado por `verify()`.

## 7.4 Não faça

- **Não injete o traço em contexto.** Nenhum hook, skill, regra ou arquivo carrega
  `.trace.jsonl` para o modelo. Esta mudança escreve e não lê.
- **Não mude a assinatura de `verify()`** nem o comportamento dos dois hooks.
- **Não acrescente valores novos a `Outcome` nem a `Phase`.**
- **Não adicione dependência externa.**
- **Não escreva campos além dos listados.** Se achar que falta um, pare e diga qual e
  por quê, em vez de acrescentar.

**[FATO]** A justificativa do "não injete" está no survey §7.2:

> "Em vez de inundar o modelo com um log de execução completo, mecanismos de
> recuperação selecionam uma fatia pequena do estado (...). Isso transforma
> continuidade de longo horizonte num problema de contextualização direcionada e
> reduz desperdício de contexto."

Escrever é barato. Carregar custa em toda sessão e compete com a prioridade nº 1.

---

# 8. Mudança 2 — o leitor do traço

**Não implemente.** Registre como adiado.

**[FATO]** O survey §3.4 diz que armazenamento sem recuperação não é memória:
*"O critério de sucesso da memória não é 'quanto salvamos?' mas 'tornamos a decisão
atual legível?'"* Isso argumenta a favor de um leitor.

**[FATO]** Mas o `README.md` do harness estabelece: *"Adicione [uma skill] quando você
pega o agente errando um assunto repetidamente — não porque você prevê que ele pode
errar."*

**[INFERÊNCIA]** Não existe traço ainda. Um leitor escrito agora é construído contra
dados imaginados, o que viola tanto a regra acima quanto a regra de design do
`CLAUDE.md` (*não crie ponto de extensão antes de existir variação real*).

**[RECOMENDAÇÃO]** Acrescente à tabela de adiados do README:

> **Leitura do traço** — quando você quiser saber se uma falha já aconteceu antes e
> perceber que não consegue responder sem abrir o arquivo na mão.

**[INFERÊNCIA]** Quando o sinal disparar, a forma provável é um relatório **para o
humano**, não contexto para o agente: qual boundary falha mais, qual comando domina o
tempo, qual falha reincide. O consumidor da revisão de skills que o survey descreve é
uma pessoa decidindo se uma skill precisa mudar.

---

# 9. Mudança 3 — a auto memory da plataforma

## 9.1 O problema, em uma frase

**[INFERÊNCIA]** Depois da Mudança 1, o harness terá dois stores da mesma dimensão
com confiabilidades diferentes e nenhuma regra de arbitragem: um determinístico,
versionado e testado, escrito pelo runner; outro probabilístico, sem esquema, escrito
pelo modelo, num store cujo escopo não corresponde ao do projeto (seção 6).

## 9.2 O que a evidência sustenta e o que não sustenta

**Não sustenta:** o argumento de custo de contexto. 652 bytes. Descarte-o.

**Sustenta:** que o mecanismo grava **instruções permanentes sem revisão nem
expiração**, e que uma delas já está errada e ativa (seção 6).

**Sustenta:** que a fragmentação por diretório de trabalho torna o store não
autoritativo — 13 stores, o do harness vazio, o útil pendurado no diretório pai.

**Sustenta, em favor dela:** o formato é bom, e parte do conteúdo é genuinamente útil.

## 9.3 [RECOMENDAÇÃO]

`"autoMemoryEnabled": false` em `.claude/settings.json` do harness, com
`.claude/settings.local.json` documentado no README como opt-in de quem quiser usar
na própria máquina. Pela precedência (restrição 4 da seção 3), o arquivo local vence
o compartilhado, e o arquivo compartilhado é o que viaja com o artefato.

Fora do harness a auto memory continua no padrão da plataforma, então o uso cotidiano
em outros projetos não muda.

**Não é** uma proposta de mover, copiar ou importar o store da plataforma para dentro
do harness. Nada se move.

## 9.4 Ação imediata, independente da decisão

**[RECOMENDAÇÃO]** Corrigir ou apagar
`C:\Users\Lucas\.claude\projects\J--TCC\memory\harness-e-o-projeto-tcc-e-secundario.md`.
Ele contradiz as prioridades atuais e orienta toda sessão aberta em `J:\TCC` enquanto
existir. Vale mesmo — talvez principalmente — se a decisão for manter a auto memory
ligada.

Isso é edição manual de um arquivo fora do repositório do harness. Não faz parte do
plano de implementação; é uma tarefa do Lucas.

---

# 10. Mudança 4 — promoção manual de dois fatos

**[RECOMENDAÇÃO]** Dois fatos hoje existem apenas em notas não revisadas da auto
memory e merecem virar conhecimento semântico revisado. "Promover" significa
**escrever à mão duas frases em arquivos que já existem**. Nada é copiado ou
importado; o store permanece intocado.

Destinos diferentes, e a diferença é o ponto:

**a) A rota arquitetural → `README.md` do harness.** A nota registra que a rota
escolhida é a nativa: o harness é configuração, hooks e ferramentas próprias sobre o
loop do SDK, e não um loop próprio sobre a API de mensagens. É um fato sobre o que o
harness **é**, e o README hoje abre com *"there is nothing to run"*, redação anterior
a essa decisão e hoje inconsistente com a existência de `verify/runner.py`.

**[DECISÃO PENDENTE]** Confirmar com o Lucas que essa continua sendo a rota. A nota é
de 05/09 e vem do mesmo arquivo que está comprovadamente desatualizado em outro
ponto (seção 6). Não trate como verdade sem confirmação.

**b) A preferência por linguagens fortemente tipadas → `~/.claude/CLAUDE.md`, escopo
de usuário. Não vai para o harness.** É preferência pessoal, e o harness é um
artefato portátil destinado a ser usado por outras pessoas. Colocá-la ali contamina o
artefato com algo que só vale para um indivíduo — precisamente o que a §3.1 do survey
adverte sobre não colapsar memória personalizada em store de projeto.

**[INFERÊNCIA]** Esses dois destinos diferentes são a distinção que a auto memory não
faz: ela guarda os dois no mesmo diretório com a mesma política.

---

# 11. Mudança 5 — não contar a mesma coisa duas vezes no README

**[FATO]** O `README.md` tem uma tabela mapeando o harness contra as seis dimensões
de harness do survey (§6.2). A linha Memory hoje diz: *"Semantic (`CLAUDE.md`) and
personalised (platform auto-memory). No episodic record, no working context."*

**[FATO]** O survey trata configuração e política como **dimensão de harness**
(§6.2.5), não como memória.

**[INFERÊNCIA]** `validation.json` contém conhecimento factual sobre o projeto —
quais comandos verificam qual fronteira — o que se encaixa na definição de semantic
knowledge da §3.1. Mas sua forma é configuração declarativa. Reivindicá-lo nas duas
dimensões infla a tabela.

**[RECOMENDAÇÃO]** Trate `validation.json` como configuração. A linha semântica fica
com `CLAUDE.md` (regras) e as skills (procedimentos), o que já é cobertura forte.

**[RECOMENDAÇÃO]** A linha Memory reescrita deve refletir, depois das mudanças:
episódico presente e determinístico (o traço); semântico presente e revisado;
working context ausente com justificativa; personalizado conforme a decisão de 9.3.

---

# 12. O que não implementar, e por quê

**Reconstrução limitada de contexto — a janela fixa de ações do InfiAgent.**

**[FATO]** O InfiAgent (§3.2–3.3) formaliza estado persistente como $S_t =
\mathcal{F}_t$ — os arquivos são o registro autoritativo — e contexto como $c_t =
g(\mathcal{F}_t, a_{t-k:t-1})$, onde $k$ é uma constante pequena (o artigo usa 10).
A cada passo o agente descarta o histórico e reconstrói o contexto a partir do estado
dos arquivos mais uma janela fixa de ações recentes.

**[INFERÊNCIA]** A metade $S_t = \mathcal{F}_t$ é implementável e é o que a Mudança 1
faz. A metade $g$ não é: a compactação do Claude Code é fechada (restrição 1 da seção
3). E, com humano no loop (seção 2.4), o problema que a janela fixa resolve não é o
deste harness.

**[RECOMENDAÇÃO]** Declarar isso no README como adoção parcial com motivo. É uma
afirmação mais forte do que implementação incompleta.

**Working context.** Mesma razão. A linha fica vazia com justificativa. O próprio
survey diz ser *"um arcabouço analítico para comparar arquiteturas de harness, e não
uma lista de verificação de implementação"* — não há obrigação de preencher todas as
linhas.

**Recuperação estruturada: RAG, grafo de conhecimento, SQLite com FTS, embeddings.**

**[FATO]** A §3.2 do survey é uma progressão de quatro estágios: contexto monolítico →
contexto com store de recuperação → memória hierárquica com orquestração → sistemas
adaptativos. Cada estágio é **resposta a um limite observado** no anterior.

**[INFERÊNCIA]** JSONL com leitura direta é o primeiro estágio, e é onde o projeto
está. Subir de estágio sem evidência do limite é o que o próprio `CLAUDE.md` proíbe:
*"não crie hierarquia, interface ou ponto de extensão antes de existir variação
real"*.

**Memória episódica escrita pelo modelo.** Depois da Mudança 1 já existirá uma,
determinística. Duas é o defeito descrito em 9.1, não a solução dele.

**Qualquer coisa fora de memória.** Ver o escopo no topo deste documento.

---

# 13. Decisões pendentes do Lucas

Não resolva nenhuma delas no plano. Liste-as como bloqueios com a pergunta exata.

1. **Auto memory desligada no harness?** Recomendação em 9.3. É dele a palavra final.
2. **A rota arquitetural da seção 10a continua válida?** A fonte é uma nota
   comprovadamente desatualizada em outro ponto.
3. **Teto de registros do traço** (seção 7.2): qual número. Depende de quantas
   verificações uma sessão típica dele gera, que ninguém mediu ainda. Sugestão de
   encaminhamento: implementar a constante com um valor inicial declarado e revisá-la
   depois da primeira semana de traço real.

---

# 14. Ordem de execução recomendada

| # | Mudança | Tipo | Depende de |
|---|---|---|---|
| 1 | Traço episódico no runner (seção 7) | código + testes | decisão 3 |
| 2 | Decidir auto memory (seção 9.3) e corrigir a nota errada (9.4) | configuração + edição manual | decisão 1 |
| 3 | Promoção manual dos dois fatos (seção 10) | texto | decisão 2 |
| 4 | Correção da tabela do README (seção 11) | texto | mudanças 1 e 2 |
| 5 | Registrar o leitor como adiado (seção 8) | texto | — |

**[RECOMENDAÇÃO]** Uma por sessão. A Mudança 1 altera `verify/runner.py`, que é o
componente que valida as outras — se ela quebrar, ela quebra em silêncio, porque
falha de hook não gera erro visível para o agente.

---

# 15. Referências

- **InfiAgent: An Infinite-Horizon Framework for General-Purpose Autonomous Agents.**
  Yu, Wang, Wang, Yang, Li. arXiv:2601.03204, 06/01/2026. Seções relevantes: §3
  (formalização de estado persistente e contexto limitado), §4 (arquitetura).
- **Externalization in LLM Agents: A Unified Review of Memory, Skills, Protocols and
  Harness Engineering.** Zhou, Chai, Chen et al. arXiv:2604.08224, 09/04/2026. Seções
  relevantes: §3.1 (as quatro dimensões), §3.2 (as quatro arquiteturas), §3.3
  (demandas da era harness), §3.4 (memória como artefato cognitivo), §6.2.4
  (observabilidade), §6.2.5 (configuração e política), §6.2.6 (orçamento de
  contexto), §7.1 (os seis acoplamentos e as dinâmicas de sistema), §7.2 (memória
  como entrada contextual).

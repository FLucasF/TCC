# Decisões e ideias — sessão de projeto do harness

Registro do raciocínio por trás do `harness/`. O `README.md` de lá diz **o que** é;
este diz **por quê**, e o que foi descartado no caminho.

Distinção mantida em todo o texto: o que foi **medido** aparece com número; o que
foi **verificado** em documentação ou código aparece com a fonte; o resto é
raciocínio e está marcado como tal.

**Atualizado em 2026-09-06** com o que foi construído depois da sessão de projeto:
executor, traço episódico, política de memória, teto de regras como teste — **e com a
remoção do executor no mesmo dia, na §11.** As mudanças de estado estão marcadas com
a data; o raciocínio original ficou, inclusive onde o estado mudou depois.

> **Leia a §11 antes das seções 2, 3, 8 e 9.** Elas descrevem a camada de verificação
> no presente, e ela não existe mais. O raciocínio continua válido; a instanciação
> não. O código está no branch `executor-em-node`.

---

## 1. A virada

A especificação original definia harness como **interface de ações + seleção de
contexto + loop de controle + guardas**. Três dessas quatro pernas são *software*.
Definido assim, o documento não tinha como gerar outra coisa — todo o resto dele é
consequência fiel dessa frase da página 2.

A definição nova é subtrativa:

> **Harness é o conjunto portátil de práticas, verificações e permissões que mantém
> o trabalho do agente consistente — com o pedido, com o código que já existe, e com
> o que o resto do sistema espera.**

Ela gera um harness pequeno por construção, e dá critério para recusar coisa: se o
modelo já faz sozinho, não entra.

---

## 2. Os princípios que sobraram

### Texto é a alavanca mais fraca

Verificado nas fontes que a própria spec reúne: formatação de prompt, sem mudança de
conteúdo, produziu variação de até 76 pontos de acurácia; a adesão degrada conforme
as restrições se acumulam; `pass^8` fica abaixo de 25%.

Consequência: permissão e verificação **dispararam ou não**; texto é seguido *na
maioria das vezes*. Por isso a organização é **por quem garante**, não por assunto.

| Camada | Quem garante | Cobertura |
|---|---|---|
| Permissões | A plataforma | Toda chamada. Determinística |
| ~~Verificação~~ | ~~`verify/runner.mjs`, chamado por dois hooks~~ | **Removida em 06/09 — ver §11.** Ficaram duas camadas |
| Guia | O modelo, lendo | Probabilística |

Uma prática só desce de camada quando a de cima não consegue expressá-la.

### O que entra no guia

Duas condições, uma delas basta:

1. **Contraria o comportamento padrão do modelo**
2. **É específico deste projeto**

O resto é ruído: ocupa contexto e dilui as regras que ficam. Teto declarado de dez
regras no núcleo; a décima primeira precisa expulsar uma.

Em 06/09 o teto virou um teste, e voltou a ser intenção quando o `verify/` saiu:
regra é **um parágrafo com um imperativo independente** (uma unidade que sai sozinha
sem quebrar outra), e o `rules.test.mjs` contava os parágrafos abaixo dos títulos,
falhando acima de dez. Hoje a contagem é à mão — dez exatos, conferidos em 06/09.
O núcleo está em dez exatos — as duas regras de teste saíram para a skill `testing`,
que já as tinha.

**O caso SOLID.** O modelo já conhece SOLID; escrever "aplique SOLID" não muda nada.
O modo de falha observado é o oposto — ele aplica cedo demais, extraindo interface e
criando fábrica para um caso único. A regra que muda comportamento é a inversa: *não
crie ponto de extensão antes de existir variação real*.

### O manifesto é o que torna genérico

`validation.json` **não sabe o que é um teste**. Ele roda o comando declarado.
`mvn test`, `pytest`, `go test`, `cargo test` são indistinguíveis para ele.

Por isso o harness não traz ferramenta nem configuração de linguagem — e por isso a
estrutura `config/ruff.toml | eslint.config.js` da spec antiga quebrava a
genericidade: era opinião embutida, e com dez linguagens seriam dez casos
particulares.

### Perguntar tem curva de custo

> Pergunte quando a decisão for **cara de reverter**, ou quando o **efeito não
> aparecer onde a mudança está**. No resto, decida e explique na resposta.

Comentário no código é barato e visível no diff — não vale parada. `valor` virando
centavos não falha em lugar nenhum e quebra o front — vale.

Perguntar demais tem o custo que o próprio guia descreve para alertas: *"alerta que
não gera ação treina o time a ignorar alertas"*. Você para de ler.

### O trabalho global é da ferramenta; o local é do modelo

Padrão que apareceu três vezes com ferramentas diferentes:

- o manifesto roda a suíte inteira → devolve PASS/FAIL
- o índice percorre o repositório → devolve `arquivo:linha`
- o jscpd varre 20 mil linhas → devolve um endereço

Em todos, a varredura acontece **fora** da janela e o que entra é uma coordenada.
Isso é sustentado por evidência: nas ablações do SWE-agent, janela de 100 linhas deu
**18,0%** e arquivo inteiro deu **12,7%** — ler mais não é só caro, é pior.

---

## 3. Decisões tomadas

| Decisão | Escolha |
|---|---|
| Natureza | Configuração declarativa mais **um** executor determinístico, chamado pelos hooks da plataforma. Sem loop próprio: rota nativa, decidida em 05/09 e confirmada em 06/09 |
| Organização | Por quem garante o cumprimento |
| Filtro do guia | Moderado, com teto de 10 no núcleo — verificado por teste desde 06/09 |
| Executor (hook) | **Construído** (06/09): `verify/runner.mjs`; `PostToolUse` roda os `fast`, `Stop` roda os `commands` e bloqueia com exit 2. Chamado como `node` no PATH — a linguagem é derivada do manifesto, ver §10 |
| Distribuição | **Container** — o harness é a raiz, o software mora dentro |
| Permissões | Média — nega o irreversível, pergunta no caro-reversível |
| Manifesto | `consumedBy`, comandos de frescor, `prerequisites`, `unmappedPathPolicy` |
| Idioma | Inglês nos arquivos que o modelo lê |
| Skills | commit, testing, error-handling, api-change, duplication-check |
| Auto memory da plataforma | **Ligada, com política** (06/09): fato datado fica, instrução sobe para camada revisada ou morre, preferência pessoal vai para `~/.claude/CLAUDE.md`. Desligar foi considerado — ver §8 |
| Traço episódico | `verify/.trace.jsonl`, um registro por verificação, gravado pelo runner e não pelo modelo; teto de 2000; nada o lê ainda (06/09) |
| jscpd | Via `npx --yes`, nada instalado; o requisito é o Node, que o frontend já exige (06/09) |
| Fase rápida do backend | **Não adotada** (06/09), apesar de medida em 2,9 s — adiada com sinal |
| Boundaries `dados` e `docs` | Declaradas sem checks, para reportar NO_CHECKS em vez de UNMAPPED; a raiz do harness nomeia `README.md` e `CLAUDE.md` em vez de `*.md`, que casava qualquer caminho (06/09) |

**Sobre o container:** verificado que configuração de projeto carrega só de
`<cwd>/.claude/`, sem busca no diretório pai. Isso torna o modelo "harness como
raiz, projetos dentro" mecanicamente correto — não há cópia nem sincronia. O custo é
que um projeto com git próprio não entra limpo.

---

## 4. O que foi cortado, e por quê

| Cortado | Motivo |
|---|---|
| Modo package (spec/design/tasks/validation, Draft→Ready) | Processo de time com stakeholders. Sozinho, você aprova o próprio documento |
| Loop, orçamento, `StopReason`, detectores | Só existem na rota programática, que existia para **medir** |
| Log JSONL do loop | Idem. Um JSONL voltou por outra porta em 06/09: o runner grava um traço por **verificação**, dentro do hook — registra o que o harness rodou, não o que o modelo fez |
| Evals completos (rubric, pass^k, ablação, kappa) | Aparato acadêmico. Sobreviveram 4 casos como smoke test |
| `spec_diff` próprio | Se o front roda typecheck contra tipos gerados, o consumidor é o detector — e é melhor |
| Wrapper próprio do Graphify | Se quiser o índice, aponte para o MCP dele. Config, não código |
| `tasks.json` | O Claude Code já tem lista de tarefas |
| `config/` com linters por linguagem | Quebrava a genericidade |
| Skill `dry-refactoring` do jscpd | Empurra para eliminar duplicação, contra a regra da terceira ocorrência |
| Servidor MCP do jscpd | Schemas no prefixo de toda chamada por consulta ocasional |

Do documento acadêmico, também deixaram de importar: idade da evidência, o carimbo
apresentado como contribuição própria, a definição não cobrir dois componentes, a
ordem evals→revisor. Tudo isso só pesava numa banca.

Em 06/09 a defensabilidade acadêmica voltou a contar como requisito — o harness em uso
continua sendo o objetivo, e o TCC precisa ser defensável com o que foi construído.
Nada acima foi reaberto por isso; a nota de memória que dizia o contrário foi
reescrita como fato datado.

---

## 5. O que foi medido

Teste real do `jscpd` no NutriPlan — 177 arquivos Java, 30 TS/TSX.

| | Clones | Duplicação | Tempo |
|---|---|---|---|
| Backend completo | 185 | 4,8% | 77ms |
| Backend `main/` | 105 | 7,2% | — |
| Frontend | 28 | 2,2% | 20ms |

**Achados que eu mudaria:**

- **Rodapé de modal duplicado 13 vezes em 9 arquivos** no frontend, com
  `components/` existindo e contendo só três componentes
- **Normalização de string em 4 lugares**, sendo que `Food.normalizeToSearch` já
  existia, pública e estática — mas morando **dentro de uma entidade de domínio do
  módulo `food`**, e por isso invisível para o módulo `patient`, que reimplementou
- `value(String[] fields, Integer index)` em 2 lugares, com `shared/util/CsvReader`
  já existindo como casa óbvia
- Formulário de importação CSV duplicado inteiro entre duas páginas

**Achados que eu não mudaria:** `loadFoodsRequested` vs `loadMeasuresRequests` —
mesma estrutura, tipos diferentes; a abstração custaria mais que a duplicação. Os
blocos de import. As 80 duplicações em teste, que são blocos de *arrange* legítimos.

### Duas armadilhas descobertas no teste

**Par reportado subestima o problema.** O jscpd reporta pares, não famílias. A
normalização veio como 1 par e eram 4 lugares; o rodapé veio como 5 pareamentos e
eram 13 ocorrências. Nunca reporte a contagem de pares como o tamanho do problema.

**Subir `--min-tokens` filtra ao contrário em Java.** Medido:

| min-tokens | Clones | O que sobra |
|---|---|---|
| 50 (padrão) | 105 | tudo |
| 100 | 23 | o achado real ainda está aqui |
| 150 | 5 | **só blocos de import — o achado real sumiu** |

Bloco de imports de Java tem 20+ linhas e muitos tokens; um método duplicado de 17
linhas tem menos. O botão que todo mundo gira primeiro é o errado.

### Economia de token: a conta honesta

| Caminho | Custo | Funciona? |
|---|---|---|
| Não fazer nada | 0 | Não — duplica |
| Regra "procure antes de criar" | ~100 + falha | **Não** — busca pelo nome errado |
| Explorar até achar | 5.000–20.000 | Talvez |
| jscpd calibrado | ~668 + 170 | Sim |
| jscpd **filtrado ao arquivo tocado** | **~30** | Sim |

A ferramenta **não economiza** tokens contra não fazer nada — ela custa. Economiza
uma ordem de grandeza contra a única alternativa que de fato encontra. E o filtro por
arquivo tocado, que só existe com executor, é o que a torna barata.

### Medições de 06/09

| O quê | Medida |
|---|---|
| Suíte do runner, 37 testes | 1,3 s — por isso roda em toda edição, como `fast` |
| Backend, `mvn -B -q -o compile` | 2,8 s quente, 3,3 s frio |
| Backend, `mvn -B -q -o test-compile` | 2,9 s |
| Traço, registros por edição | 1 a 3: um por comando; UNMAPPED e NO_CHECKS também contam |

---

## 6. Ferramentas mapeadas

O teste para descer algo do modelo para uma ferramenta:

1. Mesma entrada, mesma resposta
2. Já existe ferramenta
3. Hoje custa tokens ou erra
4. **O gatilho é observável pelo agente**

O item 4 é o que quase sempre falha. *"Cheque duplicação quando puder estar
duplicando"* é circular — ele não sabe. *"Cheque antes de criar uma função"* ele
sabe. Dispare pela ação, não pelo risco.

| Ferramenta | Mecaniza | Estado (verificado) |
|---|---|---|
| **jscpd** | duplicação estrutural | 6.1k ★, Rust, sem runtime. **Adotado** |
| **Semgrep** | regras do guia que são checáveis | 16.5k ★, multi-linguagem |
| **LSP** (`mcp-language-server`) | onde é definido, quem usa, renomear | 1.6k ★, 6 meses sem push |
| **ArchUnit** | direção de dependência | 3.8k ★, roda dentro do `mvn test` |
| **gitleaks** | segredo no commit e no histórico | 29k ★ |
| **oasdiff** | breaking change estrutural de API | 1.3k ★ |
| **squawk** | migração Postgres perigosa | 1.1k ★ |
| **PIT** | teste que passa mas não testa | 1.9k ★, lento — periódico |
| **OSV-Scanner** | dependência vulnerável | 11k ★ |
| **knip** | código morto JS/TS | 12k ★ |

**Semgrep é o mais importante conceitualmente**: ele é o mecanismo que permite descer
regra do guia para a camada de verificação. Pelo menos duas das dez do núcleo viram
regra checável — nunca engula erro, timeout em chamada externa. A de data atual em
teste saiu do núcleo para a skill `testing` em 06/09. A do timeout ficou em prosa com
exceção nomeada no README: nenhuma boundary deste projeto tem linter para ela, nem o
build Maven nem a toolchain TypeScript.

E descer uma regra **fortalece as que ficam**, porque a adesão deixa de ser dividida
com algo que uma ferramenta garantiria. O P1 não é sobre economia; é sobre não gastar
confiabilidade onde ela não é necessária.

---

## 7. Padrões que se repetiram

**O executor voltou cinco vezes** como o que viabiliza outra coisa: o filtro do jscpd
por arquivo tocado, a verificação determinística, o comando automático de frescor,
cinco das seis ferramentas acima, e por fim o registro estruturado que o paper exige
para o harness deixar de ser estático.

**Ele foi construído** quando o bloqueio real caiu — passou a existir código dentro
do harness para verificar. Vale registrar que o gatilho não foi o argumento se
acumulando, foi a condição material mudando. O padrão apontava a direção certa; o que
autorizava era outra coisa.

**O revisor de contexto limpo voltou três vezes** como o único que pega o caso
semântico: duplicação tipo 4 (mesma ideia, outra forma), que a literatura descreve
como problema aberto sem ferramenta confiável em nenhuma linguagem.

**Nenhuma ferramenta pega duplicação semântica.** O jscpd compara forma; o grafo
codifica relação, não semelhança; a busca por embeddings existe mas as
implementações têm 3 a 56 estrelas. Fica declaradamente em aberto.

---

## 8. O mapeamento contra o paper, com o estado real

O framework de seis dimensões da §6.2 do *Externalization in LLM Agents*. O paper é
explícito de que é *"um framework analítico para comparar arquiteturas de harness, e
não um checklist de implementação"* — então o objetivo não é preencher todas as
linhas, é saber quais estão vazias e por quê.

| Dimensão | Estado |
|---|---|
| **Skills** | Cinco, com divulgação progressiva. Falta o terceiro atributo do paper: revisão guiada por falha observada |
| **Verificação / Control** | Verificação por edição e portão de conclusão com limite de recursão. Sem teto de turno nem de custo — no uso interativo, quem para é você |
| **Permission** | **Parcial** — ver abaixo |
| **Protocols** | Herdado via MCP e pelo contrato dos hooks. Não desenhado |
| **Memory** | Semântica: `CLAUDE.md`, skills e README, revisadas. Episódica: `verify/.trace.jsonl`, determinística, ainda sem leitor. Personalizada: auto memory da plataforma, ligada e não governada pelo harness, com política escrita — ver abaixo. Working context: ausente de propósito — a compactação da plataforma é fechada e, com humano no loop, a janela é a pessoa |
| **Observability** | **Parcial** (06/09). O traço registra toda verificação — boundary, comando, resultado, duração, exit code — com teto de 2000 registros. Sem leitor, sem métrica |

### Permission é política, não isolamento — correção de uma imprecisão

Eu vinha marcando essa dimensão como forte. Ela é parcial.

O paper descreve Permission como *"sandboxing, isolamento de sistema de arquivos,
restrição de rede"*. O que existe aqui são **regras de permissão**, que é coisa mais
fraca. Verificado: negar `curl` e `wget` por nome não fecha a rede — um wrapper que o
matcher não descasca, como `docker exec c curl ...`, é casado como comando `docker` e
passa.

As regras valem. Não são isolamento, e a diferença não deve ser borrada.

### O portão falha aberto

Mesma correção, do outro lado. A verificação **por edição** é determinística — o
`PostToolUse` dispara em toda edição e não há como contornar.

O **portão** não tem essa garantia: é pulado quando a parada vem de interrupção do
usuário, tem a saída ignorada em erro de API, deixa o turno terminar se o callback
estourar o timeout, e é derrubado pelo Claude Code após 8 bloqueios consecutivos. O
harness desiste em 4, abaixo desse teto, e avisa em vez de silenciar.

Ele **reduz a frequência** do "pronto" sem verificação. Não o torna impossível.

### Auto memory: ligada, com política — por quê

A recomendação inicial era desligar (`autoMemoryEnabled: false`), por dois motivos:
uma nota escrita pelo modelo em 05/09 mandava ignorar crítica acadêmica e contradisse o
trabalho do dia seguinte; e o store parecia fragmentado em diretórios que não se
enxergam. O segundo motivo caiu quando verificado: o store é indexado pelo
**repositório git**, não pelo diretório aberto — o store vazio `J--TCC-harness` é de
30/08, anterior ao `git init` de 04/09. E o setting é lido do diretório aberto, então
desligar em `harness/.claude/settings.json` não alcançaria uma sessão aberta em
`J:\TCC`, que grava no mesmo store.

O que o survey pede não é remover, é governar (§3.1): não misturar estado do usuário
com o do projeto; orientação reutilizável pertence à camada de skill, não à de
memória; a evolução da memória é política explícita sobre o que se escreve, promove e
descarta. Daí a política no README: fato datado fica, instrução sobe ou morre,
preferência pessoal vai para o escopo de usuário. Uma nota errada é motivo para
revisar, não para remover. O sinal que reverte está em §9.

---

## 9. Em aberto

- ~~SEM OBJETO desde a §11~~ — **Leitor do traço.** O traço existe desde 06/09; ninguém o lê. Gatilho: querer saber
  se uma falha já aconteceu e não conseguir responder sem abrir o arquivo na mão.
- **Memória episódica para o modelo.** Gatilho: você se pegar corrigindo a mesma coisa
  pela terceira vez.
- **Desligar a auto memory.** Gatilho: uma segunda instrução escrita pelo modelo
  desviar uma sessão. Aí o store é camada de guia sem revisão: `autoMemoryEnabled:
  false` no `settings.json`, com `settings.local.json` como opt-in por máquina.
- **Regra de escrita de memória para o modelo** (fato datado, não instrução). Não entra
  por previsão; e o núcleo está em dez exatos, então entrar exige tirar uma.
- ~~SEM OBJETO desde a §11~~ — **Fase rápida do backend.** Medida em 2,9 s e não adotada. Gatilho: uma edição no
  backend chegar ao portão de 203 s com erro de compilação.
- ~~SEM OBJETO desde a §11~~ — **Hook chamado como `node`.** Os três modos foram
  exercidos na mão em 06/09 (`--hook`, `--gate` verde e `--gate` bloqueando com exit
  2), o que prova o executor, não o registro do hook.

  **Correção de um erro que circulou nesta sessão:** *"o Claude Code congela a
  configuração de hooks na abertura"* é falso. Foi verificado em 06/09 que o app
  **relê o `settings.json`**. O motivo de os hooks não terem disparado é outro e está
  na §11: a sessão foi aberta em `J:\TCC`, e configuração de projeto só carrega de
  `<cwd>/.claude/` — o harness está em `J:\TCC\harness`. A conclusão da §11 não muda;
  a explicação errada, sim.
- ~~SEM OBJETO desde a §11~~ — **`consumedBy` backend→frontend.** Inútil enquanto o `types.ts` for espelho escrito
  à mão — o comentário no topo do arquivo diz *"mirroring the backend DTOs"*.
  Gatilho: o dia em que os tipos passarem a ser gerados do OpenAPI, que o
  `springdoc` já produz em runtime.
- **`core.autocrlf`.** Está ligado, e desde que há código o `git` avisa "LF will be
  replaced by CRLF" em todo arquivo tocado. Inofensivo até existir CI.
- **Semgrep.** Quais regras do núcleo descer, e medir se as que ficam melhoram.
- **LSP para Java.** O `jdtls` precisa indexar antes; não sei o custo real num projeto
  Spring de 177 arquivos.
- **Testar regra a regra.** Rodar a mesma tarefa com e sem cada regra do núcleo e ver
  se o comportamento muda. É o que transformaria o guia de "o que eu acho que ajuda"
  em "o que eu vi mudar comportamento".

---

## 10. A linguagem do executor — migração de Python para Node (06/09, tarde)

Registro da decisão que trocou `verify/runner.py` por `verify/runner.mjs`.

### O critério, que virou o princípio 13

> **O executor não deve introduzir um runtime. Ele reusa um que o projeto já exige.**

A pergunta que começou a conversa era "qual linguagem está em toda máquina". Ela não
tem resposta:

| Candidato | Windows | Linux | macOS |
|---|---|---|---|
| POSIX `sh` | não, só com Git Bash ou WSL | sim | sim |
| PowerShell | sim, 5.1 | não | não |
| Python 3 | não | `python3` quase sempre; `python` só com `python-is-python3` | via Xcode CLT, não garantido |
| Node | não | não | não |
| Go, toolchain | não | não | não |
| Binário compilado | sim | sim | sim, ao custo de distribuir artefato por plataforma |

Verificado na máquina de desenvolvimento: `go` não está no PATH, e o TypeScript não
está instalado globalmente — só no `node_modules` do frontend, 5.9.3.

Como não há linguagem universal, o critério deixa de ser presença e passa a ser
**custo marginal no projeto**:

| Runtime | Já exigido pelo NutriPlan? | Por quê |
|---|---|---|
| `java`, `mvn` | sim | backend |
| `node`, `npm`, `npx` | sim | frontend (`tsc`, `vite`) e o jscpd via `npx --yes` |
| `python` + `pytest` | **não** | só o executor |

O harness pedia duas dependências que nenhuma boundary tinha declarado, enquanto o
README afirmava não trazer ferramenta nenhuma. A afirmação era falsa; a migração a
tornou verdadeira.

### Por que JSDoc e não TypeScript

O Node 24 roda `.ts` direto por *type stripping*, sem instalar nada — testado. Mas
duas coisas foram medidas junto:

- um erro de tipo deliberado **rodou mesmo assim**, imprimindo `42` onde a assinatura
  dizia `string`: o stripping apaga, não verifica
- `enum` falhou com `ERR_UNSUPPORTED_TYPESCRIPT_SYNTAX` — o subconjunto é só o
  apagável

Nas duas opções tipadas a verificação real é a mesma coisa: `tsc --noEmit`, rodado à
parte. Então o TypeScript não compra segurança adicional — compra sintaxe, e cobra um
piso de versão do Node (≥ 23.6) mais o subconjunto apagável. JSDoc em `.mjs` dá o
mesmo checador sem nenhum dos dois custos. Testado: `tsc --checkJs` pegou exatamente
o erro que o stripping deixou passar.

### Onde a tipagem não protege, dito de frente

As quatro entradas do executor são fronteiras de runtime — o payload do hook no
stdin, o `validation.json`, o `.state.json` e a saída dos subprocessos. Ali o JSDoc
vale zero, igual ao TypeScript valeria. Por isso cada uma tem guarda escrita à mão
(`asManifest`, `pathsFromHook`, `loadState`), não `cast`. Quatro testes novos cobrem
exatamente isso.

### O que a migração custou e o que ela achou

| | Antes | Depois |
|---|---|---|
| Dependências que nenhuma boundary pediu | `python` + `pytest` | nenhuma |
| Testes | 32, pytest | **41**, `node:test` embutido |
| Suíte | 1,3 s | **3,1 s** |
| Verificação de tipo | não existia | `tsc --checkJs`, `strict`, limpo |

**A suíte ficou 2,4× mais lenta, e isso é regressão real.** A causa é medida: cada
comando sintético sobe um processo Node (~130 ms) onde o pytest subia um Python mais
barato. Só pesa ao editar o próprio harness — a boundary `harness` é a única que roda
esta suíte. Não foi otimizado; foi registrado.

**O typecheck achou um erro de tipo real** no `traceRecords` antes de o ambiente estar
pronto: `output_head` sendo atribuído a um literal cujo tipo inferido não tinha o
campo. Sozinho, isso já paga o JSDoc.

**Dependência de desenvolvimento não é dependência de execução.** Checar tipos precisa
do TypeScript e dos tipos do Node, que não vêm com o Node e não são resolvíveis por
`npx` — testado, o `tsc` buscado por `npx` não enxerga o `@types/node` do mesmo `npx`.
Ficam declarados em `verify/package.json` e instalados com `npm install` dentro de
`verify/`. Rodar o harness não precisa de nada disso, e enquanto não estiver instalado
o typecheck reporta `BLOCKED`, nunca `FAIL` — o princípio 7 aplicado a ele mesmo.

Para isso o `which` passou a resolver pré-requisito com separador **contra a raiz do
harness**, e não contra o diretório de onde o hook disparou. Confirmado rodando o
runner de dois diretórios diferentes com o mesmo resultado.

### A assimetria, que é o achado

O `validation.json` é agnóstico de linguagem porque não sabe o que é um teste. O
executor **não pode ser**: é código, e código tem linguagem, que pode não existir no
projeto que ele verifica. A única saída completa é distribuir binário em vez de fonte
— o que troca genericidade por opacidade, e num TCC cuja contribuição são princípios
de projeto, entregar a peça central como caixa-preta é mau negócio.

Isso não foi resolvido. Foi reportado, e vale para os 72 repositórios da varredura,
nenhum dos quais nomeia o problema. Ver
[`pivo-principios-de-harness.md`](pivo-principios-de-harness.md), princípio 13.

---

## 11. A camada de verificação foi removida (06/09, noite)

Construída de manhã, migrada para Node à tarde, removida à noite. O registro completo
do que ela era está na §10 e no branch `executor-em-node`; esta seção é sobre o corte.

### O que saiu

`verify/` inteiro — executor, 41 testes, typecheck, traço —, o `validation.json`, o
`validation.example.json`, os dois hooks do `settings.json`, e a skill `api-change`.
Sobraram: permissões, dez regras no `CLAUDE.md`, quatro skills.

### O que motivou

Nenhuma observação de a camada ter mudado um resultado. O fato que pesou é
desconfortável e está medido: **na sessão que a construiu, os hooks nunca
dispararam** — a sessão tinha sido aberta um diretório acima, e a configuração de
hooks só carrega de `<cwd>/.claude/`. Dezenas de edições, zero registros automáticos
no traço. E mesmo assim a suíte, o typecheck e os três modos do executor rodaram,
porque o modelo os rodou por conta.

É uma sessão só, e é o cenário de maior incentivo possível — a tarefa *era* o
executor, com o autor olhando. Não prova que a camada é dispensável numa terça-feira
qualquer. Mas o filtro do harness é subtrativo, e nada tinha sido medido que
colocasse a verificação fora dele.

### O que o corte custou, nomeado em vez de minimizado

Três coisas que prosa não recupera:

1. **O portão tornava "pronto" contingente em vez de declarado.** Uma afirmação que
   sobreviveu a um `exit 2` é objeto diferente de uma afirmação. Prosa pede, não impede.
2. **`BLOCKED` nunca virava `FAIL`.** Sem Maven, o modelo lê "falhou" e vai consertar
   código são. É comportamento destrutivo específico, e a distinção só existia porque
   estava escrita em código.
3. **O filtro por arquivo tocado.** É o que fazia a checagem de duplicação custar ~30
   tokens em vez do relatório inteiro. Sem executor não há filtro — e é justamente a
   evidência do princípio 8, o mais forte do catálogo.

O sinal que reverte está no README, em *What was removed*. Restaurar é um checkout,
não uma reescrita.

### O que isso ensina, que é o que interessa ao TCC

**A definição subtrativa foi aplicada ao próprio código do harness**, e não só ao
guia. Isso é a definição funcionando, não falhando.

Mas o corte expõe uma fraqueza dela que não estava escrita em lugar nenhum:

> O filtro pergunta se o modelo já faz aquilo sozinho. **"O modelo quase sempre faz"
> e "o modelo sempre faz" são respostas diferentes que o filtro não distingue.** Tudo
> o que a camada removida fazia morava exatamente nessa distância.

Enquanto o filtro tiver uma resposta binária para uma pergunta que é de frequência,
ele vai cortar mecanismo de garantia toda vez. Refinar isso é trabalho para o
catálogo de princípios — e é mais valioso do que a camada que foi cortada.

### O estado das seis dimensões, depois

Quatro das seis do survey ficam vazias: Verification/Control, Observability, e as
partes episódica e de working context da Memory. Protocols perde o contrato dos
hooks e fica só com o MCP herdado.

Isso é resultado, não lacuna a pedir desculpa: é o que a definição subtrativa produz
quando é aplicada com honestidade, inclusive contra o trabalho do próprio dia.

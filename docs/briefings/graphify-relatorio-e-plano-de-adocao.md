> **Deprecated em 2026-09-07.** O plano foi executado nesta data: `graphifyy 0.9.56`
> instalado, grafo gerado em `harness/apps/graphify-out/` (2.278 nós, 8.448 arestas,
> 9s) e a skill `impact-analysis` escrita com a regra de roteamento medida. O gatilho
> registrado aqui — mais de ~20 arquivos — já estava satisfeito com folga: 207
> arquivos de código. Vale como registro para o TCC.

# Graphify — relatório e plano de adoção

## Context

Você disse que gostaria de "operar utilizando o Graphify" e pediu um relatório do que
isso seria. O nome é ambíguo (existem ~15 projetos com ele), então a primeira coisa foi
desambiguar: você confirmou que é o **Graphify-Labs/graphify**, o skill `/graphify` para
assistentes de código.

Todos os números abaixo foram verificados **na fonte primária** (API do GitHub, API do
PyPI e README oficial do repositório), não em blogs ou resumos de busca. O que não
consegui confirmar está marcado como tal.

O contexto que muda o veredito: `J:\TCC` hoje tem apenas o `README.md` e
`docs/Nova pasta/boas-praticas-programacao.md`. Você confirmou que **o TCC ainda não
começou**. Isso não invalida a ferramenta — mas muda completamente *quando* adotá-la.

---

## 1. O que é (fatos verificados)

| Campo | Valor | Fonte |
|---|---|---|
| Repositório | `Graphify-Labs/graphify` | api.github.com/repos/Graphify-Labs/graphify |
| Site | https://www.graphify.com | campo `homepage` do repo |
| Stack | Python 3.10+, tree-sitter | README + PyPI |
| Licença | Apache-2.0 | API do GitHub |
| Estrelas / forks | 114.691 / 11.142 | API do GitHub |
| Criado | 2026-04-03 | API do GitHub |
| Último push | 2026-08-30 | API do GitHub |
| Arquivado? | Não — ativo | API do GitHub |
| Pacote PyPI | `graphifyy` (dois "y") — v0.9.53 | pypi.org/pypi/graphifyy/json |
| Releases publicadas | **221** em ~5 meses | API do PyPI |

**Uma frase:** você digita `/graphify .` no assistente e ele transforma o projeto
(código, docs, PDFs, imagens, vídeo) num **knowledge graph consultável**, em vez de você
ficar caçando arquivo com grep.

**O que explicitamente não é:** não é RAG. Não tem embeddings nem vector store. É um
grafo de verdade que se percorre — o que, para um TCC, é uma diferença defensável e fácil
de explicar numa banca.

---

## 2. Como funciona por dentro

Duas vias de extração, e a distinção entre elas é o ponto mais importante do relatório:

**Código → local, determinístico, de graça.** Parsing AST via tree-sitter, 37 gramáticas.
Nenhuma chamada de LLM, nada sai da máquina. Resolve `calls` / `imports` / `inherits` /
`mixes_in` entre arquivos. **Java, Kotlin e Scala estão na lista** — um projeto Spring é
indexável direto.

**Docs, PDFs e imagens → passam por um modelo.** Via skill, usa o modelo da sua sessão do
assistente (ou seja: consome o seu Claude). Headless (`graphify extract`) exige API key de
algum provedor. Vídeo/áudio são transcritos localmente com faster-whisper.

**Saída** — três arquivos em `graphify-out/`:

- `graph.html` — grafo force-directed, clicável, filtrável
- `GRAPH_REPORT.md` — god nodes, conexões surpreendentes, 4–5 perguntas sugeridas
- `graph.json` — o grafo completo, consultável sem reler os arquivos

**Consultas:**

```
graphify query "o que conecta auth ao banco?"
graphify path "UserService" "DatabasePool"
graphify explain "RateLimiter"
```

**Rastreabilidade:** toda aresta é marcada `EXTRACTED` (explícito no fonte), `INFERRED`
(deduzido) ou `AMBIGUOUS`. Você sempre sabe o que foi lido do que foi chutado — isso é
ouro para escrever a metodologia do TCC.

**Detalhe que casa com o seu doc de boas práticas:** comentários `# NOTE:` / `# WHY:` /
`# HACK:` e citações a ADR/RFC viram **nós de primeira classe** ligados ao código que
explicam.

---

## 3. O que significa "operar utilizando"

Essa é a parte que justifica o verbo *operar* — não é uma lib que você importa, é um loop
de trabalho:

| Você faz | Graphify faz |
|---|---|
| `graphify hook install` (uma vez) | instala hooks + merge driver p/ `graph.json` não dar conflito |
| `git commit` | rebuild automático — só AST, custo zero |
| `git checkout` / `git switch` | rebuild automático |
| `git pull` / `git merge` | você roda `graphify update .` na sequência |

E `graphify claude install` grava instruções no projeto para o assistente **consultar o
grafo antes de responder**, em vez de sair lendo arquivo.

---

## 4. Veredito para o seu caso

**Hoje o ganho é zero, e é importante ser direto sobre isso.** O Graphify mapeia o que
existe. Com o TCC não iniciado, o grafo teria dois nós e nenhuma aresta interessante. Ler
`boas-praticas-programacao.md` inteiro custa menos que montar um grafo dele.

**Mas há um argumento real a favor de adotar cedo**, e ele vem do seu próprio objetivo
registrado: o projeto é veículo para *aprender a trabalhar com o agente*. O Graphify é
literalmente um modo de operar com o agente. Aprender a ferramenta num corpus alheio, sem
risco, é aprendizado legítimo — e chegar no TCC já sabendo usar vale mais que descobrir no
meio.

Daí o plano ser faseado: aprender agora em repo de terceiros, adotar de fato quando houver
código.

---

## 5. Riscos e pontos de atenção

1. **Projeto muito novo e em movimento rápido.** Criado em abril de 2026, ainda em `0.x`,
   **221 releases em ~5 meses** (~1,5 por dia). Espere quebra de comportamento entre
   versões. Fixe a versão se o TCC depender de reprodutibilidade.

2. **Supply chain — o nome importa.** O pacote correto é **`graphifyy`, dois "y"**, no
   **PyPI**. Verifiquei: `graphify` no PyPI retorna **404** (não existe, então não há
   typosquat ocupando o nome hoje). No **npm** existe um projeto homônimo diferente
   (`rhanka/graphify`, TypeScript, MIT, 20 estrelas) — não é este. O próprio README avisa
   que outros pacotes `graphify*` não são afiliados.

3. **Ele escreve na sua configuração do Claude Code.** `graphify install` registra o skill
   e `graphify claude install` grava instruções no projeto. Não é invasivo, mas é bom você
   saber que a ferramenta modifica como o seu assistente se comporta.

4. **Custo de token nos documentos.** Código é grátis e offline. PDFs e docs consomem o
   seu Claude. Numa revisão bibliográfica com dezenas de papers, isso não é desprezível.
   Existe `--code-only` para pular essa parte.

5. **O README oficial se contradiz sobre log de consultas.** A seção *Privacy* diz que
   todo `query`/`path`/`explain` é logado em `~/.cache/graphify-queries.log` por padrão,
   com `GRAPHIFY_QUERY_LOG_DISABLE=1` para desligar. Já a tabela de variáveis de ambiente
   diz o oposto — *"off by default, nothing is written unless you opt in"*, citando a issue
   #1797. **Não afirmo qual está certo**; provavelmente a prosa ficou desatualizada. Se
   privacidade de consultas importar, cheque o arquivo depois do primeiro uso em vez de
   confiar no README.

6. **Sem port em Java.** Você prefere tipagem forte, mas o CLI é Python. Isso não é um
   problema real — ele é ferramenta externa, não dependência do seu `pom.xml`. Só não
   espere integrar como lib no Spring. (Existem ports em .NET, Rust e Go; nenhum em Java.)

7. **Não confirmei:** o badge YC S26 aparece no README, mas isso é auto-declaração do
   projeto — não validei em fonte independente.

---

## 6. Plano de adoção

### Pré-requisitos (ambiente já verificado nesta máquina)

| Item | Estado |
|---|---|
| Python 3.12.10 | ✅ atende o mínimo 3.10+ |
| `uv` | ❌ ausente |
| `pipx` | ❌ ausente |
| `winget` v1.29.290 | ✅ disponível para instalar o `uv` |

### Fase 0 — aprender agora, sem tocar no TCC

Objetivo: ver o que a ferramenta produz de verdade, com custo zero e risco zero.

1. Instalar o `uv`:
   ```
   winget install astral-sh.uv
   ```
2. Instalar o CLI e registrar o skill:
   ```
   uv tool install graphifyy
   graphify install
   ```
   Se `graphify` não for encontrado depois, rodar `uv tool update-shell`.
3. Clonar um repo Java público de porte médio num diretório temporário — **fora de
   `J:\TCC`** — e rodar `/graphify . --code-only`.
   O `--code-only` garante extração 100% local e zero consumo de token.
4. Abrir `graphify-out/graph.html` e ler o `GRAPH_REPORT.md`. Testar `graphify explain` e
   `graphify path` em classes que você conhece, para calibrar quanto confiar no resultado.

**Critério de decisão:** se o `GRAPH_REPORT.md` disser algo que você não teria percebido
lendo o código, a ferramenta se paga. Se for só um índice bonito, adie.

### Fase 1 — quando o TCC tiver código

Gatilho: o projeto tiver estrutura real (digamos, mais de ~20 arquivos).

1. `graphify hook install` no `J:\TCC` — rebuild automático a cada commit, sem custo.
2. `graphify claude install` — passo que de fato caracteriza "operar utilizando": o
   assistente passa a consultar o grafo antes de responder.
3. Adicionar ao `.gitignore`:
   ```
   graphify-out/cost.json
   ```
   O resto de `graphify-out/` é feito para ser commitado.
4. Criar `.graphifyignore` se houver código gerado que polua o grafo.

### Fase 2 — bibliografia (opcional, tem custo)

`graphify add <url-do-paper>` baixa e ingere papers; PDFs entram no mesmo grafo do código.
Isso permite ligar "conceito da literatura" a "classe que implementa o conceito" — uma
seção de metodologia que se escreve sozinha. Comece por 2 ou 3 papers e meça o custo antes
de jogar a revisão inteira.

### Decisão pendente sua

Antes da Fase 1 é preciso saber o tema e a stack do TCC. Se for Java/Spring, o suporte é
de primeira linha. Se for outra coisa, reavalio.

---

## Verificação

- **Fase 0 instalou certo:** `graphify --version` responde, e `/graphify` aparece na lista
  de skills do Claude Code.
- **Extração local funcionou sem gastar token:** rodar com `--code-only` e confirmar que
  `graphify-out/graph.json` existe e que nenhuma chamada de modelo foi feita na sessão.
- **A qualidade se sustenta:** escolher duas classes de relação conhecida no repo de teste
  e rodar `graphify path A B`. Se o caminho bater com a realidade, o grafo é confiável.
- **Ponto 5 dos riscos:** depois do primeiro `graphify query`, checar se
  `~/.cache/graphify-queries.log` foi criado. Isso resolve empiricamente a contradição do
  README.
- **Fase 1 pegou:** fazer um commit qualquer e confirmar que `graph.json` mudou sozinho.

---

## Fontes

- https://github.com/Graphify-Labs/graphify — repo e README oficiais (via API do GitHub)
- https://pypi.org/project/graphifyy/ — pacote oficial (via API do PyPI)
- https://www.graphify.com — site declarado no repo

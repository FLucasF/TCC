# Plano de ajustes — harness

Cinco tarefas independentes, em ordem. **Uma por sessão do Claude Code.** Cada uma
tem critério de aceite verificável e um limite explícito de escopo.

Ordem não é preferência: as tarefas 1 e 2 consertam e depois armam a máquina que
valida as outras três. Fora de ordem, as tarefas 3–5 são implementadas sem
verificação e você não fica sabendo.

---

## Tarefa 0 — decisões que são suas, antes de abrir o Claude Code

O agente não deve inventar nenhuma destas. Decida e escreva a resposta antes de
colar as tarefas correspondentes.

**a) O que "portable" significa no README?** Portátil entre máquinas Windows, ou
entre sistemas operacionais? A resposta muda o conserto da Tarefa 1: `py -3` resolve
Windows; multiplataforma exige outra abordagem e provavelmente uma nota no README.
*Necessário para a Tarefa 1.*

**b) O que conta como "uma regra" para o cap de 10?** Hoje são 7 seções, 12
parágrafos ou 13 imperativos, dependendo de como se conta — o cap não é testável
enquanto isso não tiver resposta. Proposta: **um imperativo independente, ou seja,
uma unidade que poderia ser removida sozinha sem quebrar outra.** Se aceitar, o
`CLAUDE.md` passa a ter um imperativo por parágrafo, e o parágrafo vira a unidade
contável. *Necessário para a Tarefa 3.*

**c) A auto-memory fica ligada?** *Necessário para a Tarefa 5.* O argumento dos dois
lados está no fim deste arquivo.

---

## Tarefa 1 — tirar o caminho absoluto dos hooks

> **Contexto:** `.claude/settings.json` chama os dois hooks por
> `C:\Users\Lucas\AppData\Local\Programs\Python\Python312\python.exe`. O README
> descreve o harness como portátil na primeira linha. Em qualquer outra máquina os
> hooks falham, e falha de hook não gera erro visível para o agente — apenas some a
> verificação.
>
> **Faça:** substitua o caminho absoluto por uma resolução via PATH nos dois hooks
> (`PostToolUse` e `Stop`). Antes de escolher, verifique nesta máquina quais destes
> resolvem para um Python 3: `py -3`, `python`, `python3`. Escolha o que resolver e
> use-o. Se nenhum resolver, pare e me diga em vez de manter o caminho absoluto.
>
> **Critério de aceite:** edite qualquer arquivo dentro de um boundary declarado e
> confirme que o hook `PostToolUse` executou de fato — não que o `settings.json`
> parece certo, mas que a verificação rodou.
>
> **Não faça:** não mexa em `validation.json`, `CLAUDE.md`, README ou nas skills.
> Não adicione fallback, wrapper ou script de detecção — a mudança é o valor de dois
> campos.

Depois de aplicar, **rode um teste negativo**: quebre o nome do comando de
propósito, edite um arquivo, e confirme que você consegue perceber a ausência da
verificação. Se não der pra perceber, esse é um achado que vale mais que o conserto.

---

## Tarefa 2 — fazer o harness validar a si mesmo

> **Contexto:** existe `verify/test_runner.py` com testes do runner. O boundary
> `harness` em `.claude/validation.json` cobre `verify/**` e declara
> `commands: []` — ou seja, esses testes nunca rodam pelo portão de conclusão. O
> harness é a única parte do projeto que não se verifica.
>
> **Faça:** declare no boundary `harness` o comando que roda os testes do runner,
> com `prerequisites` correto. Use o mesmo executável de Python resolvido na Tarefa
> 1. Decida entre `fast` e `commands` pela duração medida: rode os testes, cronometre,
> e ponha em `fast` se for comparável aos 3s do typecheck do frontend, em `commands`
> se for comparável aos 203s do backend. Me diga a medição.
>
> **Critério de aceite:** editar um arquivo em `verify/` dispara os testes do runner.
> Um teste quebrado de propósito faz o portão bloquear.
>
> **Não faça:** não escreva testes novos, não altere `runner.py`, não toque nos
> boundaries `backend` e `frontend`.

---

## Tarefa 3 — tornar o cap de 10 verificável

Esta é a maior das cinco. Se preferir dividir, o corte natural é entre o passo 2
e o passo 3.

> **Contexto:** o `CLAUDE.md` tem 13 imperativos independentes contra um cap
> declarado de 10, e o README não define o que conta como uma regra. Duas das
> regras — "todo bug corrigido ganha o teste que falhava" e "testes sem
> aleatoriedade, data atual ou rede" — são específicas de teste, e existe uma skill
> `testing` cuja descrição já cobre esse território. Diferente da regra de breaking
> change, que é always-on porque *dispara* o reconhecimento de que a skill
> `api-change` se aplica, essas duas não têm função de gatilho: quem está escrevendo
> um teste já sabe que está.
>
> **Faça, nesta ordem:**
>
> 1. Mova as duas regras de teste para `.claude/skills/testing/SKILL.md`, integradas
>    ao corpo existente — não como uma seção "regras" apensada no fim. Remova-as do
>    `CLAUDE.md`.
> 2. Reestruture o `CLAUDE.md` para um imperativo por parágrafo, sem mudar o
>    conteúdo de nenhuma regra que ficou. [**Cole aqui a definição de "regra" que
>    você decidiu na Tarefa 0b.**]
> 3. Trate a seção "Verification" separadamente: o parágrafo que explica que o
>    harness roda a validação sozinho é documentação de mecanismo, não prática, e
>    não deve contar para o cap. Separe-o visualmente e deixe explícito no README que
>    ele está fora do cap, e por quê.
> 4. Adicione a `verify/` um teste que conte as regras do `CLAUDE.md` pela definição
>    acima e falhe se passarem de 10.
> 5. Atualize a seção "Adding a rule" do README com a definição.
>
> **Critério de aceite:** o teste novo passa com o `CLAUDE.md` atual e falha se você
> acrescentar um parágrafo imperativo. A skill `testing` cobre as duas práticas
> movidas sem repetição.
>
> **Não faça:** não reescreva o texto das regras que permanecem — a tarefa é
> estrutura e contagem, não redação. Não mexa nas outras quatro skills.

Depois disso o cap deixa de ser intenção e vira invariante — e é a forma mais barata
do seu item adiado "casos de avaliação".

---

## Tarefa 4 — dar ao backend um check rápido de correção

> **Contexto:** a fase `fast` do boundary `frontend` roda `npm run typecheck` em 3s.
> A do `backend` roda apenas `jscpd` com `reportOnly: true`, que não checa correção.
> Na prática, uma edição no backend não recebe nenhum sinal até o portão de 203s. A
> divisão em duas fases, que foi medida, só existe de fato em um dos dois boundaries.
>
> **Faça:** identifique o comando mais barato do backend que detecte erro de
> compilação sem rodar a suíte — em Maven normalmente `mvn -B -q -o test-compile` ou
> `mvn -B -q -o compile`. Meça a duração dos candidatos e me diga os números antes de
> escolher. Se nenhum ficar em ordem de grandeza comparável aos 3s do frontend,
> pare e me diga: pode ser que o backend não comporte fase rápida, e isso é uma
> conclusão legítima que vale registrar no README.
>
> **Critério de aceite:** editar um arquivo do backend com erro de sintaxe produz
> falha em segundos, não em 203.
>
> **Não faça:** não remova nem altere o jscpd. Não mexa no boundary `frontend`.

---

## Tarefa 5 — resolver a linha "personalised" da tabela de memória

Esta é decisão sua; o agente só executa depois que você escolher.

> **Contexto:** o README marca a dimensão Memory como "Semantic (`CLAUDE.md`) and
> personalised (platform auto-memory)". A auto-memory está ligada por padrão da
> plataforma, não por decisão do harness. Ela é o único componente de memória aqui
> escrito por processo probabilístico, e não escreve apenas preferência do usuário:
> escreve fato sobre o projeto, que passa a ser lido junto do `CLAUDE.md` sem ter
> passado por revisão. Marcar a linha como preenchida esconde isso.
>
> **Faça:** [**cole a decisão da Tarefa 0c**] e atualize a linha Memory da tabela do
> README para descrever o que de fato existe e por qual decisão.
>
> **Não faça:** não crie mecanismo de memória episódica nesta tarefa — ela continua
> adiada, com o sinal já registrado no README.

**Desligar** (`"autoMemoryEnabled": false` em `.claude/settings.json`): coerente com
a doutrina de camadas — estado não revisado escrito por processo probabilístico e
lido depois como fato é exatamente a camada 3 ocupando lugar da camada 1. O survey
chama o modo de falha de *poisoned or conflicting memories*. É a opção defensável
no texto do TCC.

**Manter ligada:** é utilidade real no uso diário e você observa o que ela escreve
antes de julgar. Nesse caso a linha da tabela deve dizer que a dimensão é atendida
por um mecanismo da plataforma que o harness não governa — o que é uma afirmação
diferente e mais honesta do que "personalised: presente".

Nada impede escolhas diferentes para o TCC e para o uso diário, desde que o README
diga qual está configurada.

---

## Achados menores, sem tarefa própria

Junte ao primeiro commit que já tocar no arquivo:

- **README, primeira linha:** "there is nothing to run" contra `verify/runner.py`
  descrito adiante. A leitura caridosa é "nada para *você* rodar" — vale desambiguar.
- **jscpd com duas rotas de aquisição:** o README manda instalar global via `irm`;
  o `validation.json` usa `npx --yes`. Escolha uma.
- **`apps/dados` e `apps/docs` sem boundary declarado.** Caem em
  `unmappedPathPolicy: "report"`. Se for intencional, vale um comentário dizendo por
  quê — o README trata declarar boundary como o passo 3 do fluxo.
- **Regra 6 ("Validate at the boundary and fail immediately")** está escrita como
  instrução positiva; o que muda comportamento é a restrição — validar *na
  fronteira, não em toda camada*. Reescrever na forma restritiva a alinha com as
  outras doze.
- **Regra 7 (timeout em toda chamada externa)** passa no teste de admissão mas é
  detectável por linter na maioria das linguagens. Pela regra do próprio README —
  *"o que uma ferramenta consegue checar não pertence à prosa"* — ou ela desce para
  a camada 2 em algum boundary, ou o README ganha a exceção explicando por que ficou.

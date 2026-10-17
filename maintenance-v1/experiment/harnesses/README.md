# Os harnesses do maintenance-v1

Cada pasta aqui é um braço do experimento: o que vai junto da cópia da base para a raiz do workspace. Os
braços acumulam, como na construção (decisão do Lucas, 15/10/2026):

| braço | o que o agente recebe | pasta |
|---|---|---|
| **N0** | só a base e o pedido | nenhuma |
| **N1** | o `CLAUDE.md` | `N1/` |
| **N2** | o `CLAUDE.md` do N1 com uma linha a mais, mandando consultar a skill, e a skill `gof-patterns` | `N2/` |

O `CLAUDE.md` do N2 é o do N1, intacto, com um item a mais no fim: "Antes de decidir a estrutura da mudança,
consulte a skill `gof-patterns`." A diferença entre o N1 e o N2 é, então, a skill **e** a instrução de
consultá-la; as duas não se separam (veja abaixo por quê).

## O `CLAUDE.md`

Curto de propósito, como a Anthropic recomenda: só o que o agente não descobre lendo o projeto, em poucas
linhas, sem repetir o README da base. Ele não nomeia nenhum pattern (isso fica para a skill do N2) e não usa as
frases dos pedidos. Fala dos dois lados do que se mede: dar a cada caso um lugar seu quando os casos crescem, e
não criar estrutura para uma variação que ninguém pediu.

## A skill

A mesma das pesquisas de construção, byte a byte: veio de
`construction-v5/experiment/harnesses/N2/.claude/skills/gof-patterns/`. Para o git, a árvore dela tem o mesmo
hash nos dois lugares, `10662f87e28079fc5ed9fade9f9b4b260a08e4e1`:

```bash
git rev-parse HEAD:construction-v5/experiment/harnesses/N2/.claude/skills/gof-patterns
```

```bash
git rev-parse HEAD:maintenance-v1/experiment/harnesses/N2/.claude/skills/gof-patterns
```

Ela fala de quando usar cada pattern e de quando não usar (a seção "Anti-Patterns to Avoid", com
"Over-Engineering", "YAGNI" e "Strategy for Everything → Simple if/switch first", e um "When NOT to use" em
cada pattern). Pode ajudar nos cenários que pedem o pattern e nos controles negativos, ou empurrar para o
exagero: os dados vão dizer.

**A skill só age se o agente a usar.** O Claude Code não lê a skill sozinho: no começo, o agente recebe só o nome
e a descrição dela, e o conteúdo só entra se o próprio modelo decidir chamá-la. A descrição da `gof-patterns`
fala de "escolher design patterns" e de "decisões de arquitetura", e os pedidos daqui, na voz do dono da
pousada, nunca falam disso. O resultado:

- no cenário 01, com o `CLAUDE.md` igual ao do N1, **nenhum dos 11 agentes do N2 abriu a skill** (16/10/2026);
- na construção, a mesma skill **nunca foi aberta**: 0 de 55 execuções do N2 e 0 de 55 do N3, no
  `construction-v5`.

Por isso, desde 17/10/2026 (decisão do Lucas), o `CLAUDE.md` do N2 manda consultar a skill. As 11 execuções do
N2 sem a linha ficam guardadas como registro de que a skill disponível não é usada espontaneamente. A transcrição
de cada execução continua dizendo se a skill foi aberta (`skill_opened` no `meta.json`): uma execução do N2 sem a
skill aberta é, na prática, um N1.

Não mexa em nada desta pasta depois que as execuções começarem: é ela que identifica o que cada execução
recebeu.

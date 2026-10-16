# Os harnesses do maintenance-v1

Cada pasta aqui é um braço do experimento: o que vai junto da cópia da base para a raiz do workspace. Os
braços acumulam, como na construção (decisão do Lucas, 15/10/2026):

| braço | o que o agente recebe | pasta |
|---|---|---|
| **N0** | só a base e o pedido | nenhuma |
| **N1** | o `CLAUDE.md` | `N1/` |
| **N2** | o mesmo `CLAUDE.md`, byte a byte, e a skill `gof-patterns` | `N2/` |

Manter o `CLAUDE.md` igual no N1 e no N2 é o que permite atribuir à skill a diferença entre os dois.

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

**A skill só age se o agente a usar.** O Claude Code decide sozinho se abre a skill, pela descrição dela. A
transcrição de cada execução mostra se ela foi lida, e isso precisa ser anotado: uma execução do N2 em que a
skill nunca foi aberta é, na prática, um N1.

Não mexa em nada desta pasta depois que as execuções começarem: é ela que identifica o que cada execução
recebeu.

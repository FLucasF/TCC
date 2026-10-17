# Os harnesses do construction-v6

Cada pasta aqui é um braço do experimento: o que vai para a raiz do workspace, que começa vazio. Os braços acumulam,
como na manutenção:

| braço | o que o agente recebe | pasta |
|---|---|---|
| **N0** | só o enunciado | nenhuma |
| **N1** | o `CLAUDE.md` | `N1/` |
| **N2** | o `CLAUDE.md` do N1 com uma linha a mais, mandando consultar a skill, e a skill `gof-patterns` | `N2/` |

O `CLAUDE.md` do N2 é o do N1, intacto, com um item a mais no fim: "Antes de decidir a estrutura, consulte a skill
`gof-patterns`." A diferença entre o N1 e o N2 é, então, a skill **e** a instrução de consultá-la: sem a instrução,
nenhum agente abria a skill (0 de 110 no construction-v5; 0 de 11 na manutenção antes da linha, 10 de 11 depois).

## O `CLAUDE.md`

Adaptado do da manutenção (decisão do Lucas, 17/10/2026), com as mesmas ideias e o mesmo tom, trocando o que era de
mudança (ler o código que a mudança toca, mudar só o que o pedido precisa) pelo que é de construção. Curto de
propósito, como a Anthropic recomenda, e sem nomear nenhum pattern (isso fica para a skill do N2). Fala dos dois
lados do que se mede: dar a cada caso um lugar seu quando os casos crescem, e não criar estrutura para uma variação
que ninguém pediu. Os dois itens a mais tocam as dimensões que a manutenção não mede: o sistema de fora (a
TransLog, o ponto do Adapter) e os testes.

## A skill

A mesma da manutenção e do construction-v5, byte a byte (veio de `maintenance-v1/experiment/harnesses/N2/`). Ela
cobre os quatro patterns que a correção olha aqui (Strategy, Adapter, State e Factory Method) e fala de quando não
usar cada um.

O `run_one_execution.py` só roda o N1 e o N2 com a pasta do braço commitada; o `meta.json` guarda o hash da árvore
(`harness_tree`) e diz se o agente abriu a skill (`skill_opened`).

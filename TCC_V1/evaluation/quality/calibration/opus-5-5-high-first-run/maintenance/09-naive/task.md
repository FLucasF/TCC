# A revisão

- **Pesquisa:** manutenção.
- **Cenário:** 09; o texto pede, sem dizer o nome, o Template Method (a referência em `.claude/skills/review-design/references/template-method.md`).
- **O texto que o agente recebeu:** `pedido.md`.
- **O projeto:** `base/` (antes da mudança), `final/` (como o agente o deixou) e `changes.patch` (a diferença, sem os arquivos de configuração do agente).
- **Os scripts:** rode cada um com `final`; o da arquitetura e o dos testes, também com `base`, para comparar.
- **O roteiro do comportamento:** `.claude/skills/review-behavior/check_09.sh final`.

Escreva a revisão em `review.md`, no formato do agente `quality-reviewer`.

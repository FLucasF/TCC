# A revisão

- **Pesquisa:** manutenção.
- **Cenário:** 02; é um **controle negativo**: o texto pede uma mudança pontual, e a resposta certa é a mudança pequena, sem estrutura nova (os critérios em `.claude/skills/review-design/references/controls.md`, na parte deste cenário).
- **O texto que o agente recebeu:** `pedido.md`.
- **O projeto:** `base/` (antes da mudança), `final/` (como o agente o deixou) e `changes.patch` (a diferença, sem os arquivos de configuração do agente).
- **Os scripts:** rode cada um com `final`; o da arquitetura e o dos testes, também com `base`, para comparar.
- **O roteiro do comportamento:** `.claude/skills/review-behavior/check_02.sh final`.

Escreva a revisão em `review.md`, no formato do agente `quality-reviewer`.

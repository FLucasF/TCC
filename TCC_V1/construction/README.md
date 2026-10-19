# A construção

O agente recebe uma pasta vazia (com o harness do braço) e o enunciado de **um** cenário: o sistema da pousada
descrito por inteiro, na voz do dono, com a parte do cenário no fim. Ele constrói o sistema do zero.

| pasta | o que tem |
|---|---|
| `experiment/scenarios/` | a moldura comum (`frame.md`), os 5 cenários e os trechos da moldura que o cenário 05 troca (`defaults/` e `05-*.<TRECHO>.md`) |
| `experiment/harnesses/` | o `N1/` (o `CLAUDE.md`) e o `N2/` (o `CLAUDE.md` com a linha da skill, e a skill `design-patterns`) |
| `infra/scripts/run_one_scenario.py` | roda uma execução; a saída vai para `runs/<cenário>-<pattern>/<braço>-<modelo>/` |

Uma execução por modelo, cenário e braço: com 3 modelos, 5 × 3 × 3 = 45.

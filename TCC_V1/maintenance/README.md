# A manutenção

O agente recebe uma cópia limpa da base (o sistema de reservas da pousada, escrito sem nenhum pattern, que funciona),
o harness do braço e o pedido de mudança de **um** cenário, na voz do dono. A base nunca é tocada.

| pasta | o que tem |
|---|---|
| `experiment/base/` | a base (`hotel/`: `mvn verify`; `java -jar target/hotel.jar`) e o roteiro dela (`ROTEIRO-DA-BASE.md`) |
| `experiment/scenarios/` | a moldura comum (`frame.md`) e os 5 pedidos |
| `experiment/harnesses/` | o `N1/` (o `CLAUDE.md`) e o `N2/` (o `CLAUDE.md` com a linha da skill, e a skill `design-patterns`) |
| `evaluation/solutions/` | as 10 soluções de referência (o pattern e a ingênua de cada cenário, como patches sobre a base) e os geradores; a correção as usa na calibração |
| `infra/scripts/run_one_scenario.py` | roda uma execução; a saída vai para `runs/<cenário>-<pattern>/<braço>-<modelo>/`, com o `changes.patch` contra a base |

Uma execução por modelo, cenário e braço: com 3 modelos, 5 × 3 × 3 = 45.

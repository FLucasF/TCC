# A manutenção

O agente recebe uma cópia limpa da base (o sistema de reservas da pousada, escrito sem nenhum pattern, que funciona),
o harness do braço e o pedido de mudança de **um** cenário, na voz do dono. A base nunca é tocada.

| pasta | o que tem |
|---|---|
| `experiment/base/` | a base (`hotel/`: `mvn verify`; `java -jar target/hotel.jar`; os dados num PostgreSQL, com o esquema em migrações do Flyway) e o roteiro dela (`ROTEIRO-DA-BASE.md`) |
| `experiment/scenarios/` | a moldura comum (`frame.md`) e os 5 pedidos |
| `experiment/harnesses/` | o `N1/` (o `CLAUDE.md`) e o `N2/` (o `CLAUDE.md` com a linha da skill, e a skill `design-patterns`) |
| `infra/scripts/run_one_scenario.py` | roda uma execução, com um PostgreSQL só dela; a saída vai para `runs/<cenário>-<pattern>/<braço>-<modelo>/`, com o `changes.patch` contra a base e a foto do esquema (`database-schema.sql`) |

Uma execução por modelo, cenário e braço: com 3 modelos, 5 × 3 × 3 = 45.

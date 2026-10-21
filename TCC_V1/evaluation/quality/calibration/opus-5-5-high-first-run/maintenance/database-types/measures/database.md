# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 3 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- Flyway: 2 migrações aplicadas
- arquivos de esquema em src/main/resources: 2
  - `src/main/resources/db/migration/V1__create_tables.sql`
  - `src/main/resources/db/migration/V2__reservation_company_code.sql`

Na manutenção, as migrações da base (`base/`) comparadas com as do projeto final:
- `src/main/resources/db/migration/V1__create_tables.sql`: EDITADA no projeto final (uma migração já aplicada mudou)
- `src/main/resources/db/migration/V2__reservation_company_code.sql`: nova

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 5 | 1 | NÃO | 0 | 0 | 0 |
| `public.reservations` | 17 | 1 | sim | 0 | 0 | 0 |
| `public.rooms` | 4 | 1 | sim | 0 | 0 | 0 |

## As conferências

**Tabela sem chave primária:** public.messages

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- `reservations.amount_paid`: double precision(53,), PONTO FLUTUANTE
- `reservations.discount`: double precision(53,), PONTO FLUTUANTE
- `reservations.refund`: double precision(53,), PONTO FLUTUANTE
- `reservations.subtotal`: double precision(53,), PONTO FLUTUANTE
- `reservations.total`: double precision(53,), PONTO FLUTUANTE
- `rooms.nightly_rate`: double precision(53,), PONTO FLUTUANTE

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- nenhuma

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- reservations.check_in (text)
- reservations.check_out (text)

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- messages.channel: text, sem CHECK
- reservations.rate_plan: text, sem CHECK
- reservations.status: text, sem CHECK
- rooms.type: text, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 5
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 0
- `FOR UPDATE` (trava de linha): 0

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/hotel/reservations/ReservationStore.java:33:                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
src/main/java/hotel/reservations/ReservationStore.java:80:                                + "AND check_in < ? AND ? < check_out")) {
src/main/java/hotel/reservations/ReservationStore.java:95:                        "SELECT " + COLUMNS + " FROM reservations WHERE id = ?")) {
src/main/java/hotel/reservations/ReservationStore.java:108:                        "SELECT " + COLUMNS + " FROM reservations ORDER BY id");
```

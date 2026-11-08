# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 3 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/pousada/persistence/Database.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 5 | 5 | sim | 0 | 0 | 0 |
| `public.reservations` | 16 | 15 | sim | 1 | 0 | 0 |
| `public.rooms` | 4 | 4 | sim | 0 | 0 | 0 |

## As conferências

**Tabela sem chave primária:** nenhuma

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- `reservations.amount_paid`: numeric(12,2), ok
- `reservations.discount`: numeric(12,2), ok
- `reservations.refund`: numeric(12,2), ok
- `reservations.subtotal`: numeric(12,2), ok
- `reservations.total`: numeric(12,2), ok
- `rooms.nightly_rate`: numeric(12,2), ok

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- reservations (room_number) -> rooms

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- messages.channel: text, sem CHECK
- reservations.rate_plan: text, sem CHECK
- reservations.status: text, sem CHECK
- rooms.type: text, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 6
- pool de conexões (HikariCP ou outro): 0 arquivos
- `DriverManager.getConnection`: 1 chamadas
- transações explícitas (`setAutoCommit(false)`): 1
- `FOR UPDATE` (trava de linha): 2

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/pousada/persistence/ReservationRepository.java:65:        return one(c, SELECT + "WHERE id = ?", id);
src/main/java/pousada/persistence/ReservationRepository.java:69:        return one(c, SELECT + "WHERE id = ? FOR UPDATE", id);
src/main/java/pousada/persistence/ReservationRepository.java:90:        return list(c, SELECT + "WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
src/main/java/pousada/persistence/ReservationRepository.java:91:                + "AND check_in <= ? AND check_out > ? ORDER BY room_number, id", night, night);
src/main/java/pousada/persistence/ReservationRepository.java:95:        return list(c, SELECT + "WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id",
src/main/java/pousada/persistence/RoomRepository.java:19:            try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM rooms ORDER BY number");
src/main/java/pousada/persistence/RoomRepository.java:29:        return query(c, "SELECT " + COLUMNS + " FROM rooms WHERE number = ?", number);
src/main/java/pousada/persistence/RoomRepository.java:34:        return query(c, "SELECT " + COLUMNS + " FROM rooms WHERE number = ? FOR UPDATE", number);
```

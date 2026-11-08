# Banco: o esquema que o serviço cria e as conferências simples

Resultado: medido. O serviço subiu contra um banco vazio e criou 2 tabelas (o esquema inteiro em `measures/database-schema.sql`).

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/br/com/marealta/persistence/Database.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.message` | 5 | 5 | sim | 0 | 0 | 0 |
| `public.reservation` | 16 | 15 | sim | 0 | 0 | 0 |

## As conferências

**Tabela sem chave primária:** nenhuma

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- `reservation.amount_paid`: numeric(12,2), ok
- `reservation.discount`: numeric(12,2), ok
- `reservation.refund`: numeric(12,2), ok
- `reservation.subtotal`: numeric(12,2), ok
- `reservation.total`: numeric(12,2), ok

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- nenhuma

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- message.channel: text, sem CHECK
- reservation.rate_plan: text, sem CHECK
- reservation.status: text, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 3
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 1
- `FOR UPDATE` (trava de linha): 1

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
src/main/java/br/com/marealta/persistence/ReservationRepository.java:61:        return db.query("SELECT " + COLUMNS + " FROM reservation WHERE id = ? FOR UPDATE",
src/main/java/br/com/marealta/persistence/ReservationRepository.java:66:        return db.query("SELECT " + COLUMNS + " FROM reservation WHERE id = ?",
```

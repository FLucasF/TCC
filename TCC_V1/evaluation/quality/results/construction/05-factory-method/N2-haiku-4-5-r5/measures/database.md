# Banco: o esquema que o serviço cria e as conferências simples

Resultado: o serviço não subiu contra um banco vazio; o esquema abaixo é o que ficou (3 tabelas). O fim do log do serviço:
```
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
Exception in thread "main" java.lang.IllegalArgumentException: Path '/reservations/:id' invalid - Javalin 4 switched from ':param' to '{param}'.
	at io.javalin.router.matcher.PathParser.<init>(PathParser.kt:10)
	at io.javalin.router.ParsedEndpoint.<init>(ParsedEndpoint.kt:12)
	at io.javalin.router.InternalRouter.addHttpEndpoint(InternalRouter.kt:44)
	at io.javalin.Javalin.addEndpoint(Javalin.java:183)
	at io.javalin.Javalin.addEndpoint(Javalin.java:28)
	at io.javalin.router.JavalinDefaultRoutingApi.addHttpHandler(JavalinDefaultRoutingApi.kt:111)
	at io.javalin.router.JavalinDefaultRoutingApi.addHttpHandler(JavalinDefaultRoutingApi.kt:103)
	at io.javalin.router.JavalinDefaultRoutingApi.get(JavalinDefaultRoutingApi.kt:128)
	at com.pousadamareaalta.Main.main(Main.java:20)
```

## Como o esquema é criado

- arquivos de esquema em src/main/resources: 0
- `CREATE TABLE` dentro do código Java: src/main/java/com/pousadamareaalta/Database.java

## As tabelas

| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |
|---|---|---|---|---|---|---|
| `public.messages` | 5 | 5 | sim | 0 | 0 | 0 |
| `public.reservations` | 20 | 14 | sim | 1 | 0 | 0 |
| `public.rooms` | 4 | 4 | sim | 0 | 0 | 0 |

## As conferências

**Tabela sem chave primária:** nenhuma

**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax, fee, valor, preco, desconto; o certo é `numeric(p, 2)`):

- `reservations.amount_paid`: numeric(10,2), ok
- `reservations.discount`: numeric(10,2), ok
- `reservations.refund`: numeric(10,2), ok
- `reservations.subtotal`: numeric(10,2), ok
- `reservations.total`: numeric(10,2), ok
- `rooms.nightly_rate`: numeric(10,2), ok

**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):

- reservations (room_number) -> rooms

**Datas guardadas como texto** (colunas com nome de data em text ou varchar):

- nenhuma

**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:

- messages.channel: character varying, sem CHECK
- reservations.rate_plan: character varying, sem CHECK
- reservations.status: character varying, sem CHECK
- rooms.type: character varying, sem CHECK

**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):

- `nenhuma`

## O código que fala com o banco

- arquivos com JDBC (`java.sql`/`javax.sql`): 4
- pool de conexões (HikariCP ou outro): 1 arquivos
- `DriverManager.getConnection`: 0 chamadas
- transações explícitas (`setAutoCommit(false)`): 0
- `FOR UPDATE` (trava de linha): 0

SQL montado com `+` (candidatos; confira se o que entra é constante ou dado de fora):
```
```

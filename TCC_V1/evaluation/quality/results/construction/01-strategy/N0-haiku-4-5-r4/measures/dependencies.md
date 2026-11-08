# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 45 |
| `com.pousada.api` | 8 | 348 |
| `com.pousada.db` | 1 | 113 |
| `com.pousada.entity` | 2 | 66 |
| `com.pousada.rateplan` | 8 | 299 |
| `com.pousada.service` | 4 | 468 |
| `com.pousada.util` | 2 | 77 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.entity
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.db
com.pousada.rateplan -> com.pousada.entity
com.pousada.rateplan -> com.pousada.util
com.pousada.service -> com.pousada.db
com.pousada.service -> com.pousada.entity
com.pousada.service -> com.pousada.rateplan
com.pousada.service -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 2 | 1 |
| `com.pousada.db` | 0 | 2 |
| `com.pousada.entity` | 0 | 3 |
| `com.pousada.rateplan` | 2 | 1 |
| `com.pousada.service` | 4 | 1 |
| `com.pousada.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim | sim |  |  |  |  |
| `com.pousada.api` | sim | sim | sim |  |  |  |
| `com.pousada.db` |  | sim |  |  | sim |  |
| `com.pousada.entity` |  |  |  |  |  |  |
| `com.pousada.rateplan` |  |  |  |  |  |  |
| `com.pousada.service` |  | sim |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  243 src/main/java/com/pousada/api/ReservationEndpoints.java
  223 src/main/java/com/pousada/service/ReservationService.java
  113 src/main/java/com/pousada/db/Database.java
   99 src/main/java/com/pousada/service/ReportService.java
   98 src/main/java/com/pousada/service/MessageService.java
   55 src/main/java/com/pousada/api/ReportEndpoints.java
   54 src/main/java/com/pousada/rateplan/CorporatePlan.java
   49 src/main/java/com/pousada/rateplan/WeekendPackagePlan.java
   49 src/main/java/com/pousada/entity/Reservation.java
   48 src/main/java/com/pousada/service/RoomService.java
```

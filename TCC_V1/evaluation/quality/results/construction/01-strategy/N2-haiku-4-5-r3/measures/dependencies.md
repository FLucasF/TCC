# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 28 |
| `com.pousada.api` | 5 | 354 |
| `com.pousada.database` | 4 | 361 |
| `com.pousada.model` | 5 | 91 |
| `com.pousada.rateplan` | 10 | 295 |
| `com.pousada.service` | 2 | 198 |
| `com.pousada.util` | 2 | 68 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.database
com.pousada.api -> com.pousada.model
com.pousada.api -> com.pousada.service
com.pousada.api -> com.pousada.util
com.pousada -> com.pousada.api
com.pousada -> com.pousada.database
com.pousada.database -> com.pousada.model
com.pousada.rateplan -> com.pousada.model
com.pousada.rateplan -> com.pousada.util
com.pousada.service -> com.pousada.database
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.rateplan
com.pousada.service -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 4 | 1 |
| `com.pousada.database` | 1 | 3 |
| `com.pousada.model` | 0 | 4 |
| `com.pousada.rateplan` | 2 | 1 |
| `com.pousada.service` | 4 | 1 |
| `com.pousada.util` | 0 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim |  | sim |  |  |  |
| `com.pousada.database` |  | sim |  |  | sim |  |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.rateplan` |  |  |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  189 src/main/java/com/pousada/api/ReservationHandler.java
  186 src/main/java/com/pousada/service/ReservationService.java
  171 src/main/java/com/pousada/database/ReservationDao.java
  116 src/main/java/com/pousada/api/ReportHandler.java
   92 src/main/java/com/pousada/database/Database.java
   57 src/main/java/com/pousada/rateplan/CorporateRatePlan.java
   57 src/main/java/com/pousada/database/RoomDao.java
   50 src/main/java/com/pousada/rateplan/LongStayRatePlan.java
   48 src/main/java/com/pousada/rateplan/WeekendPackageRatePlan.java
   48 src/main/java/com/pousada/rateplan/NonRefundableRatePlan.java
```

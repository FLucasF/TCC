# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 69 |
| `com.pousada.api` | 1 | 316 |
| `com.pousada.business` | 2 | 211 |
| `com.pousada.db` | 5 | 403 |
| `com.pousada.models` | 6 | 277 |
| `com.pousada.reports` | 5 | 298 |
| `com.pousada.utils` | 1 | 78 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.business
com.pousada.api -> com.pousada.db
com.pousada.api -> com.pousada.models
com.pousada.api -> com.pousada.reports
com.pousada.business -> com.pousada.db
com.pousada.business -> com.pousada.models
com.pousada.business -> com.pousada.utils
com.pousada -> com.pousada.api
com.pousada -> com.pousada.business
com.pousada -> com.pousada.db
com.pousada.db -> com.pousada.models
com.pousada.reports -> com.pousada.db
com.pousada.reports -> com.pousada.models
com.pousada.reports -> com.pousada.utils
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 3 | 0 |
| `com.pousada.api` | 4 | 1 |
| `com.pousada.business` | 3 | 2 |
| `com.pousada.db` | 1 | 4 |
| `com.pousada.models` | 0 | 4 |
| `com.pousada.reports` | 3 | 1 |
| `com.pousada.utils` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim | sim |  |  |  |  |
| `com.pousada.api` | sim |  |  |  |  | com.google |
| `com.pousada.business` |  |  |  |  |  |  |
| `com.pousada.db` |  | sim |  |  |  |  |
| `com.pousada.models` |  |  |  |  |  |  |
| `com.pousada.reports` |  | sim |  |  |  |  |
| `com.pousada.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  316 src/main/java/com/pousada/api/ApiHandlers.java
  191 src/main/java/com/pousada/business/ReservationService.java
  176 src/main/java/com/pousada/db/ReservationRepository.java
  128 src/main/java/com/pousada/models/Reservation.java
  106 src/main/java/com/pousada/db/Database.java
   82 src/main/java/com/pousada/reports/RatePlanReport.java
   78 src/main/java/com/pousada/utils/PricingUtils.java
   73 src/main/java/com/pousada/reports/OccupancyReport.java
   69 src/main/java/com/pousada/Application.java
   64 src/main/java/com/pousada/db/MessageRepository.java
```

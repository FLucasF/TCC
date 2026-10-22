# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 36 |
| `com.pousada.api` | 1 | 298 |
| `com.pousada.database` | 4 | 360 |
| `com.pousada.exception` | 7 | 56 |
| `com.pousada.model` | 7 | 327 |
| `com.pousada.service` | 5 | 324 |
| `com.pousada.service.pricing` | 13 | 267 |
| `com.pousada.util` | 2 | 106 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.database
com.pousada.api -> com.pousada.exception
com.pousada.api -> com.pousada.model
com.pousada.api -> com.pousada.service
com.pousada.api -> com.pousada.util
com.pousada -> com.pousada.api
com.pousada -> com.pousada.database
com.pousada.database -> com.pousada.model
com.pousada.service -> com.pousada.database
com.pousada.service -> com.pousada.exception
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.service.pricing
com.pousada.service -> com.pousada.util
com.pousada.service.pricing -> com.pousada.model
com.pousada.service.pricing -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 5 | 1 |
| `com.pousada.database` | 1 | 3 |
| `com.pousada.exception` | 0 | 2 |
| `com.pousada.model` | 0 | 4 |
| `com.pousada.service` | 5 | 1 |
| `com.pousada.service.pricing` | 2 | 1 |
| `com.pousada.util` | 0 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim | sim | sim |  |  |  |
| `com.pousada.database` |  | sim |  |  | sim |  |
| `com.pousada.exception` |  |  |  |  |  |  |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.service` |  | sim |  |  |  |  |
| `com.pousada.service.pricing` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  298 src/main/java/com/pousada/api/ApiRoutes.java
  187 src/main/java/com/pousada/service/ReservationService.java
  149 src/main/java/com/pousada/database/ReservationRepository.java
  137 src/main/java/com/pousada/service/ReportService.java
  134 src/main/java/com/pousada/model/Reservation.java
   99 src/main/java/com/pousada/database/Database.java
   84 src/main/java/com/pousada/model/Receipt.java
   60 src/main/java/com/pousada/util/PriceCalculator.java
   58 src/main/java/com/pousada/database/MessageRepository.java
   56 src/main/java/com/pousada/exception/ApiException.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 55 |
| `com.pousada.api` | 4 | 469 |
| `com.pousada.domain` | 7 | 266 |
| `com.pousada.repo` | 3 | 353 |
| `com.pousada.service` | 2 | 220 |
| `com.pousada.service.report` | 5 | 349 |
| `com.pousada.util` | 3 | 162 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.domain
com.pousada.api -> com.pousada.repo
com.pousada.api -> com.pousada.service
com.pousada.api -> com.pousada.service.report
com.pousada -> com.pousada.api
com.pousada -> com.pousada.repo
com.pousada -> com.pousada.service
com.pousada -> com.pousada.util
com.pousada.repo -> com.pousada.domain
com.pousada.repo -> com.pousada.util
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.repo
com.pousada.service -> com.pousada.util
com.pousada.service.report -> com.pousada.domain
com.pousada.service.report -> com.pousada.repo
com.pousada.service.report -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.api` | 4 | 1 |
| `com.pousada.domain` | 0 | 4 |
| `com.pousada.repo` | 2 | 4 |
| `com.pousada.service` | 3 | 2 |
| `com.pousada.service.report` | 3 | 1 |
| `com.pousada.util` | 0 | 4 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  | org |
| `com.pousada.api` | sim |  |  |  |  | org |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.repo` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.service.report` |  |  |  |  |  |  |
| `com.pousada.util` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  281 src/main/java/com/pousada/api/ReservationRoutes.java
  214 src/main/java/com/pousada/repo/ReservationRepository.java
  206 src/main/java/com/pousada/service/ReservationService.java
  129 src/main/java/com/pousada/domain/Reservation.java
  117 src/main/java/com/pousada/api/ReportRoutes.java
   91 src/main/java/com/pousada/repo/RoomRepository.java
   85 src/main/java/com/pousada/util/Database.java
   83 src/main/java/com/pousada/service/report/RatePlansReport.java
   79 src/main/java/com/pousada/service/report/TourismTaxReport.java
   74 src/main/java/com/pousada/service/report/OccupancyReport.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 278 |
| `com.pousada.database` | 1 | 79 |
| `com.pousada.domain` | 6 | 136 |
| `com.pousada.report` | 5 | 321 |
| `com.pousada.repository` | 3 | 337 |
| `com.pousada.service` | 6 | 271 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.database
com.pousada -> com.pousada.domain
com.pousada -> com.pousada.report
com.pousada -> com.pousada.repository
com.pousada -> com.pousada.service
com.pousada.report -> com.pousada.domain
com.pousada.report -> com.pousada.repository
com.pousada.repository -> com.pousada.database
com.pousada.repository -> com.pousada.domain
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 5 | 0 |
| `com.pousada.database` | 0 | 2 |
| `com.pousada.domain` | 0 | 4 |
| `com.pousada.report` | 2 | 1 |
| `com.pousada.repository` | 2 | 3 |
| `com.pousada.service` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  | com.google |
| `com.pousada.database` |  | sim |  |  | sim |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.report` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  278 src/main/java/com/pousada/App.java
  212 src/main/java/com/pousada/repository/ReservationRepository.java
  142 src/main/java/com/pousada/service/ReservationService.java
   82 src/main/java/com/pousada/repository/MessageRepository.java
   79 src/main/java/com/pousada/database/Database.java
   77 src/main/java/com/pousada/report/RatePlanReport.java
   72 src/main/java/com/pousada/report/TourismTaxReport.java
   69 src/main/java/com/pousada/report/RevenueReport.java
   68 src/main/java/com/pousada/report/OccupancyReport.java
   59 src/main/java/com/pousada/domain/Reservation.java
```

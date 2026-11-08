# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 72 |
| `com.pousadamarealta.api` | 10 | 467 |
| `com.pousadamarealta.domain` | 7 | 325 |
| `com.pousadamarealta.pricing` | 1 | 65 |
| `com.pousadamarealta.report` | 5 | 345 |
| `com.pousadamarealta.repository` | 4 | 461 |
| `com.pousadamarealta.service` | 10 | 317 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.pricing -> com.pousadamarealta.domain
com.pousadamarealta.report -> com.pousadamarealta.domain
com.pousadamarealta.report -> com.pousadamarealta.repository
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.pricing
com.pousadamarealta.service -> com.pousadamarealta.report
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.pricing` | 1 | 1 |
| `com.pousadamarealta.report` | 2 | 1 |
| `com.pousadamarealta.repository` | 1 | 4 |
| `com.pousadamarealta.service` | 4 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.pricing` |  |  |  |  |  |  |
| `com.pousadamarealta.report` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  | sim |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  266 src/main/java/com/pousadamarealta/api/ReservationController.java
  221 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  192 src/main/java/com/pousadamarealta/domain/Reservation.java
  161 src/main/java/com/pousadamarealta/service/ReservationService.java
   93 src/main/java/com/pousadamarealta/api/ReportController.java
   91 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   89 src/main/java/com/pousadamarealta/report/RatePlanReport.java
   87 src/main/java/com/pousadamarealta/report/TourismTaxReport.java
   84 src/main/java/com/pousadamarealta/repository/DatabaseManager.java
   77 src/main/java/com/pousadamarealta/report/RevenueReport.java
```

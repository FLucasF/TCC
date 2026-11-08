# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 21 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.pousadamarealta` | 1 | 94 |
| `br.com.pousadamarealta.api` | 4 | 461 |
| `br.com.pousadamarealta.database` | 1 | 80 |
| `br.com.pousadamarealta.domain.message` | 3 | 128 |
| `br.com.pousadamarealta.domain.pricing` | 1 | 44 |
| `br.com.pousadamarealta.domain.reservation` | 5 | 651 |
| `br.com.pousadamarealta.domain.room` | 3 | 110 |
| `br.com.pousadamarealta.report` | 5 | 319 |
| `br.com.pousadamarealta.util` | 2 | 63 |

## As dependências entre os pacotes do projeto

```
br.com.pousadamarealta.api -> br.com.pousadamarealta.domain.message
br.com.pousadamarealta.api -> br.com.pousadamarealta.domain.reservation
br.com.pousadamarealta.api -> br.com.pousadamarealta.domain.room
br.com.pousadamarealta.api -> br.com.pousadamarealta.report
br.com.pousadamarealta -> br.com.pousadamarealta.api
br.com.pousadamarealta -> br.com.pousadamarealta.database
br.com.pousadamarealta -> br.com.pousadamarealta.domain.message
br.com.pousadamarealta -> br.com.pousadamarealta.domain.reservation
br.com.pousadamarealta -> br.com.pousadamarealta.domain.room
br.com.pousadamarealta.domain.message -> br.com.pousadamarealta.database
br.com.pousadamarealta.domain.pricing -> br.com.pousadamarealta.domain.reservation
br.com.pousadamarealta.domain.pricing -> br.com.pousadamarealta.domain.room
br.com.pousadamarealta.domain.pricing -> br.com.pousadamarealta.util
br.com.pousadamarealta.domain.reservation -> br.com.pousadamarealta.database
br.com.pousadamarealta.domain.reservation -> br.com.pousadamarealta.domain.message
br.com.pousadamarealta.domain.reservation -> br.com.pousadamarealta.domain.pricing
br.com.pousadamarealta.domain.reservation -> br.com.pousadamarealta.domain.room
br.com.pousadamarealta.domain.reservation -> br.com.pousadamarealta.util
br.com.pousadamarealta.domain.room -> br.com.pousadamarealta.database
br.com.pousadamarealta.report -> br.com.pousadamarealta.domain.reservation
br.com.pousadamarealta.report -> br.com.pousadamarealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.pousadamarealta` | 5 | 0 |
| `br.com.pousadamarealta.api` | 4 | 1 |
| `br.com.pousadamarealta.database` | 0 | 4 |
| `br.com.pousadamarealta.domain.message` | 1 | 3 |
| `br.com.pousadamarealta.domain.pricing` | 3 | 1 |
| `br.com.pousadamarealta.domain.reservation` | 5 | 4 |
| `br.com.pousadamarealta.domain.room` | 1 | 4 |
| `br.com.pousadamarealta.report` | 2 | 1 |
| `br.com.pousadamarealta.util` | 0 | 3 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.pousadamarealta.domain.pricing
br.com.pousadamarealta.domain.reservation
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.pousadamarealta` | sim | sim | sim |  |  |  |
| `br.com.pousadamarealta.api` | sim | sim |  |  |  |  |
| `br.com.pousadamarealta.database` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.domain.message` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.domain.pricing` |  |  |  |  |  |  |
| `br.com.pousadamarealta.domain.reservation` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.domain.room` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.report` |  | sim |  |  |  |  |
| `br.com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  260 src/main/java/br/com/pousadamarealta/api/ReservationController.java
  230 src/main/java/br/com/pousadamarealta/domain/reservation/Reservation.java
  221 src/main/java/br/com/pousadamarealta/domain/reservation/ReservationRepository.java
  173 src/main/java/br/com/pousadamarealta/domain/reservation/ReservationService.java
  128 src/main/java/br/com/pousadamarealta/api/ReportController.java
   94 src/main/java/br/com/pousadamarealta/Main.java
   80 src/main/java/br/com/pousadamarealta/database/DatabaseSetup.java
   79 src/main/java/br/com/pousadamarealta/report/RatePlanReport.java
   75 src/main/java/br/com/pousadamarealta/report/TourismTaxReport.java
   66 src/main/java/br/com/pousadamarealta/report/OccupancyReport.java
```

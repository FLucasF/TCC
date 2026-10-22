# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 97 |
| `com.pousadamarealta.api` | 5 | 377 |
| `com.pousadamarealta.domain` | 7 | 411 |
| `com.pousadamarealta.report` | 5 | 320 |
| `com.pousadamarealta.repository` | 4 | 384 |
| `com.pousadamarealta.service` | 10 | 317 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.report
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.report -> com.pousadamarealta.domain
com.pousadamarealta.report -> com.pousadamarealta.repository
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.api` | 4 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.report` | 2 | 1 |
| `com.pousadamarealta.repository` | 1 | 4 |
| `com.pousadamarealta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.report` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  | sim |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  200 src/main/java/com/pousadamarealta/api/ReservationController.java
  191 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  170 src/main/java/com/pousadamarealta/service/ReservationService.java
  168 src/main/java/com/pousadamarealta/domain/Reservation.java
  115 src/main/java/com/pousadamarealta/domain/Document.java
   97 src/main/java/com/pousadamarealta/Main.java
   87 src/main/java/com/pousadamarealta/api/ReportController.java
   83 src/main/java/com/pousadamarealta/repository/Database.java
   77 src/main/java/com/pousadamarealta/report/RatePlansReportGenerator.java
   70 src/main/java/com/pousadamarealta/report/TourismTaxReportGenerator.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 17 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 116 |
| `com.pousadamarealta.controller` | 4 | 256 |
| `com.pousadamarealta.domain` | 8 | 312 |
| `com.pousadamarealta.dto` | 6 | 159 |
| `com.pousadamarealta.exception` | 7 | 62 |
| `com.pousadamarealta.report` | 5 | 312 |
| `com.pousadamarealta.repository` | 5 | 532 |
| `com.pousadamarealta.service` | 5 | 377 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.dto
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.domain
com.pousadamarealta.controller -> com.pousadamarealta.dto
com.pousadamarealta.controller -> com.pousadamarealta.exception
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.dto -> com.pousadamarealta.domain
com.pousadamarealta.report -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.report
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.controller` | 5 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.dto` | 1 | 2 |
| `com.pousadamarealta.exception` | 0 | 3 |
| `com.pousadamarealta.report` | 1 | 1 |
| `com.pousadamarealta.repository` | 1 | 3 |
| `com.pousadamarealta.service` | 4 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.report` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  | sim |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  267 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  177 src/main/java/com/pousadamarealta/domain/Reservation.java
  151 src/main/java/com/pousadamarealta/service/ReservationService.java
  119 src/main/java/com/pousadamarealta/controller/ReservationController.java
  116 src/main/java/com/pousadamarealta/Application.java
  112 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   89 src/main/java/com/pousadamarealta/controller/ReportController.java
   85 src/main/java/com/pousadamarealta/repository/Database.java
   77 src/main/java/com/pousadamarealta/report/RatePlansReport.java
   72 src/main/java/com/pousadamarealta/report/TourismTaxReport.java
```

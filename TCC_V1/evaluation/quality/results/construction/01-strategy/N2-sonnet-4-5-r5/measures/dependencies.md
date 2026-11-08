# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 18 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 94 |
| `com.pousadamarealta.api` | 4 | 310 |
| `com.pousadamarealta.domain` | 7 | 306 |
| `com.pousadamarealta.exception` | 7 | 62 |
| `com.pousadamarealta.rateplan` | 8 | 334 |
| `com.pousadamarealta.repository` | 3 | 356 |
| `com.pousadamarealta.service` | 3 | 306 |
| `com.pousadamarealta.util` | 2 | 112 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.exception
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.util
com.pousadamarealta.rateplan -> com.pousadamarealta.domain
com.pousadamarealta.rateplan -> com.pousadamarealta.exception
com.pousadamarealta.rateplan -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.util
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.rateplan
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.api` | 4 | 1 |
| `com.pousadamarealta.domain` | 0 | 4 |
| `com.pousadamarealta.exception` | 0 | 4 |
| `com.pousadamarealta.rateplan` | 3 | 1 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 4 | 2 |
| `com.pousadamarealta.util` | 0 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.rateplan` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  223 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  182 src/main/java/com/pousadamarealta/api/ReservationController.java
  170 src/main/java/com/pousadamarealta/domain/Reservation.java
  151 src/main/java/com/pousadamarealta/service/ReservationService.java
   94 src/main/java/com/pousadamarealta/Main.java
   86 src/main/java/com/pousadamarealta/util/DatabaseManager.java
   80 src/main/java/com/pousadamarealta/service/NotificationService.java
   75 src/main/java/com/pousadamarealta/service/ReportService.java
   72 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   69 src/main/java/com/pousadamarealta/rateplan/CorporateRatePlan.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 71 |
| `com.pousadamarealta.api` | 2 | 258 |
| `com.pousadamarealta.api.dto` | 5 | 143 |
| `com.pousadamarealta.domain` | 6 | 343 |
| `com.pousadamarealta.infrastructure` | 4 | 458 |
| `com.pousadamarealta.service` | 9 | 262 |
| `com.pousadamarealta.service.report` | 5 | 302 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.api.dto
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.infrastructure
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta.api.dto -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.infrastructure
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.infrastructure -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.infrastructure
com.pousadamarealta.service -> com.pousadamarealta.service.report
com.pousadamarealta.service.report -> com.pousadamarealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.api` | 4 | 1 |
| `com.pousadamarealta.api.dto` | 1 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.infrastructure` | 1 | 3 |
| `com.pousadamarealta.service` | 3 | 2 |
| `com.pousadamarealta.service.report` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.service.report` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  254 src/main/java/com/pousadamarealta/api/ApiHandlers.java
  239 src/main/java/com/pousadamarealta/infrastructure/ReservationRepository.java
  229 src/main/java/com/pousadamarealta/domain/Reservation.java
  135 src/main/java/com/pousadamarealta/service/ReservationService.java
   90 src/main/java/com/pousadamarealta/infrastructure/Database.java
   84 src/main/java/com/pousadamarealta/service/NotificationService.java
   72 src/main/java/com/pousadamarealta/infrastructure/MessageRepository.java
   71 src/main/java/com/pousadamarealta/Main.java
   70 src/main/java/com/pousadamarealta/service/report/RatePlansReport.java
   62 src/main/java/com/pousadamarealta/service/report/TourismTaxReport.java
```

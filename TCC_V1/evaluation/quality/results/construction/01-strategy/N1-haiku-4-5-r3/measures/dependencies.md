# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 21 |
| `com.pousada.api` | 4 | 327 |
| `com.pousada.api.dto` | 1 | 15 |
| `com.pousada.infrastructure` | 1 | 102 |
| `com.pousada.model` | 8 | 344 |
| `com.pousada.rateplan` | 10 | 304 |
| `com.pousada.repository` | 3 | 329 |
| `com.pousada.service` | 2 | 357 |
| `com.pousada.util` | 2 | 54 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.api.dto
com.pousada.api -> com.pousada.model
com.pousada.api -> com.pousada.rateplan
com.pousada.api -> com.pousada.repository
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.infrastructure
com.pousada.rateplan -> com.pousada.model
com.pousada.rateplan -> com.pousada.util
com.pousada.repository -> com.pousada.infrastructure
com.pousada.repository -> com.pousada.model
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.rateplan
com.pousada.service -> com.pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.api` | 5 | 1 |
| `com.pousada.api.dto` | 0 | 1 |
| `com.pousada.infrastructure` | 0 | 2 |
| `com.pousada.model` | 0 | 4 |
| `com.pousada.rateplan` | 2 | 2 |
| `com.pousada.repository` | 2 | 2 |
| `com.pousada.service` | 3 | 1 |
| `com.pousada.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim |  | sim |  |  |  |
| `com.pousada.api.dto` |  |  |  |  |  |  |
| `com.pousada.infrastructure` |  | sim |  |  | sim |  |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.rateplan` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  275 src/main/java/com/pousada/service/ReservationService.java
  219 src/main/java/com/pousada/api/ReservationController.java
  208 src/main/java/com/pousada/repository/ReservationRepository.java
  170 src/main/java/com/pousada/model/Reservation.java
  102 src/main/java/com/pousada/infrastructure/Database.java
   93 src/main/java/com/pousada/model/Document.java
   82 src/main/java/com/pousada/service/ReportService.java
   66 src/main/java/com/pousada/repository/MessageRepository.java
   60 src/main/java/com/pousada/rateplan/CorporateRatePlan.java
   55 src/main/java/com/pousada/repository/RoomRepository.java
```

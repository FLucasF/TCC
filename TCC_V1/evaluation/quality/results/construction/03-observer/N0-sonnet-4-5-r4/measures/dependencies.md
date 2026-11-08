# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 10 pacotes, 22 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 96 |
| `com.pousadamarealta.api` | 4 | 249 |
| `com.pousadamarealta.api.dto` | 6 | 146 |
| `com.pousadamarealta.config` | 1 | 78 |
| `com.pousadamarealta.domain.event` | 5 | 45 |
| `com.pousadamarealta.domain.event.handlers` | 3 | 171 |
| `com.pousadamarealta.domain.model` | 6 | 284 |
| `com.pousadamarealta.domain.service` | 10 | 383 |
| `com.pousadamarealta.domain.util` | 4 | 86 |
| `com.pousadamarealta.repository` | 3 | 329 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.api.dto
com.pousadamarealta.api -> com.pousadamarealta.domain.model
com.pousadamarealta.api -> com.pousadamarealta.domain.service
com.pousadamarealta.api.dto -> com.pousadamarealta.domain.model
com.pousadamarealta.api.dto -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta -> com.pousadamarealta.domain.event
com.pousadamarealta -> com.pousadamarealta.domain.event.handlers
com.pousadamarealta -> com.pousadamarealta.domain.service
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta.domain.event -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.event.handlers -> com.pousadamarealta.domain.event
com.pousadamarealta.domain.event.handlers -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.event.handlers -> com.pousadamarealta.domain.service
com.pousadamarealta.domain.event.handlers -> com.pousadamarealta.domain.util
com.pousadamarealta.domain.service -> com.pousadamarealta.domain.event
com.pousadamarealta.domain.service -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.service -> com.pousadamarealta.repository
com.pousadamarealta.domain.util -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.util -> com.pousadamarealta.repository
com.pousadamarealta.repository -> com.pousadamarealta.domain.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.api.dto` | 2 | 1 |
| `com.pousadamarealta.config` | 0 | 1 |
| `com.pousadamarealta.domain.event` | 1 | 3 |
| `com.pousadamarealta.domain.event.handlers` | 4 | 1 |
| `com.pousadamarealta.domain.model` | 0 | 7 |
| `com.pousadamarealta.domain.service` | 3 | 3 |
| `com.pousadamarealta.domain.util` | 2 | 1 |
| `com.pousadamarealta.repository` | 1 | 4 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.api.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.config` |  | sim |  |  | sim |  |
| `com.pousadamarealta.domain.event` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.event.handlers` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.domain.util` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  188 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  176 src/main/java/com/pousadamarealta/domain/service/ReservationService.java
  156 src/main/java/com/pousadamarealta/domain/model/Reservation.java
  156 src/main/java/com/pousadamarealta/api/ReservationController.java
   96 src/main/java/com/pousadamarealta/Main.java
   85 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   79 src/main/java/com/pousadamarealta/domain/service/ReportService.java
   78 src/main/java/com/pousadamarealta/config/Database.java
   67 src/main/java/com/pousadamarealta/domain/event/handlers/FinanceNotificationHandler.java
   66 src/main/java/com/pousadamarealta/domain/service/PriceCalculator.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 50 |
| `com.pousada.marealta.api` | 5 | 399 |
| `com.pousada.marealta.config` | 3 | 129 |
| `com.pousada.marealta.domain` | 7 | 305 |
| `com.pousada.marealta.messaging` | 3 | 86 |
| `com.pousada.marealta.messaging.processors` | 3 | 99 |
| `com.pousada.marealta.repository` | 3 | 303 |
| `com.pousada.marealta.service` | 4 | 416 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta.api -> com.pousada.marealta.domain
com.pousada.marealta.api -> com.pousada.marealta.repository
com.pousada.marealta.api -> com.pousada.marealta.service
com.pousada.marealta -> com.pousada.marealta.api
com.pousada.marealta -> com.pousada.marealta.config
com.pousada.marealta -> com.pousada.marealta.repository
com.pousada.marealta.messaging -> com.pousada.marealta.domain
com.pousada.marealta.messaging.processors -> com.pousada.marealta.domain
com.pousada.marealta.messaging.processors -> com.pousada.marealta.messaging
com.pousada.marealta.repository -> com.pousada.marealta.config
com.pousada.marealta.repository -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.config
com.pousada.marealta.service -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.messaging
com.pousada.marealta.service -> com.pousada.marealta.messaging.processors
com.pousada.marealta.service -> com.pousada.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 3 | 0 |
| `com.pousada.marealta.api` | 3 | 1 |
| `com.pousada.marealta.config` | 0 | 3 |
| `com.pousada.marealta.domain` | 0 | 5 |
| `com.pousada.marealta.messaging` | 1 | 2 |
| `com.pousada.marealta.messaging.processors` | 2 | 1 |
| `com.pousada.marealta.repository` | 2 | 3 |
| `com.pousada.marealta.service` | 5 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.api` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.config` |  | sim |  |  |  |  |
| `com.pousada.marealta.domain` |  |  |  |  |  |  |
| `com.pousada.marealta.messaging` |  |  |  |  |  |  |
| `com.pousada.marealta.messaging.processors` |  |  |  |  |  |  |
| `com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  212 src/main/java/com/pousada/marealta/service/ReservationService.java
  208 src/main/java/com/pousada/marealta/repository/ReservationRepository.java
  195 src/main/java/com/pousada/marealta/api/ReservationsHandler.java
  145 src/main/java/com/pousada/marealta/domain/Reservation.java
   97 src/main/java/com/pousada/marealta/service/ReportService.java
   75 src/main/java/com/pousada/marealta/api/Handler.java
   71 src/main/java/com/pousada/marealta/config/DatabaseMigration.java
   63 src/main/java/com/pousada/marealta/messaging/MessageData.java
   56 src/main/java/com/pousada/marealta/service/PriceCalculator.java
   53 src/main/java/com/pousada/marealta/repository/MessageRepository.java
```

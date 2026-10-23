# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 1 | 57 |
| `com.marealta.api` | 4 | 455 |
| `com.marealta.api.dto` | 2 | 30 |
| `com.marealta.domain.model` | 11 | 332 |
| `com.marealta.domain.service` | 5 | 348 |
| `com.marealta.infrastructure.database` | 3 | 374 |
| `com.marealta.infrastructure.messaging` | 1 | 58 |

## As dependências entre os pacotes do projeto

```
com.marealta.api -> com.marealta.api.dto
com.marealta.api -> com.marealta.domain.model
com.marealta.api -> com.marealta.domain.service
com.marealta.api -> com.marealta.infrastructure.database
com.marealta.api -> com.marealta.infrastructure.messaging
com.marealta -> com.marealta.api
com.marealta -> com.marealta.infrastructure.database
com.marealta -> com.marealta.infrastructure.messaging
com.marealta.domain.service -> com.marealta.domain.model
com.marealta.domain.service -> com.marealta.infrastructure.messaging
com.marealta.infrastructure.database -> com.marealta.domain.model
com.marealta.infrastructure.messaging -> com.marealta.domain.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 3 | 0 |
| `com.marealta.api` | 5 | 1 |
| `com.marealta.api.dto` | 0 | 1 |
| `com.marealta.domain.model` | 0 | 4 |
| `com.marealta.domain.service` | 2 | 1 |
| `com.marealta.infrastructure.database` | 1 | 2 |
| `com.marealta.infrastructure.messaging` | 1 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim | sim | sim |  |  |  |
| `com.marealta.api` | sim |  | sim |  |  |  |
| `com.marealta.api.dto` |  |  | sim |  |  |  |
| `com.marealta.domain.model` |  |  |  |  |  |  |
| `com.marealta.domain.service` |  | sim |  |  |  |  |
| `com.marealta.infrastructure.database` |  | sim |  |  |  |  |
| `com.marealta.infrastructure.messaging` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  337 src/main/java/com/marealta/api/ApiController.java
  231 src/main/java/com/marealta/infrastructure/database/ReservationRepository.java
  147 src/main/java/com/marealta/domain/model/Reservation.java
  138 src/main/java/com/marealta/domain/service/DocumentService.java
  103 src/main/java/com/marealta/api/ValidationService.java
   90 src/main/java/com/marealta/domain/service/ReportService.java
   85 src/main/java/com/marealta/domain/service/PricingService.java
   72 src/main/java/com/marealta/infrastructure/database/Database.java
   71 src/main/java/com/marealta/infrastructure/database/RoomRepository.java
   58 src/main/java/com/marealta/infrastructure/messaging/MessageRepository.java
```

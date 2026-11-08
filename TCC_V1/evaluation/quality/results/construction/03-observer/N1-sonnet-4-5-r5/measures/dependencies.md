# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 12 pacotes, 32 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 1 | 131 |
| `com.marealta.api.controller` | 4 | 227 |
| `com.marealta.api.dto` | 5 | 143 |
| `com.marealta.api.error` | 3 | 46 |
| `com.marealta.application.handler` | 5 | 263 |
| `com.marealta.domain.event` | 3 | 20 |
| `com.marealta.domain.model` | 5 | 213 |
| `com.marealta.domain.repository` | 2 | 27 |
| `com.marealta.domain.service` | 9 | 334 |
| `com.marealta.infrastructure.database` | 4 | 489 |
| `com.marealta.infrastructure.messaging` | 3 | 58 |
| `com.marealta.infrastructure.notification` | 2 | 33 |

## As dependências entre os pacotes do projeto

```
com.marealta.api.controller -> com.marealta.api.dto
com.marealta.api.controller -> com.marealta.api.error
com.marealta.api.controller -> com.marealta.domain.model
com.marealta.api.controller -> com.marealta.domain.repository
com.marealta.api.controller -> com.marealta.domain.service
com.marealta.api.controller -> com.marealta.infrastructure.messaging
com.marealta.api.dto -> com.marealta.domain.model
com.marealta.api.dto -> com.marealta.infrastructure.messaging
com.marealta.api.error -> com.marealta.domain.service
com.marealta.application.handler -> com.marealta.domain.event
com.marealta.application.handler -> com.marealta.domain.model
com.marealta.application.handler -> com.marealta.infrastructure.messaging
com.marealta.application.handler -> com.marealta.infrastructure.notification
com.marealta -> com.marealta.api.controller
com.marealta -> com.marealta.api.error
com.marealta -> com.marealta.application.handler
com.marealta -> com.marealta.domain.event
com.marealta -> com.marealta.domain.repository
com.marealta -> com.marealta.domain.service
com.marealta -> com.marealta.infrastructure.database
com.marealta -> com.marealta.infrastructure.messaging
com.marealta -> com.marealta.infrastructure.notification
com.marealta.domain.event -> com.marealta.domain.model
com.marealta.domain.repository -> com.marealta.domain.model
com.marealta.domain.service -> com.marealta.domain.event
com.marealta.domain.service -> com.marealta.domain.model
com.marealta.domain.service -> com.marealta.domain.repository
com.marealta.domain.service -> com.marealta.infrastructure.notification
com.marealta.infrastructure.database -> com.marealta.domain.model
com.marealta.infrastructure.database -> com.marealta.domain.repository
com.marealta.infrastructure.database -> com.marealta.infrastructure.messaging
com.marealta.infrastructure.notification -> com.marealta.domain.event
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 9 | 0 |
| `com.marealta.api.controller` | 6 | 1 |
| `com.marealta.api.dto` | 2 | 1 |
| `com.marealta.api.error` | 1 | 2 |
| `com.marealta.application.handler` | 4 | 1 |
| `com.marealta.domain.event` | 1 | 4 |
| `com.marealta.domain.model` | 0 | 7 |
| `com.marealta.domain.repository` | 1 | 4 |
| `com.marealta.domain.service` | 4 | 3 |
| `com.marealta.infrastructure.database` | 3 | 1 |
| `com.marealta.infrastructure.messaging` | 0 | 5 |
| `com.marealta.infrastructure.notification` | 1 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim | sim | sim |  |  |  |
| `com.marealta.api.controller` | sim |  |  |  |  |  |
| `com.marealta.api.dto` |  |  |  |  |  |  |
| `com.marealta.api.error` | sim |  |  |  |  |  |
| `com.marealta.application.handler` |  |  |  |  |  |  |
| `com.marealta.domain.event` |  |  |  |  |  |  |
| `com.marealta.domain.model` |  |  |  |  |  |  |
| `com.marealta.domain.repository` |  |  |  |  |  |  |
| `com.marealta.domain.service` |  |  |  |  |  |  |
| `com.marealta.infrastructure.database` |  | sim |  |  |  |  |
| `com.marealta.infrastructure.messaging` |  |  |  |  |  |  |
| `com.marealta.infrastructure.notification` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  271 src/main/java/com/marealta/infrastructure/database/JdbcReservationRepository.java
  183 src/main/java/com/marealta/domain/service/ReservationService.java
  144 src/main/java/com/marealta/domain/model/Reservation.java
  132 src/main/java/com/marealta/api/controller/ReservationController.java
  131 src/main/java/com/marealta/Main.java
   90 src/main/java/com/marealta/application/handler/GuestNotificationHandler.java
   84 src/main/java/com/marealta/domain/service/ReportService.java
   83 src/main/java/com/marealta/infrastructure/database/DatabaseInitializer.java
   70 src/main/java/com/marealta/infrastructure/database/JdbcRoomRepository.java
   67 src/main/java/com/marealta/domain/service/PricingService.java
```

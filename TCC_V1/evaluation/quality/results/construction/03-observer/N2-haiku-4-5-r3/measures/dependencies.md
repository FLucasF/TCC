# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 14 pacotes, 48 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 51 |
| `com.pousada.marealta.application.reservation` | 3 | 238 |
| `com.pousada.marealta.domain.notification` | 5 | 184 |
| `com.pousada.marealta.domain.pricing` | 3 | 79 |
| `com.pousada.marealta.domain.repository` | 3 | 40 |
| `com.pousada.marealta.domain.reservation` | 2 | 209 |
| `com.pousada.marealta.domain.room` | 2 | 92 |
| `com.pousada.marealta.domain.shared` | 4 | 246 |
| `com.pousada.marealta.infrastructure.database` | 1 | 47 |
| `com.pousada.marealta.infrastructure.notification` | 3 | 242 |
| `com.pousada.marealta.infrastructure.persistence.postgres` | 3 | 482 |
| `com.pousada.marealta.infrastructure.web` | 1 | 102 |
| `com.pousada.marealta.infrastructure.web.dto` | 5 | 138 |
| `com.pousada.marealta.infrastructure.web.handler` | 4 | 368 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta.application.reservation -> com.pousada.marealta.domain.notification
com.pousada.marealta.application.reservation -> com.pousada.marealta.domain.pricing
com.pousada.marealta.application.reservation -> com.pousada.marealta.domain.repository
com.pousada.marealta.application.reservation -> com.pousada.marealta.domain.reservation
com.pousada.marealta.application.reservation -> com.pousada.marealta.domain.room
com.pousada.marealta.application.reservation -> com.pousada.marealta.domain.shared
com.pousada.marealta -> com.pousada.marealta.domain.repository
com.pousada.marealta -> com.pousada.marealta.infrastructure.database
com.pousada.marealta -> com.pousada.marealta.infrastructure.persistence.postgres
com.pousada.marealta -> com.pousada.marealta.infrastructure.web
com.pousada.marealta.domain.notification -> com.pousada.marealta.domain.reservation
com.pousada.marealta.domain.notification -> com.pousada.marealta.domain.shared
com.pousada.marealta.domain.pricing -> com.pousada.marealta.domain.shared
com.pousada.marealta.domain.repository -> com.pousada.marealta.domain.notification
com.pousada.marealta.domain.repository -> com.pousada.marealta.domain.reservation
com.pousada.marealta.domain.repository -> com.pousada.marealta.domain.room
com.pousada.marealta.domain.repository -> com.pousada.marealta.domain.shared
com.pousada.marealta.domain.reservation -> com.pousada.marealta.domain.pricing
com.pousada.marealta.domain.reservation -> com.pousada.marealta.domain.room
com.pousada.marealta.domain.reservation -> com.pousada.marealta.domain.shared
com.pousada.marealta.domain.room -> com.pousada.marealta.domain.shared
com.pousada.marealta.infrastructure.notification -> com.pousada.marealta.domain.notification
com.pousada.marealta.infrastructure.notification -> com.pousada.marealta.domain.reservation
com.pousada.marealta.infrastructure.notification -> com.pousada.marealta.domain.room
com.pousada.marealta.infrastructure.notification -> com.pousada.marealta.domain.shared
com.pousada.marealta.infrastructure.persistence.postgres -> com.pousada.marealta.domain.notification
com.pousada.marealta.infrastructure.persistence.postgres -> com.pousada.marealta.domain.pricing
com.pousada.marealta.infrastructure.persistence.postgres -> com.pousada.marealta.domain.repository
com.pousada.marealta.infrastructure.persistence.postgres -> com.pousada.marealta.domain.reservation
com.pousada.marealta.infrastructure.persistence.postgres -> com.pousada.marealta.domain.room
com.pousada.marealta.infrastructure.persistence.postgres -> com.pousada.marealta.domain.shared
com.pousada.marealta.infrastructure.web -> com.pousada.marealta.application.reservation
com.pousada.marealta.infrastructure.web -> com.pousada.marealta.domain.notification
com.pousada.marealta.infrastructure.web -> com.pousada.marealta.domain.repository
com.pousada.marealta.infrastructure.web -> com.pousada.marealta.infrastructure.notification
com.pousada.marealta.infrastructure.web -> com.pousada.marealta.infrastructure.web.handler
com.pousada.marealta.infrastructure.web.dto -> com.pousada.marealta.domain.notification
com.pousada.marealta.infrastructure.web.dto -> com.pousada.marealta.domain.pricing
com.pousada.marealta.infrastructure.web.dto -> com.pousada.marealta.domain.reservation
com.pousada.marealta.infrastructure.web.dto -> com.pousada.marealta.domain.room
com.pousada.marealta.infrastructure.web.dto -> com.pousada.marealta.domain.shared
com.pousada.marealta.infrastructure.web.handler -> com.pousada.marealta.application.reservation
com.pousada.marealta.infrastructure.web.handler -> com.pousada.marealta.domain.notification
com.pousada.marealta.infrastructure.web.handler -> com.pousada.marealta.domain.repository
com.pousada.marealta.infrastructure.web.handler -> com.pousada.marealta.domain.reservation
com.pousada.marealta.infrastructure.web.handler -> com.pousada.marealta.domain.room
com.pousada.marealta.infrastructure.web.handler -> com.pousada.marealta.domain.shared
com.pousada.marealta.infrastructure.web.handler -> com.pousada.marealta.infrastructure.web.dto
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 4 | 0 |
| `com.pousada.marealta.application.reservation` | 6 | 2 |
| `com.pousada.marealta.domain.notification` | 2 | 7 |
| `com.pousada.marealta.domain.pricing` | 1 | 4 |
| `com.pousada.marealta.domain.repository` | 4 | 5 |
| `com.pousada.marealta.domain.reservation` | 3 | 7 |
| `com.pousada.marealta.domain.room` | 1 | 7 |
| `com.pousada.marealta.domain.shared` | 0 | 10 |
| `com.pousada.marealta.infrastructure.database` | 0 | 1 |
| `com.pousada.marealta.infrastructure.notification` | 4 | 1 |
| `com.pousada.marealta.infrastructure.persistence.postgres` | 6 | 1 |
| `com.pousada.marealta.infrastructure.web` | 5 | 1 |
| `com.pousada.marealta.infrastructure.web.dto` | 5 | 1 |
| `com.pousada.marealta.infrastructure.web.handler` | 7 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` |  | sim |  |  |  |  |
| `com.pousada.marealta.application.reservation` |  |  |  |  |  |  |
| `com.pousada.marealta.domain.notification` |  |  |  |  |  |  |
| `com.pousada.marealta.domain.pricing` |  |  |  |  |  |  |
| `com.pousada.marealta.domain.repository` |  |  |  |  |  |  |
| `com.pousada.marealta.domain.reservation` |  |  |  |  |  |  |
| `com.pousada.marealta.domain.room` |  |  |  |  |  |  |
| `com.pousada.marealta.domain.shared` |  |  |  |  |  |  |
| `com.pousada.marealta.infrastructure.database` |  | sim |  | sim | sim |  |
| `com.pousada.marealta.infrastructure.notification` |  |  |  |  |  |  |
| `com.pousada.marealta.infrastructure.persistence.postgres` |  | sim |  |  |  |  |
| `com.pousada.marealta.infrastructure.web` | sim |  |  |  |  |  |
| `com.pousada.marealta.infrastructure.web.dto` |  |  |  |  |  |  |
| `com.pousada.marealta.infrastructure.web.handler` | sim |  |  |  |  | com.google |

## As maiores classes (linhas, src/main)

```
  291 src/main/java/com/pousada/marealta/infrastructure/persistence/postgres/PostgresReservationRepository.java
  200 src/main/java/com/pousada/marealta/infrastructure/web/handler/ReservationsHandler.java
  181 src/main/java/com/pousada/marealta/domain/reservation/Reservation.java
  181 src/main/java/com/pousada/marealta/application/reservation/ReservationService.java
  120 src/main/java/com/pousada/marealta/infrastructure/web/handler/ReportsHandler.java
  119 src/main/java/com/pousada/marealta/domain/notification/PersistedNotification.java
  104 src/main/java/com/pousada/marealta/infrastructure/persistence/postgres/PostgresNotificationRepository.java
  104 src/main/java/com/pousada/marealta/infrastructure/notification/GuestNotifier.java
  102 src/main/java/com/pousada/marealta/infrastructure/web/GuesthouseApi.java
   87 src/main/java/com/pousada/marealta/infrastructure/persistence/postgres/PostgresRoomRepository.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 55 |
| `com.pousada.marealta.api` | 4 | 374 |
| `com.pousada.marealta.domain` | 8 | 304 |
| `com.pousada.marealta.infrastructure.messaging` | 6 | 139 |
| `com.pousada.marealta.infrastructure.persistence` | 4 | 476 |
| `com.pousada.marealta.service` | 4 | 313 |
| `com.pousada.marealta.util` | 5 | 114 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta.api -> com.pousada.marealta.domain
com.pousada.marealta.api -> com.pousada.marealta.infrastructure.persistence
com.pousada.marealta.api -> com.pousada.marealta.service
com.pousada.marealta.api -> com.pousada.marealta.util
com.pousada.marealta -> com.pousada.marealta.api
com.pousada.marealta -> com.pousada.marealta.infrastructure.messaging
com.pousada.marealta -> com.pousada.marealta.infrastructure.persistence
com.pousada.marealta -> com.pousada.marealta.service
com.pousada.marealta.infrastructure.messaging -> com.pousada.marealta.domain
com.pousada.marealta.infrastructure.messaging -> com.pousada.marealta.infrastructure.persistence
com.pousada.marealta.infrastructure.messaging -> com.pousada.marealta.util
com.pousada.marealta.infrastructure.persistence -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.infrastructure.messaging
com.pousada.marealta.service -> com.pousada.marealta.infrastructure.persistence
com.pousada.marealta.service -> com.pousada.marealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 4 | 0 |
| `com.pousada.marealta.api` | 4 | 1 |
| `com.pousada.marealta.domain` | 0 | 4 |
| `com.pousada.marealta.infrastructure.messaging` | 3 | 2 |
| `com.pousada.marealta.infrastructure.persistence` | 1 | 4 |
| `com.pousada.marealta.service` | 4 | 2 |
| `com.pousada.marealta.util` | 0 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.api` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.domain` |  |  |  |  |  |  |
| `com.pousada.marealta.infrastructure.messaging` |  |  |  |  |  |  |
| `com.pousada.marealta.infrastructure.persistence` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |
| `com.pousada.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  242 src/main/java/com/pousada/marealta/api/ReservationController.java
  221 src/main/java/com/pousada/marealta/infrastructure/persistence/ReservationRepository.java
  197 src/main/java/com/pousada/marealta/service/ReservationService.java
  124 src/main/java/com/pousada/marealta/domain/Reservation.java
  110 src/main/java/com/pousada/marealta/infrastructure/persistence/Database.java
   92 src/main/java/com/pousada/marealta/infrastructure/persistence/MessageRepository.java
   69 src/main/java/com/pousada/marealta/service/ReportService.java
   63 src/main/java/com/pousada/marealta/infrastructure/messaging/MessageSender.java
   57 src/main/java/com/pousada/marealta/api/ReportController.java
   55 src/main/java/com/pousada/marealta/Application.java
```

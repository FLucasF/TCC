# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 20 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 29 |
| `com.pousada.marealta.config` | 1 | 92 |
| `com.pousada.marealta.controller` | 13 | 478 |
| `com.pousada.marealta.domain` | 9 | 222 |
| `com.pousada.marealta.notification` | 5 | 199 |
| `com.pousada.marealta.repository` | 3 | 296 |
| `com.pousada.marealta.service` | 8 | 323 |
| `com.pousada.marealta.util` | 4 | 81 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.config
com.pousada.marealta -> com.pousada.marealta.controller
com.pousada.marealta -> com.pousada.marealta.notification
com.pousada.marealta -> com.pousada.marealta.repository
com.pousada.marealta.controller -> com.pousada.marealta.domain
com.pousada.marealta.controller -> com.pousada.marealta.notification
com.pousada.marealta.controller -> com.pousada.marealta.repository
com.pousada.marealta.controller -> com.pousada.marealta.service
com.pousada.marealta.controller -> com.pousada.marealta.util
com.pousada.marealta.notification -> com.pousada.marealta.domain
com.pousada.marealta.notification -> com.pousada.marealta.repository
com.pousada.marealta.notification -> com.pousada.marealta.util
com.pousada.marealta.repository -> com.pousada.marealta.config
com.pousada.marealta.repository -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.notification
com.pousada.marealta.service -> com.pousada.marealta.repository
com.pousada.marealta.service -> com.pousada.marealta.util
com.pousada.marealta.util -> com.pousada.marealta.domain
com.pousada.marealta.util -> com.pousada.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 4 | 0 |
| `com.pousada.marealta.config` | 0 | 2 |
| `com.pousada.marealta.controller` | 5 | 1 |
| `com.pousada.marealta.domain` | 0 | 5 |
| `com.pousada.marealta.notification` | 3 | 3 |
| `com.pousada.marealta.repository` | 2 | 5 |
| `com.pousada.marealta.service` | 4 | 1 |
| `com.pousada.marealta.util` | 2 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.config` |  | sim |  |  |  |  |
| `com.pousada.marealta.controller` | sim | sim |  |  |  | com.google |
| `com.pousada.marealta.domain` |  |  |  |  |  |  |
| `com.pousada.marealta.notification` |  | sim |  |  |  |  |
| `com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  | sim |  |  |  |  |
| `com.pousada.marealta.util` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  288 src/main/java/com/pousada/marealta/controller/ReservationController.java
  254 src/main/java/com/pousada/marealta/service/ReservationService.java
  187 src/main/java/com/pousada/marealta/repository/ReservationRepository.java
  133 src/main/java/com/pousada/marealta/controller/ReportController.java
  108 src/main/java/com/pousada/marealta/domain/Reservation.java
   92 src/main/java/com/pousada/marealta/config/DatabaseConfig.java
   69 src/main/java/com/pousada/marealta/service/PricingService.java
   59 src/main/java/com/pousada/marealta/repository/MessageRepository.java
   59 src/main/java/com/pousada/marealta/notification/GovernanceNotificationListener.java
   50 src/main/java/com/pousada/marealta/repository/RoomRepository.java
```

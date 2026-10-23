# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 18 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 92 |
| `com.pousada.marealta.config` | 2 | 111 |
| `com.pousada.marealta.controller` | 4 | 267 |
| `com.pousada.marealta.domain` | 7 | 309 |
| `com.pousada.marealta.dto` | 6 | 151 |
| `com.pousada.marealta.messaging` | 2 | 57 |
| `com.pousada.marealta.messaging.rules` | 3 | 173 |
| `com.pousada.marealta.repository` | 3 | 392 |
| `com.pousada.marealta.service` | 10 | 425 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.config
com.pousada.marealta -> com.pousada.marealta.controller
com.pousada.marealta -> com.pousada.marealta.messaging
com.pousada.marealta -> com.pousada.marealta.messaging.rules
com.pousada.marealta -> com.pousada.marealta.repository
com.pousada.marealta -> com.pousada.marealta.service
com.pousada.marealta.controller -> com.pousada.marealta.domain
com.pousada.marealta.controller -> com.pousada.marealta.dto
com.pousada.marealta.controller -> com.pousada.marealta.service
com.pousada.marealta.dto -> com.pousada.marealta.domain
com.pousada.marealta.messaging -> com.pousada.marealta.domain
com.pousada.marealta.messaging.rules -> com.pousada.marealta.domain
com.pousada.marealta.messaging.rules -> com.pousada.marealta.messaging
com.pousada.marealta.repository -> com.pousada.marealta.config
com.pousada.marealta.repository -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.domain
com.pousada.marealta.service -> com.pousada.marealta.messaging
com.pousada.marealta.service -> com.pousada.marealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 6 | 0 |
| `com.pousada.marealta.config` | 0 | 2 |
| `com.pousada.marealta.controller` | 3 | 1 |
| `com.pousada.marealta.domain` | 0 | 6 |
| `com.pousada.marealta.dto` | 1 | 1 |
| `com.pousada.marealta.messaging` | 1 | 3 |
| `com.pousada.marealta.messaging.rules` | 2 | 1 |
| `com.pousada.marealta.repository` | 2 | 2 |
| `com.pousada.marealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.config` |  | sim |  |  |  |  |
| `com.pousada.marealta.controller` | sim |  |  |  |  |  |
| `com.pousada.marealta.domain` |  |  |  |  |  |  |
| `com.pousada.marealta.dto` |  |  | sim |  |  |  |
| `com.pousada.marealta.messaging` |  |  |  |  |  |  |
| `com.pousada.marealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  252 src/main/java/com/pousada/marealta/repository/ReservationRepository.java
  233 src/main/java/com/pousada/marealta/service/ReservationService.java
  178 src/main/java/com/pousada/marealta/domain/Reservation.java
  174 src/main/java/com/pousada/marealta/controller/ReservationController.java
   92 src/main/java/com/pousada/marealta/App.java
   87 src/main/java/com/pousada/marealta/config/Database.java
   80 src/main/java/com/pousada/marealta/service/ReportService.java
   73 src/main/java/com/pousada/marealta/repository/MessageRepository.java
   71 src/main/java/com/pousada/marealta/messaging/rules/ValidateEmailRule.java
   70 src/main/java/com/pousada/marealta/domain/Message.java
```

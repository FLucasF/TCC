# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 22 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 109 |
| `com.pousadamarealta.api` | 4 | 297 |
| `com.pousadamarealta.api.dto` | 6 | 129 |
| `com.pousadamarealta.database` | 1 | 70 |
| `com.pousadamarealta.domain` | 9 | 348 |
| `com.pousadamarealta.messaging` | 4 | 149 |
| `com.pousadamarealta.messaging.rules` | 3 | 98 |
| `com.pousadamarealta.repository` | 3 | 393 |
| `com.pousadamarealta.service` | 9 | 270 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.api.dto
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.messaging
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta.api.dto -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.messaging -> com.pousadamarealta.domain
com.pousadamarealta.messaging -> com.pousadamarealta.messaging.rules
com.pousadamarealta.messaging -> com.pousadamarealta.repository
com.pousadamarealta.messaging.rules -> com.pousadamarealta.domain
com.pousadamarealta.messaging.rules -> com.pousadamarealta.messaging
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.api` | 5 | 1 |
| `com.pousadamarealta.api.dto` | 1 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.domain` | 0 | 7 |
| `com.pousadamarealta.messaging` | 3 | 4 |
| `com.pousadamarealta.messaging.rules` | 2 | 1 |
| `com.pousadamarealta.repository` | 2 | 4 |
| `com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.messaging.rules
com.pousadamarealta.messaging
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.api.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  237 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  195 src/main/java/com/pousadamarealta/api/ReservationController.java
  159 src/main/java/com/pousadamarealta/domain/Reservation.java
  132 src/main/java/com/pousadamarealta/service/ReservationService.java
  109 src/main/java/com/pousadamarealta/Main.java
   88 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   77 src/main/java/com/pousadamarealta/service/ReportService.java
   73 src/main/java/com/pousadamarealta/messaging/OutgoingMessage.java
   70 src/main/java/com/pousadamarealta/database/DatabaseManager.java
   68 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```

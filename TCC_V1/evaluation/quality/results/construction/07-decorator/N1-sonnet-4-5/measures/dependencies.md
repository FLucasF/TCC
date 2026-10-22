# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 109 |
| `com.pousadamarealta.domain` | 8 | 379 |
| `com.pousadamarealta.messaging` | 7 | 189 |
| `com.pousadamarealta.repository` | 4 | 459 |
| `com.pousadamarealta.service` | 8 | 321 |
| `com.pousadamarealta.web` | 4 | 277 |
| `com.pousadamarealta.web.dto` | 6 | 155 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.web
com.pousadamarealta.messaging -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.web -> com.pousadamarealta.domain
com.pousadamarealta.web -> com.pousadamarealta.repository
com.pousadamarealta.web -> com.pousadamarealta.service
com.pousadamarealta.web -> com.pousadamarealta.web.dto
com.pousadamarealta.web.dto -> com.pousadamarealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.messaging` | 1 | 2 |
| `com.pousadamarealta.repository` | 1 | 3 |
| `com.pousadamarealta.service` | 3 | 2 |
| `com.pousadamarealta.web` | 4 | 1 |
| `com.pousadamarealta.web.dto` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.web` | sim |  |  |  |  |  |
| `com.pousadamarealta.web.dto` |  |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  233 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  232 src/main/java/com/pousadamarealta/domain/Reservation.java
  207 src/main/java/com/pousadamarealta/service/ReservationService.java
  178 src/main/java/com/pousadamarealta/web/ReservationController.java
  109 src/main/java/com/pousadamarealta/App.java
   81 src/main/java/com/pousadamarealta/repository/DatabaseInitializer.java
   78 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   76 src/main/java/com/pousadamarealta/service/ReportService.java
   69 src/main/java/com/pousadamarealta/messaging/ProcessedMessage.java
   67 src/main/java/com/pousadamarealta/repository/RoomRepository.java
```

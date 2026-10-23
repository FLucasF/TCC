# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 18 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 15 |
| `com.pousadamarealta.config` | 1 | 85 |
| `com.pousadamarealta.domain` | 9 | 471 |
| `com.pousadamarealta.messaging` | 5 | 160 |
| `com.pousadamarealta.messaging.rules` | 3 | 91 |
| `com.pousadamarealta.repository` | 4 | 470 |
| `com.pousadamarealta.service` | 9 | 259 |
| `com.pousadamarealta.web` | 1 | 232 |
| `com.pousadamarealta.web.dto` | 6 | 154 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta.config -> com.pousadamarealta.domain
com.pousadamarealta.config -> com.pousadamarealta.messaging
com.pousadamarealta.config -> com.pousadamarealta.messaging.rules
com.pousadamarealta.config -> com.pousadamarealta.repository
com.pousadamarealta.config -> com.pousadamarealta.service
com.pousadamarealta.config -> com.pousadamarealta.web
com.pousadamarealta.messaging -> com.pousadamarealta.domain
com.pousadamarealta.messaging.rules -> com.pousadamarealta.messaging
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
| `com.pousadamarealta` | 1 | 0 |
| `com.pousadamarealta.config` | 6 | 1 |
| `com.pousadamarealta.domain` | 0 | 6 |
| `com.pousadamarealta.messaging` | 1 | 3 |
| `com.pousadamarealta.messaging.rules` | 1 | 1 |
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
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.config` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  | sim |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.web` | sim |  | sim |  |  |  |
| `com.pousadamarealta.web.dto` |  |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  237 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  232 src/main/java/com/pousadamarealta/web/WebApplication.java
  222 src/main/java/com/pousadamarealta/domain/Reservation.java
  152 src/main/java/com/pousadamarealta/service/ReservationService.java
  110 src/main/java/com/pousadamarealta/domain/ReservationDocument.java
   87 src/main/java/com/pousadamarealta/repository/DatabaseConnection.java
   85 src/main/java/com/pousadamarealta/config/Configuration.java
   77 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   71 src/main/java/com/pousadamarealta/service/ReportService.java
   69 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```

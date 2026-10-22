# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 46 |
| `com.pousadamarealta.db` | 1 | 121 |
| `com.pousadamarealta.dto` | 1 | 20 |
| `com.pousadamarealta.exception` | 6 | 42 |
| `com.pousadamarealta.model` | 6 | 284 |
| `com.pousadamarealta.repository` | 3 | 364 |
| `com.pousadamarealta.service` | 4 | 483 |
| `com.pousadamarealta.web` | 1 | 317 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.db
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.web
com.pousadamarealta.repository -> com.pousadamarealta.db
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.dto
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.web -> com.pousadamarealta.dto
com.pousadamarealta.web -> com.pousadamarealta.exception
com.pousadamarealta.web -> com.pousadamarealta.model
com.pousadamarealta.web -> com.pousadamarealta.repository
com.pousadamarealta.web -> com.pousadamarealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.db` | 0 | 2 |
| `com.pousadamarealta.dto` | 0 | 2 |
| `com.pousadamarealta.exception` | 0 | 2 |
| `com.pousadamarealta.model` | 0 | 3 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 4 | 2 |
| `com.pousadamarealta.web` | 5 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.db` |  | sim |  |  |  |  |
| `com.pousadamarealta.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  317 src/main/java/com/pousadamarealta/web/ApiController.java
  244 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  204 src/main/java/com/pousadamarealta/service/ReservationService.java
  168 src/main/java/com/pousadamarealta/service/ReportService.java
  156 src/main/java/com/pousadamarealta/model/Reservation.java
  121 src/main/java/com/pousadamarealta/db/Database.java
   74 src/main/java/com/pousadamarealta/service/NotificationService.java
   68 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   59 src/main/java/com/pousadamarealta/model/Message.java
   52 src/main/java/com/pousadamarealta/repository/RoomRepository.java
```

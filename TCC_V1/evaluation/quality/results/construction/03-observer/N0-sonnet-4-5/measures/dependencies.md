# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 83 |
| `com.pousadamarealta.api` | 6 | 328 |
| `com.pousadamarealta.database` | 1 | 103 |
| `com.pousadamarealta.domain` | 6 | 270 |
| `com.pousadamarealta.events` | 10 | 283 |
| `com.pousadamarealta.repository` | 3 | 389 |
| `com.pousadamarealta.service` | 8 | 327 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.events
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.events -> com.pousadamarealta.domain
com.pousadamarealta.events -> com.pousadamarealta.repository
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.events
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.domain` | 0 | 4 |
| `com.pousadamarealta.events` | 2 | 2 |
| `com.pousadamarealta.repository` | 2 | 4 |
| `com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  | sim |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.events` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  251 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  204 src/main/java/com/pousadamarealta/api/ReservationController.java
  190 src/main/java/com/pousadamarealta/service/ReservationService.java
  156 src/main/java/com/pousadamarealta/domain/Reservation.java
  103 src/main/java/com/pousadamarealta/database/Database.java
   83 src/main/java/com/pousadamarealta/Main.java
   82 src/main/java/com/pousadamarealta/events/GuestNotificationListener.java
   72 src/main/java/com/pousadamarealta/service/ReportService.java
   71 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   67 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```

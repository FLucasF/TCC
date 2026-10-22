# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 38 |
| `com.pousada.api` | 4 | 392 |
| `com.pousada.database` | 4 | 439 |
| `com.pousada.event` | 5 | 86 |
| `com.pousada.event.listener` | 3 | 158 |
| `com.pousada.model` | 3 | 92 |
| `com.pousada.service` | 6 | 268 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.database
com.pousada.api -> com.pousada.model
com.pousada.api -> com.pousada.service
com.pousada -> com.pousada.api
com.pousada -> com.pousada.database
com.pousada -> com.pousada.event
com.pousada -> com.pousada.event.listener
com.pousada.database -> com.pousada.model
com.pousada.event -> com.pousada.model
com.pousada.event.listener -> com.pousada.database
com.pousada.event.listener -> com.pousada.event
com.pousada.event.listener -> com.pousada.model
com.pousada.event.listener -> com.pousada.service
com.pousada.service -> com.pousada.database
com.pousada.service -> com.pousada.event
com.pousada.service -> com.pousada.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.api` | 3 | 1 |
| `com.pousada.database` | 1 | 4 |
| `com.pousada.event` | 1 | 3 |
| `com.pousada.event.listener` | 4 | 1 |
| `com.pousada.model` | 0 | 5 |
| `com.pousada.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim |  |  |  |  | com.google |
| `com.pousada.database` |  | sim |  |  | sim |  |
| `com.pousada.event` |  |  |  |  |  |  |
| `com.pousada.event.listener` |  |  |  |  |  |  |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  226 src/main/java/com/pousada/database/ReservationRepository.java
  223 src/main/java/com/pousada/api/ReservationsEndpoint.java
  159 src/main/java/com/pousada/service/ReservationService.java
  109 src/main/java/com/pousada/api/ReportsEndpoint.java
  108 src/main/java/com/pousada/database/Database.java
   81 src/main/java/com/pousada/service/PricingService.java
   74 src/main/java/com/pousada/event/listener/GuestNotificationListener.java
   58 src/main/java/com/pousada/model/Reservation.java
   54 src/main/java/com/pousada/database/RoomRepository.java
   51 src/main/java/com/pousada/database/MessageRepository.java
```

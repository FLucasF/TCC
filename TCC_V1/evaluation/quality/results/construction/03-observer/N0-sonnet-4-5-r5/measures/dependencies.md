# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 21 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 104 |
| `com.pousadamarealta.controller` | 4 | 289 |
| `com.pousadamarealta.database` | 1 | 98 |
| `com.pousadamarealta.domain` | 7 | 315 |
| `com.pousadamarealta.event` | 5 | 69 |
| `com.pousadamarealta.event.listeners` | 5 | 227 |
| `com.pousadamarealta.exception` | 1 | 44 |
| `com.pousadamarealta.repository` | 3 | 323 |
| `com.pousadamarealta.service` | 5 | 332 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.event
com.pousadamarealta -> com.pousadamarealta.event.listeners
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.domain
com.pousadamarealta.controller -> com.pousadamarealta.exception
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.event -> com.pousadamarealta.domain
com.pousadamarealta.event.listeners -> com.pousadamarealta.domain
com.pousadamarealta.event.listeners -> com.pousadamarealta.event
com.pousadamarealta.event.listeners -> com.pousadamarealta.service
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.event
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 7 | 0 |
| `com.pousadamarealta.controller` | 4 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.event` | 1 | 3 |
| `com.pousadamarealta.event.listeners` | 3 | 1 |
| `com.pousadamarealta.exception` | 0 | 3 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 4 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.controller` | sim |  | sim |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.event` |  |  |  |  |  |  |
| `com.pousadamarealta.event.listeners` |  |  |  |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  205 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  166 src/main/java/com/pousadamarealta/controller/ReservationController.java
  156 src/main/java/com/pousadamarealta/domain/Reservation.java
  153 src/main/java/com/pousadamarealta/service/ReservationService.java
  104 src/main/java/com/pousadamarealta/Main.java
   98 src/main/java/com/pousadamarealta/database/Database.java
   82 src/main/java/com/pousadamarealta/service/ReportService.java
   76 src/main/java/com/pousadamarealta/event/listeners/GuestNotificationListener.java
   62 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   59 src/main/java/com/pousadamarealta/domain/Message.java
```

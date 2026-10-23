# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 120 |
| `com.pousadamarealta.controller` | 5 | 225 |
| `com.pousadamarealta.domain` | 9 | 353 |
| `com.pousadamarealta.dto` | 6 | 158 |
| `com.pousadamarealta.messaging` | 6 | 169 |
| `com.pousadamarealta.repository` | 4 | 460 |
| `com.pousadamarealta.service` | 10 | 367 |
| `com.pousadamarealta.util` | 2 | 60 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.util
com.pousadamarealta.controller -> com.pousadamarealta.domain
com.pousadamarealta.controller -> com.pousadamarealta.dto
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.util
com.pousadamarealta.dto -> com.pousadamarealta.domain
com.pousadamarealta.messaging -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.util -> com.pousadamarealta.dto
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.controller` | 4 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.dto` | 1 | 2 |
| `com.pousadamarealta.messaging` | 1 | 2 |
| `com.pousadamarealta.repository` | 1 | 2 |
| `com.pousadamarealta.service` | 3 | 2 |
| `com.pousadamarealta.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.controller` | sim |  | sim |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  243 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  196 src/main/java/com/pousadamarealta/service/ReservationService.java
  176 src/main/java/com/pousadamarealta/domain/Reservation.java
  120 src/main/java/com/pousadamarealta/App.java
   94 src/main/java/com/pousadamarealta/controller/ReservationController.java
   80 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   80 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   71 src/main/java/com/pousadamarealta/domain/Message.java
   57 src/main/java/com/pousadamarealta/repository/DatabaseInitializer.java
   54 src/main/java/com/pousadamarealta/controller/ExceptionHandler.java
```

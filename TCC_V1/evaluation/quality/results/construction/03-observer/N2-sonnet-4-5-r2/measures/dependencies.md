# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 18 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 2 | 146 |
| `com.pousadamarealta.api` | 1 | 20 |
| `com.pousadamarealta.api.controller` | 4 | 383 |
| `com.pousadamarealta.api.dto` | 6 | 142 |
| `com.pousadamarealta.domain` | 8 | 470 |
| `com.pousadamarealta.notification` | 5 | 170 |
| `com.pousadamarealta.repository` | 4 | 460 |
| `com.pousadamarealta.util` | 2 | 57 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api.controller -> com.pousadamarealta.api
com.pousadamarealta.api.controller -> com.pousadamarealta.api.dto
com.pousadamarealta.api.controller -> com.pousadamarealta.domain
com.pousadamarealta.api.controller -> com.pousadamarealta.notification
com.pousadamarealta.api.controller -> com.pousadamarealta.repository
com.pousadamarealta.api.dto -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.api.controller
com.pousadamarealta -> com.pousadamarealta.api.dto
com.pousadamarealta -> com.pousadamarealta.notification
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta.domain -> com.pousadamarealta.notification
com.pousadamarealta.domain -> com.pousadamarealta.util
com.pousadamarealta.notification -> com.pousadamarealta.domain
com.pousadamarealta.notification -> com.pousadamarealta.repository
com.pousadamarealta.notification -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.notification
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.api` | 0 | 2 |
| `com.pousadamarealta.api.controller` | 5 | 1 |
| `com.pousadamarealta.api.dto` | 1 | 2 |
| `com.pousadamarealta.domain` | 2 | 4 |
| `com.pousadamarealta.notification` | 3 | 4 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.util` | 0 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.repository
com.pousadamarealta.notification
--- um ciclo:
com.pousadamarealta.domain
com.pousadamarealta.notification
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim |  |  | sim | com.google |
| `com.pousadamarealta.api` |  |  |  |  |  |  |
| `com.pousadamarealta.api.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.api.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.notification` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  246 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  232 src/main/java/com/pousadamarealta/domain/Reservation.java
  225 src/main/java/com/pousadamarealta/api/controller/ReservationController.java
  146 src/main/java/com/pousadamarealta/App.java
  112 src/main/java/com/pousadamarealta/api/controller/ReportController.java
  106 src/main/java/com/pousadamarealta/domain/ReservationDocument.java
   87 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   65 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   62 src/main/java/com/pousadamarealta/repository/DatabaseInitializer.java
   56 src/main/java/com/pousadamarealta/notification/HousekeepingNotifier.java
```

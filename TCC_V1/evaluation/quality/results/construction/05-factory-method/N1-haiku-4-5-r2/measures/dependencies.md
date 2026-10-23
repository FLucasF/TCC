# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 7 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.posadamarealta` | 1 | 127 |
| `com.posadamarealta.handlers` | 9 | 342 |
| `com.posadamarealta.model` | 4 | 117 |
| `com.posadamarealta.repository` | 3 | 306 |
| `com.posadamarealta.service` | 3 | 382 |

## As dependências entre os pacotes do projeto

```
com.posadamarealta -> com.posadamarealta.handlers
com.posadamarealta.handlers -> com.posadamarealta.model
com.posadamarealta.handlers -> com.posadamarealta.repository
com.posadamarealta.handlers -> com.posadamarealta.service
com.posadamarealta.repository -> com.posadamarealta.model
com.posadamarealta.service -> com.posadamarealta.model
com.posadamarealta.service -> com.posadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.posadamarealta` | 1 | 0 |
| `com.posadamarealta.handlers` | 3 | 1 |
| `com.posadamarealta.model` | 0 | 3 |
| `com.posadamarealta.repository` | 1 | 2 |
| `com.posadamarealta.service` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.posadamarealta` | sim | sim |  |  | sim |  |
| `com.posadamarealta.handlers` | sim | sim | sim |  |  |  |
| `com.posadamarealta.model` |  |  |  |  |  |  |
| `com.posadamarealta.repository` |  | sim |  |  |  |  |
| `com.posadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  194 src/main/java/com/posadamarealta/repository/ReservationRepository.java
  194 src/main/java/com/posadamarealta/handlers/ReservationHandler.java
  177 src/main/java/com/posadamarealta/service/ReservationService.java
  127 src/main/java/com/posadamarealta/App.java
  114 src/main/java/com/posadamarealta/handlers/ReportHandler.java
  112 src/main/java/com/posadamarealta/service/DocumentService.java
   93 src/main/java/com/posadamarealta/service/UtilService.java
   57 src/main/java/com/posadamarealta/repository/RoomRepository.java
   57 src/main/java/com/posadamarealta/model/Reservation.java
   55 src/main/java/com/posadamarealta/repository/MessageRepository.java
```

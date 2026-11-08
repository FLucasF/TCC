# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 16 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 78 |
| `com.pousadamarealta.controller` | 5 | 321 |
| `com.pousadamarealta.model` | 6 | 284 |
| `com.pousadamarealta.report` | 5 | 239 |
| `com.pousadamarealta.repository` | 4 | 368 |
| `com.pousadamarealta.service` | 8 | 274 |
| `com.pousadamarealta.util` | 3 | 73 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.model
com.pousadamarealta.controller -> com.pousadamarealta.report
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.util
com.pousadamarealta.report -> com.pousadamarealta.model
com.pousadamarealta.report -> com.pousadamarealta.repository
com.pousadamarealta.report -> com.pousadamarealta.service
com.pousadamarealta.report -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.controller` | 5 | 1 |
| `com.pousadamarealta.model` | 0 | 4 |
| `com.pousadamarealta.report` | 4 | 1 |
| `com.pousadamarealta.repository` | 1 | 4 |
| `com.pousadamarealta.service` | 3 | 3 |
| `com.pousadamarealta.util` | 0 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.controller` | sim |  | sim |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.report` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  212 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  192 src/main/java/com/pousadamarealta/service/ReservationService.java
  156 src/main/java/com/pousadamarealta/model/Reservation.java
  155 src/main/java/com/pousadamarealta/controller/ReservationController.java
   78 src/main/java/com/pousadamarealta/Main.java
   76 src/main/java/com/pousadamarealta/controller/ReportController.java
   68 src/main/java/com/pousadamarealta/report/RatePlansReport.java
   65 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   64 src/main/java/com/pousadamarealta/repository/Database.java
   59 src/main/java/com/pousadamarealta/model/Message.java
```

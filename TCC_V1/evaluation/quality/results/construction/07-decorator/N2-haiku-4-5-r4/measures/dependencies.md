# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 66 |
| `com.pousadamarealta.db` | 1 | 141 |
| `com.pousadamarealta.handler` | 4 | 349 |
| `com.pousadamarealta.model` | 6 | 105 |
| `com.pousadamarealta.service` | 3 | 595 |
| `com.pousadamarealta.util` | 2 | 101 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.db
com.pousadamarealta -> com.pousadamarealta.handler
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.handler -> com.pousadamarealta.model
com.pousadamarealta.handler -> com.pousadamarealta.service
com.pousadamarealta.service -> com.pousadamarealta.db
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.db` | 0 | 2 |
| `com.pousadamarealta.handler` | 2 | 1 |
| `com.pousadamarealta.model` | 0 | 2 |
| `com.pousadamarealta.service` | 3 | 2 |
| `com.pousadamarealta.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.db` |  | sim |  |  | sim |  |
| `com.pousadamarealta.handler` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  330 src/main/java/com/pousadamarealta/service/ReservationService.java
  227 src/main/java/com/pousadamarealta/handler/ReservationHandler.java
  147 src/main/java/com/pousadamarealta/service/ReportService.java
  141 src/main/java/com/pousadamarealta/db/Database.java
  118 src/main/java/com/pousadamarealta/service/MessageService.java
   66 src/main/java/com/pousadamarealta/Main.java
   60 src/main/java/com/pousadamarealta/util/Pricing.java
   54 src/main/java/com/pousadamarealta/handler/ReportHandler.java
   48 src/main/java/com/pousadamarealta/model/Reservation.java
   41 src/main/java/com/pousadamarealta/util/EmailValidator.java
```

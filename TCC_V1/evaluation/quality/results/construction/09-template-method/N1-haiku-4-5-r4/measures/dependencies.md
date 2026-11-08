# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 405 |
| `com.pousada.db` | 1 | 420 |
| `com.pousada.dto` | 7 | 89 |
| `com.pousada.model` | 7 | 188 |
| `com.pousada.service` | 9 | 375 |
| `com.pousada.util` | 1 | 63 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.db
com.pousada -> com.pousada.dto
com.pousada -> com.pousada.model
com.pousada -> com.pousada.service
com.pousada.db -> com.pousada.model
com.pousada.service -> com.pousada.db
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.db` | 1 | 2 |
| `com.pousada.dto` | 0 | 1 |
| `com.pousada.model` | 0 | 3 |
| `com.pousada.service` | 3 | 1 |
| `com.pousada.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  | sim |  |  |  |
| `com.pousada.db` |  | sim |  |  | sim |  |
| `com.pousada.dto` |  |  |  |  |  |  |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  420 src/main/java/com/pousada/db/Database.java
  405 src/main/java/com/pousada/App.java
  182 src/main/java/com/pousada/service/ReservationService.java
  145 src/main/java/com/pousada/service/ReportGenerator.java
   95 src/main/java/com/pousada/model/Reservation.java
   63 src/main/java/com/pousada/util/PriceCalculator.java
   48 src/main/java/com/pousada/service/DocumentGenerator.java
   33 src/main/java/com/pousada/model/Room.java
   23 src/main/java/com/pousada/model/Message.java
   22 src/main/java/com/pousada/dto/ReservationResponse.java
```

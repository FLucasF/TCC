# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareal` | 2 | 37 |
| `com.pousadamareal.config` | 1 | 85 |
| `com.pousadamareal.dao` | 3 | 293 |
| `com.pousadamareal.handlers` | 8 | 474 |
| `com.pousadamareal.model` | 6 | 220 |
| `com.pousadamareal.util` | 2 | 162 |

## As dependências entre os pacotes do projeto

```
com.pousadamareal -> com.pousadamareal.config
com.pousadamareal -> com.pousadamareal.handlers
com.pousadamareal.dao -> com.pousadamareal.config
com.pousadamareal.dao -> com.pousadamareal.model
com.pousadamareal.handlers -> com.pousadamareal.dao
com.pousadamareal.handlers -> com.pousadamareal.model
com.pousadamareal.handlers -> com.pousadamareal.util
com.pousadamareal.util -> com.pousadamareal.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareal` | 2 | 0 |
| `com.pousadamareal.config` | 0 | 2 |
| `com.pousadamareal.dao` | 2 | 1 |
| `com.pousadamareal.handlers` | 3 | 1 |
| `com.pousadamareal.model` | 0 | 3 |
| `com.pousadamareal.util` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareal` | sim |  |  |  |  |  |
| `com.pousadamareal.config` |  | sim |  |  |  |  |
| `com.pousadamareal.dao` |  | sim |  |  |  |  |
| `com.pousadamareal.handlers` | sim |  |  |  |  | com.google |
| `com.pousadamareal.model` |  |  |  |  |  | com.google.gson |
| `com.pousadamareal.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  308 src/main/java/com/pousadamareal/handlers/ReservationHandler.java
  193 src/main/java/com/pousadamareal/dao/ReservationDAO.java
  124 src/main/java/com/pousadamareal/handlers/ReportHandler.java
  109 src/main/java/com/pousadamareal/util/DocumentFormatter.java
   94 src/main/java/com/pousadamareal/model/Reservation.java
   85 src/main/java/com/pousadamareal/config/DatabaseConfig.java
   81 src/main/java/com/pousadamareal/model/Document.java
   53 src/main/java/com/pousadamareal/util/PriceCalculator.java
   50 src/main/java/com/pousadamareal/dao/RoomDAO.java
   50 src/main/java/com/pousadamareal/dao/MessageDAO.java
```

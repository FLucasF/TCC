# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareal` | 1 | 48 |
| `com.pousadamareal.db` | 1 | 94 |
| `com.pousadamareal.handler` | 4 | 393 |
| `com.pousadamareal.model` | 7 | 115 |
| `com.pousadamareal.repository` | 3 | 284 |
| `com.pousadamareal.service` | 4 | 469 |
| `com.pousadamareal.util` | 1 | 14 |

## As dependências entre os pacotes do projeto

```
com.pousadamareal -> com.pousadamareal.db
com.pousadamareal -> com.pousadamareal.handler
com.pousadamareal.handler -> com.pousadamareal.model
com.pousadamareal.handler -> com.pousadamareal.repository
com.pousadamareal.handler -> com.pousadamareal.service
com.pousadamareal.handler -> com.pousadamareal.util
com.pousadamareal.repository -> com.pousadamareal.db
com.pousadamareal.repository -> com.pousadamareal.model
com.pousadamareal.service -> com.pousadamareal.model
com.pousadamareal.service -> com.pousadamareal.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareal` | 2 | 0 |
| `com.pousadamareal.db` | 0 | 2 |
| `com.pousadamareal.handler` | 4 | 1 |
| `com.pousadamareal.model` | 0 | 3 |
| `com.pousadamareal.repository` | 2 | 2 |
| `com.pousadamareal.service` | 2 | 1 |
| `com.pousadamareal.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareal` | sim |  |  |  |  |  |
| `com.pousadamareal.db` |  | sim |  |  |  |  |
| `com.pousadamareal.handler` | sim | sim |  |  |  | com.google |
| `com.pousadamareal.model` |  |  |  |  |  |  |
| `com.pousadamareal.repository` |  | sim |  |  |  |  |
| `com.pousadamareal.service` |  | sim |  |  |  |  |
| `com.pousadamareal.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  291 src/main/java/com/pousadamareal/handler/ReservationHandler.java
  184 src/main/java/com/pousadamareal/repository/ReservationRepository.java
  181 src/main/java/com/pousadamareal/service/ReservationService.java
  146 src/main/java/com/pousadamareal/service/DocumentGenerator.java
   94 src/main/java/com/pousadamareal/db/Database.java
   87 src/main/java/com/pousadamareal/service/ReportService.java
   55 src/main/java/com/pousadamareal/service/PricingUtils.java
   51 src/main/java/com/pousadamareal/repository/MessageRepository.java
   51 src/main/java/com/pousadamareal/model/Reservation.java
   49 src/main/java/com/pousadamareal/repository/RoomRepository.java
```

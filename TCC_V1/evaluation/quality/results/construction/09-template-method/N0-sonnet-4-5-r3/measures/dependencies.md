# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealba` | 1 | 84 |
| `com.pousada.marealba.config` | 1 | 85 |
| `com.pousada.marealba.controller` | 4 | 341 |
| `com.pousada.marealba.exception` | 2 | 54 |
| `com.pousada.marealba.model` | 7 | 405 |
| `com.pousada.marealba.repository` | 3 | 408 |
| `com.pousada.marealba.service` | 4 | 451 |
| `com.pousada.marealba.util` | 3 | 80 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealba -> com.pousada.marealba.config
com.pousada.marealba -> com.pousada.marealba.controller
com.pousada.marealba -> com.pousada.marealba.exception
com.pousada.marealba -> com.pousada.marealba.repository
com.pousada.marealba -> com.pousada.marealba.service
com.pousada.marealba.controller -> com.pousada.marealba.exception
com.pousada.marealba.controller -> com.pousada.marealba.model
com.pousada.marealba.controller -> com.pousada.marealba.service
com.pousada.marealba.controller -> com.pousada.marealba.util
com.pousada.marealba.repository -> com.pousada.marealba.config
com.pousada.marealba.repository -> com.pousada.marealba.model
com.pousada.marealba.service -> com.pousada.marealba.exception
com.pousada.marealba.service -> com.pousada.marealba.model
com.pousada.marealba.service -> com.pousada.marealba.repository
com.pousada.marealba.service -> com.pousada.marealba.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealba` | 5 | 0 |
| `com.pousada.marealba.config` | 0 | 2 |
| `com.pousada.marealba.controller` | 4 | 1 |
| `com.pousada.marealba.exception` | 0 | 3 |
| `com.pousada.marealba.model` | 0 | 3 |
| `com.pousada.marealba.repository` | 2 | 2 |
| `com.pousada.marealba.service` | 4 | 2 |
| `com.pousada.marealba.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealba` | sim |  |  |  |  |  |
| `com.pousada.marealba.config` |  | sim |  |  |  |  |
| `com.pousada.marealba.controller` | sim |  |  |  |  |  |
| `com.pousada.marealba.exception` |  |  |  |  |  |  |
| `com.pousada.marealba.model` |  |  |  |  |  |  |
| `com.pousada.marealba.repository` |  | sim |  |  |  |  |
| `com.pousada.marealba.service` |  |  |  |  |  |  |
| `com.pousada.marealba.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  276 src/main/java/com/pousada/marealba/repository/ReservationRepository.java
  248 src/main/java/com/pousada/marealba/service/ReservationService.java
  156 src/main/java/com/pousada/marealba/service/ReportService.java
  156 src/main/java/com/pousada/marealba/model/Reservation.java
  156 src/main/java/com/pousada/marealba/controller/ReservationController.java
  115 src/main/java/com/pousada/marealba/model/Document.java
  114 src/main/java/com/pousada/marealba/controller/ReportController.java
   85 src/main/java/com/pousada/marealba/config/Database.java
   84 src/main/java/com/pousada/marealba/Main.java
   69 src/main/java/com/pousada/marealba/repository/MessageRepository.java
```

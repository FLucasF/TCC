# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealte` | 1 | 19 |
| `br.com.marealte.controller` | 4 | 400 |
| `br.com.marealte.database` | 2 | 103 |
| `br.com.marealte.dto` | 2 | 72 |
| `br.com.marealte.model` | 7 | 256 |
| `br.com.marealte.repository` | 3 | 374 |
| `br.com.marealte.routes` | 1 | 42 |
| `br.com.marealte.service` | 4 | 266 |

## As dependências entre os pacotes do projeto

```
br.com.marealte -> br.com.marealte.database
br.com.marealte -> br.com.marealte.routes
br.com.marealte.controller -> br.com.marealte.dto
br.com.marealte.controller -> br.com.marealte.model
br.com.marealte.controller -> br.com.marealte.repository
br.com.marealte.controller -> br.com.marealte.service
br.com.marealte.dto -> br.com.marealte.model
br.com.marealte.repository -> br.com.marealte.database
br.com.marealte.repository -> br.com.marealte.model
br.com.marealte.routes -> br.com.marealte.controller
br.com.marealte.routes -> br.com.marealte.repository
br.com.marealte.routes -> br.com.marealte.service
br.com.marealte.service -> br.com.marealte.model
br.com.marealte.service -> br.com.marealte.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealte` | 2 | 0 |
| `br.com.marealte.controller` | 4 | 1 |
| `br.com.marealte.database` | 0 | 2 |
| `br.com.marealte.dto` | 1 | 1 |
| `br.com.marealte.model` | 0 | 4 |
| `br.com.marealte.repository` | 2 | 3 |
| `br.com.marealte.routes` | 3 | 1 |
| `br.com.marealte.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealte` | sim |  |  |  |  |  |
| `br.com.marealte.controller` | sim |  |  |  |  |  |
| `br.com.marealte.database` |  | sim |  |  |  |  |
| `br.com.marealte.dto` |  |  |  |  |  |  |
| `br.com.marealte.model` |  |  | sim |  |  |  |
| `br.com.marealte.repository` |  | sim |  |  |  |  |
| `br.com.marealte.routes` | sim |  |  |  |  |  |
| `br.com.marealte.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  315 src/main/java/br/com/marealte/controller/ReservationController.java
  252 src/main/java/br/com/marealte/repository/ReservationRepository.java
  124 src/main/java/br/com/marealte/service/DocumentService.java
   91 src/main/java/br/com/marealte/model/Reservation.java
   88 src/main/java/br/com/marealte/database/DatabaseSetup.java
   79 src/main/java/br/com/marealte/model/Document.java
   72 src/main/java/br/com/marealte/service/ReportService.java
   62 src/main/java/br/com/marealte/repository/RoomRepository.java
   60 src/main/java/br/com/marealte/repository/MessageRepository.java
   55 src/main/java/br/com/marealte/dto/CreateReservationRequest.java
```

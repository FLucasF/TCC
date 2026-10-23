# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealto` | 1 | 95 |
| `br.com.marealto.dto` | 6 | 275 |
| `br.com.marealto.handler` | 1 | 216 |
| `br.com.marealto.model` | 10 | 283 |
| `br.com.marealto.repository` | 4 | 499 |
| `br.com.marealto.service` | 10 | 497 |

## As dependências entre os pacotes do projeto

```
br.com.marealto -> br.com.marealto.handler
br.com.marealto -> br.com.marealto.repository
br.com.marealto -> br.com.marealto.service
br.com.marealto.dto -> br.com.marealto.model
br.com.marealto.dto -> br.com.marealto.service
br.com.marealto.handler -> br.com.marealto.dto
br.com.marealto.handler -> br.com.marealto.model
br.com.marealto.handler -> br.com.marealto.repository
br.com.marealto.handler -> br.com.marealto.service
br.com.marealto.repository -> br.com.marealto.model
br.com.marealto.service -> br.com.marealto.model
br.com.marealto.service -> br.com.marealto.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealto` | 3 | 0 |
| `br.com.marealto.dto` | 2 | 1 |
| `br.com.marealto.handler` | 4 | 1 |
| `br.com.marealto.model` | 0 | 4 |
| `br.com.marealto.repository` | 1 | 3 |
| `br.com.marealto.service` | 2 | 3 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealto` | sim |  | sim |  |  |  |
| `br.com.marealto.dto` |  |  | sim |  |  |  |
| `br.com.marealto.handler` | sim |  |  |  |  |  |
| `br.com.marealto.model` |  |  |  |  |  |  |
| `br.com.marealto.repository` |  | sim |  |  | sim |  |
| `br.com.marealto.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  254 src/main/java/br/com/marealto/repository/ReservationRepository.java
  216 src/main/java/br/com/marealto/handler/ApiHandler.java
  197 src/main/java/br/com/marealto/service/ReservationService.java
  156 src/main/java/br/com/marealto/model/Reservation.java
  153 src/main/java/br/com/marealto/service/DocumentService.java
   95 src/main/java/br/com/marealto/App.java
   93 src/main/java/br/com/marealto/repository/RoomRepository.java
   93 src/main/java/br/com/marealto/repository/Database.java
   77 src/main/java/br/com/marealto/service/ReportService.java
   77 src/main/java/br/com/marealto/dto/CreateReservationRequest.java
```

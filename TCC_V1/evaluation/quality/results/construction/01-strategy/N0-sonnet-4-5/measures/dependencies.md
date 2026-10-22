# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareaalta` | 1 | 50 |
| `com.pousadamareaalta.api` | 3 | 265 |
| `com.pousadamareaalta.api.dto` | 5 | 79 |
| `com.pousadamareaalta.exception` | 6 | 42 |
| `com.pousadamareaalta.model` | 7 | 318 |
| `com.pousadamareaalta.pricing` | 10 | 277 |
| `com.pousadamareaalta.repository` | 3 | 369 |
| `com.pousadamareaalta.service` | 4 | 320 |

## As dependências entre os pacotes do projeto

```
com.pousadamareaalta.api -> com.pousadamareaalta.api.dto
com.pousadamareaalta.api -> com.pousadamareaalta.exception
com.pousadamareaalta.api -> com.pousadamareaalta.model
com.pousadamareaalta.api -> com.pousadamareaalta.service
com.pousadamareaalta -> com.pousadamareaalta.api
com.pousadamareaalta -> com.pousadamareaalta.repository
com.pousadamareaalta -> com.pousadamareaalta.service
com.pousadamareaalta.pricing -> com.pousadamareaalta.exception
com.pousadamareaalta.pricing -> com.pousadamareaalta.model
com.pousadamareaalta.repository -> com.pousadamareaalta.model
com.pousadamareaalta.service -> com.pousadamareaalta.exception
com.pousadamareaalta.service -> com.pousadamareaalta.model
com.pousadamareaalta.service -> com.pousadamareaalta.pricing
com.pousadamareaalta.service -> com.pousadamareaalta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareaalta` | 3 | 0 |
| `com.pousadamareaalta.api` | 4 | 1 |
| `com.pousadamareaalta.api.dto` | 0 | 1 |
| `com.pousadamareaalta.exception` | 0 | 3 |
| `com.pousadamareaalta.model` | 0 | 4 |
| `com.pousadamareaalta.pricing` | 2 | 1 |
| `com.pousadamareaalta.repository` | 1 | 2 |
| `com.pousadamareaalta.service` | 4 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareaalta` | sim |  | sim |  |  |  |
| `com.pousadamareaalta.api` | sim |  |  |  |  |  |
| `com.pousadamareaalta.api.dto` |  |  | sim |  |  |  |
| `com.pousadamareaalta.exception` |  |  |  |  |  |  |
| `com.pousadamareaalta.model` |  |  |  |  |  |  |
| `com.pousadamareaalta.pricing` |  |  |  |  |  |  |
| `com.pousadamareaalta.repository` |  | sim |  |  |  |  |
| `com.pousadamareaalta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  235 src/main/java/com/pousadamareaalta/repository/ReservationRepository.java
  191 src/main/java/com/pousadamareaalta/service/ReservationService.java
  183 src/main/java/com/pousadamareaalta/api/ApiController.java
  160 src/main/java/com/pousadamareaalta/model/Reservation.java
   80 src/main/java/com/pousadamareaalta/service/ReportService.java
   78 src/main/java/com/pousadamareaalta/api/ApiMapper.java
   70 src/main/java/com/pousadamareaalta/repository/MessageRepository.java
   64 src/main/java/com/pousadamareaalta/repository/Database.java
   59 src/main/java/com/pousadamareaalta/model/Message.java
   50 src/main/java/com/pousadamareaalta/App.java
```

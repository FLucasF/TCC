# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 10 pacotes, 19 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 140 |
| `com.pousadamarealta.api` | 1 | 24 |
| `com.pousadamarealta.api.controller` | 4 | 207 |
| `com.pousadamarealta.api.dto` | 5 | 401 |
| `com.pousadamarealta.config` | 1 | 31 |
| `com.pousadamarealta.domain.event` | 5 | 79 |
| `com.pousadamarealta.domain.event.listeners` | 3 | 164 |
| `com.pousadamarealta.domain.model` | 6 | 315 |
| `com.pousadamarealta.domain.service` | 10 | 344 |
| `com.pousadamarealta.repository` | 3 | 390 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api.controller -> com.pousadamarealta.api.dto
com.pousadamarealta.api.controller -> com.pousadamarealta.domain.model
com.pousadamarealta.api.controller -> com.pousadamarealta.domain.service
com.pousadamarealta.api.dto -> com.pousadamarealta.domain.model
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.api.controller
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta -> com.pousadamarealta.domain.event
com.pousadamarealta -> com.pousadamarealta.domain.event.listeners
com.pousadamarealta -> com.pousadamarealta.domain.service
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta.domain.event -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.event.listeners -> com.pousadamarealta.domain.event
com.pousadamarealta.domain.event.listeners -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.event.listeners -> com.pousadamarealta.domain.service
com.pousadamarealta.domain.service -> com.pousadamarealta.domain.event
com.pousadamarealta.domain.service -> com.pousadamarealta.domain.model
com.pousadamarealta.domain.service -> com.pousadamarealta.repository
com.pousadamarealta.repository -> com.pousadamarealta.domain.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 7 | 0 |
| `com.pousadamarealta.api` | 0 | 1 |
| `com.pousadamarealta.api.controller` | 3 | 1 |
| `com.pousadamarealta.api.dto` | 1 | 1 |
| `com.pousadamarealta.config` | 0 | 1 |
| `com.pousadamarealta.domain.event` | 1 | 3 |
| `com.pousadamarealta.domain.event.listeners` | 3 | 1 |
| `com.pousadamarealta.domain.model` | 0 | 6 |
| `com.pousadamarealta.domain.service` | 3 | 3 |
| `com.pousadamarealta.repository` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.api` |  |  | sim |  |  |  |
| `com.pousadamarealta.api.controller` | sim | sim |  |  |  |  |
| `com.pousadamarealta.api.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.config` |  | sim |  |  |  |  |
| `com.pousadamarealta.domain.event` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.event.listeners` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  230 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  200 src/main/java/com/pousadamarealta/domain/service/ReservationService.java
  194 src/main/java/com/pousadamarealta/domain/model/Reservation.java
  140 src/main/java/com/pousadamarealta/Main.java
  140 src/main/java/com/pousadamarealta/api/dto/ReservationResponse.java
  117 src/main/java/com/pousadamarealta/api/controller/ReservationController.java
  108 src/main/java/com/pousadamarealta/api/dto/DocumentResponse.java
   87 src/main/java/com/pousadamarealta/domain/service/ReportService.java
   82 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   78 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```

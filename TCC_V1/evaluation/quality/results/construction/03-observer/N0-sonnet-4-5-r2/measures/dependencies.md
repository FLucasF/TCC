# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 11 pacotes, 23 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 114 |
| `com.pousadamarealta.config` | 1 | 21 |
| `com.pousadamarealta.domain.events` | 4 | 45 |
| `com.pousadamarealta.domain.model` | 7 | 299 |
| `com.pousadamarealta.repository` | 3 | 413 |
| `com.pousadamarealta.service` | 10 | 335 |
| `com.pousadamarealta.service.listeners` | 5 | 225 |
| `com.pousadamarealta.util` | 2 | 77 |
| `com.pousadamarealta.web.controller` | 4 | 361 |
| `com.pousadamarealta.web.dto` | 6 | 566 |
| `com.pousadamarealta.web.mapper` | 1 | 79 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta -> com.pousadamarealta.domain.events
com.pousadamarealta -> com.pousadamarealta.domain.model
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.service.listeners
com.pousadamarealta -> com.pousadamarealta.web.controller
com.pousadamarealta.domain.events -> com.pousadamarealta.domain.model
com.pousadamarealta.repository -> com.pousadamarealta.domain.model
com.pousadamarealta.service -> com.pousadamarealta.domain.events
com.pousadamarealta.service -> com.pousadamarealta.domain.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
com.pousadamarealta.service.listeners -> com.pousadamarealta.domain.events
com.pousadamarealta.service.listeners -> com.pousadamarealta.domain.model
com.pousadamarealta.service.listeners -> com.pousadamarealta.service
com.pousadamarealta.service.listeners -> com.pousadamarealta.util
com.pousadamarealta.web.controller -> com.pousadamarealta.domain.model
com.pousadamarealta.web.controller -> com.pousadamarealta.service
com.pousadamarealta.web.controller -> com.pousadamarealta.web.dto
com.pousadamarealta.web.controller -> com.pousadamarealta.web.mapper
com.pousadamarealta.web.mapper -> com.pousadamarealta.domain.model
com.pousadamarealta.web.mapper -> com.pousadamarealta.web.dto
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 7 | 0 |
| `com.pousadamarealta.config` | 0 | 1 |
| `com.pousadamarealta.domain.events` | 1 | 3 |
| `com.pousadamarealta.domain.model` | 0 | 7 |
| `com.pousadamarealta.repository` | 1 | 2 |
| `com.pousadamarealta.service` | 4 | 3 |
| `com.pousadamarealta.service.listeners` | 4 | 1 |
| `com.pousadamarealta.util` | 0 | 2 |
| `com.pousadamarealta.web.controller` | 4 | 1 |
| `com.pousadamarealta.web.dto` | 0 | 2 |
| `com.pousadamarealta.web.mapper` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.config` |  | sim |  |  | sim |  |
| `com.pousadamarealta.domain.events` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.service.listeners` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |
| `com.pousadamarealta.web.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.web.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.web.mapper` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  247 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  243 src/main/java/com/pousadamarealta/service/ReservationService.java
  190 src/main/java/com/pousadamarealta/web/controller/ReservationController.java
  183 src/main/java/com/pousadamarealta/web/dto/ReservationDTO.java
  166 src/main/java/com/pousadamarealta/domain/model/Reservation.java
  139 src/main/java/com/pousadamarealta/web/dto/DocumentDTO.java
  125 src/main/java/com/pousadamarealta/web/controller/ReportController.java
  114 src/main/java/com/pousadamarealta/Main.java
   93 src/main/java/com/pousadamarealta/web/dto/CreateReservationRequest.java
   84 src/main/java/com/pousadamarealta/repository/RoomRepository.java
```

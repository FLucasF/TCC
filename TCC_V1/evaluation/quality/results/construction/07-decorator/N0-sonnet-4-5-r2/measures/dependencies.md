# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 10 pacotes, 23 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 89 |
| `com.pousadamarealta.controller` | 1 | 197 |
| `com.pousadamarealta.controller.dto` | 2 | 121 |
| `com.pousadamarealta.domain.exception` | 1 | 20 |
| `com.pousadamarealta.domain.model` | 9 | 459 |
| `com.pousadamarealta.domain.repository` | 3 | 33 |
| `com.pousadamarealta.infrastructure` | 4 | 387 |
| `com.pousadamarealta.messaging` | 4 | 123 |
| `com.pousadamarealta.messaging.rules` | 3 | 91 |
| `com.pousadamarealta.service` | 4 | 388 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.domain.repository
com.pousadamarealta -> com.pousadamarealta.infrastructure
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.controller.dto
com.pousadamarealta.controller -> com.pousadamarealta.domain.exception
com.pousadamarealta.controller -> com.pousadamarealta.domain.model
com.pousadamarealta.controller -> com.pousadamarealta.domain.repository
com.pousadamarealta.controller -> com.pousadamarealta.messaging
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.domain.repository -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure -> com.pousadamarealta.domain.model
com.pousadamarealta.infrastructure -> com.pousadamarealta.domain.repository
com.pousadamarealta.messaging -> com.pousadamarealta.domain.model
com.pousadamarealta.messaging -> com.pousadamarealta.domain.repository
com.pousadamarealta.messaging -> com.pousadamarealta.messaging.rules
com.pousadamarealta.messaging.rules -> com.pousadamarealta.domain.model
com.pousadamarealta.messaging.rules -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.domain.exception
com.pousadamarealta.service -> com.pousadamarealta.domain.model
com.pousadamarealta.service -> com.pousadamarealta.domain.repository
com.pousadamarealta.service -> com.pousadamarealta.messaging
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.controller` | 6 | 1 |
| `com.pousadamarealta.controller.dto` | 0 | 1 |
| `com.pousadamarealta.domain.exception` | 0 | 2 |
| `com.pousadamarealta.domain.model` | 0 | 6 |
| `com.pousadamarealta.domain.repository` | 1 | 5 |
| `com.pousadamarealta.infrastructure` | 2 | 1 |
| `com.pousadamarealta.messaging` | 3 | 4 |
| `com.pousadamarealta.messaging.rules` | 2 | 1 |
| `com.pousadamarealta.service` | 4 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.messaging.rules
com.pousadamarealta.messaging
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.controller.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.domain.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.model` |  |  |  |  |  |  |
| `com.pousadamarealta.domain.repository` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure` |  | sim |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  227 src/main/java/com/pousadamarealta/infrastructure/PostgresReservationRepository.java
  197 src/main/java/com/pousadamarealta/controller/ApiController.java
  178 src/main/java/com/pousadamarealta/domain/model/Reservation.java
  157 src/main/java/com/pousadamarealta/service/ReservationService.java
  119 src/main/java/com/pousadamarealta/domain/model/Document.java
   94 src/main/java/com/pousadamarealta/controller/dto/CreateReservationRequest.java
   89 src/main/java/com/pousadamarealta/Application.java
   88 src/main/java/com/pousadamarealta/service/DocumentService.java
   79 src/main/java/com/pousadamarealta/infrastructure/PostgresMessageRepository.java
   76 src/main/java/com/pousadamarealta/service/ReportService.java
```

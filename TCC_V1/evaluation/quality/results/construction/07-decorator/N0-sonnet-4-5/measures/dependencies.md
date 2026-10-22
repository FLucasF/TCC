# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 10 pacotes, 22 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 68 |
| `com.pousadamarealta.config` | 1 | 111 |
| `com.pousadamarealta.controller` | 1 | 222 |
| `com.pousadamarealta.controller.dto` | 6 | 387 |
| `com.pousadamarealta.messaging` | 4 | 157 |
| `com.pousadamarealta.messaging.rules` | 3 | 81 |
| `com.pousadamarealta.model` | 8 | 314 |
| `com.pousadamarealta.repository` | 3 | 366 |
| `com.pousadamarealta.service` | 9 | 308 |
| `com.pousadamarealta.util` | 3 | 166 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.controller.dto
com.pousadamarealta.controller -> com.pousadamarealta.messaging
com.pousadamarealta.controller -> com.pousadamarealta.model
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.controller.dto -> com.pousadamarealta.model
com.pousadamarealta.messaging -> com.pousadamarealta.messaging.rules
com.pousadamarealta.messaging -> com.pousadamarealta.model
com.pousadamarealta.messaging -> com.pousadamarealta.repository
com.pousadamarealta.messaging.rules -> com.pousadamarealta.messaging
com.pousadamarealta.messaging.rules -> com.pousadamarealta.model
com.pousadamarealta.repository -> com.pousadamarealta.config
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
com.pousadamarealta.util -> com.pousadamarealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 0 |
| `com.pousadamarealta.config` | 0 | 2 |
| `com.pousadamarealta.controller` | 4 | 1 |
| `com.pousadamarealta.controller.dto` | 1 | 1 |
| `com.pousadamarealta.messaging` | 3 | 4 |
| `com.pousadamarealta.messaging.rules` | 2 | 1 |
| `com.pousadamarealta.model` | 0 | 7 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 4 | 2 |
| `com.pousadamarealta.util` | 1 | 1 |

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
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.config` |  | sim |  |  | sim |  |
| `com.pousadamarealta.controller` | sim |  | sim |  |  |  |
| `com.pousadamarealta.controller.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  233 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  222 src/main/java/com/pousadamarealta/controller/ApiController.java
  192 src/main/java/com/pousadamarealta/service/ReservationService.java
  154 src/main/java/com/pousadamarealta/model/Reservation.java
  122 src/main/java/com/pousadamarealta/controller/dto/ReservationResponse.java
  111 src/main/java/com/pousadamarealta/config/Database.java
   99 src/main/java/com/pousadamarealta/util/PricingCalculator.java
   97 src/main/java/com/pousadamarealta/controller/dto/ReceiptResponse.java
   87 src/main/java/com/pousadamarealta/service/ReportService.java
   73 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```

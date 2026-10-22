# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 21 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 103 |
| `com.pousadamarealta.controller` | 2 | 250 |
| `com.pousadamarealta.controller.dto` | 6 | 145 |
| `com.pousadamarealta.domain` | 6 | 226 |
| `com.pousadamarealta.infrastructure` | 1 | 105 |
| `com.pousadamarealta.messaging` | 9 | 169 |
| `com.pousadamarealta.messaging.rules` | 3 | 92 |
| `com.pousadamarealta.repository` | 6 | 465 |
| `com.pousadamarealta.service` | 9 | 290 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.domain
com.pousadamarealta -> com.pousadamarealta.infrastructure
com.pousadamarealta -> com.pousadamarealta.messaging
com.pousadamarealta -> com.pousadamarealta.messaging.rules
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.controller.dto
com.pousadamarealta.controller -> com.pousadamarealta.domain
com.pousadamarealta.controller -> com.pousadamarealta.messaging
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.controller.dto -> com.pousadamarealta.domain
com.pousadamarealta.controller.dto -> com.pousadamarealta.messaging
com.pousadamarealta.messaging -> com.pousadamarealta.repository
com.pousadamarealta.messaging.rules -> com.pousadamarealta.messaging
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 7 | 0 |
| `com.pousadamarealta.controller` | 5 | 1 |
| `com.pousadamarealta.controller.dto` | 2 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.infrastructure` | 0 | 1 |
| `com.pousadamarealta.messaging` | 1 | 6 |
| `com.pousadamarealta.messaging.rules` | 1 | 1 |
| `com.pousadamarealta.repository` | 2 | 4 |
| `com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.messaging
com.pousadamarealta.repository
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.controller.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.infrastructure` |  | sim |  |  | sim |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.messaging.rules` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  291 src/main/java/com/pousadamarealta/repository/JdbcReservationRepository.java
  250 src/main/java/com/pousadamarealta/controller/ApiController.java
  152 src/main/java/com/pousadamarealta/service/ReservationService.java
  116 src/main/java/com/pousadamarealta/domain/Reservation.java
  105 src/main/java/com/pousadamarealta/infrastructure/Database.java
  103 src/main/java/com/pousadamarealta/App.java
   84 src/main/java/com/pousadamarealta/service/ReportService.java
   69 src/main/java/com/pousadamarealta/repository/JdbcMessageRepository.java
   66 src/main/java/com/pousadamarealta/repository/JdbcRoomRepository.java
   57 src/main/java/com/pousadamarealta/domain/PriceCalculator.java
```

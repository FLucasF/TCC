# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 9 pacotes, 21 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 95 |
| `com.pousadamarealta.config` | 1 | 101 |
| `com.pousadamarealta.controller` | 4 | 405 |
| `com.pousadamarealta.domain` | 6 | 268 |
| `com.pousadamarealta.event` | 5 | 46 |
| `com.pousadamarealta.notification` | 5 | 195 |
| `com.pousadamarealta.repository` | 3 | 367 |
| `com.pousadamarealta.service` | 9 | 335 |
| `com.pousadamarealta.util` | 2 | 74 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.event
com.pousadamarealta -> com.pousadamarealta.notification
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.domain
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.event -> com.pousadamarealta.domain
com.pousadamarealta.notification -> com.pousadamarealta.domain
com.pousadamarealta.notification -> com.pousadamarealta.event
com.pousadamarealta.notification -> com.pousadamarealta.service
com.pousadamarealta.notification -> com.pousadamarealta.util
com.pousadamarealta.repository -> com.pousadamarealta.config
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.event
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
com.pousadamarealta.util -> com.pousadamarealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.config` | 0 | 2 |
| `com.pousadamarealta.controller` | 3 | 1 |
| `com.pousadamarealta.domain` | 0 | 6 |
| `com.pousadamarealta.event` | 1 | 3 |
| `com.pousadamarealta.notification` | 4 | 1 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 4 | 3 |
| `com.pousadamarealta.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.config` |  | sim |  |  | sim |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.event` |  |  |  |  |  |  |
| `com.pousadamarealta.notification` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  267 src/main/java/com/pousadamarealta/controller/ReservationController.java
  234 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  169 src/main/java/com/pousadamarealta/service/ReservationService.java
  154 src/main/java/com/pousadamarealta/domain/Reservation.java
  101 src/main/java/com/pousadamarealta/config/Database.java
   95 src/main/java/com/pousadamarealta/Main.java
   82 src/main/java/com/pousadamarealta/service/ReportService.java
   69 src/main/java/com/pousadamarealta/controller/ReportController.java
   67 src/main/java/com/pousadamarealta/service/PricingService.java
   67 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```

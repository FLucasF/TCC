# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 13 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 87 |
| `com.pousadamarealta.config` | 1 | 82 |
| `com.pousadamarealta.controller` | 4 | 283 |
| `com.pousadamarealta.domain` | 7 | 418 |
| `com.pousadamarealta.exception` | 1 | 20 |
| `com.pousadamarealta.service` | 4 | 641 |
| `com.pousadamarealta.service.rateplan` | 6 | 244 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.config
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.domain
com.pousadamarealta.controller -> com.pousadamarealta.exception
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.service -> com.pousadamarealta.config
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.service.rateplan
com.pousadamarealta.service.rateplan -> com.pousadamarealta.domain
com.pousadamarealta.service.rateplan -> com.pousadamarealta.exception
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.config` | 0 | 2 |
| `com.pousadamarealta.controller` | 3 | 1 |
| `com.pousadamarealta.domain` | 0 | 3 |
| `com.pousadamarealta.exception` | 0 | 4 |
| `com.pousadamarealta.service` | 4 | 2 |
| `com.pousadamarealta.service.rateplan` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.config` |  | sim |  |  |  |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.service` |  | sim |  |  |  |  |
| `com.pousadamarealta.service.rateplan` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  393 src/main/java/com/pousadamarealta/service/ReservationService.java
  165 src/main/java/com/pousadamarealta/domain/Reservation.java
  158 src/main/java/com/pousadamarealta/controller/ReservationController.java
  120 src/main/java/com/pousadamarealta/service/ReportService.java
  115 src/main/java/com/pousadamarealta/domain/Document.java
   87 src/main/java/com/pousadamarealta/Main.java
   82 src/main/java/com/pousadamarealta/config/Database.java
   67 src/main/java/com/pousadamarealta/service/RoomService.java
   61 src/main/java/com/pousadamarealta/service/rateplan/StandardRatePlan.java
   61 src/main/java/com/pousadamarealta/service/MessageService.java
```

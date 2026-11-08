# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 19 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 86 |
| `com.pousadamarealta.controller` | 4 | 293 |
| `com.pousadamarealta.dto` | 6 | 89 |
| `com.pousadamarealta.exception` | 1 | 20 |
| `com.pousadamarealta.model` | 6 | 296 |
| `com.pousadamarealta.repository` | 4 | 395 |
| `com.pousadamarealta.service` | 4 | 370 |
| `com.pousadamarealta.service.rateplan` | 8 | 273 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.dto
com.pousadamarealta -> com.pousadamarealta.exception
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.service.rateplan
com.pousadamarealta.controller -> com.pousadamarealta.dto
com.pousadamarealta.controller -> com.pousadamarealta.exception
com.pousadamarealta.controller -> com.pousadamarealta.model
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.exception
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.service.rateplan
com.pousadamarealta.service.rateplan -> com.pousadamarealta.exception
com.pousadamarealta.service.rateplan -> com.pousadamarealta.model
com.pousadamarealta.service.rateplan -> com.pousadamarealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 6 | 0 |
| `com.pousadamarealta.controller` | 5 | 1 |
| `com.pousadamarealta.dto` | 0 | 2 |
| `com.pousadamarealta.exception` | 0 | 4 |
| `com.pousadamarealta.model` | 0 | 4 |
| `com.pousadamarealta.repository` | 1 | 3 |
| `com.pousadamarealta.service` | 4 | 3 |
| `com.pousadamarealta.service.rateplan` | 3 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta.service.rateplan
com.pousadamarealta.service
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.controller` | sim |  |  |  |  |  |
| `com.pousadamarealta.dto` |  |  | sim |  |  |  |
| `com.pousadamarealta.exception` |  |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.service.rateplan` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  237 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  165 src/main/java/com/pousadamarealta/model/Reservation.java
  163 src/main/java/com/pousadamarealta/controller/ReservationController.java
  155 src/main/java/com/pousadamarealta/service/ReservationService.java
   89 src/main/java/com/pousadamarealta/service/ReportService.java
   86 src/main/java/com/pousadamarealta/Application.java
   82 src/main/java/com/pousadamarealta/service/NotificationService.java
   68 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   63 src/main/java/com/pousadamarealta/repository/Database.java
   61 src/main/java/com/pousadamarealta/controller/ReportController.java
```

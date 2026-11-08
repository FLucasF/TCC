# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 67 |
| `com.pousadamarealta.controller` | 4 | 263 |
| `com.pousadamarealta.dto` | 2 | 133 |
| `com.pousadamarealta.model` | 6 | 302 |
| `com.pousadamarealta.repository` | 4 | 424 |
| `com.pousadamarealta.service` | 11 | 533 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.controller
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.controller -> com.pousadamarealta.dto
com.pousadamarealta.controller -> com.pousadamarealta.model
com.pousadamarealta.controller -> com.pousadamarealta.repository
com.pousadamarealta.controller -> com.pousadamarealta.service
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.dto
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.controller` | 4 | 1 |
| `com.pousadamarealta.dto` | 0 | 2 |
| `com.pousadamarealta.model` | 0 | 3 |
| `com.pousadamarealta.repository` | 1 | 3 |
| `com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.controller` | sim |  | sim |  |  |  |
| `com.pousadamarealta.dto` |  |  |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  | sim |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  277 src/main/java/com/pousadamarealta/service/ReservationService.java
  215 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  167 src/main/java/com/pousadamarealta/service/PricingService.java
  165 src/main/java/com/pousadamarealta/model/Reservation.java
  152 src/main/java/com/pousadamarealta/controller/ReservationController.java
  114 src/main/java/com/pousadamarealta/dto/ReceiptResponse.java
  100 src/main/java/com/pousadamarealta/repository/DatabaseManager.java
   89 src/main/java/com/pousadamarealta/service/ReportService.java
   67 src/main/java/com/pousadamarealta/Main.java
   59 src/main/java/com/pousadamarealta/model/Message.java
```

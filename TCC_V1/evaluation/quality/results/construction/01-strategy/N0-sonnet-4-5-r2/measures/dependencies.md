# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 65 |
| `com.pousadamarealta.api` | 7 | 341 |
| `com.pousadamarealta.domain` | 6 | 336 |
| `com.pousadamarealta.repository` | 4 | 437 |
| `com.pousadamarealta.service` | 11 | 494 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 3 | 0 |
| `com.pousadamarealta.api` | 3 | 1 |
| `com.pousadamarealta.domain` | 0 | 3 |
| `com.pousadamarealta.repository` | 1 | 3 |
| `com.pousadamarealta.service` | 2 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  | sim |  |  |  |
| `com.pousadamarealta.api` | sim |  | sim |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  221 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  205 src/main/java/com/pousadamarealta/domain/Reservation.java
  180 src/main/java/com/pousadamarealta/api/ApiController.java
  166 src/main/java/com/pousadamarealta/service/PricingService.java
  142 src/main/java/com/pousadamarealta/service/ReservationService.java
   88 src/main/java/com/pousadamarealta/repository/Database.java
   80 src/main/java/com/pousadamarealta/service/ReportService.java
   71 src/main/java/com/pousadamarealta/repository/MessageRepository.java
   65 src/main/java/com/pousadamarealta/App.java
   64 src/main/java/com/pousadamarealta/service/DocumentService.java
```

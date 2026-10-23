# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 94 |
| `com.pousadamarealta.api` | 7 | 357 |
| `com.pousadamarealta.domain` | 8 | 168 |
| `com.pousadamarealta.infra` | 1 | 80 |
| `com.pousadamarealta.messaging` | 8 | 171 |
| `com.pousadamarealta.repository` | 3 | 353 |
| `com.pousadamarealta.service` | 10 | 452 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.messaging
com.pousadamarealta.api -> com.pousadamarealta.repository
com.pousadamarealta.api -> com.pousadamarealta.service
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.infra
com.pousadamarealta -> com.pousadamarealta.repository
com.pousadamarealta -> com.pousadamarealta.service
com.pousadamarealta.domain -> com.pousadamarealta.messaging
com.pousadamarealta.repository -> com.pousadamarealta.domain
com.pousadamarealta.repository -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.domain
com.pousadamarealta.service -> com.pousadamarealta.messaging
com.pousadamarealta.service -> com.pousadamarealta.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.api` | 4 | 1 |
| `com.pousadamarealta.domain` | 1 | 3 |
| `com.pousadamarealta.infra` | 0 | 1 |
| `com.pousadamarealta.messaging` | 0 | 4 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.service` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  | sim |  |
| `com.pousadamarealta.api` | sim |  |  |  |  |  |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.infra` |  | sim |  |  |  |  |
| `com.pousadamarealta.messaging` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  231 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  204 src/main/java/com/pousadamarealta/api/ApiController.java
  186 src/main/java/com/pousadamarealta/service/ReservationService.java
  119 src/main/java/com/pousadamarealta/service/MessageService.java
  100 src/main/java/com/pousadamarealta/domain/Reservation.java
   94 src/main/java/com/pousadamarealta/App.java
   81 src/main/java/com/pousadamarealta/service/ReportService.java
   80 src/main/java/com/pousadamarealta/infra/DatabaseSetup.java
   66 src/main/java/com/pousadamarealta/service/PricingService.java
   61 src/main/java/com/pousadamarealta/repository/RoomRepository.java
```

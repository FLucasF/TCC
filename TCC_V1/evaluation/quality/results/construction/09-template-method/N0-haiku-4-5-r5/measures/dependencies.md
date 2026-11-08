# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 2 | 47 |
| `com.pousadamarealta.db` | 1 | 98 |
| `com.pousadamarealta.model` | 4 | 118 |
| `com.pousadamarealta.repository` | 3 | 244 |
| `com.pousadamarealta.routes` | 7 | 371 |
| `com.pousadamarealta.service` | 5 | 408 |
| `com.pousadamarealta.util` | 3 | 83 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.db
com.pousadamarealta -> com.pousadamarealta.routes
com.pousadamarealta.repository -> com.pousadamarealta.db
com.pousadamarealta.repository -> com.pousadamarealta.model
com.pousadamarealta.routes -> com.pousadamarealta.model
com.pousadamarealta.routes -> com.pousadamarealta.repository
com.pousadamarealta.routes -> com.pousadamarealta.service
com.pousadamarealta.routes -> com.pousadamarealta.util
com.pousadamarealta.service -> com.pousadamarealta.model
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.util
com.pousadamarealta.util -> com.pousadamarealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 2 | 0 |
| `com.pousadamarealta.db` | 0 | 2 |
| `com.pousadamarealta.model` | 0 | 4 |
| `com.pousadamarealta.repository` | 2 | 2 |
| `com.pousadamarealta.routes` | 4 | 1 |
| `com.pousadamarealta.service` | 3 | 1 |
| `com.pousadamarealta.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.db` |  | sim |  |  |  |  |
| `com.pousadamarealta.model` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.routes` | sim |  | sim |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  210 src/main/java/com/pousadamarealta/routes/ReservationRoutes.java
  179 src/main/java/com/pousadamarealta/service/ReportService.java
  154 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
  144 src/main/java/com/pousadamarealta/service/ReservationService.java
   98 src/main/java/com/pousadamarealta/db/Database.java
   91 src/main/java/com/pousadamarealta/routes/ReportRoutes.java
   58 src/main/java/com/pousadamarealta/service/PricingService.java
   48 src/main/java/com/pousadamarealta/repository/RoomRepository.java
   48 src/main/java/com/pousadamarealta/model/Reservation.java
   47 src/main/java/com/pousadamarealta/Application.java
```

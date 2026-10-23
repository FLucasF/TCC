# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 34 |
| `com.pousada.marealta.db` | 1 | 98 |
| `com.pousada.marealta.exception` | 2 | 14 |
| `com.pousada.marealta.model` | 3 | 85 |
| `com.pousada.marealta.routes` | 4 | 420 |
| `com.pousada.marealta.service` | 4 | 527 |
| `com.pousada.marealta.util` | 3 | 121 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.db
com.pousada.marealta -> com.pousada.marealta.routes
com.pousada.marealta.routes -> com.pousada.marealta.exception
com.pousada.marealta.routes -> com.pousada.marealta.model
com.pousada.marealta.routes -> com.pousada.marealta.service
com.pousada.marealta.service -> com.pousada.marealta.db
com.pousada.marealta.service -> com.pousada.marealta.exception
com.pousada.marealta.service -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 2 | 0 |
| `com.pousada.marealta.db` | 0 | 2 |
| `com.pousada.marealta.exception` | 0 | 2 |
| `com.pousada.marealta.model` | 0 | 2 |
| `com.pousada.marealta.routes` | 3 | 1 |
| `com.pousada.marealta.service` | 4 | 1 |
| `com.pousada.marealta.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.db` |  | sim |  |  |  |  |
| `com.pousada.marealta.exception` |  |  |  |  |  |  |
| `com.pousada.marealta.model` |  |  |  |  |  |  |
| `com.pousada.marealta.routes` | sim |  | sim |  |  |  |
| `com.pousada.marealta.service` |  | sim |  |  |  |  |
| `com.pousada.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  286 src/main/java/com/pousada/marealta/routes/ReservationRoutes.java
  281 src/main/java/com/pousada/marealta/service/ReservationService.java
  101 src/main/java/com/pousada/marealta/service/MessageService.java
   98 src/main/java/com/pousada/marealta/db/Database.java
   93 src/main/java/com/pousada/marealta/service/ReportService.java
   80 src/main/java/com/pousada/marealta/util/PriceCalculator.java
   76 src/main/java/com/pousada/marealta/routes/ReportRoutes.java
   52 src/main/java/com/pousada/marealta/service/RoomService.java
   47 src/main/java/com/pousada/marealta/model/Reservation.java
   41 src/main/java/com/pousada/marealta/util/EmailValidator.java
```

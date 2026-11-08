# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 8 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 1 | 18 |
| `com.marealta.api` | 2 | 38 |
| `com.marealta.api.handlers` | 9 | 366 |
| `com.marealta.database` | 3 | 128 |
| `com.marealta.domain` | 3 | 81 |
| `com.marealta.service` | 7 | 703 |

## As dependências entre os pacotes do projeto

```
com.marealta.api -> com.marealta.api.handlers
com.marealta.api.handlers -> com.marealta.api
com.marealta.api.handlers -> com.marealta.domain
com.marealta.api.handlers -> com.marealta.service
com.marealta -> com.marealta.api
com.marealta -> com.marealta.database
com.marealta.service -> com.marealta.database
com.marealta.service -> com.marealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 2 | 0 |
| `com.marealta.api` | 1 | 2 |
| `com.marealta.api.handlers` | 3 | 1 |
| `com.marealta.database` | 0 | 2 |
| `com.marealta.domain` | 0 | 2 |
| `com.marealta.service` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.marealta.api
com.marealta.api.handlers
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim |  |  |  |  |  |
| `com.marealta.api` | sim |  |  |  |  |  |
| `com.marealta.api.handlers` | sim |  |  |  |  | com.google |
| `com.marealta.database` |  | sim |  |  | sim |  |
| `com.marealta.domain` |  |  |  |  |  |  |
| `com.marealta.service` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  306 src/main/java/com/marealta/service/ReservationService.java
  271 src/main/java/com/marealta/api/handlers/ReservationHandler.java
  130 src/main/java/com/marealta/service/ReportService.java
  110 src/main/java/com/marealta/service/PricingService.java
   66 src/main/java/com/marealta/service/RoomService.java
   61 src/main/java/com/marealta/database/SchemaCreator.java
   50 src/main/java/com/marealta/api/handlers/ReportHandler.java
   48 src/main/java/com/marealta/domain/Reservation.java
   46 src/main/java/com/marealta/service/ReceiptService.java
   45 src/main/java/com/marealta/service/MessageService.java
```

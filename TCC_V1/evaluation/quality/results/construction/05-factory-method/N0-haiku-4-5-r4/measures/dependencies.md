# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 2 | 41 |
| `com.pousada.marealta.database` | 1 | 122 |
| `com.pousada.marealta.dto` | 3 | 171 |
| `com.pousada.marealta.handlers` | 8 | 386 |
| `com.pousada.marealta.model` | 5 | 177 |
| `com.pousada.marealta.repository` | 3 | 295 |
| `com.pousada.marealta.service` | 3 | 244 |
| `com.pousada.marealta.util` | 3 | 202 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.database
com.pousada.marealta -> com.pousada.marealta.handlers
com.pousada.marealta.handlers -> com.pousada.marealta.dto
com.pousada.marealta.handlers -> com.pousada.marealta.model
com.pousada.marealta.handlers -> com.pousada.marealta.repository
com.pousada.marealta.handlers -> com.pousada.marealta.service
com.pousada.marealta.handlers -> com.pousada.marealta.util
com.pousada.marealta.repository -> com.pousada.marealta.database
com.pousada.marealta.repository -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.dto
com.pousada.marealta.service -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.repository
com.pousada.marealta.service -> com.pousada.marealta.util
com.pousada.marealta.util -> com.pousada.marealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 2 | 0 |
| `com.pousada.marealta.database` | 0 | 2 |
| `com.pousada.marealta.dto` | 0 | 2 |
| `com.pousada.marealta.handlers` | 5 | 1 |
| `com.pousada.marealta.model` | 0 | 4 |
| `com.pousada.marealta.repository` | 2 | 2 |
| `com.pousada.marealta.service` | 4 | 1 |
| `com.pousada.marealta.util` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  |  |
| `com.pousada.marealta.database` |  | sim |  |  | sim |  |
| `com.pousada.marealta.dto` |  |  | sim |  |  |  |
| `com.pousada.marealta.handlers` | sim |  | sim |  |  |  |
| `com.pousada.marealta.model` |  |  |  |  |  |  |
| `com.pousada.marealta.repository` |  | sim |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |
| `com.pousada.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  224 src/main/java/com/pousada/marealta/handlers/ReservationHandler.java
  193 src/main/java/com/pousada/marealta/service/ReservationService.java
  192 src/main/java/com/pousada/marealta/repository/ReservationRepository.java
  132 src/main/java/com/pousada/marealta/util/DocumentFormatter.java
  122 src/main/java/com/pousada/marealta/database/Database.java
  118 src/main/java/com/pousada/marealta/model/Reservation.java
  116 src/main/java/com/pousada/marealta/handlers/ReportHandler.java
   70 src/main/java/com/pousada/marealta/util/PriceCalculator.java
   68 src/main/java/com/pousada/marealta/dto/ReservationResponse.java
   62 src/main/java/com/pousada/marealta/dto/DocumentResponse.java
```

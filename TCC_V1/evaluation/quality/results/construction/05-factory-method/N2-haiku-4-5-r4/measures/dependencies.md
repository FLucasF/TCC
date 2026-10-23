# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.posadamareaita` | 1 | 32 |
| `com.posadamareaita.api` | 4 | 323 |
| `com.posadamareaita.database` | 1 | 108 |
| `com.posadamareaita.domain` | 6 | 177 |
| `com.posadamareaita.repository` | 3 | 276 |
| `com.posadamareaita.service` | 4 | 385 |
| `com.posadamareaita.util` | 5 | 135 |

## As dependências entre os pacotes do projeto

```
com.posadamareaita.api -> com.posadamareaita.domain
com.posadamareaita.api -> com.posadamareaita.repository
com.posadamareaita.api -> com.posadamareaita.service
com.posadamareaita -> com.posadamareaita.api
com.posadamareaita -> com.posadamareaita.database
com.posadamareaita.repository -> com.posadamareaita.database
com.posadamareaita.repository -> com.posadamareaita.domain
com.posadamareaita.service -> com.posadamareaita.domain
com.posadamareaita.service -> com.posadamareaita.repository
com.posadamareaita.service -> com.posadamareaita.util
com.posadamareaita.util -> com.posadamareaita.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.posadamareaita` | 2 | 0 |
| `com.posadamareaita.api` | 3 | 1 |
| `com.posadamareaita.database` | 0 | 2 |
| `com.posadamareaita.domain` | 0 | 4 |
| `com.posadamareaita.repository` | 2 | 2 |
| `com.posadamareaita.service` | 3 | 1 |
| `com.posadamareaita.util` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.posadamareaita` | sim |  |  |  |  |  |
| `com.posadamareaita.api` | sim |  |  |  |  | com.google |
| `com.posadamareaita.database` |  | sim |  |  | sim |  |
| `com.posadamareaita.domain` |  |  |  |  |  |  |
| `com.posadamareaita.repository` |  | sim |  |  |  |  |
| `com.posadamareaita.service` |  |  |  |  |  |  |
| `com.posadamareaita.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  312 src/main/java/com/posadamareaita/api/Api.java
  189 src/main/java/com/posadamareaita/service/ReservationService.java
  173 src/main/java/com/posadamareaita/repository/ReservationRepository.java
  118 src/main/java/com/posadamareaita/service/DocumentService.java
  108 src/main/java/com/posadamareaita/database/Database.java
   88 src/main/java/com/posadamareaita/domain/Reservation.java
   67 src/main/java/com/posadamareaita/service/ReportService.java
   56 src/main/java/com/posadamareaita/repository/MessageRepository.java
   52 src/main/java/com/posadamareaita/util/PriceCalculator.java
   47 src/main/java/com/posadamareaita/repository/RoomRepository.java
```

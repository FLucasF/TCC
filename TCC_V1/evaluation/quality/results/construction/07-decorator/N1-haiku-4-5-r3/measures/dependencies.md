# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 8 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 319 |
| `com.pousada.marealta.api` | 1 | 11 |
| `com.pousada.marealta.db` | 4 | 352 |
| `com.pousada.marealta.model` | 8 | 136 |
| `com.pousada.marealta.service` | 4 | 282 |
| `com.pousada.marealta.util` | 5 | 122 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.api
com.pousada.marealta -> com.pousada.marealta.db
com.pousada.marealta -> com.pousada.marealta.model
com.pousada.marealta -> com.pousada.marealta.service
com.pousada.marealta.db -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.db
com.pousada.marealta.service -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 4 | 0 |
| `com.pousada.marealta.api` | 0 | 1 |
| `com.pousada.marealta.db` | 1 | 2 |
| `com.pousada.marealta.model` | 0 | 3 |
| `com.pousada.marealta.service` | 3 | 1 |
| `com.pousada.marealta.util` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.api` |  |  |  |  |  |  |
| `com.pousada.marealta.db` |  | sim |  |  |  |  |
| `com.pousada.marealta.model` |  |  |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |
| `com.pousada.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  319 src/main/java/com/pousada/marealta/Application.java
  183 src/main/java/com/pousada/marealta/db/ReservationRepository.java
  129 src/main/java/com/pousada/marealta/service/ReservationService.java
   75 src/main/java/com/pousada/marealta/service/ReportService.java
   71 src/main/java/com/pousada/marealta/db/Database.java
   52 src/main/java/com/pousada/marealta/db/MessageRepository.java
   48 src/main/java/com/pousada/marealta/util/PriceCalculator.java
   46 src/main/java/com/pousada/marealta/service/MessageService.java
   46 src/main/java/com/pousada/marealta/db/RoomRepository.java
   45 src/main/java/com/pousada/marealta/model/Reservation.java
```

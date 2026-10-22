# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 2 | 67 |
| `com.pousada.api` | 2 | 390 |
| `com.pousada.api.dto` | 6 | 435 |
| `com.pousada.database` | 1 | 104 |
| `com.pousada.models` | 4 | 338 |
| `com.pousada.repositories` | 3 | 318 |
| `com.pousada.services` | 4 | 350 |
| `com.pousada.utils` | 5 | 158 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.api.dto
com.pousada.api -> com.pousada.models
com.pousada.api -> com.pousada.repositories
com.pousada.api -> com.pousada.services
com.pousada.api -> com.pousada.utils
com.pousada -> com.pousada.api
com.pousada -> com.pousada.database
com.pousada -> com.pousada.repositories
com.pousada -> com.pousada.services
com.pousada.repositories -> com.pousada.database
com.pousada.repositories -> com.pousada.models
com.pousada.services -> com.pousada.models
com.pousada.services -> com.pousada.repositories
com.pousada.services -> com.pousada.utils
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 0 |
| `com.pousada.api` | 5 | 1 |
| `com.pousada.api.dto` | 0 | 1 |
| `com.pousada.database` | 0 | 2 |
| `com.pousada.models` | 0 | 3 |
| `com.pousada.repositories` | 2 | 3 |
| `com.pousada.services` | 3 | 2 |
| `com.pousada.utils` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  | com.google |
| `com.pousada.api` | sim |  |  |  |  | com.google |
| `com.pousada.api.dto` |  |  |  |  |  |  |
| `com.pousada.database` |  | sim |  |  |  |  |
| `com.pousada.models` |  |  |  |  |  |  |
| `com.pousada.repositories` |  | sim |  |  |  |  |
| `com.pousada.services` |  |  |  |  |  |  |
| `com.pousada.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  324 src/main/java/com/pousada/api/ApiController.java
  196 src/main/java/com/pousada/repositories/ReservationRepository.java
  162 src/main/java/com/pousada/services/ReservationService.java
  153 src/main/java/com/pousada/models/Reservation.java
  150 src/main/java/com/pousada/api/dto/ReservationResponse.java
  114 src/main/java/com/pousada/api/dto/ReceiptResponse.java
  104 src/main/java/com/pousada/database/Database.java
   84 src/main/java/com/pousada/models/Receipt.java
   80 src/main/java/com/pousada/services/ReportService.java
   76 src/main/java/com/pousada/api/dto/CreateReservationRequest.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 10 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 36 |
| `com.pousada.api` | 1 | 264 |
| `com.pousada.db` | 1 | 249 |
| `com.pousada.models` | 9 | 245 |
| `com.pousada.services` | 3 | 372 |
| `com.pousada.utils` | 3 | 114 |

## As dependências entre os pacotes do projeto

```
com.pousada.api -> com.pousada.db
com.pousada.api -> com.pousada.models
com.pousada.api -> com.pousada.services
com.pousada -> com.pousada.api
com.pousada -> com.pousada.db
com.pousada -> com.pousada.services
com.pousada.db -> com.pousada.models
com.pousada.services -> com.pousada.db
com.pousada.services -> com.pousada.models
com.pousada.services -> com.pousada.utils
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 3 | 0 |
| `com.pousada.api` | 3 | 1 |
| `com.pousada.db` | 1 | 3 |
| `com.pousada.models` | 0 | 3 |
| `com.pousada.services` | 3 | 2 |
| `com.pousada.utils` | 0 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.api` | sim |  |  |  |  | com.google |
| `com.pousada.db` |  | sim |  |  | sim |  |
| `com.pousada.models` |  |  |  |  |  |  |
| `com.pousada.services` |  |  |  |  |  |  |
| `com.pousada.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  264 src/main/java/com/pousada/api/ApiRoutes.java
  249 src/main/java/com/pousada/db/Database.java
  224 src/main/java/com/pousada/services/ReservationService.java
   88 src/main/java/com/pousada/models/Reservation.java
   83 src/main/java/com/pousada/services/ReportService.java
   78 src/main/java/com/pousada/models/Receipt.java
   71 src/main/java/com/pousada/utils/PriceCalculator.java
   65 src/main/java/com/pousada/services/MessageService.java
   45 src/main/java/com/pousada/models/Room.java
   43 src/main/java/com/pousada/utils/EmailValidator.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 43 |
| `com.pousada.database` | 1 | 93 |
| `com.pousada.dto` | 6 | 137 |
| `com.pousada.models` | 3 | 79 |
| `com.pousada.routes` | 4 | 441 |
| `com.pousada.services` | 5 | 610 |
| `com.pousada.utils` | 3 | 124 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.database
com.pousada -> com.pousada.routes
com.pousada.dto -> com.pousada.models
com.pousada.routes -> com.pousada.database
com.pousada.routes -> com.pousada.dto
com.pousada.routes -> com.pousada.models
com.pousada.routes -> com.pousada.services
com.pousada.routes -> com.pousada.utils
com.pousada.services -> com.pousada.database
com.pousada.services -> com.pousada.models
com.pousada.services -> com.pousada.utils
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 2 | 0 |
| `com.pousada.database` | 0 | 3 |
| `com.pousada.dto` | 1 | 1 |
| `com.pousada.models` | 0 | 3 |
| `com.pousada.routes` | 5 | 1 |
| `com.pousada.services` | 3 | 1 |
| `com.pousada.utils` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.database` |  | sim |  |  | sim |  |
| `com.pousada.dto` |  |  |  |  |  |  |
| `com.pousada.models` |  |  |  |  |  |  |
| `com.pousada.routes` | sim |  |  |  |  |  |
| `com.pousada.services` |  | sim |  |  |  |  |
| `com.pousada.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  295 src/main/java/com/pousada/routes/ReservationRoutes.java
  215 src/main/java/com/pousada/services/ReportService.java
  196 src/main/java/com/pousada/services/ReservationService.java
   98 src/main/java/com/pousada/routes/ReportRoutes.java
   93 src/main/java/com/pousada/database/Database.java
   90 src/main/java/com/pousada/services/MessageService.java
   62 src/main/java/com/pousada/services/RoomService.java
   60 src/main/java/com/pousada/utils/PricingUtils.java
   53 src/main/java/com/pousada/utils/FormatUtils.java
   47 src/main/java/com/pousada/services/VoucherService.java
```

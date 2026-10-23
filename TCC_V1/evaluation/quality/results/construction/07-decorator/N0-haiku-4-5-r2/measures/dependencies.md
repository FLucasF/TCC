# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 9 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 266 |
| `com.pousada.marealta.api` | 1 | 16 |
| `com.pousada.marealta.db` | 1 | 342 |
| `com.pousada.marealta.models` | 6 | 154 |
| `com.pousada.marealta.services` | 2 | 213 |
| `com.pousada.marealta.util` | 3 | 143 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.api
com.pousada.marealta -> com.pousada.marealta.db
com.pousada.marealta -> com.pousada.marealta.models
com.pousada.marealta -> com.pousada.marealta.services
com.pousada.marealta.db -> com.pousada.marealta.models
com.pousada.marealta.services -> com.pousada.marealta.db
com.pousada.marealta.services -> com.pousada.marealta.models
com.pousada.marealta.services -> com.pousada.marealta.util
com.pousada.marealta.util -> com.pousada.marealta.models
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 4 | 0 |
| `com.pousada.marealta.api` | 0 | 1 |
| `com.pousada.marealta.db` | 1 | 2 |
| `com.pousada.marealta.models` | 0 | 4 |
| `com.pousada.marealta.services` | 3 | 1 |
| `com.pousada.marealta.util` | 1 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim | sim |  |  |  |  |
| `com.pousada.marealta.api` |  |  | sim |  |  |  |
| `com.pousada.marealta.db` |  | sim |  |  |  |  |
| `com.pousada.marealta.models` |  |  | sim |  |  |  |
| `com.pousada.marealta.services` |  | sim |  |  |  |  |
| `com.pousada.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  342 src/main/java/com/pousada/marealta/db/Database.java
  266 src/main/java/com/pousada/marealta/Main.java
  148 src/main/java/com/pousada/marealta/services/ReservationService.java
   65 src/main/java/com/pousada/marealta/services/EmailService.java
   63 src/main/java/com/pousada/marealta/util/PricingUtil.java
   51 src/main/java/com/pousada/marealta/models/Reservation.java
   42 src/main/java/com/pousada/marealta/util/EmailValidator.java
   42 src/main/java/com/pousada/marealta/models/Receipt.java
   38 src/main/java/com/pousada/marealta/util/ReceiptFormatter.java
   24 src/main/java/com/pousada/marealta/models/Message.java
```

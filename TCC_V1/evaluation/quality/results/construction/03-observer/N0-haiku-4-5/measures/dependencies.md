# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 9 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 5 | 789 |
| `com.pousadamarealta.api` | 4 | 85 |
| `com.pousadamarealta.exceptions` | 3 | 30 |
| `com.pousadamarealta.models` | 6 | 94 |
| `com.pousadamarealta.notifications` | 4 | 124 |
| `com.pousadamarealta.utils` | 3 | 108 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.models
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.exceptions
com.pousadamarealta -> com.pousadamarealta.models
com.pousadamarealta -> com.pousadamarealta.notifications
com.pousadamarealta -> com.pousadamarealta.utils
com.pousadamarealta.notifications -> com.pousadamarealta
com.pousadamarealta.notifications -> com.pousadamarealta.models
com.pousadamarealta.notifications -> com.pousadamarealta.utils
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 5 | 1 |
| `com.pousadamarealta.api` | 1 | 1 |
| `com.pousadamarealta.exceptions` | 0 | 1 |
| `com.pousadamarealta.models` | 0 | 3 |
| `com.pousadamarealta.notifications` | 3 | 1 |
| `com.pousadamarealta.utils` | 0 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta
com.pousadamarealta.notifications
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim |  |  |  |  |
| `com.pousadamarealta.api` |  |  |  |  |  |  |
| `com.pousadamarealta.exceptions` |  |  |  |  |  |  |
| `com.pousadamarealta.models` |  |  |  |  |  |  |
| `com.pousadamarealta.notifications` |  |  |  |  |  |  |
| `com.pousadamarealta.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  366 src/main/java/com/pousadamarealta/ReservationService.java
  150 src/main/java/com/pousadamarealta/Main.java
  133 src/main/java/com/pousadamarealta/ReportService.java
   92 src/main/java/com/pousadamarealta/Database.java
   54 src/main/java/com/pousadamarealta/utils/PriceCalculator.java
   48 src/main/java/com/pousadamarealta/MessageService.java
   45 src/main/java/com/pousadamarealta/models/Reservation.java
   44 src/main/java/com/pousadamarealta/notifications/GovernanceNotificationListener.java
   44 src/main/java/com/pousadamarealta/api/ReservationResponse.java
   39 src/main/java/com/pousadamarealta/notifications/FinanceNotificationListener.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 1 | 47 |
| `com.pousadamarealta.api` | 5 | 373 |
| `com.pousadamarealta.domain` | 8 | 362 |
| `com.pousadamarealta.events` | 2 | 33 |
| `com.pousadamarealta.listeners` | 3 | 120 |
| `com.pousadamarealta.services` | 5 | 754 |
| `com.pousadamarealta.utils` | 2 | 85 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta.api -> com.pousadamarealta.domain
com.pousadamarealta.api -> com.pousadamarealta.services
com.pousadamarealta -> com.pousadamarealta.api
com.pousadamarealta -> com.pousadamarealta.events
com.pousadamarealta -> com.pousadamarealta.listeners
com.pousadamarealta -> com.pousadamarealta.services
com.pousadamarealta.events -> com.pousadamarealta.domain
com.pousadamarealta.listeners -> com.pousadamarealta.domain
com.pousadamarealta.listeners -> com.pousadamarealta.events
com.pousadamarealta.listeners -> com.pousadamarealta.services
com.pousadamarealta.listeners -> com.pousadamarealta.utils
com.pousadamarealta.services -> com.pousadamarealta.domain
com.pousadamarealta.services -> com.pousadamarealta.events
com.pousadamarealta.services -> com.pousadamarealta.utils
com.pousadamarealta.utils -> com.pousadamarealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 4 | 0 |
| `com.pousadamarealta.api` | 2 | 1 |
| `com.pousadamarealta.domain` | 0 | 5 |
| `com.pousadamarealta.events` | 1 | 3 |
| `com.pousadamarealta.listeners` | 4 | 1 |
| `com.pousadamarealta.services` | 3 | 3 |
| `com.pousadamarealta.utils` | 1 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.api` | sim |  |  |  |  | com.google |
| `com.pousadamarealta.domain` |  |  |  |  |  |  |
| `com.pousadamarealta.events` |  |  |  |  |  |  |
| `com.pousadamarealta.listeners` |  |  |  |  |  |  |
| `com.pousadamarealta.services` |  | sim |  |  | sim |  |
| `com.pousadamarealta.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  416 src/main/java/com/pousadamarealta/services/Database.java
  272 src/main/java/com/pousadamarealta/api/ReservationsController.java
  173 src/main/java/com/pousadamarealta/services/ReservationService.java
  120 src/main/java/com/pousadamarealta/domain/Reservation.java
   81 src/main/java/com/pousadamarealta/services/ReportService.java
   78 src/main/java/com/pousadamarealta/domain/Document.java
   60 src/main/java/com/pousadamarealta/services/DocumentService.java
   57 src/main/java/com/pousadamarealta/api/ReportsController.java
   54 src/main/java/com/pousadamarealta/utils/PricingCalculator.java
   47 src/main/java/com/pousadamarealta/Main.java
```

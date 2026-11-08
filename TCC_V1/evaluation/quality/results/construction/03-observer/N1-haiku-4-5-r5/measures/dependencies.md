# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 11 pacotes, 28 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 2 | 300 |
| `com.pousada.marealta.api` | 1 | 19 |
| `com.pousada.marealta.api.request` | 1 | 12 |
| `com.pousada.marealta.api.response` | 3 | 94 |
| `com.pousada.marealta.db` | 4 | 408 |
| `com.pousada.marealta.enums` | 4 | 33 |
| `com.pousada.marealta.model` | 4 | 178 |
| `com.pousada.marealta.notification` | 4 | 50 |
| `com.pousada.marealta.notification.notifiers` | 3 | 102 |
| `com.pousada.marealta.service` | 2 | 215 |
| `com.pousada.marealta.util` | 3 | 151 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta.api.response -> com.pousada.marealta.enums
com.pousada.marealta.api.response -> com.pousada.marealta.model
com.pousada.marealta -> com.pousada.marealta.api
com.pousada.marealta -> com.pousada.marealta.api.request
com.pousada.marealta -> com.pousada.marealta.api.response
com.pousada.marealta -> com.pousada.marealta.db
com.pousada.marealta -> com.pousada.marealta.enums
com.pousada.marealta -> com.pousada.marealta.model
com.pousada.marealta -> com.pousada.marealta.notification
com.pousada.marealta -> com.pousada.marealta.notification.notifiers
com.pousada.marealta -> com.pousada.marealta.service
com.pousada.marealta.db -> com.pousada.marealta.enums
com.pousada.marealta.db -> com.pousada.marealta.model
com.pousada.marealta.model -> com.pousada.marealta.enums
com.pousada.marealta.notification -> com.pousada.marealta.model
com.pousada.marealta.notification.notifiers -> com.pousada.marealta.db
com.pousada.marealta.notification.notifiers -> com.pousada.marealta.enums
com.pousada.marealta.notification.notifiers -> com.pousada.marealta.model
com.pousada.marealta.notification.notifiers -> com.pousada.marealta.notification
com.pousada.marealta.notification.notifiers -> com.pousada.marealta.util
com.pousada.marealta.service -> com.pousada.marealta.db
com.pousada.marealta.service -> com.pousada.marealta.enums
com.pousada.marealta.service -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.notification
com.pousada.marealta.service -> com.pousada.marealta.util
com.pousada.marealta.util -> com.pousada.marealta.db
com.pousada.marealta.util -> com.pousada.marealta.enums
com.pousada.marealta.util -> com.pousada.marealta.model
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 9 | 0 |
| `com.pousada.marealta.api` | 0 | 1 |
| `com.pousada.marealta.api.request` | 0 | 1 |
| `com.pousada.marealta.api.response` | 2 | 1 |
| `com.pousada.marealta.db` | 2 | 4 |
| `com.pousada.marealta.enums` | 0 | 7 |
| `com.pousada.marealta.model` | 1 | 7 |
| `com.pousada.marealta.notification` | 1 | 3 |
| `com.pousada.marealta.notification.notifiers` | 5 | 1 |
| `com.pousada.marealta.service` | 5 | 1 |
| `com.pousada.marealta.util` | 3 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.api` |  |  |  |  |  |  |
| `com.pousada.marealta.api.request` |  |  |  |  |  |  |
| `com.pousada.marealta.api.response` |  |  |  |  |  |  |
| `com.pousada.marealta.db` |  | sim |  |  | sim |  |
| `com.pousada.marealta.enums` |  |  |  |  |  |  |
| `com.pousada.marealta.model` |  |  |  |  |  |  |
| `com.pousada.marealta.notification` |  |  |  |  |  |  |
| `com.pousada.marealta.notification.notifiers` |  |  |  |  |  |  |
| `com.pousada.marealta.service` |  |  |  |  |  |  |
| `com.pousada.marealta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  300 src/main/java/com/pousada/marealta/App.java
  204 src/main/java/com/pousada/marealta/db/ReservationRepository.java
  145 src/main/java/com/pousada/marealta/service/ReservationService.java
  108 src/main/java/com/pousada/marealta/db/Database.java
   70 src/main/java/com/pousada/marealta/service/ReportService.java
   69 src/main/java/com/pousada/marealta/model/Reservation.java
   59 src/main/java/com/pousada/marealta/util/PricingCalculator.java
   50 src/main/java/com/pousada/marealta/model/Document.java
   49 src/main/java/com/pousada/marealta/util/DocumentGenerator.java
   49 src/main/java/com/pousada/marealta/db/MessageRepository.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada.marealta` | 1 | 89 |
| `com.pousada.marealta.handler` | 5 | 426 |
| `com.pousada.marealta.model` | 11 | 450 |
| `com.pousada.marealta.persistence` | 4 | 430 |
| `com.pousada.marealta.service` | 7 | 464 |
| `com.pousada.marealta.util` | 4 | 91 |

## As dependências entre os pacotes do projeto

```
com.pousada.marealta -> com.pousada.marealta.handler
com.pousada.marealta -> com.pousada.marealta.model
com.pousada.marealta -> com.pousada.marealta.persistence
com.pousada.marealta -> com.pousada.marealta.service
com.pousada.marealta -> com.pousada.marealta.util
com.pousada.marealta.handler -> com.pousada.marealta.model
com.pousada.marealta.handler -> com.pousada.marealta.persistence
com.pousada.marealta.handler -> com.pousada.marealta.service
com.pousada.marealta.persistence -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.model
com.pousada.marealta.service -> com.pousada.marealta.persistence
com.pousada.marealta.service -> com.pousada.marealta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada.marealta` | 5 | 0 |
| `com.pousada.marealta.handler` | 3 | 1 |
| `com.pousada.marealta.model` | 0 | 4 |
| `com.pousada.marealta.persistence` | 1 | 3 |
| `com.pousada.marealta.service` | 3 | 2 |
| `com.pousada.marealta.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada.marealta` | sim | sim |  |  |  |  |
| `com.pousada.marealta.handler` | sim |  |  |  |  | com.google |
| `com.pousada.marealta.model` |  |  |  |  |  |  |
| `com.pousada.marealta.persistence` |  | sim |  |  | sim |  |
| `com.pousada.marealta.service` |  | sim |  |  |  |  |
| `com.pousada.marealta.util` | sim |  |  |  |  | com.google |

## As maiores classes (linhas, src/main)

```
  238 src/main/java/com/pousada/marealta/handler/ReservationHandler.java
  196 src/main/java/com/pousada/marealta/persistence/ReservationDao.java
  167 src/main/java/com/pousada/marealta/service/ReservationService.java
  162 src/main/java/com/pousada/marealta/model/Reservation.java
  125 src/main/java/com/pousada/marealta/service/PricingService.java
  100 src/main/java/com/pousada/marealta/persistence/Database.java
   89 src/main/java/com/pousada/marealta/Application.java
   86 src/main/java/com/pousada/marealta/service/ReportService.java
   86 src/main/java/com/pousada/marealta/service/MessageService.java
   78 src/main/java/com/pousada/marealta/model/Receipt.java
```

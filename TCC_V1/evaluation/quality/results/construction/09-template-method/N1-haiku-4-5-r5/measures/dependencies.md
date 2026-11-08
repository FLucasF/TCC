# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareaalta` | 2 | 348 |
| `com.pousadamareaalta.database` | 4 | 362 |
| `com.pousadamareaalta.domain` | 4 | 122 |
| `com.pousadamareaalta.service` | 2 | 194 |
| `com.pousadamareaalta.service.report` | 5 | 248 |
| `com.pousadamareaalta.util` | 2 | 101 |

## As dependências entre os pacotes do projeto

```
com.pousadamareaalta -> com.pousadamareaalta.database
com.pousadamareaalta -> com.pousadamareaalta.domain
com.pousadamareaalta -> com.pousadamareaalta.service
com.pousadamareaalta -> com.pousadamareaalta.service.report
com.pousadamareaalta.database -> com.pousadamareaalta.domain
com.pousadamareaalta.service -> com.pousadamareaalta.database
com.pousadamareaalta.service -> com.pousadamareaalta.domain
com.pousadamareaalta.service -> com.pousadamareaalta.util
com.pousadamareaalta.service.report -> com.pousadamareaalta.database
com.pousadamareaalta.service.report -> com.pousadamareaalta.domain
com.pousadamareaalta.service.report -> com.pousadamareaalta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareaalta` | 4 | 0 |
| `com.pousadamareaalta.database` | 1 | 3 |
| `com.pousadamareaalta.domain` | 0 | 4 |
| `com.pousadamareaalta.service` | 3 | 1 |
| `com.pousadamareaalta.service.report` | 3 | 1 |
| `com.pousadamareaalta.util` | 0 | 2 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareaalta` | sim | sim | sim |  |  |  |
| `com.pousadamareaalta.database` |  | sim |  |  |  |  |
| `com.pousadamareaalta.domain` |  |  | sim |  |  |  |
| `com.pousadamareaalta.service` |  | sim |  |  |  |  |
| `com.pousadamareaalta.service.report` |  | sim |  |  |  |  |
| `com.pousadamareaalta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  348 src/main/java/com/pousadamareaalta/Main.java
  174 src/main/java/com/pousadamareaalta/database/ReservationRepository.java
  133 src/main/java/com/pousadamareaalta/service/ReservationService.java
   94 src/main/java/com/pousadamareaalta/database/Database.java
   71 src/main/java/com/pousadamareaalta/service/report/RatePlansReport.java
   64 src/main/java/com/pousadamareaalta/util/PriceCalculator.java
   63 src/main/java/com/pousadamareaalta/service/report/TourismTaxReport.java
   61 src/main/java/com/pousadamareaalta/service/MessageService.java
   59 src/main/java/com/pousadamareaalta/domain/Reservation.java
   54 src/main/java/com/pousadamareaalta/service/report/RevenueReport.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 11 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 9 | 377 |
| `com.pousada.api.dto` | 5 | 111 |
| `com.pousada.application` | 2 | 228 |
| `com.pousada.domain` | 9 | 263 |
| `com.pousada.infrastructure` | 4 | 334 |
| `com.pousada.reports` | 5 | 353 |

## As dependências entre os pacotes do projeto

```
com.pousada.api.dto -> com.pousada.domain
com.pousada.application -> com.pousada.domain
com.pousada.application -> com.pousada.infrastructure
com.pousada -> com.pousada.api.dto
com.pousada -> com.pousada.application
com.pousada -> com.pousada.domain
com.pousada -> com.pousada.infrastructure
com.pousada -> com.pousada.reports
com.pousada.infrastructure -> com.pousada.domain
com.pousada.reports -> com.pousada.domain
com.pousada.reports -> com.pousada.infrastructure
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 5 | 0 |
| `com.pousada.api.dto` | 1 | 1 |
| `com.pousada.application` | 2 | 1 |
| `com.pousada.domain` | 0 | 5 |
| `com.pousada.infrastructure` | 1 | 3 |
| `com.pousada.reports` | 2 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  | com.google |
| `com.pousada.api.dto` |  |  |  |  |  |  |
| `com.pousada.application` |  |  |  |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.infrastructure` |  | sim |  |  |  |  |
| `com.pousada.reports` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  208 src/main/java/com/pousada/application/ReservationService.java
  173 src/main/java/com/pousada/ReservationsHandler.java
  159 src/main/java/com/pousada/infrastructure/ReservationRepository.java
  104 src/main/java/com/pousada/ReportsHandler.java
   93 src/main/java/com/pousada/domain/Reservation.java
   91 src/main/java/com/pousada/reports/RatePlansReport.java
   83 src/main/java/com/pousada/reports/RevenueReport.java
   80 src/main/java/com/pousada/reports/TourismTaxReport.java
   75 src/main/java/com/pousada/infrastructure/Database.java
   65 src/main/java/com/pousada/reports/OccupancyReport.java
```

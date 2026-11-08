# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 1 | 42 |
| `com.pousada.domain` | 16 | 359 |
| `com.pousada.report` | 8 | 296 |
| `com.pousada.repository` | 6 | 294 |
| `com.pousada.service` | 3 | 111 |
| `com.pousada.web` | 2 | 154 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.domain
com.pousada -> com.pousada.report
com.pousada -> com.pousada.repository
com.pousada -> com.pousada.service
com.pousada -> com.pousada.web
com.pousada.report -> com.pousada.domain
com.pousada.report -> com.pousada.repository
com.pousada.repository -> com.pousada.domain
com.pousada.service -> com.pousada.domain
com.pousada.service -> com.pousada.repository
com.pousada.web -> com.pousada.domain
com.pousada.web -> com.pousada.report
com.pousada.web -> com.pousada.repository
com.pousada.web -> com.pousada.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 5 | 0 |
| `com.pousada.domain` | 0 | 5 |
| `com.pousada.report` | 2 | 2 |
| `com.pousada.repository` | 1 | 4 |
| `com.pousada.service` | 2 | 2 |
| `com.pousada.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim |  |  |  |  |  |
| `com.pousada.domain` |  |  |  |  |  |  |
| `com.pousada.report` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  | sim |  |
| `com.pousada.service` |  |  |  |  |  |  |
| `com.pousada.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  129 src/main/java/com/pousada/repository/PostgresReservationRepository.java
  112 src/main/java/com/pousada/domain/Reservation.java
   86 src/main/java/com/pousada/repository/Database.java
   81 src/main/java/com/pousada/web/Api.java
   73 src/main/java/com/pousada/web/RequestParser.java
   73 src/main/java/com/pousada/service/ReservationService.java
   63 src/main/java/com/pousada/report/OccupancyReport.java
   51 src/main/java/com/pousada/report/MonthlyReport.java
   47 src/main/java/com/pousada/repository/PostgresMessageRepository.java
   44 src/main/java/com/pousada/report/Report.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 39 |
| `br.com.marealta.domain` | 14 | 291 |
| `br.com.marealta.report` | 11 | 313 |
| `br.com.marealta.service` | 3 | 139 |
| `br.com.marealta.store` | 6 | 318 |
| `br.com.marealta.web` | 2 | 161 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.report
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.store
br.com.marealta -> br.com.marealta.web
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.store
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.store
br.com.marealta.store -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.report
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 0 |
| `br.com.marealta.domain` | 0 | 4 |
| `br.com.marealta.report` | 2 | 2 |
| `br.com.marealta.service` | 2 | 2 |
| `br.com.marealta.store` | 1 | 3 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.report` |  |  |  |  |  |  |
| `br.com.marealta.service` |  |  |  |  |  |  |
| `br.com.marealta.store` |  | sim |  |  | sim |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  269 src/main/java/br/com/marealta/store/PostgresStore.java
  101 src/main/java/br/com/marealta/service/ReservationService.java
   81 src/main/java/br/com/marealta/web/App.java
   80 src/main/java/br/com/marealta/web/RequestParser.java
   75 src/main/java/br/com/marealta/domain/Reservation.java
   49 src/main/java/br/com/marealta/report/MonthlyReport.java
   43 src/main/java/br/com/marealta/domain/Receipt.java
   41 src/main/java/br/com/marealta/store/Tx.java
   40 src/main/java/br/com/marealta/report/TourismTaxReport.java
   39 src/main/java/br/com/marealta/Main.java
```

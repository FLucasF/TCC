# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.maretalta` | 1 | 28 |
| `br.com.maretalta.domain` | 10 | 239 |
| `br.com.maretalta.report` | 9 | 260 |
| `br.com.maretalta.service` | 4 | 165 |
| `br.com.maretalta.store` | 7 | 406 |
| `br.com.maretalta.web` | 1 | 178 |

## As dependências entre os pacotes do projeto

```
br.com.maretalta -> br.com.maretalta.store
br.com.maretalta -> br.com.maretalta.web
br.com.maretalta.report -> br.com.maretalta.domain
br.com.maretalta.report -> br.com.maretalta.service
br.com.maretalta.report -> br.com.maretalta.store
br.com.maretalta.service -> br.com.maretalta.domain
br.com.maretalta.service -> br.com.maretalta.store
br.com.maretalta.store -> br.com.maretalta.domain
br.com.maretalta.web -> br.com.maretalta.domain
br.com.maretalta.web -> br.com.maretalta.report
br.com.maretalta.web -> br.com.maretalta.service
br.com.maretalta.web -> br.com.maretalta.store
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.maretalta` | 2 | 0 |
| `br.com.maretalta.domain` | 0 | 4 |
| `br.com.maretalta.report` | 3 | 1 |
| `br.com.maretalta.service` | 2 | 2 |
| `br.com.maretalta.store` | 1 | 4 |
| `br.com.maretalta.web` | 4 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.maretalta` | sim |  |  |  |  |  |
| `br.com.maretalta.domain` |  |  |  |  |  |  |
| `br.com.maretalta.report` |  |  |  |  |  |  |
| `br.com.maretalta.service` |  |  |  |  |  |  |
| `br.com.maretalta.store` |  | sim |  |  | sim |  |
| `br.com.maretalta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  288 src/main/java/br/com/maretalta/store/PostgresStore.java
  178 src/main/java/br/com/maretalta/web/App.java
  123 src/main/java/br/com/maretalta/service/ReservationService.java
   74 src/main/java/br/com/maretalta/store/InMemoryStore.java
   52 src/main/java/br/com/maretalta/domain/Reservation.java
   44 src/main/java/br/com/maretalta/store/Store.java
   44 src/main/java/br/com/maretalta/domain/Receipt.java
   40 src/main/java/br/com/maretalta/report/MonthlyReport.java
   39 src/main/java/br/com/maretalta/report/Reports.java
   37 src/main/java/br/com/maretalta/report/TourismTaxReport.java
```

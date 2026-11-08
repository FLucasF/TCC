# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 59 |
| `br.com.marealta.domain` | 9 | 146 |
| `br.com.marealta.report` | 10 | 277 |
| `br.com.marealta.service` | 4 | 241 |
| `br.com.marealta.store` | 7 | 473 |
| `br.com.marealta.web` | 6 | 164 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.report
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.store
br.com.marealta -> br.com.marealta.web
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.service
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
| `br.com.marealta` | 5 | 0 |
| `br.com.marealta.domain` | 0 | 5 |
| `br.com.marealta.report` | 3 | 2 |
| `br.com.marealta.service` | 2 | 3 |
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
  340 src/main/java/br/com/marealta/store/PostgresStore.java
  153 src/main/java/br/com/marealta/service/ReservationService.java
   86 src/main/java/br/com/marealta/store/MemoryStore.java
   69 src/main/java/br/com/marealta/web/RequestParser.java
   66 src/main/java/br/com/marealta/web/WebApp.java
   58 src/main/java/br/com/marealta/service/Receipt.java
   44 src/main/java/br/com/marealta/domain/Reservation.java
   41 src/main/java/br/com/marealta/domain/Pricing.java
   39 src/main/java/br/com/marealta/store/Tx.java
   39 src/main/java/br/com/marealta/report/OccupancyReport.java
```

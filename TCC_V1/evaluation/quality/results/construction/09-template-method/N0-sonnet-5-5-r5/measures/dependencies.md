# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 11 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 84 |
| `br.com.marealta.domain` | 15 | 432 |
| `br.com.marealta.report` | 10 | 288 |
| `br.com.marealta.store` | 8 | 378 |
| `br.com.marealta.web` | 2 | 136 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.report
br.com.marealta -> br.com.marealta.store
br.com.marealta -> br.com.marealta.web
br.com.marealta.domain -> br.com.marealta.store
br.com.marealta.report -> br.com.marealta.domain
br.com.marealta.report -> br.com.marealta.store
br.com.marealta.store -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.report
br.com.marealta.web -> br.com.marealta.store
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 0 |
| `br.com.marealta.domain` | 1 | 4 |
| `br.com.marealta.report` | 2 | 2 |
| `br.com.marealta.store` | 1 | 4 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.domain
br.com.marealta.store
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim | sim |  | sim |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.report` |  |  |  |  |  |  |
| `br.com.marealta.store` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  248 src/main/java/br/com/marealta/store/JdbcStore.java
  130 src/main/java/br/com/marealta/domain/ReservationService.java
   88 src/main/java/br/com/marealta/store/MemoryStore.java
   70 src/main/java/br/com/marealta/web/RequestParser.java
   66 src/main/java/br/com/marealta/web/Routes.java
   60 src/main/java/br/com/marealta/App.java
   48 src/main/java/br/com/marealta/domain/Reservation.java
   42 src/main/java/br/com/marealta/report/RatePlanReport.java
   41 src/main/java/br/com/marealta/report/TourismTaxReport.java
   40 src/main/java/br/com/marealta/domain/DomainException.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 11 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.mealta` | 1 | 39 |
| `br.com.mealta.domain` | 17 | 499 |
| `br.com.mealta.report` | 8 | 329 |
| `br.com.mealta.store` | 8 | 371 |
| `br.com.mealta.web` | 7 | 216 |

## As dependências entre os pacotes do projeto

```
br.com.mealta -> br.com.mealta.domain
br.com.mealta -> br.com.mealta.report
br.com.mealta -> br.com.mealta.store
br.com.mealta -> br.com.mealta.web
br.com.mealta.domain -> br.com.mealta.store
br.com.mealta.report -> br.com.mealta.domain
br.com.mealta.report -> br.com.mealta.store
br.com.mealta.store -> br.com.mealta.domain
br.com.mealta.web -> br.com.mealta.domain
br.com.mealta.web -> br.com.mealta.report
br.com.mealta.web -> br.com.mealta.store
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.mealta` | 4 | 0 |
| `br.com.mealta.domain` | 1 | 4 |
| `br.com.mealta.report` | 2 | 2 |
| `br.com.mealta.store` | 1 | 4 |
| `br.com.mealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.mealta.domain
br.com.mealta.store
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.mealta` | sim |  |  |  |  |  |
| `br.com.mealta.domain` |  |  |  |  |  |  |
| `br.com.mealta.report` |  |  |  |  |  |  |
| `br.com.mealta.store` |  | sim |  |  | sim |  |
| `br.com.mealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  153 src/main/java/br/com/mealta/store/PgReservationRepository.java
  116 src/main/java/br/com/mealta/domain/Reservation.java
   97 src/main/java/br/com/mealta/domain/ReservationService.java
   86 src/main/java/br/com/mealta/web/Api.java
   78 src/main/java/br/com/mealta/web/CreateReservationParser.java
   72 src/main/java/br/com/mealta/store/Database.java
   65 src/main/java/br/com/mealta/report/ReportCatalog.java
   55 src/main/java/br/com/mealta/report/TourismTaxReport.java
   55 src/main/java/br/com/mealta/report/RatePlanReport.java
   54 src/main/java/br/com/mealta/report/RevenueReport.java
```

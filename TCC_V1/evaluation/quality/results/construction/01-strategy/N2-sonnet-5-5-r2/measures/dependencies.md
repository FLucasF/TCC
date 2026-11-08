# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 13 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 23 |
| `br.com.marealta.domain` | 13 | 346 |
| `br.com.marealta.persistence` | 4 | 260 |
| `br.com.marealta.plan` | 8 | 234 |
| `br.com.marealta.service` | 5 | 182 |
| `br.com.marealta.web` | 3 | 240 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.domain -> br.com.marealta.plan
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.plan
br.com.marealta.plan -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.persistence
br.com.marealta.service -> br.com.marealta.plan
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.plan
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 0 |
| `br.com.marealta.domain` | 1 | 4 |
| `br.com.marealta.persistence` | 2 | 2 |
| `br.com.marealta.plan` | 1 | 4 |
| `br.com.marealta.service` | 3 | 2 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.domain
br.com.marealta.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.plan` |  |  |  |  |  |  |
| `br.com.marealta.service` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  131 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  109 src/main/java/br/com/marealta/domain/Reservation.java
  102 src/main/java/br/com/marealta/service/ReservationService.java
   94 src/main/java/br/com/marealta/web/Api.java
   91 src/main/java/br/com/marealta/persistence/Database.java
   74 src/main/java/br/com/marealta/web/BookingParser.java
   72 src/main/java/br/com/marealta/web/Views.java
   51 src/main/java/br/com/marealta/service/ReportService.java
   39 src/main/java/br/com/marealta/plan/WeekendPackagePlan.java
   38 src/main/java/br/com/marealta/persistence/MessageRepository.java
```

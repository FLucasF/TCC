# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 42 |
| `br.com.marealta.db` | 3 | 253 |
| `br.com.marealta.model` | 4 | 28 |
| `br.com.marealta.plan` | 12 | 236 |
| `br.com.marealta.service` | 4 | 229 |
| `br.com.marealta.web` | 1 | 209 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.db
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.db -> br.com.marealta.model
br.com.marealta.plan -> br.com.marealta
br.com.marealta.plan -> br.com.marealta.model
br.com.marealta.service -> br.com.marealta
br.com.marealta.service -> br.com.marealta.db
br.com.marealta.service -> br.com.marealta.model
br.com.marealta.service -> br.com.marealta.plan
br.com.marealta.web -> br.com.marealta
br.com.marealta.web -> br.com.marealta.model
br.com.marealta.web -> br.com.marealta.plan
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 3 |
| `br.com.marealta.db` | 1 | 2 |
| `br.com.marealta.model` | 0 | 4 |
| `br.com.marealta.plan` | 2 | 2 |
| `br.com.marealta.service` | 4 | 2 |
| `br.com.marealta.web` | 4 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.service
br.com.marealta.plan
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.service
--- um ciclo:
br.com.marealta
br.com.marealta.web
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.db` |  | sim |  |  | sim |  |
| `br.com.marealta.model` |  |  |  |  |  |  |
| `br.com.marealta.plan` |  |  |  |  |  |  |
| `br.com.marealta.service` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  209 src/main/java/br/com/marealta/web/App.java
  144 src/main/java/br/com/marealta/db/Repository.java
  129 src/main/java/br/com/marealta/service/ReservationService.java
  109 src/main/java/br/com/marealta/db/Database.java
   63 src/main/java/br/com/marealta/service/ReportService.java
   37 src/main/java/br/com/marealta/service/Receipt.java
   34 src/main/java/br/com/marealta/plan/Money.java
   31 src/main/java/br/com/marealta/plan/AbstractRatePlan.java
   28 src/main/java/br/com/marealta/plan/WeekendPackagePlan.java
   28 src/main/java/br/com/marealta/plan/RatePlans.java
```

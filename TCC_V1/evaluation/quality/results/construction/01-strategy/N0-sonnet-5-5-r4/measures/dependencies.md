# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 3 pacotes, 5 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 16 | 618 |
| `br.com.marealta.plan` | 9 | 204 |
| `br.com.marealta.web` | 1 | 206 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.plan
br.com.marealta -> br.com.marealta.web
br.com.marealta.plan -> br.com.marealta
br.com.marealta.web -> br.com.marealta
br.com.marealta.web -> br.com.marealta.plan
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 2 | 2 |
| `br.com.marealta.plan` | 1 | 2 |
| `br.com.marealta.web` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.plan
--- um ciclo:
br.com.marealta
br.com.marealta.web
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  | sim |  |
| `br.com.marealta.plan` |  |  |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  206 src/main/java/br/com/marealta/web/Api.java
  157 src/main/java/br/com/marealta/Repository.java
  122 src/main/java/br/com/marealta/ReservationService.java
  115 src/main/java/br/com/marealta/Db.java
   49 src/main/java/br/com/marealta/ReportService.java
   47 src/main/java/br/com/marealta/Receipt.java
   37 src/main/java/br/com/marealta/plan/PricingContext.java
   35 src/main/java/br/com/marealta/Money.java
   29 src/main/java/br/com/marealta/plan/WeekendPackagePlan.java
   29 src/main/java/br/com/marealta/plan/CorporatePlan.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 2 pacotes, 2 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 15 | 801 |
| `br.com.marealta.plan` | 8 | 199 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.plan
br.com.marealta.plan -> br.com.marealta
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 1 | 1 |
| `br.com.marealta.plan` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim | sim |  | sim |  |
| `br.com.marealta.plan` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  282 src/main/java/br/com/marealta/ReservationService.java
  153 src/main/java/br/com/marealta/App.java
  126 src/main/java/br/com/marealta/Db.java
   79 src/main/java/br/com/marealta/RequestParser.java
   40 src/main/java/br/com/marealta/plan/RatePlan.java
   39 src/main/java/br/com/marealta/plan/WeekendPackagePlan.java
   38 src/main/java/br/com/marealta/Receipt.java
   37 src/main/java/br/com/marealta/plan/PercentagePlan.java
   36 src/main/java/br/com/marealta/Pricing.java
   36 src/main/java/br/com/marealta/Money.java
```

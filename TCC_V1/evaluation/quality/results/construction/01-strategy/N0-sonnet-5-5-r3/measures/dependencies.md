# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 2 pacotes, 2 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 17 | 820 |
| `com.marealta.plan` | 10 | 175 |

## As dependências entre os pacotes do projeto

```
com.marealta -> com.marealta.plan
com.marealta.plan -> com.marealta
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 1 | 1 |
| `com.marealta.plan` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.marealta
com.marealta.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim | sim | sim |  | sim |  |
| `com.marealta.plan` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  187 src/main/java/com/marealta/App.java
  157 src/main/java/com/marealta/Store.java
  149 src/main/java/com/marealta/ReservationService.java
   98 src/main/java/com/marealta/Db.java
   45 src/main/java/com/marealta/Reports.java
   40 src/main/java/com/marealta/Money.java
   35 src/main/java/com/marealta/Receipt.java
   30 src/main/java/com/marealta/plan/WeekendPackagePlan.java
   29 src/main/java/com/marealta/Seasons.java
   28 src/main/java/com/marealta/plan/RatePlans.java
```

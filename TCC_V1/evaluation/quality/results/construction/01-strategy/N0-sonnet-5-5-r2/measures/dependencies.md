# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 2 pacotes, 2 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 14 | 745 |
| `pousada.plan` | 9 | 218 |

## As dependências entre os pacotes do projeto

```
pousada.plan -> pousada
pousada -> pousada.plan
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 1 | 1 |
| `pousada.plan` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada
pousada.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim | sim |  | sim |  |
| `pousada.plan` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  293 src/main/java/pousada/ReservationService.java
  215 src/main/java/pousada/Api.java
   83 src/main/java/pousada/Db.java
   41 src/main/java/pousada/Receipt.java
   36 src/main/java/pousada/Money.java
   33 src/main/java/pousada/plan/WeekendPackagePlan.java
   30 src/main/java/pousada/plan/RatePlans.java
   29 src/main/java/pousada/plan/CorporatePlan.java
   28 src/main/java/pousada/plan/BookingContext.java
   26 src/main/java/pousada/plan/LongStayPlan.java
```

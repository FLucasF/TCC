# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 22 | 914 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.pousada` | sim | sim | sim |  | sim |  |

## As maiores classes (linhas, src/main)

```
  262 src/main/java/br/com/marealta/pousada/Repo.java
  167 src/main/java/br/com/marealta/pousada/ReservationService.java
  135 src/main/java/br/com/marealta/pousada/Api.java
   86 src/main/java/br/com/marealta/pousada/MailDispatcher.java
   75 src/main/java/br/com/marealta/pousada/Db.java
   64 src/main/java/br/com/marealta/pousada/MailConfig.java
   58 src/main/java/br/com/marealta/pousada/Pricing.java
   46 src/main/java/br/com/marealta/pousada/Domain.java
   21 src/main/java/br/com/marealta/pousada/Main.java
```

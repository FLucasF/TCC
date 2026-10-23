# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.pousada` | 27 | 995 |

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
  262 src/main/java/br/com/marealta/pousada/ReservationService.java
  200 src/main/java/br/com/marealta/pousada/WebApp.java
  107 src/main/java/br/com/marealta/pousada/Database.java
  101 src/main/java/br/com/marealta/pousada/MessageDispatcher.java
   80 src/main/java/br/com/marealta/pousada/Config.java
   68 src/main/java/br/com/marealta/pousada/JdbcMessageStore.java
   56 src/main/java/br/com/marealta/pousada/Receipt.java
   45 src/main/java/br/com/marealta/pousada/Pricing.java
   35 src/main/java/br/com/marealta/pousada/Domain.java
   23 src/main/java/br/com/marealta/pousada/Main.java
```

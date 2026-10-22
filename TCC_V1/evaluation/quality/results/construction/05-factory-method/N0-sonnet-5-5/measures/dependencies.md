# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 24 | 902 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim | sim |  | sim |  |

## As maiores classes (linhas, src/main)

```
  239 src/main/java/br/com/marealta/Api.java
  173 src/main/java/br/com/marealta/Repository.java
  158 src/main/java/br/com/marealta/Service.java
  105 src/main/java/br/com/marealta/Db.java
   83 src/main/java/br/com/marealta/Documents.java
   49 src/main/java/br/com/marealta/Domain.java
   45 src/main/java/br/com/marealta/Pricing.java
   35 src/main/java/br/com/marealta/Money.java
   15 src/main/java/br/com/marealta/Main.java
```

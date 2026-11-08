# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 28 | 1087 |

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
  352 src/main/java/br/com/marealta/Service.java
  236 src/main/java/br/com/marealta/Main.java
  154 src/main/java/br/com/marealta/Messaging.java
  119 src/main/java/br/com/marealta/Database.java
   72 src/main/java/br/com/marealta/Config.java
   53 src/main/java/br/com/marealta/Receipt.java
   51 src/main/java/br/com/marealta/Pricing.java
   30 src/main/java/br/com/marealta/Domain.java
   20 src/main/java/br/com/marealta/ApiException.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 25 | 956 |

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
  236 src/main/java/br/com/marealta/Api.java
  228 src/main/java/br/com/marealta/Store.java
  153 src/main/java/br/com/marealta/ReservationService.java
  100 src/main/java/br/com/marealta/Db.java
   86 src/main/java/br/com/marealta/Documents.java
   41 src/main/java/br/com/marealta/Money.java
   38 src/main/java/br/com/marealta/Pricing.java
   36 src/main/java/br/com/marealta/Model.java
   20 src/main/java/br/com/marealta/ApiException.java
   18 src/main/java/br/com/marealta/Main.java
```

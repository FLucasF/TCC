# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 23 | 946 |

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
  188 src/main/java/br/com/marealta/ReservationService.java
  135 src/main/java/br/com/marealta/App.java
  115 src/main/java/br/com/marealta/Db.java
   99 src/main/java/br/com/marealta/MessagePolicy.java
   86 src/main/java/br/com/marealta/MessageService.java
   71 src/main/java/br/com/marealta/ReportService.java
   43 src/main/java/br/com/marealta/Config.java
   42 src/main/java/br/com/marealta/Documents.java
   36 src/main/java/br/com/marealta/Pricing.java
   30 src/main/java/br/com/marealta/Money.java
```

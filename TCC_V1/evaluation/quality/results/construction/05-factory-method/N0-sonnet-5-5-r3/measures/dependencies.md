# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 23 | 936 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim | sim |  | sim |  |

## As maiores classes (linhas, src/main)

```
  302 src/main/java/pousada/Service.java
  243 src/main/java/pousada/App.java
  122 src/main/java/pousada/Identification.java
  114 src/main/java/pousada/Db.java
   47 src/main/java/pousada/Money.java
   38 src/main/java/pousada/Pricing.java
   30 src/main/java/pousada/Domain.java
   20 src/main/java/pousada/Main.java
   20 src/main/java/pousada/ApiException.java
```

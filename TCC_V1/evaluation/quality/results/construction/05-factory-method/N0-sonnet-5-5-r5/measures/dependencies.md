# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 26 | 903 |

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
  183 src/main/java/pousada/Store.java
  162 src/main/java/pousada/Api.java
  112 src/main/java/pousada/ReservationService.java
   82 src/main/java/pousada/Database.java
   79 src/main/java/pousada/Identification.java
   48 src/main/java/pousada/Reports.java
   37 src/main/java/pousada/Pricing.java
   36 src/main/java/pousada/Money.java
   32 src/main/java/pousada/Reservation.java
   31 src/main/java/pousada/Document.java
```

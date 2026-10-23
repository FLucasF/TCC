# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 23 | 981 |

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
  240 src/main/java/pousada/Api.java
  175 src/main/java/pousada/Store.java
  122 src/main/java/pousada/ReservationService.java
   90 src/main/java/pousada/Database.java
   78 src/main/java/pousada/Documents.java
   41 src/main/java/pousada/Money.java
   32 src/main/java/pousada/Reports.java
   32 src/main/java/pousada/Quote.java
   27 src/main/java/pousada/Reservation.java
   23 src/main/java/pousada/GuestIdentity.java
```

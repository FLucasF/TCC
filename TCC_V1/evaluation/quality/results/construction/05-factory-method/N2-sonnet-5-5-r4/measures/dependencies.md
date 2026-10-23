# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 24 | 1073 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  197 src/main/java/pousada/Repository.java
  164 src/main/java/pousada/ReservationService.java
  143 src/main/java/pousada/Main.java
  102 src/main/java/pousada/Database.java
   60 src/main/java/pousada/Reports.java
   57 src/main/java/pousada/IdentificationTypes.java
   50 src/main/java/pousada/BookingReceipt.java
   47 src/main/java/pousada/Reservation.java
   44 src/main/java/pousada/Document.java
   40 src/main/java/pousada/PortugueseDocument.java
```

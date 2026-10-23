# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.maralta.pousada` | 32 | 1230 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.maralta.pousada` | sim | sim | sim |  | sim |  |

## As maiores classes (linhas, src/main)

```
  148 src/main/java/br/com/maralta/pousada/ReservationRepository.java
  113 src/main/java/br/com/maralta/pousada/Api.java
  101 src/main/java/br/com/maralta/pousada/ReservationService.java
   91 src/main/java/br/com/maralta/pousada/ReservationRequest.java
   85 src/main/java/br/com/maralta/pousada/Database.java
   69 src/main/java/br/com/maralta/pousada/Document.java
   68 src/main/java/br/com/maralta/pousada/Reservation.java
   56 src/main/java/br/com/maralta/pousada/Identifications.java
   47 src/main/java/br/com/maralta/pousada/Language.java
   44 src/main/java/br/com/maralta/pousada/ApiException.java
```

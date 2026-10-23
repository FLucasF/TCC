# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 25 | 1373 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  268 src/main/java/com/pousadamarealta/ReservationService.java
  219 src/main/java/com/pousadamarealta/ReservationController.java
  212 src/main/java/com/pousadamarealta/DocumentService.java
  203 src/main/java/com/pousadamarealta/ReportController.java
   82 src/main/java/com/pousadamarealta/Database.java
   69 src/main/java/com/pousadamarealta/PricingService.java
   56 src/main/java/com/pousadamarealta/Reservation.java
   54 src/main/java/com/pousadamarealta/MessageController.java
   48 src/main/java/com/pousadamarealta/Document.java
   31 src/main/java/com/pousadamarealta/Main.java
```

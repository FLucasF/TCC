# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 27 | 1405 |

## As dependências entre os pacotes do projeto

Nenhuma (um pacote só, ou pacotes que não se usam).

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim | sim |  |  | sim | com.google |

## As maiores classes (linhas, src/main)

```
  314 src/main/java/com/pousada/ReservationService.java
  138 src/main/java/com/pousada/App.java
  110 src/main/java/com/pousada/Database.java
   94 src/main/java/com/pousada/TourismTaxReport.java
   91 src/main/java/com/pousada/RatePlansReport.java
   89 src/main/java/com/pousada/RevenueReport.java
   85 src/main/java/com/pousada/OccupancyReport.java
   50 src/main/java/com/pousada/MessageService.java
   48 src/main/java/com/pousada/RoomService.java
   47 src/main/java/com/pousada/PriceCalculator.java
```

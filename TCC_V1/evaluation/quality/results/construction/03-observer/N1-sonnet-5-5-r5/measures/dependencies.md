# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 1 pacotes, 0 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 27 | 1017 |

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
  136 src/main/java/br/com/marealta/ReservationRepository.java
  129 src/main/java/br/com/marealta/Routes.java
  111 src/main/java/br/com/marealta/ReservationService.java
   85 src/main/java/br/com/marealta/Database.java
   63 src/main/java/br/com/marealta/Reservation.java
   44 src/main/java/br/com/marealta/ReportService.java
   42 src/main/java/br/com/marealta/DomainException.java
   39 src/main/java/br/com/marealta/Receipt.java
   38 src/main/java/br/com/marealta/MessageRepository.java
   37 src/main/java/br/com/marealta/Pricing.java
```

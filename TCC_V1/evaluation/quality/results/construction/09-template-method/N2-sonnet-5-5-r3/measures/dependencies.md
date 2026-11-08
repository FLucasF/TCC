# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 2 pacotes, 2 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.marealta` | 23 | 869 |
| `com.marealta.report` | 8 | 299 |

## As dependências entre os pacotes do projeto

```
com.marealta -> com.marealta.report
com.marealta.report -> com.marealta
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.marealta` | 1 | 1 |
| `com.marealta.report` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.marealta
com.marealta.report
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.marealta` | sim | sim | sim |  |  |  |
| `com.marealta.report` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  129 src/main/java/com/marealta/ReservationRepository.java
  111 src/main/java/com/marealta/App.java
   98 src/main/java/com/marealta/Reservation.java
   87 src/main/java/com/marealta/ReservationService.java
   68 src/main/java/com/marealta/ReservationRequest.java
   54 src/main/java/com/marealta/report/TourismTaxReport.java
   47 src/main/java/com/marealta/MessageRepository.java
   46 src/main/java/com/marealta/report/RevenueReport.java
   46 src/main/java/com/marealta/report/ReportCatalog.java
   46 src/main/java/com/marealta/report/RatePlansReport.java
```

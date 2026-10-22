# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 4 pacotes, 7 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 3 | 475 |
| `com.pousada.model` | 5 | 163 |
| `com.pousada.repository` | 3 | 312 |
| `com.pousada.service` | 5 | 439 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.model
com.pousada -> com.pousada.repository
com.pousada -> com.pousada.service
com.pousada.repository -> com.pousada
com.pousada.repository -> com.pousada.model
com.pousada.service -> com.pousada.model
com.pousada.service -> com.pousada.repository
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 3 | 1 |
| `com.pousada.model` | 0 | 3 |
| `com.pousada.repository` | 2 | 2 |
| `com.pousada.service` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousada
com.pousada.service
com.pousada.repository
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim | sim |  |  | sim | com.google |
| `com.pousada.model` |  |  |  |  |  |  |
| `com.pousada.repository` |  | sim |  |  |  |  |
| `com.pousada.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  333 src/main/java/com/pousada/RouteConfig.java
  191 src/main/java/com/pousada/repository/ReservationRepository.java
  157 src/main/java/com/pousada/service/ReportService.java
  138 src/main/java/com/pousada/service/ReservationService.java
  122 src/main/java/com/pousada/Database.java
   92 src/main/java/com/pousada/model/Reservation.java
   68 src/main/java/com/pousada/repository/MessageRepository.java
   61 src/main/java/com/pousada/service/PricingCalculator.java
   56 src/main/java/com/pousada/service/DocumentService.java
   53 src/main/java/com/pousada/repository/RoomRepository.java
```

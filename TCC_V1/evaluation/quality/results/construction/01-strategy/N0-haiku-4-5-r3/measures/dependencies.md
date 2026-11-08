# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 8 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareaita` | 6 | 542 |
| `com.pousadamareaita.model` | 4 | 209 |
| `com.pousadamareaita.repository` | 3 | 343 |
| `com.pousadamareaita.service` | 3 | 275 |
| `com.pousadamareaita.util` | 2 | 115 |

## As dependências entre os pacotes do projeto

```
com.pousadamareaita -> com.pousadamareaita.model
com.pousadamareaita -> com.pousadamareaita.repository
com.pousadamareaita -> com.pousadamareaita.service
com.pousadamareaita.repository -> com.pousadamareaita
com.pousadamareaita.repository -> com.pousadamareaita.model
com.pousadamareaita.service -> com.pousadamareaita.model
com.pousadamareaita.service -> com.pousadamareaita.repository
com.pousadamareaita.service -> com.pousadamareaita.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareaita` | 3 | 1 |
| `com.pousadamareaita.model` | 0 | 3 |
| `com.pousadamareaita.repository` | 2 | 2 |
| `com.pousadamareaita.service` | 3 | 1 |
| `com.pousadamareaita.util` | 0 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamareaita
com.pousadamareaita.service
com.pousadamareaita.repository
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareaita` | sim | sim | sim |  | sim |  |
| `com.pousadamareaita.model` |  |  |  |  |  |  |
| `com.pousadamareaita.repository` |  | sim |  |  |  |  |
| `com.pousadamareaita.service` |  |  |  |  |  |  |
| `com.pousadamareaita.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  225 src/main/java/com/pousadamareaita/ReservationHandler.java
  209 src/main/java/com/pousadamareaita/service/ReservationService.java
  205 src/main/java/com/pousadamareaita/repository/ReservationRepository.java
  136 src/main/java/com/pousadamareaita/model/Reservation.java
  119 src/main/java/com/pousadamareaita/ReportHandler.java
  115 src/main/java/com/pousadamareaita/util/PricingCalculator.java
   99 src/main/java/com/pousadamareaita/Database.java
   74 src/main/java/com/pousadamareaita/repository/MessageRepository.java
   66 src/main/java/com/pousadamareaita/service/EmailService.java
   64 src/main/java/com/pousadamareaita/repository/RoomRepository.java
```

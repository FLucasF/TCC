# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 9 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamareaalta` | 2 | 132 |
| `com.pousadamareaalta.documents` | 5 | 181 |
| `com.pousadamareaalta.handlers` | 43 | 606 |
| `com.pousadamareaalta.models` | 4 | 136 |
| `com.pousadamareaalta.repositories` | 3 | 307 |
| `com.pousadamareaalta.utils` | 1 | 58 |

## As dependências entre os pacotes do projeto

```
com.pousadamareaalta -> com.pousadamareaalta.handlers
com.pousadamareaalta.documents -> com.pousadamareaalta.models
com.pousadamareaalta.documents -> com.pousadamareaalta.utils
com.pousadamareaalta.handlers -> com.pousadamareaalta.documents
com.pousadamareaalta.handlers -> com.pousadamareaalta.models
com.pousadamareaalta.handlers -> com.pousadamareaalta.repositories
com.pousadamareaalta.handlers -> com.pousadamareaalta.utils
com.pousadamareaalta.repositories -> com.pousadamareaalta
com.pousadamareaalta.repositories -> com.pousadamareaalta.models
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamareaalta` | 1 | 1 |
| `com.pousadamareaalta.documents` | 2 | 1 |
| `com.pousadamareaalta.handlers` | 4 | 1 |
| `com.pousadamareaalta.models` | 0 | 3 |
| `com.pousadamareaalta.repositories` | 2 | 1 |
| `com.pousadamareaalta.utils` | 0 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamareaalta
com.pousadamareaalta.handlers
com.pousadamareaalta.repositories
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamareaalta` | sim | sim |  |  | sim |  |
| `com.pousadamareaalta.documents` |  |  |  |  |  |  |
| `com.pousadamareaalta.handlers` | sim |  | sim |  |  |  |
| `com.pousadamareaalta.models` |  |  |  |  |  |  |
| `com.pousadamareaalta.repositories` |  | sim |  |  |  |  |
| `com.pousadamareaalta.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  428 src/main/java/com/pousadamareaalta/handlers/ReservationHandler.java
  195 src/main/java/com/pousadamareaalta/repositories/ReservationRepository.java
  123 src/main/java/com/pousadamareaalta/handlers/ReportHandler.java
  101 src/main/java/com/pousadamareaalta/Database.java
   61 src/main/java/com/pousadamareaalta/repositories/MessageRepository.java
   58 src/main/java/com/pousadamareaalta/utils/PriceCalculator.java
   57 src/main/java/com/pousadamareaalta/documents/CnpjDocument.java
   56 src/main/java/com/pousadamareaalta/models/Reservation.java
   53 src/main/java/com/pousadamareaalta/documents/CpfDocument.java
   51 src/main/java/com/pousadamareaalta/repositories/RoomRepository.java
```

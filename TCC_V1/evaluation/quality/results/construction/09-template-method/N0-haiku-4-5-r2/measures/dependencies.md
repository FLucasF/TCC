# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 9 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousada` | 4 | 457 |
| `com.pousada.models` | 4 | 178 |
| `com.pousada.repositories` | 3 | 326 |
| `com.pousada.services` | 5 | 424 |
| `com.pousada.utils` | 4 | 127 |

## As dependências entre os pacotes do projeto

```
com.pousada -> com.pousada.models
com.pousada -> com.pousada.repositories
com.pousada -> com.pousada.services
com.pousada -> com.pousada.utils
com.pousada.repositories -> com.pousada
com.pousada.repositories -> com.pousada.models
com.pousada.services -> com.pousada.models
com.pousada.services -> com.pousada.repositories
com.pousada.services -> com.pousada.utils
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousada` | 4 | 1 |
| `com.pousada.models` | 0 | 3 |
| `com.pousada.repositories` | 2 | 2 |
| `com.pousada.services` | 3 | 1 |
| `com.pousada.utils` | 0 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousada
com.pousada.services
com.pousada.repositories
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousada` | sim | sim |  |  | sim | com.google |
| `com.pousada.models` |  |  |  |  |  |  |
| `com.pousada.repositories` |  | sim |  |  | sim |  |
| `com.pousada.services` |  |  |  |  |  |  |
| `com.pousada.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  306 src/main/java/com/pousada/Api.java
  203 src/main/java/com/pousada/repositories/ReservationRepository.java
  165 src/main/java/com/pousada/services/ReportService.java
  149 src/main/java/com/pousada/services/ReservationService.java
  117 src/main/java/com/pousada/Database.java
   90 src/main/java/com/pousada/services/ReceiptService.java
   73 src/main/java/com/pousada/models/Reservation.java
   62 src/main/java/com/pousada/repositories/RoomRepository.java
   61 src/main/java/com/pousada/repositories/MessageRepository.java
   60 src/main/java/com/pousada/utils/PriceCalculator.java
```

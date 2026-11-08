# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 8 pacotes, 16 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `com.pousadamarealta` | 2 | 55 |
| `com.pousadamarealta.database` | 2 | 125 |
| `com.pousadamarealta.models` | 6 | 153 |
| `com.pousadamarealta.notifications` | 5 | 128 |
| `com.pousadamarealta.repository` | 3 | 275 |
| `com.pousadamarealta.routes` | 4 | 272 |
| `com.pousadamarealta.service` | 2 | 250 |
| `com.pousadamarealta.utils` | 2 | 130 |

## As dependências entre os pacotes do projeto

```
com.pousadamarealta -> com.pousadamarealta.database
com.pousadamarealta -> com.pousadamarealta.routes
com.pousadamarealta.notifications -> com.pousadamarealta.models
com.pousadamarealta.notifications -> com.pousadamarealta.repository
com.pousadamarealta.notifications -> com.pousadamarealta.utils
com.pousadamarealta.repository -> com.pousadamarealta.database
com.pousadamarealta.repository -> com.pousadamarealta.models
com.pousadamarealta.routes -> com.pousadamarealta
com.pousadamarealta.routes -> com.pousadamarealta.models
com.pousadamarealta.routes -> com.pousadamarealta.repository
com.pousadamarealta.routes -> com.pousadamarealta.service
com.pousadamarealta.service -> com.pousadamarealta.models
com.pousadamarealta.service -> com.pousadamarealta.notifications
com.pousadamarealta.service -> com.pousadamarealta.repository
com.pousadamarealta.service -> com.pousadamarealta.utils
com.pousadamarealta.utils -> com.pousadamarealta.models
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `com.pousadamarealta` | 2 | 1 |
| `com.pousadamarealta.database` | 0 | 2 |
| `com.pousadamarealta.models` | 0 | 5 |
| `com.pousadamarealta.notifications` | 3 | 1 |
| `com.pousadamarealta.repository` | 2 | 3 |
| `com.pousadamarealta.routes` | 4 | 1 |
| `com.pousadamarealta.service` | 4 | 1 |
| `com.pousadamarealta.utils` | 1 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
com.pousadamarealta
com.pousadamarealta.routes
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `com.pousadamarealta` | sim |  |  |  |  |  |
| `com.pousadamarealta.database` |  | sim |  |  | sim |  |
| `com.pousadamarealta.models` |  |  |  |  |  |  |
| `com.pousadamarealta.notifications` |  |  |  |  |  |  |
| `com.pousadamarealta.repository` |  | sim |  |  |  |  |
| `com.pousadamarealta.routes` | sim |  | sim |  |  |  |
| `com.pousadamarealta.service` |  |  |  |  |  |  |
| `com.pousadamarealta.utils` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  182 src/main/java/com/pousadamarealta/service/ReservationService.java
  174 src/main/java/com/pousadamarealta/routes/ReservationRoutes.java
  171 src/main/java/com/pousadamarealta/repository/ReservationRepository.java
   75 src/main/java/com/pousadamarealta/database/SchemaInitializer.java
   68 src/main/java/com/pousadamarealta/service/ReportService.java
   66 src/main/java/com/pousadamarealta/utils/FormatterUtils.java
   64 src/main/java/com/pousadamarealta/utils/PricingUtils.java
   64 src/main/java/com/pousadamarealta/models/Reservation.java
   58 src/main/java/com/pousadamarealta/routes/ReportRoutes.java
   58 src/main/java/com/pousadamarealta/repository/MessageRepository.java
```

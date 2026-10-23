# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 11 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 3 | 88 |
| `br.com.marealta.db` | 2 | 100 |
| `br.com.marealta.message` | 14 | 320 |
| `br.com.marealta.reservation` | 13 | 569 |
| `br.com.marealta.web` | 4 | 293 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.db
br.com.marealta -> br.com.marealta.message
br.com.marealta -> br.com.marealta.reservation
br.com.marealta -> br.com.marealta.web
br.com.marealta.reservation -> br.com.marealta
br.com.marealta.reservation -> br.com.marealta.db
br.com.marealta.reservation -> br.com.marealta.message
br.com.marealta.reservation -> br.com.marealta.web
br.com.marealta.web -> br.com.marealta.db
br.com.marealta.web -> br.com.marealta.message
br.com.marealta.web -> br.com.marealta.reservation
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 1 |
| `br.com.marealta.db` | 0 | 3 |
| `br.com.marealta.message` | 0 | 3 |
| `br.com.marealta.reservation` | 4 | 2 |
| `br.com.marealta.web` | 3 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.reservation
--- um ciclo:
br.com.marealta.web
br.com.marealta.reservation
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  |  |  |
| `br.com.marealta.db` |  | sim |  |  | sim |  |
| `br.com.marealta.message` |  | sim |  |  |  |  |
| `br.com.marealta.reservation` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  125 src/main/java/br/com/marealta/reservation/ReservationRepository.java
  108 src/main/java/br/com/marealta/reservation/ReservationService.java
  105 src/main/java/br/com/marealta/web/Routes.java
  100 src/main/java/br/com/marealta/db/Database.java
   74 src/main/java/br/com/marealta/web/Views.java
   69 src/main/java/br/com/marealta/web/ReservationRequestParser.java
   68 src/main/java/br/com/marealta/reservation/RoomRepository.java
   60 src/main/java/br/com/marealta/message/Delivery.java
   54 src/main/java/br/com/marealta/reservation/ReportService.java
   53 src/main/java/br/com/marealta/reservation/Reservation.java
```

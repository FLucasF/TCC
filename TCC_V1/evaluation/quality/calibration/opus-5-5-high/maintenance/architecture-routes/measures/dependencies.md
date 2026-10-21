# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 15 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `hotel` | 2 | 84 |
| `hotel.documents` | 2 | 79 |
| `hotel.messages` | 3 | 75 |
| `hotel.reports` | 5 | 258 |
| `hotel.reservations` | 11 | 370 |
| `hotel.rooms` | 3 | 69 |
| `hotel.web` | 5 | 399 |

## As dependências entre os pacotes do projeto

```
hotel.documents -> hotel.reservations
hotel.documents -> hotel.rooms
hotel -> hotel.messages
hotel -> hotel.reports
hotel -> hotel.reservations
hotel -> hotel.rooms
hotel -> hotel.web
hotel.reports -> hotel.reservations
hotel.reports -> hotel.rooms
hotel.reservations -> hotel.rooms
hotel.web -> hotel.documents
hotel.web -> hotel.messages
hotel.web -> hotel.reports
hotel.web -> hotel.reservations
hotel.web -> hotel.rooms
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `hotel` | 5 | 0 |
| `hotel.documents` | 2 | 1 |
| `hotel.messages` | 0 | 2 |
| `hotel.reports` | 2 | 2 |
| `hotel.reservations` | 1 | 4 |
| `hotel.rooms` | 0 | 5 |
| `hotel.web` | 5 | 1 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `hotel` | sim | sim |  | sim | sim |  |
| `hotel.documents` |  |  |  |  |  |  |
| `hotel.messages` |  | sim |  |  |  |  |
| `hotel.reports` |  |  |  |  |  |  |
| `hotel.reservations` |  | sim |  |  |  |  |
| `hotel.rooms` |  | sim |  |  |  |  |
| `hotel.web` | sim | sim | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  333 src/main/java/hotel/web/HttpApi.java
  143 src/main/java/hotel/reservations/ReservationStore.java
  115 src/main/java/hotel/reservations/Reservation.java
   58 src/main/java/hotel/reports/RevenueReport.java
   57 src/main/java/hotel/reports/TourismTaxReport.java
   56 src/main/java/hotel/rooms/Rooms.java
   56 src/main/java/hotel/messages/SentMessages.java
   55 src/main/java/hotel/documents/ReservationReceipt.java
   54 src/main/java/hotel/reports/OccupancyReport.java
   53 src/main/java/hotel/reservations/PriceCalculator.java
```

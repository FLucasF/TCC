# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 17 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `hotel` | 2 | 93 |
| `hotel.documents` | 2 | 79 |
| `hotel.messages` | 3 | 75 |
| `hotel.reports` | 9 | 361 |
| `hotel.reservations` | 13 | 508 |
| `hotel.rooms` | 3 | 69 |
| `hotel.web` | 5 | 252 |

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
hotel.reservations -> hotel.documents
hotel.reservations -> hotel.messages
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
| `hotel.documents` | 2 | 2 |
| `hotel.messages` | 0 | 3 |
| `hotel.reports` | 2 | 2 |
| `hotel.reservations` | 3 | 4 |
| `hotel.rooms` | 0 | 5 |
| `hotel.web` | 5 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
hotel.documents
hotel.reservations
```

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
| `hotel.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  186 src/main/java/hotel/web/HttpApi.java
  143 src/main/java/hotel/reservations/ReservationStore.java
  138 src/main/java/hotel/reservations/ReservationService.java
  115 src/main/java/hotel/reservations/Reservation.java
   84 src/main/java/hotel/reports/RatePlansReport.java
   83 src/main/java/hotel/reports/TourismTaxReport.java
   78 src/main/java/hotel/reports/RevenueReport.java
   71 src/main/java/hotel/reports/OccupancyReport.java
   56 src/main/java/hotel/rooms/Rooms.java
   56 src/main/java/hotel/messages/SentMessages.java
```

# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 18 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta.reservas` | 1 | 50 |
| `br.com.marealta.reservas.api` | 7 | 224 |
| `br.com.marealta.reservas.domain` | 11 | 307 |
| `br.com.marealta.reservas.notification` | 8 | 142 |
| `br.com.marealta.reservas.persistence` | 5 | 409 |
| `br.com.marealta.reservas.report` | 1 | 60 |
| `br.com.marealta.reservas.service` | 2 | 131 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.reservas.api -> br.com.marealta.reservas.domain
br.com.marealta.reservas.api -> br.com.marealta.reservas.persistence
br.com.marealta.reservas.api -> br.com.marealta.reservas.report
br.com.marealta.reservas.api -> br.com.marealta.reservas.service
br.com.marealta.reservas -> br.com.marealta.reservas.api
br.com.marealta.reservas -> br.com.marealta.reservas.notification
br.com.marealta.reservas -> br.com.marealta.reservas.persistence
br.com.marealta.reservas -> br.com.marealta.reservas.report
br.com.marealta.reservas -> br.com.marealta.reservas.service
br.com.marealta.reservas.notification -> br.com.marealta.reservas.domain
br.com.marealta.reservas.notification -> br.com.marealta.reservas.persistence
br.com.marealta.reservas.persistence -> br.com.marealta.reservas.domain
br.com.marealta.reservas.persistence -> br.com.marealta.reservas.notification
br.com.marealta.reservas.report -> br.com.marealta.reservas.domain
br.com.marealta.reservas.report -> br.com.marealta.reservas.persistence
br.com.marealta.reservas.service -> br.com.marealta.reservas.domain
br.com.marealta.reservas.service -> br.com.marealta.reservas.notification
br.com.marealta.reservas.service -> br.com.marealta.reservas.persistence
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta.reservas` | 5 | 0 |
| `br.com.marealta.reservas.api` | 4 | 1 |
| `br.com.marealta.reservas.domain` | 0 | 5 |
| `br.com.marealta.reservas.notification` | 2 | 3 |
| `br.com.marealta.reservas.persistence` | 2 | 5 |
| `br.com.marealta.reservas.report` | 2 | 2 |
| `br.com.marealta.reservas.service` | 3 | 2 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.reservas.persistence
br.com.marealta.reservas.notification
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta.reservas` | sim |  |  |  |  |  |
| `br.com.marealta.reservas.api` | sim |  | sim |  |  |  |
| `br.com.marealta.reservas.domain` |  |  |  |  |  |  |
| `br.com.marealta.reservas.notification` |  |  |  |  |  |  |
| `br.com.marealta.reservas.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.reservas.report` |  |  |  |  |  |  |
| `br.com.marealta.reservas.service` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  148 src/main/java/br/com/marealta/reservas/persistence/ReservationRepository.java
  122 src/main/java/br/com/marealta/reservas/service/ReservationService.java
  115 src/main/java/br/com/marealta/reservas/domain/Reservation.java
  112 src/main/java/br/com/marealta/reservas/persistence/Database.java
   95 src/main/java/br/com/marealta/reservas/persistence/RoomRepository.java
   93 src/main/java/br/com/marealta/reservas/api/RequestParser.java
   87 src/main/java/br/com/marealta/reservas/api/Api.java
   60 src/main/java/br/com/marealta/reservas/report/Reports.java
   50 src/main/java/br/com/marealta/reservas/Main.java
   47 src/main/java/br/com/marealta/reservas/persistence/MessageRepository.java
```

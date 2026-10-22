# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 10 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 263 |
| `br.com.marealta.domain` | 8 | 442 |
| `br.com.marealta.domain.rateplan` | 7 | 231 |
| `br.com.marealta.repository` | 3 | 448 |
| `br.com.marealta.service` | 8 | 264 |
| `br.com.marealta.web.dto` | 6 | 150 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.repository
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web.dto
br.com.marealta.domain -> br.com.marealta.domain.rateplan
br.com.marealta.domain.rateplan -> br.com.marealta.domain
br.com.marealta.repository -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.repository
br.com.marealta.web.dto -> br.com.marealta.domain
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 0 |
| `br.com.marealta.domain` | 1 | 5 |
| `br.com.marealta.domain.rateplan` | 1 | 1 |
| `br.com.marealta.repository` | 1 | 2 |
| `br.com.marealta.service` | 2 | 1 |
| `br.com.marealta.web.dto` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.domain
br.com.marealta.domain.rateplan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim | sim |  | sim |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.domain.rateplan` |  |  |  |  |  |  |
| `br.com.marealta.repository` |  | sim |  |  |  |  |
| `br.com.marealta.service` |  |  |  |  |  |  |
| `br.com.marealta.web.dto` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  263 src/main/java/br/com/marealta/Application.java
  254 src/main/java/br/com/marealta/repository/ReservationRepository.java
  183 src/main/java/br/com/marealta/domain/Reservation.java
  107 src/main/java/br/com/marealta/repository/RoomRepository.java
  101 src/main/java/br/com/marealta/service/ReservationService.java
   87 src/main/java/br/com/marealta/repository/MessageRepository.java
   74 src/main/java/br/com/marealta/domain/DateRange.java
   73 src/main/java/br/com/marealta/service/ReportService.java
   65 src/main/java/br/com/marealta/domain/Money.java
   53 src/main/java/br/com/marealta/service/DocumentService.java
```

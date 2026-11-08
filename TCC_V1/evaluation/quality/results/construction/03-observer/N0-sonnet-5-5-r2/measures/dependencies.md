# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 51 |
| `br.com.marealta.domain` | 14 | 264 |
| `br.com.marealta.events` | 10 | 181 |
| `br.com.marealta.infra` | 5 | 427 |
| `br.com.marealta.service` | 7 | 277 |
| `br.com.marealta.web` | 1 | 149 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.events
br.com.marealta -> br.com.marealta.infra
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.events -> br.com.marealta.domain
br.com.marealta.events -> br.com.marealta.service
br.com.marealta.infra -> br.com.marealta.domain
br.com.marealta.infra -> br.com.marealta.service
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.events
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 0 |
| `br.com.marealta.domain` | 0 | 4 |
| `br.com.marealta.events` | 2 | 2 |
| `br.com.marealta.infra` | 2 | 1 |
| `br.com.marealta.service` | 2 | 4 |
| `br.com.marealta.web` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.service
br.com.marealta.events
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.events` |  |  |  |  |  |  |
| `br.com.marealta.infra` |  | sim |  |  | sim |  |
| `br.com.marealta.service` |  |  |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  169 src/main/java/br/com/marealta/infra/JdbcReservationRepository.java
  149 src/main/java/br/com/marealta/web/App.java
  142 src/main/java/br/com/marealta/infra/Database.java
  130 src/main/java/br/com/marealta/service/ReservationService.java
   68 src/main/java/br/com/marealta/service/ReportService.java
   67 src/main/java/br/com/marealta/domain/Reservation.java
   62 src/main/java/br/com/marealta/infra/JdbcRoomRepository.java
   56 src/main/java/br/com/marealta/domain/Receipt.java
   54 src/main/java/br/com/marealta/infra/JdbcMessageStore.java
   51 src/main/java/br/com/marealta/Main.java
```

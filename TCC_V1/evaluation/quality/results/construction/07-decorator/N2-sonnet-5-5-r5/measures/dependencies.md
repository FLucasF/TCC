# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 9 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 4 | 240 |
| `br.com.marealta.domain` | 12 | 280 |
| `br.com.marealta.messaging` | 12 | 200 |
| `br.com.marealta.persistence` | 5 | 350 |
| `br.com.marealta.web` | 1 | 134 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.domain
br.com.marealta -> br.com.marealta.messaging
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.web
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.messaging
br.com.marealta.web -> br.com.marealta
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.persistence
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 1 |
| `br.com.marealta.domain` | 0 | 3 |
| `br.com.marealta.messaging` | 0 | 2 |
| `br.com.marealta.persistence` | 2 | 2 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim | sim |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.messaging` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  143 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  134 src/main/java/br/com/marealta/web/Routes.java
  128 src/main/java/br/com/marealta/ReservationService.java
  108 src/main/java/br/com/marealta/persistence/Database.java
   59 src/main/java/br/com/marealta/Reports.java
   53 src/main/java/br/com/marealta/Main.java
   51 src/main/java/br/com/marealta/persistence/StoredMessages.java
   51 src/main/java/br/com/marealta/domain/Reservation.java
   48 src/main/java/br/com/marealta/persistence/RoomRepository.java
   46 src/main/java/br/com/marealta/domain/Receipt.java
```

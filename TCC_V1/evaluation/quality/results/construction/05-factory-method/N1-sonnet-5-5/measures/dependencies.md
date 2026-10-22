# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 41 |
| `br.com.marealta.domain` | 7 | 200 |
| `br.com.marealta.identity` | 9 | 217 |
| `br.com.marealta.persistence` | 7 | 370 |
| `br.com.marealta.service` | 5 | 215 |
| `br.com.marealta.web` | 3 | 264 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.domain -> br.com.marealta.identity
br.com.marealta.identity -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.identity
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.identity
br.com.marealta.service -> br.com.marealta.persistence
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.identity
br.com.marealta.web -> br.com.marealta.persistence
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 0 |
| `br.com.marealta.domain` | 1 | 4 |
| `br.com.marealta.identity` | 1 | 4 |
| `br.com.marealta.persistence` | 2 | 3 |
| `br.com.marealta.service` | 3 | 2 |
| `br.com.marealta.web` | 4 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.domain
br.com.marealta.identity
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.identity` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim | sim |  | sim |  |
| `br.com.marealta.service` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  159 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  135 src/main/java/br/com/marealta/service/ReservationService.java
  100 src/main/java/br/com/marealta/persistence/Database.java
   97 src/main/java/br/com/marealta/web/Routes.java
   94 src/main/java/br/com/marealta/domain/Reservation.java
   93 src/main/java/br/com/marealta/web/ReservationRequestParser.java
   74 src/main/java/br/com/marealta/web/Views.java
   73 src/main/java/br/com/marealta/persistence/RoomRepository.java
   65 src/main/java/br/com/marealta/service/ReportService.java
   41 src/main/java/br/com/marealta/identity/IdentityKinds.java
```

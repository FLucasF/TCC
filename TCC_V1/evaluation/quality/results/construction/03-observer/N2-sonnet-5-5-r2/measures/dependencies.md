# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 14 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 68 |
| `br.com.marealta.app` | 4 | 177 |
| `br.com.marealta.domain` | 13 | 374 |
| `br.com.marealta.notification` | 7 | 124 |
| `br.com.marealta.persistence` | 6 | 292 |
| `br.com.marealta.web` | 5 | 236 |

## As dependências entre os pacotes do projeto

```
br.com.marealta.app -> br.com.marealta.domain
br.com.marealta.app -> br.com.marealta.notification
br.com.marealta.app -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.app
br.com.marealta -> br.com.marealta.notification
br.com.marealta -> br.com.marealta.persistence
br.com.marealta -> br.com.marealta.web
br.com.marealta.notification -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.app
br.com.marealta.persistence -> br.com.marealta.domain
br.com.marealta.persistence -> br.com.marealta.notification
br.com.marealta.web -> br.com.marealta.app
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.persistence
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 0 |
| `br.com.marealta.app` | 3 | 3 |
| `br.com.marealta.domain` | 0 | 4 |
| `br.com.marealta.notification` | 1 | 3 |
| `br.com.marealta.persistence` | 3 | 3 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.app
br.com.marealta.persistence
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim | sim |  |  |  |  |
| `br.com.marealta.app` |  |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.notification` |  |  |  |  |  |  |
| `br.com.marealta.persistence` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  153 src/main/java/br/com/marealta/domain/Reservation.java
  132 src/main/java/br/com/marealta/persistence/Database.java
  128 src/main/java/br/com/marealta/persistence/ReservationRepository.java
  117 src/main/java/br/com/marealta/web/Api.java
   90 src/main/java/br/com/marealta/app/ReservationService.java
   86 src/main/java/br/com/marealta/web/ReservationRequestParser.java
   55 src/main/java/br/com/marealta/app/ReportService.java
   43 src/main/java/br/com/marealta/domain/Pricing.java
   41 src/main/java/br/com/marealta/Application.java
   38 src/main/java/br/com/marealta/domain/Receipt.java
```

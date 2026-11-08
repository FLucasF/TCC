# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 16 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 2 | 73 |
| `br.com.marealta.domain` | 10 | 221 |
| `br.com.marealta.infra` | 4 | 245 |
| `br.com.marealta.plan` | 9 | 172 |
| `br.com.marealta.service` | 4 | 196 |
| `br.com.marealta.web` | 2 | 129 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.infra
br.com.marealta -> br.com.marealta.plan
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.domain -> br.com.marealta
br.com.marealta.domain -> br.com.marealta.plan
br.com.marealta.infra -> br.com.marealta.domain
br.com.marealta.plan -> br.com.marealta
br.com.marealta.plan -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.infra
br.com.marealta.service -> br.com.marealta.plan
br.com.marealta.web -> br.com.marealta
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 4 | 4 |
| `br.com.marealta.domain` | 2 | 4 |
| `br.com.marealta.infra` | 1 | 2 |
| `br.com.marealta.plan` | 2 | 3 |
| `br.com.marealta.service` | 4 | 2 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.service
br.com.marealta.infra
br.com.marealta.domain
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.service
br.com.marealta.plan
--- um ciclo:
br.com.marealta
br.com.marealta.web
br.com.marealta.service
--- um ciclo:
br.com.marealta
br.com.marealta.web
--- um ciclo:
br.com.marealta.domain
br.com.marealta.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  | sim |  |  |  |
| `br.com.marealta.domain` |  |  | sim |  |  |  |
| `br.com.marealta.infra` |  | sim |  |  |  |  |
| `br.com.marealta.plan` |  |  |  |  |  |  |
| `br.com.marealta.service` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  129 src/main/java/br/com/marealta/infra/ReservationRepository.java
  108 src/main/java/br/com/marealta/service/ReservationService.java
   81 src/main/java/br/com/marealta/infra/Database.java
   70 src/main/java/br/com/marealta/web/RequestParser.java
   64 src/main/java/br/com/marealta/domain/Reservation.java
   62 src/main/java/br/com/marealta/service/ReportService.java
   59 src/main/java/br/com/marealta/web/Routes.java
   50 src/main/java/br/com/marealta/App.java
   45 src/main/java/br/com/marealta/domain/Receipt.java
   35 src/main/java/br/com/marealta/infra/MessageRepository.java
```

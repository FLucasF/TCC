# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 6 pacotes, 12 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.marealta` | 1 | 30 |
| `br.com.marealta.domain` | 10 | 283 |
| `br.com.marealta.infra` | 4 | 273 |
| `br.com.marealta.plan` | 8 | 216 |
| `br.com.marealta.service` | 6 | 261 |
| `br.com.marealta.web` | 3 | 254 |

## As dependências entre os pacotes do projeto

```
br.com.marealta -> br.com.marealta.infra
br.com.marealta -> br.com.marealta.service
br.com.marealta -> br.com.marealta.web
br.com.marealta.domain -> br.com.marealta.plan
br.com.marealta.infra -> br.com.marealta.domain
br.com.marealta.plan -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.domain
br.com.marealta.service -> br.com.marealta.infra
br.com.marealta.service -> br.com.marealta.plan
br.com.marealta.web -> br.com.marealta.domain
br.com.marealta.web -> br.com.marealta.plan
br.com.marealta.web -> br.com.marealta.service
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.marealta` | 3 | 0 |
| `br.com.marealta.domain` | 1 | 4 |
| `br.com.marealta.infra` | 1 | 2 |
| `br.com.marealta.plan` | 1 | 3 |
| `br.com.marealta.service` | 3 | 2 |
| `br.com.marealta.web` | 3 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.marealta.domain
br.com.marealta.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.marealta` | sim |  |  |  |  |  |
| `br.com.marealta.domain` |  |  |  |  |  |  |
| `br.com.marealta.infra` |  | sim |  |  | sim |  |
| `br.com.marealta.plan` |  |  |  |  |  |  |
| `br.com.marealta.service` |  | sim |  |  |  |  |
| `br.com.marealta.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  150 src/main/java/br/com/marealta/infra/ReservationRepository.java
  122 src/main/java/br/com/marealta/service/ReservationService.java
  102 src/main/java/br/com/marealta/web/RequestParser.java
   87 src/main/java/br/com/marealta/domain/Reservation.java
   86 src/main/java/br/com/marealta/infra/Db.java
   80 src/main/java/br/com/marealta/web/Api.java
   72 src/main/java/br/com/marealta/web/Views.java
   54 src/main/java/br/com/marealta/service/ReportService.java
   48 src/main/java/br/com/marealta/service/Receipt.java
   45 src/main/java/br/com/marealta/domain/ApiException.java
```

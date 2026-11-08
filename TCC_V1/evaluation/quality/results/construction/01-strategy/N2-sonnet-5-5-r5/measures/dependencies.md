# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 5 pacotes, 10 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.mare` | 1 | 22 |
| `br.com.mare.domain` | 14 | 416 |
| `br.com.mare.infra` | 4 | 250 |
| `br.com.mare.plan` | 10 | 192 |
| `br.com.mare.web` | 3 | 210 |

## As dependências entre os pacotes do projeto

```
br.com.mare -> br.com.mare.domain
br.com.mare -> br.com.mare.infra
br.com.mare -> br.com.mare.web
br.com.mare.domain -> br.com.mare.infra
br.com.mare.domain -> br.com.mare.plan
br.com.mare.infra -> br.com.mare.domain
br.com.mare.infra -> br.com.mare.plan
br.com.mare.plan -> br.com.mare.domain
br.com.mare.web -> br.com.mare.domain
br.com.mare.web -> br.com.mare.plan
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.mare` | 3 | 0 |
| `br.com.mare.domain` | 2 | 4 |
| `br.com.mare.infra` | 2 | 2 |
| `br.com.mare.plan` | 1 | 3 |
| `br.com.mare.web` | 2 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
br.com.mare.domain
br.com.mare.infra
--- um ciclo:
br.com.mare.domain
br.com.mare.plan
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.mare` | sim |  |  |  |  |  |
| `br.com.mare.domain` |  | sim |  |  |  |  |
| `br.com.mare.infra` |  | sim |  |  |  |  |
| `br.com.mare.plan` |  |  |  |  |  |  |
| `br.com.mare.web` | sim |  | sim |  |  |  |

## As maiores classes (linhas, src/main)

```
  128 src/main/java/br/com/mare/infra/ReservationRepository.java
  105 src/main/java/br/com/mare/domain/ReservationService.java
  103 src/main/java/br/com/mare/domain/Reservation.java
   86 src/main/java/br/com/mare/infra/Db.java
   74 src/main/java/br/com/mare/web/RequestParser.java
   71 src/main/java/br/com/mare/web/Views.java
   65 src/main/java/br/com/mare/web/Routes.java
   44 src/main/java/br/com/mare/domain/ReportService.java
   39 src/main/java/br/com/mare/domain/Receipt.java
   36 src/main/java/br/com/mare/plan/WeekendPackagePlan.java
```

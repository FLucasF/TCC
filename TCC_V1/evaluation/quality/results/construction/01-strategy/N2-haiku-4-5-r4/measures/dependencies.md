# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 7 pacotes, 14 dependências entre eles, sem ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `br.com.mareaalta` | 1 | 47 |
| `br.com.mareaalta.api` | 3 | 414 |
| `br.com.mareaalta.domain` | 6 | 313 |
| `br.com.mareaalta.domain.rateplan` | 10 | 342 |
| `br.com.mareaalta.repository` | 1 | 358 |
| `br.com.mareaalta.service` | 2 | 314 |
| `br.com.mareaalta.util` | 2 | 104 |

## As dependências entre os pacotes do projeto

```
br.com.mareaalta.api -> br.com.mareaalta.domain
br.com.mareaalta.api -> br.com.mareaalta.service
br.com.mareaalta.api -> br.com.mareaalta.util
br.com.mareaalta -> br.com.mareaalta.api
br.com.mareaalta -> br.com.mareaalta.repository
br.com.mareaalta -> br.com.mareaalta.service
br.com.mareaalta.domain.rateplan -> br.com.mareaalta.domain
br.com.mareaalta.domain.rateplan -> br.com.mareaalta.util
br.com.mareaalta.repository -> br.com.mareaalta.domain
br.com.mareaalta.repository -> br.com.mareaalta.util
br.com.mareaalta.service -> br.com.mareaalta.domain
br.com.mareaalta.service -> br.com.mareaalta.domain.rateplan
br.com.mareaalta.service -> br.com.mareaalta.repository
br.com.mareaalta.service -> br.com.mareaalta.util
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `br.com.mareaalta` | 3 | 0 |
| `br.com.mareaalta.api` | 3 | 1 |
| `br.com.mareaalta.domain` | 0 | 4 |
| `br.com.mareaalta.domain.rateplan` | 2 | 1 |
| `br.com.mareaalta.repository` | 2 | 2 |
| `br.com.mareaalta.service` | 4 | 2 |
| `br.com.mareaalta.util` | 0 | 4 |

## Os ciclos

Nenhum ciclo entre pacotes.

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `br.com.mareaalta` | sim |  |  |  |  |  |
| `br.com.mareaalta.api` | sim |  |  |  |  | com.google |
| `br.com.mareaalta.domain` |  |  |  |  |  |  |
| `br.com.mareaalta.domain.rateplan` |  |  |  |  |  |  |
| `br.com.mareaalta.repository` |  | sim |  |  |  |  |
| `br.com.mareaalta.service` |  | sim |  |  |  |  |
| `br.com.mareaalta.util` |  |  |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  414 src/main/java/br/com/mareaalta/api/ReservationAPI.java
  358 src/main/java/br/com/mareaalta/repository/Database.java
  314 src/main/java/br/com/mareaalta/service/ReservationService.java
  161 src/main/java/br/com/mareaalta/domain/Reservation.java
   93 src/main/java/br/com/mareaalta/domain/rateplan/CorporateRatePlan.java
   72 src/main/java/br/com/mareaalta/util/DateUtils.java
   58 src/main/java/br/com/mareaalta/domain/rateplan/LongStayRatePlan.java
   58 src/main/java/br/com/mareaalta/domain/Message.java
   54 src/main/java/br/com/mareaalta/domain/rateplan/StandardRatePlan.java
   54 src/main/java/br/com/mareaalta/domain/rateplan/NonRefundableRatePlan.java
```

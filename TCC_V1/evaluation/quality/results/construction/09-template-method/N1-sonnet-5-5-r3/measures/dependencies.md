# Arquitetura: os pacotes e as dependências (jdeps)

Resultado: medido. 2 pacotes, 2 dependências entre eles, com ciclo.

## Os pacotes

| pacote | classes | linhas de Java (src/main) |
|---|---|---|
| `pousada` | 23 | 929 |
| `pousada.report` | 11 | 336 |

## As dependências entre os pacotes do projeto

```
pousada -> pousada.report
pousada.report -> pousada
```

| pacote | depende de (quantos) | é usado por (quantos) |
|---|---|---|
| `pousada` | 1 | 1 |
| `pousada.report` | 1 | 1 |

## Os ciclos

O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):
```
--- um ciclo:
pousada
pousada.report
```

## As bibliotecas que cada pacote usa

Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway).

| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |
|---|---|---|---|---|---|---|
| `pousada` | sim | sim | sim |  |  |  |
| `pousada.report` |  | sim |  |  |  |  |

## As maiores classes (linhas, src/main)

```
  135 src/main/java/pousada/ReservationRepository.java
  103 src/main/java/pousada/Schema.java
  103 src/main/java/pousada/ReservationService.java
   70 src/main/java/pousada/Api.java
   69 src/main/java/pousada/Reservation.java
   62 src/main/java/pousada/ReservationRequest.java
   48 src/main/java/pousada/RoomRepository.java
   47 src/main/java/pousada/report/ReportSource.java
   44 src/main/java/pousada/ApiException.java
   43 src/main/java/pousada/report/Report.java
```

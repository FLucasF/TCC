# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 1 erros, 0 pulados); 19 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 145 de 832 (17.4%)
- ramos (if/switch): 35 de 190 (18.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 53 | 0 de 2 |
| `com.pousadamarealta.config` | 0 de 25 | 0 de 6 |
| `com.pousadamarealta.controller` | 0 de 155 | 0 de 38 |
| `com.pousadamarealta.domain.enums` | 23 de 25 | 0 de 0 |
| `com.pousadamarealta.domain.events` | 2 de 4 | 0 de 0 |
| `com.pousadamarealta.domain.model` | 54 de 87 | 11 de 24 |
| `com.pousadamarealta.dto` | 0 de 47 | 0 de 0 |
| `com.pousadamarealta.repository` | 2 de 178 | 0 de 46 |
| `com.pousadamarealta.service` | 56 de 148 | 22 de 46 |
| `com.pousadamarealta.service.event` | 8 de 8 | 2 de 2 |
| `com.pousadamarealta.service.event.listeners` | 0 de 102 | 0 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.ReservationServiceTest` | 9 | 0 | 0 |
| `com.pousadamarealta.ReservationSystemIT` | 1 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   ReservationSystemIT » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 1

Os arquivos:
```
10 testes  src/test/java/com/pousadamarealta/ReservationSystemIT.java
9 testes  src/test/java/com/pousadamarealta/ReservationServiceTest.java
```

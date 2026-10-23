# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 1 erros, 0 pulados); 25 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 111 de 972 (11.4%)
- ramos (if/switch): 34 de 146 (23.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 65 | 0 de 0 |
| `com.pousadamarealta.controller` | 0 de 112 | 0 de 40 |
| `com.pousadamarealta.database` | 0 de 9 | 0 de 0 |
| `com.pousadamarealta.dto` | 0 de 160 | 0 de 0 |
| `com.pousadamarealta.exception` | 0 de 12 | 0 de 0 |
| `com.pousadamarealta.mapper` | 0 de 47 | 0 de 2 |
| `com.pousadamarealta.messaging` | 42 de 46 | 4 de 4 |
| `com.pousadamarealta.messaging.rules` | 30 de 31 | 14 de 16 |
| `com.pousadamarealta.model` | 9 de 112 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 166 | 0 de 24 |
| `com.pousadamarealta.service` | 30 de 212 | 16 de 60 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.MessageRulesTest` | 6 | 0 | 0 |
| `com.pousadamarealta.PricingServiceTest` | 9 | 0 | 0 |
| `com.pousadamarealta.ReservationIT` | 1 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   ReservationIT » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/com/pousadamarealta/ReservationIT.java
9 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
6 testes  src/test/java/com/pousadamarealta/MessageRulesTest.java
```

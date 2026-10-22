# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 3 testes rodaram (0 falhas, 3 erros, 0 pulados); 18 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 0 de 903 (0.0%)
- ramos (if/switch): 0 de 170 (0.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 33 | 0 de 8 |
| `com.pousadamarealta.config` | 0 de 36 | 0 de 4 |
| `com.pousadamarealta.controller` | 0 de 132 | 0 de 40 |
| `com.pousadamarealta.controller.dto` | 0 de 105 | 0 de 2 |
| `com.pousadamarealta.messaging` | 0 de 61 | 0 de 6 |
| `com.pousadamarealta.messaging.rules` | 0 de 31 | 0 de 22 |
| `com.pousadamarealta.model` | 0 de 115 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 182 | 0 de 34 |
| `com.pousadamarealta.service` | 0 de 137 | 0 de 32 |
| `com.pousadamarealta.util` | 0 de 71 | 0 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.ReservationSystemIT` | 1 | 1 | 0 |
| `com.pousadamarealta.TestModeIT` | 1 | 1 | 0 |
| `com.pousadamarealta.TestModeIT` | 1 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   TestModeIT » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
[ERROR]   ReservationSystemIT » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
[ERROR]   TestModeIT » IllegalState Previous attempts to find a Docker environment failed. Will not retry. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 2
- contra o banco (DataSource, JDBC, Testcontainers): 2
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
15 testes  src/test/java/com/pousadamarealta/ReservationSystemIT.java
3 testes  src/test/java/com/pousadamarealta/TestModeIT.java
```

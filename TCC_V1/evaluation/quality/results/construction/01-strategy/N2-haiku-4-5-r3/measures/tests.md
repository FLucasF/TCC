# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 98 de 643 (15.2%)
- ramos (if/switch): 23 de 127 (18.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 17 | 0 de 0 |
| `com.pousada.api` | 0 de 211 | 0 de 30 |
| `com.pousada.database` | 0 de 176 | 0 de 24 |
| `com.pousada.model` | 0 de 36 | 0 de 0 |
| `com.pousada.rateplan` | 88 de 106 | 18 de 33 |
| `com.pousada.service` | 0 de 76 | 0 de 30 |
| `com.pousada.util` | 10 de 21 | 5 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.MoneyTest` | 1 | 0 | 0 |
| `com.pousada.RatePlanCalculationTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousada/RatePlanCalculationTest.java
1 testes  src/test/java/com/pousada/MoneyTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 112 de 802 (14.0%)
- ramos (if/switch): 24 de 140 (17.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 55 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 159 | 0 de 24 |
| `com.pousadamarealta.domain` | 15 de 112 | 0 de 0 |
| `com.pousadamarealta.exception` | 6 de 18 | 0 de 0 |
| `com.pousadamarealta.rateplan` | 84 de 116 | 19 de 32 |
| `com.pousadamarealta.repository` | 0 de 168 | 0 de 28 |
| `com.pousadamarealta.service` | 0 de 146 | 0 de 38 |
| `com.pousadamarealta.util` | 7 de 28 | 5 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.rateplan.RatePlanTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousadamarealta/rateplan/RatePlanTest.java
```

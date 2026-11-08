# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 117 de 724 (16.2%)
- ramos (if/switch): 23 de 117 (19.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 50 | 0 de 0 |
| `com.pousadamarealta.controller` | 0 de 143 | 0 de 24 |
| `com.pousadamarealta.dto` | 0 de 6 | 0 de 0 |
| `com.pousadamarealta.exception` | 4 de 6 | 0 de 0 |
| `com.pousadamarealta.model` | 36 de 108 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 166 | 0 de 26 |
| `com.pousadamarealta.service` | 17 de 171 | 12 de 46 |
| `com.pousadamarealta.service.rateplan` | 60 de 74 | 11 de 21 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.service.PricingServiceTest` | 7 | 0 | 0 |
| `com.pousadamarealta.service.rateplan.RatePlanTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousadamarealta/service/rateplan/RatePlanTest.java
7 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
```

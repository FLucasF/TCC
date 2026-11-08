# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 101 de 811 (12.5%)
- ramos (if/switch): 33 de 191 (17.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 11 | 0 de 6 |
| `com.pousadamarealta.api` | 0 de 144 | 0 de 10 |
| `com.pousadamarealta.api.request` | 3 de 10 | 0 de 0 |
| `com.pousadamarealta.api.response` | 0 de 88 | 0 de 0 |
| `com.pousadamarealta.database` | 0 de 178 | 0 de 44 |
| `com.pousadamarealta.domain` | 3 de 49 | 0 de 0 |
| `com.pousadamarealta.pricing` | 95 de 125 | 33 de 63 |
| `com.pousadamarealta.service` | 0 de 206 | 0 de 68 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.api.RoutesTest` | 3 | 0 | 0 |
| `com.pousadamarealta.pricing.PricingTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/pricing/PricingTest.java
3 testes  src/test/java/com/pousadamarealta/api/RoutesTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 461 de 756 (61.0%)
- ramos (if/switch): 65 de 166 (39.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 27 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 163 | 0 de 36 |
| `com.pousadamarealta.domain` | 110 de 128 | 1 de 18 |
| `com.pousadamarealta.repository` | 175 de 200 | 23 de 36 |
| `com.pousadamarealta.service` | 176 de 238 | 41 de 68 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.ApiIT` | 6 | 0 | 0 |
| `com.pousadamarealta.PricingServiceTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
6 testes  src/test/java/com/pousadamarealta/ApiIT.java
```

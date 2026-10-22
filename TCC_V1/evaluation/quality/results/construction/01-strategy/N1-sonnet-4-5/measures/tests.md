# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 15 testes rodaram (0 falhas, 0 erros, 0 pulados); 15 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 378 de 792 (47.7%)
- ramos (if/switch): 62 de 168 (36.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 173 | 0 de 20 |
| `br.com.marealta.domain` | 137 de 171 | 27 de 70 |
| `br.com.marealta.domain.rateplan` | 39 de 56 | 13 de 16 |
| `br.com.marealta.repository` | 143 de 223 | 17 de 46 |
| `br.com.marealta.service` | 59 de 117 | 5 de 14 |
| `br.com.marealta.web.dto` | 0 de 52 | 0 de 2 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.domain.MoneyTest` | 2 | 0 | 0 |
| `br.com.marealta.domain.RatePlanTest` | 7 | 0 | 0 |
| `br.com.marealta.IntegrationTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/domain/RatePlanTest.java
6 testes  src/test/java/br/com/marealta/IntegrationTest.java
2 testes  src/test/java/br/com/marealta/domain/MoneyTest.java
```

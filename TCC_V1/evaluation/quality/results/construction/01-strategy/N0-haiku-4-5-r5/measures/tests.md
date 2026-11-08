# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 4 testes rodaram (0 falhas, 0 erros, 0 pulados); 4 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 40 de 717 (5.6%)
- ramos (if/switch): 20 de 192 (10.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta` | 0 de 7 | 0 de 0 |
| `com.marealta.api` | 0 de 20 | 0 de 0 |
| `com.marealta.api.handlers` | 0 de 246 | 0 de 44 |
| `com.marealta.database` | 0 de 44 | 0 de 2 |
| `com.marealta.domain` | 0 de 31 | 0 de 0 |
| `com.marealta.service` | 40 de 369 | 20 de 146 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.service.PricingServiceTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/com/marealta/service/PricingServiceTest.java
```

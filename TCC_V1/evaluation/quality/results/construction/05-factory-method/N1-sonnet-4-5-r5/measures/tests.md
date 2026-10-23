# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 4 testes rodaram (0 falhas, 0 erros, 0 pulados); 4 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 52 de 764 (6.8%)
- ramos (if/switch): 14 de 225 (6.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta` | 0 de 31 | 0 de 8 |
| `com.marealta.api` | 0 de 254 | 0 de 90 |
| `com.marealta.api.dto` | 0 de 2 | 0 de 0 |
| `com.marealta.domain.model` | 14 de 99 | 0 de 12 |
| `com.marealta.domain.service` | 38 de 182 | 14 de 61 |
| `com.marealta.infrastructure.database` | 0 de 170 | 0 de 50 |
| `com.marealta.infrastructure.messaging` | 0 de 26 | 0 de 4 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.domain.service.PricingServiceTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/com/marealta/domain/service/PricingServiceTest.java
```

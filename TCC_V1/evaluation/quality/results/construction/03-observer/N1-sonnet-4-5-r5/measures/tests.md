# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 47 de 794 (5.9%)
- ramos (if/switch): 16 de 188 (8.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta` | 0 de 61 | 0 de 10 |
| `com.marealta.api.controller` | 0 de 111 | 0 de 38 |
| `com.marealta.api.dto` | 0 de 44 | 0 de 0 |
| `com.marealta.api.error` | 0 de 28 | 0 de 14 |
| `com.marealta.application.handler` | 0 de 97 | 0 de 18 |
| `com.marealta.domain.event` | 0 de 2 | 0 de 0 |
| `com.marealta.domain.model` | 17 de 72 | 0 de 16 |
| `com.marealta.domain.service` | 30 de 138 | 16 de 40 |
| `com.marealta.infrastructure.database` | 0 de 214 | 0 de 48 |
| `com.marealta.infrastructure.messaging` | 0 de 17 | 0 de 0 |
| `com.marealta.infrastructure.notification` | 0 de 10 | 0 de 4 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.domain.service.PricingServiceTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/marealta/domain/service/PricingServiceTest.java
```

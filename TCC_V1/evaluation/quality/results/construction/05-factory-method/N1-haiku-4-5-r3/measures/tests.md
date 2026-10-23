# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 112 de 750 (14.9%)
- ramos (if/switch): 26 de 204 (12.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.mareaalta` | 0 de 26 | 0 de 8 |
| `com.mareaalta.api` | 0 de 217 | 0 de 70 |
| `com.mareaalta.domain` | 38 de 105 | 0 de 0 |
| `com.mareaalta.infrastructure` | 0 de 188 | 0 de 40 |
| `com.mareaalta.service` | 74 de 214 | 26 de 86 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.mareaalta.service.DocumentGeneratorTest` | 3 | 0 | 0 |
| `com.mareaalta.service.PricingServiceTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/mareaalta/service/PricingServiceTest.java
3 testes  src/test/java/com/mareaalta/service/DocumentGeneratorTest.java
```

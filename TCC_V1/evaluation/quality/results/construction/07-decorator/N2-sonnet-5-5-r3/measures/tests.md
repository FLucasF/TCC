# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 89 de 490 (18.2%)
- ramos (if/switch): 38 de 118 (32.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta` | 0 de 18 | 0 de 10 |
| `com.marealta.domain` | 28 de 124 | 11 de 42 |
| `com.marealta.mail` | 61 de 62 | 27 de 28 |
| `com.marealta.persistence` | 0 de 124 | 0 de 12 |
| `com.marealta.report` | 0 de 29 | 0 de 4 |
| `com.marealta.service` | 0 de 46 | 0 de 6 |
| `com.marealta.web` | 0 de 87 | 0 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.MailPipelineTest` | 5 | 0 | 0 |
| `com.marealta.PricingTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/marealta/MailPipelineTest.java
3 testes  src/test/java/com/marealta/PricingTest.java
```

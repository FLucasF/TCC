# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 31 de 849 (3.7%)
- ramos (if/switch): 14 de 134 (10.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 24 | 0 de 0 |
| `br.com.marealta.api` | 0 de 202 | 0 de 20 |
| `br.com.marealta.domain` | 3 de 109 | 0 de 10 |
| `br.com.marealta.report` | 0 de 137 | 0 de 26 |
| `br.com.marealta.repository` | 0 de 227 | 0 de 34 |
| `br.com.marealta.service` | 28 de 150 | 14 de 44 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.PricingServiceTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/br/com/marealta/PricingServiceTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 498 de 554 (89.9%)
- ramos (if/switch): 76 de 98 (77.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.reservas` | 9 de 14 | 0 de 4 |
| `br.com.marealta.reservas.api` | 86 de 101 | 10 de 20 |
| `br.com.marealta.reservas.domain` | 123 de 123 | 30 de 34 |
| `br.com.marealta.reservas.notification` | 52 de 53 | 5 de 6 |
| `br.com.marealta.reservas.persistence` | 145 de 180 | 19 de 22 |
| `br.com.marealta.reservas.report` | 29 de 29 | 4 de 4 |
| `br.com.marealta.reservas.service` | 54 de 54 | 8 de 8 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.reservas.ApiIntegrationTest` | 4 | 0 | 0 |
| `br.com.marealta.reservas.PricingTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/reservas/PricingTest.java
4 testes  src/test/java/br/com/marealta/reservas/ApiIntegrationTest.java
```

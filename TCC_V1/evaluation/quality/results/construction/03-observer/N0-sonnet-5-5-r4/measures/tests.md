# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 413 de 461 (89.6%)
- ramos (if/switch): 92 de 115 (80.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 3 de 10 | 0 de 8 |
| `br.com.marealta.pousada.domain` | 81 de 81 | 31 de 34 |
| `br.com.marealta.pousada.events` | 37 de 37 | 14 de 14 |
| `br.com.marealta.pousada.persistence` | 148 de 174 | 14 de 16 |
| `br.com.marealta.pousada.service` | 78 de 78 | 19 de 19 |
| `br.com.marealta.pousada.web` | 66 de 81 | 14 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.ApiIntegrationTest` | 4 | 0 | 0 |
| `br.com.marealta.pousada.PricingTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/marealta/pousada/PricingTest.java
4 testes  src/test/java/br/com/marealta/pousada/ApiIntegrationTest.java
```

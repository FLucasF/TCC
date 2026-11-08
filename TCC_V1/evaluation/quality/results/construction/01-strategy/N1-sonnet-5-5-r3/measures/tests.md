# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 97 de 483 (20.1%)
- ramos (if/switch): 23 de 90 (25.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 9 | 0 de 4 |
| `br.com.marealta.domain` | 60 de 123 | 13 de 28 |
| `br.com.marealta.http` | 0 de 120 | 0 de 20 |
| `br.com.marealta.persistence` | 0 de 103 | 0 de 8 |
| `br.com.marealta.plan` | 37 de 38 | 10 de 10 |
| `br.com.marealta.service` | 0 de 90 | 0 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.RatePlanTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/RatePlanTest.java
```

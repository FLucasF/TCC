# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 89 de 430 (20.7%)
- ramos (if/switch): 37 de 132 (28.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 6 de 20 | 0 de 8 |
| `br.com.marealta.db` | 0 de 100 | 0 de 14 |
| `br.com.marealta.model` | 1 de 6 | 0 de 6 |
| `br.com.marealta.plan` | 68 de 73 | 34 de 38 |
| `br.com.marealta.service` | 14 de 106 | 3 de 28 |
| `br.com.marealta.web` | 0 de 125 | 0 de 38 |

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

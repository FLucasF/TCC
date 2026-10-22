# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 497 de 522 (95.2%)
- ramos (if/switch): 74 de 94 (78.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 7 de 12 | 0 de 4 |
| `br.com.marealta.domain` | 86 de 86 | 23 de 28 |
| `br.com.marealta.identity` | 59 de 59 | 4 de 4 |
| `br.com.marealta.persistence` | 138 de 144 | 17 de 18 |
| `br.com.marealta.service` | 87 de 89 | 11 de 12 |
| `br.com.marealta.web` | 120 de 132 | 19 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 3 | 0 | 0 |
| `br.com.marealta.QuoteAndDocumentTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/QuoteAndDocumentTest.java
3 testes  src/test/java/br/com/marealta/ApiTest.java
```

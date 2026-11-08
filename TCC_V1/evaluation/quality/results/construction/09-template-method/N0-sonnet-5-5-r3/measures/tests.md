# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 434 de 470 (92.3%)
- ramos (if/switch): 87 de 122 (71.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 13 de 25 | 0 de 8 |
| `br.com.marealta.db` | 126 de 136 | 13 de 18 |
| `br.com.marealta.domain` | 61 de 61 | 20 de 22 |
| `br.com.marealta.report` | 107 de 110 | 22 de 28 |
| `br.com.marealta.service` | 68 de 69 | 15 de 16 |
| `br.com.marealta.web` | 59 de 69 | 17 de 30 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 2 | 0 | 0 |
| `br.com.marealta.DomainTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/DomainTest.java
2 testes  src/test/java/br/com/marealta/ApiTest.java
```

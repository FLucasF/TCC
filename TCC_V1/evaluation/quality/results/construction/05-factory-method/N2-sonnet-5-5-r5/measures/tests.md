# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 532 de 560 (95.0%)
- ramos (if/switch): 79 de 106 (74.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 4 de 8 | 0 de 4 |
| `br.com.marealta.domain` | 296 de 297 | 53 de 66 |
| `br.com.marealta.persistence` | 136 de 149 | 8 de 10 |
| `br.com.marealta.web` | 96 de 106 | 18 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 5 | 0 | 0 |
| `br.com.marealta.DomainTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/marealta/DomainTest.java
5 testes  src/test/java/br/com/marealta/ApiTest.java
```

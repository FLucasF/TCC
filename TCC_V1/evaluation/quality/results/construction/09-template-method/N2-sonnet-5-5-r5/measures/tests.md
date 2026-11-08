# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 446 de 498 (89.6%)
- ramos (if/switch): 57 de 90 (63.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.mare` | 5 de 17 | 0 de 12 |
| `br.com.mare.domain` | 186 de 194 | 26 de 34 |
| `br.com.mare.persistence` | 103 de 123 | 9 de 12 |
| `br.com.mare.report` | 88 de 90 | 9 de 10 |
| `br.com.mare.web` | 64 de 74 | 13 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.mare.ApiTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/mare/ApiTest.java
```

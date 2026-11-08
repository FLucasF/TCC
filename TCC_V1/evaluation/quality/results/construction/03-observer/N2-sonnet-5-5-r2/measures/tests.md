# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 18 testes rodaram (0 falhas, 0 erros, 0 pulados); 18 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 475 de 503 (94.4%)
- ramos (if/switch): 77 de 98 (78.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 9 de 20 | 0 de 8 |
| `br.com.marealta.app` | 70 de 70 | 10 de 10 |
| `br.com.marealta.domain` | 142 de 143 | 37 de 40 |
| `br.com.marealta.notification` | 43 de 44 | 6 de 6 |
| `br.com.marealta.persistence` | 118 de 123 | 7 de 8 |
| `br.com.marealta.web` | 93 de 103 | 17 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 4 | 0 | 0 |
| `br.com.marealta.DomainTest` | 10 | 0 | 0 |
| `br.com.marealta.NotificationTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/br/com/marealta/DomainTest.java
4 testes  src/test/java/br/com/marealta/NotificationTest.java
4 testes  src/test/java/br/com/marealta/ApiTest.java
```

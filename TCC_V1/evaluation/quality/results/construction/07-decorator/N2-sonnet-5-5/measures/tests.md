# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 489 de 537 (91.1%)
- ramos (if/switch): 89 de 111 (80.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 6 de 17 | 0 de 6 |
| `br.com.marealta.domain` | 139 de 140 | 39 de 42 |
| `br.com.marealta.messaging` | 59 de 59 | 22 de 23 |
| `br.com.marealta.persistence` | 105 de 125 | 10 de 14 |
| `br.com.marealta.report` | 30 de 30 | 6 de 6 |
| `br.com.marealta.service` | 37 de 37 | 4 de 4 |
| `br.com.marealta.web` | 113 de 129 | 8 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 2 | 0 | 0 |
| `br.com.marealta.DomainTest` | 9 | 0 | 0 |
| `br.com.marealta.MessagingTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/br/com/marealta/DomainTest.java
6 testes  src/test/java/br/com/marealta/MessagingTest.java
2 testes  src/test/java/br/com/marealta/ApiTest.java
```

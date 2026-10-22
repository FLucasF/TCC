# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 173 de 530 (32.6%)
- ramos (if/switch): 61 de 120 (50.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 76 | 0 de 14 |
| `br.com.marealta.domain` | 113 de 120 | 36 de 39 |
| `br.com.marealta.mail` | 60 de 72 | 25 de 29 |
| `br.com.marealta.persistence` | 0 de 128 | 0 de 14 |
| `br.com.marealta.report` | 0 de 41 | 0 de 4 |
| `br.com.marealta.web` | 0 de 93 | 0 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.DomainTest` | 8 | 0 | 0 |
| `br.com.marealta.MailTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/marealta/DomainTest.java
6 testes  src/test/java/br/com/marealta/MailTest.java
```

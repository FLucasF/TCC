# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 397 de 435 (91.3%)
- ramos (if/switch): 82 de 105 (78.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 6 de 18 | 0 de 8 |
| `br.com.marealta.pousada.domain` | 85 de 86 | 42 de 47 |
| `br.com.marealta.pousada.notification` | 33 de 34 | 2 de 2 |
| `br.com.marealta.pousada.persistence` | 115 de 123 | 9 de 10 |
| `br.com.marealta.pousada.service` | 97 de 101 | 18 de 18 |
| `br.com.marealta.pousada.web` | 61 de 73 | 11 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.ApiIntegrationTest` | 4 | 0 | 0 |
| `br.com.marealta.pousada.DomainTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/pousada/DomainTest.java
4 testes  src/test/java/br/com/marealta/pousada/ApiIntegrationTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 468 de 511 (91.6%)
- ramos (if/switch): 67 de 86 (77.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 8 de 17 | 0 de 4 |
| `br.com.marealta.pousada.app` | 60 de 61 | 10 de 12 |
| `br.com.marealta.pousada.domain` | 134 de 134 | 33 de 36 |
| `br.com.marealta.pousada.notification` | 59 de 59 | 4 de 4 |
| `br.com.marealta.pousada.persistence` | 114 de 134 | 8 de 10 |
| `br.com.marealta.pousada.web` | 93 de 106 | 12 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.ApiTest` | 3 | 0 | 0 |
| `br.com.marealta.pousada.DomainTest` | 6 | 0 | 0 |
| `br.com.marealta.pousada.NotifiersTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/pousada/DomainTest.java
3 testes  src/test/java/br/com/marealta/pousada/NotifiersTest.java
3 testes  src/test/java/br/com/marealta/pousada/ApiTest.java
```

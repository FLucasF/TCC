# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 417 de 468 (89.1%)
- ramos (if/switch): 79 de 101 (78.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta.pousada` | 0 de 11 | 0 de 0 |
| `com.marealta.pousada.domain` | 208 de 208 | 44 de 52 |
| `com.marealta.pousada.mail` | 53 de 54 | 23 de 27 |
| `com.marealta.pousada.persistence` | 89 de 116 | 4 de 6 |
| `com.marealta.pousada.web` | 67 de 79 | 8 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.pousada.ApiTest` | 5 | 0 | 0 |
| `com.marealta.pousada.JdbcRepositoryTest` | 1 | 0 | 0 |
| `com.marealta.pousada.PricingTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/marealta/pousada/ApiTest.java
4 testes  src/test/java/com/marealta/pousada/PricingTest.java
1 testes  src/test/java/com/marealta/pousada/JdbcRepositoryTest.java
0 testes  src/test/java/com/marealta/pousada/InMemory.java
```

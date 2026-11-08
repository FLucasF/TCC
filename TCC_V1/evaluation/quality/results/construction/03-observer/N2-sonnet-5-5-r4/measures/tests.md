# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 317 de 464 (68.3%)
- ramos (if/switch): 76 de 105 (72.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 0 de 14 | 0 de 4 |
| `br.com.marealta.pousada.domain` | 178 de 179 | 49 de 53 |
| `br.com.marealta.pousada.notification` | 40 de 40 | 6 de 6 |
| `br.com.marealta.pousada.persistence` | 0 de 119 | 0 de 8 |
| `br.com.marealta.pousada.report` | 25 de 25 | 8 de 10 |
| `br.com.marealta.pousada.web` | 74 de 87 | 13 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.ApiTest` | 2 | 0 | 0 |
| `br.com.marealta.pousada.QuoteTest` | 4 | 0 | 0 |
| `br.com.marealta.pousada.ReservationServiceTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/pousada/ReservationServiceTest.java
4 testes  src/test/java/br/com/marealta/pousada/QuoteTest.java
2 testes  src/test/java/br/com/marealta/pousada/ApiTest.java
0 testes  src/test/java/br/com/marealta/pousada/InMemory.java
```

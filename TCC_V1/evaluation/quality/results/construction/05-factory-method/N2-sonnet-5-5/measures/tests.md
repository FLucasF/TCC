# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 385 de 540 (71.3%)
- ramos (if/switch): 79 de 114 (69.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 385 de 540 | 79 de 114 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 3 | 0 | 0 |
| `br.com.marealta.DomainTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/br/com/marealta/DomainTest.java
3 testes  src/test/java/br/com/marealta/ApiTest.java
0 testes  src/test/java/br/com/marealta/InMemoryReservationRepository.java
0 testes  src/test/java/br/com/marealta/InMemoryMessageRepository.java
```

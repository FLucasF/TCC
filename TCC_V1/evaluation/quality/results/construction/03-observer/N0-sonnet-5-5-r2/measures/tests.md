# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 308 de 514 (59.9%)
- ramos (if/switch): 84 de 120 (70.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 15 | 0 de 8 |
| `br.com.marealta.domain` | 86 de 86 | 42 de 44 |
| `br.com.marealta.events` | 60 de 61 | 13 de 14 |
| `br.com.marealta.infra` | 0 de 184 | 0 de 18 |
| `br.com.marealta.service` | 95 de 95 | 16 de 16 |
| `br.com.marealta.web` | 67 de 73 | 13 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.HttpApiTest` | 2 | 0 | 0 |
| `br.com.marealta.PricingTest` | 5 | 0 | 0 |
| `br.com.marealta.ReservationFlowTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/ReservationFlowTest.java
5 testes  src/test/java/br/com/marealta/PricingTest.java
2 testes  src/test/java/br/com/marealta/HttpApiTest.java
0 testes  src/test/java/br/com/marealta/InMemory.java
```

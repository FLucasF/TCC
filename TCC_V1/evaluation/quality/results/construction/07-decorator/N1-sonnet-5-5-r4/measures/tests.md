# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 489 de 538 (90.9%)
- ramos (if/switch): 100 de 117 (85.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 4 de 14 | 0 de 0 |
| `br.com.marealta.pousada.domain` | 147 de 148 | 51 de 54 |
| `br.com.marealta.pousada.messaging` | 73 de 76 | 32 de 37 |
| `br.com.marealta.pousada.persistence` | 132 de 158 | 6 de 8 |
| `br.com.marealta.pousada.service` | 52 de 52 | 2 de 2 |
| `br.com.marealta.pousada.web` | 81 de 90 | 9 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.ApiTest` | 4 | 0 | 0 |
| `br.com.marealta.pousada.MessagePipelineTest` | 5 | 0 | 0 |
| `br.com.marealta.pousada.PricingTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 2
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/marealta/pousada/PricingTest.java
5 testes  src/test/java/br/com/marealta/pousada/MessagePipelineTest.java
4 testes  src/test/java/br/com/marealta/pousada/ApiTest.java
0 testes  src/test/java/br/com/marealta/pousada/TestSystem.java
```

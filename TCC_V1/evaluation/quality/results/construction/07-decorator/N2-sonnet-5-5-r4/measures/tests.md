# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 385 de 423 (91.0%)
- ramos (if/switch): 87 de 113 (77.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 7 de 16 | 0 de 4 |
| `pousada.domain` | 76 de 77 | 29 de 34 |
| `pousada.messaging` | 58 de 59 | 24 de 25 |
| `pousada.persistence` | 133 de 144 | 16 de 22 |
| `pousada.service` | 56 de 58 | 8 de 8 |
| `pousada.web` | 55 de 69 | 10 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiIntegrationTest` | 2 | 0 | 0 |
| `pousada.MessagePipelineTest` | 6 | 0 | 0 |
| `pousada.PricingTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/pousada/MessagePipelineTest.java
3 testes  src/test/java/pousada/PricingTest.java
2 testes  src/test/java/pousada/ApiIntegrationTest.java
```

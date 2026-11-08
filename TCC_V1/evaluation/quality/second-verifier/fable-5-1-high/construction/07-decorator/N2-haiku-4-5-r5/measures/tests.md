# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 85 de 787 (10.8%)
- ramos (if/switch): 31 de 196 (15.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 23 | 0 de 0 |
| `com.pousada.marealta.api` | 0 de 225 | 0 de 44 |
| `com.pousada.marealta.config` | 0 de 31 | 0 de 10 |
| `com.pousada.marealta.domain` | 3 de 105 | 0 de 10 |
| `com.pousada.marealta.messaging` | 31 de 31 | 4 de 4 |
| `com.pousada.marealta.messaging.processors` | 30 de 40 | 16 de 30 |
| `com.pousada.marealta.repository` | 0 de 151 | 0 de 26 |
| `com.pousada.marealta.service` | 21 de 181 | 11 de 72 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.messaging.MessageProcessorChainTest` | 4 | 0 | 0 |
| `com.pousada.marealta.service.PriceCalculatorTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousada/marealta/service/PriceCalculatorTest.java
4 testes  src/test/java/com/pousada/marealta/messaging/MessageProcessorChainTest.java
```

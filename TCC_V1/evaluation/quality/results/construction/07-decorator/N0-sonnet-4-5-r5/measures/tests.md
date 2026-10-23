# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 107 de 833 (12.8%)
- ramos (if/switch): 42 de 168 (25.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 48 | 0 de 4 |
| `com.pousada.marealta.config` | 0 de 23 | 0 de 2 |
| `com.pousada.marealta.controller` | 0 de 156 | 0 de 22 |
| `com.pousada.marealta.domain` | 27 de 110 | 0 de 16 |
| `com.pousada.marealta.dto` | 0 de 47 | 0 de 0 |
| `com.pousada.marealta.messaging` | 13 de 13 | 4 de 4 |
| `com.pousada.marealta.messaging.rules` | 51 de 53 | 26 de 30 |
| `com.pousada.marealta.repository` | 0 de 195 | 0 de 38 |
| `com.pousada.marealta.service` | 16 de 188 | 12 de 52 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.MessageRulesTest` | 11 | 0 | 0 |
| `com.pousada.marealta.PricingServiceTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/pousada/marealta/MessageRulesTest.java
6 testes  src/test/java/com/pousada/marealta/PricingServiceTest.java
```

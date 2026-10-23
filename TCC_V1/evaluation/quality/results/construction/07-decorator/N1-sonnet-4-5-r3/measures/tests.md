# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 15 testes rodaram (0 falhas, 0 erros, 0 pulados); 15 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 198 de 827 (23.9%)
- ramos (if/switch): 46 de 166 (27.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 7 | 0 de 0 |
| `com.pousadamarealta.config` | 0 de 34 | 0 de 10 |
| `com.pousadamarealta.domain` | 112 de 183 | 26 de 46 |
| `com.pousadamarealta.messaging` | 54 de 55 | 10 de 10 |
| `com.pousadamarealta.messaging.rules` | 32 de 35 | 10 de 14 |
| `com.pousadamarealta.repository` | 0 de 200 | 0 de 38 |
| `com.pousadamarealta.service` | 0 de 105 | 0 de 16 |
| `com.pousadamarealta.web` | 0 de 161 | 0 de 32 |
| `com.pousadamarealta.web.dto` | 0 de 47 | 0 de 0 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.ReservationTest` | 9 | 0 | 0 |
| `com.pousadamarealta.messaging.MessagePipelineTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
6 testes  src/test/java/com/pousadamarealta/messaging/MessagePipelineTest.java
```

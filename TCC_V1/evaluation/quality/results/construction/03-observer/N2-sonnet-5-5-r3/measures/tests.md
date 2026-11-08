# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

Sem relatório do JaCoCo: o agente do JaCoCo não entrou nos testes (por exemplo, um `argLine` no surefire sem `@{argLine}`), ou nenhum teste rodou.

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.ApiTest` | 5 | 0 | 0 |
| `com.marealta.domain.QuoteTest` | 3 | 0 | 0 |
| `com.marealta.domain.ReceiptTest` | 4 | 0 | 0 |
| `com.marealta.events.ListenersTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/marealta/ApiTest.java
4 testes  src/test/java/com/marealta/events/ListenersTest.java
4 testes  src/test/java/com/marealta/domain/ReceiptTest.java
3 testes  src/test/java/com/marealta/domain/QuoteTest.java
```

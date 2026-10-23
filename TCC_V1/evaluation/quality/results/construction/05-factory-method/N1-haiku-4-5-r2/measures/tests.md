# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 128 de 667 (19.2%)
- ramos (if/switch): 37 de 204 (18.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.posadamarealta` | 0 de 46 | 0 de 6 |
| `com.posadamarealta.handlers` | 0 de 210 | 0 de 68 |
| `com.posadamarealta.model` | 23 de 37 | 0 de 0 |
| `com.posadamarealta.repository` | 0 de 180 | 0 de 32 |
| `com.posadamarealta.service` | 105 de 194 | 37 de 98 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.posadamarealta.integration.ReservationFlowTest` | 6 | 0 | 0 |
| `com.posadamarealta.service.DocumentServiceTest` | 4 | 0 | 0 |
| `com.posadamarealta.service.UtilServiceTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/posadamarealta/service/UtilServiceTest.java
6 testes  src/test/java/com/posadamarealta/integration/ReservationFlowTest.java
4 testes  src/test/java/com/posadamarealta/service/DocumentServiceTest.java
```

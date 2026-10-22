# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 82 de 818 (10.0%)
- ramos (if/switch): 21 de 172 (12.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.maralta` | 0 de 21 | 0 de 0 |
| `com.pousada.maralta.controller` | 0 de 127 | 0 de 54 |
| `com.pousada.maralta.controller.dto` | 0 de 91 | 0 de 0 |
| `com.pousada.maralta.domain` | 57 de 101 | 10 de 28 |
| `com.pousada.maralta.infrastructure` | 0 de 33 | 0 de 2 |
| `com.pousada.maralta.repository` | 0 de 195 | 0 de 44 |
| `com.pousada.maralta.service` | 25 de 250 | 11 de 44 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.maralta.integration.ReservationFlowTest` | 3 | 0 | 0 |
| `com.pousada.maralta.service.PricingServiceTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousada/maralta/service/PricingServiceTest.java
3 testes  src/test/java/com/pousada/maralta/integration/ReservationFlowTest.java
```

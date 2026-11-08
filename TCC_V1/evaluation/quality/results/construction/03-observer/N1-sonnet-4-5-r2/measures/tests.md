# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 103 de 680 (15.1%)
- ramos (if/switch): 27 de 116 (23.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 39 | 0 de 4 |
| `com.pousadamarealta.application.notification` | 0 de 98 | 0 de 12 |
| `com.pousadamarealta.application.service` | 0 de 103 | 0 de 18 |
| `com.pousadamarealta.domain.event` | 13 de 13 | 3 de 4 |
| `com.pousadamarealta.domain.model` | 90 de 123 | 24 de 36 |
| `com.pousadamarealta.infrastructure.http` | 0 de 115 | 0 de 22 |
| `com.pousadamarealta.infrastructure.http.dto` | 0 de 46 | 0 de 0 |
| `com.pousadamarealta.infrastructure.persistence` | 0 de 143 | 0 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.EventPublisherTest` | 3 | 0 | 0 |
| `com.pousadamarealta.ReservationTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/pousadamarealta/ReservationTest.java
3 testes  src/test/java/com/pousadamarealta/EventPublisherTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 18 testes rodaram (0 falhas, 0 erros, 0 pulados); 18 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 110 de 790 (13.9%)
- ramos (if/switch): 35 de 154 (22.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 70 | 0 de 10 |
| `com.pousadamarealta.api` | 0 de 6 | 0 de 0 |
| `com.pousadamarealta.api.controller` | 0 de 191 | 0 de 58 |
| `com.pousadamarealta.api.dto` | 0 de 47 | 0 de 2 |
| `com.pousadamarealta.domain` | 101 de 181 | 25 de 30 |
| `com.pousadamarealta.notification` | 0 de 56 | 0 de 8 |
| `com.pousadamarealta.repository` | 0 de 217 | 0 de 34 |
| `com.pousadamarealta.util` | 9 de 22 | 10 de 12 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.ReservationTest` | 11 | 0 | 0 |
| `com.pousadamarealta.notification.ReservationObserverTest` | 3 | 0 | 0 |
| `com.pousadamarealta.util.SeasonCalculatorTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
4 testes  src/test/java/com/pousadamarealta/util/SeasonCalculatorTest.java
3 testes  src/test/java/com/pousadamarealta/notification/ReservationObserverTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 27 de 761 (3.5%)
- ramos (if/switch): 16 de 168 (9.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 25 | 0 de 0 |
| `com.pousadamarealta.handler` | 0 de 347 | 0 de 80 |
| `com.pousadamarealta.infrastructure` | 0 de 26 | 0 de 8 |
| `com.pousadamarealta.model` | 3 de 102 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 141 | 0 de 30 |
| `com.pousadamarealta.service` | 0 de 79 | 0 de 30 |
| `com.pousadamarealta.util` | 24 de 41 | 16 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.PriceCalculatorTest` | 4 | 0 | 0 |
| `com.pousadamarealta.ReservationServiceTest` | 11 | 0 | 0 |
| `com.pousadamarealta.RoundingTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/pousadamarealta/ReservationServiceTest.java
4 testes  src/test/java/com/pousadamarealta/PriceCalculatorTest.java
2 testes  src/test/java/com/pousadamarealta/RoundingTest.java
```

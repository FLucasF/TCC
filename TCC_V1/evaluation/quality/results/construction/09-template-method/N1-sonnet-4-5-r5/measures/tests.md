# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 25 testes rodaram (0 falhas, 0 erros, 0 pulados); 25 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 101 de 846 (11.9%)
- ramos (if/switch): 32 de 134 (23.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamarealta` | 0 de 45 | 0 de 0 |
| `br.com.pousadamarealta.api` | 0 de 230 | 0 de 36 |
| `br.com.pousadamarealta.database` | 0 de 13 | 0 de 0 |
| `br.com.pousadamarealta.domain.message` | 0 de 54 | 0 de 4 |
| `br.com.pousadamarealta.domain.pricing` | 16 de 17 | 4 de 4 |
| `br.com.pousadamarealta.domain.reservation` | 52 de 293 | 18 de 58 |
| `br.com.pousadamarealta.domain.room` | 14 de 43 | 0 de 10 |
| `br.com.pousadamarealta.report` | 0 de 130 | 0 de 12 |
| `br.com.pousadamarealta.util` | 19 de 21 | 10 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamarealta.domain.pricing.PricingCalculatorTest` | 6 | 0 | 0 |
| `br.com.pousadamarealta.domain.reservation.ReservationTest` | 11 | 0 | 0 |
| `br.com.pousadamarealta.util.DateUtilsTest` | 5 | 0 | 0 |
| `br.com.pousadamarealta.util.MoneyUtilsTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/br/com/pousadamarealta/domain/reservation/ReservationTest.java
6 testes  src/test/java/br/com/pousadamarealta/domain/pricing/PricingCalculatorTest.java
5 testes  src/test/java/br/com/pousadamarealta/util/DateUtilsTest.java
3 testes  src/test/java/br/com/pousadamarealta/util/MoneyUtilsTest.java
```

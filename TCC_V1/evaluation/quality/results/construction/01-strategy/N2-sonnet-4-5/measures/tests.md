# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 20 testes rodaram (0 falhas, 0 erros, 0 pulados); 20 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 140 de 753 (18.6%)
- ramos (if/switch): 26 de 148 (17.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 48 | 0 de 0 |
| `com.pousadamarealta.api` | 0 de 138 | 0 de 40 |
| `com.pousadamarealta.api.dto` | 0 de 46 | 0 de 0 |
| `com.pousadamarealta.domain` | 72 de 110 | 4 de 18 |
| `com.pousadamarealta.domain.rateplan` | 44 de 52 | 13 de 16 |
| `com.pousadamarealta.exception` | 4 de 12 | 0 de 0 |
| `com.pousadamarealta.infrastructure` | 0 de 24 | 0 de 2 |
| `com.pousadamarealta.repository` | 0 de 202 | 0 de 44 |
| `com.pousadamarealta.service` | 0 de 73 | 0 de 8 |
| `com.pousadamarealta.util` | 20 de 48 | 9 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.rateplan.CorporateRatePlanTest` | 3 | 0 | 0 |
| `com.pousadamarealta.domain.rateplan.LongStayRatePlanTest` | 2 | 0 | 0 |
| `com.pousadamarealta.domain.rateplan.NonRefundableRatePlanTest` | 2 | 0 | 0 |
| `com.pousadamarealta.domain.rateplan.StandardRatePlanTest` | 2 | 0 | 0 |
| `com.pousadamarealta.domain.rateplan.WeekendPackageRatePlanTest` | 4 | 0 | 0 |
| `com.pousadamarealta.domain.ReservationTest` | 4 | 0 | 0 |
| `com.pousadamarealta.util.PriceCalculatorTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 7
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
4 testes  src/test/java/com/pousadamarealta/domain/rateplan/WeekendPackageRatePlanTest.java
3 testes  src/test/java/com/pousadamarealta/util/PriceCalculatorTest.java
3 testes  src/test/java/com/pousadamarealta/domain/rateplan/CorporateRatePlanTest.java
2 testes  src/test/java/com/pousadamarealta/domain/rateplan/StandardRatePlanTest.java
2 testes  src/test/java/com/pousadamarealta/domain/rateplan/NonRefundableRatePlanTest.java
2 testes  src/test/java/com/pousadamarealta/domain/rateplan/LongStayRatePlanTest.java
```

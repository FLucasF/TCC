# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 98 de 710 (13.8%)
- ramos (if/switch): 23 de 202 (11.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 188 | 0 de 48 |
| `com.pousadamarealta.api` | 0 de 53 | 0 de 4 |
| `com.pousadamarealta.database` | 0 de 190 | 0 de 58 |
| `com.pousadamarealta.models` | 6 de 32 | 0 de 0 |
| `com.pousadamarealta.rateplans` | 92 de 124 | 23 de 34 |
| `com.pousadamarealta.services` | 0 de 123 | 0 de 58 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.rateplans.CorporatePlanTest` | 2 | 0 | 0 |
| `com.pousadamarealta.rateplans.LongStayTest` | 2 | 0 | 0 |
| `com.pousadamarealta.rateplans.NonRefundablePlanTest` | 1 | 0 | 0 |
| `com.pousadamarealta.rateplans.StandardPlanTest` | 2 | 0 | 0 |
| `com.pousadamarealta.rateplans.WeekendPackageTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 5
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
2 testes  src/test/java/com/pousadamarealta/rateplans/WeekendPackageTest.java
2 testes  src/test/java/com/pousadamarealta/rateplans/StandardPlanTest.java
2 testes  src/test/java/com/pousadamarealta/rateplans/LongStayTest.java
2 testes  src/test/java/com/pousadamarealta/rateplans/CorporatePlanTest.java
1 testes  src/test/java/com/pousadamarealta/rateplans/NonRefundablePlanTest.java
```

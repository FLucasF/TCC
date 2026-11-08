# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 20 testes rodaram (0 falhas, 0 erros, 0 pulados); 20 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 108 de 845 (12.8%)
- ramos (if/switch): 41 de 197 (20.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 10 | 0 de 0 |
| `com.pousada.api` | 0 de 201 | 0 de 48 |
| `com.pousada.api.dto` | 0 de 1 | 0 de 0 |
| `com.pousada.infrastructure` | 0 de 35 | 0 de 6 |
| `com.pousada.model` | 11 de 109 | 0 de 0 |
| `com.pousada.rateplan` | 82 de 106 | 31 de 55 |
| `com.pousada.repository` | 0 de 177 | 0 de 26 |
| `com.pousada.service` | 0 de 189 | 0 de 52 |
| `com.pousada.util` | 15 de 17 | 10 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.rateplan.CorporateRatePlanTest` | 3 | 0 | 0 |
| `com.pousada.rateplan.LongStayTest` | 3 | 0 | 0 |
| `com.pousada.rateplan.NonRefundableTest` | 2 | 0 | 0 |
| `com.pousada.rateplan.StandardRatePlanTest` | 1 | 0 | 0 |
| `com.pousada.rateplan.WeekendPackageTest` | 3 | 0 | 0 |
| `com.pousada.util.MoneyTest` | 3 | 0 | 0 |
| `com.pousada.util.SeasonTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 7
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousada/util/SeasonTest.java
3 testes  src/test/java/com/pousada/util/MoneyTest.java
3 testes  src/test/java/com/pousada/rateplan/WeekendPackageTest.java
3 testes  src/test/java/com/pousada/rateplan/LongStayTest.java
3 testes  src/test/java/com/pousada/rateplan/CorporateRatePlanTest.java
2 testes  src/test/java/com/pousada/rateplan/NonRefundableTest.java
1 testes  src/test/java/com/pousada/rateplan/StandardRatePlanTest.java
```

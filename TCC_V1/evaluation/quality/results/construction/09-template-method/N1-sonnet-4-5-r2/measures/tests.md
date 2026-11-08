# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 39 de 911 (4.3%)
- ramos (if/switch): 15 de 160 (9.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 40 | 0 de 2 |
| `com.pousadamarealta.api` | 0 de 226 | 0 de 46 |
| `com.pousadamarealta.domain` | 3 de 121 | 0 de 10 |
| `com.pousadamarealta.pricing` | 27 de 28 | 13 de 16 |
| `com.pousadamarealta.report` | 9 de 136 | 2 de 18 |
| `com.pousadamarealta.repository` | 0 de 231 | 0 de 42 |
| `com.pousadamarealta.service` | 0 de 129 | 0 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.pricing.PricingCalculatorTest` | 7 | 0 | 0 |
| `com.pousadamarealta.report.CsvReportTest` | 1 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamarealta/pricing/PricingCalculatorTest.java
1 testes  src/test/java/com/pousadamarealta/report/CsvReportTest.java
```

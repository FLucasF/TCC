# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 55 de 898 (6.1%)
- ramos (if/switch): 11 de 136 (8.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 51 | 0 de 2 |
| `com.pousadamarealta.api` | 0 de 220 | 0 de 16 |
| `com.pousadamarealta.domain` | 17 de 143 | 0 de 10 |
| `com.pousadamarealta.report` | 13 de 128 | 0 de 30 |
| `com.pousadamarealta.repository` | 0 de 200 | 0 de 30 |
| `com.pousadamarealta.service` | 25 de 156 | 11 de 48 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.report.ReportGeneratorTest` | 1 | 0 | 0 |
| `com.pousadamarealta.service.PricingServiceTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
1 testes  src/test/java/com/pousadamarealta/report/ReportGeneratorTest.java
```

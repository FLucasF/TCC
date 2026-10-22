# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 25 testes rodaram (0 falhas, 0 erros, 0 pulados); 25 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 212 de 811 (26.1%)
- ramos (if/switch): 50 de 144 (34.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 38 | 0 de 2 |
| `com.pousadamarealta.api` | 0 de 161 | 0 de 26 |
| `com.pousadamarealta.api.dto` | 0 de 45 | 0 de 0 |
| `com.pousadamarealta.domain` | 104 de 137 | 41 de 50 |
| `com.pousadamarealta.infrastructure` | 0 de 210 | 0 de 40 |
| `com.pousadamarealta.service` | 0 de 111 | 0 de 14 |
| `com.pousadamarealta.service.report` | 108 de 109 | 9 de 12 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.ReservationTest` | 18 | 0 | 0 |
| `com.pousadamarealta.service.report.ReportTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
18 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
7 testes  src/test/java/com/pousadamarealta/service/report/ReportTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 22 testes rodaram (0 falhas, 0 erros, 0 pulados); 22 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 375 de 887 (42.3%)
- ramos (if/switch): 39 de 123 (31.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 47 | 0 de 2 |
| `com.pousada.api` | 0 de 247 | 0 de 32 |
| `com.pousada.api.dto` | 0 de 5 | 0 de 0 |
| `com.pousada.domain` | 115 de 118 | 0 de 6 |
| `com.pousada.domain.rateplan` | 13 de 16 | 1 de 2 |
| `com.pousada.domain.state` | 29 de 45 | 3 de 5 |
| `com.pousada.report` | 0 de 136 | 0 de 12 |
| `com.pousada.repository` | 162 de 213 | 23 de 44 |
| `com.pousada.service` | 56 de 60 | 12 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.IntegrationIT` | 8 | 0 | 0 |
| `com.pousada.PricingServiceTest` | 5 | 0 | 0 |
| `com.pousada.ReservationTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousada/ReservationTest.java
8 testes  src/test/java/com/pousada/IntegrationIT.java
5 testes  src/test/java/com/pousada/PricingServiceTest.java
```

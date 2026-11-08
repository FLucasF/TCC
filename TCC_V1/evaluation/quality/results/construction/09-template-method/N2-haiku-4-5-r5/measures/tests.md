# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 0 erros, 0 pulados); 13 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 41 de 726 (5.6%)
- ramos (if/switch): 16 de 190 (8.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 195 | 0 de 22 |
| `com.pousada.database` | 0 de 42 | 0 de 18 |
| `com.pousada.domain` | 13 de 45 | 0 de 0 |
| `com.pousada.report` | 0 de 129 | 0 de 42 |
| `com.pousada.repository` | 0 de 198 | 0 de 48 |
| `com.pousada.service` | 28 de 117 | 16 de 60 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.service.PricingServiceTest` | 4 | 0 | 0 |
| `com.pousada.service.SeasonalityCalculatorTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousada/service/SeasonalityCalculatorTest.java
4 testes  src/test/java/com/pousada/service/PricingServiceTest.java
```

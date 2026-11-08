# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 19 testes rodaram (0 falhas, 0 erros, 0 pulados); 19 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 330 de 738 (44.7%)
- ramos (if/switch): 45 de 106 (42.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 50 | 0 de 0 |
| `com.pousadamarealta.controller` | 0 de 169 | 0 de 16 |
| `com.pousadamarealta.model` | 91 de 102 | 0 de 0 |
| `com.pousadamarealta.report` | 0 de 109 | 0 de 26 |
| `com.pousadamarealta.repository` | 105 de 160 | 9 de 20 |
| `com.pousadamarealta.service` | 115 de 122 | 24 de 30 |
| `com.pousadamarealta.util` | 19 de 26 | 12 de 14 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.DateUtilsTest` | 5 | 0 | 0 |
| `com.pousadamarealta.IntegrationTest` | 8 | 0 | 0 |
| `com.pousadamarealta.PricingServiceTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/IntegrationTest.java
6 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
5 testes  src/test/java/com/pousadamarealta/DateUtilsTest.java
```

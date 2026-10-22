# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 84 de 795 (10.6%)
- ramos (if/switch): 20 de 153 (13.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 20 | 0 de 8 |
| `com.pousada.api` | 0 de 197 | 0 de 30 |
| `com.pousada.database` | 0 de 160 | 0 de 20 |
| `com.pousada.exception` | 0 de 18 | 0 de 0 |
| `com.pousada.model` | 15 de 108 | 0 de 0 |
| `com.pousada.service` | 0 de 160 | 0 de 48 |
| `com.pousada.service.pricing` | 50 de 92 | 7 de 19 |
| `com.pousada.util` | 19 de 40 | 13 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.service.pricing.PricingStrategyTest` | 4 | 0 | 0 |
| `com.pousada.util.PriceCalculatorTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousada/util/PriceCalculatorTest.java
4 testes  src/test/java/com/pousada/service/pricing/PricingStrategyTest.java
```

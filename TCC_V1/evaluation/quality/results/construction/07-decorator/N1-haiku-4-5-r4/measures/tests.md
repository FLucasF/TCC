# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 28 de 601 (4.7%)
- ramos (if/switch): 26 de 180 (14.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 14 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 203 | 0 de 52 |
| `com.pousadamarealta.dao` | 0 de 161 | 0 de 28 |
| `com.pousadamarealta.db` | 0 de 32 | 0 de 4 |
| `com.pousadamarealta.models` | 2 de 14 | 0 de 0 |
| `com.pousadamarealta.service` | 0 de 147 | 0 de 60 |
| `com.pousadamarealta.util` | 26 de 30 | 26 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.integration.PricingIntegrationTest` | 2 | 0 | 0 |
| `com.pousadamarealta.integration.SpecificationExampleTest` | 1 | 0 | 0 |
| `com.pousadamarealta.util.EmailValidatorTest` | 2 | 0 | 0 |
| `com.pousadamarealta.util.PricingCalculatorTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousadamarealta/util/PricingCalculatorTest.java
2 testes  src/test/java/com/pousadamarealta/util/EmailValidatorTest.java
2 testes  src/test/java/com/pousadamarealta/integration/PricingIntegrationTest.java
1 testes  src/test/java/com/pousadamarealta/integration/SpecificationExampleTest.java
```

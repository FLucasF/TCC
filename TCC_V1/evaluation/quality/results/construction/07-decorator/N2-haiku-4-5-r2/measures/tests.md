# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 60 de 842 (7.1%)
- ramos (if/switch): 32 de 170 (18.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 14 | 0 de 0 |
| `com.pousada.marealta.config` | 0 de 37 | 0 de 4 |
| `com.pousada.marealta.domain` | 17 de 124 | 0 de 4 |
| `com.pousada.marealta.http` | 0 de 260 | 0 de 40 |
| `com.pousada.marealta.messaging` | 7 de 40 | 2 de 14 |
| `com.pousada.marealta.repository` | 0 de 212 | 0 de 34 |
| `com.pousada.marealta.service` | 0 de 114 | 0 de 38 |
| `com.pousada.marealta.util` | 36 de 41 | 30 de 36 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.messaging.EmailValidationRuleTest` | 2 | 0 | 0 |
| `com.pousada.marealta.util.EmailValidatorTest` | 8 | 0 | 0 |
| `com.pousada.marealta.util.PricingCalculatorTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousada/marealta/util/EmailValidatorTest.java
6 testes  src/test/java/com/pousada/marealta/util/PricingCalculatorTest.java
2 testes  src/test/java/com/pousada/marealta/messaging/EmailValidationRuleTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 14 testes rodaram (0 falhas, 0 erros, 0 pulados); 14 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 106 de 835 (12.7%)
- ramos (if/switch): 40 de 140 (28.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 59 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 178 | 0 de 10 |
| `com.pousadamarealta.api.dto` | 0 de 35 | 0 de 0 |
| `com.pousadamarealta.database` | 0 de 15 | 0 de 0 |
| `com.pousadamarealta.domain` | 37 de 133 | 14 de 26 |
| `com.pousadamarealta.messaging` | 33 de 54 | 6 de 10 |
| `com.pousadamarealta.messaging.rules` | 36 de 40 | 20 de 26 |
| `com.pousadamarealta.repository` | 0 de 194 | 0 de 40 |
| `com.pousadamarealta.service` | 0 de 127 | 0 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.PriceCalculatorTest` | 7 | 0 | 0 |
| `com.pousadamarealta.messaging.MessageRuleChainTest` | 3 | 0 | 0 |
| `com.pousadamarealta.messaging.rules.EmailValidationRuleTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamarealta/domain/PriceCalculatorTest.java
4 testes  src/test/java/com/pousadamarealta/messaging/rules/EmailValidationRuleTest.java
3 testes  src/test/java/com/pousadamarealta/messaging/MessageRuleChainTest.java
```

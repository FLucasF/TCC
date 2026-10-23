# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 17 testes rodaram (0 falhas, 0 erros, 0 pulados); 17 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 98 de 895 (10.9%)
- ramos (if/switch): 26 de 168 (15.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 81 | 0 de 8 |
| `com.pousadamarealta.api.controller` | 0 de 129 | 0 de 38 |
| `com.pousadamarealta.api.dto` | 0 de 31 | 0 de 0 |
| `com.pousadamarealta.database` | 0 de 276 | 0 de 42 |
| `com.pousadamarealta.domain` | 41 de 115 | 0 de 0 |
| `com.pousadamarealta.exception` | 0 de 12 | 0 de 0 |
| `com.pousadamarealta.messaging` | 30 de 55 | 14 de 26 |
| `com.pousadamarealta.service` | 27 de 196 | 12 de 54 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.messaging.ArchiveCopyRuleTest` | 2 | 0 | 0 |
| `com.pousadamarealta.messaging.EmailValidationRuleTest` | 5 | 0 | 0 |
| `com.pousadamarealta.messaging.TestModeRuleTest` | 2 | 0 | 0 |
| `com.pousadamarealta.service.PricingServiceTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 4
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
5 testes  src/test/java/com/pousadamarealta/messaging/EmailValidationRuleTest.java
2 testes  src/test/java/com/pousadamarealta/messaging/TestModeRuleTest.java
2 testes  src/test/java/com/pousadamarealta/messaging/ArchiveCopyRuleTest.java
```

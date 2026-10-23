# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 19 testes rodaram (0 falhas, 0 erros, 0 pulados); 19 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 93 de 751 (12.4%)
- ramos (if/switch): 29 de 156 (18.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 46 | 0 de 10 |
| `com.pousadamarealta.api` | 0 de 178 | 0 de 20 |
| `com.pousadamarealta.domain` | 3 de 62 | 0 de 10 |
| `com.pousadamarealta.infra` | 0 de 18 | 0 de 4 |
| `com.pousadamarealta.messaging` | 64 de 66 | 15 de 20 |
| `com.pousadamarealta.repository` | 0 de 185 | 0 de 34 |
| `com.pousadamarealta.service` | 26 de 196 | 14 de 58 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.BasicIT` | 1 | 0 | 0 |
| `com.pousadamarealta.MessageRulesTest` | 9 | 0 | 0 |
| `com.pousadamarealta.PricingServiceTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousadamarealta/PricingServiceTest.java
9 testes  src/test/java/com/pousadamarealta/MessageRulesTest.java
1 testes  src/test/java/com/pousadamarealta/BasicIT.java
```

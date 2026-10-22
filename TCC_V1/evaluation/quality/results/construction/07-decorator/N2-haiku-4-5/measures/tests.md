# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 67 de 703 (9.5%)
- ramos (if/switch): 17 de 150 (11.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 24 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 228 | 0 de 36 |
| `com.pousadamarealta.db` | 3 de 166 | 0 de 32 |
| `com.pousadamarealta.model` | 11 de 74 | 0 de 0 |
| `com.pousadamarealta.service` | 53 de 211 | 17 de 74 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.service.MessageServiceTest` | 4 | 0 | 0 |
| `com.pousadamarealta.service.PricingServiceTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
4 testes  src/test/java/com/pousadamarealta/service/MessageServiceTest.java
```

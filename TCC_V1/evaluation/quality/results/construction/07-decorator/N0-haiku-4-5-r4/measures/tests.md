# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 36 de 608 (5.9%)
- ramos (if/switch): 20 de 148 (13.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 9 | 0 de 0 |
| `com.pousadamarealta.api` | 0 de 314 | 0 de 82 |
| `com.pousadamarealta.database` | 0 de 32 | 0 de 2 |
| `com.pousadamarealta.model` | 0 de 32 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 147 | 0 de 32 |
| `com.pousadamarealta.service` | 36 de 74 | 20 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.service.MessageServiceTest` | 5 | 0 | 0 |
| `com.pousadamarealta.service.PricingServiceTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousadamarealta/service/MessageServiceTest.java
4 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
```

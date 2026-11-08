# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 47 de 828 (5.7%)
- ramos (if/switch): 14 de 148 (9.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 45 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 225 | 0 de 22 |
| `com.pousadamarealta.domain` | 17 de 103 | 0 de 6 |
| `com.pousadamarealta.repository` | 0 de 218 | 0 de 42 |
| `com.pousadamarealta.service` | 30 de 237 | 14 de 70 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.service.PricingServiceTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
```

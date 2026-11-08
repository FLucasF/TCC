# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 26 de 706 (3.7%)
- ramos (if/switch): 16 de 152 (10.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 24 | 0 de 0 |
| `com.pousadamarealta.db` | 0 de 32 | 0 de 4 |
| `com.pousadamarealta.model` | 0 de 47 | 0 de 0 |
| `com.pousadamarealta.repository` | 0 de 135 | 0 de 24 |
| `com.pousadamarealta.routes` | 0 de 234 | 0 de 34 |
| `com.pousadamarealta.service` | 26 de 199 | 16 de 82 |
| `com.pousadamarealta.util` | 0 de 35 | 0 de 8 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.service.PricingServiceTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
```

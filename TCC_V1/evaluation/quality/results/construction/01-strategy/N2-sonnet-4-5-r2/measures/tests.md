# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 85 de 809 (10.5%)
- ramos (if/switch): 24 de 150 (16.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 50 | 0 de 8 |
| `com.pousadamarealta.domain.model` | 9 de 101 | 0 de 18 |
| `com.pousadamarealta.domain.service` | 0 de 153 | 0 de 36 |
| `com.pousadamarealta.domain.service.rateplan` | 76 de 86 | 24 de 32 |
| `com.pousadamarealta.infrastructure.database` | 0 de 202 | 0 de 40 |
| `com.pousadamarealta.infrastructure.web` | 0 de 217 | 0 de 16 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.service.rateplan.RatePlanTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousadamarealta/domain/service/rateplan/RatePlanTest.java
```

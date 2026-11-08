# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 72 de 746 (9.7%)
- ramos (if/switch): 22 de 150 (14.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.marealta.pousada` | 0 de 76 | 0 de 0 |
| `com.marealta.pousada.controller` | 0 de 95 | 0 de 30 |
| `com.marealta.pousada.domain.model` | 9 de 103 | 0 de 22 |
| `com.marealta.pousada.domain.rateplan` | 49 de 70 | 14 de 28 |
| `com.marealta.pousada.dto` | 0 de 46 | 0 de 0 |
| `com.marealta.pousada.exception` | 2 de 12 | 0 de 0 |
| `com.marealta.pousada.repository` | 0 de 186 | 0 de 38 |
| `com.marealta.pousada.service` | 0 de 142 | 0 de 18 |
| `com.marealta.pousada.util` | 12 de 16 | 8 de 14 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.marealta.pousada.RatePlanTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/marealta/pousada/RatePlanTest.java
```

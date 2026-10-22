# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 47 de 925 (5.1%)
- ramos (if/switch): 9 de 172 (5.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamaralta` | 0 de 51 | 0 de 4 |
| `com.pousadamaralta.api.controller` | 0 de 240 | 0 de 40 |
| `com.pousadamaralta.api.dto` | 0 de 57 | 0 de 0 |
| `com.pousadamaralta.domain.identification` | 0 de 75 | 0 de 10 |
| `com.pousadamaralta.domain.model` | 17 de 99 | 0 de 28 |
| `com.pousadamaralta.domain.rateplan` | 7 de 10 | 0 de 0 |
| `com.pousadamaralta.domain.service` | 23 de 163 | 9 de 36 |
| `com.pousadamaralta.infrastructure.database` | 0 de 11 | 0 de 0 |
| `com.pousadamaralta.infrastructure.repository` | 0 de 219 | 0 de 54 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamaralta.PricingServiceTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousadamaralta/PricingServiceTest.java
```

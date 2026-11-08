# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 25 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 39 de 892 (4.4%)
- ramos (if/switch): 16 de 168 (9.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 66 | 0 de 8 |
| `com.pousadamarealta.controller` | 0 de 121 | 0 de 44 |
| `com.pousadamarealta.domain` | 12 de 116 | 0 de 0 |
| `com.pousadamarealta.dto` | 0 de 47 | 0 de 0 |
| `com.pousadamarealta.exception` | 0 de 18 | 0 de 0 |
| `com.pousadamarealta.report` | 0 de 111 | 0 de 10 |
| `com.pousadamarealta.repository` | 0 de 241 | 0 de 62 |
| `com.pousadamarealta.service` | 27 de 172 | 16 de 44 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.service.PricingServiceTest` | 12 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
13 testes  src/test/java/com/pousadamarealta/IntegrationTestManual.java
12 testes  src/test/java/com/pousadamarealta/service/PricingServiceTest.java
```

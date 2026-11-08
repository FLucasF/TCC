# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 110 de 804 (13.7%)
- ramos (if/switch): 9 de 114 (7.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 54 | 0 de 8 |
| `com.pousadamarealta.config` | 0 de 17 | 0 de 0 |
| `com.pousadamarealta.controller` | 0 de 156 | 0 de 16 |
| `com.pousadamarealta.domain` | 50 de 149 | 0 de 0 |
| `com.pousadamarealta.exception` | 0 de 6 | 0 de 0 |
| `com.pousadamarealta.service` | 0 de 331 | 0 de 62 |
| `com.pousadamarealta.service.rateplan` | 60 de 91 | 9 de 28 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.ReservationTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/ReservationTest.java
```

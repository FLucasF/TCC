# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 24 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 68 de 754 (9.0%)
- ramos (if/switch): 20 de 144 (13.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 18 | 0 de 0 |
| `com.pousadamarealta.api` | 0 de 221 | 0 de 18 |
| `com.pousadamarealta.domain` | 68 de 111 | 20 de 38 |
| `com.pousadamarealta.notification` | 0 de 42 | 0 de 8 |
| `com.pousadamarealta.repository` | 0 de 228 | 0 de 60 |
| `com.pousadamarealta.service` | 0 de 106 | 0 de 16 |
| `com.pousadamarealta.util` | 0 de 28 | 0 de 4 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.ReservationTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
14 testes  src/test/java/com/pousadamarealta/ReservationSystemIT.java
10 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
```

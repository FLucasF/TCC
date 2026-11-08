# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 29 de 895 (3.2%)
- ramos (if/switch): 14 de 190 (7.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 29 | 0 de 8 |
| `com.pousadamarealta.api` | 0 de 274 | 0 de 40 |
| `com.pousadamarealta.domain` | 6 de 129 | 0 de 12 |
| `com.pousadamarealta.events` | 0 de 12 | 0 de 4 |
| `com.pousadamarealta.listeners` | 0 de 40 | 0 de 4 |
| `com.pousadamarealta.services` | 0 de 381 | 0 de 104 |
| `com.pousadamarealta.utils` | 23 de 30 | 14 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.ReservationSystemTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousadamarealta/ReservationSystemTest.java
```

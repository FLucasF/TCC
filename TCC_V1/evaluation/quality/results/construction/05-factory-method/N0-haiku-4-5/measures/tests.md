# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 4 testes rodaram (0 falhas, 0 erros, 0 pulados); 4 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 8 de 772 (1.0%)
- ramos (if/switch): 2 de 236 (0.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamareaita` | 0 de 29 | 0 de 8 |
| `com.pousadamareaita.api` | 0 de 234 | 0 de 70 |
| `com.pousadamareaita.database` | 0 de 24 | 0 de 4 |
| `com.pousadamareaita.exception` | 0 de 6 | 0 de 0 |
| `com.pousadamareaita.model` | 0 de 122 | 0 de 0 |
| `com.pousadamareaita.service` | 0 de 333 | 0 de 144 |
| `com.pousadamareaita.util` | 8 de 24 | 2 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamareaita.MoneyUtilsTest` | 3 | 0 | 0 |
| `com.pousadamareaita.ReservationServiceTest` | 1 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
3 testes  src/test/java/com/pousadamareaita/MoneyUtilsTest.java
1 testes  src/test/java/com/pousadamareaita/ReservationServiceTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 2 testes rodaram (0 falhas, 0 erros, 0 pulados); 2 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 10 de 781 (1.3%)
- ramos (if/switch): 10 de 196 (5.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 30 | 0 de 8 |
| `com.pousada.database` | 0 de 31 | 0 de 4 |
| `com.pousada.domain` | 0 de 115 | 0 de 0 |
| `com.pousada.handler` | 0 de 272 | 0 de 64 |
| `com.pousada.service` | 0 de 313 | 0 de 110 |
| `com.pousada.util` | 10 de 20 | 10 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.ReservationTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
2 testes  src/test/java/com/pousada/ReservationTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 39 de 756 (5.2%)
- ramos (if/switch): 18 de 166 (10.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 26 | 0 de 2 |
| `com.pousada.database` | 0 de 22 | 0 de 2 |
| `com.pousada.dto` | 0 de 47 | 0 de 0 |
| `com.pousada.models` | 0 de 31 | 0 de 0 |
| `com.pousada.routes` | 0 de 235 | 0 de 72 |
| `com.pousada.services` | 0 de 346 | 0 de 68 |
| `com.pousada.utils` | 39 de 49 | 18 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.ReservationTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousada/ReservationTest.java
```

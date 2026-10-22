# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 4 testes rodaram (0 falhas, 0 erros, 0 pulados); 4 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 33 de 721 (4.6%)
- ramos (if/switch): 8 de 162 (4.9%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 20 | 0 de 0 |
| `com.pousada.api` | 0 de 254 | 0 de 54 |
| `com.pousada.database` | 0 de 232 | 0 de 40 |
| `com.pousada.event` | 0 de 26 | 0 de 4 |
| `com.pousada.event.listener` | 0 de 50 | 0 de 12 |
| `com.pousada.model` | 0 de 31 | 0 de 0 |
| `com.pousada.service` | 33 de 108 | 8 de 52 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.ReservationTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/com/pousada/ReservationTest.java
```

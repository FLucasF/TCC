# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 79 de 825 (9.6%)
- ramos (if/switch): 19 de 154 (12.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 86 | 0 de 2 |
| `com.pousadamarealta.api` | 0 de 6 | 0 de 0 |
| `com.pousadamarealta.api.controller` | 0 de 101 | 0 de 20 |
| `com.pousadamarealta.api.dto` | 0 de 91 | 0 de 0 |
| `com.pousadamarealta.config` | 0 de 11 | 0 de 8 |
| `com.pousadamarealta.domain.event` | 0 de 27 | 0 de 4 |
| `com.pousadamarealta.domain.event.listeners` | 0 de 58 | 0 de 8 |
| `com.pousadamarealta.domain.model` | 79 de 126 | 19 de 38 |
| `com.pousadamarealta.domain.service` | 0 de 146 | 0 de 32 |
| `com.pousadamarealta.repository` | 0 de 173 | 0 de 42 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.domain.model.ReservationTest` | 8 | 0 | 0 |
| `com.pousadamarealta.domain.model.RoomTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/domain/model/ReservationTest.java
3 testes  src/test/java/com/pousadamarealta/domain/model/RoomTest.java
```

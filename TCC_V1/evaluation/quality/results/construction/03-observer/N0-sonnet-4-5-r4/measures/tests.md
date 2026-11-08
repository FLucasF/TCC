# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 11 testes rodaram (0 falhas, 0 erros, 0 pulados); 11 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 615 de 817 (75.3%)
- ramos (if/switch): 88 de 142 (62.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 53 | 0 de 0 |
| `com.pousadamarealta.api` | 71 de 145 | 12 de 28 |
| `com.pousadamarealta.api.dto` | 33 de 46 | 0 de 0 |
| `com.pousadamarealta.config` | 18 de 18 | 1 de 2 |
| `com.pousadamarealta.domain.event` | 12 de 12 | 3 de 4 |
| `com.pousadamarealta.domain.event.handlers` | 58 de 60 | 7 de 10 |
| `com.pousadamarealta.domain.model` | 102 de 102 | 0 de 0 |
| `com.pousadamarealta.domain.service` | 129 de 179 | 29 de 46 |
| `com.pousadamarealta.domain.util` | 33 de 38 | 6 de 8 |
| `com.pousadamarealta.repository` | 159 de 164 | 30 de 44 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.ReservationSystemIT` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/pousadamarealta/ReservationSystemIT.java
```

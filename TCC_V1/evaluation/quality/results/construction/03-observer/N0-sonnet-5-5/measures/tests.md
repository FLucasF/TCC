# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 215 de 481 (44.7%)
- ramos (if/switch): 96 de 140 (68.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 1 de 7 | 0 de 4 |
| `br.com.marealta.domain` | 65 de 66 | 32 de 36 |
| `br.com.marealta.event` | 5 de 5 | 0 de 0 |
| `br.com.marealta.notify` | 25 de 25 | 12 de 12 |
| `br.com.marealta.service` | 95 de 96 | 40 de 42 |
| `br.com.marealta.store` | 24 de 161 | 12 de 24 |
| `br.com.marealta.web` | 0 de 121 | 0 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.MoneyTest` | 2 | 0 | 0 |
| `br.com.marealta.ReservationFlowTest` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/ReservationFlowTest.java
2 testes  src/test/java/br/com/marealta/MoneyTest.java
```

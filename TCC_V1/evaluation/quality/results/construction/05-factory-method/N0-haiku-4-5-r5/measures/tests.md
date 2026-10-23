# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 27 de 780 (3.5%)
- ramos (if/switch): 14 de 182 (7.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealta` | 0 de 211 | 0 de 56 |
| `com.pousada.marealta.config` | 0 de 42 | 0 de 16 |
| `com.pousada.marealta.dto` | 0 de 24 | 0 de 0 |
| `com.pousada.marealta.model` | 0 de 114 | 0 de 0 |
| `com.pousada.marealta.repository` | 0 de 134 | 0 de 22 |
| `com.pousada.marealta.service` | 27 de 255 | 14 de 88 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealta.ReservationIntegrationTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousada/marealta/ReservationIntegrationTest.java
```

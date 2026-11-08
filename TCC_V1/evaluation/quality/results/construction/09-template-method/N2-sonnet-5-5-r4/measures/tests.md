# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 321 de 456 (70.4%)
- ramos (if/switch): 47 de 76 (61.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 5 de 11 | 0 de 0 |
| `com.pousada.domain` | 135 de 137 | 24 de 32 |
| `com.pousada.report` | 88 de 88 | 6 de 6 |
| `com.pousada.repository` | 0 de 113 | 0 de 10 |
| `com.pousada.service` | 35 de 36 | 7 de 8 |
| `com.pousada.web` | 58 de 71 | 10 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.ApiTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousada/ApiTest.java
0 testes  src/test/java/com/pousada/InMemoryReservationRepository.java
0 testes  src/test/java/com/pousada/InMemoryMessageRepository.java
```

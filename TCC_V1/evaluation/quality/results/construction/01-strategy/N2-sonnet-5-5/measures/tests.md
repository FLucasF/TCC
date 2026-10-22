# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 68 de 414 (16.4%)
- ramos (if/switch): 21 de 104 (20.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 20 | 0 de 6 |
| `com.pousada.domain` | 10 de 31 | 7 de 22 |
| `com.pousada.error` | 5 de 12 | 0 de 0 |
| `com.pousada.persistence` | 0 de 126 | 0 de 16 |
| `com.pousada.plan` | 53 de 54 | 14 de 14 |
| `com.pousada.service` | 0 de 102 | 0 de 20 |
| `com.pousada.web` | 0 de 69 | 0 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.plan.RatePlansTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousada/plan/RatePlansTest.java
```

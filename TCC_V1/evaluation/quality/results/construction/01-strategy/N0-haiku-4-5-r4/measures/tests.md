# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 3 testes rodaram (0 falhas, 0 erros, 0 pulados); 3 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 6 de 696 (0.9%)
- ramos (if/switch): 0 de 185 (0.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 22 | 0 de 4 |
| `com.pousada.api` | 0 de 235 | 0 de 41 |
| `com.pousada.db` | 0 de 39 | 0 de 10 |
| `com.pousada.entity` | 0 de 26 | 0 de 0 |
| `com.pousada.rateplan` | 0 de 108 | 0 de 44 |
| `com.pousada.service` | 0 de 243 | 0 de 74 |
| `com.pousada.util` | 6 de 23 | 0 de 12 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.MoneyTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
3 testes  src/test/java/com/pousada/MoneyTest.java
```

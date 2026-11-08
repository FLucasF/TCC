# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 73 de 870 (8.4%)
- ramos (if/switch): 26 de 152 (17.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada.marealto` | 0 de 37 | 0 de 0 |
| `com.pousada.marealto.api` | 0 de 214 | 0 de 20 |
| `com.pousada.marealto.database` | 0 de 20 | 0 de 0 |
| `com.pousada.marealto.domain` | 9 de 103 | 0 de 10 |
| `com.pousada.marealto.messaging` | 0 de 25 | 0 de 0 |
| `com.pousada.marealto.rateplan` | 64 de 90 | 26 de 32 |
| `com.pousada.marealto.repository` | 0 de 216 | 0 de 46 |
| `com.pousada.marealto.service` | 0 de 165 | 0 de 44 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.marealto.rateplan.RatePlanTest` | 9 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
9 testes  src/test/java/com/pousada/marealto/rateplan/RatePlanTest.java
```

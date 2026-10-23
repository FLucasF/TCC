# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 1 testes rodaram (0 falhas, 1 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 0 de 820 (0.0%)
- ramos (if/switch): 0 de 154 (0.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 43 | 0 de 0 |
| `com.pousada.api` | 0 de 189 | 0 de 24 |
| `com.pousada.domain` | 0 de 131 | 0 de 34 |
| `com.pousada.infrastructure` | 0 de 28 | 0 de 2 |
| `com.pousada.messaging` | 0 de 82 | 0 de 30 |
| `com.pousada.repository` | 0 de 185 | 0 de 38 |
| `com.pousada.service` | 0 de 162 | 0 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.IntegrationTest` | 1 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   IntegrationTest » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/com/pousada/IntegrationTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 1 testes rodaram (0 falhas, 1 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 0 de 752 (0.0%)
- ramos (if/switch): 0 de 130 (0.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 18 | 0 de 0 |
| `com.pousada.api` | 0 de 169 | 0 de 16 |
| `com.pousada.domain` | 0 de 149 | 0 de 28 |
| `com.pousada.domain.rateplan` | 0 de 90 | 0 de 32 |
| `com.pousada.repository` | 0 de 178 | 0 de 24 |
| `com.pousada.service` | 0 de 148 | 0 de 30 |

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
- com Thread.sleep: 1

Os arquivos:
```
16 testes  src/test/java/com/pousada/IntegrationTest.java
```

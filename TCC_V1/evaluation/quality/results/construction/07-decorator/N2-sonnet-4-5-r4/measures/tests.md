# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 1 testes rodaram (0 falhas, 1 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 0 de 832 (0.0%)
- ramos (if/switch): 0 de 176 (0.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 60 | 0 de 12 |
| `com.pousadamarealta.controller` | 0 de 112 | 0 de 2 |
| `com.pousadamarealta.domain` | 0 de 153 | 0 de 28 |
| `com.pousadamarealta.dto` | 0 de 53 | 0 de 4 |
| `com.pousadamarealta.messaging` | 0 de 62 | 0 de 22 |
| `com.pousadamarealta.repository` | 0 de 217 | 0 de 52 |
| `com.pousadamarealta.service` | 0 de 146 | 0 de 20 |
| `com.pousadamarealta.util` | 0 de 29 | 0 de 36 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.IntegrationTest` | 1 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   IntegrationTest » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 1

Os arquivos:
```
8 testes  src/test/java/com/pousadamarealta/IntegrationTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 1 testes rodaram (0 falhas, 1 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 0 de 802 (0.0%)
- ramos (if/switch): 0 de 146 (0.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 47 | 0 de 0 |
| `com.pousadamarealta.config` | 0 de 26 | 0 de 2 |
| `com.pousadamarealta.controller` | 0 de 200 | 0 de 30 |
| `com.pousadamarealta.domain` | 0 de 96 | 0 de 0 |
| `com.pousadamarealta.event` | 0 de 12 | 0 de 4 |
| `com.pousadamarealta.notification` | 0 de 59 | 0 de 8 |
| `com.pousadamarealta.repository` | 0 de 181 | 0 de 42 |
| `com.pousadamarealta.service` | 0 de 146 | 0 de 48 |
| `com.pousadamarealta.util` | 0 de 35 | 0 de 12 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.SystemIT` | 1 | 1 | 0 |

## Os testes que falharam

```
[ERROR]   SystemIT » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/com/pousadamarealta/SystemIT.java
```

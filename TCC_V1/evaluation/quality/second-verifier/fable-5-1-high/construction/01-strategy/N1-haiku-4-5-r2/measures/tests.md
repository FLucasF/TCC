# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 3 testes rodaram (0 falhas, 0 erros, 0 pulados); 3 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 5 de 687 (0.7%)
- ramos (if/switch): 0 de 138 (0.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 154 | 0 de 26 |
| `com.pousada.api.dto` | 0 de 52 | 0 de 2 |
| `com.pousada.domain` | 5 de 67 | 0 de 12 |
| `com.pousada.domain.rateplans` | 0 de 94 | 0 de 18 |
| `com.pousada.infra` | 0 de 171 | 0 de 30 |
| `com.pousada.service` | 0 de 149 | 0 de 50 |

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

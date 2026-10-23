# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 18 de 666 (2.7%)
- ramos (if/switch): 14 de 154 (9.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 15 | 0 de 2 |
| `com.pousada.api` | 0 de 201 | 0 de 48 |
| `com.pousada.db` | 0 de 13 | 0 de 0 |
| `com.pousada.model` | 0 de 57 | 0 de 0 |
| `com.pousada.repository` | 0 de 187 | 0 de 22 |
| `com.pousada.service` | 0 de 125 | 0 de 56 |
| `com.pousada.util` | 18 de 68 | 14 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.PricingTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/com/pousada/PricingTest.java
```

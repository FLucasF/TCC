# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 86 de 798 (10.8%)
- ramos (if/switch): 22 de 160 (13.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 21 | 0 de 0 |
| `com.pousada.api` | 0 de 270 | 0 de 42 |
| `com.pousada.db` | 0 de 48 | 0 de 16 |
| `com.pousada.domain` | 10 de 64 | 0 de 0 |
| `com.pousada.rateplan` | 63 de 77 | 17 de 22 |
| `com.pousada.repository` | 0 de 130 | 0 de 22 |
| `com.pousada.service` | 0 de 149 | 0 de 40 |
| `com.pousada.utils` | 13 de 39 | 5 de 18 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.RatePlanIT` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/com/pousada/RatePlanIT.java
```

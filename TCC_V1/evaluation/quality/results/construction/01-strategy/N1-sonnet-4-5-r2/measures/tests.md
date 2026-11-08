# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 95 de 794 (12.0%)
- ramos (if/switch): 31 de 145 (21.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.maralta` | 0 de 41 | 0 de 4 |
| `br.com.maralta.controller` | 0 de 150 | 0 de 16 |
| `br.com.maralta.controller.dto` | 0 de 46 | 0 de 0 |
| `br.com.maralta.database` | 0 de 41 | 0 de 12 |
| `br.com.maralta.domain` | 9 de 112 | 0 de 0 |
| `br.com.maralta.domain.rateplan` | 86 de 105 | 31 de 51 |
| `br.com.maralta.repository` | 0 de 173 | 0 de 30 |
| `br.com.maralta.service` | 0 de 126 | 0 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.maralta.RatePlanTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/maralta/RatePlanTest.java
```

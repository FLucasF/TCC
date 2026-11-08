# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 83 de 912 (9.1%)
- ramos (if/switch): 12 de 144 (8.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamarealta` | 0 de 40 | 0 de 0 |
| `br.com.pousadamarealta.api` | 0 de 38 | 0 de 0 |
| `br.com.pousadamarealta.api.controller` | 0 de 186 | 0 de 34 |
| `br.com.pousadamarealta.api.dto` | 0 de 46 | 0 de 0 |
| `br.com.pousadamarealta.domain.exception` | 2 de 12 | 0 de 0 |
| `br.com.pousadamarealta.domain.model` | 22 de 117 | 0 de 0 |
| `br.com.pousadamarealta.domain.rateplan` | 59 de 107 | 12 de 38 |
| `br.com.pousadamarealta.domain.repository` | 0 de 185 | 0 de 32 |
| `br.com.pousadamarealta.domain.service` | 0 de 151 | 0 de 36 |
| `br.com.pousadamarealta.infrastructure.database` | 0 de 30 | 0 de 4 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamarealta.RatePlanTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/br/com/pousadamarealta/RatePlanTest.java
```

# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 1 erros, 0 pulados); 29 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 15 de 782 (1.9%)
- ramos (if/switch): 12 de 184 (6.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealte` | 0 de 8 | 0 de 0 |
| `br.com.marealte.controller` | 0 de 222 | 0 de 96 |
| `br.com.marealte.database` | 0 de 33 | 0 de 2 |
| `br.com.marealte.dto` | 0 de 33 | 0 de 0 |
| `br.com.marealte.model` | 0 de 121 | 0 de 0 |
| `br.com.marealte.repository` | 0 de 204 | 0 de 48 |
| `br.com.marealte.routes` | 0 de 20 | 0 de 0 |
| `br.com.marealte.service` | 15 de 141 | 12 de 38 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealte.PousadaIT` | 1 | 1 | 0 |
| `br.com.marealte.service.PricingServiceTest` | 9 | 0 | 0 |

## Os testes que falharam

```
[ERROR]   PousadaIT » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 1

Os arquivos:
```
20 testes  src/test/java/br/com/marealte/PousadaIT.java
9 testes  src/test/java/br/com/marealte/service/PricingServiceTest.java
```

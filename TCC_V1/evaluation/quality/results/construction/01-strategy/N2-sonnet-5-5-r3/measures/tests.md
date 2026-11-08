# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 84 de 374 (22.5%)
- ramos (if/switch): 23 de 108 (21.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 5 de 22 | 0 de 4 |
| `br.com.marealta.domain` | 46 de 62 | 13 de 38 |
| `br.com.marealta.infra` | 0 de 105 | 0 de 14 |
| `br.com.marealta.plan` | 33 de 48 | 10 de 12 |
| `br.com.marealta.service` | 0 de 77 | 0 de 14 |
| `br.com.marealta.web` | 0 de 60 | 0 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.PlansTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/PlansTest.java
```

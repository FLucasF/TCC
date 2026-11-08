# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 9 testes rodaram (0 falhas, 0 erros, 0 pulados); 9 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 456 de 493 (92.5%)
- ramos (if/switch): 79 de 102 (77.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 3 de 9 | 0 de 0 |
| `br.com.marealta.domain` | 128 de 130 | 28 de 36 |
| `br.com.marealta.persistence` | 100 de 110 | 13 de 14 |
| `br.com.marealta.plan` | 49 de 51 | 15 de 16 |
| `br.com.marealta.service` | 67 de 68 | 9 de 10 |
| `br.com.marealta.web` | 109 de 125 | 14 de 26 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 1 | 0 | 0 |
| `br.com.marealta.PlansTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/marealta/PlansTest.java
1 testes  src/test/java/br/com/marealta/ApiTest.java
```

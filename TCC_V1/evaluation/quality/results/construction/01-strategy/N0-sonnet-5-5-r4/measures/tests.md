# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 8 testes rodaram (0 falhas, 0 erros, 0 pulados); 8 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 85 de 447 (19.0%)
- ramos (if/switch): 26 de 122 (21.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 33 de 269 | 3 de 60 |
| `br.com.marealta.plan` | 52 de 55 | 23 de 24 |
| `br.com.marealta.web` | 0 de 123 | 0 de 38 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.PlansTest` | 8 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/marealta/PlansTest.java
```

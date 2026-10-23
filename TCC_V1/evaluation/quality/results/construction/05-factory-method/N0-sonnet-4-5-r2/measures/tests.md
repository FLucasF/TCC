# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 7 testes rodaram (0 falhas, 0 erros, 0 pulados); 7 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 496 de 829 (59.8%)
- ramos (if/switch): 73 de 216 (33.8%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 0 de 64 | 0 de 10 |
| `br.com.marealta.controller` | 59 de 128 | 22 de 70 |
| `br.com.marealta.database` | 7 de 10 | 0 de 0 |
| `br.com.marealta.dto` | 38 de 60 | 6 de 25 |
| `br.com.marealta.exception` | 10 de 22 | 0 de 0 |
| `br.com.marealta.model` | 107 de 109 | 0 de 0 |
| `br.com.marealta.repository` | 154 de 206 | 22 de 46 |
| `br.com.marealta.service` | 121 de 230 | 23 de 65 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.PousadaSystemIT` | 7 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/br/com/marealta/PousadaSystemIT.java
```

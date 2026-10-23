# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 12 testes rodaram (0 falhas, 0 erros, 0 pulados); 12 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 434 de 466 (93.1%)
- ramos (if/switch): 80 de 103 (77.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta` | 86 de 96 | 10 de 16 |
| `br.com.marealta.domain` | 94 de 94 | 25 de 30 |
| `br.com.marealta.messaging` | 66 de 67 | 21 de 23 |
| `br.com.marealta.persistence` | 134 de 144 | 13 de 14 |
| `br.com.marealta.web` | 54 de 65 | 11 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.ApiTest` | 2 | 0 | 0 |
| `br.com.marealta.DomainTest` | 6 | 0 | 0 |
| `br.com.marealta.MessagePipelineTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/marealta/DomainTest.java
4 testes  src/test/java/br/com/marealta/MessagePipelineTest.java
2 testes  src/test/java/br/com/marealta/ApiTest.java
```

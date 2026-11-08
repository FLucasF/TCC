# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 442 de 484 (91.3%)
- ramos (if/switch): 74 de 96 (77.1%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.marealta.pousada` | 13 de 22 | 0 de 8 |
| `br.com.marealta.pousada.domain` | 138 de 139 | 35 de 38 |
| `br.com.marealta.pousada.notification` | 35 de 36 | 6 de 6 |
| `br.com.marealta.pousada.persistence` | 134 de 152 | 11 de 12 |
| `br.com.marealta.pousada.service` | 59 de 60 | 9 de 10 |
| `br.com.marealta.pousada.web` | 63 de 75 | 13 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.marealta.pousada.DomainTest` | 8 | 0 | 0 |
| `br.com.marealta.pousada.EndToEndTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
8 testes  src/test/java/br/com/marealta/pousada/DomainTest.java
2 testes  src/test/java/br/com/marealta/pousada/EndToEndTest.java
```

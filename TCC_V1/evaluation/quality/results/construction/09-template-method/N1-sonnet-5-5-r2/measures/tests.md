# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 515 de 559 (92.1%)
- ramos (if/switch): 66 de 86 (76.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 14 | 0 de 8 |
| `pousada.domain` | 143 de 143 | 26 de 28 |
| `pousada.persistence` | 133 de 153 | 6 de 6 |
| `pousada.report` | 106 de 107 | 11 de 14 |
| `pousada.service` | 55 de 55 | 8 de 8 |
| `pousada.web` | 78 de 87 | 15 de 22 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.ApiTest` | 4 | 0 | 0 |
| `pousada.DomainTest` | 7 | 0 | 0 |
| `pousada.ReportsTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
7 testes  src/test/java/pousada/DomainTest.java
5 testes  src/test/java/pousada/ReportsTest.java
4 testes  src/test/java/pousada/ApiTest.java
```

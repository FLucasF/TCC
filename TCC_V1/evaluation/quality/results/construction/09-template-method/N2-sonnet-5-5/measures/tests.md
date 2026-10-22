# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 2 testes rodaram (0 falhas, 0 erros, 0 pulados); 2 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 456 de 495 (92.1%)
- ramos (if/switch): 68 de 104 (65.4%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 11 de 17 | 0 de 4 |
| `pousada.db` | 111 de 127 | 10 de 14 |
| `pousada.domain` | 145 de 147 | 33 de 48 |
| `pousada.report` | 86 de 89 | 9 de 12 |
| `pousada.service` | 46 de 46 | 6 de 6 |
| `pousada.web` | 57 de 69 | 10 de 20 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.SystemTest` | 2 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 1
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
2 testes  src/test/java/pousada/SystemTest.java
```

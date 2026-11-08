# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 0 erros, 0 pulados); 6 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 82 de 465 (17.6%)
- ramos (if/switch): 27 de 114 (23.7%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.mare` | 0 de 9 | 0 de 4 |
| `br.com.mare.domain` | 33 de 185 | 15 de 48 |
| `br.com.mare.infra` | 0 de 109 | 0 de 14 |
| `br.com.mare.plan` | 49 de 51 | 12 de 14 |
| `br.com.mare.web` | 0 de 111 | 0 de 34 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.mare.PricingTest` | 6 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
6 testes  src/test/java/br/com/mare/PricingTest.java
```

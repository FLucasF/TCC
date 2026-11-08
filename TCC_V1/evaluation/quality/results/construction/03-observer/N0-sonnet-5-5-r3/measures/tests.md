# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 461 de 492 (93.7%)
- ramos (if/switch): 102 de 124 (82.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamarealta.reservas` | 7 de 16 | 0 de 8 |
| `br.com.pousadamarealta.reservas.domain` | 89 de 90 | 37 de 40 |
| `br.com.pousadamarealta.reservas.notification` | 47 de 47 | 15 de 16 |
| `br.com.pousadamarealta.reservas.persistence` | 137 de 150 | 12 de 14 |
| `br.com.pousadamarealta.reservas.service` | 92 de 93 | 14 de 14 |
| `br.com.pousadamarealta.reservas.web` | 89 de 96 | 24 de 32 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamarealta.reservas.ApiTest` | 6 | 0 | 0 |
| `br.com.pousadamarealta.reservas.DomainTest` | 10 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
10 testes  src/test/java/br/com/pousadamarealta/reservas/DomainTest.java
6 testes  src/test/java/br/com/pousadamarealta/reservas/ApiTest.java
```

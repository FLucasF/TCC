# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 13 testes rodaram (0 falhas, 1 erros, 0 pulados); 26 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 85 de 757 (11.2%)
- ramos (if/switch): 23 de 150 (15.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamarealta` | 0 de 63 | 0 de 4 |
| `br.com.pousadamarealta.api` | 0 de 155 | 0 de 18 |
| `br.com.pousadamarealta.api.dto` | 0 de 46 | 0 de 0 |
| `br.com.pousadamarealta.domain` | 85 de 121 | 23 de 28 |
| `br.com.pousadamarealta.repository` | 0 de 191 | 0 de 60 |
| `br.com.pousadamarealta.service` | 0 de 106 | 0 de 30 |
| `br.com.pousadamarealta.service.observer` | 0 de 75 | 0 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamarealta.ReservationSystemIT` | 1 | 1 | 0 |
| `br.com.pousadamarealta.ReservationTest` | 12 | 0 | 0 |

## Os testes que falharam

```
[ERROR]   ReservationSystemIT » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 1

Os arquivos:
```
14 testes  src/test/java/br/com/pousadamarealta/ReservationSystemIT.java
12 testes  src/test/java/br/com/pousadamarealta/ReservationTest.java
```

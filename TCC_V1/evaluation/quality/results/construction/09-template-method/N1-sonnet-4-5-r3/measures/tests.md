# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 6 testes rodaram (0 falhas, 1 erros, 0 pulados); 21 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 58 de 936 (6.2%)
- ramos (if/switch): 14 de 140 (10.0%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `pousada` | 0 de 41 | 0 de 0 |
| `pousada.config` | 0 de 24 | 0 de 4 |
| `pousada.controller` | 0 de 253 | 0 de 26 |
| `pousada.domain` | 17 de 105 | 0 de 8 |
| `pousada.dto` | 0 de 35 | 0 de 0 |
| `pousada.pricing` | 41 de 42 | 14 de 16 |
| `pousada.report` | 0 de 118 | 0 de 12 |
| `pousada.repository` | 0 de 195 | 0 de 44 |
| `pousada.service` | 0 de 123 | 0 de 30 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `pousada.IntegrationTest` | 1 | 1 | 0 |
| `pousada.PricingCalculatorTest` | 5 | 0 | 0 |

## Os testes que falharam

```
[ERROR]   IntegrationTest » IllegalState Could not find a valid Docker environment. Please see logs and check configuration
```

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 1
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
16 testes  src/test/java/pousada/IntegrationTest.java
5 testes  src/test/java/pousada/PricingCalculatorTest.java
```

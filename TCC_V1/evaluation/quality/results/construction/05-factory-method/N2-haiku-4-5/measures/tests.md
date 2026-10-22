# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 4 testes rodaram (0 falhas, 0 erros, 0 pulados); 4 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 45 de 914 (4.9%)
- ramos (if/switch): 9 de 212 (4.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `br.com.pousadamareal` | 0 de 35 | 0 de 8 |
| `br.com.pousadamareal.api.dto` | 0 de 59 | 0 de 8 |
| `br.com.pousadamareal.api.handlers` | 0 de 197 | 0 de 50 |
| `br.com.pousadamareal.db` | 0 de 34 | 0 de 6 |
| `br.com.pousadamareal.domain` | 8 de 188 | 0 de 10 |
| `br.com.pousadamareal.repository` | 0 de 150 | 0 de 34 |
| `br.com.pousadamareal.service` | 25 de 230 | 4 de 86 |
| `br.com.pousadamareal.util` | 12 de 21 | 5 de 10 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `br.com.pousadamareal.service.PricingServiceTest` | 4 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
4 testes  src/test/java/br/com/pousadamareal/service/PricingServiceTest.java
```

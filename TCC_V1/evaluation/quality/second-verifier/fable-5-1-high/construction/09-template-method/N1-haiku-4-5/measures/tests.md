# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 5 testes rodaram (0 falhas, 0 erros, 0 pulados); 5 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 15 de 809 (1.9%)
- ramos (if/switch): 9 de 200 (4.5%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 45 | 0 de 4 |
| `com.pousada.api` | 0 de 203 | 0 de 44 |
| `com.pousada.business` | 0 de 99 | 0 de 44 |
| `com.pousada.db` | 0 de 180 | 0 de 44 |
| `com.pousada.models` | 0 de 96 | 0 de 12 |
| `com.pousada.reports` | 0 de 153 | 0 de 28 |
| `com.pousada.utils` | 15 de 33 | 9 de 24 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.PricingUtilsTest` | 5 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 1
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousada/PricingUtilsTest.java
```

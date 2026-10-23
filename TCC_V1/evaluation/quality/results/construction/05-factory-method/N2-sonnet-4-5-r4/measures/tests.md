# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 16 testes rodaram (0 falhas, 0 erros, 0 pulados); 16 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 196 de 819 (23.9%)
- ramos (if/switch): 41 de 176 (23.3%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousadamarealta` | 0 de 43 | 0 de 8 |
| `com.pousadamarealta.document` | 102 de 105 | 18 de 22 |
| `com.pousadamarealta.domain` | 94 de 131 | 23 de 30 |
| `com.pousadamarealta.repository` | 0 de 223 | 0 de 42 |
| `com.pousadamarealta.service` | 0 de 118 | 0 de 32 |
| `com.pousadamarealta.web` | 0 de 199 | 0 de 42 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousadamarealta.document.DocumentGenerationTest` | 5 | 0 | 0 |
| `com.pousadamarealta.domain.ReservationTest` | 11 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
11 testes  src/test/java/com/pousadamarealta/domain/ReservationTest.java
5 testes  src/test/java/com/pousadamarealta/document/DocumentGenerationTest.java
```

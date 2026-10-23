# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 10 testes rodaram (0 falhas, 0 erros, 0 pulados); 10 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 97 de 704 (13.8%)
- ramos (if/switch): 43 de 164 (26.2%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `com.pousada` | 0 de 20 | 0 de 8 |
| `com.pousada.api` | 0 de 186 | 0 de 18 |
| `com.pousada.db` | 0 de 143 | 0 de 28 |
| `com.pousada.models` | 16 de 118 | 0 de 8 |
| `com.pousada.services` | 27 de 181 | 9 de 66 |
| `com.pousada.utils` | 54 de 56 | 34 de 36 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `com.pousada.EmailValidatorTest` | 2 | 0 | 0 |
| `com.pousada.MessageServiceTest` | 5 | 0 | 0 |
| `com.pousada.PriceCalculatorTest` | 3 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 3
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 0
- contra o banco (DataSource, JDBC, Testcontainers): 0
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
5 testes  src/test/java/com/pousada/MessageServiceTest.java
3 testes  src/test/java/com/pousada/PriceCalculatorTest.java
2 testes  src/test/java/com/pousada/EmailValidatorTest.java
```

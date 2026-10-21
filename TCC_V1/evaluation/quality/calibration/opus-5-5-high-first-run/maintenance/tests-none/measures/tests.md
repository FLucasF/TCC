# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)

Resultado: medido. 1 testes rodaram (0 falhas, 0 erros, 0 pulados); 1 métodos de teste escritos no código.

## A cobertura (JaCoCo)

- linhas: 110 de 583 (18.9%)
- ramos (if/switch): 4 de 152 (2.6%)

| pacote | linhas cobertas | ramos cobertos |
|---|---|---|
| `hotel` | 25 de 30 | 2 de 4 |
| `hotel.documents` | 7 de 87 | 0 de 16 |
| `hotel.messages` | 6 de 33 | 0 de 2 |
| `hotel.reports` | 7 de 48 | 0 de 22 |
| `hotel.reservations` | 13 de 233 | 0 de 56 |
| `hotel.rooms` | 20 de 29 | 2 de 4 |
| `hotel.web` | 32 de 123 | 0 de 48 |

## Os testes por classe

| classe de teste | testes | falhas + erros | pulados |
|---|---|---|---|
| `hotel.HotelSmokeTest` | 1 | 0 | 0 |

## Os tipos de teste (pelo código dos testes)

- arquivos de teste: 2
- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): 1
- contra o banco (DataSource, JDBC, Testcontainers): 2
- com H2 ou outro banco em memória: 0
- com mocks (Mockito, mock(...)): 0
- com Thread.sleep: 0

Os arquivos:
```
1 testes  src/test/java/hotel/HotelSmokeTest.java
0 testes  src/test/java/hotel/EmptyDatabase.java
```

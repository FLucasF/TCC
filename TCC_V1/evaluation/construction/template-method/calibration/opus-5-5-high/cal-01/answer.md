# Cenário 09

## Visão geral

O agente montou um sistema enxuto e correto, mas deixou os quatro relatórios sem o pattern. `OccupancyReport`, `RevenueReport`, `TourismTaxReport` e `RatePlanReport` são quatro classes `final` independentes. Cada uma repete os mesmos passos dentro do seu `generate`: filtrar, ordenar, escrever a linha de título, o cabeçalho, as linhas e o total. A linha de título "Pousada Maré Alta,…" está copiada quatro vezes. O filtro do financeiro também está copiado inteiro no relatório por plano. Não há esqueleto comum, nem classe abstrata, nem escritor genérico. Os próximos relatórios que o contador já anunciou (cancelamentos, hóspedes por estado) vão ser uma quinta e uma sexta cópia. Não há estrutura sobrando. O comportamento confere em tudo o que testei, e os 28 testes do agente passam.

## Como foi implementado, e por que o julgamento

**O projeto.** O código está no pacote `hotel`, dividido assim:
- `rooms`: os cinco quartos fixos em `Rooms`, `Room` e `RoomType`.
- `reservations`:
  - `Reservation`, a entidade com o estado e o dinheiro (`isPaid()` em `Reservation.java:80`, `amountKept()` em `Reservation.java:87`);
  - `ReservationService`, com o ciclo de vida, as recusas e o e-mail de confirmação;
  - `PriceCalculator`, com a alta temporada, o desconto e o arredondamento HALF_EVEN;
  - `ReservationStore`, um `LinkedHashMap` em memória;
  - `ErrorCode` e `HotelException`.
- `documents`: o comprovante (`ReservationReceipt`) e a formatação em reais e datas (`TextFormat`).
- `messages`: `EmailSender` e `SentMessages`, a lista de enviadas.
- `reports`: os quatro relatórios.
- `web`: `HttpApi`, com as rotas Javalin, a leitura e validação dos parâmetros, e o mapeamento de `ErrorCode` para status HTTP em `HttpApi.java:116-123`.

**O caminho de uma requisição e a montagem.** A rota em `HttpApi` lê e valida a entrada, chama `ReservationService` ou o relatório, e devolve JSON ou CSV. Uma `HotelException` vira `{error, message}` com o status certo. `Hotel` monta tudo à mão (`Hotel.java:21-30`) e sobe na porta `PORT`, ou 7070 se ela não vier (`Hotel.java:44`). Os testes do agente são JUnit em quatro classes, 28 casos no total: preço, serviço, relatórios e um teste HTTP de ponta a ponta.

**A parte que pede o pattern.** Os quatro relatórios não têm nenhum tipo em comum. Cada `generate` tem a sequência inteira escrita à mão:
- **Ocupação** (`OccupancyReport.java:23-46`): o laço de filtro está em 24-30, a ordenação em 31, o título em 34, o cabeçalho em 35, as linhas em 36-43 e o total em 44.
- **Financeiro** (`RevenueReport.java:23-49`): o filtro está em 24-31, a ordenação em 32, o título em 35, o cabeçalho em 36, as linhas e a soma em 38-46 e o total em 47.
- **Taxa de turismo** (`TourismTaxReport.java:22-50`): o filtro está em 23-29, a ordenação em 30 (copiada do financeiro), o título em 33, o cabeçalho em 34, as linhas em 36-47 e o total em 48. A taxa é a constante `TAX_PER_GUEST_PER_NIGHT` (`TourismTaxReport.java:14`).
- **Por plano** (`RatePlanReport.java:20-48`): o filtro do financeiro está copiado linha a linha (21-28 contra `RevenueReport.java:24-31`), o título em 31, o cabeçalho em 32, uma linha por `RatePlan.values()` em 34-45 e o total em 46.

`HttpApi` também recebe os quatro relatórios como tipos concretos distintos (`HttpApi.java:32-35`). Cada rota chama o `generate` do seu (`HttpApi.java:91-106`).

**O raciocínio.** O cenário pede o esqueleto (título, cabeçalho, linhas, total) num lugar só, e cada relatório preenchendo só os passos que mudam. Isso pode vir por herança (Template Method) ou por composição (um escritor genérico). O projeto não tem nenhuma das duas formas. Também não tem uma utilidade parcial de CSV que pudesse levar a `malfeito`. É exatamente o caso que as instruções chamam de `omissão`: os quatro relatórios escritos inteiros, cada um com a sua linha de título. Não há classe abstrata nem interface no código; `grep` por `interface|abstract` não acha nada.

**O exagero colateral.** Não há interface, fábrica nem builder. `EmailSender` é uma classe pequena que fixa o canal `"EMAIL"` (`EmailSender.java:12-14`), sem interface, e tem uma função real. Não pesa.

**O comportamento.** Os valores, as ordens, as colunas, os títulos, as recusas, o comprovante e o e-mail conferem com o enunciado nas chamadas que fiz.

## Item a item

**O pattern pedido.** Os quatro relatórios são classes independentes, cada uma com a sequência completa no seu `generate`: `OccupancyReport.java:23-46`, `RevenueReport.java:23-49`, `TourismTaxReport.java:22-50` e `RatePlanReport.java:20-48`. A linha de título está copiada em `OccupancyReport.java:34`, `RevenueReport.java:35`, `TourismTaxReport.java:33` e `RatePlanReport.java:31`. O filtro do financeiro está duplicado em `RatePlanReport.java:21-28`. A ordenação do financeiro está duplicada em `TourismTaxReport.java:30`. Não existe esqueleto comum, nem por herança nem por composição, então cada novo relatório pedido pelo contador será mais uma cópia. Não é o Template Method nem a alternativa aceita.
Conclusão: omissão

**O exagero colateral.** Não há. Não existe interface com uma implementação só, nem fábrica, nem builder, nem camada que só repassa. A montagem em `Hotel.java:21-30` é direta, e `EmailSender` (`EmailSender.java:4-15`) é uma classe concreta pequena com função real.
Conclusão: contenção

**O comportamento.** Testei numa cópia do projeto (`mvn -q package -DskipTests`, depois `java -jar target/hotel.jar` na porta 7070). Criei seis reservas em julho de 2027 e confirmei as quatro primeiras. Fiz o check-in da 1, cancelei a 3 (NON_REFUNDABLE confirmada, refund 0.00) e a 4 (STANDARD confirmada, refund 599.76). As reservas 5 e 6 ficaram pendentes.
- **Preços:** a 1 (STANDARD, 10/07 a 12/07) deu 599.76, como no exemplo do enunciado. A 2 (DELUXE, 3 noites, NON_REFUNDABLE) deu 1193.09: 1403.64 menos 210.55. A 3 deu 591.50.
- **Ocupação:** `GET /reports/occupancy?date=2027-07-10` devolveu `Pousada Maré Alta,occupancy,2027-07-10`, o cabeçalho `room,type,reservation,check_in,check_out`, a linha `101,STANDARD,1,…` e `total,1/5`.
- **Financeiro:** `GET /reports/revenue?month=2027-07` devolveu o título, as reservas 2, 1 e 3 (a não reembolsável cancelada conta, a STANDARD cancelada não) e `total,2384.35`.
- **Taxa de turismo:** `GET /reports/tourism-tax?month=2027-07` devolveu o título, as colunas pedidas, `2,…,3,3,40.50` e `1,…,2,2,18.00` (só as pagas, na ordem do financeiro, sem a cancelada) e `total,58.50`.
- **Por plano:** `GET /reports/rate-plans?month=2027-07` devolveu `STANDARD,1,599.76`, `NON_REFUNDABLE,2,1784.59` (a cancelada não reembolsável incluída) e `total,3,2384.35`. Em agosto, sem reservas, vieram as duas linhas zeradas e `total,0,0.00`.
- **Formato:** todas as respostas vieram como `text/csv; charset=utf-8`. `month=2027-7` e a falta de `month` deram 400 INVALID_REQUEST.
- **Parte comum:**
  - as recusas vieram como pedido: 409 ROOM_UNAVAILABLE, 422 CAPACITY_EXCEEDED, 400 para um corpo que não é JSON, 404 RESERVATION_NOT_FOUND, 409 OPERATION_NOT_ALLOWED ao confirmar uma reserva com check-in, e 409 para a segunda via de uma reserva pendente;
  - o check-out devolveu o comprovante com todos os campos;
  - o e-mail tem o assunto e o texto exatos. Com a reserva de Carla Dias numa noite NON_REFUNDABLE veio "1 diária" e "Desconto: R$ 58,48" (meio para o par sobre 58,485).
- **Testes do agente:** `mvn verify` deu 28 testes, 0 falhas, 0 erros, BUILD SUCCESS.
- **Fim:** o serviço foi derrubado e a porta 7070 não responde mais.
Conclusão: segue

## Dúvidas

Nenhuma.

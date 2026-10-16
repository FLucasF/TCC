# Pousada Maré Alta: o sistema de reservas

O serviço que a pousada usa para as reservas: o site cria e confirma as reservas, a recepção faz o
check-in e o check-out, e o contador tira os relatórios.

## Como rodar

```bash
mvn verify
```

```bash
java -jar target/hotel.jar
```

O serviço sobe na porta 7070 (ou na que estiver em `PORT`). Os dados ficam em memória: quando o
serviço reinicia, as reservas se perdem.

## Como funciona

- **Quartos:** cinco, de três tipos (`STANDARD`, `DELUXE`, `SUITE`), cada um com a sua capacidade e a
  sua diária.
- **Preço:** cada noite custa a diária do quarto. Na alta temporada (de 15 de dezembro ao fim de
  fevereiro, e o mês de julho inteiro), a noite custa 20% a mais. Em cima dessa soma vem o que o plano
  de tarifa da reserva disser. O dinheiro é arredondado para centavos, meio para o par, a cada passo.
- **Planos de tarifa:** cada plano tem o seu preço, as suas regras de quem pode reservar e a sua regra
  de devolução; a reserva que não cabe nas regras do plano é recusada com `INVALID_REQUEST`.
- **Reserva:** nasce `PENDING`. Quando o hóspede paga, é confirmada (`CONFIRMED`), e ele recebe o
  comprovante por e-mail. Depois vêm o check-in (`CHECKED_IN`) e o check-out (`CHECKED_OUT`), que
  entrega o comprovante final.
- **Cancelamento:** só antes do check-in. O que volta para o hóspede é o que o plano promete, sobre o
  que ele pagou (uma reserva `PENDING` nunca foi paga, então não volta nada).
- **Mensagens:** os e-mails enviados ficam guardados, e a recepção os consulta em `/messages`.
- **Relatórios (CSV):** a ocupação de uma noite, para a recepção, e o dinheiro que ficou com a pousada
  das reservas que começam num mês, para o contador.

## Os planos de tarifa

| plano | preço | quem pode | se cancelar, volta |
|---|---|---|---|
| `STANDARD` | as noites, sem desconto | qualquer reserva | tudo o que pagou |
| `NON_REFUNDABLE` | as noites, com 15% de desconto | qualquer reserva | nada |
| `CORPORATE` | as noites, com o desconto da empresa: `TECNOSUL` 12%, `AGROVALE` 18%, `FARMAVIDA` 25% | quem informa o `companyCode` de uma dessas empresas | tudo o que pagou |
| `WEEKEND_PACKAGE` | preço fechado, o ano inteiro: R$ 520,00 no standard, R$ 820,00 no deluxe, R$ 1.200,00 na suíte | entrada numa sexta e saída no domingo seguinte (duas noites) | metade do que pagou |
| `LONG_STAY` | as noites, com 10% de desconto | estadias de 7 noites ou mais | tudo o que pagou |

Cada plano é uma implementação de `RatePlanPolicy` (um arquivo por plano, em `hotel.reservations`),
ligada ao seu nome no enum `RatePlan`. Um plano novo é uma classe nova e uma linha no enum.

## A API

| | |
|---|---|
| `GET /rooms` | os quartos |
| `POST /reservations` | cria uma reserva |
| `GET /reservations/{id}` | uma reserva |
| `POST /reservations/{id}/confirmation` | confirma (o hóspede pagou) |
| `POST /reservations/{id}/cancellation` | cancela |
| `POST /reservations/{id}/check-in` | registra a entrada |
| `POST /reservations/{id}/check-out` | registra a saída e devolve o comprovante final |
| `GET /reservations/{id}/document` | a segunda via do comprovante |
| `GET /messages` | as mensagens enviadas |
| `GET /reports/occupancy?date=2027-07-10` | a ocupação de uma noite |
| `GET /reports/revenue?month=2027-07` | o financeiro de um mês |

O corpo de `POST /reservations`:

```json
{
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

O `companyCode` é um campo a mais, só do `CORPORATE`: nele é obrigatório, e nos outros planos, se vier,
é ignorado. Todos os outros campos são obrigatórios em qualquer plano.

Os erros vêm como `{"error": "<código>", "message": "..."}`:

| código | status | quando |
|---|---|---|
| `INVALID_REQUEST` | 400 | falta um campo, o corpo não é JSON, a saída não é depois da entrada, ou a reserva não cabe nas regras do plano de tarifa |
| `ROOM_NOT_FOUND` | 404 | o quarto não existe |
| `RESERVATION_NOT_FOUND` | 404 | a reserva não existe |
| `ROOM_UNAVAILABLE` | 409 | o quarto já está reservado em alguma das noites |
| `OPERATION_NOT_ALLOWED` | 409 | a reserva não está na situação que a operação pede |
| `CAPACITY_EXCEEDED` | 422 | mais hóspedes do que o quarto comporta |

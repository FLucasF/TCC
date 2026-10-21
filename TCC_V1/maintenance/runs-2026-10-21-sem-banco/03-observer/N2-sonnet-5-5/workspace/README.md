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
  fevereiro, e o mês de julho inteiro), a noite custa 20% a mais. O plano `NON_REFUNDABLE` dá 15% de
  desconto sobre a soma das noites. O dinheiro é arredondado para centavos, meio para o par, a cada passo.
- **Reserva:** nasce `PENDING`. Quando o hóspede paga, é confirmada (`CONFIRMED`), e ele recebe o
  comprovante por e-mail. Depois vêm o check-in (`CHECKED_IN`) e o check-out (`CHECKED_OUT`), que
  entrega o comprovante final.
- **Cancelamento:** só antes do check-in. No plano `STANDARD`, o hóspede recebe de volta o que pagou;
  no `NON_REFUNDABLE`, não recebe nada.
- **Avisos da equipe:** quando uma reserva é confirmada, a governança (datas e hóspedes) e o financeiro (valor pago)
  recebem e-mail; quando uma reserva confirmada é cancelada, a governança recebe as datas liberadas e o financeiro o
  reembolso (só se houver algo a devolver). Quem é avisado é definido em `Hotel`, com um `ReservationListener` por
  interessado.
- **Mensagens:** os e-mails enviados ficam guardados, e a recepção os consulta em `/messages`.
- **Relatórios (CSV):** a ocupação de uma noite, para a recepção, e o dinheiro que ficou com a pousada
  das reservas que começam num mês, para o contador.

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

Os erros vêm como `{"error": "<código>", "message": "..."}`:

| código | status | quando |
|---|---|---|
| `INVALID_REQUEST` | 400 | falta um campo, o corpo não é JSON, ou a saída não é depois da entrada |
| `ROOM_NOT_FOUND` | 404 | o quarto não existe |
| `RESERVATION_NOT_FOUND` | 404 | a reserva não existe |
| `ROOM_UNAVAILABLE` | 409 | o quarto já está reservado em alguma das noites |
| `OPERATION_NOT_ALLOWED` | 409 | a reserva não está na situação que a operação pede |
| `CAPACITY_EXCEEDED` | 422 | mais hóspedes do que o quarto comporta |

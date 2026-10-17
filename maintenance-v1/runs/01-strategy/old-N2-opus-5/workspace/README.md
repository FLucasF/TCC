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
  fevereiro, e o mês de julho inteiro), a noite custa 20% a mais. O plano de tarifa desconta sobre a
  soma das noites, ou fecha o preço do seu jeito. O dinheiro é arredondado para centavos, meio para o
  par, a cada passo.
- **Planos de tarifa:** cada plano traz o seu preço, quem pode reservá-lo e o que devolve no
  cancelamento. Quando a reserva não cabe nas regras do plano, ela não é feita (`INVALID_REQUEST`).

  | plano | preço | quem pode | devolve |
  |---|---|---|---|
  | `STANDARD` | as noites, sem desconto | qualquer estadia | tudo o que foi pago |
  | `NON_REFUNDABLE` | 15% de desconto | qualquer estadia | nada |
  | `CORPORATE` | o desconto do convênio da empresa: `TECNOSUL` 12%, `AGROVALE` 18%, `FARMAVIDA` 25% | quem informa em `companyCode` o código de uma dessas empresas | tudo o que foi pago |
  | `WEEKEND_PACKAGE` | preço fechado o ano inteiro, sem desconto: R$ 520,00 no standard, R$ 820,00 no deluxe, R$ 1.200,00 na suíte | entrada numa sexta e saída no domingo seguinte (duas noites) | metade do que foi pago |
  | `LONG_STAY` | 10% de desconto | estadias de 7 noites ou mais | tudo o que foi pago |

  Um plano novo é uma implementação nova de `RatePlanPolicy` e uma constante nova em `RatePlan`.
- **Reserva:** nasce `PENDING`. Quando o hóspede paga, é confirmada (`CONFIRMED`), e ele recebe o
  comprovante por e-mail. Depois vêm o check-in (`CHECKED_IN`) e o check-out (`CHECKED_OUT`), que
  entrega o comprovante final.
- **Cancelamento:** só antes do check-in, e o plano diz quanto volta (uma reserva `PENDING` nunca foi
  paga, então nada volta).
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
  "ratePlan": "STANDARD",
  "companyCode": "TECNOSUL"
}
```

Todos os campos são obrigatórios, menos o `companyCode`, que só o plano `CORPORATE` pede (nos outros
planos ele é ignorado).

Os erros vêm como `{"error": "<código>", "message": "..."}`:

| código | status | quando |
|---|---|---|
| `INVALID_REQUEST` | 400 | falta um campo, o corpo não é JSON, a saída não é depois da entrada, ou a estadia não cabe nas regras do plano de tarifa |
| `ROOM_NOT_FOUND` | 404 | o quarto não existe |
| `RESERVATION_NOT_FOUND` | 404 | a reserva não existe |
| `ROOM_UNAVAILABLE` | 409 | o quarto já está reservado em alguma das noites |
| `OPERATION_NOT_ALLOWED` | 409 | a reserva não está na situação que a operação pede |
| `CAPACITY_EXCEEDED` | 422 | mais hóspedes do que o quarto comporta |

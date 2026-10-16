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
  fevereiro, e o mês de julho inteiro), a noite custa 20% a mais. O dinheiro é arredondado para
  centavos, meio para o par, a cada passo.
- **Planos de tarifa:** cada um tem o seu jeito de calcular o preço, a sua regra de quem pode usar e a
  sua regra de devolução no cancelamento. Um plano novo é só uma nova implementação de
  `RatePlanPolicy` ligada a uma constante de `RatePlan`; o resto do sistema não muda.
  - `STANDARD`: preço cheio; devolve tudo no cancelamento.
  - `NON_REFUNDABLE`: 15% de desconto sobre a soma das noites; não devolve nada.
  - `CORPORATE`: convênio com empresas. O hóspede informa o `companyCode` (o nome da empresa em
    maiúsculas): `TECNOSUL` (12%), `AGROVALE` (18%) ou `FARMAVIDA` (25%) de desconto sobre as noites.
    Sem código, ou com um código desconhecido, a reserva não é feita. Devolve tudo no cancelamento.
  - `WEEKEND_PACKAGE`: entrada na sexta e saída no domingo logo depois (exatamente duas noites), por um
    preço fechado o ano inteiro — R$ 520,00 no standard, R$ 820,00 no deluxe, R$ 1.200,00 na suíte. Em
    qualquer outra data não pode ser reservado. Devolve metade no cancelamento.
  - `LONG_STAY`: sete noites ou mais; 10% de desconto sobre as noites (a alta temporada continua
    valendo). Com menos de sete noites não pode ser usado. Devolve tudo no cancelamento.
- **Reserva:** nasce `PENDING`. Quando o hóspede paga, é confirmada (`CONFIRMED`), e ele recebe o
  comprovante por e-mail. Depois vêm o check-in (`CHECKED_IN`) e o check-out (`CHECKED_OUT`), que
  entrega o comprovante final.
- **Cancelamento:** só antes do check-in. O quanto volta depende do plano (veja acima).
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

O `companyCode` é um campo opcional, só do plano `CORPORATE`; nos outros planos, se vier, é ignorado.

Os erros vêm como `{"error": "<código>", "message": "..."}`:

| código | status | quando |
|---|---|---|
| `INVALID_REQUEST` | 400 | falta um campo, o corpo não é JSON, a saída não é depois da entrada, ou a reserva fere uma regra do plano de tarifa |
| `ROOM_NOT_FOUND` | 404 | o quarto não existe |
| `RESERVATION_NOT_FOUND` | 404 | a reserva não existe |
| `ROOM_UNAVAILABLE` | 409 | o quarto já está reservado em alguma das noites |
| `OPERATION_NOT_ALLOWED` | 409 | a reserva não está na situação que a operação pede |
| `CAPACITY_EXCEEDED` | 422 | mais hóspedes do que o quarto comporta |

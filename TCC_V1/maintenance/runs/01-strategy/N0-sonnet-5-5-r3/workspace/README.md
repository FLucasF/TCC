# Pousada Maré Alta: o sistema de reservas

O serviço que a pousada usa para as reservas: o site cria e confirma as reservas, a recepção faz o
check-in e o check-out, e o contador tira os relatórios.

## Como rodar

Os dados ficam num PostgreSQL. O endereço vem na variável `DB_URL`
(`jdbc:postgresql://<máquina>:<porta>/<banco>`), o usuário em `DB_USER` e a senha em `DB_PASSWORD`.

```bash
mvn verify
```

```bash
java -jar target/hotel.jar
```

O serviço sobe na porta 7070 (ou na que estiver em `PORT`). Ao subir, ele cria e atualiza as tabelas
com as migrações do Flyway, em `src/main/resources/db/migration` (uma mudança no esquema é uma
migração nova, `V2__...sql`, e as que já existem não mudam). Os testes usam o mesmo banco, pelas
mesmas variáveis, e o esvaziam antes de cada teste (`EmptyDatabase`): as reservas e as mensagens
somem, e a numeração volta a 1.

## Como funciona

- **Quartos:** cinco, de três tipos (`STANDARD`, `DELUXE`, `SUITE`), cada um com a sua capacidade e a
  sua diária. Vêm com o esquema do banco.
- **Dados:** as tabelas `rooms`, `reservations` e `messages`. O banco também não deixa um quarto ter
  duas reservas (não canceladas) na mesma noite, mesmo com dois pedidos chegando juntos.
- **Preço:** cada noite custa a diária do quarto. Na alta temporada (de 15 de dezembro ao fim de
  fevereiro, e o mês de julho inteiro), a noite custa 20% a mais. Cada plano de tarifa (`RatePlan`) tem as suas regras de quem pode usar, de preço e de devolução; um plano novo é uma constante nova nesse enum.
  `NON_REFUNDABLE`: 15% de desconto sobre a soma das noites. `CORPORATE`: desconto da empresa do convênio (`companyCode`: `TECNOSUL` 12%, `AGROVALE` 18%, `FARMAVIDA` 25%; sem código válido a reserva é recusada). `WEEKEND_PACKAGE`: só sexta a domingo (2 noites), preço fechado o ano todo (standard R$ 520, deluxe R$ 820, suíte R$ 1.200), sem desconto. `LONG_STAY`: 7 noites ou mais, 10% de desconto. O dinheiro é arredondado para centavos, meio para o par, a cada passo.
- **Reserva:** nasce `PENDING`. Quando o hóspede paga, é confirmada (`CONFIRMED`), e ele recebe o
  comprovante por e-mail. Depois vêm o check-in (`CHECKED_IN`) e o check-out (`CHECKED_OUT`), que
  entrega o comprovante final.
- **Cancelamento:** só antes do check-in. No plano `STANDARD`, o hóspede recebe de volta o que pagou;
  no `NON_REFUNDABLE`, não recebe nada; `CORPORATE` e `LONG_STAY` devolvem tudo, e `WEEKEND_PACKAGE` devolve metade.
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

O campo opcional `companyCode` só vale para `CORPORATE` (nos outros planos é ignorado). Quando as regras do plano não permitem a reserva, a resposta é 400 `INVALID_REQUEST`.

Os erros vêm como `{"error": "<código>", "message": "..."}`:

| código | status | quando |
|---|---|---|
| `INVALID_REQUEST` | 400 | falta um campo, o corpo não é JSON, a saída não é depois da entrada, ou o plano não aceita a reserva |
| `ROOM_NOT_FOUND` | 404 | o quarto não existe |
| `RESERVATION_NOT_FOUND` | 404 | a reserva não existe |
| `ROOM_UNAVAILABLE` | 409 | o quarto já está reservado em alguma das noites |
| `OPERATION_NOT_ALLOWED` | 409 | a reserva não está na situação que a operação pede |
| `CAPACITY_EXCEEDED` | 422 | mais hóspedes do que o quarto comporta |

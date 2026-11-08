# Exemplos de Uso da API

## Criando uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## Confirmando uma Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 499.80,
  ...
}
```

Isso também envia um e-mail de confirmação (armazenado em `/messages`).

## Consultando uma Reserva

```bash
curl http://localhost:7070/reservations/1
```

## Cancelando uma Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

- STANDARD: recebe reembolso total
- NON_REFUNDABLE: sem reembolso

## Check-in e Check-out

```bash
# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out (retorna o comprovante)
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

Resposta:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "11122233344",
  "roomNumber": "201",
  "roomType": "DELUXE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80
}
```

## Listando Quartos

```bash
curl http://localhost:7070/rooms
```

Resposta:
```json
[
  {
    "number": "101",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": "102",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

## Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  }
]
```

## Relatórios CSV

### Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta:
```csv
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,2,2027-07-10,2027-07-13
total,2/5
```

### Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta:
```csv
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
2,201,2027-07-10,2027-07-13,CONFIRMED,995.47
total,1595.23
```

### Relatório de Taxa de Turismo

```bash
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"
```

Resposta:
```csv
Pousada Maré Alta,tourism-tax,2027-07
reservation,room,check_in,check_out,guests,nights,tax
1,101,2027-07-10,2027-07-12,2,2,18.00
2,201,2027-07-10,2027-07-13,2,3,27.00
total,45.00
```

### Relatório de Planos de Tarifa

```bash
curl "http://localhost:7070/reports/rate-plans?month=2027-07"
```

Resposta:
```csv
Pousada Maré Alta,rate-plans,2027-07
rate_plan,reservations,amount
STANDARD,1,599.76
NON_REFUNDABLE,1,995.47
total,2,1595.23
```

## Códigos de Erro

| Código | Status | Descrição |
|--------|--------|-----------|
| INVALID_REQUEST | 400 | Campos faltando, JSON inválido, data inválida |
| ROOM_NOT_FOUND | 404 | Quarto não existe |
| RESERVATION_NOT_FOUND | 404 | Reserva não existe |
| ROOM_UNAVAILABLE | 409 | Quarto já reservado nas datas |
| OPERATION_NOT_ALLOWED | 409 | Operação não permitida para o status atual |
| CAPACITY_EXCEEDED | 422 | Mais hóspedes que a capacidade do quarto |

Exemplo de erro:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

## Cálculo de Preços

### Exemplo 1: Alta Temporada

Reserva STANDARD, quarto 101 (R$ 249,90), de 10/07/2027 a 12/07/2027 (2 noites em julho = alta temporada):

- Noite 1: R$ 249,90 × 1,20 = R$ 299,88
- Noite 2: R$ 249,90 × 1,20 = R$ 299,88
- Subtotal: R$ 599,76
- Desconto: R$ 0,00
- Total: R$ 599,76

### Exemplo 2: Plano NON_REFUNDABLE

Reserva NON_REFUNDABLE, quarto 201 (R$ 389,90), de 10/07/2027 a 13/07/2027 (3 noites):

- Noite 1: R$ 389,90 × 1,20 = R$ 467,88
- Noite 2: R$ 389,90 × 1,20 = R$ 467,88
- Noite 3: R$ 389,90 × 1,20 = R$ 467,88
- Subtotal: R$ 1.403,64
- Desconto: R$ 1.403,64 × 0,15 = R$ 210,55 (arredondado)
- Total: R$ 1.193,09

### Exemplo 3: Baixa Temporada

Reserva STANDARD, quarto 101 (R$ 249,90), de 10/03/2027 a 12/03/2027 (2 noites):

- Noite 1: R$ 249,90
- Noite 2: R$ 249,90
- Subtotal: R$ 499,80
- Desconto: R$ 0,00
- Total: R$ 499,80

## Taxa de Turismo

A taxa é calculada como: **Número de hóspedes × Número de noites × R$ 4,50**

Exemplo: 2 hóspedes, 3 noites = 2 × 3 × 4,50 = R$ 27,00

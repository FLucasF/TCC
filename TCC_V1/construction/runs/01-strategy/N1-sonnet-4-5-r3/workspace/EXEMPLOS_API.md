# Exemplos de Uso da API

## Listar Quartos

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

## Criar Reserva

### Plano Standard

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Plano Corporativo (Tecnosul)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@tecnosul.com.br",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

### Pacote de Fim de Semana

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

### Estadia Longa

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }'
```

Resposta (exemplo):
```json
{
  "id": 1,
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## Consultar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## Confirmar Reserva (Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  ...
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  ...
}
```

## Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta (comprovante):
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

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Resposta mostra o campo `refund` calculado segundo o plano:
```json
{
  "id": 1,
  ...
  "status": "CANCELLED",
  "amountPaid": 779.80,
  "refund": 779.80
}
```

## Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## Listar Mensagens Enviadas

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

## Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,3,2027-07-09,2027-07-11
total,2/5
```

## Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
3,201,2027-07-09,2027-07-11,CHECKED_OUT,779.80
total,1379.56
```

## Tratamento de Erros

### Quarto não encontrado
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": "999", ...}'
```

Resposta (404):
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found: 999"
}
```

### Quarto já reservado
Resposta (409):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

### Capacidade excedida
Resposta (422):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

### Operação não permitida
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
# Quando a reserva está PENDING
```

Resposta (409):
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Can only check-in a confirmed reservation"
}
```

### Dados inválidos
Resposta (400):
```json
{
  "error": "INVALID_REQUEST",
  "message": "Check-out must be after check-in"
}
```

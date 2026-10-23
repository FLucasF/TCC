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
  {
    "number": "201",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": "202",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": "301",
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

## Criar Reserva

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

Resposta (201 Created):
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

## Criar Reserva com Desconto Não Reembolsável

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Costa",
    "guestEmail": "maria@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resposta:
```json
{
  "id": 2,
  "roomNumber": "201",
  "guestName": "Maria Costa",
  "guestEmail": "maria@example.com",
  "guestCpf": "99988877766",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "NON_REFUNDABLE",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 116.97,
  "total": 662.83,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
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
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  "refund": null
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

## Buscar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## Buscar Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
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
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada...",
    "status": "SENT"
  }
]
```

## Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CONFIRMED,779.80
total,779.80
```

## Erros

### Quarto não encontrado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta (404):
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found: 999"
}
```

### Quarto não disponível

Resposta (409):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

### Capacidade excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta (422):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Room capacity exceeded"
}
```

### Operação não permitida

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

Resposta (409 - se a reserva não estiver confirmada):
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Only confirmed reservations can be checked in"
}
```

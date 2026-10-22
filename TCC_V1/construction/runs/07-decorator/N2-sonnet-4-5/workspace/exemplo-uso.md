# Exemplos de Uso da API

## 1. Listar quartos disponíveis

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

## 2. Criar uma reserva

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
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## 3. Confirmar uma reserva

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

## 4. Consultar mensagens enviadas

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

## 5. Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 6. Fazer check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
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

## 7. Obter segunda via do comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## 8. Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-11"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## 9. Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,779.80
```

## 10. Cancelar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Casos de erro

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

### Quarto indisponível
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
Resposta (409):
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Cannot confirm this reservation"
}
```

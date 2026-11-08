# Exemplos de Requisições HTTP

## 1. Listar Quartos

```bash
curl http://localhost:7070/rooms
```

Resposta:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": 102,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

## 2. Criar Reserva STANDARD (temporada alta)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta (201):
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

## 3. Criar Reserva CORPORATE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432101",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

## 4. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  ...
  "status": "CONFIRMED",
  "amountPaid": 599.76,
  ...
}
```

## 5. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 6. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta (comprovante):
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678901",
  "roomNumber": 101,
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0,
  "total": 599.76
}
```

## 7. Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## 8. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## 9. Listar E-mails Enviados

```bash
curl http://localhost:7070/messages
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!...\n"
  }
]
```

## 10. Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

## 11. Relatório Financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
total,599.76
```

## Casos de Teste Importantes

### WEEKEND_PACKAGE válido
Sexta a domingo (exatamente 2 noites)
```json
{
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14",
  "ratePlan": "WEEKEND_PACKAGE"
}
```

### WEEKEND_PACKAGE inválido
Não é sexta a domingo
```json
{
  "checkIn": "2027-03-13",
  "checkOut": "2027-03-15",
  "ratePlan": "WEEKEND_PACKAGE"
}
```
Retorna: `400 INVALID_REQUEST`

### LONG_STAY válido
7+ noites
```json
{
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-08",
  "ratePlan": "LONG_STAY"
}
```

### LONG_STAY inválido
Menos de 7 noites
```json
{
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-07",
  "ratePlan": "LONG_STAY"
}
```
Retorna: `400 INVALID_REQUEST`

### CORPORATE sem código
```json
{
  "ratePlan": "CORPORATE"
}
```
Retorna: `400 INVALID_REQUEST`

### Quarto indisponível
Tentar reservar mesmo quarto na mesma noite
Retorna: `409 ROOM_UNAVAILABLE`

### Capacidade excedida
```json
{
  "roomNumber": 101,
  "guests": 5
}
```
Retorna: `422 CAPACITY_EXCEEDED`

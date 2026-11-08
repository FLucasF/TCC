# Exemplos de Uso da API

## Quartos

### Listar todos os quartos

```bash
curl http://localhost:7070/rooms
```

**Resposta:**
```json
[
  {
    "number": "101",
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
    "number": "301",
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

## Reservas

### Criar uma reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (201 Created):**
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678900",
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

### Criar reserva não reembolsável em alta temporada

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

**Resposta:**
```json
{
  "id": 2,
  "nights": 2,
  "subtotal": 935.76,
  "discount": 140.36,
  "total": 795.40,
  "status": "PENDING"
}
```

**Explicação do cálculo:**
- Quarto DELUXE: R$ 389,90 (base)
- Alta temporada (julho): R$ 389,90 × 1.20 = R$ 467,88 por noite
- 2 noites: R$ 467,88 × 2 = R$ 935,76 (subtotal)
- Desconto não reembolsável (15%): R$ 935,76 × 0.15 = R$ 140,36
- Total: R$ 935,76 - R$ 140,36 = R$ 795,40

### Buscar uma reserva

```bash
curl http://localhost:7070/reservations/1
```

### Confirmar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**Resposta:**
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 499.80,
  "refund": null
}
```

**Efeito colateral:** Um e-mail de confirmação é enviado (guardado internamente).

### Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

**Resposta:**
```json
{
  "id": 1,
  "status": "CHECKED_IN"
}
```

### Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

**Resposta:**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678900",
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80
}
```

### Obter segunda via do comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

### Cancelar reserva

**Plano STANDARD (com reembolso):**
```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

**Resposta:**
```json
{
  "id": 1,
  "status": "CANCELLED",
  "amountPaid": 499.80,
  "refund": 499.80
}
```

**Plano NON_REFUNDABLE (sem reembolso):**
```bash
curl -X POST http://localhost:7070/reservations/2/cancellation
```

**Resposta:**
```json
{
  "id": 2,
  "status": "CANCELLED",
  "amountPaid": 795.40,
  "refund": 0.00
}
```

## Mensagens

### Listar mensagens enviadas

```bash
curl http://localhost:7070/messages
```

**Resposta:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  }
]
```

## Relatórios

### Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

**Resposta (text/csv):**
```csv
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,2,2027-07-10,2027-07-15
total,2/5
```

### Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

**Resposta (text/csv):**
```csv
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
2,201,2027-07-10,2027-07-15,CHECKED_OUT,795.4
3,202,2027-07-20,2027-07-22,CANCELLED,0
total,1395.16
```

**Nota:** Reserva cancelada com plano NON_REFUNDABLE aparece com valor pago menos reembolso.

### Relatório de taxa de turismo

```bash
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"
```

**Resposta (text/csv):**
```csv
Pousada Maré Alta,tourism-tax,2027-07
reservation,room,check_in,check_out,guests,nights,tax
1,101,2027-07-10,2027-07-12,2,2,18
2,201,2027-07-10,2027-07-15,3,5,67.5
total,85.5
```

**Cálculo:** R$ 4,50 × hóspedes × noites

### Relatório de planos de tarifa

```bash
curl "http://localhost:7070/reports/rate-plans?month=2027-07"
```

**Resposta (text/csv):**
```csv
Pousada Maré Alta,rate-plans,2027-07
rate_plan,reservations,amount
STANDARD,2,1199.52
NON_REFUNDABLE,1,795.4
total,3,1994.92
```

## Erros

### Quarto não encontrado (404)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "João",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (404):**
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found"
}
```

### Quarto indisponível (409)

```bash
# Tentando criar segunda reserva no mesmo período
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    ...
  }'
```

**Resposta (409):**
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

### Capacidade excedida (422)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guests": 5,
    ...
  }'
```

**Resposta (422):**
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

### Operação não permitida (409)

```bash
# Tentando fazer check-in em reserva PENDING
curl -X POST http://localhost:7070/reservations/1/check-in
```

**Resposta (409):**
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Operation not allowed for current reservation status"
}
```

### Dados inválidos (400)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-10",
    ...
  }'
```

**Resposta (400):**
```json
{
  "error": "INVALID_REQUEST",
  "message": "Invalid request parameters"
}
```

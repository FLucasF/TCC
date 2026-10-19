# API Reference - Pousada Maré Alta

## Base URL
```
http://localhost:7070
```

## Rooms

### GET /rooms
Lista todos os quartos disponíveis.

**Response:**
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

---

## Reservations

### POST /reservations
Cria uma nova reserva.

**Body:**
```json
{
  "roomNumber": 101,
  "guestName": "string",
  "guestEmail": "email@example.com",
  "checkIn": "YYYY-MM-DD",
  "checkOut": "YYYY-MM-DD",
  "guests": 1,
  "ratePlan": "STANDARD" | "NON_REFUNDABLE",
  
  "guestCpf": "XXX.XXX.XXX-XX" | null,
  "companyCnpj": "XX.XXX.XXX/XXXX-XX" | null,
  "companyName": "string" | null,
  "guestPassport": "string" | null,
  "guestCountry": "string" | null
}
```

*Nota: Forneça EXATAMENTE UM tipo de identificação (CPF OU CNPJ+companyName OU Passaporte+country)*

**Response:** (201 Created)
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "string",
  "guestEmail": "email@example.com",
  "guestCpf": "XXX.XXX.XXX-XX" | null,
  "companyCnpj": "..." | null,
  "companyName": "..." | null,
  "guestPassport": "..." | null,
  "guestCountry": "..." | null,
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 1,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

---

### GET /reservations/{id}
Obtém detalhes de uma reserva.

**Response:**
```json
{
  "id": 1,
  "status": "PENDING" | "CONFIRMED" | "CHECKED_IN" | "CHECKED_OUT" | "CANCELLED",
  ...
}
```

---

### POST /reservations/{id}/confirmation
Confirma uma reserva pendente (registra pagamento).

**Body:**
```json
{
  "amountPaid": 599.76
}
```

**Response:**
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 599.76,
  ...
}
```

*Efeito colateral: Email enviado com comprovante*

---

### POST /reservations/{id}/check-in
Faz check-in de uma reserva confirmada.

**Response:**
```json
{
  "id": 1,
  "status": "CHECKED_IN",
  ...
}
```

---

### POST /reservations/{id}/check-out
Faz check-out e retorna o comprovante.

**Response:**
```json
{
  "title": "Comprovante de reserva" | "Fatura de hospedagem" | "Booking receipt",
  "reservationId": 1,
  "guestName": "string",
  "guestCpf": "..." | null,
  "companyCnpj": "..." | null,
  "companyName": "..." | null,
  "guestPassport": "..." | null,
  "guestCountry": "..." | null,
  "roomNumber": 101,
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76,
  "text": "Comprovante de reserva nº 1\nHóspede: Name (CPF XXX.XXX.XXX-XX)\n..."
}
```

---

### POST /reservations/{id}/cancellation
Cancela uma reserva (PENDING ou CONFIRMED).

**Response:**
```json
{
  "id": 1,
  "status": "CANCELLED",
  "amountPaid": 599.76,
  "refund": 599.76 | 0.00
}
```

*Reembolso depende do ratePlan:*
- *STANDARD: reembolso total*
- *NON_REFUNDABLE: sem reembolso*

---

### GET /reservations/{id}/document
Obtém segunda via do comprovante.

**Response:**
(Mesmo formato que check-out)

---

## Messages

### GET /messages
Lista emails enviados.

**Response:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "guest@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Guest Name!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  },
  ...
]
```

---

## Reports

### GET /reports/occupancy?date=YYYY-MM-DD
Relatório de ocupação para uma data.

**Response:** (text/csv; charset=utf-8)
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,2,2027-07-10,2027-07-13
total,2/5
```

*Nota: Check-in na data conta; check-out na data NÃO conta*

---

### GET /reports/revenue?month=YYYY-MM
Relatório de receita para um mês.

**Response:** (text/csv; charset=utf-8)
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
2,201,2027-01-20,2027-01-23,CANCELLED,926.88
total,1526.64
```

*Nota: Amount = amountPaid - refund*

---

## Error Responses

Todos os erros retornam:
```json
{
  "error": "ERROR_CODE",
  "message": "descriptive message"
}
```

### Status Codes

| Code | Error | Cause |
|------|-------|-------|
| 400 | INVALID_REQUEST | Missing field, invalid format, invalid date |
| 404 | ROOM_NOT_FOUND | Room ID doesn't exist |
| 404 | RESERVATION_NOT_FOUND | Reservation ID doesn't exist |
| 409 | ROOM_UNAVAILABLE | Room booked for those dates |
| 409 | OPERATION_NOT_ALLOWED | Invalid state transition |
| 422 | CAPACITY_EXCEEDED | Too many guests |

---

## Rate Plans

| Plan | Discount | Refund on Cancel |
|------|----------|------------------|
| STANDARD | 0% | 100% |
| NON_REFUNDABLE | 15% | 0% |

---

## Pricing Examples

### July (High Season) - STANDARD rate plan
- STANDARD room: R$ 249,90 × 1.20 = R$ 299,88/night
- DELUXE room: R$ 389,90 × 1.20 = R$ 467,88/night
- SUITE room: R$ 579,90 × 1.20 = R$ 695,88/night

### January (High Season) - NON_REFUNDABLE rate plan
- STANDARD: (R$ 249,90 × 1.20) × 3 nights - 15% = R$ 763,05
- DELUXE: (R$ 389,90 × 1.20) × 3 nights - 15% = R$ 1.193,07

### September (Low Season) - STANDARD rate plan
- STANDARD: R$ 249,90 × 2 nights = R$ 499,80
- (No high season multiplier)

---

## High Season Dates

```
Dec 15 - Feb 28 (inclusive)
July 1 - July 31 (inclusive)
```

All other dates = low season (normal price)

---

## Notes

- All amounts are in BRL (Brazilian Real)
- Amounts always use 2 decimal places
- Rounding: HALF_EVEN (banker's rounding)
- Dates in ISO 8601 format (YYYY-MM-DD)
- Data is in-memory; restart clears everything

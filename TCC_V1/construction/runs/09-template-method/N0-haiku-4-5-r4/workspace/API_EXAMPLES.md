# Exemplos de Uso da API

## 1. Obter lista de quartos

```
GET /rooms

Response:
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

## 2. Criar reserva

```
POST /reservations
Content-Type: application/json

{
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}

Response (201):
{
  "id": 7,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
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

## 3. Confirmar reserva (com pagamento)

```
POST /reservations/7/confirmation
Content-Type: application/json

{
  "amountPaid": 779.80
}

Response:
{
  "id": 7,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  ...
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  "refund": null
}
```

## 4. Verificar emails enviados

```
GET /messages

Response:
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@email.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  }
]
```

## 5. Check-in

```
POST /reservations/7/check-in

Response:
{
  "id": 7,
  "status": "CHECKED_IN",
  ...
}
```

## 6. Check-out (retorna comprovante)

```
POST /reservations/7/check-out

Response:
{
  "title": "Comprovante de reserva",
  "reservationId": 7,
  "guestName": "Carla Dias",
  "guestCpf": "11122233344",
  "roomNumber": 201,
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

```
GET /reservations/7/document

Response:
{
  "title": "Comprovante de reserva",
  "reservationId": 7,
  ...
}
```

## 8. Cancelar reserva

```
POST /reservations/7/cancellation

Response:
{
  "id": 7,
  "status": "CANCELLED",
  "amountPaid": 779.80,
  "refund": 779.80
}
```

## 9. Relatório de ocupação

```
GET /reports/occupancy?date=2027-07-10

Response (text/csv):
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,3,2027-07-09,2027-07-12
301,SUITE,5,2027-07-10,2027-07-15
total,3/5
```

## 10. Relatório financeiro

```
GET /reports/revenue?month=2027-07

Response (text/csv):
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
3,201,2027-07-09,2027-07-12,CHECKED_IN,779.80
total,,,,,1379.56
```

## 11. Relatório de taxa de turismo

```
GET /reports/tourism-tax?month=2027-07

Response (text/csv):
Pousada Maré Alta,tourism-tax,2027-07
reservation,room,check_in,check_out,guests,nights,tax
1,101,2027-07-10,2027-07-12,2,2,18.00
3,201,2027-07-09,2027-07-12,2,3,27.00
total,,,,,45.00
```

## 12. Relatório de planos de tarifa

```
GET /reports/rate-plans?month=2027-07

Response (text/csv):
Pousada Maré Alta,rate-plans,2027-07
rate_plan,reservations,amount
STANDARD,2,1179.76
NON_REFUNDABLE,1,499.62
total,3,1679.38
```

## Códigos de Erro

- `400 INVALID_REQUEST`: Requisição inválida (campos obrigatórios faltando, formato inválido)
- `404 ROOM_NOT_FOUND`: Quarto não encontrado
- `404 RESERVATION_NOT_FOUND`: Reserva não encontrada
- `409 ROOM_UNAVAILABLE`: Quarto não disponível para as datas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida para o status atual
- `422 CAPACITY_EXCEEDED`: Número de hóspedes excede a capacidade do quarto

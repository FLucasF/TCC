# Exemplo de Uso da API

Este documento mostra exemplos de como usar os endpoints da API.

## 1. Listar Quartos

```bash
curl -X GET http://localhost:7070/rooms
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

## 2. Criar Reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
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
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80",
  "status": "PENDING",
  "amountPaid": "0.00",
  "refund": null
}
```

## 3. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": "779.80"
  }'
```

Resposta:
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": "779.80",
  ...
}
```

## 4. Ver Mensagens Enviadas

```bash
curl -X GET http://localhost:7070/messages
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

## 5. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 6. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta (documento):
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "11122233344",
  "roomNumber": 201,
  "roomType": "DELUXE",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "nights": 2,
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80",
  "text": "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n..."
}
```

## 7. Relatório de Ocupação

```bash
curl -X GET "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## 8. Relatório Financeiro

```bash
curl -X GET "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CONFIRMED,779.80
total,,,,,779.80
```

## Exemplos com CNPJ e Passaporte

### Criar reserva com CNPJ

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@company.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

### Criar reserva com Passaporte

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

O documento será automaticamente em inglês para estrangeiros e em português para brasileiros.

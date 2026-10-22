# Exemplo de Uso - Pousada Maré Alta

Este documento mostra exemplos de requisições HTTP para o sistema de reservas.

## 1. Listar Quartos Disponíveis

```bash
curl http://localhost:7070/rooms
```

Resposta:
```json
[
  {"number": 101, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 102, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 201, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 202, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 301, "type": "SUITE", "capacity": 4, "nightlyRate": 579.90}
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
    "guestCpf": "111.222.333-44",
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
  "guestCpf": "111.222.333-44",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.0,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0.0,
  "refund": null
}
```

## 3. Criar Reserva com CNPJ

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@company.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## 4. Criar Reserva com Passaporte (Estrangeiro)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Nota: Este é em julho (alta temporada), então:
- Taxa normal: 579.90 * 1.20 = 695.88 por noite
- Subtotal: 695.88 * 2 = 1.391,76
- Total: 1.391,76

## 5. Confirmar Reserva (Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  ...
}
```

A confirmação dispara um e-mail que pode ser recuperado em `/messages`.

## 6. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

Resposta:
```json
{
  "id": 1,
  "status": "CHECKED_IN",
  ...
}
```

## 7. Fazer Check-out e Obter Comprovante

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta com o documento:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "111.222.333-44",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "roomNumber": 201,
  "roomType": "DELUXE",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.0,
  "total": 779.80,
  "text": "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n"
}
```

## 8. Segunda Via do Documento

```bash
curl http://localhost:7070/reservations/1/document
```

Mesma resposta do check-out. Disponível após confirmação da reserva.

## 9. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Se foi PENDING: não há refund
Se foi CONFIRMED e plano STANDARD: refund = amountPaid
Se foi CONFIRMED e plano NON_REFUNDABLE: refund = 0.0

## 10. Listar Mensagens Enviadas

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

## 11. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
301,SUITE,3,2027-07-10,2027-07-12
total,1/5
```

## 12. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
3,301,2027-07-10,2027-07-12,CHECKED_OUT,1391.76
total,1391.76
```

## Cálculo de Exemplo: Alta Temporada

Para uma reserva de 10/07/2027 a 12/07/2027 (julho = alta temporada):

1. Taxa base da SUITE: 579.90
2. Multiplicador de alta temporada: 579.90 × 1.20 = 695.88
3. Arredondamento (meio para par): 695.88
4. 2 noites: 695.88 × 2 = 1.391,76
5. Sem desconto (plano STANDARD)
6. Total: 1.391,76

Se fosse plano NON_REFUNDABLE:
- Desconto: 1.391,76 × 0.15 = 208.764 → arredonda para 208.76
- Total: 1.391,76 - 208.76 = 1.183,00

## Erro: Quarto Indisponível

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta (409 Conflict):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the specified dates"
}
```

## Erro: Capacidade Excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-17",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta (422 Unprocessable Entity):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

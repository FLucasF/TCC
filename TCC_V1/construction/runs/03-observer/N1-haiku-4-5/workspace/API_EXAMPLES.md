# Exemplos de Uso da API

Assumindo que a aplicação está rodando em `http://localhost:7070`

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
  ...
]
```

## 2. Criar Reserva

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

## 3. Consultar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## 4. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": 201,
  ...
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  ...
}
```

Isso dispara:
- Email para o hóspede em `carla@example.com`
- Email para governança em `governanca@pousadamarealta.com.br`
- Email para financeiro em `financeiro@pousadamarealta.com.br`

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
  "guestName": "Carla Dias",
  "guestCpf": "111.222.333-44",
  "roomNumber": 201,
  "roomType": "DELUXE",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80
}
```

## 7. Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## 8. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Se era confirmada (plano STANDARD), dispara:
- Email para governança (cancelamento)
- Email para financeiro (reembolso)

Se era confirmada (plano NON_REFUNDABLE):
- Email para governança apenas

## 9. Listar Mensagens Enviadas

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
    "body": "Olá, Carla Dias!...",
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "governanca@pousadamarealta.com.br",
    "subject": "Reserva confirmada - quarto 201",
    "body": "Entrada: 10/03/2027\nSaída: 12/03/2027\nHóspedes: 2"
  },
  ...
]
```

## 10. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,,2027-03-10,2027-03-10
102,STANDARD,,2027-03-10,2027-03-10
201,DELUXE,1,2027-03-10,2027-03-12
202,DELUXE,,2027-03-10,2027-03-10
301,SUITE,,2027-03-10,2027-03-10
total,1/5
```

## 11. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,,,,,779.80
```

## Erro: Quarto não disponível

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Pedro Silva",
    "guestEmail": "pedro@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-03-11",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta (409 Conflict):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room not available for these dates"
}
```

## Erro: Capacidade excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "999.888.777-66",
    "checkIn": "2027-04-01",
    "checkOut": "2027-04-05",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta (422 Unprocessable Entity):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Too many guests for this room"
}
```

## Reserva com Desconto NON_REFUNDABLE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "123.456.789-10",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resposta:
```json
{
  "id": 2,
  "roomNumber": 101,
  "guestName": "Maria Santos",
  ...
  "nights": 2,
  "subtotal": 599.76,
  "discount": 89.96,
  "total": 509.80,
  "status": "PENDING",
  ...
}
```

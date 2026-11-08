# Exemplos de Uso da API

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
  {
    "number": 201,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": 202,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": 301,
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

## 2. Criar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
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
  "guestEmail": "carla@email.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

## 3. Obter Detalhes da Reserva

```bash
curl http://localhost:7070/reservations/1
```

## 4. Confirmar uma Reserva (Pagamento Recebido)

Isso dispara notificações para o hóspede, governança e financeiro.

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0,
  "total": 779.80,
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  "refund": null
}
```

## 5. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 6. Fazer Check-out (Retorna Comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta:
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
  "discount": 0,
  "total": 779.80
}
```

## 7. Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## 8. Cancelar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Resposta (com reembolso se aplicável):
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0,
  "total": 779.80,
  "status": "CANCELLED",
  "amountPaid": 779.80,
  "refund": 779.80
}
```

## 9. Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## 10. Relatório Financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-03
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,779.80
```

## 11. Listar Mensagens de Notificação

```bash
curl http://localhost:7070/messages
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@email.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "governanca@pousadamarealta.com.br",
    "subject": "Reserva confirmada - quarto 201",
    "body": "Entrada: 10/03/2027\nSaída: 12/03/2027\nHóspedes: 2"
  },
  {
    "id": 3,
    "channel": "EMAIL",
    "to": "financeiro@pousadamarealta.com.br",
    "subject": "Pagamento recebido - reserva 1",
    "body": "Valor: R$ 779,80"
  }
]
```

## Erro: Quarto já reservado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Outro Hóspede",
    "guestEmail": "outro@email.com",
    "guestCpf": "444.555.666-77",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-11",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta (409 Conflict):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the requested dates"
}
```

## Erro: Capacidade excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Grupo Grande",
    "guestEmail": "grupo@email.com",
    "guestCpf": "888.999.000-11",
    "checkIn": "2027-05-01",
    "checkOut": "2027-05-03",
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

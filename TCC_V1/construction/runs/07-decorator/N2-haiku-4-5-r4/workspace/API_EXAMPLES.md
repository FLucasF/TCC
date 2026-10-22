# Exemplos de Requisições - API Pousada Maré Alta

## Listar Quartos

```bash
curl -X GET http://localhost:7070/rooms
```

**Resposta (200)**:
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

## Criar Reserva

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

**Resposta (201)**:
```json
{
  "id": 1,
  "roomNumber": 201,
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

## Obter Reserva

```bash
curl -X GET http://localhost:7070/reservations/1
```

**Resposta (200)**: Same as above

## Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**Resposta (200)**:
```json
{
  "id": 1,
  "roomNumber": 201,
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

**Efeito Colateral**: Envia e-mail para `carla@example.com` com o comprovante (ou para `testes@pousadamarealta.com.br` em modo teste).

## Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

**Resposta (200)**:
```json
{
  "id": 1,
  "roomNumber": 201,
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
  "status": "CHECKED_IN",
  "amountPaid": 779.80,
  "refund": null
}
```

## Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

**Resposta (200)**:
```json
{
  "id": 1,
  "roomNumber": 201,
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
  "status": "CHECKED_OUT",
  "amountPaid": 779.80,
  "refund": null,
  "document": "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80"
}
```

## Obter Segunda Via do Comprovante

```bash
curl -X GET http://localhost:7070/reservations/1/document
```

**Resposta (200)**:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "111.222.333-44",
  "roomNumber": 201,
  "roomType": "DELUXE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80"
}
```

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

**Resposta (200)**:
```json
{
  "id": 1,
  "roomNumber": 201,
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
  "status": "CANCELLED",
  "amountPaid": 779.80,
  "refund": 779.80
}
```

## Listar Mensagens Enviadas

```bash
curl -X GET http://localhost:7070/messages
```

**Resposta (200)**:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº 1\n...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº 1\n...",
    "status": "SENT"
  }
]
```

## Obter Relatório de Ocupação

```bash
curl -X GET "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

**Resposta (200)** (text/csv):
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## Obter Relatório de Receita

```bash
curl -X GET "http://localhost:7070/reports/revenue?month=2027-03"
```

**Resposta (200)** (text/csv):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.76
total,,,,,779.76
```

## Erros Comuns

### E-mail Inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João",
    "guestEmail": "joao.gmail.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (201)**: Reserva é criada, mas o e-mail de confirmação será bloqueado.

### Quarto Não Encontrado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    ...
  }'
```

**Resposta (404)**:
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found"
}
```

### Quarto Indisponível

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria",
    "guestEmail": "maria@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (409)** (Se já existe outra reserva não cancelada para essas datas):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is unavailable for these dates"
}
```

### Capacidade Excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Grupo Grande",
    "guestEmail": "grupo@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (422)**:
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

### Operação Não Permitida

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

(Se a reserva já está confirmada)

**Resposta (409)**:
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "This operation is not allowed for this reservation"
}
```

## Modo de Teste

Com `HOTEL_TEST_MODE=true`, uma confirmação de reserva resulta em:

```json
{
  "id": 1,
  "channel": "EMAIL",
  "to": "testes@pousadamarealta.com.br",
  "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
  "body": "Destinatário original: carla@example.com\n\nOlá, Carla Dias!\n...",
  "status": "SENT"
}
```

Sem a cópia para arquivo.

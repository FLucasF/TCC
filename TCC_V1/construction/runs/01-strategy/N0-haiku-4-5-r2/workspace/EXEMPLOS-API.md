# Exemplos de Uso da API - Pousada Maré Alta

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

## 2. Criar Reserva STANDARD

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
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
  "roomNumber": 101,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
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

## 3. Criar Reserva CORPORATE (Tecnosul)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@tecnosul.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

Resposta:
```json
{
  "id": 2,
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@tecnosul.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "CORPORATE",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 93.58,
  "total": 686.22,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## 4. Criar Reserva WEEKEND_PACKAGE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 3,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

Resposta:
```json
{
  "id": 3,
  "roomNumber": 201,
  "guestName": "Maria Santos",
  "guestEmail": "maria@example.com",
  "guestCpf": "98765432109",
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14",
  "guests": 3,
  "ratePlan": "WEEKEND_PACKAGE",
  "nights": 2,
  "subtotal": 820.00,
  "discount": 0.00,
  "total": 820.00,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## 5. Criar Reserva LONG_STAY

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }'
```

Resposta:
```json
{
  "id": 4,
  "roomNumber": 102,
  "guestName": "Pedro Costa",
  "guestEmail": "pedro@example.com",
  "guestCpf": "55544433322",
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-08",
  "guests": 1,
  "ratePlan": "LONG_STAY",
  "nights": 7,
  "subtotal": 1749.30,
  "discount": 174.93,
  "total": 1574.37,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## 6. Obter Detalhes da Reserva

```bash
curl http://localhost:7070/reservations/1
```

## 7. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 499.80
  }'
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80,
  "status": "CONFIRMED",
  "amountPaid": 499.80,
  "refund": null
}
```

Neste momento, um e-mail é enviado (armazenado) para carla@example.com com o comprovante.

## 8. Check-In

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json"
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80,
  "status": "CHECKED_IN",
  "amountPaid": 499.80,
  "refund": null
}
```

## 9. Check-Out (Retorna Comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json"
```

Resposta:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "11122233344",
  "roomNumber": 101,
  "roomType": "STANDARD",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80
}
```

## 10. Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

Resposta: (Mesmo formato do check-out)

## 11. Cancelar Reserva

Antes de check-in (PENDING ou CONFIRMED):

```bash
curl -X POST http://localhost:7070/reservations/2/cancellation \
  -H "Content-Type: application/json"
```

Se foi PENDING: sem reembolso (refund = null)
Se foi CONFIRMED:
- STANDARD: reembolso total
- NON_REFUNDABLE: sem reembolso (refund = 0)
- CORPORATE: reembolso total
- WEEKEND_PACKAGE: 50% de reembolso
- LONG_STAY: reembolso total

## 12. Listar E-mails Enviados

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
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 101 (STANDARD)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 499,80\nTotal: R$ 499,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"
  }
]
```

## 13. Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
201,DELUXE,2,2027-03-10,2027-03-12
total,2/5
```

## 14. Relatório Financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-03
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80
2,201,2027-03-10,2027-03-12,CONFIRMED,686.22
4,102,2027-03-01,2027-03-08,PENDING,0.00
total,1186.02
```

## Códigos de Erro

### 400 Bad Request (INVALID_REQUEST)
- Campo obrigatório faltando
- Formato de data inválido
- Plano de tarifa inválido
- Empresa não reconhecida para CORPORATE
- WEEKEND_PACKAGE em datas inválidas
- LONG_STAY com menos de 7 noites

### 404 Not Found
- ROOM_NOT_FOUND: Quarto não existe
- RESERVATION_NOT_FOUND: Reserva não existe

### 409 Conflict
- ROOM_UNAVAILABLE: Quarto já reservado para essas datas
- OPERATION_NOT_ALLOWED: Operação incompatível com estado da reserva

### 422 Unprocessable Entity
- CAPACITY_EXCEEDED: Mais hóspedes que a capacidade do quarto

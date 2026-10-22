# Exemplos de Uso da API - Pousada Maré Alta

## 1. Consultar Quartos

```bash
curl http://localhost:7070/rooms | jq .
```

**Resposta:**
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
  }' | jq .
```

**Resposta:**
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
  "discount": 0.00,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## 3. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq .
```

**Resposta:**
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
  "discount": 0.00,
  "total": 779.80,
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  "refund": null
}
```

## 4. Consultar Mensagens Enviadas

```bash
curl http://localhost:7070/messages | jq .
```

**Resposta:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"
  }
]
```

## 5. Criar Reserva com CNPJ

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Ana Souza",
    "guestEmail": "ana@company.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

**Resposta (resumida):**
```json
{
  "id": 2,
  "roomNumber": 301,
  "guestName": "Ana Souza",
  "guestEmail": "ana@company.com",
  "guestCpf": null,
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "status": "PENDING",
  "nights": 2,
  "subtotal": 1159.80,
  "total": 1159.80,
  ...
}
```

## 6. Confirmar e Visualizar Documento (Fatura)

```bash
curl -X POST http://localhost:7070/reservations/2/confirmation | jq .
```

```bash
curl http://localhost:7070/reservations/2/document | jq .
```

**Resposta com Fatura:**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 2,
  "guestName": "Ana Souza",
  "guestCpf": null,
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "guestPassport": null,
  "guestCountry": null,
  "roomNumber": 301,
  "roomType": "SUITE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 1159.80,
  "discount": 0.00,
  "total": 1159.80,
  "text": "Fatura de hospedagem nº 2\nEmpresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)\nHóspede: Ana Souza\nQuarto: 301 (SUITE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 1.159,80\nTotal: R$ 1.159,80\nISS retido (5%): R$ 57,99"
}
```

## 7. Criar Reserva com Passaporte (Alta Temporada)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

**Resposta (note a alta temporada em julho):**
```json
{
  "id": 3,
  "roomNumber": 102,
  "guestName": "John Smith",
  "guestEmail": "john@example.com",
  "guestCpf": null,
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
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

## 8. Confirmar e Fazer Check-in/Check-out

```bash
curl -X POST http://localhost:7070/reservations/3/confirmation | jq .
```

```bash
curl -X POST http://localhost:7070/reservations/3/check-in | jq .
```

```bash
curl -X POST http://localhost:7070/reservations/3/check-out | jq .
```

**Resposta do Check-out com Booking Receipt:**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 3,
  "guestName": "John Smith",
  "guestCpf": null,
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": "X1234567",
  "guestCountry": "United Kingdom",
  "roomNumber": 102,
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76,
  "text": "Booking receipt no. 3\nGuest: John Smith (passport X1234567, United Kingdom)\nRoom: 102 (STANDARD)\nCheck-in: 2027-07-10 | Check-out: 2027-07-12 | 2 nights\nNights: BRL 599.76\nTotal: BRL 599.76"
}
```

## 9. Criar Reserva com Desconto NON_REFUNDABLE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Silva",
    "guestEmail": "maria@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-07-15",
    "checkOut": "2027-07-17",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
```

**Resposta (com 15% de desconto em alta temporada):**
```json
{
  "id": 4,
  "roomNumber": 201,
  "guestName": "Maria Silva",
  "guestEmail": "maria@example.com",
  "guestCpf": "555.666.777-88",
  "checkIn": "2027-07-15",
  "checkOut": "2027-07-17",
  "guests": 2,
  "ratePlan": "NON_REFUNDABLE",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 116.97,
  "total": 662.83,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## 10. Cancelar Reserva

### Reembolsável (STANDARD)
```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq .
```

**Resposta:**
```json
{
  "id": 1,
  "status": "CANCELLED",
  "amountPaid": 779.80,
  "refund": 779.80
}
```

### Não Reembolsável (NON_REFUNDABLE)
```bash
curl -X POST http://localhost:7070/reservations/4/confirmation | jq .
curl -X POST http://localhost:7070/reservations/4/cancellation | jq .
```

**Resposta:**
```json
{
  "id": 4,
  "status": "CANCELLED",
  "amountPaid": 662.83,
  "refund": null
}
```

## 11. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10" -H "Accept: text/csv"
```

**Resposta:**
```
room,type,reservation,check_in,check_out
102,STANDARD,3,2027-07-10,2027-07-12
total,1/5
```

## 12. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07" -H "Accept: text/csv"
```

**Resposta:**
```
reservation,room,check_in,check_out,status,amount
3,102,2027-07-10,2027-07-12,CHECKED_OUT,599.76
total,,,546.41
```

## Códigos de Erro

| Erro | Código HTTP | Cenário |
|------|-------------|---------|
| INVALID_REQUEST | 400 | Campo faltando, formato inválido, etc |
| ROOM_NOT_FOUND | 404 | Quarto não existe |
| RESERVATION_NOT_FOUND | 404 | Reserva não existe |
| ROOM_UNAVAILABLE | 409 | Quarto já reservado para as datas |
| OPERATION_NOT_ALLOWED | 409 | Estado da reserva não permite a operação |
| CAPACITY_EXCEEDED | 422 | Mais hóspedes que a capacidade do quarto |

## Fluxos Comuns

### Hóspede Brasileiro (CPF)
1. POST /reservations com guestCpf
2. POST /reservations/{id}/confirmation
3. POST /reservations/{id}/check-in
4. POST /reservations/{id}/check-out

### Empresa (CNPJ)
1. POST /reservations com companyCnpj + companyName
2. POST /reservations/{id}/confirmation
3. POST /reservations/{id}/check-in
4. POST /reservations/{id}/check-out

### Estrangeiro (Passaporte)
1. POST /reservations com guestPassport + guestCountry
2. POST /reservations/{id}/confirmation
3. POST /reservations/{id}/check-in
4. POST /reservations/{id}/check-out

### Segunda Via
```bash
GET /reservations/{id}/document
```

### Cancelamento
```bash
POST /reservations/{id}/cancellation
```

Reembolso automático se STANDARD + CONFIRMED, zero reembolso se NON_REFUNDABLE.

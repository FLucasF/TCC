# Exemplos de Uso da API

Assumindo que a API está rodando em `http://localhost:7070`.

## 1. Listar Quartos

```bash
curl http://localhost:7070/rooms | jq
```

Resposta:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.9
  },
  {
    "number": 102,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.9
  },
  {
    "number": 201,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.9
  },
  {
    "number": 202,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.9
  },
  {
    "number": 301,
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.9
  }
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
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta:
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
  "subtotal": 779.8,
  "discount": 0.0,
  "total": 779.8,
  "status": "PENDING",
  "amountPaid": 0.0,
  "refund": null
}
```

## 3. Obter Reserva

```bash
curl http://localhost:7070/reservations/1 | jq
```

## 4. Confirmar Reserva (Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

Resposta:
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
  "subtotal": 779.8,
  "discount": 0.0,
  "total": 779.8,
  "status": "CONFIRMED",
  "amountPaid": 779.8,
  "refund": null
}
```

## 5. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

## 6. Check-out (com comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
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
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 779.8,
  "discount": 0.0,
  "total": 779.8
}
```

## 7. Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document | jq
```

## 8. Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages | jq
```

Resposta (após confirmação da reserva):
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"
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

## 9. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## 10. Relatório de Receita

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,,,,779.80
```

## 11. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq
```

Resposta (reserva confirmada, plano STANDARD):
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
  "subtotal": 779.8,
  "discount": 0.0,
  "total": 779.8,
  "status": "CANCELLED",
  "amountPaid": 779.8,
  "refund": 779.8
}
```

## Códigos de Erro

### 400 Bad Request (INVALID_REQUEST)
- Falta de campo obrigatório
- Corpo inválido
- Data ou mês em formato incorreto
- Checkout antes de checkin
- Menos de 1 hóspede

### 404 Not Found
- **ROOM_NOT_FOUND**: Quarto não existe
- **RESERVATION_NOT_FOUND**: Reserva não existe

### 409 Conflict
- **ROOM_UNAVAILABLE**: Quarto já reservado nas datas solicitadas
- **OPERATION_NOT_ALLOWED**: Operação incompatível com status da reserva

### 422 Unprocessable Entity
- **CAPACITY_EXCEEDED**: Número de hóspedes excede capacidade do quarto

## Exemplo: Fluxo Completo

```bash
# 1. Criar reserva
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }')

RESERVATION_ID=$(echo $RESERVATION | jq .id)
echo "Reserva criada: $RESERVATION_ID"

# 2. Confirmar pagamento
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation | jq .status

# 3. Check-in
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-in | jq .status

# 4. Check-out
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-out | jq .total

# 5. Ver e-mails enviados
curl -s http://localhost:7070/messages | jq '.[].subject'
```

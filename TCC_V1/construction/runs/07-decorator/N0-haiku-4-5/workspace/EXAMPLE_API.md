# Exemplos de Uso da API

Assumindo que o sistema está rodando em `http://localhost:7070`

## 1. Listar quartos

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

## 2. Criar reserva

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

## 3. Confirmar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

Resposta: reserva com status CONFIRMED, amountPaid = total

## 4. Listar mensagens enviadas

```bash
curl http://localhost:7070/messages | jq
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  }
]
```

## 5. Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

Resposta: reserva com status CHECKED_IN

## 6. Fazer check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

Resposta: comprovante
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
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

```bash
curl http://localhost:7070/reservations/1/document | jq
```

## 8. Relatório de ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## 9. Relatório financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-03
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,779.80
```

## 10. Criar reserva com plano NON_REFUNDABLE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq
```

Resposta:
```json
{
  "id": 2,
  "roomNumber": 301,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "55566677788",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 3,
  "ratePlan": "NON_REFUNDABLE",
  "nights": 2,
  "subtotal": 1439.76,
  "discount": 215.96,
  "total": 1223.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

Note: `subtotal` = 2 noites * 579.90 * 1.20 (alta temporada em julho) = 1439.76
`discount` = 1439.76 * 15% = 215.96
`total` = 1439.76 - 215.96 = 1223.80

## 11. Erro: E-mail inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana",
    "guestEmail": "ana@gmail",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-16",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta: 201 (criada)
Quando confirma, a mensagem será BLOCKED, não SENT

## 12. Cancelar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq
```

Resposta: reserva com status CANCELLED, refund = amountPaid (para STANDARD) ou 0 (para NON_REFUNDABLE)

## 13. Erro: Quarto não encontrado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta (404):
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found"
}
```

## 14. Modo de teste (HOTEL_TEST_MODE=true)

Se você rodar com `HOTEL_TEST_MODE=true`, ao criar uma reserva e confirmar:

```bash
curl http://localhost:7070/messages | jq
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "testes@pousadamarealta.com.br",
    "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
    "body": "Destinatário original: carla@example.com\nOlá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  }
]
```

Note:
- E-mail redirecionado para `testes@pousadamarealta.com.br`
- Assunto começa com `[TESTE]`
- Primeira linha do corpo: "Destinatário original: carla@example.com"
- Nenhuma cópia para arquivo (mesmo que habilitada)

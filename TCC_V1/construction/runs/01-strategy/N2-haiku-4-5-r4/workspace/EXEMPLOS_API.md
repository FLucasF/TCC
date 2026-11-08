# Exemplos de Uso da API - Pousada Maré Alta

## Listar Quartos

```bash
curl -s http://localhost:7070/rooms | jq
```

Resposta esperada:
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

## Exemplo 1: Reserva Standard (Tarifa Normal)

### Criar Reserva

Standard de 10/07/2027 a 12/07/2027 (2 noites em alta temporada)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta esperada:
- Subtotal: 2 × 249,90 × 1,20 = 599,76
- Desconto: 0
- Total: 599,76

```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

### Confirmar Pagamento

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": 599.76}' | jq
```

Status muda para `CONFIRMED` e `amountPaid` fica 599.76. Um e-mail é enviado (salvo em messages).

### Verificar Mensagens Enviadas

```bash
curl -s http://localhost:7070/messages | jq
```

A mensagem contém o comprovante formatado.

## Exemplo 2: Convênio Tecnosul (CORPORATE)

Deluxe de 10/03/2027 a 12/03/2027 (2 noites em março, sem alta temporada)

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
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }' | jq
```

Resposta esperada:
- Subtotal: 2 × 389,90 = 779,80
- Desconto: 779,80 × 0,12 = 93,58
- Total: 686,22

```json
{
  "id": 2,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "111.222.333-44",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "CORPORATE",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 93.58,
  "total": 686.22,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

## Exemplo 3: Pacote Fim de Semana (WEEKEND_PACKAGE)

Deluxe, sexta 12/03/2027 a domingo 14/03/2027

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }' | jq
```

Resposta esperada:
- Subtotal: 820,00 (preço fixo)
- Desconto: 0
- Total: 820,00

```json
{
  "id": 3,
  "roomNumber": 202,
  "guestName": "Ana Costa",
  "guestEmail": "ana@example.com",
  "guestCpf": "555.666.777-88",
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14",
  "guests": 2,
  "ratePlan": "WEEKEND_PACKAGE",
  "nights": 2,
  "subtotal": 820.00,
  "discount": 0,
  "total": 820.00,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

### Confirmar e depois Cancelar

```bash
# Confirmar
curl -X POST http://localhost:7070/reservations/3/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": 820.00}' | jq

# Cancelar (reembolso 50%)
curl -X POST http://localhost:7070/reservations/3/cancellation \
  -H "Content-Type: application/json" \
  -d '{}' | jq
```

Resposta esperada após cancelamento:
- Status: `CANCELLED`
- Refund: 820,00 × 0,50 = 410,00

## Exemplo 4: Estadia Longa (LONG_STAY)

Standard de 01/03/2027 a 08/03/2027 (7 noites em março)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Paulo Mendes",
    "guestEmail": "paulo@example.com",
    "guestCpf": "999.000.111-22",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }' | jq
```

Resposta esperada:
- Subtotal: 7 × 249,90 = 1749,30
- Desconto: 1749,30 × 0,10 = 174,93
- Total: 1574,37

```json
{
  "id": 4,
  "roomNumber": 102,
  "guestName": "Paulo Mendes",
  "guestEmail": "paulo@example.com",
  "guestCpf": "999.000.111-22",
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-08",
  "guests": 1,
  "ratePlan": "LONG_STAY",
  "nights": 7,
  "subtotal": 1749.30,
  "discount": 174.93,
  "total": 1574.37,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

## Exemplo 5: Plano Não Reembolsável (NON_REFUNDABLE)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "333.444.555-66",
    "checkIn": "2027-07-15",
    "checkOut": "2027-07-17",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq
```

Resposta esperada:
- Subtotal: 2 × 579,90 × 1,20 = 1391,76
- Desconto: 1391,76 × 0,15 = 208,76
- Total: 1183,00

## Fluxo Completo: Reserva → Confirmação → Check-in → Check-out

```bash
# 1. Criar reserva
RESERVATION_ID=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Carlos oliveira",
    "guestEmail": "carlos@example.com",
    "guestCpf": "777.888.999-00",
    "checkIn": "2027-07-20",
    "checkOut": "2027-07-22",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')

# 2. Confirmar pagamento
curl -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": 599.76}' | jq '.status'

# 3. Check-in
curl -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.status'

# 4. Check-out
curl -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq '.status'

# 5. Obter documento (segunda via)
curl -s http://localhost:7070/reservations/$RESERVATION_ID/document | jq
```

## Relatórios

### Ocupação por Data

```bash
curl -s http://localhost:7070/reports/occupancy?date=2027-07-10
```

Resposta esperada (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### Receita por Mês

```bash
curl -s http://localhost:7070/reports/revenue?month=2027-07
```

Resposta esperada (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,,,,,599.76
```

## Tratamento de Erros

### Quarto não existe

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "000.000.000-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada (404):
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Quarto não encontrado"
}
```

### Capacidade excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "000.000.000-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada (422):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Número de hóspedes excede a capacidade"
}
```

### Código de empresa inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "000.000.000-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "INVALID"
  }'
```

Resposta esperada (400):
```json
{
  "error": "INVALID_REQUEST",
  "message": "Código de empresa inválido"
}
```

### Datas de fim de semana inválidas para pacote

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "000.000.000-00",
    "checkIn": "2027-03-13",
    "checkOut": "2027-03-15",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

Resposta esperada (400):
```json
{
  "error": "INVALID_REQUEST",
  "message": "Entrada deve ser numa sexta-feira"
}
```

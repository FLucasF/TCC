# Exemplos de Uso da API

Este arquivo contém exemplos práticos de uso do sistema de reservas.

## Cenário 1: Reserva Simples

### 1.1. Listar quartos disponíveis

```bash
curl http://localhost:7070/rooms | jq
```

Resposta:
```json
[
  {
    "number": "101",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": "102",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": "201",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": "202",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": "301",
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

### 1.2. Criar uma reserva (baixa temporada, plano standard)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta (201 Created):
```json
{
  "id": 1,
  "roomNumber": "201",
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

### 1.3. Confirmar a reserva (pagamento recebido)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": "201",
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

### 1.4. Verificar mensagens enviadas

```bash
curl http://localhost:7070/messages | jq
```

Resposta (3 e-mails enviados automaticamente):
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
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
    "body": "R$ 779,80"
  }
]
```

## Cenário 2: Alta Temporada com Desconto

### 2.1. Criar reserva em alta temporada com plano não reembolsável

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq
```

Resposta:
```json
{
  "id": 2,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "NON_REFUNDABLE",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 89.96,
  "total": 509.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

**Observações:**
- Julho inteiro é alta temporada (+20%): R$ 249,90 → R$ 299,88/noite
- 2 noites = R$ 599,76
- Desconto não reembolsável (15%): R$ 89,96
- Total final: R$ 509,80

## Cenário 3: Cancelamento

### 3.1. Cancelar reserva confirmada com reembolso (plano standard)

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": "201",
  "status": "CANCELLED",
  "refund": 779.80,
  ...
}
```

E-mails enviados:
- Governança: "Reserva cancelada - quarto 201"
- Financeiro: "Reembolso - reserva 1" (R$ 779,80)

### 3.2. Cancelar reserva não reembolsável

```bash
curl -X POST http://localhost:7070/reservations/2/confirmation | jq
curl -X POST http://localhost:7070/reservations/2/cancellation | jq
```

Resposta:
```json
{
  "id": 2,
  "status": "CANCELLED",
  "refund": 0.00,
  ...
}
```

E-mails enviados:
- Governança: "Reserva cancelada - quarto 101"
- Financeiro: **nenhum** (não há reembolso)

## Cenário 4: Check-in e Check-out

### 4.1. Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

### 4.2. Fazer check-out (gera comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

Resposta:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "11122233344",
  "roomNumber": "201",
  "roomType": "DELUXE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80
}
```

### 4.3. Obter segunda via do comprovante

```bash
curl http://localhost:7070/reservations/1/document | jq
```

## Cenário 5: Relatórios

### 5.1. Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

### 5.2. Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CONFIRMED,779.80
total,779.80
```

## Cenário 6: Tratamento de Erros

### 6.1. Quarto não existe

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-04-01",
    "checkOut": "2027-04-02",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta (404):
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found: 999"
}
```

### 6.2. Quarto já reservado (conflito)

```bash
# Tentar reservar quarto 201 com datas que conflitam com reserva existente
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Conflito",
    "guestEmail": "conflito@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-11",
    "checkOut": "2027-03-13",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta (409):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room unavailable: 201"
}
```

### 6.3. Capacidade excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Muita Gente",
    "guestEmail": "muita@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-05-01",
    "checkOut": "2027-05-02",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta (422):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Capacity exceeded: 2 < 5"
}
```

### 6.4. Operação não permitida

```bash
# Tentar fazer check-in em reserva pendente
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

Resposta (409):
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Only confirmed reservations can be checked in"
}
```

## Dicas

- Use `| jq` para formatar o JSON de forma legível
- Salve o ID da reserva retornado ao criar para usar nas operações seguintes
- Verifique `/messages` após confirmações e cancelamentos para ver os e-mails enviados
- Os relatórios retornam `text/csv`, salve em arquivo: `curl ... > report.csv`

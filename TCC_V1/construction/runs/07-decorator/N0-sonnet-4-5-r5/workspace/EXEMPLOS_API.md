# Exemplos de Uso da API

## 1. Listar Quartos

```bash
curl http://localhost:7070/rooms
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
    "number": "201",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  ...
]
```

## 2. Criar Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Silva",
    "guestEmail": "ana@exemplo.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta (201 Created):
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "Ana Silva",
  "guestEmail": "ana@exemplo.com",
  "guestCpf": "12345678900",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
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

Nota: Alta temporada em julho aplica 20% adicional:
- Diária base: R$ 249,90
- Com alta temporada: R$ 299,88
- 2 noites: R$ 599,76

## 3. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 599.76,
  ...
}
```

O sistema envia automaticamente um e-mail para o hóspede:
- Assunto: "Reserva confirmada - Pousada Maré Alta"
- Corpo: Saudação + comprovante + instruções de check-in

## 4. Consultar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "ana@exemplo.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Ana Silva!\n\nSua reserva...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Ana Silva!\n\nSua reserva...",
    "status": "SENT"
  }
]
```

## 5. Exemplo de E-mail Bloqueado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "João",
    "guestEmail": "joao.gmail.com",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-02",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

A reserva é criada normalmente, mas ao confirmar:

```bash
curl -X POST http://localhost:7070/reservations/2/confirmation
curl http://localhost:7070/messages
```

Mensagem aparece como BLOCKED:
```json
{
  "id": 3,
  "channel": "EMAIL",
  "to": "joao.gmail.com",
  "subject": "Reserva confirmada - Pousada Maré Alta",
  "body": "...",
  "status": "BLOCKED"
}
```

## 6. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 7. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta (DocumentResponse):
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Ana Silva",
  "guestCpf": "12345678900",
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76
}
```

## 8. Cancelamento

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Plano STANDARD: reembolsa o valor pago
Plano NON_REFUNDABLE: não reembolsa

## 9. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

## 10. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
total,599.76
```

## 11. Plano NON_REFUNDABLE (15% desconto)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Carla Dias",
    "guestEmail": "carla@exemplo.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Cálculo:
- 2 noites × R$ 579,90 = R$ 1.159,80 (subtotal)
- 15% desconto = R$ 173,97
- Total = R$ 985,83

## Códigos de Erro

### 400 INVALID_REQUEST
- Campos obrigatórios faltando
- Formato de data inválido
- Check-out não é depois do check-in
- Menos de 1 hóspede

### 404 ROOM_NOT_FOUND
- Quarto não existe

### 404 RESERVATION_NOT_FOUND
- Reserva não existe

### 409 ROOM_UNAVAILABLE
- Quarto já está reservado nas datas solicitadas

### 409 OPERATION_NOT_ALLOWED
- Operação não permitida no estado atual da reserva

### 422 CAPACITY_EXCEEDED
- Número de hóspedes excede capacidade do quarto

## Modo de Teste

Com `HOTEL_TEST_MODE=true`:

```bash
# Cria e confirma reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Verifica mensagens
curl http://localhost:7070/messages
```

Mensagem é redirecionada:
```json
{
  "id": 1,
  "channel": "EMAIL",
  "to": "testes@pousadamarealta.com.br",
  "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
  "body": "Destinatário original: ana@exemplo.com\nOlá, Ana Silva!...",
  "status": "SENT"
}
```

Sem cópia para arquivo em modo teste.

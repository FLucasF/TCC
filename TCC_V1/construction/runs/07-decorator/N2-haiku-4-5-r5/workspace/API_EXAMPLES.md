# Exemplos de Requisições - Pousada Maré Alta

## Inicializar Servidor

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="sua_senha"
export PORT="7070"
java -jar target/hotel-system.jar
```

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

## 2. Criar Reserva (Plano STANDARD)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678900",
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

## 3. Criar Reserva (Plano NON_REFUNDABLE em Alta Temporada)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
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
  "guestEmail": "maria@example.com",
  "guestCpf": "98765432100",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 1,
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

## 4. Obter Dados da Reserva

```bash
curl http://localhost:7070/reservations/1
```

## 5. Confirmar Reserva (Fazer Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": "779.80"}'
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678900",
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

A confirmação dispara um e-mail para o hóspede com o comprovante.

## 6. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json"
```

Resposta:
```json
{
  "id": 1,
  ...
  "status": "CHECKED_IN",
  ...
}
```

## 7. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json"
```

Resposta (comprovante em JSON):
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678900",
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

## 8. Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

Resposta (mesma do check-out):
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  ...
}
```

## 9. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation \
  -H "Content-Type: application/json"
```

Para reserva STANDARD confirmada:
```json
{
  "id": 1,
  ...
  "status": "CANCELLED",
  "amountPaid": 779.80,
  "refund": 779.80
}
```

Para reserva NON_REFUNDABLE confirmada:
```json
{
  "id": 2,
  ...
  "status": "CANCELLED",
  "amountPaid": 509.80,
  "refund": 0.00
}
```

## 10. Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!\n\nSua reserva na Pousada Maré Alta está confirmada...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "...",
    "status": "SENT"
  }
]
```

### Com E-mail Inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Ana Costa",
    "guestEmail": "ana@gmail",
    "guestCpf": "11111111100",
    "checkIn": "2027-03-15",
    "checkOut": "2027-03-16",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Depois, ao confirmar:

```bash
curl -X POST http://localhost:7070/reservations/3/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": "579.90"}'
```

A mensagem fica bloqueada na lista:

```json
{
  "id": 3,
  "channel": "EMAIL",
  "to": "ana@gmail",
  "subject": "Reserva confirmada - Pousada Maré Alta",
  "body": "...",
  "status": "BLOCKED"
}
```

## 11. Relatório de Ocupação (CSV)

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta:
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-07-10,2027-07-12
301,SUITE,3,2027-07-10,2027-07-15
total,2/5
```

## 12. Relatório de Receita (CSV)

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta:
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-07-10,2027-07-12,CHECKED_OUT,779.80
2,101,2027-07-10,2027-07-12,CANCELLED,509.80
total,,,,,1289.60
```

## 13. Modo de Teste

```bash
export HOTEL_TEST_MODE=true
java -jar target/hotel-system.jar
```

Com modo de teste ativo, todos os e-mails vão para `testes@pousadamarealta.com.br` com prefixo `[TESTE]`:

```json
{
  "id": 4,
  "channel": "EMAIL",
  "to": "testes@pousadamarealta.com.br",
  "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
  "body": "Destinatário original: joao@example.com\n\nOlá, João Silva!\n\n...",
  "status": "SENT"
}
```

Neste modo, não há cópia para arquivo.

## Códigos de Erro

### 400 - Bad Request (INVALID_REQUEST)
- Falta campo obrigatório
- Data ou mês em formato inválido
- Saída antes da entrada

### 404 - Not Found
- ROOM_NOT_FOUND: quarto não existe
- RESERVATION_NOT_FOUND: reserva não existe

### 409 - Conflict
- ROOM_UNAVAILABLE: quarto já reservado naquela noite
- OPERATION_NOT_ALLOWED: operação não permitida no estado atual

### 422 - Unprocessable Entity
- CAPACITY_EXCEEDED: mais hóspedes que a capacidade do quarto

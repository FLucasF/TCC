# Guia de Testes

## Setup Inicial

### 1. Criar Banco de Dados PostgreSQL

```bash
createdb pousada
```

### 2. Executar a Aplicação

```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="postgres" \
DB_PASSWORD="postgres" \
PORT=7070 \
java -jar target/reservas-1.0.0.jar
```

## Testes Manuais com curl

### 1. Listar Quartos

```bash
curl http://localhost:7070/rooms
```

Esperado: Lista dos 5 quartos com tipo e capacidade

### 2. Criar Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Esperado: 201 com ID de reserva (será 1 na primeira)

### 3. Buscar Reserva

```bash
curl http://localhost:7070/reservations/1
```

Esperado: Status PENDING, amountPaid 0.00

### 4. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 599.76
  }'
```

Esperado: Status CONFIRMED, amountPaid 599.76

### 5. Verificar Mensagens

```bash
curl http://localhost:7070/messages
```

Esperado: 3 mensagens (hóspede, governança, financeiro)

### 6. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}'
```

Esperado: Status CHECKED_IN

### 7. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}'
```

Esperado: Comprovante com dados da reserva

### 8. Segunda Via

```bash
curl http://localhost:7070/reservations/1/document
```

Esperado: Mesmo comprovante do check-out

### 9. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Esperado: CSV mostrando quarto 101 ocupado naquela data

### 10. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Esperado: CSV mostrando reserva 1 com amount 599.76

## Testes de Alta Temporada

Criar reserva de 15/12/2027 a 17/12/2027:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432101",
    "checkIn": "2027-12-15",
    "checkOut": "2027-12-17",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Esperado: Preço = (389.90 * 1.20 * 2) - 15% = (467.88 * 2) - 140.36 = 795.40

## Testes de Erro

### Quarto Inexistente

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Esperado: 404 ROOM_NOT_FOUND

### Quarto Indisponível

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Esperado: 409 ROOM_UNAVAILABLE (pois reserva 1 ocupa essa data)

### Excesso de Capacidade

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Esperado: 422 CAPACITY_EXCEEDED (quarto 101 é para 2 pessoas)

## Testes de Cancelamento

### Cancelar Confirmada (STANDARD)

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation \
  -H "Content-Type: application/json" \
  -d '{}'
```

Esperado: Status CANCELLED, refund = 599.76

### Verificar Notificações

```bash
curl http://localhost:7070/messages
```

Esperado: 2 novas mensagens (governança sobre cancelamento, financeiro sobre reembolso)

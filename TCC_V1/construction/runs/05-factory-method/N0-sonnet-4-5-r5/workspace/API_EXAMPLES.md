# Exemplos de Uso da API

## Setup Inicial

1. Inicie o PostgreSQL:
```bash
docker-compose up -d
```

2. Inicie o servidor:
```bash
./start.sh
```

Ou manualmente:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="postgres"
java -jar target/pousada-mare-alta-1.0.0.jar
```

## Exemplos de Chamadas

### 1. Listar Quartos

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

### 2. Criar Reserva com CPF (Pessoa Física)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "123.456.789-00",
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
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@email.com",
  "guestCpf": "123.456.789-00",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
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

### 3. Criar Reserva com CNPJ (Empresa)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@empresa.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq
```

Resposta:
```json
{
  "id": 2,
  "roomNumber": "201",
  "guestName": "Ana Souza",
  "guestEmail": "ana@empresa.com",
  "guestCpf": null,
  "companyCnpj": "12.345.678/0001-90",
  "companyName": "Tecnosul Ltda",
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
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

### 4. Criar Reserva com Passaporte (Estrangeiro)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@email.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }' | jq
```

### 5. Criar Reserva em Alta Temporada (Julho)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Maria Costa",
    "guestEmail": "maria@email.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta (note o subtotal de R$ 599,76 devido à alta temporada):
```json
{
  "id": 3,
  "subtotal": 599.76,
  "total": 599.76,
  "status": "PENDING"
}
```

### 6. Consultar Reserva

```bash
curl http://localhost:7070/reservations/1 | jq
```

### 7. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

Resposta:
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 499.80,
  ...
}
```

### 8. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

### 9. Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

Resposta (documento/comprovante):
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "123.456.789-00",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80,
  "text": "Comprovante de reserva nº 1\nHóspede: João Silva (CPF 123.456.789-00)\nQuarto: 101 (STANDARD)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 499,80\nTotal: R$ 499,80"
}
```

### 10. Obter Segunda Via do Documento

```bash
curl http://localhost:7070/reservations/1/document | jq
```

### 11. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq
```

Resposta (STANDARD com reembolso):
```json
{
  "id": 1,
  "status": "CANCELLED",
  "refund": 499.80,
  ...
}
```

### 12. Listar E-mails Enviados

```bash
curl http://localhost:7070/messages | jq
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@email.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  }
]
```

### 13. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
201,DELUXE,2,2027-03-10,2027-03-12
301,SUITE,3,2027-03-10,2027-03-12
total,3/5
```

### 14. Relatório de Receita

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CONFIRMED,499.80
2,201,2027-03-10,2027-03-12,CONFIRMED,662.83
3,301,2027-03-10,2027-03-12,CONFIRMED,1159.80
total,,,,,2322.43
```

## Cenários de Teste Completos

### Cenário 1: Reserva Normal (CPF)

```bash
# 1. Criar reserva
RESERVATION_ID=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Pedro Alves",
    "guestEmail": "pedro@email.com",
    "guestCpf": "222.333.444-55",
    "checkIn": "2027-04-15",
    "checkOut": "2027-04-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')

echo "Reserva criada: $RESERVATION_ID"

# 2. Confirmar
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation" | jq '.status'

# 3. Check-in
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/check-in" | jq '.status'

# 4. Check-out
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/check-out" | jq '.text'
```

### Cenário 2: Reserva com Empresa (CNPJ)

```bash
RESERVATION_ID=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "Carlos Lima",
    "guestEmail": "carlos@empresa.com",
    "companyCnpj": "98.765.432/0001-10",
    "companyName": "Empresa ABC Ltda",
    "checkIn": "2027-05-20",
    "checkOut": "2027-05-22",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')

# Confirmar e obter documento (fatura)
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation" > /dev/null
curl -s "http://localhost:7070/reservations/$RESERVATION_ID/document" | jq '.text'
```

### Cenário 3: Cancelamento com Reembolso

```bash
RESERVATION_ID=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Lucia Ferreira",
    "guestEmail": "lucia@email.com",
    "guestCpf": "444.555.666-77",
    "checkIn": "2027-06-10",
    "checkOut": "2027-06-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')

# Confirmar
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation" > /dev/null

# Cancelar e verificar reembolso
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/cancellation" | jq '{status, refund}'
```

### Cenário 4: Cancelamento Não Reembolsável

```bash
RESERVATION_ID=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Roberto Santos",
    "guestEmail": "roberto@email.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq -r '.id')

# Confirmar
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation" > /dev/null

# Cancelar - sem reembolso
curl -s -X POST "http://localhost:7070/reservations/$RESERVATION_ID/cancellation" | jq '{status, refund, total}'
```

## Tratamento de Erros

### Quarto Inexistente (404)
```bash
curl -i -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "Test",
    "guestEmail": "test@test.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta:
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found"
}
```

### Capacidade Excedida (422)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Test",
    "guestEmail": "test@test.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta:
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

### Quarto Indisponível (409)
```bash
# Primeiro criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "First Guest",
    "guestEmail": "first@email.com",
    "guestCpf": "111.111.111-11",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-15",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' > /dev/null

# Tentar criar outra no mesmo período
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Second Guest",
    "guestEmail": "second@email.com",
    "guestCpf": "222.222.222-22",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

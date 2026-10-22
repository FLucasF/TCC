# Guia de Testes - Pousada Maré Alta

## Preparação do Ambiente de Teste

### 1. Iniciar PostgreSQL com Docker

```bash
docker run --name pousada-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=pousada \
  -p 5432:5432 \
  -d postgres:18
```

### 2. Compilar e Executar a Aplicação

```bash
mvn clean package

export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha123
export PORT=7070

java -jar target/app.jar &
sleep 3
```

## Testes Básicos

### 1. Listar Quartos

```bash
curl -s http://localhost:7070/rooms | jq .
```

Resposta esperada:
```json
[
  {"number": 101, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 102, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 201, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 202, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 301, "type": "SUITE", "capacity": 4, "nightlyRate": 579.90}
]
```

### 2. Criar Reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "roomNumber": 201,
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

Resposta esperada: Reserva com `id: 1`, status `PENDING`, sem pagamento.

### 3. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}' | jq .
```

### 4. Verificar Mensagens Enviadas

```bash
curl -s http://localhost:7070/messages | jq .
```

Deve conter o email de confirmação da reserva.

### 5. Obter Segunda Via do Documento

```bash
curl -s http://localhost:7070/reservations/1/document | jq .
```

### 6. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

### 7. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

Retorna o documento do check-out.

### 8. Relatório de Ocupação

```bash
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-11"
```

### 9. Relatório Financeiro

```bash
curl -s "http://localhost:7070/reports/revenue?month=2027-03"
```

## Testes Avançados

### Reserva com CNPJ

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Ana Souza",
    "guestEmail": "ana@empresa.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "roomNumber": 201,
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

### Reserva com Passaporte

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "John Smith",
    "guestEmail": "john@email.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "roomNumber": 301,
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
```

### Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/2/cancellation \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

## Validações de Erro

### Quarto Não Encontrado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Test",
    "guestEmail": "test@email.com",
    "guestCpf": "11122233344",
    "roomNumber": 999,
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada: `404 ROOM_NOT_FOUND`

### Capacidade Excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Test",
    "guestEmail": "test@email.com",
    "guestCpf": "11122233344",
    "roomNumber": 101,
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada: `422 CAPACITY_EXCEEDED`

### Quarto Indisponível

Fazer duas reservas para o mesmo quarto e datas:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Guest1",
    "guestEmail": "guest1@email.com",
    "guestCpf": "11122233344",
    "roomNumber": 201,
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .id
```

Confirmar a primeira:

```bash
curl -X POST http://localhost:7070/reservations/3/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'
```

Tentar criar segunda (deve falhar):

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Guest2",
    "guestEmail": "guest2@email.com",
    "guestCpf": "11122233344",
    "roomNumber": 201,
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada: `409 ROOM_UNAVAILABLE`

## Exemplo de Fluxo Completo

```bash
#!/bin/bash

# 1. Criar reserva
RESPONSE=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "Maria Silva",
    "guestEmail": "maria@email.com",
    "guestCpf": "12345678900",
    "roomNumber": 101,
    "checkIn": "2027-08-15",
    "checkOut": "2027-08-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')

ID=$(echo $RESPONSE | jq .id)
echo "Reserva criada: $ID"

# 2. Confirmar
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}' | jq .status

# 3. Check-in
curl -s -X POST http://localhost:7070/reservations/$ID/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq .status

# 4. Check-out
curl -s -X POST http://localhost:7070/reservations/$ID/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq .title

# 5. Ver relatório
curl -s "http://localhost:7070/reports/occupancy?date=2027-08-16"
```

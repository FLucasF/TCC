# Exemplos de Uso da API

## Listar Quartos

```bash
curl http://localhost:7070/rooms
```

## Criar Reserva com CPF

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
    "ratePlan": "STANDARD"
  }'
```

## Criar Reserva com CNPJ (Empresa)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Criar Reserva com Passaporte (Estrangeiro)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "202",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## Consultar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

## Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Check-out (retorna documento)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Obter Segunda Via do Documento

```bash
curl http://localhost:7070/reservations/1/document
```

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

## Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

## Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

## Exemplo Completo: Fluxo de Reserva

```bash
# 1. Criar reserva
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-03",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

ID=$(echo $RESERVATION | jq -r '.id')
echo "Reserva criada: $ID"

# 2. Confirmar reserva
curl -X POST http://localhost:7070/reservations/$ID/confirmation

# 3. Fazer check-in
curl -X POST http://localhost:7070/reservations/$ID/check-in

# 4. Fazer check-out
curl -X POST http://localhost:7070/reservations/$ID/check-out

# 5. Ver mensagens enviadas
curl http://localhost:7070/messages | jq
```

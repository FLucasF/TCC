# Guia de Testes - API de Reservas

## Pré-requisitos

1. PostgreSQL 18 rodando
2. Banco de dados criado (veja README.md)
3. Aplicação rodando em http://localhost:7070

## Exemplos de Requisições

### 1. Listar Quartos

```bash
curl http://localhost:7070/rooms
```

Resposta esperada:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

### 2. Criar Reserva (STANDARD)

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
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada (201):
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  ...
  "status": "PENDING",
  "amountPaid": 0
}
```

### 3. Criar Reserva (CORPORATE - TECNOSUL)

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
    "guests": 1,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

Esperado: Preço com 12% de desconto

### 4. Criar Reserva (WEEKEND_PACKAGE)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

Esperado: Preço fixo de R$ 820,00

### 5. Criar Reserva (LONG_STAY)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "999.888.777-66",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }'
```

Esperado: 7 noites com 10% de desconto

### 6. Obter Reserva

```bash
curl http://localhost:7070/reservations/1
```

### 7. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 599.76
  }'
```

Resposta: Reserva com status "CONFIRMED" e email enviado (visível em /messages)

### 8. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

Resposta: Reserva com status "CHECKED_IN"

### 9. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta: Comprovante de saída

### 10. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Resposta: Reserva com status "CANCELLED" e refund calculado

### 11. Obter Segunda Via de Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

(Apenas funciona para reservas pagas)

### 12. Listar Mensagens

```bash
curl http://localhost:7070/messages
```

Resposta: Lista de emails enviados

### 13. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### 14. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,,,,,599.76
```

## Testes de Validação

### Erro: Quarto não existe

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "000.000.000-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Esperado: 404 ROOM_NOT_FOUND

### Erro: Capacidade excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "000.000.000-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Esperado: 422 CAPACITY_EXCEEDED

### Erro: CORPORATE sem empresa válida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "000.000.000-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "CORPORATE",
    "companyCode": "EMPRESA_INVALIDA"
  }'
```

Esperado: 400 INVALID_REQUEST

### Erro: Quarto já reservado

```bash
# Primeira reserva (sucesso)
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Pessoa 1",
    "guestEmail": "p1@example.com",
    "guestCpf": "111.111.111-11",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'

# Segunda reserva na mesma data (erro)
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Pessoa 2",
    "guestEmail": "p2@example.com",
    "guestCpf": "222.222.222-22",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Esperado: 409 ROOM_UNAVAILABLE

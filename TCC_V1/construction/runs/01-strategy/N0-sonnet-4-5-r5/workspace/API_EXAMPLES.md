# Exemplos de Requisições da API

## Listar Quartos

```bash
curl http://localhost:7070/rooms
```

## Criar Reserva - Plano Standard

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
  }'
```

## Criar Reserva - Plano Corporativo

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

## Criar Reserva - Pacote de Fim de Semana

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

## Criar Reserva - Estadia Longa

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Pedro Alves",
    "guestEmail": "pedro@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }'
```

## Criar Reserva - Alta Temporada

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
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

## Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Obter Comprovante (Segunda Via)

```bash
curl http://localhost:7070/reservations/1/document
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

## Casos de Teste

### Teste 1: Reserva Deluxe fora de Alta Temporada com Tecnosul

```bash
# Criar reserva
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
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'

# Resultado esperado:
# nights: 2
# subtotal: 779.80
# discount: 93.58
# total: 686.22
```

### Teste 2: Pacote Deluxe com Cancelamento

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "José Oliveira",
    "guestEmail": "jose@example.com",
    "guestCpf": "22233344455",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'

# Confirmar
curl -X POST http://localhost:7070/reservations/2/confirmation

# Cancelar
curl -X POST http://localhost:7070/reservations/2/cancellation

# Resultado esperado:
# total: 820.00
# amountPaid: 820.00
# refund: 410.00
```

### Teste 3: Estadia Longa Standard

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ricardo Souza",
    "guestEmail": "ricardo@example.com",
    "guestCpf": "33344455566",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }'

# Resultado esperado:
# nights: 7
# subtotal: 1749.30
# discount: 174.93
# total: 1574.37
```

## Testes de Erro

### Quarto Não Existe

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Resultado esperado: 404 ROOM_NOT_FOUND
```

### Capacidade Excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'

# Resultado esperado: 422 CAPACITY_EXCEEDED
```

### Pacote de Fim de Semana em Dia Errado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'

# Resultado esperado: 400 INVALID_REQUEST
```

### Código de Empresa Inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "INVALID"
  }'

# Resultado esperado: 400 INVALID_REQUEST
```

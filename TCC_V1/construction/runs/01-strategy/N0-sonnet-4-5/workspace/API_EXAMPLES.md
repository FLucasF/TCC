# Exemplos de Uso da API

## Listar Quartos

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
    "number": 201,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  ...
]
```

## Criar Reserva

### Plano Standard (Normal)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Plano Não Reembolsável

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

### Plano Corporativo

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carlos Dias",
    "guestEmail": "carlos@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

Empresas disponíveis:
- `TECNOSUL` - 12% de desconto
- `AGROVALE` - 18% de desconto
- `FARMAVIDA` - 25% de desconto

### Pacote de Fim de Semana

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "444.555.666-77",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

Obs: Entrada deve ser sexta-feira e saída domingo (exatamente 2 noites).

### Estadia Longa

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Pedro Alves",
    "guestEmail": "pedro@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }'
```

Obs: Mínimo de 7 noites.

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

Retorna o comprovante da reserva.

## Obter Segunda Via do Comprovante

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
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

Retorna CSV:
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
201,DELUXE,2,2027-03-10,2027-03-12
total,2/5
```

## Relatório Financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-03
```

Retorna CSV:
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CONFIRMED,499.80
2,201,2027-03-10,2027-03-12,CONFIRMED,662.83
total,1162.63
```

## Tratamento de Erros

### 400 INVALID_REQUEST
- Dados faltando ou inválidos
- Formato de data incorreto
- Violação de regras do plano de tarifa

### 404 ROOM_NOT_FOUND
- Quarto não existe

### 404 RESERVATION_NOT_FOUND
- Reserva não existe

### 409 ROOM_UNAVAILABLE
- Quarto já reservado para as datas

### 409 OPERATION_NOT_ALLOWED
- Operação não permitida no estado atual da reserva

### 422 CAPACITY_EXCEEDED
- Número de hóspedes excede capacidade do quarto

Exemplo de erro:
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Room capacity exceeded"
}
```

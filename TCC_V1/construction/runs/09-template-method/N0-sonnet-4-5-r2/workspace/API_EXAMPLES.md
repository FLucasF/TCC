# Exemplos de Uso da API

## Listar Quartos

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
    "number": "102",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

## Criar Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
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
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
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

## Confirmar Reserva (Pagar)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": "201",
  ...
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  "refund": null
}
```

## Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta (documento):
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

## Obter Comprovante (Segunda Via)

```bash
curl http://localhost:7070/reservations/1/document
```

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Consultar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

## Relatórios

### Ocupação de uma data

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```csv
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,5,2027-07-10,2027-07-12
201,DELUXE,8,2027-07-09,2027-07-11
total,2/5
```

### Relatório Financeiro de um mês

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

### Taxa de Turismo

```bash
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"
```

### Comparação de Planos de Tarifa

```bash
curl "http://localhost:7070/reports/rate-plans?month=2027-07"
```

## Exemplo: Reserva com desconto (Não Reembolsável)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resposta (com alta temporada + desconto):
```json
{
  "id": 2,
  "roomNumber": "101",
  "guestName": "João Silva",
  ...
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

## Códigos de Erro

- `400 INVALID_REQUEST`: Requisição inválida (campo faltando, formato incorreto)
- `404 ROOM_NOT_FOUND`: Quarto não encontrado
- `404 RESERVATION_NOT_FOUND`: Reserva não encontrada
- `409 ROOM_UNAVAILABLE`: Quarto indisponível nas datas selecionadas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no status atual
- `422 CAPACITY_EXCEEDED`: Número de hóspedes excede capacidade do quarto

Exemplo de erro:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

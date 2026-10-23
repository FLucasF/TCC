# Exemplos de uso da API

## Listar quartos

```bash
curl http://localhost:7070/rooms
```

## Criar reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Consultar reserva

```bash
curl http://localhost:7070/reservations/1
```

## Confirmar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

## Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Fazer check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Obter comprovante (segunda via)

```bash
curl http://localhost:7070/reservations/1/document
```

## Cancelar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Listar mensagens enviadas

```bash
curl http://localhost:7070/messages
```

## Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

## Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

## Exemplo completo: Fluxo de reserva

```bash
# 1. Listar quartos disponíveis
curl http://localhost:7070/rooms

# 2. Criar uma reserva
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }')

echo $RESERVATION | jq .

# Extrair o ID da reserva
ID=$(echo $RESERVATION | jq -r .id)

# 3. Confirmar a reserva (efetuar pagamento)
curl -X POST http://localhost:7070/reservations/$ID/confirmation

# 4. Verificar mensagens enviadas
curl http://localhost:7070/messages | jq .

# 5. Fazer check-in
curl -X POST http://localhost:7070/reservations/$ID/check-in

# 6. Fazer check-out e obter comprovante
curl -X POST http://localhost:7070/reservations/$ID/check-out | jq .
```

## Códigos de erro

- `400 INVALID_REQUEST`: Requisição inválida (campo faltando, formato errado)
- `404 ROOM_NOT_FOUND`: Quarto não encontrado
- `404 RESERVATION_NOT_FOUND`: Reserva não encontrada
- `409 ROOM_UNAVAILABLE`: Quarto já reservado nas datas solicitadas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no status atual
- `422 CAPACITY_EXCEEDED`: Número de hóspedes excede capacidade do quarto

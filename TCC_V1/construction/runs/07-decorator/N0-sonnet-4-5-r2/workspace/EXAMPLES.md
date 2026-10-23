# Exemplos de Uso da API

## Iniciar o Sistema

```bash
# Configurar variáveis de ambiente
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070
export HOTEL_TEST_MODE=false

# Executar
java -jar target/reservas-1.0.0.jar
```

## Exemplos de Requisições

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
  ...
]
```

### 2. Criar Reserva (Baixa Temporada, Plano Standard)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
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
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
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

### 3. Criar Reserva (Alta Temporada - Julho)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta (note o subtotal e total maiores devido à alta temporada):
```json
{
  "id": 2,
  "subtotal": 599.76,
  "total": 599.76,
  ...
}
```

### 4. Criar Reserva com Desconto (Non-Refundable)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq
```

Resposta (com desconto de 15%):
```json
{
  "id": 3,
  "subtotal": 779.80,
  "discount": 116.97,
  "total": 662.83,
  ...
}
```

### 5. Confirmar Reserva (Envia E-mail)

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

### 6. Verificar Mensagens Enviadas

```bash
curl http://localhost:7070/messages | jq
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
    "body": "Olá, João Silva!\n\nSua reserva na Pousada Maré Alta está confirmada...",
    "status": "SENT"
  }
]
```

### 7. Testar E-mail Inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Email Inválido",
    "guestEmail": "ana@gmail",
    "guestCpf": "44455566677",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.id' | xargs -I {} curl -X POST http://localhost:7070/reservations/{}/confirmation
```

Verificar mensagens:
```bash
curl http://localhost:7070/messages | jq
```

Resultado: Mensagem com status "BLOCKED".

### 8. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

### 9. Check-out (Retorna Comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

Resposta:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678901",
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80
}
```

### 10. Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document | jq
```

### 11. Cancelar Reserva (Plano Standard - Com Reembolso)

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq
```

Resposta:
```json
{
  "id": 1,
  "status": "CANCELLED",
  "amountPaid": 499.80,
  "refund": 499.80,
  ...
}
```

### 12. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
101,STANDARD,2,2027-07-10,2027-07-12
total,1/5
```

### 13. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
2,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,599.76
```

## Modo de Teste

Para testar o modo de teste:

```bash
export HOTEL_TEST_MODE=true
java -jar target/reservas-1.0.0.jar
```

Criar e confirmar uma reserva:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Test Guest",
    "guestEmail": "real.guest@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.id' | xargs -I {} curl -X POST http://localhost:7070/reservations/{}/confirmation
```

Verificar mensagens:
```bash
curl http://localhost:7070/messages | jq
```

Resultado esperado:
- 1 mensagem (não 2, pois não gera cópia em modo teste)
- Destinatário: `testes@pousadamarealta.com.br`
- Assunto: `[TESTE] Reserva confirmada - Pousada Maré Alta`
- Corpo começa com: `Destinatário original: real.guest@example.com`

## Casos de Erro

### Quarto não encontrado (404)
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
```

Resposta:
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found: 999"
}
```

### Capacidade excedida (422)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Família Grande",
    "guestEmail": "familia@example.com",
    "guestCpf": "33333333333",
    "checkIn": "2027-06-10",
    "checkOut": "2027-06-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta:
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Room capacity exceeded"
}
```

### Quarto indisponível (409)
```bash
# Primeira reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Primeiro",
    "guestEmail": "primeiro@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-15",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Segunda reserva conflitante
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Segundo",
    "guestEmail": "segundo@example.com",
    "guestCpf": "22222222222",
    "checkIn": "2027-05-12",
    "checkOut": "2027-05-17",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

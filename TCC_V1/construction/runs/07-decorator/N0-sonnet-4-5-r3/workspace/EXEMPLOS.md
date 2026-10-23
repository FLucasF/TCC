# Exemplos de Uso da API

## Executar o Sistema

### Com Docker Compose
```bash
docker-compose up
```

### Direto com JAR
```bash
# Inicie o PostgreSQL primeiro
docker run -d \
  --name postgres \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:18

# Execute a aplicação
java -jar target/reservas-1.0.0.jar
```

### Modo de Teste
```bash
export HOTEL_TEST_MODE=true
java -jar target/reservas-1.0.0.jar
```

## Exemplos de Requisições

### 1. Listar Quartos
```bash
curl http://localhost:7070/rooms
```

**Resposta:**
```json
[
  {
    "number": "101",
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
  ...
]
```

### 2. Criar Reserva
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
  }'
```

**Resposta:**
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

### 3. Confirmar Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**Resposta:**
```json
{
  "id": 1,
  ...
  "status": "CONFIRMED",
  "amountPaid": 499.80,
  ...
}
```

**E-mail enviado para joao@example.com:**
```
Olá, João Silva!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

Comprovante de reserva nº 1
Hóspede: João Silva (CPF 123.456.789-01)
Quarto: 101 (STANDARD)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 499,80
Total: R$ 499,80

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

**Cópia arquivada para arquivo@pousadamarealta.com.br:**
```
Assunto: [Cópia] Reserva confirmada - Pousada Maré Alta
(mesmo corpo)
```

### 4. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 5. Check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

**Resposta:**
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

### 6. Segunda Via do Comprovante
```bash
curl http://localhost:7070/reservations/1/document
```

### 7. Listar Mensagens Enviadas
```bash
curl http://localhost:7070/messages
```

**Resposta:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!...",
    "status": "SENT"
  }
]
```

### 8. Relatório de Ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

**Resposta (CSV):**
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
total,1/5
```

### 9. Relatório de Receita
```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

**Resposta (CSV):**
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80
total,499.80
```

### 10. Cancelar Reserva (com Reembolso)
```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

**Plano STANDARD:** recebe reembolso total  
**Plano NON_REFUNDABLE:** sem reembolso

## Exemplos de Alta Temporada

### Reserva em Julho (alta temporada)
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
  }'
```

**Preço:**
- Diária normal: R$ 249,90
- Diária alta temporada (20% a mais): R$ 299,88
- 2 noites: R$ 599,76

### Reserva Não Reembolsável (15% desconto)
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
    "ratePlan": "NON_REFUNDABLE"
  }'
```

**Preço:**
- Diária: R$ 389,90
- 2 noites: R$ 779,80
- Desconto 15%: R$ 116,97
- Total: R$ 662,83

## Sistema de Mensagens

### E-mail Bloqueado (inválido)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail",
    "guestCpf": "11111111111",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Ao confirmar, a mensagem aparecerá com `status: "BLOCKED"` no GET /messages.

### Modo de Teste
Com `HOTEL_TEST_MODE=true`:

**Mensagem original:**
- Para: joao@example.com
- Assunto: Reserva confirmada - Pousada Maré Alta

**Mensagem redirecionada:**
- Para: testes@pousadamarealta.com.br
- Assunto: [TESTE] Reserva confirmada - Pousada Maré Alta
- Corpo: Destinatário original: joao@example.com\n[corpo original]
- **Sem cópia para arquivo** (no modo de teste)

## Erros Comuns

### Quarto não encontrado (404)
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found: 999"
}
```

### Quarto sem disponibilidade (409)
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

### Capacidade excedida (422)
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Room capacity exceeded. Max: 2, requested: 5"
}
```

### Operação não permitida (409)
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Reservation cannot be confirmed in status: CANCELLED"
}
```

### Requisição inválida (400)
```json
{
  "error": "INVALID_REQUEST",
  "message": "Check-out date must be after check-in date"
}
```

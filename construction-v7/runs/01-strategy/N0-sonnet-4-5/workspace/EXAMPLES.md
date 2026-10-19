# Exemplos de Uso - API Pousada Maré Alta

## Criar Reservas

### Reserva Normal (STANDARD)
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

**Resposta:**
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0
}
```

### Reserva Não Reembolsável (NON_REFUNDABLE)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

**Resposta:**
```json
{
  "id": 2,
  "roomNumber": "201",
  "guestName": "Maria Santos",
  "guestEmail": "maria@example.com",
  "guestCpf": "98765432100",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "NON_REFUNDABLE",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 116.97,
  "total": 662.83,
  "status": "PENDING",
  "amountPaid": 0
}
```

### Reserva Corporativa (CORPORATE)
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
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

**Resposta:**
```json
{
  "id": 3,
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "CORPORATE",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 93.58,
  "total": 686.22,
  "status": "PENDING",
  "amountPaid": 0
}
```

### Pacote de Fim de Semana (WEEKEND_PACKAGE)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

**Resposta:**
```json
{
  "id": 4,
  "roomNumber": "201",
  "guestName": "Pedro Costa",
  "guestEmail": "pedro@example.com",
  "guestCpf": "55566677788",
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14",
  "guests": 2,
  "ratePlan": "WEEKEND_PACKAGE",
  "nights": 2,
  "subtotal": 820.00,
  "discount": 0,
  "total": 820.00,
  "status": "PENDING",
  "amountPaid": 0
}
```

### Estadia Longa (LONG_STAY)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Lima",
    "guestEmail": "ana@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }'
```

**Resposta:**
```json
{
  "id": 5,
  "roomNumber": "101",
  "guestName": "Ana Lima",
  "guestEmail": "ana@example.com",
  "guestCpf": "99988877766",
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-08",
  "guests": 2,
  "ratePlan": "LONG_STAY",
  "nights": 7,
  "subtotal": 1749.30,
  "discount": 174.93,
  "total": 1574.37,
  "status": "PENDING",
  "amountPaid": 0
}
```

## Fluxo Completo de Reserva

### 1. Consultar Quartos Disponíveis
```bash
curl http://localhost:7070/rooms
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
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### 3. Confirmar Reserva (Pagamento)
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**E-mail enviado automaticamente:**
```
Para: joao@example.com
Assunto: Reserva confirmada - Pousada Maré Alta

Olá, João Silva!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

Comprovante de reserva nº 1
Hóspede: João Silva (CPF 123.456.789-01)
Quarto: 101 (STANDARD)
Entrada: 10/07/2027 | Saída: 12/07/2027 | 2 diárias
Diárias: R$ 599.76
Total: R$ 599.76

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

### 4. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 5. Check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

**Resposta (Comprovante):**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678901",
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0,
  "total": 599.76
}
```

### 6. Segunda Via do Comprovante
```bash
curl http://localhost:7070/reservations/1/document
```

## Cancelamentos

### Cancelar Reserva STANDARD (reembolso total)
```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

**Resposta:**
```json
{
  "id": 1,
  "status": "CANCELLED",
  "amountPaid": 599.76,
  "refund": 599.76
}
```

### Cancelar Reserva NON_REFUNDABLE (sem reembolso)
```bash
curl -X POST http://localhost:7070/reservations/2/cancellation
```

**Resposta:**
```json
{
  "id": 2,
  "status": "CANCELLED",
  "amountPaid": 662.83,
  "refund": 0
}
```

### Cancelar Reserva WEEKEND_PACKAGE (reembolso 50%)
```bash
curl -X POST http://localhost:7070/reservations/4/cancellation
```

**Resposta:**
```json
{
  "id": 4,
  "status": "CANCELLED",
  "amountPaid": 820.00,
  "refund": 410.00
}
```

## Relatórios

### Relatório de Ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

**Resposta (CSV):**
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,3,2027-07-09,2027-07-11
total,2/5
```

### Relatório Financeiro
```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

**Resposta (CSV):**
```csv
reservation,room,check_in,check_out,status,amount
2,201,2027-03-10,2027-03-12,CONFIRMED,662.83
3,201,2027-03-10,2027-03-12,CONFIRMED,686.22
4,201,2027-03-12,2027-03-14,CANCELLED,410.00
5,101,2027-03-01,2027-03-08,CONFIRMED,1574.37
total,3333.42
```

## Mensagens Enviadas
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
    "body": "Olá, João Silva!..."
  }
]
```

## Tratamento de Erros

### Quarto não encontrado
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (404):**
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Quarto não encontrado"
}
```

### Capacidade excedida
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
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (422):**
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Número de hóspedes excede a capacidade do quarto"
}
```

### Quarto indisponível (já reservado)
```bash
# Primeira reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": "101", "guestName": "João", "guestEmail": "joao@example.com", "guestCpf": "12345678901", "checkIn": "2027-07-10", "checkOut": "2027-07-12", "guests": 2, "ratePlan": "STANDARD"}'

# Tentar reservar o mesmo quarto nas mesmas datas
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": "101", "guestName": "Maria", "guestEmail": "maria@example.com", "guestCpf": "98765432100", "checkIn": "2027-07-10", "checkOut": "2027-07-12", "guests": 2, "ratePlan": "STANDARD"}'
```

**Resposta (409):**
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Quarto não está disponível nas datas selecionadas"
}
```

### Operação não permitida
```bash
# Tentar fazer check-in em reserva pendente
curl -X POST http://localhost:7070/reservations/1/check-in
```

**Resposta (409):**
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Apenas reservas confirmadas podem fazer check-in"
}
```

### Código de empresa inválido
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
    "ratePlan": "CORPORATE",
    "companyCode": "INVALIDA"
  }'
```

**Resposta (400):**
```json
{
  "error": "INVALID_REQUEST",
  "message": "Código de empresa inválido para o plano corporativo"
}
```

### Pacote de fim de semana em dia errado
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

**Resposta (400):**
```json
{
  "error": "INVALID_REQUEST",
  "message": "O pacote de fim de semana deve começar na sexta-feira"
}
```

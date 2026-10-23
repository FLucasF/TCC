# Exemplos de Uso da API

## 1. Listar Quartos

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
  {
    "number": "201",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": "202",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": "301",
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

## 2. Criar Reserva - Pessoa Física (CPF)

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

Resposta (201 Created):
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.0,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0.0,
  "refund": null
}
```

## 3. Criar Reserva - Empresa (CNPJ)

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
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Recebe 15% de desconto e fatura com ISS retido.

## 4. Criar Reserva - Estrangeiro (Passaporte)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }'
```

Recebe booking receipt em inglês.

## 5. Criar Reserva com Alta Temporada

Julho inteiro e 15 de dezembro a 28 de fevereiro têm acréscimo de 20%:

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
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta mostrará subtotal de R$ 599,76 (R$ 249,90 × 1,20 × 2 noites).

## 6. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
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

Um e-mail de confirmação é enviado (armazenado em `/messages`).

## 7. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 8. Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Retorna o documento (comprovante, fatura ou booking receipt):
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678901",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.0,
  "total": 499.80,
  "text": "Comprovante de reserva nº 1\nHóspede: João Silva (CPF 123.456.789-01)\n..."
}
```

## 9. Segunda Via do Documento

```bash
curl http://localhost:7070/reservations/1/document
```

Só funciona para reservas pagas (CONFIRMED, CHECKED_IN ou CHECKED_OUT).

## 10. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

- Plano STANDARD: devolve o valor pago
- Plano NON_REFUNDABLE: não devolve nada
- Só pode cancelar antes do check-in

## 11. Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "joao@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, João Silva!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  }
]
```

## 12. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
201,DELUXE,2,2027-03-10,2027-03-12
total,2/5
```

## 13. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.8
2,201,2027-03-10,2027-03-12,CHECKED_OUT,662.83
total,1162.63
```

## Códigos de Erro

### 400 - INVALID_REQUEST
- Campos obrigatórios faltando
- Formato de data inválido
- Check-out não é depois do check-in
- Menos de 1 hóspede
- Identificação inválida (deve ser CPF, ou CNPJ+nome da empresa, ou passaporte+país)

### 404 - ROOM_NOT_FOUND
- Quarto não existe

### 404 - RESERVATION_NOT_FOUND
- Reserva não existe

### 409 - ROOM_UNAVAILABLE
- Quarto já reservado nas datas solicitadas

### 409 - OPERATION_NOT_ALLOWED
- Tentando confirmar uma reserva que não está PENDING
- Tentando fazer check-in de uma reserva que não está CONFIRMED
- Tentando fazer check-out de uma reserva que não está CHECKED_IN
- Tentando cancelar após check-in

### 422 - CAPACITY_EXCEEDED
- Número de hóspedes excede a capacidade do quarto

Exemplo de erro:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room 101 is not available for the selected dates"
}
```

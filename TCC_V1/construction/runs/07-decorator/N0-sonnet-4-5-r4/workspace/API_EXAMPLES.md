# Exemplos de Uso da API

Este documento mostra exemplos práticos de como usar a API do sistema de reservas.

## Pré-requisitos

1. PostgreSQL rodando (use `docker-compose up -d`)
2. Sistema compilado (`mvn package`)
3. Sistema rodando (`./run-example.sh` ou `java -jar target/reservas-1.0.0.jar`)

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
  ...
]
```

## 2. Criar Reserva (Temporada Normal)

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

Resposta:
```json
{
  "id": 1,
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
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

## 3. Criar Reserva (Alta Temporada com Desconto)

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
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resposta (note o subtotal de alta temporada e o desconto):
```json
{
  "id": 2,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
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

## 4. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Após confirmar, verifique as mensagens enviadas:

```bash
curl http://localhost:7070/messages
```

Você verá a mensagem de confirmação e a cópia para o arquivo:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  }
]
```

## 5. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 6. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta (comprovante):
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

## 7. Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## 8. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## 9. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## 10. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CONFIRMED,779.80
total,779.80
```

## Teste de E-mail Inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Costa",
    "guestEmail": "ana.example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-11",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Ao confirmar essa reserva e verificar as mensagens:

```bash
curl -X POST http://localhost:7070/reservations/3/confirmation
curl http://localhost:7070/messages
```

Você verá que a mensagem foi bloqueada:
```json
{
  "id": 3,
  "channel": "EMAIL",
  "to": "ana.example.com",
  "subject": "Reserva confirmada - Pousada Maré Alta",
  "body": "...",
  "status": "BLOCKED"
}
```

## Modo de Teste

Inicie o sistema com `HOTEL_TEST_MODE=true`:

```bash
export HOTEL_TEST_MODE=true
java -jar target/reservas-1.0.0.jar
```

Agora todas as mensagens serão redirecionadas para `testes@pousadamarealta.com.br`:

```json
{
  "id": 1,
  "channel": "EMAIL",
  "to": "testes@pousadamarealta.com.br",
  "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
  "body": "Destinatário original: carla@example.com\nOlá, Carla Dias!...",
  "status": "SENT"
}
```

## Tratamento de Erros

### Quarto não encontrado (404)
```bash
curl http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": "999", ...}'
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
    "guestCpf": "11111111111",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
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
# Primeiro crie e confirme uma reserva
curl -X POST http://localhost:7070/reservations ...
curl -X POST http://localhost:7070/reservations/1/confirmation

# Depois tente criar outra para as mesmas datas
curl -X POST http://localhost:7070/reservations ...
```

Resposta:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room not available for selected dates"
}
```

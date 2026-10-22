# Exemplos de Uso da API

## 1. Listar Quartos

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
    "number": 102,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

## 2. Criar Reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
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
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
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

## 3. Criar Reserva com CNPJ

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Ana Souza",
    "guestEmail": "ana@empresa.com.br",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## 4. Criar Reserva com Passaporte

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "John Smith",
    "guestEmail": "john@example.com",
    "guestPassport": "X1234567",
    "guestCountry": "United Kingdom",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

## 5. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 779.80
  }'
```

E-mail é automaticamente enviado (armazenado em mensagens).

## 6. Consultar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## 7. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 8. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta inclui o comprovante:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "roomNumber": 201,
  "roomType": "DELUXE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80,
  "text": "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\n..."
}
```

## 9. Segunda Via do Documento

```bash
curl http://localhost:7070/reservations/1/document
```

## 10. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/2/cancellation
```

Se a reserva era STANDARD e já tinha sido confirmada, o reembolso é total.

## 11. Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

Resposta (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,3,2027-07-10,2027-07-12
201,DELUXE,5,2027-07-10,2027-07-15
total,2/5
```

## 12. Relatório Financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```

Resposta (CSV):
```
reservation,room,check_in,check_out,status,amount
3,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
5,201,2027-07-10,2027-07-15,CONFIRMED,1949.50
total,,,sum,2549.26
```

## 13. Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

Resposta:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada..."
  }
]
```

## Notas Importantes

1. **Datas**: Sempre use formato ISO (YYYY-MM-DD)
2. **Dinheiro**: No JSON, use números com até 2 casas decimais (779.80)
3. **Alta Temporada**: Julho inteiro + 15/12 a 28/02 (20% de aumento)
4. **Desconto NON_REFUNDABLE**: 15% sobre o subtotal
5. **ISS (Fatura)**: 5% retido sobre o total
6. **Arredondamento**: "Meio para o par" em todas as etapas

# Exemplos de Uso da API

## Criar Reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta (resumida):
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## Criar Reserva com CNPJ

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Criar Reserva com Passaporte

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
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Após a confirmação, um e-mail é registrado no sistema e pode ser consultado:

```bash
curl http://localhost:7070/messages
```

## Documento - CPF

GET `/reservations/1/document` retorna:

```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "123.456.789-00",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "roomNumber": "101",
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.00,
  "total": 599.76,
  "text": "Comprovante de reserva nº 1\nHóspede: João Silva (CPF 123.456.789-00)\nQuarto: 101 (STANDARD)\nEntrada: 10/07/2027 | Saída: 12/07/2027 | 2 diárias\nDiárias: R$ 599,76\nTotal: R$ 599,76"
}
```

## Documento - CNPJ

```json
{
  "title": "Fatura de hospedagem",
  "text": "Fatura de hospedagem nº 2\nEmpresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)\nHóspede: Ana Souza\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\nISS retido (5%): R$ 38,99"
}
```

## Documento - Passaporte

```json
{
  "title": "Booking receipt",
  "text": "Booking receipt no. 3\nGuest: John Smith (passport X1234567, United Kingdom)\nRoom: 301 (SUITE)\nCheck-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights\nNights: BRL 1,159.80\nDiscount: BRL 173.97\nTotal: BRL 985.83"
}
```

## Relatórios

### Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta (CSV):
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,599.76
```

## Fluxo Completo

1. Criar reserva → PENDING
2. Confirmar → CONFIRMED (e-mail enviado)
3. Check-in → CHECKED_IN
4. Check-out → CHECKED_OUT (retorna documento)

## Cancelamento

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

- Plano STANDARD: reembolso total
- Plano NON_REFUNDABLE: sem reembolso

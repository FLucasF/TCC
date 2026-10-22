# Exemplo de Uso Completo - Pousada Maré Alta

Passo a passo demonstrando o ciclo completo de uma reserva.

## 1. Listar Quartos

```bash
curl -s http://localhost:7070/rooms | jq
```

Resposta esperada:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": "249.90"
  },
  {
    "number": 102,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": "249.90"
  },
  {
    "number": 201,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": "389.90"
  },
  {
    "number": 202,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": "389.90"
  },
  {
    "number": 301,
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": "579.90"
  }
]
```

## 2. Criar Reserva (Hóspede com CPF)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

Resposta esperada (status 201):
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
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
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80",
  "status": "PENDING",
  "amountPaid": "0.00",
  "refund": null
}
```

## 3. Confirmar Reserva (Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}' | jq
```

Resposta esperada:
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
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
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80",
  "status": "CONFIRMED",
  "amountPaid": "779.80",
  "refund": null
}
```

## 4. Verificar E-mail Enviado

```bash
curl -s http://localhost:7070/messages | jq
```

Resposta esperada:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@email.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!"
  }
]
```

## 5. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

Resposta esperada:
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
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
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80",
  "status": "CHECKED_IN",
  "amountPaid": "779.80",
  "refund": null
}
```

## 6. Fazer Check-out (Recebe Comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

Resposta esperada:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "11122233344",
  "companyCnpj": null,
  "companyName": null,
  "guestPassport": null,
  "guestCountry": null,
  "roomNumber": 201,
  "roomType": "DELUXE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": "779.80",
  "discount": "0.00",
  "total": "779.80",
  "text": "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80"
}
```

## Exemplo com CNPJ (Fatura)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@empresa.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .text
```

Retorna comprovante em formato de fatura com ISS:
```
Fatura de hospedagem nº 2
Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)
Hóspede: Ana Souza
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80
ISS retido (5%): R$ 38,99
```

## Exemplo com Passaporte (Booking Receipt)

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
  }' | jq .text
```

Retorna booking receipt em inglês:
```
Booking receipt no. 3
Guest: John Smith (passport X1234567, United Kingdom)
Room: 201 (DELUXE)
Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights
Nights: BRL 779.80
Total: BRL 779.80
```

## Exemplo com Desconto (Plano NON_REFUNDABLE)

Quando escolhe plano NON_REFUNDABLE, recebe 15% de desconto:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Silva",
    "guestEmail": "maria@email.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq '.total, .discount'
```

Resposta:
```json
"662.83"
"116.97"
```

Subtotal: 779.80, Desconto (15%): 116.97, Total: 662.83

## Relatório de Ocupação

```bash
curl -s "http://localhost:7070/reports/occupancy?date=2027-03-10" 
```

Resposta CSV:
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

## Relatório de Receita

```bash
curl -s "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta CSV:
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
total,779.80
```

## Casos de Erro

### Quarto não encontrado
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{...,"roomNumber": 999,...}'
```
Resposta: `404` com `ROOM_NOT_FOUND`

### Quarto já reservado
```bash
# Já existe reserva de 10/03 a 12/03 no quarto 201
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{...,"checkIn": "2027-03-10","checkOut": "2027-03-12",...}'
```
Resposta: `409` com `ROOM_UNAVAILABLE`

### Excesso de hóspedes
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{...,"roomNumber": 201,"guests": 5,...}'
```
Resposta: `422` com `CAPACITY_EXCEEDED` (DELUXE tem máx 3)

### Identificação inválida
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{...,"guestCpf": "111","companyCnpj": "222",...}'
```
Resposta: `400` com `INVALID_REQUEST` (apenas uma identificação)

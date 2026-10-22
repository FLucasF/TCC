# Exemplos de Uso da API

Este documento contém exemplos de requisições HTTP para testar o sistema.

## 1. Listar Quartos

```bash
curl -X GET http://localhost:7070/rooms
```

Resposta esperada:
```json
[
  {
    "number": 101,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

## 2. Criar Reserva (com CPF)

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

Resposta esperada:
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

## 3. Criar Reserva (com CNPJ)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@tecnosul.com.br",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

## 4. Criar Reserva (com Passaporte)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
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

## 5. Confirmar Reserva (Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 779.80
  }'
```

Resposta esperada:
```json
{
  "id": 1,
  ...
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  ...
}
```

## 6. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}'
```

## 7. Fazer Check-out

Retorna o comprovante de check-out:

```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}'
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
  "subtotal": 779.80,
  "discount": 0.00,
  "total": 779.80,
  "text": "Comprovante de reserva nº 1\nHóspede: Carla Dias (CPF 111.222.333-44)\nQuarto: 201 (DELUXE)\nEntrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias\nDiárias: R$ 779,80\nTotal: R$ 779,80"
}
```

## 8. Obter Segunda Via do Comprovante

```bash
curl -X GET http://localhost:7070/reservations/1/document
```

## 9. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation \
  -H "Content-Type: application/json" \
  -d '{}'
```

Para uma reserva confirmada com plano STANDARD, o hóspede recebe reembolso total.
Para uma reserva com plano NON_REFUNDABLE, não há reembolso.

## 10. Listar Mensagens (E-mails Enviados)

```bash
curl -X GET http://localhost:7070/messages
```

Resposta esperada:
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\nComprovante de reserva nº 1\n..."
  }
]
```

## 11. Relatório de Ocupação

```bash
curl -X GET "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta esperada (CSV):
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
301,SUITE,2,2027-03-10,2027-03-12
total,2/5
```

## 12. Relatório Financeiro

```bash
curl -X GET "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta esperada (CSV):
```
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
2,301,2027-03-10,2027-03-12,CONFIRMED,1559.45
total,,,3339.25
```

## Notas Importantes

### Preço com Alta Temporada (Julho)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Silva",
    "guestEmail": "maria@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resultado: 2 noites × R$ 299,88 = R$ 599,76 (20% de acréscimo)

### Preço com Desconto NON_REFUNDABLE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "João Santos",
    "guestEmail": "joao@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resultado:
- Subtotal: R$ 1.159,80 (2 × R$ 579,90)
- Desconto (15%): R$ 173,97
- Total: R$ 985,83

## Códigos de Erro

| HTTP Status | Código | Significado |
|-------------|--------|-------------|
| 400 | INVALID_REQUEST | Requisição inválida |
| 404 | ROOM_NOT_FOUND | Quarto não encontrado |
| 404 | RESERVATION_NOT_FOUND | Reserva não encontrada |
| 409 | ROOM_UNAVAILABLE | Quarto indisponível nas datas |
| 409 | OPERATION_NOT_ALLOWED | Operação não permitida para este estado |
| 422 | CAPACITY_EXCEEDED | Mais hóspedes do que a capacidade |

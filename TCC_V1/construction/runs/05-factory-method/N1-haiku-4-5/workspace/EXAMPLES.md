# Exemplos de Requisições

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
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## 3. Criar Reserva com CNPJ (Empresa)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Souza",
    "guestEmail": "ana@company.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

## 4. Criar Reserva com Passaporte (Estrangeiro)

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
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## 5. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/{id}/confirmation
```

A confirmação envia um e-mail com o comprovante/fatura/booking receipt.

## 6. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/{id}/cancellation
```

- Reserva STANDARD confirmada: reembolso total
- Reserva NON_REFUNDABLE: sem reembolso
- Reserva PENDING: sem reembolso (nenhum pagamento foi realizado)

## 7. Check-in

```bash
curl -X POST http://localhost:7070/reservations/{id}/check-in
```

## 8. Check-out

```bash
curl -X POST http://localhost:7070/reservations/{id}/check-out
```

Retorna o comprovante/fatura/booking receipt.

## 9. Obter Segunda Via do Documento

```bash
curl http://localhost:7070/reservations/{id}/document
```

Disponível apenas para reservas confirmadas ou com check-out realizado.

## 10. Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

## 11. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Retorna CSV com quartos ocupados naquela noite.

## 12. Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Retorna CSV com receita do mês (reservas confirmadas, com check-in ou check-out, ou NON_REFUNDABLE canceladas).

## Códigos de Erro

| Código | Erro | Significado |
|--------|------|-------------|
| 400 | INVALID_REQUEST | Campos inválidos ou faltando |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto já reservado para as datas |
| 409 | OPERATION_NOT_ALLOWED | Operação não permitida para o status atual |
| 422 | CAPACITY_EXCEEDED | Número de hóspedes excede a capacidade |

## Cálculo de Preços

### Exemplo 1: STANDARD de 2 noites (sem alta temporada)
- Base: R$ 249,90 x 2 noites = R$ 499,80
- Desconto: R$ 0,00 (plano STANDARD)
- Total: R$ 499,80

### Exemplo 2: DELUXE de 2 noites em julho (alta temporada)
- Base: R$ 389,90 x 1.20 (alta temporada) = R$ 467,88
- Subtotal: R$ 467,88 x 2 noites = R$ 935,76
- Se NON_REFUNDABLE: Desconto = R$ 935,76 x 0,15 = R$ 140,36
- Total: R$ 795,40

### Observação
- Arredondamento é feito após cada passo (noite, subtotal, desconto, total)
- Método de arredondamento: "meio para o par" (banker's rounding)

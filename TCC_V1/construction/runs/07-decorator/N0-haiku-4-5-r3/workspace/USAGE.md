# Exemplos de Uso da API

## 1. Obter Quartos

```bash
curl http://localhost:7070/rooms
```

Resposta esperada:
```json
[
  {"number": 101, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 102, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 201, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 202, "type": "DELUXE", "capacity": 3, "nightlyRate": 389.90},
  {"number": 301, "type": "SUITE", "capacity": 4, "nightlyRate": 579.90}
]
```

## 2. Criar Reserva

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

Resposta esperada (201 Created):
```json
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "111.222.333-44",
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

## 3. Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 779.80}'
```

Ao confirmar, um email é enviado para o hóspede (ou testes@pousadamarealta.com.br em modo teste).

## 4. Obter Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## 5. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## 6. Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Retorna o comprovante de checkout.

## 7. Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

No plano STANDARD: reembolso integral (se confirmada ou com check-in)
No plano NON_REFUNDABLE: sem reembolso

## 8. Obter Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

Retorna todas as mensagens com status SENT ou BLOCKED.

## 9. Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-03-10
```

Retorna CSV com ocupação dos quartos naquela data.

## 10. Relatório Financeiro

```bash
curl http://localhost:7070/reports/revenue?month=2027-03
```

Retorna CSV com receita do mês (reservas que começam naquele mês).

## Modo Teste

Para ativar modo teste (redireciona emails para testes@pousadamarealta.com.br):

```bash
export HOTEL_TEST_MODE=true
java -jar target/mare-alta-1.0.0.jar
```

Em modo teste:
- Emails são redirecionados para testes@pousadamarealta.com.br
- Emails inválidos ainda são bloqueados
- Não há cópia para o arquivo automática

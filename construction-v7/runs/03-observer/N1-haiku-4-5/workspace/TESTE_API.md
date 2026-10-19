# Testes da API - Pousada Maré Alta

## Cenários Testados

### 1. Criar Reserva em Temporada Alta (Julho)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Alice Johnson",
    "guestEmail": "alice@test.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-15",
    "checkOut": "2027-07-17",
    "guests": 3,
    "ratePlan": "STANDARD"
  }'
```

**Resultado esperado:**
- 2 noites em temporada alta
- Taxa base SUITE: R$ 579,90 * 1,20 = R$ 695,88 por noite
- Subtotal: R$ 1.391,76

### 2. Confirmar Reserva
Quando confirmada, gera notificações para:
- Hóspede (alice@test.com)
- Governança (governanca@pousadamarealta.com.br)
- Financeiro (financeiro@pousadamarealta.com.br)

### 3. Relatório de Ocupação
```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-15
```

Mostra quais quartos estão ocupados em determinada noite.

### 4. Relatório Financeiro
```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```

Mostra todas as reservas que começam no mês, com valores recebidos/devolvidos.

### 5. Cenários de Erro Validados
- ❌ Quarto não encontrado → 404 ROOM_NOT_FOUND
- ❌ Reserva não encontrada → 404 RESERVATION_NOT_FOUND
- ❌ Quarto indisponível → 409 ROOM_UNAVAILABLE
- ❌ Capacidade excedida → 422 CAPACITY_EXCEEDED
- ❌ Operação não permitida → 409 OPERATION_NOT_ALLOWED

## Fluxo Completo de Reserva

PENDING (criada) → CONFIRMED (paga) → CHECKED_IN → CHECKED_OUT

Cancelamento é permitido em PENDING ou CONFIRMED.

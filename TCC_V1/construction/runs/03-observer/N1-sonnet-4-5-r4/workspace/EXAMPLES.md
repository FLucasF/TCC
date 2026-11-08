# Exemplos de Uso da API

## Listar Quartos

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
  ...
]
```

## Criar Reserva

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

## Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Após a confirmação, o sistema envia automaticamente:
- Email para o hóspede com comprovante
- Email para a governança com datas e número de hóspedes
- Email para o financeiro com o valor recebido

## Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Retorna o comprovante em JSON.

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Se a reserva já estava confirmada, notifica governança e financeiro (se houver reembolso).

## Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

## Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Retorna CSV com quartos ocupados na data especificada.

## Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Retorna CSV com receitas do mês.

## Planos de Tarifa

- `STANDARD`: Preço normal, reembolso total em caso de cancelamento
- `NON_REFUNDABLE`: 15% de desconto, sem reembolso

## Alta Temporada

Diárias custam 20% a mais em:
- 15 de dezembro a 28 de fevereiro
- Todo o mês de julho

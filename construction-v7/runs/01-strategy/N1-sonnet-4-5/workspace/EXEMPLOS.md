# Exemplos de Uso da API

## Listar Quartos

```bash
curl http://localhost:7070/rooms
```

## Criar Reserva - Plano Standard

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@exemplo.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Criar Reserva - Plano Corporate

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@exemplo.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

## Criar Reserva - Pacote de Fim de Semana

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@exemplo.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-14",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

## Criar Reserva - Long Stay

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@exemplo.com",
    "guestCpf": "55544433322",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 2,
    "ratePlan": "LONG_STAY"
  }'
```

## Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

## Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Consultar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## Obter Comprovante (Segunda Via)

```bash
curl http://localhost:7070/reservations/1/document
```

## Listar Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

## Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

## Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

## Exemplos de Cálculos

### Alta Temporada (Julho)
- Standard (101) de 10/07/2027 a 12/07/2027
- 2 noites × R$ 299,88 = R$ 599,76

### Convênio Tecnosul (12%)
- Deluxe (201) de 10/03/2027 a 12/03/2027
- Diárias: R$ 779,80
- Desconto: R$ 93,58
- Total: R$ 686,22

### Pacote de Fim de Semana
- Deluxe (201) de 12/03/2027 (sexta) a 14/03/2027 (domingo)
- Total fixo: R$ 820,00

### Long Stay (7+ noites)
- Standard (101) de 01/03/2027 a 08/03/2027
- Diárias: R$ 1.749,30
- Desconto 10%: R$ 174,93
- Total: R$ 1.574,37

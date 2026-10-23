# Exemplos de Uso da API

## Listar Quartos

```bash
curl http://localhost:7070/rooms
```

## Criar Reserva (Pessoa Física com CPF)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Criar Reserva (Empresa com CNPJ)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Ana Souza",
    "guestEmail": "ana@example.com",
    "companyCnpj": "12345678000190",
    "companyName": "Tecnosul Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

## Criar Reserva (Estrangeiro com Passaporte)

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
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

## Consultar Reserva

```bash
curl http://localhost:7070/reservations/1
```

## Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

## Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## Cancelar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Listar Mensagens

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

## Exemplos de Alta Temporada

### Julho (alta temporada)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Neste caso, cada noite custará R$ 299,88 (R$ 249,90 + 20%), 
totalizando R$ 599,76 para 2 noites.

### Plano Não Reembolsável

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Neste caso, o valor será calculado com 15% de desconto sobre o subtotal.

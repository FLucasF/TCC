# Guia Rápido de Teste

## 1. Iniciar o Sistema

```bash
# Build do projeto
mvn clean package

# Iniciar PostgreSQL e aplicação
chmod +x run.sh
./run.sh
```

O sistema estará rodando em `http://localhost:7070`

## 2. Testar a API

### Listar Quartos

```bash
curl http://localhost:7070/rooms
```

Resposta esperada: Lista com 5 quartos (101, 102, 201, 202, 301)

### Criar Reserva em Alta Temporada (Julho)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada:
- Status: 201
- `subtotal`: 599.76 (2 noites × R$ 299.88 cada, com alta temporada)
- `discount`: 0.0
- `total`: 599.76
- `status`: "PENDING"

### Criar Reserva com Desconto

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resposta esperada:
- `subtotal`: 779.80 (2 noites × R$ 389.90)
- `discount`: 116.97 (15% de desconto)
- `total`: 662.83

### Confirmar Reserva (Gera Notificações!)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta esperada:
- `status`: "CONFIRMED"
- `amountPaid`: 599.76

### Ver Mensagens Enviadas

```bash
curl http://localhost:7070/messages
```

Você deve ver 3 mensagens:
1. Para `joao@example.com` - Comprovante do hóspede
2. Para `governanca@pousadamarealta.com.br` - Aviso da governança
3. Para `financeiro@pousadamarealta.com.br` - Aviso do financeiro

### Tentar Reservar Quarto Ocupado

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-07-11",
    "checkOut": "2027-07-13",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada:
- Status: 409
- `error`: "ROOM_UNAVAILABLE"

### Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

Resposta esperada:
- `status`: "CHECKED_IN"

### Fazer Check-out (Retorna Comprovante)

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta esperada:
- Documento com `title`: "Comprovante de reserva"
- Todos os dados da reserva formatados

### Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-11"
```

Resposta esperada (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### Relatório Financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resposta esperada (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
total,599.76
```

## 3. Testar Cancelamento com Notificações

### Criar e Confirmar Reserva

```bash
# Criar
RESERVATION_ID=$(curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Carlos Oliveira",
    "guestEmail": "carlos@example.com",
    "guestCpf": "33333333333",
    "checkIn": "2027-09-01",
    "checkOut": "2027-09-03",
    "guests": 3,
    "ratePlan": "STANDARD"
  }' | jq -r '.id')

echo "Reserva criada: $RESERVATION_ID"

# Confirmar
curl -X POST "http://localhost:7070/reservations/$RESERVATION_ID/confirmation"
```

### Cancelar

```bash
curl -X POST "http://localhost:7070/reservations/$RESERVATION_ID/cancellation"
```

### Ver Novas Mensagens de Cancelamento

```bash
curl http://localhost:7070/messages | jq '.[-2:]'
```

Você deve ver:
1. Mensagem para governança sobre cancelamento
2. Mensagem para financeiro sobre reembolso

## 4. Testar Validações

### Capacidade Excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Lima",
    "guestEmail": "ana@example.com",
    "guestCpf": "22222222222",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-03",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada:
- Status: 422
- `error`: "CAPACITY_EXCEEDED"

### Quarto Inexistente

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-03",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resposta esperada:
- Status: 404
- `error`: "ROOM_NOT_FOUND"

## Verificando o Sistema de Notificações

O ponto mais importante do sistema é que **cada confirmação e cancelamento gera notificações automaticamente**:

1. **Confirmar reserva** → 3 e-mails enviados (hóspede, governança, financeiro)
2. **Cancelar reserva confirmada** → até 2 e-mails (governança sempre, financeiro só se houver reembolso)

Para adicionar novos interessados (restaurante, transfer), basta seguir o guia em `EXTENSIBILITY_EXAMPLE.md`.

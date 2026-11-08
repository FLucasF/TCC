# Exemplos de Uso da API

## Configuração Inicial

Antes de executar os exemplos, configure as variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada_marealta"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT="7070"

java -jar target/pousada-system.jar
```

## 1. Listar Quartos Disponíveis

```bash
curl http://localhost:7070/rooms | jq .
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
  {
    "number": 102,
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": 201,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": 202,
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": 301,
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

## 2. Criar uma Reserva (Plano STANDARD)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Resposta esperada:
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "123.456.789-00",
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "guests": 1,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.0,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0.0,
  "refund": null
}
```

## 3. Criar uma Reserva em Alta Temporada (Julho)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Neste caso, cada noite custa 20% a mais (R$ 299,88 em vez de R$ 249,90).

## 4. Criar uma Reserva com Plano NON_REFUNDABLE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carlos Oliveira",
    "guestEmail": "carlos@example.com",
    "guestCpf": "456.789.123-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
```

Resposta esperada (com 15% de desconto):
```json
{
  "id": 3,
  "roomNumber": 201,
  "guestName": "Carlos Oliveira",
  "...": "...",
  "subtotal": 779.80,
  "discount": 116.97,
  "total": 662.83,
  "status": "PENDING"
}
```

## 5. Confirmar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 499.80}' | jq .
```

Agora a reserva muda para status "CONFIRMED" e os emails são enviados.

## 6. Consultar Emails Enviados

```bash
curl http://localhost:7070/messages | jq '.[] | {to: .to, subject: .subject, body: .body}'
```

Exemplo de resposta:
```json
{
  "to": "joao@example.com",
  "subject": "Reserva confirmada - Pousada Maré Alta",
  "body": "Olá, João Silva!..."
}
{
  "to": "governanca@pousadamarealta.com.br",
  "subject": "Reserva confirmada - quarto 101",
  "body": "Quarto 101\nEntrada: 10/03/2027\nSaída: 12/03/2027\nHóspedes: 1"
}
{
  "to": "financeiro@pousadamarealta.com.br",
  "subject": "Pagamento recebido - reserva 1",
  "body": "Valor recebido: R$ 499,80"
}
```

## 7. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq .
```

Status muda para "CHECKED_IN".

## 8. Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq .
```

Retorna o comprovante de check-out:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "123.456.789-00",
  "roomNumber": 101,
  "roomType": 101,
  "checkIn": "10/03/2027",
  "checkOut": "12/03/2027",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.0,
  "total": 499.80
}
```

Status muda para "CHECKED_OUT".

## 9. Obter Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document | jq .
```

Retorna o mesmo formato do check-out.

## 10. Cancelar uma Reserva (Antes do Check-in)

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq .
```

Se a reserva estava confirmada com plano STANDARD:
- Status muda para "CANCELLED"
- Refund é definido para o valor pago (reembolso completo)
- Emails são enviados para governança e financeiro

Se fosse NON_REFUNDABLE:
- Status muda para "CANCELLED"
- Refund é definido para 0 (sem reembolso)
- Apenas governança é notificada

## 11. Relatório de Ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

Resposta em CSV:
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
total,1/5
```

## 12. Relatório de Receita (Mensal)

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

Resposta em CSV:
```
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80
3,201,2027-03-10,2027-03-12,CONFIRMED,662.83
total,1162.63
```

## Cenários de Teste

### Cenário 1: Reserva Confirmada e Cancelada com Reembolso

```bash
# Criar
RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test User",
    "guestEmail": "test@example.com",
    "guestCpf": "111.111.111-11",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')

ID=$(echo $RES | jq -r '.id')
TOTAL=$(echo $RES | jq -r '.total')

# Confirmar
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation \
  -H "Content-Type: application/json" \
  -d "{\"amountPaid\": $TOTAL}"

# Cancelar (reembolso será solicitado)
curl -s -X POST http://localhost:7070/reservations/$ID/cancellation | jq .

# Ver emails
curl -s http://localhost:7070/messages | jq '.[-2:] | .[] | {to, subject}'
```

### Cenário 2: Não Conseguir Fazer Reserva em Quarto Ocupado

```bash
# Primeira reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Guest 1",
    "guestEmail": "g1@example.com",
    "guestCpf": "222.222.222-22",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq '.id'

# Tentar a mesma data (mesmo hotel não aceitará)
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Guest 2",
    "guestEmail": "g2@example.com",
    "guestCpf": "333.333.333-33",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Esperado: erro 409 com ROOM_UNAVAILABLE

### Cenário 3: Exceder Capacidade do Quarto

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Too Many",
    "guestEmail": "many@example.com",
    "guestCpf": "444.444.444-44",
    "checkIn": "2027-06-10",
    "checkOut": "2027-06-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }' | jq .
```

Esperado: erro 422 com CAPACITY_EXCEEDED

## Validações da API

A API valida:

- Quarto existe: `ROOM_NOT_FOUND` (404)
- Quarto disponível: `ROOM_UNAVAILABLE` (409)
- Capacidade: `CAPACITY_EXCEEDED` (422)
- Data saída > data entrada: `INVALID_REQUEST` (400)
- Campos obrigatórios: `INVALID_REQUEST` (400)
- Operação permitida para o status: `OPERATION_NOT_ALLOWED` (409)
- Reserva existe: `RESERVATION_NOT_FOUND` (404)

# Testando a API

## Exemplo de Fluxo Completo

Assumindo que o servidor está rodando em `http://localhost:7070` e o banco de dados está configurado.

### 1. Listar Quartos

```bash
curl -s http://localhost:7070/rooms | jq .
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

### 2. Criar uma Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

Resposta esperada (com ID 1):
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 0.00,
  "total": 499.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

### 3. Confirmar a Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{
    "amountPaid": 499.80
  }' | jq .
```

Isso envia um email para o hóspede com o comprovante.

### 4. Ver Mensagens Enviadas

```bash
curl -s http://localhost:7070/messages | jq .
```

Você verá:
- A mensagem de confirmação para o hóspede (SENT)
- A cópia para arquivo (SENT) - se não estiver em modo teste

### 5. Fazer Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq .
```

### 6. Fazer Check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq .
```

Retorna o comprovante.

### 7. Pedir Segunda Via do Comprovante

```bash
curl -s http://localhost:7070/reservations/1/document | jq .
```

## Exemplos com Alta Temporada

Reserva em julho (alta temporada):

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Note que `subtotal` será `599.76` (2 noites × R$ 299,88).

## Exemplos com Desconto Non-Refundable

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carlos Lima",
    "guestEmail": "carlos@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq .
```

O desconto será 15% da subtotal.

## Teste em Modo Teste

Com `HOTEL_TEST_MODE=true`:

```bash
curl -s http://localhost:7070/messages | jq '.[] | select(.status == "SENT")'
```

Você verá mensagens para `testes@pousadamarealta.com.br` com assunto `[TESTE] Reserva confirmada...`.

## Relatórios

### Ocupação

```bash
curl -s 'http://localhost:7070/reports/occupancy?date=2027-07-10' | head -10
```

Exemplo de resposta:
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
102,STANDARD,2,2027-07-10,2027-07-11
201,DELUXE,3,2027-07-09,2027-07-12
total,3/5
```

### Financeiro

```bash
curl -s 'http://localhost:7070/reports/revenue?month=2027-07'
```

Exemplo de resposta:
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
2,102,2027-07-10,2027-07-11,CONFIRMED,299.88
3,201,2027-07-09,2027-07-12,CHECKED_OUT,1119.60
total,2019.24
```

## Testando Validações

### Email Inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Silva",
    "guestEmail": "anamail.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Espera criar a reserva normalmente, mas quando confirmar:

```bash
curl -X POST http://localhost:7070/reservations/N/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 249.90}' | jq .
```

Olhando as mensagens:
```bash
curl -s http://localhost:7070/messages | jq '.[] | select(.to == "anamail.com")'
```

Você verá uma mensagem com `status: "BLOCKED"`.

### Quarto não Disponível

```bash
# Criar primeira reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-08-15",
    "checkOut": "2027-08-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .

# Tentar agendar no mesmo quarto e período
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Lucia Oliveira",
    "guestEmail": "lucia@example.com",
    "guestCpf": "22222222222",
    "checkIn": "2027-08-15",
    "checkOut": "2027-08-17",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

Erro esperado:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for these dates"
}
```

# Teste Integrado - Cenário Completo

Este teste demonstra um cenário realista completo do sistema de reservas.

## Pré-requisitos

1. Compilar o projeto:
```bash
cd /workspace
mvn clean verify
```

2. Iniciar o servidor:
```bash
# Modo normal
java -jar target/reservas-1.0.0.jar

# Ou em outro terminal, modo teste
HOTEL_TEST_MODE=true java -jar target/reservas-1.0.0.jar
```

## Cenário 1: Reserva Confirmada com Check-in e Check-out

### Passo 1: Listar quartos disponíveis
```bash
curl -s http://localhost:7070/rooms | jq '.[] | {number, type, capacity, nightlyRate}'
```

Resultado esperado:
```json
{
  "number": 101,
  "type": "STANDARD",
  "capacity": 2,
  "nightlyRate": 249.9
}
...
```

### Passo 2: Criar uma reserva (STANDARD, fora da alta temporada)
```bash
curl -s -X POST http://localhost:7070/reservations \
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
  }' | jq '{id, roomNumber, guestName, status, total}'
```

Resultado esperado:
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "status": "PENDING",
  "total": 499.8
}
```

### Passo 3: Confirmar a reserva
```bash
curl -s -X POST http://localhost:7070/reservations/1/confirmation | jq '{id, status, amountPaid}'
```

Resultado esperado:
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 499.8
}
```

### Passo 4: Verificar que os emails foram enviados
```bash
curl -s http://localhost:7070/messages | jq '.[] | {to, status, subject}'
```

Resultado esperado:
```json
{
  "to": "joao@example.com",
  "status": "SENT",
  "subject": "Reserva confirmada - Pousada Maré Alta"
}
{
  "to": "arquivo@pousadamarealta.com.br",
  "status": "SENT",
  "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta"
}
```

### Passo 5: Fazer check-in
```bash
curl -s -X POST http://localhost:7070/reservations/1/check-in | jq '{id, status}'
```

Resultado esperado:
```json
{
  "id": 1,
  "status": "CHECKED_IN"
}
```

### Passo 6: Fazer check-out (retorna comprovante)
```bash
curl -s -X POST http://localhost:7070/reservations/1/check-out
```

Resultado esperado (texto):
```
Comprovante de reserva nº 1
Hóspede: João Silva (CPF 123.456.789-01)
Quarto: 101 (STANDARD)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 499,80
Total: R$ 499,80
```

### Passo 7: Obter segunda via do comprovante
```bash
curl -s http://localhost:7070/reservations/1/document
```

Resultado: Mesmo comprovante do check-out

---

## Cenário 2: Reserva NON_REFUNDABLE com Desconto

### Passo 1: Criar reserva NON_REFUNDABLE em alta temporada (julho)
```bash
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq '{id, nights, subtotal, discount, total}'
```

Resultado esperado (alta temporada: +20%, NON_REFUNDABLE: -15%):
```json
{
  "id": 2,
  "nights": 2,
  "subtotal": 935.76,
  "discount": 140.36,
  "total": 795.4
}
```

Explicação:
- Quarto 201 DELUXE: R$ 389,90 base
- Noite 1 (10/07): R$ 389,90 × 1,2 = R$ 467,88
- Noite 2 (11/07): R$ 389,90 × 1,2 = R$ 467,88
- Subtotal: R$ 935,76
- Desconto 15%: R$ 140,36
- Total: R$ 795,40

### Passo 2: Confirmar reserva NON_REFUNDABLE
```bash
curl -s -X POST http://localhost:7070/reservations/2/confirmation | jq '{id, status, amountPaid}'
```

### Passo 3: Cancelar a reserva (sem reembolso)
```bash
curl -s -X POST http://localhost:7070/reservations/2/cancellation | jq '{id, status, amountPaid, refund}'
```

Resultado esperado (NON_REFUNDABLE não reembolsa):
```json
{
  "id": 2,
  "status": "CANCELLED",
  "amountPaid": 795.4,
  "refund": null
}
```

---

## Cenário 3: Email Inválido (Bloqueado)

### Passo 1: Tentar criar reserva com email inválido
```bash
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Ana Costa",
    "guestEmail": "ana@gmail",
    "guestCpf": "55555555555",
    "checkIn": "2027-04-10",
    "checkOut": "2027-04-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '{id, guestEmail}'
```

Resultado esperado:
```json
{
  "id": 3,
  "guestEmail": "ana@gmail"
}
```

### Passo 2: Confirmar a reserva
```bash
curl -s -X POST http://localhost:7070/reservations/3/confirmation | jq '{status}'
```

### Passo 3: Verificar que o email foi bloqueado
```bash
curl -s http://localhost:7070/messages | jq '.[] | select(.status == "BLOCKED")'
```

Resultado esperado:
```json
{
  "id": 3,
  "channel": "EMAIL",
  "to": "ana@gmail",
  "subject": "Reserva confirmada - Pousada Maré Alta",
  "body": "...",
  "status": "BLOCKED"
}
```

---

## Cenário 4: Relatórios

### Passo 1: Relatório de ocupação
```bash
curl -s http://localhost:7070/reports/occupancy?date=2027-03-10
```

Resultado esperado (CSV):
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-03-10,2027-03-12
total,1/5
```

### Passo 2: Relatório de receita
```bash
curl -s http://localhost:7070/reports/revenue?month=2027-03
```

Resultado esperado (CSV):
```
reservation,room,check_in,check_out,status,amount
1,101,2027-03-10,2027-03-12,CHECKED_OUT,499.80
total,499.80
```

---

## Cenário 5: Erro - Quarto Indisponível

### Passo 1: Criar primeira reserva
```bash
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Teste 1",
    "guestEmail": "teste1@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.id'
```

### Passo 2: Tentar criar segunda reserva nas mesmas datas
```bash
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Teste 2",
    "guestEmail": "teste2@example.com",
    "guestCpf": "22222222222",
    "checkIn": "2027-05-10",
    "checkOut": "2027-05-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '{error, message}'
```

Resultado esperado:
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for selected dates"
}
```

---

## Cenário 6: Erro - Capacidade Excedida

```bash
curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "33333333333",
    "checkIn": "2027-06-10",
    "checkOut": "2027-06-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq '{error, message}'
```

Resultado esperado:
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

---

## Script Completo (Bash)

Você pode salvar este conteúdo como `teste-cenario.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:7070"

echo "=== Teste Integrado - Cenário Completo ==="
echo ""

# Cenário 1: Reserva STANDARD confirmada
echo "1. Criando reserva STANDARD..."
RES1=$(curl -s -X POST $BASE_URL/reservations \
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
  }' | jq '.id')
echo "Reserva criada: #$RES1"

echo "2. Confirmando reserva..."
curl -s -X POST $BASE_URL/reservations/$RES1/confirmation > /dev/null
echo "Confirmada!"

echo "3. Check-in..."
curl -s -X POST $BASE_URL/reservations/$RES1/check-in > /dev/null
echo "Check-in realizado!"

echo "4. Check-out..."
curl -s -X POST $BASE_URL/reservations/$RES1/check-out
echo ""

# Cenário 2: Reserva NON_REFUNDABLE
echo -e "\n5. Criando reserva NON_REFUNDABLE em alta temporada..."
RES2=$(curl -s -X POST $BASE_URL/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq '.id')
echo "Reserva criada: #$RES2"

echo "6. Confirmando reserva NON_REFUNDABLE..."
curl -s -X POST $BASE_URL/reservations/$RES2/confirmation > /dev/null
echo "Confirmada!"

echo "7. Cancelando (sem reembolso)..."
curl -s -X POST $BASE_URL/reservations/$RES2/cancellation | jq '{status, refund}'

# Relatórios
echo -e "\n8. Relatório de ocupação (2027-03-10):"
curl -s $BASE_URL/reports/occupancy?date=2027-03-10

echo -e "\n9. Relatório de receita (2027-03):"
curl -s $BASE_URL/reports/revenue?month=2027-03

echo -e "\n10. Mensagens enviadas:"
curl -s $BASE_URL/messages | jq 'length' | xargs echo "Total:"
curl -s $BASE_URL/messages | jq '[.[] | select(.status == "SENT")] | length' | xargs echo "SENT:"
curl -s $BASE_URL/messages | jq '[.[] | select(.status == "BLOCKED")] | length' | xargs echo "BLOCKED:"

echo -e "\n=== Teste Completo ==="
```

Execute com:
```bash
chmod +x teste-cenario.sh
./teste-cenario.sh
```

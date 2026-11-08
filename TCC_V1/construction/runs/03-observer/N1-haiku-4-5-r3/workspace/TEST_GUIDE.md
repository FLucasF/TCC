# Guia de Testes - Pousada Maré Alta

## Iniciando o Sistema

### Com PostgreSQL local
```bash
# Criar o banco de dados (se necessário)
createdb pousada

# Iniciar o servidor
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="password"
export PORT="7070"
java -jar target/reservations-system.jar
```

## Testando a API

### 1. Listar Quartos
```bash
curl http://localhost:7070/rooms
```

Resultado esperado:
```json
[
  {"number": 101, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  {"number": 102, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
  ...
]
```

### 2. Criar Reserva (Exemplo: 2 noites em julho - alta temporada)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resultado esperado:
- Status: 201
- Subtotal: 599.76 (2 noites de 299.88 cada, calculado com 20% acréscimo)
- Total: 599.76 (sem desconto no plano STANDARD)
- Status: PENDING

### 3. Confirmar Reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resultado esperado:
- Status: CONFIRMED
- amountPaid: 599.76

### 4. Mensagens Enviadas
```bash
curl http://localhost:7070/messages
```

Resultado esperado:
- 1 mensagem para o hóspede (João Silva)
- 1 mensagem para a governança
- 1 mensagem para o financeiro

### 5. Reserva com Desconto (NON_REFUNDABLE)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432109",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resultado esperado:
- Subtotal: 779.80 (2 noites de 389.90 cada, sem aumento - não é alta temporada)
- Discount: 116.97 (15% de 779.80)
- Total: 662.83

### 6. Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

Resultado esperado:
- Status: CHECKED_IN

### 7. Check-out (Recebe Comprovante)
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resultado esperado:
- Documento formatado com dados da reserva

### 8. Cancelamento (Antes do Check-in)
```bash
curl -X POST http://localhost:7070/reservations/2/cancellation
```

Resultado esperado:
- Status: CANCELLED
- Refund: 662.83 (se for STANDARD) ou 0 (se for NON_REFUNDABLE)

### 9. Relatório de Ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resultado esperado:
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### 10. Relatório Financeiro
```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Resultado esperado:
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
total,599.76
```

## Cenários de Erro

### Quarto Não Encontrado
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resultado esperado: 404 ROOM_NOT_FOUND

### Quarto Indisponível
```bash
# Tentar reservar o mesmo quarto nas mesmas datas
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Another Guest",
    "guestEmail": "another@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Resultado esperado: 409 ROOM_UNAVAILABLE

### Capacidade Excedida
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resultado esperado: 422 CAPACITY_EXCEEDED

## Notas Importantes

1. **Arredondamento**: Usa "meio para o par" (HALF_EVEN). Exemplo: 10.495 → 10.50, 10.505 → 10.50
2. **Alta Temporada**: 15 dez a fim de fev e julho inteiro (20% acréscimo)
3. **Saídas**: Quem sai no mesmo dia da noite não é contado na ocupação
4. **Refund**: Reservas NON_REFUNDABLE não devolvem nada quando canceladas
5. **Mensagens**: E-mails não são enviados de verdade, apenas armazenados no banco

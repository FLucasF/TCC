# Exemplos de uso da API

## Criar uma reserva

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

Resposta:
```json
{
  "id": 1,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0,
  "refund": null
}
```

## Confirmar uma reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**Efeitos:**
- Status muda para CONFIRMED
- Envia e-mail para o hóspede
- Envia cópia para arquivo@pousadamarealta.com.br

## Testar validação de e-mail

```bash
# E-mail inválido (sem domínio completo)
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Silva",
    "guestEmail": "ana.gmail",
    "guestCpf": "99988877766",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'

# Confirmar a reserva
curl -X POST http://localhost:7070/reservations/2/confirmation

# Verificar mensagens - a mensagem estará BLOCKED
curl http://localhost:7070/messages
```

## Criar reserva não reembolsável (15% de desconto)

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

Resposta mostrará:
```json
{
  "subtotal": 779.80,
  "discount": 116.97,
  "total": 662.83
}
```

## Cancelar reserva

```bash
# Cancelar reserva padrão (reembolsa o valor)
curl -X POST http://localhost:7070/reservations/1/cancellation

# Cancelar reserva não reembolsável (não reembolsa)
curl -X POST http://localhost:7070/reservations/3/cancellation
```

## Check-in e Check-out

```bash
# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out (retorna o comprovante)
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Segunda via do comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## Relatórios

```bash
# Relatório de ocupação para uma data
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"

# Relatório financeiro de um mês
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

## Modo de teste

```bash
# Iniciar em modo de teste
HOTEL_TEST_MODE=true java -jar target/reservas-1.0-SNAPSHOT.jar

# Criar e confirmar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "Teste",
    "guestEmail": "cliente@real.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-09-10",
    "checkOut": "2027-09-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }'

curl -X POST http://localhost:7070/reservations/1/confirmation

# Verificar mensagens - irão para testes@pousadamarealta.com.br
curl http://localhost:7070/messages | jq
```

## Exemplos de cálculo de preços

### Alta temporada (julho)
- Quarto 101 (STANDARD): R$ 249,90 → R$ 299,88 por noite
- 2 noites: R$ 599,76

### Baixa temporada (março)
- Quarto 101 (STANDARD): R$ 249,90 por noite
- 2 noites: R$ 499,80

### Plano não reembolsável
- 2 noites DELUXE (março): R$ 389,90 × 2 = R$ 779,80
- Desconto 15%: R$ 116,97
- Total: R$ 662,83

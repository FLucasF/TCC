# Exemplos de Uso do Sistema

## Cenário 1: Reserva Simples (STANDARD, Sem Desconto)

```bash
# 1. Criar reserva - March 10-12, 2027, STANDARD plan
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@email.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Resposta esperada:
{
  "id": 1,
  "roomNumber": 201,
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,      # 389.90 × 2
  "discount": 0.00,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}

# 2. Confirmar reserva (envia e-mail)
curl -X POST http://localhost:7070/reservations/1/confirmation

# 3. Verificar e-mail enviado
curl http://localhost:7070/messages
# Verifica que o e-mail foi salvo com assunto "Reserva confirmada - Pousada Maré Alta"

# 4. Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# 5. Check-out (retorna comprovante)
curl -X POST http://localhost:7070/reservations/1/check-out
# Retorna comprovante em JSON

# 6. Obter segunda via
curl http://localhost:7070/reservations/1/document
```

## Cenário 2: Alta Temporada com Desconto Non-Refundable

```bash
# July 10-12, 2027 (HIGH SEASON)
# STANDARD: 249.90 × 1.20 = 299.88/night × 2 = 599.76
# NON_REFUNDABLE discount: 599.76 × 0.15 = 89.96
# Total: 509.80

curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'

# Resposta:
{
  "id": 2,
  "roomNumber": 101,
  "nights": 2,
  "subtotal": 599.76,       # High season rate applied
  "discount": 89.96,        # 15% discount
  "total": 509.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## Cenário 3: Cancelamento e Reembolso

```bash
# Criar e confirmar uma reserva STANDARD
RESERVATION_ID=3
# ... criar e confirmar ...

# Cancelar
curl -X POST http://localhost:7070/reservations/$RESERVATION_ID/cancellation

# Resposta (STANDARD reembolsa tudo):
{
  "id": 3,
  "status": "CANCELLED",
  "amountPaid": 779.80,
  "refund": 779.80         # Total refund for STANDARD
}

# Para NON_REFUNDABLE: refund seria null (sem reembolso)
```

## Cenário 4: Verificar Conflito de Data

```bash
# Primeira reserva: 15-17 de julho
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Maria Santos",
    "guestEmail": "maria@email.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-07-15",
    "checkOut": "2027-07-17",
    "guests": 3,
    "ratePlan": "STANDARD"
  }'

# Confirmar primeira
curl -X POST http://localhost:7070/reservations/4/confirmation

# Tentar sobrepor: 16-18 de julho → ERRO
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Outro Hóspede",
    "guestEmail": "outro@email.com",
    "guestCpf": "99900011122",
    "checkIn": "2027-07-16",
    "checkOut": "2027-07-18",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Resposta (409 Conflict):
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for these dates"
}
```

## Cenário 5: Relatório de Ocupação

```bash
# Gerar relatório para 10 de julho de 2027
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"

# Resposta (CSV):
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,2,2027-07-10,2027-07-12
201,DELUXE,1,2027-03-10,2027-03-12
total,2/5
```

## Cenário 6: Relatório Financeiro

```bash
# Receita de julho de 2027
curl "http://localhost:7070/reports/revenue?month=2027-07"

# Resposta (CSV):
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
2,101,2027-07-10,2027-07-12,CONFIRMED,509.80
total,,,599.76
```

## Cenário 7: Relatório de Taxa de Turismo

```bash
# Taxa de turismo para julho (R$ 4,50 por hóspede/noite)
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"

# Resposta (CSV):
Pousada Maré Alta,tourism-tax,2027-07
reservation,room,check_in,check_out,guests,nights,tax
2,101,2027-07-10,2027-07-12,2,2,18.00
total,,,,27.00
```

## Cenário 8: Análise de Planos de Tarifa

```bash
# Comparar receita por plano em julho
curl "http://localhost:7070/reports/rate-plans?month=2027-07"

# Resposta (CSV):
Pousada Maré Alta,rate-plans,2027-07
rate_plan,reservations,amount
STANDARD,1,779.80
NON_REFUNDABLE,1,509.80
total,2,1289.60
```

## Cenário 9: Validação de Capacidade

```bash
# Tentar 4 hóspedes em quarto STANDARD (máximo 2)
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Família Grande",
    "guestEmail": "familia@email.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-03",
    "guests": 4,
    "ratePlan": "STANDARD"
  }'

# Resposta (422 Unprocessable Entity):
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Guests exceed room capacity"
}
```

## Cenário 10: Validação de Datas

```bash
# Tentar saída antes de entrada
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Cliente",
    "guestEmail": "cliente@email.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-05",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'

# Resposta (400 Bad Request):
{
  "error": "INVALID_REQUEST",
  "message": "Check-out must be after check-in"
}
```

## Cenário 11: Operação Não Permitida

```bash
# Tentar confirmar uma reserva já confirmada
# (Reserva 1 já está CONFIRMED)

curl -X POST http://localhost:7070/reservations/1/confirmation

# Resposta (409 Conflict):
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Only pending reservations can be confirmed"
}
```

## Cenário 12: Buscar Comprovante de Reserva Pendente

```bash
# Criar mas não confirmar
# Reserva 10 está PENDING

curl http://localhost:7070/reservations/10/document

# Resposta (409 Conflict):
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Document only available for confirmed or checked-in reservations"
}
```

## Fluxo Completo de Uma Estadia

```bash
# 1. Criar reserva
RESP=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Ana Costa",
    "guestEmail": "ana@email.com",
    "guestCpf": "33344455566",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-05",
    "guests": 3,
    "ratePlan": "STANDARD"
  }')

RES_ID=$(echo $RESP | jq '.id')
echo "Reserva criada: $RES_ID"
echo "Status: PENDING, Valor: $(echo $RESP | jq '.total')"

# 2. Confirmar (hóspede paga)
curl -s -X POST http://localhost:7070/reservations/$RES_ID/confirmation | jq '.status, .amountPaid'
# Status: CONFIRMED, amountPaid: valor total

# 3. Check-in (recepção)
curl -s -X POST http://localhost:7070/reservations/$RES_ID/check-in | jq '.status'
# Status: CHECKED_IN

# 4. Check-out (recepção entrega comprovante)
RECEIPT=$(curl -s -X POST http://localhost:7070/reservations/$RES_ID/check-out)
echo $RECEIPT | jq '.title, .reservationId, .guestName, .total'

# 5. Verificar finalização
curl -s http://localhost:7070/reservations/$RES_ID | jq '.status'
# Status: CHECKED_OUT

# Logs de operações
echo "Mensagens enviadas:"
curl -s http://localhost:7070/messages | jq '.[] | select(.to == "ana@email.com")'
```

## Notas Sobre Arredondamento

O sistema usa "HALF_EVEN" (arredondar para o par mais próximo):

```
249.90 × 1.20 = 299.88 (não 299.87)
389.90 × 1.20 = 467.88 (não 467.87)
579.90 × 1.20 = 695.88 (não 695.87)
```

Cada operação é arredondada:
1. Tarifa da noite (base × multiplicador)
2. Subtotal (soma das noites)
3. Desconto (subtotal × percentual)
4. Total (subtotal - desconto)

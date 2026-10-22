# Exemplos de Uso da API

## Configuração Inicial

```bash
# Subir PostgreSQL
docker run -d --name postgres-pousada \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=pousada123 \
  -p 5432:5432 \
  postgres:18

# Executar aplicação
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=pousada
export DB_PASSWORD=pousada123
export PORT=7070
java -jar target/reservation-system-1.0.0.jar
```

## Fluxo Completo de Reserva

### 1. Listar Quartos Disponíveis

```bash
curl http://localhost:7070/rooms
```

**Resposta:**
```json
[
  {
    "number": "101",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  {
    "number": "201",
    "type": "DELUXE",
    "capacity": 3,
    "nightlyRate": 389.90
  },
  {
    "number": "301",
    "type": "SUITE",
    "capacity": 4,
    "nightlyRate": 579.90
  }
]
```

### 2. Criar Reserva

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

**Resposta (201 Created):**
```json
{
  "id": 1,
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@example.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.0,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0.0,
  "refund": null
}
```

### 3. Confirmar Reserva (Pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**Resposta:**
```json
{
  "id": 1,
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  ...
}
```

**E-mail enviado automaticamente:**
```
Para: carla@example.com
Assunto: Reserva confirmada - Pousada Maré Alta

Olá, Carla Dias!

Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

Comprovante de reserva nº 1
Hóspede: Carla Dias (CPF 111.222.333-44)
Quarto: 201 (DELUXE)
Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
Diárias: R$ 779,80
Total: R$ 779,80

A entrada é a partir das 14h, e a saída, até as 12h.
Até breve!
```

### 4. Check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 5. Check-out e Comprovante

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

**Resposta:**
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "Carla Dias",
  "guestCpf": "11122233344",
  "roomNumber": "201",
  "roomType": "DELUXE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "nights": 2,
  "subtotal": 779.80,
  "discount": 0.0,
  "total": 779.80
}
```

### 6. Segunda Via do Comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## Cenários Especiais

### Reserva com Desconto (Não Reembolsável)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "301",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-04-01",
    "checkOut": "2027-04-05",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

**Cálculo:**
- 4 noites × R$ 579,90 = R$ 2.319,60 (subtotal)
- Desconto 15% = R$ 347,94
- **Total = R$ 1.971,66**

### Reserva em Alta Temporada

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Maria Costa",
    "guestEmail": "maria@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Cálculo:**
- Diária normal: R$ 249,90
- Alta temporada (+20%): R$ 299,88
- 2 noites = **R$ 599,76**

### Cancelamento com Reembolso

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

**Resposta (plano STANDARD):**
```json
{
  "status": "CANCELLED",
  "refund": 779.80
}
```

**Resposta (plano NON_REFUNDABLE):**
```json
{
  "status": "CANCELLED",
  "refund": 0.0
}
```

## Mensagens e Auditoria

### Listar Todas as Mensagens

```bash
curl http://localhost:7070/messages
```

**Resposta:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "carla@example.com",
    "subject": "Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  }
]
```

### E-mail Bloqueado (Inválido)

Se criar reserva com e-mail inválido:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Ana Souza",
    "guestEmail": "ana.gmail.com",
    "checkIn": "2027-05-01",
    "checkOut": "2027-05-02",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Ao confirmar, a mensagem aparece como BLOCKED:

```json
{
  "id": 3,
  "channel": "EMAIL",
  "to": "ana.gmail.com",
  "subject": "Reserva confirmada - Pousada Maré Alta",
  "status": "BLOCKED"
}
```

## Relatórios

### Ocupação por Noite

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

**Resposta (CSV):**
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
301,SUITE,5,2027-03-10,2027-03-15
total,2/5
```

### Receita por Mês

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

**Resposta (CSV):**
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CHECKED_OUT,779.80
3,102,2027-03-15,2027-03-16,CONFIRMED,249.90
7,301,2027-03-20,2027-03-25,CANCELLED,1969.62
total,2999.32
```

**Nota:** Reservas canceladas não reembolsáveis aparecem no relatório porque o dinheiro ficou com a pousada.

## Tratamento de Erros

### Quarto Não Encontrado (404)

```bash
curl http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": "999", ...}'
```

```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found: 999"
}
```

### Quarto Indisponível (409)

```bash
# Tentar reservar quarto já ocupado
```

```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

### Capacidade Excedida (422)

```bash
# 5 pessoas em quarto DELUXE (capacidade 3)
```

```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Room capacity exceeded"
}
```

### Operação Não Permitida (409)

```bash
# Tentar fazer check-in em reserva PENDING
curl -X POST http://localhost:7070/reservations/1/check-in
```

```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Only confirmed reservations can be checked in"
}
```

## Modo de Teste

### Ativar Modo de Teste

```bash
export HOTEL_TEST_MODE=true
java -jar target/reservation-system-1.0.0.jar
```

### Comportamento em Teste

Ao confirmar reserva com `HOTEL_TEST_MODE=true`:

**E-mail real NÃO é enviado.** Mensagem é redirecionada:

```json
{
  "id": 1,
  "channel": "EMAIL",
  "to": "testes@pousadamarealta.com.br",
  "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
  "body": "Destinatário original: carla@example.com\n\nOlá, Carla Dias!...",
  "status": "SENT"
}
```

**Sem cópia para arquivo** (apenas 1 mensagem, não 2)

**E-mails inválidos continuam bloqueados**

## Scripts Úteis

### Criar Múltiplas Reservas (teste de carga)

```bash
#!/bin/bash
for i in {1..10}; do
  curl -X POST http://localhost:7070/reservations \
    -H "Content-Type: application/json" \
    -d "{
      \"roomNumber\": \"$((100 + i % 5 + 1))\",
      \"guestName\": \"Guest $i\",
      \"guestEmail\": \"guest$i@example.com\",
      \"guestCpf\": \"1234567890$i\",
      \"checkIn\": \"2027-06-01\",
      \"checkOut\": \"2027-06-05\",
      \"guests\": 2,
      \"ratePlan\": \"STANDARD\"
    }"
done
```

### Confirmar Todas as Reservas Pendentes

```bash
#!/bin/bash
for id in {1..10}; do
  curl -X POST http://localhost:7070/reservations/$id/confirmation
done
```

### Backup de Mensagens

```bash
curl http://localhost:7070/messages > mensagens-$(date +%Y%m%d).json
```

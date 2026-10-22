# Exemplos de Uso da API

## Pré-requisitos

1. PostgreSQL rodando (ex: `docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:18`)
2. Sistema iniciado: `java -jar target/reservations-1.0.0.jar`

## Testes de Integração

Para rodar os testes de integração completos (requer PostgreSQL):

```bash
# Renomear o arquivo de volta
mv src/test/java/com/pousadamarealta/ReservationSystemIT.java.manual src/test/java/com/pousadamarealta/ReservationSystemIT.java

# Executar
mvn test -Dtest=ReservationSystemIT

# Renomear de volta se quiser
mv src/test/java/com/pousadamarealta/ReservationSystemIT.java src/test/java/com/pousadamarealta/ReservationSystemIT.java.manual
```

## Exemplos com curl

### 1. Listar quartos disponíveis

```bash
curl http://localhost:7070/rooms
```

Resposta:
```json
[
  {
    "number": "101",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  },
  ...
]
```

### 2. Criar uma reserva

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

Resposta (201 Created):
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
  "discount": 0.00,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

### 3. Confirmar reserva (pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Isso também envia um email para o hóspede (guardado internamente).

### 4. Verificar mensagens enviadas

```bash
curl http://localhost:7070/messages
```

### 5. Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 6. Fazer check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Retorna o comprovante.

### 7. Obter segunda via do comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

### 8. Cancelar reserva

```bash
curl -X POST http://localhost:7070/reservations/2/cancellation
```

### 9. Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Resposta (CSV):
```csv
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### 10. Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

### 11. Relatório de taxa de turismo

```bash
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"
```

### 12. Relatório de planos de tarifa

```bash
curl "http://localhost:7070/reports/rate-plans?month=2027-07"
```

## Erros Comuns

### Quarto não encontrado (404)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta (404):
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Quarto não encontrado: 999"
}
```

### Quarto indisponível (409)

Tentar reservar um quarto já ocupado no período.

Resposta (409):
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Quarto 101 não está disponível no período de 2027-07-10 a 2027-07-12"
}
```

### Capacidade excedida (422)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-08-10",
    "checkOut": "2027-08-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

Resposta (422):
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Quarto 101 comporta no máximo 2 hóspedes, mas foram solicitados 5"
}
```

### Operação não permitida (409)

Tentar fazer check-out sem ter feito check-in:

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

Resposta (409):
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Só é possível fazer check-out de uma reserva com check-in"
}
```

## Exemplo de Fluxo Completo

```bash
# 1. Criar reserva não reembolsável (com desconto)
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-15",
    "checkOut": "2027-07-17",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'

# 2. Confirmar
curl -X POST http://localhost:7070/reservations/2/confirmation

# 3. Ver email enviado
curl http://localhost:7070/messages

# 4. Check-in
curl -X POST http://localhost:7070/reservations/2/check-in

# 5. Check-out e receber comprovante
curl -X POST http://localhost:7070/reservations/2/check-out

# 6. Ver relatórios do mês
curl "http://localhost:7070/reports/occupancy?date=2027-07-15"
curl "http://localhost:7070/reports/revenue?month=2027-07"
curl "http://localhost:7070/reports/tourism-tax?month=2027-07"
curl "http://localhost:7070/reports/rate-plans?month=2027-07"
```

## Cálculo de Preços - Exemplos

### Alta Temporada (Julho)

- Quarto 101 (STANDARD): R$ 249,90 → R$ 299,88/noite (+20%)
- 2 noites: R$ 599,76

### Baixa Temporada (Março)

- Quarto 101 (STANDARD): R$ 249,90/noite
- 2 noites: R$ 499,80

### Desconto Não Reembolsável

- Quarto 201 (DELUXE) baixa temporada: R$ 389,90/noite
- 2 noites: R$ 779,80
- Desconto 15%: R$ 116,97
- **Total: R$ 662,83**

### Taxa de Turismo

- R$ 4,50 por hóspede por noite
- 2 hóspedes × 3 noites = R$ 27,00

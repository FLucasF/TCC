# Exemplos de uso da API

## Iniciando o sistema

```bash
# Subir o banco de dados
docker-compose up -d

# Compilar e executar
mvn package
java -jar target/marealta-1.0.0.jar

# Ou com variáveis de ambiente customizadas
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=postgres \
PORT=7070 \
java -jar target/marealta-1.0.0.jar
```

## Listando quartos disponíveis

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
    "number": "102",
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
    "number": "202",
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

## Criando uma reserva

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
  "discount": 0.00,
  "total": 779.80,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## Confirmando uma reserva (pagamento)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**Resposta:**
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
  "discount": 0.00,
  "total": 779.80,
  "status": "CONFIRMED",
  "amountPaid": 779.80,
  "refund": 0.00
}
```

Neste momento, um e-mail de confirmação é enviado (armazenado na lista de mensagens).

## Consultando mensagens enviadas

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
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada...",
    "status": "SENT"
  },
  {
    "id": 2,
    "channel": "EMAIL",
    "to": "arquivo@pousadamarealta.com.br",
    "subject": "[Cópia] Reserva confirmada - Pousada Maré Alta",
    "body": "Olá, Carla Dias!\n\nSua reserva na Pousada Maré Alta está confirmada...",
    "status": "SENT"
  }
]
```

## Fazendo check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

## Fazendo check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

**Resposta (comprovante):**
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
  "discount": 0.00,
  "total": 779.80
}
```

## Obtendo segunda via do comprovante

```bash
curl http://localhost:7070/reservations/1/document
```

## Cancelando uma reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

## Reserva com desconto (não reembolsável)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

**Resposta:**
```json
{
  "id": 2,
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "98765432100",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 1,
  "ratePlan": "NON_REFUNDABLE",
  "nights": 2,
  "subtotal": 499.80,
  "discount": 74.97,
  "total": 424.83,
  "status": "PENDING",
  "amountPaid": 0.00,
  "refund": null
}
```

## Reserva em alta temporada

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

**Resposta (noite em julho = +20%):**
```json
{
  "id": 3,
  "roomNumber": "101",
  "guestName": "Ana Costa",
  "guestEmail": "ana@example.com",
  "guestCpf": "55566677788",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 1,
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

Cada noite custa R$ 249,90 × 1,20 = R$ 299,88

## E-mail inválido (será bloqueado)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "102",
    "guestName": "Pedro Lima",
    "guestEmail": "pedro.gmail.com",
    "guestCpf": "12312312312",
    "checkIn": "2027-04-01",
    "checkOut": "2027-04-02",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

Após confirmar, a mensagem aparecerá como `BLOCKED` na lista de mensagens.

## Modo de teste

```bash
# Reiniciar o sistema em modo de teste
HOTEL_TEST_MODE=true java -jar target/marealta-1.0.0.jar

# Criar e confirmar uma reserva
curl -X POST http://localhost:7070/reservations/4/confirmation

# Verificar mensagens - será enviado para testes@pousadamarealta.com.br
curl http://localhost:7070/messages
```

**Resposta:**
```json
[
  {
    "id": 1,
    "channel": "EMAIL",
    "to": "testes@pousadamarealta.com.br",
    "subject": "[TESTE] Reserva confirmada - Pousada Maré Alta",
    "body": "Destinatário original: carla@example.com\n\nOlá, Carla Dias!\n\nSua reserva...",
    "status": "SENT"
  }
]
```

## Relatórios

### Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-03-10"
```

**Resposta (CSV):**
```csv
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-03-10,2027-03-12
total,1/5
```

### Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-03"
```

**Resposta (CSV):**
```csv
reservation,room,check_in,check_out,status,amount
1,201,2027-03-10,2027-03-12,CONFIRMED,779.80
total,779.80
```

## Tratamento de erros

### Quarto não encontrado (404)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "999",
    "guestName": "Teste",
    "guestEmail": "teste@example.com",
    "guestCpf": "12312312312",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

**Resposta:**
```json
{
  "error": "ROOM_NOT_FOUND",
  "message": "Room not found"
}
```

### Quarto indisponível (409)

```bash
# Tentar reservar o mesmo quarto nas mesmas datas
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "201",
    "guestName": "Outro Hóspede",
    "guestEmail": "outro@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

**Resposta:**
```json
{
  "error": "ROOM_UNAVAILABLE",
  "message": "Room is not available for the selected dates"
}
```

### Capacidade excedida (422)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "Família Grande",
    "guestEmail": "familia@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-05-01",
    "checkOut": "2027-05-02",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

**Resposta:**
```json
{
  "error": "CAPACITY_EXCEEDED",
  "message": "Number of guests exceeds room capacity"
}
```

### Operação não permitida (409)

```bash
# Tentar fazer check-in de uma reserva pendente
curl -X POST http://localhost:7070/reservations/1/check-in
```

**Resposta:**
```json
{
  "error": "OPERATION_NOT_ALLOWED",
  "message": "Operation not allowed in current reservation status"
}
```

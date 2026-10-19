# Exemplos de Uso da API

## Iniciar o servidor

```bash
java -jar target/hotel-reservations-1.0-SNAPSHOT.jar
```

## Listar quartos

```bash
curl http://localhost:7070/rooms | jq
```

## Criar uma reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Ana Silva",
    "guestEmail": "ana@gmail.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq
```

## Confirmar uma reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation | jq
```

## Ver mensagens enviadas

```bash
curl http://localhost:7070/messages | jq
```

## Obter comprovante (segunda via)

```bash
curl http://localhost:7070/reservations/1/document | jq
```

## Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

## Fazer check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out | jq
```

## Cancelar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation | jq
```

## Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Saída:
```csv
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

## Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Saída:
```csv
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
total,599.76
```

## Testar com email inválido

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Santos",
    "guestEmail": "joao@invalid",
    "guestCpf": "98765432100",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq

curl -X POST http://localhost:7070/reservations/2/confirmation | jq

# Verificar que a mensagem foi bloqueada
curl http://localhost:7070/messages | jq '.[] | select(.status == "BLOCKED")'
```

## Modo de teste

```bash
# Parar o servidor normal e iniciar em modo de teste
HOTEL_TEST_MODE=true java -jar target/hotel-reservations-1.0-SNAPSHOT.jar

# Em outro terminal, criar e confirmar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Maria Costa",
    "guestEmail": "maria@example.com",
    "guestCpf": "11111111111",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-03",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq

curl -X POST http://localhost:7070/reservations/1/confirmation | jq

# Verificar que o email foi redirecionado
curl http://localhost:7070/messages | jq '.[0]'
```

## Casos de erro

### Quarto não encontrado (404)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Test",
    "guestEmail": "test@test.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-09-01",
    "checkOut": "2027-09-03",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq
```

### Capacidade excedida (422)

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@test.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-09-01",
    "checkOut": "2027-09-03",
    "guests": 5,
    "ratePlan": "STANDARD"
  }' | jq
```

### Operação não permitida (409)

```bash
# Criar reserva pendente e tentar fazer check-in sem confirmar
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Test",
    "guestEmail": "test@test.com",
    "guestCpf": "44444444444",
    "checkIn": "2027-10-01",
    "checkOut": "2027-10-03",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq

curl -X POST http://localhost:7070/reservations/1/check-in | jq
```

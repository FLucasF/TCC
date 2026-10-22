# Quick Start - Pousada Maré Alta

## 30 segundos para começar

### 1. Compilar (requer Java 21 + Maven)

```bash
mvn clean package
```

### 2. Iniciar PostgreSQL (com Docker)

```bash
docker run --name pousada-db \
  -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=pousada \
  -p 5432:5432 -d postgres:18
```

### 3. Iniciar a aplicação

```bash
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=senha123

java -jar target/app.jar
```

Pronto! API disponível em `http://localhost:7070`

## Testes Básicos

### Ver quartos disponíveis
```bash
curl http://localhost:7070/rooms | jq .
```

### Criar reserva (CPF)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "12345678901",
    "roomNumber": 201,
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq .
```

### Confirmar reserva (com pagamento)
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}' | jq .
```

### Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

### Check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

### Ver mensagens (emails enviados)
```bash
curl http://localhost:7070/messages | jq .
```

### Ver relatório de ocupação
```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-11"
```

### Ver relatório financeiro
```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

## Variáveis de Ambiente

```bash
DB_URL         jdbc:postgresql://localhost:5432/pousada
DB_USER        postgres
DB_PASSWORD    senha123
PORT           7070 (padrão)
```

## Documentação Completa

- **README.md** - Instruções gerais
- **TESTING.md** - Exemplos detalhados de testes
- **ARCHITECTURE.md** - Detalhes técnicos
- **IMPLEMENTATION.md** - O que foi implementado

## Com Docker Compose (mais fácil)

```bash
docker-compose up
```

Inicia PostgreSQL + aplicação juntos.

---

Veja mais em `README.md` e `TESTING.md`.

# Guia de Início Rápido

## Com Docker Compose

Para testar rapidamente o sistema com PostgreSQL em Docker:

```bash
# 1. Subir o banco de dados
docker-compose up -d postgres

# 2. Esperar o banco estar pronto (healthcheck verifica)
docker-compose ps

# 3. Compilar o projeto
mvn clean package

# 4. Executar o servidor
DB_URL=jdbc:postgresql://localhost:5432/marealta \
DB_USER=marealta \
DB_PASSWORD=marealta123 \
PORT=7070 \
java -jar target/marealta-1.0.0.jar

# 5. Testar a API (em outro terminal)
curl http://localhost:7070/rooms | jq
```

## Exemplo de Fluxo Completo

### 1. Listar quartos

```bash
curl -X GET http://localhost:7070/rooms
```

Resposta:
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

### 2. Criar uma reserva

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

Resposta:
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.0,
  "total": 599.76,
  "status": "PENDING",
  "amountPaid": 0.0,
  "refund": null
}
```

### 3. Confirmar a reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Resposta:
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.0,
  "total": 599.76,
  "status": "CONFIRMED",
  "amountPaid": 599.76,
  "refund": null
}
```

### 4. Verificar mensagens enviadas

```bash
curl -X GET http://localhost:7070/messages | jq
```

Resposta incluirá:
- Email para o hóspede (assunto: "Reserva confirmada - Pousada Maré Alta")
- Email para governança (assunto: "Reserva confirmada - quarto 101")
- Email para financeiro (assunto: "Pagamento recebido - reserva 1")

### 5. Obter comprovante

```bash
curl -X GET http://localhost:7070/reservations/1/document
```

Resposta:
```json
{
  "title": "Comprovante de reserva",
  "reservationId": 1,
  "guestName": "João Silva",
  "guestCpf": "12345678901",
  "roomNumber": 101,
  "roomType": "STANDARD",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "nights": 2,
  "subtotal": 599.76,
  "discount": 0.0,
  "total": 599.76
}
```

### 6. Fazer check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### 7. Fazer check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

### 8. Gerar relatórios

Ocupação para uma data:
```bash
curl -X GET "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Receita para um mês:
```bash
curl -X GET "http://localhost:7070/reports/revenue?month=2027-07"
```

## Testes com Alta Temporada

A alta temporada (com 20% de acréscimo) aplica-se em:
- 15 de dezembro até o fim de fevereiro
- Mês inteiro de julho

Exemplo:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-12-20",
    "checkOut": "2027-12-22",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

Resultado:
- Tarifa diária: R$ 389,90 × 1,20 = R$ 467,88
- 2 noites: R$ 935,76
- Desconto NON_REFUNDABLE (15%): R$ 140,36
- Total: R$ 795,40

## Parar a Execução

```bash
# Parar o servidor (Ctrl+C no terminal)

# Parar o banco de dados
docker-compose down

# Remover dados (para começar do zero)
docker-compose down -v
```

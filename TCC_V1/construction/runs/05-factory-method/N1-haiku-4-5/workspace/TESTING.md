# Guia de Testes

## Configuração do Ambiente de Teste

### Com Docker Compose (Recomendado)

```bash
docker-compose up
```

Aguarde até ver a mensagem "Server started".

### Manual

1. Inicie o PostgreSQL:
```bash
docker run -d \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=pousada \
  -e POSTGRES_PASSWORD=senha123 \
  -p 5432:5432 \
  postgres:18-alpine
```

2. Compile e execute:
```bash
mvn clean package
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="pousada"
export DB_PASSWORD="senha123"
export PORT=7070
java -jar target/pousada-mare-alta-1.0.0.jar
```

## Testes de Funcionalidade

### 1. Teste de Criação de Reserva com CPF

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }' | jq .
```

**Esperado**: Resposta com id gerado, status PENDING, amountPaid = 0

### 2. Teste de Cálculo de Preço em Alta Temporada

Julho é alta temporada. Para um STANDARD (R$ 249,90):
- Diária em alta temporada: R$ 249,90 × 1.20 = R$ 299,88
- 2 noites: R$ 299,88 × 2 = R$ 599,76

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }' | jq '.subtotal, .discount, .total'
```

**Esperado**: subtotal = 599.76, discount = 0, total = 599.76

### 3. Teste de Desconto NON_REFUNDABLE

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Carlos Oliveira",
    "guestEmail": "carlos@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }' | jq '.subtotal, .discount, .total'
```

**Esperado**: 
- Diária SUITE em alta temporada: R$ 579,90 × 1.20 = R$ 695,88
- Subtotal: R$ 695,88 × 2 = R$ 1391,76
- Desconto: R$ 1391,76 × 0,15 = R$ 208,76
- Total: R$ 1183,00

### 4. Teste de Erro: Capacidade Excedida

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 3,
    "ratePlan": "STANDARD"
  }'
```

**Esperado**: 422 CAPACITY_EXCEEDED (quarto 101 comporta apenas 2 pessoas)

### 5. Teste de Error: Quarto Indisponível

```bash
# Criar primeira reserva
RESERVATION=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "123.123.123-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')

# Confirmar primeira reserva
ID=$(echo $RESERVATION | jq -r '.id')
curl -X POST http://localhost:7070/reservations/$ID/confirmation

# Tentar criar segunda reserva na mesma data
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Bruno Silva",
    "guestEmail": "bruno@example.com",
    "guestCpf": "456.456.456-00",
    "checkIn": "2027-03-11",
    "checkOut": "2027-03-13",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

**Esperado**: 409 ROOM_UNAVAILABLE

### 6. Teste de Confirmação e Cancelamento

```bash
# Criar e confirmar
RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 202,
    "guestName": "Laura Martins",
    "guestEmail": "laura@example.com",
    "guestCpf": "789.789.789-00",
    "checkIn": "2027-08-01",
    "checkOut": "2027-08-03",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')
ID=$(echo $RES | jq -r '.id')

# Confirmar
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation | jq '.status, .amountPaid'

# Cancelar (deve reembolsar porque é STANDARD)
curl -s -X POST http://localhost:7070/reservations/$ID/cancellation | jq '.status, .refund'
```

**Esperado**: 
- Após confirmação: status = CONFIRMED, amountPaid > 0
- Após cancelamento: status = CANCELLED, refund = amountPaid

### 7. Teste de Documento Fatura (CNPJ)

```bash
RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 203,
    "guestName": "Felipe Rocha",
    "guestEmail": "felipe@company.com",
    "companyCnpj": "12.345.678/0001-90",
    "companyName": "TechCorp Ltda",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
ID=$(echo $RES | jq -r '.id')

# Confirmar
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation

# Obter documento
curl -s -X GET http://localhost:7070/reservations/$ID/document | jq '.text'
```

**Esperado**: Fatura de hospedagem com ISS retido de 5%

### 8. Teste de Documento Booking Receipt (Passaporte)

```bash
RES=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 204,
    "guestName": "Michael Johnson",
    "guestEmail": "michael@example.com",
    "guestPassport": "AB123456",
    "guestCountry": "United States",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }')
ID=$(echo $RES | jq -r '.id')

# Confirmar
curl -s -X POST http://localhost:7070/reservations/$ID/confirmation

# Obter documento
curl -s -X GET http://localhost:7070/reservations/$ID/document | jq '.text'
```

**Esperado**: Booking receipt em inglês com BRL currency

### 9. Teste de Relatório de Ocupação

```bash
# Criar e confirmar 2 reservas para a mesma data
# ...
curl -s http://localhost:7070/reports/occupancy?date=2027-03-10
```

**Esperado**: CSV com os quartos ocupados naquela noite

### 10. Teste de Relatório Financeiro

```bash
curl -s "http://localhost:7070/reports/revenue?month=2027-03"
```

**Esperado**: CSV com receita bruta do mês (menos reembolsos)

## Validações Automáticas com Maven

```bash
mvn verify
```

Execute após qualquer mudança no código para garantir que tudo ainda funciona.

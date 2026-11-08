# Guia de Testes - Pousada Maré Alta

Este documento descreve cenários de teste importantes para o sistema de reservas.

## Pré-requisitos

1. PostgreSQL 18 rodando e acessível
2. Banco de dados criado e variáveis de ambiente configuradas:
   ```bash
   export DB_URL="jdbc:postgresql://localhost:5432/pousada"
   export DB_USER="seu_usuario"
   export DB_PASSWORD="sua_senha"
   export PORT=7070
   ```

## Iniciando o Servidor

```bash
java -jar target/mare-alta-1.0.0.jar
```

O servidor iniciará na porta 7070 por padrão.

## Cenários de Teste

### 1. Criar Reserva em Temporada Baixa (STANDARD)

**Requisição:**
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Resultado Esperado:**
- Status 201
- Subtotal: R$ 499,80 (249,90 × 2 noites)
- Desconto: R$ 0,00
- Total: R$ 499,80
- Status: PENDING

### 2. Criar Reserva em Alta Temporada (JULHO)

**Requisição:**
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "98765432100",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**Resultado Esperado:**
- Subtotal: R$ 599,76 (249,90 × 1,20 × 2 noites)
- Total: R$ 599,76

### 3. Plano NON_REFUNDABLE com Desconto

**Requisição:**
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Pedro Costa",
    "guestEmail": "pedro@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-13",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

**Resultado Esperado:**
- Subtotal: R$ 1.169,70 (389,90 × 3 noites)
- Desconto: R$ 175,46 (1.169,70 × 0,15)
- Total: R$ 994,24

### 4. Validações de Erro

#### Quarto não existe:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 999,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

**Resultado Esperado:** 404 ROOM_NOT_FOUND

#### Capacidade excedida:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 5,
    "ratePlan": "STANDARD"
  }'
```

**Resultado Esperado:** 422 CAPACITY_EXCEEDED

#### Check-out antes de check-in:
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "Test",
    "guestEmail": "test@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-10",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

**Resultado Esperado:** 400 INVALID_REQUEST

### 5. Ciclo Completo de Reserva

**Passo 1: Criar Reserva**
```bash
RESPONSE=$(curl -s -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Ana Lima",
    "guestEmail": "ana@example.com",
    "guestCpf": "55566677788",
    "checkIn": "2027-04-15",
    "checkOut": "2027-04-18",
    "guests": 2,
    "ratePlan": "STANDARD"
  }')

RESERVATION_ID=$(echo $RESPONSE | jq '.id')
```

**Passo 2: Confirmar Reserva**
```bash
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 749.70}'
```

**Resultado Esperado:**
- Status: CONFIRMED
- amountPaid: 749.70
- Email enviado: verificar em GET /messages

**Passo 3: Check-in**
```bash
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-in
```

**Resultado Esperado:** Status: CHECKED_IN

**Passo 4: Check-out**
```bash
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-out
```

**Resultado Esperado:** Status: CHECKED_OUT + Comprovante

### 6. Cancelamento com Reembolso

**Requisição:**
```bash
curl -s -X POST http://localhost:7070/reservations/$RESERVATION_ID/cancellation
```

**Resultado Esperado (se era STANDARD):**
- Status: CANCELLED
- refund: igual ao amountPaid

**Resultado Esperado (se era NON_REFUNDABLE):**
- Status: CANCELLED
- refund: 0.00

### 7. Relatório de Ocupação

```bash
curl -s "http://localhost:7070/reports/occupancy?date=2027-07-10" | head -20
```

**Esperado:**
```
Pousada Maré Alta,occupancy,2027-07-10
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
102,STANDARD,2,2027-07-10,2027-07-13
...
total,N/5
```

### 8. Relatório Financeiro

```bash
curl -s "http://localhost:7070/reports/revenue?month=2027-07"
```

**Esperado:**
```
Pousada Maré Alta,revenue,2027-07
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76
...
total,,,,1234.56
```

### 9. Taxa de Turismo

```bash
curl -s "http://localhost:7070/reports/tourism-tax?month=2027-07"
```

**Esperado:**
- Cada linha com: reservation_id, room, check_in, check_out, guests, nights, tax
- tax = 4,50 × guests × nights

### 10. Análise por Plano de Tarifa

```bash
curl -s "http://localhost:7070/reports/rate-plans?month=2027-07"
```

**Esperado:**
```
Pousada Maré Alta,rate-plans,2027-07
rate_plan,reservations,amount
STANDARD,2,1199.52
NON_REFUNDABLE,1,789.30
total,3,1988.82
```

## Teste de Alta Temporada (DEZEMBRO)

Data de 15 de dezembro é alta temporada:

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 301,
    "guestName": "Carlos Mendes",
    "guestEmail": "carlos@example.com",
    "guestCpf": "99988877766",
    "checkIn": "2027-12-20",
    "checkOut": "2027-12-23",
    "guests": 3,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

**Resultado Esperado:**
- SUITE por 3 noites em alta temporada: 579,90 × 1,20 × 3 = 2.087,64
- Com 15% desconto NON_REFUNDABLE: 2.087,64 - 313,15 = 1.774,49

## Testes Unitários

```bash
mvn test
```

Todos os testes devem passar:
- PriceUtilTest: Validar cálculos de preço e arredondamento
- DateUtilTest: Validar detecção de alta temporada
- PricingServiceTest: Validar cenários completos de preço

## Checklist de Validação

- [ ] Quartos retornam corretamente com capacidades
- [ ] Reserva em temporada baixa calcula preço correto
- [ ] Reserva em alta temporada aplica 20% acréscimo
- [ ] Plano NON_REFUNDABLE aplica 15% desconto
- [ ] Arredondamento "meio para o par" funciona em todos os passos
- [ ] Quarto indisponível bloqueia reserva sobreposta
- [ ] Confirmação envia email e persiste amountPaid
- [ ] Check-in/Check-out seguem ciclo correto
- [ ] Cancelamento reembolsa se STANDARD, não reembolsa se NON_REFUNDABLE
- [ ] Relatório ocupação mostra apenas reservas confirmadas/checadas
- [ ] Relatório financeiro mostra net amount (após reembolso)
- [ ] Taxa de turismo calcula corretamente por hóspede/noite
- [ ] Análise por plano agrega corretamente
- [ ] Segunda via do comprovante retorna para reserva paga

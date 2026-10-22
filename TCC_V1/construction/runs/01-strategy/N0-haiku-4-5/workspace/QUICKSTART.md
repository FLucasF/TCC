# Quick Start - Pousada Maré Alta

## 1 minuto para começar

### Passo 1: Preparar o Banco
```bash
# Certifique-se que PostgreSQL está rodando
psql -U postgres -c "CREATE DATABASE pousada;"
```

### Passo 2: Configurar Variáveis
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha_aqui"
export PORT=7070
```

### Passo 3: Executar
```bash
java -jar target/mare-alta-1.0.0.jar
```

Você verá na saída algo como:
```
INFO io.javalin.Javalin - Starting Javalin...
INFO io.javalin.Javalin - Listening on http://localhost:7070/
```

### Passo 4: Testar
```bash
# Em outro terminal
curl http://localhost:7070/rooms | jq

# Deve retornar 5 quartos:
# [
#   {"number": 101, "type": "STANDARD", "capacity": 2, "nightlyRate": 249.90},
#   ...
# ]
```

## Criar Sua Primeira Reserva

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
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

Resposta (reserve o ID para próximas operações):
```json
{
  "id": 1,
  "roomNumber": 101,
  "guestName": "João Silva",
  "status": "PENDING",
  "nights": 2,
  "subtotal": 599.80,
  "discount": 0.00,
  "total": 599.80,
  "amountPaid": 0.00
}
```

## Fluxo Completo

```bash
# 1. Criar reserva (PENDING)
RESERVATION_ID=1

# 2. Confirmar (PENDING → CONFIRMED)
curl -X POST http://localhost:7070/reservations/$RESERVATION_ID/confirmation

# 3. Consultar (verá email enviado)
curl http://localhost:7070/messages

# 4. Check-in (CONFIRMED → CHECKED_IN)
curl -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-in

# 5. Check-out (CHECKED_IN → CHECKED_OUT, retorna comprovante)
curl -X POST http://localhost:7070/reservations/$RESERVATION_ID/check-out
```

## Testar Planos de Tarifa

### 1. NON_REFUNDABLE (15% desconto)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Maria Santos",
    "guestEmail": "maria@example.com",
    "guestCpf": "987.654.321-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "NON_REFUNDABLE"
  }'
```

### 2. CORPORATE (TECNOSUL - 12% desconto)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carlos Empresa",
    "guestEmail": "carlos@tecnosul.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "CORPORATE",
    "companyCode": "TECNOSUL"
  }'
```

### 3. WEEKEND_PACKAGE (Sexta-domingo)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Ana Costa",
    "guestEmail": "ana@example.com",
    "guestCpf": "555.666.777-88",
    "checkIn": "2027-03-12",
    "checkOut": "2027-03-15",
    "guests": 2,
    "ratePlan": "WEEKEND_PACKAGE"
  }'
```

### 4. LONG_STAY (7+ noites, 10% desconto)
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 102,
    "guestName": "Pedro Morador",
    "guestEmail": "pedro@example.com",
    "guestCpf": "999.888.777-66",
    "checkIn": "2027-03-01",
    "checkOut": "2027-03-08",
    "guests": 1,
    "ratePlan": "LONG_STAY"
  }'
```

## Relatórios

### Ocupação de um dia
```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

Retorna CSV:
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
total,1/5
```

### Receita de um mês
```bash
curl http://localhost:7070/reports/revenue?month=2027-07
```

Retorna CSV:
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.80
total,,,,-,599.80
```

## Cancelamento

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

Retorna a reserva com:
- status: "CANCELLED"
- refund: valor reembolsado (depende do plano)

## Erros Comuns

### Quarto não existe
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 999, ...}'
```
Resposta: `404 ROOM_NOT_FOUND`

### Quarto ocupado
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 101, "checkIn": "2027-07-10", ...}'
```
Resposta: `409 ROOM_UNAVAILABLE`

### Muitos hóspedes
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"roomNumber": 101, "guests": 5, ...}'
```
Resposta: `422 CAPACITY_EXCEEDED`

### CORPORATE sem empresa
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"ratePlan": "CORPORATE", ...}'
```
Resposta: `400 INVALID_REQUEST` - Company code required

### WEEKEND_PACKAGE sem sexta-domingo
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{"checkIn": "2027-03-01", "checkOut": "2027-03-08", "ratePlan": "WEEKEND_PACKAGE", ...}'
```
Resposta: `400 INVALID_REQUEST` - Must start on Friday

## Documentação Completa

- **README.md** - Todos os endpoints com detalhes
- **VALIDATION.md** - Validação dos cálculos
- **DEPLOYMENT.md** - Deploy em produção

## Suporte

Para problemas:
1. Verifique se PostgreSQL está rodando
2. Verifique as variáveis de ambiente
3. Consulte os logs da aplicação
4. Leia DEPLOYMENT.md para troubleshooting

---

**Divirta-se! O sistema está pronto para usar! 🚀**

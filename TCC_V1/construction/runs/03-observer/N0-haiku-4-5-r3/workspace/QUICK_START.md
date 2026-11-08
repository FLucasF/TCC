# Quick Start - Pousada Maré Alta

## 30 segundos para ter o sistema rodando

### 1. Pré-requisitos
- Java 21
- PostgreSQL 18
- Maven 3.6+

### 2. Compilar
```bash
mvn clean package
```

### 3. Banco de Dados
```bash
createdb pousada_marealta
```

### 4. Executar
```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada_marealta"
export DB_USER=$(whoami)
export DB_PASSWORD=""
java -jar target/pousada-system.jar
```

### 5. Testar
```bash
# Em outro terminal
curl http://localhost:7070/rooms | jq .

# Criar uma reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 101,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-00",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 1,
    "ratePlan": "STANDARD"
  }'
```

## Que tipo de sistema é este?

✅ API REST completa para reservas
✅ Banco PostgreSQL automático
✅ Sistema de notificações extensível
✅ Relatórios em CSV
✅ Cálculo de preços com alta temporada
✅ Validações completas
✅ Tratamento de erros apropriado

## Estrutura de Quartos

| Quarto | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101, 102 | STANDARD | 2 | R$ 249,90 |
| 201, 202 | DELUXE | 3 | R$ 389,90 |
| 301 | SUITE | 4 | R$ 579,90 |

## Planos de Tarifa

- **STANDARD**: Reembolso total em cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

## Alta Temporada

- 15 de dezembro a 29 de fevereiro: +20%
- Julho inteiro: +20%

## Endpoints Principais

```
GET /rooms
POST /reservations
GET /reservations/{id}
POST /reservations/{id}/confirmation
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
POST /reservations/{id}/cancellation
GET /reservations/{id}/document
GET /messages
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```

## Notificações Automáticas

Quando uma reserva é confirmada:
- 📧 Email ao hóspede (comprovante)
- 📧 Email à governança (datas e hóspedes)
- 📧 Email ao financeiro (valor pago)

Quando cancelada (se confirmada):
- 📧 Email à governança (datas liberadas)
- 📧 Email ao financeiro (se houver reembolso)

## Próximas Documentações

- **README.md** - Setup detalhado
- **EXAMPLES.md** - Exemplos de curl
- **ARCHITECTURE.md** - Design do sistema
- **NOTIFICATIONS.md** - Como estender notificações

# Como usar o Sistema de Reservas da Pousada Maré Alta

## Inicializar

### Pré-requisitos
- Java 21
- PostgreSQL 18

### Ambiente

Defina as variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/reservas"
export DB_USER="reservas"
export DB_PASSWORD="reservas"
export PORT="7070"
```

### Iniciar o servidor

```bash
java -jar target/reservas.jar
```

O servidor inicializará na porta 7070 e criará automaticamente as tabelas necessárias.

## API Endpoints

### Quartos

**GET /rooms** - Lista todos os quartos

```bash
curl http://localhost:7070/rooms
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

### Reservas

**POST /reservations** - Criar uma nova reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

**GET /reservations/{id}** - Obter dados da reserva

```bash
curl http://localhost:7070/reservations/1
```

**POST /reservations/{id}/confirmation** - Confirmar a reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

**POST /reservations/{id}/check-in** - Fazer o check-in

```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

**POST /reservations/{id}/check-out** - Fazer o check-out

```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

**POST /reservations/{id}/cancellation** - Cancelar a reserva

```bash
curl -X POST http://localhost:7070/reservations/1/cancellation
```

**GET /reservations/{id}/document** - Obter o comprovante (segunda via)

```bash
curl http://localhost:7070/reservations/1/document
```

### Mensagens

**GET /messages** - Listar todas as mensagens enviadas

```bash
curl http://localhost:7070/messages
```

### Relatórios

**GET /reports/occupancy?date=YYYY-MM-DD** - Relatório de ocupação

```bash
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

Retorna CSV com a ocupação de um dia específico.

**GET /reports/revenue?month=YYYY-MM** - Relatório financeiro

```bash
curl "http://localhost:7070/reports/revenue?month=2027-07"
```

Retorna CSV com as reservas que começam em um mês específico.

## Fluxo de Operação

1. **Criar reserva** (status: PENDING)
2. **Confirmar reserva** (status: CONFIRMED) - E-mails são enviados
3. **Check-in** (status: CHECKED_IN)
4. **Check-out** (status: CHECKED_OUT)

### Cancelamento

Uma reserva pode ser cancelada em dois momentos:

- **Pendente**: Sem custo ao hóspede
- **Confirmada**: Depende do plano de tarifa
  - STANDARD: Reembolso integral
  - NON_REFUNDABLE: Sem reembolso

## Sistema de Notificações

Quando uma reserva é confirmada:
- Hóspede recebe e-mail com o comprovante
- Governança recebe notificação com datas e número de hóspedes
- Financeiro recebe notificação com o valor pago

Quando uma reserva confirmada é cancelada:
- Governança recebe notificação sobre as datas que ficaram livres
- Financeiro recebe notificação apenas se houver reembolso

As mensagens são armazenadas no banco de dados e podem ser consultadas via GET /messages.

## Para Desenvolvimento

### Iniciar um banco PostgreSQL local

```bash
docker-compose up
```

### Compilar

```bash
mvn clean compile
```

### Testar

```bash
mvn test
```

### Build completo

```bash
mvn clean verify
```

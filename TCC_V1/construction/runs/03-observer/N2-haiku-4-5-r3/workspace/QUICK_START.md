# Quick Start - Pousada Maré Alta

## 1. Iniciar PostgreSQL

```bash
# Criar banco de dados
createdb pousada_maraealta
```

## 2. Executar a Aplicação

```bash
# Com variáveis de ambiente
export DB_URL=jdbc:postgresql://localhost:5432/pousada_maraealta
export DB_USER=postgres
export DB_PASSWORD=sua_senha
export PORT=7070

java -jar target/mare-alta-reservas.jar
```

A aplicação iniciará na porta 7070 (ou a definida em `PORT`).

## 3. Testar a API

```bash
# Listar quartos
curl http://localhost:7070/rooms

# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "123.456.789-10",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva (substitua 1 pelo ID da reserva)
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": 599.76}'

# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out (retorna recibo)
curl -X POST http://localhost:7070/reservations/1/check-out

# Listar mensagens enviadas
curl http://localhost:7070/messages

# Relatório de ocupação
curl http://localhost:7070/reports/occupancy?date=2027-07-10

# Relatório de receita
curl http://localhost:7070/reports/revenue?month=2027-07
```

## 4. Estrutura do Projeto

- **Domain**: Lógica de negócio pura (Money, Reservation, Room, etc.)
- **Application**: Serviços que orquestram operações (ReservationService)
- **Infrastructure**: Persistência, Web API, Notificações
- **Migrations**: Flyway SQL migrations executadas automaticamente

## 5. Banco de Dados

Migrações automáticas criam:
- Tabela `rooms` com 5 quartos pré-carregados
- Tabela `reservations` com ciclo de vida completo
- Tabela `notifications` para auditoria de mensagens enviadas

## 6. Sistema de Notificações

Implementado via Observer Pattern. Novos notificadores podem ser adicionados em `GuesthouseApi.java`:

```java
List<NotificationSubscriber> subscribers = Arrays.asList(
    new GuestNotifier(),
    new GovernanceNotifier(),
    new FinanceNotifier()
    // Adicione novos aqui!
);
```

Cada notificador decide independentemente se deve tratar o evento.

## Docs Completas

Veja `README.md` para documentação completa da API e arquitetura.

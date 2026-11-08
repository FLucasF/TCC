# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, construído com Java 21, Javalin 6.7.0 e PostgreSQL 18.

## Arquitetura

O sistema utiliza o **padrão Observer** para notificações de eventos de reserva:

- Quando uma reserva é **confirmada**, três notificações são enviadas:
  - **Hóspede**: e-mail com comprovante da reserva
  - **Governança**: e-mail com informações para preparo do quarto
  - **Financeiro**: e-mail sobre pagamento recebido

- Quando uma reserva **confirmada** é **cancelada**, notificações são enviadas para:
  - **Governança**: e-mail sobre datas liberadas
  - **Financeiro**: e-mail sobre reembolso (apenas se houver valor a devolver)

### Extensibilidade

Para adicionar novos interessados (restaurante, transfer, etc.), basta:
1. Criar uma nova classe que implementa `ReservationEventListener`
2. Registrar o listener no `EventPublisher` em `Application.java`

Nenhuma modificação é necessária nas classes existentes.

## Estrutura do Projeto

```
src/main/java/br/com/pousadamarealta/
├── Application.java                 # Ponto de entrada
├── domain/                          # Modelos de domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   └── enums (RoomType, ReservationStatus, RatePlan)
├── events/                          # Sistema de eventos (Observer)
│   ├── ReservationEvent.java
│   ├── ReservationConfirmedEvent.java
│   ├── ReservationCancelledEvent.java
│   ├── EventPublisher.java
│   └── listeners/
│       ├── GuestNotifier.java
│       ├── HousekeepingNotifier.java
│       └── FinanceNotifier.java
├── repository/                      # Acesso a dados
├── service/                         # Lógica de negócio
└── api/                            # Controllers REST
```

## Executando o Sistema

### Requisitos
- Java 21
- PostgreSQL 18
- Maven 3.6+

### Variáveis de Ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="senha"
export PORT=7070  # opcional, padrão: 7070
```

### Build

```bash
mvn package
```

### Execução

```bash
java -jar target/reservations-1.0.0.jar
```

O sistema inicializa automaticamente o banco de dados (cria tabelas e popula os quartos).

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Regras de Negócio

### Quartos
- 101, 102: STANDARD (2 pessoas, R$ 249,90/noite)
- 201, 202: DELUXE (3 pessoas, R$ 389,90/noite)
- 301: SUITE (4 pessoas, R$ 579,90/noite)

### Alta Temporada (+20%)
- 15 de dezembro a 28/29 de fevereiro
- Todo mês de julho

### Planos de Tarifa
- **STANDARD**: Preço normal, reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Estados da Reserva
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
                ↓
            CANCELLED

## Testes

```bash
mvn verify
```

Inclui:
- Testes unitários de cálculo de preços e alta temporada
- Testes de integração do fluxo completo de reservas
- Testes de relatórios
- Verificação do sistema de notificações (Observer)

## Exemplo de Uso

```bash
# Criar reserva
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

# Confirmar reserva (envia notificações)
curl -X POST http://localhost:7070/reservations/1/confirmation

# Verificar mensagens enviadas
curl http://localhost:7070/messages
```

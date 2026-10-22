# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Arquitetura

O sistema implementa o padrão **Observer** para notificações, permitindo que múltiplos interessados (hóspede, governança, financeiro) sejam notificados automaticamente quando uma reserva é confirmada ou cancelada, sem que o código de reservas precise conhecê-los. Novos observadores podem ser facilmente adicionados sem modificar o código existente.

### Estrutura

- **Domain**: Entidades do negócio (Room, Reservation, Message)
- **Service**: Lógica de negócio e orquestração
- **Service/Observer**: Implementação do padrão Observer para notificações
- **Repository**: Acesso a dados PostgreSQL
- **API**: Controllers REST e DTOs

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.8+

## Configuração

O sistema utiliza variáveis de ambiente para configuração:

- `DB_URL`: URL de conexão JDBC (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta do servidor (padrão: `7070`)

## Build

```bash
mvn package
```

## Execução

```bash
java -jar target/reservas-1.0.0.jar
```

Ou com variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=8080 \
java -jar target/reservas-1.0.0.jar
```

## Testes

Os testes de integração utilizam Testcontainers e requerem Docker:

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria nova reserva
- `GET /reservations/{id}` - Consulta reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Obtém comprovante (2ª via)

### Mensagens

- `GET /messages` - Lista mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro (CSV)

## Exemplo de Uso

### Criar uma reserva

```bash
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
```

### Confirmar reserva (envia notificações)

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

Isso dispara automaticamente três notificações:
- Email ao hóspede com comprovante
- Email à governança com datas e número de hóspedes
- Email ao financeiro com valor pago

### Ver mensagens enviadas

```bash
curl http://localhost:7070/messages
```

## Sistema de Notificações

O sistema utiliza o padrão Observer para notificações desacopladas:

- **GuestNotificationObserver**: Envia email de confirmação ao hóspede
- **HousekeepingNotificationObserver**: Notifica governança sobre reservas e cancelamentos
- **FinanceNotificationObserver**: Notifica financeiro sobre pagamentos e reembolsos

Para adicionar novos observadores (ex: restaurante, transfer), basta:
1. Criar uma classe que implemente `ReservationObserver`
2. Registrá-la no `NotificationService` em `App.java`

Nenhum código existente precisa ser modificado.

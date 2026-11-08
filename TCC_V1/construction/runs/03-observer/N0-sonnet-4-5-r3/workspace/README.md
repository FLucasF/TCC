# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Funcionalidades

### Gestão de Quartos
- 5 quartos: 101, 102 (STANDARD), 201, 202 (DELUXE), 301 (SUITE)
- Capacidades e preços por tipo de quarto
- Alta temporada (15/dez a fim de fev + julho inteiro) com acréscimo de 20%

### Reservas
- Criação, confirmação, check-in e check-out
- Dois planos de tarifa: STANDARD e NON_REFUNDABLE (15% desconto)
- Validação de disponibilidade e capacidade
- Cálculo automático de preços com arredondamento "meio para o par"

### Sistema de Notificações Extensível
**Implementação baseada em eventos** que permite fácil adição de novos destinatários:

Quando uma reserva é confirmada:
- Hóspede recebe comprovante por e-mail
- Governança é notificada sobre a reserva (quarto, datas, nº de hóspedes)
- Financeiro é avisado do pagamento recebido

Quando uma reserva confirmada é cancelada:
- Governança é notificada das datas liberadas
- Financeiro recebe aviso de reembolso (se aplicável)

Para adicionar novos listeners (ex: restaurante, transfer), basta:
1. Criar uma nova classe implementando `EventListener<ReservationConfirmedEvent>` ou `EventListener<ReservationCancelledEvent>`
2. Registrar no `EventPublisher` no `Main.java`

Não é necessário modificar o código existente - apenas adicionar o novo listener.

### Relatórios
- Ocupação por data (CSV)
- Receita por mês (CSV)

## Executando o Sistema

### Com Docker Compose (Recomendado)

```bash
# Build do projeto
mvn clean package -DskipTests

# Iniciar serviços
docker-compose up -d

# O sistema estará disponível em http://localhost:7070
```

### Manualmente

1. Inicie um PostgreSQL:
```bash
docker run -d \
  -e POSTGRES_DB=pousada \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:18-alpine
```

2. Build e execução:
```bash
mvn clean package -DskipTests

export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070

java -jar target/reservations-1.0.0.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista todas as mensagens enviadas (e-mails)

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

## Exemplos de Uso

### Criar uma reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar reserva (dispara notificações)
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Ver mensagens enviadas
```bash
curl http://localhost:7070/messages
```

## Testes

Os testes de integração usam Testcontainers e requerem Docker:

```bash
mvn verify
```

## Arquitetura

### Padrão de Eventos
O sistema usa um **Event Publisher/Subscriber pattern** para desacoplar a lógica de negócio das notificações:

- `EventPublisher`: gerencia os listeners e publica eventos
- `EventListener<T>`: interface para criar novos listeners
- Eventos: `ReservationConfirmedEvent`, `ReservationCancelledEvent`

### Estrutura de Pacotes
```
com.pousadamarealta/
├── config/         - Configuração do banco de dados
├── domain/         - Entidades e enums
├── event/          - Sistema de eventos
├── notification/   - Listeners de notificação
├── repository/     - Acesso a dados
├── service/        - Lógica de negócio
├── controller/     - Endpoints REST
└── util/           - Utilitários
```

## Tecnologias

- Java 21
- Javalin 6.7.0 (web framework)
- PostgreSQL 18
- HikariCP (connection pool)
- Jackson (JSON)
- JUnit 5 + Testcontainers (testes)
- Maven

## Regras de Negócio

- Alta temporada: 15/dez a 28-29/fev + julho inteiro (+20%)
- Plano NON_REFUNDABLE: 15% desconto, sem reembolso
- Arredondamento: "meio para o par" (RoundingMode.HALF_EVEN)
- Reserva PENDING pode ser cancelada sem avisos
- Reserva CONFIRMED cancelada gera notificações para governança e (se houver reembolso) financeiro
- Comprovante só existe para reservas pagas (CONFIRMED, CHECKED_IN, CHECKED_OUT)

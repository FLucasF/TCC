# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, construído com Java 21, Javalin e PostgreSQL.

## Arquitetura Extensível de Notificações

O sistema foi projetado com uma **arquitetura baseada em eventos** (Event-Driven Architecture) que permite adicionar novos assinantes de notificações facilmente, sem modificar o código existente.

### Como funciona

1. **EventBus**: Um barramento central de eventos que gerencia a publicação e subscrição de eventos
2. **Domain Events**: Eventos como `ReservationConfirmedEvent` e `ReservationCancelledEvent` 
3. **Event Handlers**: Manipuladores específicos para cada tipo de notificação:
   - `GuestNotificationHandler`: Envia comprovante ao hóspede
   - `HousekeepingNotificationHandler`: Notifica governança sobre reservas
   - `FinanceNotificationHandler`: Notifica financeiro sobre pagamentos/reembolsos

### Adicionando novos assinantes

Para adicionar um novo assinante (ex: restaurante, transfer), basta:

1. Criar um novo handler implementando `EventHandler<ReservationConfirmedEvent>`
2. Registrar o handler no EventBus durante a inicialização

Exemplo:

```java
// Criar o handler
public class RestaurantNotificationHandler implements EventHandler<ReservationConfirmedEvent> {
    @Override
    public void handle(ReservationConfirmedEvent event) {
        Reservation r = event.reservation();
        // Enviar email ao restaurante...
    }
}

// Registrar no Main.java
RestaurantNotificationHandler restaurantHandler = new RestaurantNotificationHandler(messageService);
eventBus.subscribe(ReservationConfirmedEvent.class, restaurantHandler);
```

## Funcionalidades

### Quartos
- 101, 102: STANDARD (2 pessoas) - R$ 249,90/noite
- 201, 202: DELUXE (3 pessoas) - R$ 389,90/noite
- 301: SUITE (4 pessoas) - R$ 579,90/noite

### Alta Temporada (+20%)
- 15/12 a 28/02
- Todo mês de julho

### Planos de Tarifa
- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Fluxo de Reserva
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓
CANCELLED (apenas antes do check-in)
```

### Notificações Automáticas

**Quando confirmar uma reserva:**
- Hóspede: Recebe comprovante por email
- Governança: Recebe aviso sobre quarto, datas e nº de hóspedes
- Financeiro: Recebe aviso sobre pagamento recebido

**Quando cancelar uma reserva confirmada:**
- Governança: Recebe aviso sobre quarto liberado
- Financeiro: Recebe aviso sobre reembolso (se aplicável)

## Requisitos

- Java 21+
- PostgreSQL 18+ (para produção)
- Maven 3.6+

## Configuração

Configure as seguintes variáveis de ambiente:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
export PORT=7070  # Opcional, padrão é 7070
```

## Executando

### Build
```bash
mvn package
```

### Executar
```bash
java -jar target/reservas-1.0.0.jar
```

### Testes
```bash
mvn verify
```

## API REST

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - Relatório financeiro (CSV)

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

### Confirmar reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Verificar mensagens enviadas
```bash
curl http://localhost:7070/messages
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── Main.java                           # Inicialização da aplicação
├── api/                                # Controllers REST
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   ├── ReportController.java
│   └── dto/                            # Data Transfer Objects
├── config/
│   └── Database.java                   # Configuração do banco
├── domain/
│   ├── event/                          # Sistema de eventos
│   │   ├── EventBus.java              # Barramento de eventos
│   │   ├── DomainEvent.java
│   │   ├── ReservationConfirmedEvent.java
│   │   ├── ReservationCancelledEvent.java
│   │   └── handlers/                   # Handlers de notificação
│   │       ├── GuestNotificationHandler.java
│   │       ├── HousekeepingNotificationHandler.java
│   │       └── FinanceNotificationHandler.java
│   ├── model/                          # Modelos de domínio
│   ├── service/                        # Serviços de negócio
│   └── util/                           # Utilitários (formatação)
└── repository/                         # Acesso a dados
```

## Detalhes Técnicos

### Arredondamento
Todos os valores monetários usam `BigDecimal` com arredondamento `HALF_EVEN` (banqueiro), conforme especificação.

### Banco de Dados
O sistema cria automaticamente as tabelas necessárias ao iniciar:
- `rooms`: Catálogo de quartos
- `reservations`: Reservas
- `messages`: Log de mensagens enviadas

### Validações
- Data de saída deve ser posterior à entrada
- Mínimo 1 hóspede
- Capacidade do quarto respeitada
- Sem conflitos de reserva no mesmo quarto/período
- Transições de status validadas

### Códigos de Erro
- `400 INVALID_REQUEST`: Dados inválidos
- `404 ROOM_NOT_FOUND`: Quarto não existe
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 ROOM_UNAVAILABLE`: Quarto já reservado
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no status atual
- `422 CAPACITY_EXCEEDED`: Mais hóspedes que a capacidade do quarto

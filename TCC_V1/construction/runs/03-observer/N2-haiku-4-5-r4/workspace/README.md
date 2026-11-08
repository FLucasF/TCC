# Sistema de Reservas - Pousada Maré Alta

Um sistema de gerenciamento de reservas para uma pousada pequena no litoral, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Compilação

```bash
mvn clean package
```

O JAR será criado em `target/reserva-system.jar`.

## Execução

### Requisitos
- PostgreSQL 18+
- Java 21+
- Variáveis de ambiente:
  - `DB_URL`: URL do banco de dados (ex: `jdbc:postgresql://localhost:5432/pousada`)
  - `DB_USER`: Usuário do PostgreSQL
  - `DB_PASSWORD`: Senha do PostgreSQL
  - `PORT`: Porta da API (padrão: 7070)

### Exemplo
```bash
DB_URL="jdbc:postgresql://localhost:5432/pousada" \
DB_USER="postgres" \
DB_PASSWORD="senha" \
PORT=7070 \
java -jar target/reserva-system.jar
```

## API Endpoints

### Quartos
- **GET /rooms** - Lista todos os quartos

### Reservas
- **POST /reservations** - Criar nova reserva
- **GET /reservations/{id}** - Obter detalhes de uma reserva
- **POST /reservations/{id}/confirmation** - Confirmar reserva (pagamento)
- **POST /reservations/{id}/cancellation** - Cancelar reserva
- **POST /reservations/{id}/check-in** - Check-in
- **POST /reservations/{id}/check-out** - Check-out e receber comprovante
- **GET /reservations/{id}/document** - Segunda via do comprovante

### Mensagens
- **GET /messages** - Lista de e-mails enviados (notificações internas)

### Relatórios
- **GET /reports/occupancy?date=2027-07-10** - Ocupação de quartos em uma data (CSV)
- **GET /reports/revenue?month=2027-07** - Receita de um mês (CSV)

## Fluxo de Reserva

1. **Criação**: `POST /reservations` - Reserva nasce em status PENDING
2. **Confirmação**: `POST /reservations/{id}/confirmation` - Reserva vai para CONFIRMED após pagamento
3. **Check-in**: `POST /reservations/{id}/check-in` - Status CHECKED_IN
4. **Check-out**: `POST /reservations/{id}/check-out` - Status CHECKED_OUT e recebe comprovante
5. **Cancelamento**: `POST /reservations/{id}/cancellation` - Possível apenas antes do check-in

## Sistema de Notificações

Quando uma reserva é confirmada:
- **Hóspede** recebe e-mail com comprovante da reserva
- **Governança** (governanca@pousadamarealta.com.br) recebe notificação com datas e número de hóspedes
- **Financeiro** (financeiro@pousadamarealta.com.br) recebe notificação com valor pago

Quando uma reserva confirmada é cancelada:
- **Governança** recebe notificação com datas que ficaram livres
- **Financeiro** recebe notificação de reembolso (se aplicável)

## Preços

### Quartos e Diárias Base
- **101, 102** (STANDARD, 2 pessoas): R$ 249,90
- **201, 202** (DELUXE, 3 pessoas): R$ 389,90
- **301** (SUITE, 4 pessoas): R$ 579,90

### Temporadas
- **Alta temporada**: 15/dez - 28/fev e mês de julho (+20%)
- **Baixa temporada**: Demais períodos

### Planos de Tarifa
- **STANDARD**: Preço cheio, reembolso total em cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em cancelamento

## Testes

```bash
mvn test
```

## Estrutura do Projeto

```
src/main/java/com/pousadamarealta/
├── Main.java                  # Ponto de entrada
├── domain/                    # Modelos de domínio
│   ├── Room.java
│   ├── Reservation.java
│   ├── RoomType.java
│   ├── ReservationStatus.java
│   ├── RatePlan.java
│   ├── Message.java
│   ├── MessageChannel.java
│   └── Document.java
├── services/                  # Lógica de negócio
│   ├── ReservationService.java
│   ├── MessageService.java
│   ├── DocumentService.java
│   ├── ReportService.java
│   └── Database.java
├── events/                    # Sistema de eventos (Observer Pattern)
│   ├── ReservationEventListener.java
│   └── ReservationEventDispatcher.java
├── listeners/                 # Implementações de listeners
│   ├── GuestNotificationListener.java
│   ├── GovernanceNotificationListener.java
│   └── FinanceNotificationListener.java
├── api/                       # Controladores REST
│   ├── RoomsController.java
│   ├── ReservationsController.java
│   ├── MessagesController.java
│   └── ReportsController.java
└── utils/                     # Utilitários
    ├── Formatting.java
    └── PricingCalculator.java
```

## Notas de Implementação

- O sistema usa **Observer Pattern** para notificações, permitindo adicionar novos interessados (restaurante, transfer) sem modificar o código existente.
- Os arredondamentos monetários usam o método "meio para o par" (HALF_EVEN).
- As mensagens (e-mails) são armazenadas em um banco de dados relacional para auditoria.
- O banco de dados é inicializado automaticamente na primeira execução.

# Estrutura do Projeto

```
/workspace
├── pom.xml                                    # Configuração Maven
├── README.md                                  # Instruções de uso
├── TEST_GUIDE.md                             # Guia de testes com exemplos curl
├── IMPLEMENTATION_SUMMARY.md                 # Resumo da implementação
├── PROJECT_STRUCTURE.md                      # Este arquivo
│
├── src/
│   ├── main/java/com/pousadamarealta/
│   │   ├── Main.java                         # Ponto de entrada da aplicação
│   │   │
│   │   ├── model/
│   │   │   ├── Room.java                     # Modelo de quarto
│   │   │   ├── Reservation.java              # Modelo de reserva
│   │   │   ├── Message.java                  # Modelo de mensagem
│   │   │   ├── RatePlan.java                 # Enum: STANDARD, NON_REFUNDABLE
│   │   │   └── ReservationStatus.java        # Enum: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
│   │   │
│   │   ├── dto/
│   │   │   ├── CreateReservationRequest.java # DTO para POST /reservations
│   │   │   ├── ReservationResponse.java      # Resposta de reserva
│   │   │   ├── DocumentResponse.java         # Comprovante
│   │   │   ├── MessageResponse.java          # Resposta de mensagem
│   │   │   └── ErrorResponse.java            # Resposta de erro
│   │   │
│   │   ├── database/
│   │   │   └── Database.java                 # Inicialização do banco e criação de schema
│   │   │
│   │   ├── repository/
│   │   │   ├── ReservationRepository.java    # Acesso a reservas
│   │   │   ├── RoomRepository.java           # Acesso a quartos
│   │   │   └── MessageRepository.java        # Acesso a mensagens
│   │   │
│   │   ├── service/
│   │   │   ├── ReservationService.java       # Lógica de negócio de reservas
│   │   │   └── ReportService.java            # Geração de relatórios CSV
│   │   │
│   │   ├── notification/
│   │   │   ├── ReservationNotifier.java      # Interface para notificadores
│   │   │   ├── NotificationService.java      # Gerenciador de notificadores
│   │   │   ├── GovernanceNotifier.java       # Notificador da governança
│   │   │   └── FinanceNotifier.java          # Notificador do financeiro
│   │   │
│   │   ├── route/
│   │   │   ├── RoomRoutes.java               # Endpoints de quartos
│   │   │   ├── ReservationRoutes.java        # Endpoints de reservas
│   │   │   ├── MessageRoutes.java            # Endpoints de mensagens
│   │   │   └── ReportRoutes.java             # Endpoints de relatórios
│   │   │
│   │   └── util/
│   │       ├── DateUtils.java                # Utilitários de data (sazonalidade, formatação)
│   │       └── MoneyUtils.java               # Utilitários financeiros (arredondamento, cálculos)
│   │
│   └── test/java/com/pousadamarealta/
│       └── util/
│           ├── DateUtilsTest.java            # Testes de datas e sazonalidade
│           └── MoneyUtilsTest.java           # Testes de arredondamento
│
├── target/
│   └── reservations-system.jar                # JAR executável
│
└── dependency-reduced-pom.xml                # Gerado automaticamente
```

## Fluxos Principais

### 1. Criar Reserva
```
POST /reservations
  → ReservationRoutes.register()
  → ReservationService.createReservation()
  → Validações (quarto existe, disponível, capacidade)
  → Cálculos (noites, sazonalidade, desconto, arredondamento)
  → ReservationRepository.save()
```

### 2. Confirmar Reserva
```
POST /reservations/{id}/confirmation
  → ReservationService.confirmReservation()
  → Validação (status = PENDING)
  → ReservationRepository.update()
  → Envio de e-mail para hóspede
  → NotificationService.notifyConfirmed()
    ├── GovernanceNotifier.onReservationConfirmed()
    │   └── MessageRepository.save() (governança)
    └── FinanceNotifier.onReservationConfirmed()
        └── MessageRepository.save() (financeiro)
```

### 3. Cancelar Reserva
```
POST /reservations/{id}/cancellation
  → ReservationService.cancelReservation()
  → Validação (status = PENDING ou CONFIRMED)
  → Cálculo de reembolso (baseado em ratePlan)
  → ReservationRepository.update()
  → Se foi confirmada: NotificationService.notifyCancelled()
    ├── GovernanceNotifier.onReservationCancelled()
    │   └── MessageRepository.save() (se tinha pagamento)
    └── FinanceNotifier.onReservationCancelled()
        └── MessageRepository.save() (se houver reembolso)
```

### 4. Gerar Relatório
```
GET /reports/occupancy?date=2027-07-10
  → ReportRoutes.register()
  → ReportService.generateOccupancyReport()
  → Busca reservas por data (check_in <= date < check_out)
  → Gera CSV com quartos ocupados
```

## Banco de Dados

### Tabelas

#### rooms
```sql
number INTEGER PRIMARY KEY
type VARCHAR(20)
capacity INTEGER
nightly_rate NUMERIC(10,2)
```

#### reservations
```sql
id BIGSERIAL PRIMARY KEY
room_number INTEGER (FK)
guest_name VARCHAR(255)
guest_email VARCHAR(255)
guest_cpf VARCHAR(14)
check_in DATE
check_out DATE
guests INTEGER
rate_plan VARCHAR(20)
nights INTEGER
subtotal NUMERIC(10,2)
discount NUMERIC(10,2)
total NUMERIC(10,2)
status VARCHAR(20)
amount_paid NUMERIC(10,2)
refund NUMERIC(10,2)
```

#### messages
```sql
id BIGSERIAL PRIMARY KEY
channel VARCHAR(50)
to_address VARCHAR(255)
subject VARCHAR(255)
body TEXT
```

### Índices
- `reservations(room_number, check_in, check_out)` - para buscar conflitos
- `reservations(check_in)` - para relatórios e ocupação

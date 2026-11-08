# Arquitetura - Pousada Maré Alta

## Visão Geral

Sistema modular e extensível de reservas para pousada, seguindo princípios SOLID:
- **S**ingle Responsibility: Cada classe tem uma responsabilidade
- **O**pen/Closed: Aberto para extensão (novos notificadores), fechado para modificação
- **L**iskov Substitution: Notificadores podem ser intercambiáveis
- **I**nterface Segregation: Interfaces mínimas e focadas
- **D**ependency Inversion: Dependência em abstrações

## Estrutura de Camadas

```
┌─────────────────────────────────────┐
│        API REST (App.java)          │ ← Endpoints HTTP
├─────────────────────────────────────┤
│  Service Layer                      │
│  ├─ ReservationService              │ ← Lógica de reserva
│  ├─ ReportService                   │ ← Relatórios
│  └─ NotificationManager             │ ← Orquestração de eventos
├─────────────────────────────────────┤
│  Repository Layer                   │
│  ├─ RoomRepository                  │ ← Acesso a quartos
│  ├─ ReservationRepository           │ ← Acesso a reservas
│  └─ MessageRepository               │ ← Acesso a mensagens
├─────────────────────────────────────┤
│  Database Layer                     │
│  └─ Database (HikariCP)             │ ← Pool de conexões
├─────────────────────────────────────┤
│  PostgreSQL                         │ ← Persistência
└─────────────────────────────────────┘
```

## Fluxo de Confirmação de Reserva

```
POST /reservations/{id}/confirmation
          ↓
ReservationService.confirmReservation()
          ↓
┌─────────────────────────────────────┐
│ 1. Valida status PENDING             │
│ 2. Atualiza DB (CONFIRMED)           │
│ 3. Carrega reserva atualizada        │
│ 4. Dispara evento CONFIRMED          │
└─────────────────────────────────────┘
          ↓
NotificationManager.notify(event)
          ↓
    ┌─────────────────────┐
    │ Para cada notificador│
    └─────────────────────┘
        ↓       ↓       ↓
      Guest  Housing Finance
     Notifier Notifier Notifier
        ↓       ↓       ↓
   Email    Email    Email
    ↓        ↓       ↓
  Guest   Governance Financeiro
 
    Todas inscritas em MessageRepository
```

## Padrões de Design

### Observer (Notificações)

```java
ReservationNotifier[] notifiers = {
    new GuestNotifier(),
    new HousekeepingNotifier(),
    new FinanceNotifier()
};

for (ReservationNotifier n : notifiers) {
    n.handleEvent(event);
}
```

**Benefício**: Adicionar novos departamentos sem mexer no código existente.

### Repository (Acesso a Dados)

```java
ReservationRepository.create(...)
ReservationRepository.findById(...)
ReservationRepository.updateStatus(...)
```

**Benefício**: Isola lógica de banco de dados, facilita testes e mudança de BD.

### Value Object (Enums)

```java
ReservationStatus.PENDING
ReservationStatus.CONFIRMED
RatePlan.STANDARD
RatePlan.NON_REFUNDABLE
```

**Benefício**: Type-safe, evita strings mágicas, refactoring automático.

## Fluxo de Dados - Criar Reserva

```
Cliente HTTP
    ↓
POST /reservations (JSON)
    ↓
App.createReservation()
    ↓
ReservationService.createReservation()
    ├─ Valida entrada (roomNumber, dates, guests)
    ├─ Busca quarto em RoomRepository
    ├─ Verifica capacidade (guests <= capacity)
    ├─ Verifica conflitos em ReservationRepository
    ├─ Calcula preço em PricingCalculator
    │  ├─ Verifica alta temporada
    │  ├─ Calcula tarifa por noite
    │  ├─ Soma noites
    │  ├─ Aplica desconto se NON_REFUNDABLE
    │  └─ Arredonda cada passo (meio para par)
    └─ Insere em ReservationRepository
        ↓
    Retorna Reservation (PENDING, amountPaid=0)
        ↓
ReservationResponse (serializa para JSON)
        ↓
Cliente HTTP (201 Created)
```

## Cálculo de Preços - Detalhado

```
CheckIn: 10/07/2027
CheckOut: 12/07/2027
Quarto: 201 (DELUXE, R$ 389,90)
RatePlan: STANDARD
Hóspedes: 2

1. Noites entre CheckIn e CheckOut
   └─ 12 - 10 = 2 noites

2. Para cada noite:
   a) 10/07: alta temporada? SIM
      └─ R$ 389,90 × 1,20 = R$ 467,88
   b) 11/07: alta temporada? SIM
      └─ R$ 389,90 × 1,20 = R$ 467,88

3. Subtotal (soma das noites)
   └─ R$ 467,88 + R$ 467,88 = R$ 935,76

4. Desconto (RatePlan)
   a) Se STANDARD: R$ 0,00
   b) Se NON_REFUNDABLE: R$ 935,76 × 0,15 = R$ 140,36

5. Total
   └─ R$ 935,76 - R$ 140,36 = R$ 795,40
```

## Estrutura de Dados - Tabelas

### rooms
```sql
CREATE TABLE rooms (
    number INT PRIMARY KEY,
    type VARCHAR(20),           -- STANDARD, DELUXE, SUITE
    capacity INT,               -- 2, 3, 4
    nightly_rate NUMERIC(10,2)  -- 249.90, 389.90, 579.90
);
```

### reservations
```sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    room_number INT,
    guest_name VARCHAR(255),
    guest_email VARCHAR(255),
    guest_cpf VARCHAR(20),
    check_in DATE,
    check_out DATE,
    guests INT,
    rate_plan VARCHAR(20),      -- STANDARD, NON_REFUNDABLE
    nights INT,
    subtotal NUMERIC(10,2),
    discount NUMERIC(10,2),
    total NUMERIC(10,2),
    status VARCHAR(20),         -- PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
    amount_paid NUMERIC(10,2),
    refund NUMERIC(10,2)        -- NULL até cancelamento
);
```

### messages
```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    channel VARCHAR(20),        -- EMAIL
    "to" VARCHAR(255),          -- email do destinatário
    subject VARCHAR(255),
    body TEXT
);
```

## Caminho para Adicionar Funcionalidade

### Novo Quarto
1. Atualizar enum `RoomType`
2. Inserir novo registro em `rooms` table
3. Pronto!

### Novo Status de Reserva
1. Atualizar enum `ReservationStatus`
2. Adicionar método em `ReservationService`
3. Adicionar endpoint em `App.java`
4. Atualizar estado de transição (qual status pode ir para qual)

### Novo Departamento Notificado
1. Criar `NotificadorXxx implements ReservationNotifier`
2. Registrar em `NotificationManager` (em `App.initializeNotifiers()`)
3. **Nenhuma outra mudança necessária**

### Novo Campo de Reserva
1. Adicionar coluna em tabela `reservations`
2. Adicionar campo em classe `Reservation`
3. Adicionar em `ReservationRepository.mapRow()`
4. Pronto!

### Novo Tipo de Relatório
1. Adicionar método em `ReportService`
2. Adicionar endpoint em `App.java`
3. Formatar saída em CSV conforme necessário

## Validações

### Ao Criar Reserva
- [ ] Quarto existe? → ROOM_NOT_FOUND
- [ ] Data válida (checkout > checkin)? → INVALID_REQUEST
- [ ] Hóspedes > 0? → INVALID_REQUEST
- [ ] Hóspedes <= capacidade? → CAPACITY_EXCEEDED
- [ ] Quarto disponível? → ROOM_UNAVAILABLE

### Ao Confirmar
- [ ] Reserva existe? → RESERVATION_NOT_FOUND
- [ ] Status é PENDING? → OPERATION_NOT_ALLOWED

### Ao Cancelar
- [ ] Reserva existe? → RESERVATION_NOT_FOUND
- [ ] Status é PENDING ou CONFIRMED? → OPERATION_NOT_ALLOWED

### Ao Check-in
- [ ] Reserva existe? → RESERVATION_NOT_FOUND
- [ ] Status é CONFIRMED? → OPERATION_NOT_ALLOWED

### Ao Check-out
- [ ] Reserva existe? → RESERVATION_NOT_FOUND
- [ ] Status é CHECKED_IN? → OPERATION_NOT_ALLOWED

## Segurança

### Pontos de Entrada (Validação Obrigatória)
- URLs: Validar formato (datas como YYYY-MM-DD)
- JSON: Validar campos obrigatórios
- Valores numéricos: Validar range

### Proteção de SQL Injection
- Usar `PreparedStatement` para todas as queries ✓
- Nunca concatenar SQL dinamicamente ✓

### Proteção de Buffer Overflow
- Java gerencia memória automaticamente ✓
- BigDecimal para cálculos monetários (sem float/double) ✓

### Logs
- Implementar logs estruturados (SLF4J) ✓
- Evitar logging de dados sensíveis (CPF, email)

## Extensibilidade Futura

### Fácil de Implementar
- ✓ Novo quarto: Só inserir em DB
- ✓ Novo departamento: Novo notificador
- ✓ Novo relatório: Novo método + endpoint
- ✓ Novo status: Novo enum + método + endpoint

### Requer Refactoring
- [ ] Múltiplas pousadas (adicionar tenant_id)
- [ ] Preços dinâmicos (adicionar tabela de pricing)
- [ ] Autenticação (adicionar JWT)
- [ ] Histórico detalhado (audit log)

## Performance

### Índices Recomendados
```sql
CREATE INDEX idx_reservations_room_dates 
  ON reservations(room_number, check_in, check_out);
CREATE INDEX idx_reservations_status 
  ON reservations(status);
CREATE INDEX idx_messages_channel 
  ON messages(channel);
```

### Pool de Conexões
- HikariCP com 10 conexões (configurável)
- Timeout padrão respeitado
- Conexões recicladas automaticamente

## Diagrama de Classes

```
Enum
├─ RoomType
├─ ReservationStatus
├─ RatePlan
└─ MessageChannel

Model
├─ Room
├─ Reservation
├─ Message
└─ Document

Service
├─ ReservationService (statics)
├─ ReportService (statics)
└─ NotificationManager (statics)

Repository
├─ RoomRepository (statics)
├─ ReservationRepository (statics)
└─ MessageRepository (statics)

Notification
├─ ReservationEvent
├─ ReservationNotifier (interface)
└─ notifiers/
    ├─ GuestNotifier
    ├─ HousekeepingNotifier
    └─ FinanceNotifier

API
├─ ApiError
├─ request/
│  └─ CreateReservationRequest
└─ response/
    ├─ ReservationResponse
    ├─ DocumentResponse
    └─ MessageResponse

Util
├─ PricingCalculator
├─ Formatter
└─ DocumentGenerator
```

## Princípio: Adicione Sem Modificar

Cada novo caso entra num lugar só:
- Novo quarto? Tabela `rooms`
- Novo departamento? Nova classe `XxxNotifier`
- Novo relatório? Novo método em `ReportService`
- Novo status? Novo enum + método

Nunca modifique lógica existente para adicionar casos novos.

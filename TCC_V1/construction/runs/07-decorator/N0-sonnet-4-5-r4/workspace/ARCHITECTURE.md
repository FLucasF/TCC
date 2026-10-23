# Arquitetura do Sistema

## Visão Geral

O sistema foi construído seguindo uma arquitetura em camadas com separação clara de responsabilidades:

```
┌─────────────────────────────────────────────┐
│           API REST (Javalin)                │
│  Controllers: Room, Reservation, Message,   │
│              Report                         │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Camada de Serviços                  │
│  RoomService, ReservationService,           │
│  PricingService, ReportService,             │
│  MessageService                             │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│      Camada de Persistência (DAOs)          │
│  RoomDAO, ReservationDAO, MessageDAO        │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         PostgreSQL Database                 │
└─────────────────────────────────────────────┘
```

## Estrutura de Pacotes

```
com.pousadamarealta/
├── api/
│   ├── controller/     # Controllers REST
│   └── dto/            # Data Transfer Objects
├── database/           # DAOs e gerenciamento do banco
├── domain/             # Entidades e enums de domínio
├── exception/          # Exceções customizadas
├── messaging/          # Sistema de mensagens com regras
└── service/            # Lógica de negócio
```

## Componentes Principais

### 1. Domain Layer

**Entidades:**
- `Room`: Representa um quarto com número, tipo, capacidade e diária base
- `Reservation`: Reserva completa com todas as informações e cálculos
- `Message`: Mensagem enviada pelo sistema

**Enums:**
- `RoomType`: STANDARD, DELUXE, SUITE (com capacidades e diárias)
- `ReservationStatus`: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT, CANCELLED
- `RatePlan`: STANDARD (sem desconto), NON_REFUNDABLE (15% desconto)
- `MessageChannel`: EMAIL
- `MessageStatus`: SENT, BLOCKED

### 2. Pricing Service

Implementa a lógica complexa de cálculo de preços:

- **Alta Temporada**: 15/12 a fim de fevereiro + julho completo → +20% na diária
- **Arredondamento**: HALF_EVEN (meio para o par) em cada etapa
- **Cálculo**:
  1. Calcular diária de cada noite (base ou alta temporada)
  2. Somar todas as noites → subtotal
  3. Aplicar desconto do plano (0% ou 15%)
  4. Calcular total (subtotal - desconto)

```java
// Exemplo: STANDARD em julho
baseRate = 249.90
highSeasonRate = 249.90 * 1.20 = 299.88 (HALF_EVEN)
2 noites = 299.88 + 299.88 = 599.76
desconto NON_REFUNDABLE = 599.76 * 0.15 = 89.96 (HALF_EVEN)
total = 599.76 - 89.96 = 509.80
```

### 3. Messaging System

Arquitetura extensível usando **Chain of Responsibility**:

```
Mensagem Original
      ↓
EmailValidationRule → define status (SENT ou BLOCKED)
      ↓
[Modo Normal]              [Modo Teste]
ArchiveCopyRule            TestModeRule
(gera cópia)               (redireciona)
      ↓
Mensagens são salvas no banco
```

**Fluxo de uma mensagem:**

1. Criação da mensagem (channel, to, subject, body)
2. Aplicação sequencial das regras:
   - Validação: marca como SENT ou BLOCKED
   - Arquivo/Teste: cria cópia ou redireciona
3. Persistência de todas as mensagens resultantes

### 4. Reservation Service

Gerencia todo o ciclo de vida das reservas:

**Criação:**
- Valida quarto, capacidade, datas
- Verifica disponibilidade (conflitos)
- Calcula preços usando PricingService
- Cria reserva no estado PENDING

**Transições de Estado:**
- `confirm()`: PENDING → CONFIRMED
  - Define amountPaid = total
  - Envia e-mail de confirmação via MessageService
- `checkIn()`: CONFIRMED → CHECKED_IN
- `checkOut()`: CHECKED_IN → CHECKED_OUT
  - Retorna comprovante
- `cancel()`: PENDING/CONFIRMED → CANCELLED
  - Calcula refund (total se STANDARD confirmada, zero se NON_REFUNDABLE)

**Validações:**
- Capacidade do quarto vs. número de hóspedes
- Disponibilidade nas datas (query SQL verifica overlapping)
- Estados permitidos para cada operação

### 5. Report Service

Gera relatórios em formato CSV:

**Ocupação (por data):**
```sql
SELECT rooms com reservas pagas (CONFIRMED, CHECKED_IN, CHECKED_OUT)
WHERE check_in <= date AND check_out > date
```

**Financeiro (por mês):**
```sql
SELECT reservas WHERE EXTRACT(MONTH FROM check_in) = month
E que geraram receita (amountPaid - refund > 0)
```

### 6. Database Layer

**Inicialização automática:**
- Cria tabelas se não existirem
- Popula os 5 quartos fixos (101, 102, 201, 202, 301)
- Usa PostgreSQL 18

**Tabelas:**

```sql
rooms (number PK, type, capacity, nightly_rate)
reservations (id PK, room_number FK, guest_*, dates, pricing, status)
messages (id PK, channel, recipient, subject, body, status)
```

## Tratamento de Erros

HTTP Status codes seguem REST:

- `400 INVALID_REQUEST`: Campos faltando, JSON inválido, formato de data errado
- `404 ROOM_NOT_FOUND`: Quarto não existe
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 ROOM_UNAVAILABLE`: Quarto ocupado nas datas
- `409 OPERATION_NOT_ALLOWED`: Transição de estado inválida
- `422 CAPACITY_EXCEEDED`: Mais hóspedes que a capacidade

## Decisões de Design

### 1. Chain of Responsibility para Mensagens
- **Por quê?** Permite adicionar/remover regras facilmente
- **Trade-off:** Mais complexo que if/else, mas muito mais extensível
- **Futuro:** Fácil adicionar rate limiting, horários, etc.

### 2. BigDecimal para Dinheiro
- **Por quê?** Precisão exata para cálculos financeiros
- **Trade-off:** Mais verboso que double, mas evita erros de arredondamento
- **HALF_EVEN:** Arredondamento bancário padrão (0.5 vai para o par)

### 3. Enums para Tipos Fixos
- **Por quê?** Type safety, impossível ter tipos inválidos
- **Trade-off:** Mudanças requerem recompilação, mas os tipos são estáveis

### 4. Quartos Fixos vs. Dinâmicos
- **Decisão:** Fixos no código, pré-populados no banco
- **Motivo:** "Os quartos quase nunca mudam" (requisito)
- **Trade-off:** Mais simples, mas adicionar quarto requer deploy

### 5. Estado Imutável das Reservas
- **Decisão:** Só status, amountPaid e refund mudam
- **Motivo:** Integridade dos dados (preços não mudam após criação)
- **Benefício:** Auditoria clara, cálculos consistentes

## Extensões Futuras

O sistema foi projetado para facilitar:

1. **Novas regras de mensagem**: Implementar MessageRule
2. **Novos tipos de quarto**: Adicionar ao enum e popular banco
3. **Novos planos de tarifa**: Adicionar ao enum com taxa
4. **Períodos de alta temporada**: Método `isHighSeason()` centralizado
5. **Relatórios**: ReportService facilmente extensível
6. **Canais de mensagem**: Enum suporta SMS, WhatsApp, etc.

## Testes

**Testes Unitários:**
- `PricingServiceTest`: Valida cálculos de preço e alta temporada
- `EmailValidationRuleTest`: Valida regra de e-mail
- `ArchiveCopyRuleTest`: Valida geração de cópia
- `TestModeRuleTest`: Valida redirecionamento em modo teste

**Cobertura:**
- Lógica de pricing completa
- Sistema de mensagens completo
- Todas as regras de negócio críticas

## Performance

**Otimizações:**
- Queries SQL com índices (PK, FK)
- Connection pool do PostgreSQL
- Cálculos de preço em memória (sem queries desnecessárias)

**Considerações:**
- Para alta carga: adicionar cache de quartos
- Para escala: connection pool configurável
- Para auditoria: adicionar tabela de eventos

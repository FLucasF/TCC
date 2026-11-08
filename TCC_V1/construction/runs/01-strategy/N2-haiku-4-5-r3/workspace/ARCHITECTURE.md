# Arquitetura do Sistema

## Visão Geral

O sistema de reservas da Pousada Maré Alta é construído em Java 21 com o framework Javalin 6.7.0 e PostgreSQL 18, usando o padrão Strategy para gerenciar diferentes planos de tarifa.

## Padrões de Design

### Strategy Pattern
O padrão Strategy foi implementado para os planos de tarifa. Cada plano implementa a interface `RatePlan` com os seguintes métodos:

- `validate()`: Valida se o plano pode ser usado com os parâmetros fornecidos
- `calculate()`: Calcula o preço (subtotal, desconto e total)
- `calculateRefund()`: Calcula o reembolso em caso de cancelamento

**Implementações:**
1. `StandardRatePlan`: Sem desconto, reembolso integral
2. `NonRefundableRatePlan`: 15% de desconto, sem reembolso
3. `CorporateRatePlan`: Desconto por empresa, reembolso integral
4. `WeekendPackageRatePlan`: Preço fixo (sexta-domingo), 50% de reembolso
5. `LongStayRatePlan`: 7+ noites, 10% de desconto, reembolso integral

### Factory Pattern
O `RatePlanFactory` cria instâncias dos planos de tarifa baseado no enum `RatePlanType`.

### DAO Pattern
Classes especializadas (`RoomDao`, `ReservationDao`, `MessageDao`) gerenciam o acesso aos dados no PostgreSQL.

## Estrutura de Pacotes

```
com.pousada
├── Application.java               # Classe principal
├── api/
│   ├── RoomHandler.java          # GET /rooms
│   ├── ReservationHandler.java   # CRUD de reservas
│   ├── MessageHandler.java       # GET /messages
│   └── ReportHandler.java        # Relatórios em CSV
├── database/
│   ├── Database.java             # Inicialização e pool de conexões
│   ├── RoomDao.java              # Quartos
│   ├── ReservationDao.java       # Reservas
│   └── MessageDao.java           # Mensagens
├── model/
│   ├── Room.java
│   ├── Reservation.java
│   ├── Message.java
│   ├── RatePlanType.java         # STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE, LONG_STAY
│   └── ReservationStatus.java    # PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
├── rateplan/
│   ├── RatePlan.java             # Interface
│   ├── RatePlanFactory.java      # Factory
│   ├── RatePlanException.java
│   ├── StandardRatePlan.java
│   ├── NonRefundableRatePlan.java
│   ├── CorporateRatePlan.java
│   ├── WeekendPackageRatePlan.java
│   └── LongStayRatePlan.java
├── service/
│   ├── ReservationService.java   # Lógica de negócio
│   └── ApiException.java         # Exceções HTTP
└── util/
    ├── Money.java                # Arredondamento e formatação
    └── Dates.java                # Operações com datas
```

## Fluxo de Dados

### Criação de Reserva
1. `ReservationHandler.create()` recebe a requisição
2. Valida campos obrigatórios
3. `ReservationService.createReservation()`:
   - Valida quarto existe
   - Valida datas
   - Valida capacidade
   - Cria contexto com dados específicos do plano
   - Valida usando o plano escolhido
   - Verifica disponibilidade do quarto
   - Calcula preços usando o plano
   - Insere no banco via `ReservationDao`
4. Retorna reserva criada

### Confirmação de Reserva
1. `ReservationHandler.confirm()` recebe amountPaid
2. `ReservationService.confirmReservation()`:
   - Valida status é PENDING
   - Atualiza status para CONFIRMED
   - Registra amountPaid
   - Gera comprovante
   - Cria e armazena mensagem de email
3. Retorna reserva confirmada

### Cancelamento
1. `ReservationHandler.cancel()` é chamado
2. `ReservationService.cancelReservation()`:
   - Valida status permite cancelamento
   - Obtém plano de tarifa
   - Calcula refund usando o plano
   - Atualiza status para CANCELLED
   - Registra refund
3. Retorna reserva cancelada

## Cálculo de Preços

Todos os cálculos seguem estas regras:
1. Cada noite é calculada individualmente
2. Se em alta temporada, multiplica por 1.20
3. Cada valor intermediário é arredondado com banker's rounding (HALF_EVEN)
4. Desconto é calculado sobre o subtotal
5. Total = subtotal - desconto

### Alta Temporada
- 15 de dezembro até 28 de fevereiro (inclusive)
- Julho inteiro

### Banker's Rounding (HALF_EVEN)
- 1.125 → 1.12 (arredonda para par)
- 1.135 → 1.14 (arredonda para par)

## Banco de Dados

### Tabela: rooms
- `number` (PK): Número do quarto
- `type`: STANDARD, DELUXE, SUITE
- `capacity`: Número máximo de hóspedes
- `nightly_rate`: Diária em R$

### Tabela: reservations
- `id` (PK): Identificador único
- `room_number` (FK): Referência ao quarto
- `guest_name`, `guest_email`, `guest_cpf`: Dados do hóspede
- `check_in`, `check_out`: Datas
- `guests`: Número de hóspedes
- `rate_plan`: Tipo de plano
- `company_code`: Código da empresa (opcional, para CORPORATE)
- `nights`: Número de noites
- `subtotal`, `discount`, `total`: Valores em R$
- `status`: Estado da reserva
- `amount_paid`: O que foi pago
- `refund`: O que foi devolvido (NULL se não cancelada)

### Tabela: messages
- `id` (PK): Identificador único
- `channel`: EMAIL
- `recipient`: Email do destinatário
- `subject`: Assunto
- `body`: Corpo da mensagem

## Relatórios

### Ocupação (CSV)
Mostra quais quartos estão ocupados em um dia específico.
- Inclui reservas pagas que não foram canceladas
- Check-in conta como ocupado, check-out não conta

### Receita (CSV)
Mostra dinheiro que ficou com a pousada em um mês.
- Inclui reservas que começam no mês
- Calcula: amount_paid - refund
- Inclui reservas canceladas se tiverem lucro

## Validações

### Nível de Requisição
- Campos obrigatórios presentes
- Formatos corretos (datas ISO, tipos enumerados)
- Tipos de dados corretos (inteiros para guests, etc)

### Nível de Negócio (RatePlan)
- CORPORATE: empresa válida
- WEEKEND_PACKAGE: entrada sexta, saída domingo, exatamente 2 noites
- LONG_STAY: mínimo 7 noites

### Nível de Reserva
- Quarto existe e tem capacidade suficiente
- Check-out depois de check-in
- Pelo menos 1 hóspede
- Quarto não está reservado nas datas

### Nível de Operação
- Transições de estado permitidas
- Apenas reservas pagas podem ter segunda via de comprovante

## Códigos de Erro HTTP

| Código | Erro | Descrição |
|--------|------|-----------|
| 400 | INVALID_REQUEST | Dados inválidos ou incompletos |
| 404 | ROOM_NOT_FOUND | Quarto não existe |
| 404 | RESERVATION_NOT_FOUND | Reserva não existe |
| 409 | ROOM_UNAVAILABLE | Quarto ocupado nas datas |
| 409 | OPERATION_NOT_ALLOWED | Estado da reserva não permite operação |
| 422 | CAPACITY_EXCEEDED | Mais hóspedes que capacidade |

## Extensibilidade

Para adicionar um novo plano de tarifa:

1. Criar classe que implementa `RatePlan`
2. Adicionar tipo no enum `RatePlanType`
3. Adicionar case no `RatePlanFactory`

Nenhuma outra parte do código precisa mudar.

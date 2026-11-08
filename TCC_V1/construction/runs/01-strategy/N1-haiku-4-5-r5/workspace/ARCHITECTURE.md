# Arquitetura do Sistema de Reservas

## Design Extensível para Planos de Tarifa

O requisito "o comercial vive inventando plano" foi atendido com um design baseado em **Strategy Pattern**:

```
RatePlan (interface)
├── StandardPlan
├── NonRefundablePlan
├── CorporatePlan
├── WeekendPackagePlan
└── LongStayPlan

RatePlanFactory (registry centralizado)
```

Para adicionar um novo plano (ex: "Senior Citizen"):
1. Criar `SeniorCitizenPlan implements RatePlan`
2. Adicionar `register(new SeniorCitizenPlan())` em `RatePlanFactory`
3. Nenhuma outra classe precisa ser modificada

## Estrutura de Diretórios

```
src/main/java/com/pousadamarealta/
├── Main.java                           (Javalin app com endpoints)
├── api/
│   ├── ApiError.java
│   ├── ReservationRequest.java
│   ├── ReservationResponse.java
│   ├── ReceiptResponse.java
│   ├── RoomResponse.java
│   └── MessageResponse.java
├── database/
│   ├── Database.java                   (Inicialização PostgreSQL)
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── models/
│   ├── Room.java
│   ├── Reservation.java
│   └── Message.java
├── rateplans/
│   ├── RatePlan.java                   (interface)
│   ├── RatePlanFactory.java            (registry)
│   ├── StandardPlan.java
│   ├── NonRefundablePlan.java
│   ├── CorporatePlan.java
│   ├── WeekendPackagePlan.java
│   ├── LongStayPlan.java
│   └── SeasonalHelper.java             (helper com temporada)
└── services/
    ├── ReservationService.java         (lógica de reserva)
    ├── ReceiptService.java             (formata comprovante)
    └── ReportService.java              (gera relatórios CSV)

src/test/java/com/pousadamarealta/rateplans/
├── StandardPlanTest.java
├── NonRefundablePlanTest.java
├── CorporatePlanTest.java
├── WeekendPackageTest.java
└── LongStayTest.java
```

## Fluxo de uma Reserva

```
POST /reservations
  ↓
  Validar entrada (campos, guests, datas)
  ↓
  Validar quarto existe
  ↓
  Verificar capacidade
  ↓
  RatePlanFactory.getPlan() → RatePlan
  ↓
  plan.validateAppliesTo() → exceção se inválido
  ↓
  Verificar conflitos de datas
  ↓
  plan.calculatePrice() → subtotal, discount, total
  ↓
  ReservationRepository.create() → id
  ↓
  Response: Reservation (PENDING, amountPaid=0)

POST /reservations/{id}/confirmation
  ↓
  Validar status = PENDING
  ↓
  ReservationRepository.updateConfirmation(total)
  ↓
  Gerar comprovante
  ↓
  MessageRepository.create() → email guardado
  ↓
  Response: Reservation (CONFIRMED, amountPaid=total)

POST /reservations/{id}/cancellation
  ↓
  Validar status = PENDING ou CONFIRMED
  ↓
  plan.getRefundAmount(amountPaid) → refund
  ↓
  ReservationRepository.updateCancellation(refund)
  ↓
  Response: Reservation (CANCELLED, refund=X)
```

## Cálculo de Preços

Cada plano implementa `PriceCalculation calculatePrice(Room, LocalDate, LocalDate)`:

```java
subtotal = Σ(nightlyRate * seasonalMultiplier) para cada noite
           ↓ arredondar HALF_EVEN
discount = subtotal * descountRate
           ↓ arredondar HALF_EVEN
total = subtotal - discount
        ↓ arredondar HALF_EVEN
```

- **StandardPlan**: sem desconto, alta temporada +20%
- **NonRefundablePlan**: 15% desconto
- **CorporatePlan**: desconto por empresa (12%, 18%, 25%)
- **WeekendPackagePlan**: preço fechado (não multiplicado por sazonalidade)
- **LongStayPlan**: 10% desconto, sazonalidade continua valendo

## Validações

### RatePlan.validateAppliesTo()
- **CorporatePlan**: company code obrigatório e válido
- **WeekendPackagePlan**: check-in = sexta, check-out = domingo, exatamente 2 noites
- **LongStayPlan**: mínimo 7 noites
- **Outros**: sem validações específicas

### ReservationService
- Guest count ≥ 1 e ≤ capacidade do quarto
- Check-out > Check-in
- Sem conflitos de datas (quarto não pode ter 2 reservas confirmadas/pagas na mesma noite)

### Estados permitidos
```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
↓
CANCELLED (só de PENDING ou CONFIRMED)
```

## Relatórios

### Occupancy Report
```
room,type,reservation,check_in,check_out
101,STANDARD,1,2027-07-10,2027-07-12
201,DELUXE,2,2027-07-11,2027-07-13
total,2/5
```

Mostra reservas **pagas** que estão ocupando no dia (check_in ≤ data < check_out).

### Revenue Report
```
reservation,room,check_in,check_out,status,amount
1,101,2027-07-10,2027-07-12,CONFIRMED,599.76
2,201,2027-07-11,2027-07-13,CHECKED_OUT,820.00
total,1419.76
```

Mostra reservas que começam no mês e deixaram dinheiro com a pousada:
- amount = amountPaid - refund (se houver)
- Inclui CONFIRMED, CHECKED_IN, CHECKED_OUT
- Inclui CANCELLED se foi paga (NON_REFUNDABLE cancelada continua contando)

## Segurança e Consistência

- Banco de dados criado automaticamente na inicialização
- Constraints em SQL (guests ≥ 1, check_out > check_in)
- Índices em queries frequentes (room + dates, status)
- Transações implícitas por PreparedStatement
- Sem SQL injection (sempre com PreparedStatement)

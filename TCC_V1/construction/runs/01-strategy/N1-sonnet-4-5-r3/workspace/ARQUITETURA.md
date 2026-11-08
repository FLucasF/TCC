# Arquitetura do Sistema

## Organização do Código

O sistema foi organizado para que **novos planos de tarifa entrem num lugar só**, sem mexer no que já funciona.

### Como Adicionar um Novo Plano de Tarifa

Como o comercial vive inventando novos planos (melhor idade, antecedência, etc.), o sistema foi preparado para isso:

1. **Crie uma nova classe** em `src/main/java/pousada/domain/rateplan/`
   - Estenda `AbstractRatePlan`
   - Implemente apenas 4 métodos:
     - `name()`: retorna o nome do plano (ex: "SENIOR_CITIZEN")
     - `validate()`: verifica se o plano pode ser usado (ex: idade mínima, antecedência)
     - `calculatePricing()`: calcula subtotal, desconto e total
     - `calculateRefund()`: calcula quanto devolver em caso de cancelamento

2. **Registre o plano** em `RatePlanFactory.java`
   - Adicione uma entrada no mapa `PLANS`

**E pronto!** Nenhum outro arquivo precisa ser modificado. Todos os controllers, repositórios, validações e cálculos já funcionam com o novo plano.

### Exemplo: Plano "Melhor Idade"

```java
public class SeniorCitizenRatePlan extends AbstractRatePlan {
    
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");
    
    @Override
    public String name() {
        return "SENIOR_CITIZEN";
    }
    
    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        // Validação seria feita no momento da reserva, verificando idade do hóspede
        // Por enquanto, sem validação adicional
    }
    
    @Override
    public PricingResult calculatePricing(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        BigDecimal subtotal = calculateSubtotalForNights(checkIn, checkOut, roomType);
        BigDecimal discount = round(subtotal.multiply(DISCOUNT_RATE));
        BigDecimal total = round(subtotal.subtract(discount));
        
        return new PricingResult(subtotal, discount, total);
    }
    
    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid; // Reembolso total
    }
}
```

Depois, registre em `RatePlanFactory`:

```java
private static final Map<String, RatePlan> PLANS = Map.of(
    "STANDARD", new StandardRatePlan(),
    "NON_REFUNDABLE", new NonRefundableRatePlan(),
    "CORPORATE", new CorporateRatePlan(),
    "WEEKEND_PACKAGE", new WeekendPackageRatePlan(),
    "LONG_STAY", new LongStayRatePlan(),
    "SENIOR_CITIZEN", new SeniorCitizenRatePlan()  // <- Nova linha
);
```

### Lógica Comum Reutilizável

A classe `AbstractRatePlan` já oferece:

- `calculateNightPrice()`: calcula o preço de uma noite (com alta temporada se aplicável)
- `calculateSubtotalForNights()`: soma as noites entre check-in e check-out
- `round()`: arredondamento HALF_EVEN para centavos
- `isHighSeason()`: verifica se uma data está em alta temporada
- `countNights()`: conta noites entre duas datas
- `isWeekendPackagePeriod()`: verifica se é sexta a domingo

## Estrutura de Pacotes

```
pousada/
├── Main.java                      # Ponto de entrada, configura Javalin
├── config/
│   └── Database.java              # Configuração do pool de conexões
├── domain/
│   ├── Room.java                  # Modelo de quarto
│   ├── RoomType.java              # Tipos de quarto (enum)
│   ├── Reservation.java           # Modelo de reserva
│   ├── ReservationStatus.java    # Status da reserva (enum)
│   ├── Message.java               # Modelo de mensagem
│   └── rateplan/                  # Planos de tarifa (ponto de extensão)
│       ├── RatePlan.java          # Interface
│       ├── AbstractRatePlan.java  # Lógica comum
│       ├── RatePlanFactory.java   # Registro de planos
│       └── [implementações]       # Um arquivo por plano
├── repository/                    # Acesso ao banco de dados
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── service/
│   └── NotificationService.java   # Geração de emails/comprovantes
└── api/                           # Controllers HTTP
    ├── RoomController.java
    ├── ReservationController.java
    ├── MessageController.java
    ├── ReportController.java
    └── ErrorResponse.java
```

## Pontos de Variação

Os pontos do código preparados para mudanças futuras:

1. **Planos de tarifa** (principal) - descrito acima
2. **Tipos de quarto** - adicionar em `RoomType` enum e inicializar em `RoomRepository`
3. **Canais de notificação** - atualmente só EMAIL, mas preparado para SMS, WhatsApp, etc.
4. **Relatórios** - adicionar novos endpoints em `ReportController`

## Regras de Negócio Centralizadas

- **Alta temporada**: `AbstractRatePlan.isHighSeason()`
- **Transições de status**: métodos `confirm()`, `doCheckIn()`, `doCheckOut()`, `cancel()` em `Reservation`
- **Validação de disponibilidade**: `ReservationRepository.findByRoomAndDateRange()`
- **Arredondamento**: `AbstractRatePlan.round()` - HALF_EVEN em todos os cálculos
- **Geração de comprovantes**: `NotificationService.generateReceipt()`

## Banco de Dados

O sistema cria as tabelas automaticamente na primeira execução:

- `rooms` - quartos da pousada (inicializados com os 5 quartos)
- `reservations` - todas as reservas
- `messages` - mensagens enviadas (emails)

Usa HikariCP para pool de conexões e PostgreSQL 18.

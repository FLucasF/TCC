# Arquitetura do Sistema

## Organização do Código

O sistema foi organizado seguindo as diretrizes do CLAUDE.md, com foco especial na facilidade de adicionar novos planos de tarifa sem modificar código existente.

### Estrutura de Pacotes

```
br.com.maralta/
├── Application.java              # Ponto de entrada
├── controller/                   # Controladores HTTP
│   ├── dto/                     # DTOs de request/response
│   ├── RoomController
│   ├── ReservationController
│   ├── MessageController
│   ├── ReportController
│   └── ExceptionHandler
├── domain/                       # Entidades de domínio
│   ├── rateplan/                # Planos de tarifa
│   │   ├── RatePlan            # Interface
│   │   ├── StandardRatePlan
│   │   ├── NonRefundableRatePlan
│   │   ├── CorporateRatePlan
│   │   ├── WeekendPackageRatePlan
│   │   ├── LongStayRatePlan
│   │   └── RatePlanFactory
│   ├── Room
│   ├── Reservation
│   └── Message
├── repository/                   # Acesso ao banco
│   ├── RoomRepository
│   ├── ReservationRepository
│   └── MessageRepository
├── service/                      # Lógica de negócio
│   ├── ReservationService
│   └── MessageService
└── database/                     # Inicialização do banco
    ├── Database
    └── DatabaseInitializer
```

## Design dos Planos de Tarifa

### Por que essa organização?

O enunciado deixa claro que novos planos de tarifa serão adicionados frequentemente:
- "Só este ano foram esses três"
- "Eles já estão falando de um para a melhor idade e de outro para quem reserva com 60 dias de antecedência"
- "Cada plano chega com o seu jeito de calcular o preço, com as suas regras de quem pode usar e com a sua regra de devolução quando a pessoa cancela"

### Como adicionar um novo plano

Para adicionar um novo plano de tarifa (ex: "Melhor Idade"), siga estes passos:

1. **Adicione o tipo no enum** (1 lugar):
```java
// RatePlanType.java
public enum RatePlanType {
    STANDARD,
    NON_REFUNDABLE,
    CORPORATE,
    WEEKEND_PACKAGE,
    LONG_STAY,
    SENIOR_CITIZEN  // <- novo plano
}
```

2. **Crie a implementação** (1 arquivo novo):
```java
// SeniorCitizenRatePlan.java
public class SeniorCitizenRatePlan implements RatePlan {
    
    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, 
                        int nights, RoomType roomType, String companyCode) {
        // Validações específicas (ex: idade mínima)
    }

    @Override
    public BigDecimal calculateSubtotal(...) {
        // Cálculo das diárias (pode reutilizar lógica padrão)
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        // Desconto específico (ex: 20%)
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        // Política de reembolso
    }
}
```

3. **Registre no factory** (1 linha):
```java
// RatePlanFactory.java
public static RatePlan create(RatePlanType type) {
    return switch (type) {
        case STANDARD -> new StandardRatePlan();
        case NON_REFUNDABLE -> new NonRefundableRatePlan();
        case CORPORATE -> new CorporateRatePlan();
        case WEEKEND_PACKAGE -> new WeekendPackageRatePlan();
        case LONG_STAY -> new LongStayRatePlan();
        case SENIOR_CITIZEN -> new SeniorCitizenRatePlan();  // <- novo
    };
}
```

**Pronto!** Todo o resto do sistema (criação de reserva, cálculo de preços, validações, etc.) já funciona com o novo plano sem precisar de modificações.

### Reutilização de Código

A interface `RatePlan` oferece métodos auxiliares que todos os planos podem usar:

- `isHighSeason(LocalDate)` - verifica se é alta temporada
- `isFriday(LocalDate)` - verifica se é sexta-feira
- `isSunday(LocalDate)` - verifica se é domingo

Isso evita duplicação e garante que a lógica de temporadas seja consistente.

## Cálculos de Preço

Todos os valores monetários usam `BigDecimal` com arredondamento `HALF_EVEN` (arredondamento "meio para o par") conforme especificado:

1. Preço de cada noite (base + 20% se alta temporada)
2. Soma das noites (subtotal)
3. Desconto
4. Total = subtotal - desconto

Cada etapa é arredondada para 2 casas decimais.

## Fluxo de Estados da Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
   ↓
CANCELLED (só antes do check-in)
```

## Banco de Dados

O sistema cria 3 tabelas automaticamente ao iniciar:

1. `rooms` - os 5 quartos da pousada
2. `reservations` - todas as reservas
3. `messages` - e-mails enviados (simulados)

Os quartos são populados automaticamente na primeira execução.

## Extensibilidade

Além de planos de tarifa, o sistema está preparado para outras extensões:

- **Novos tipos de mensagem**: basta adicionar ao enum `MessageChannel`
- **Novos tipos de quarto**: adicionar ao enum `RoomType` e popular no banco
- **Novos relatórios**: adicionar métodos no `ReportController`

## Validações

Todas as regras de negócio são validadas:

- Capacidade do quarto
- Disponibilidade (sem sobreposição de datas)
- Transições de estado válidas
- Regras específicas de cada plano de tarifa

Erros retornam códigos HTTP e mensagens adequadas conforme a especificação.

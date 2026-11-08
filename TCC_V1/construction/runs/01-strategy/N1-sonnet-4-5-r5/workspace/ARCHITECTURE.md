# Arquitetura do Sistema

## Visão Geral

O sistema foi organizado seguindo as orientações do CLAUDE.md, especialmente quanto à extensibilidade dos planos de tarifa.

## Estrutura de Diretórios

```
src/main/java/com/marealta/pousada/
├── Main.java                     # Inicialização da aplicação
├── controller/                   # Controllers REST
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   └── ReportController.java
├── domain/
│   ├── model/                    # Modelos de domínio
│   │   ├── Room.java
│   │   ├── RoomType.java
│   │   ├── Reservation.java
│   │   ├── ReservationStatus.java
│   │   ├── Message.java
│   │   └── MessageChannel.java
│   └── rateplan/                 # Planos de tarifa (padrão Strategy)
│       ├── RatePlan.java         # Interface
│       ├── AbstractRatePlan.java # Classe base
│       ├── StandardRatePlan.java
│       ├── NonRefundableRatePlan.java
│       ├── CorporateRatePlan.java
│       ├── WeekendPackageRatePlan.java
│       ├── LongStayRatePlan.java
│       └── RatePlanFactory.java
├── service/                      # Lógica de negócio
│   ├── RoomService.java
│   ├── ReservationService.java
│   ├── MessageService.java
│   └── ReportService.java
├── repository/                   # Acesso a dados
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── dto/                          # Data Transfer Objects
│   ├── RoomDto.java
│   ├── ReservationDto.java
│   ├── CreateReservationRequest.java
│   ├── DocumentDto.java
│   ├── MessageDto.java
│   └── ErrorResponse.java
├── exception/                    # Exceções customizadas
│   ├── InvalidRequestException.java
│   ├── RoomNotFoundException.java
│   ├── ReservationNotFoundException.java
│   ├── RoomUnavailableException.java
│   ├── OperationNotAllowedException.java
│   └── CapacityExceededException.java
└── util/                         # Utilitários
    ├── MoneyUtil.java            # Arredondamento HALF_EVEN
    └── DateUtil.java             # Cálculo de alta temporada
```

## Pontos-Chave da Arquitetura

### 1. Planos de Tarifa (Padrão Strategy)

A parte mais importante do design é a estrutura de planos de tarifa. Cada plano é uma classe independente que implementa a interface `RatePlan`:

```java
public interface RatePlan {
    String getName();
    void validate(...);
    BigDecimal calculateSubtotal(...);
    BigDecimal calculateDiscount(...);
    BigDecimal calculateRefund(...);
}
```

**Para adicionar um novo plano de tarifa:**

1. Crie uma nova classe que estende `AbstractRatePlan`
2. Implemente os métodos específicos do plano
3. Adicione o nome do plano no `RatePlanFactory`

**Exemplo:** Para adicionar um plano "Melhor Idade":

```java
public class SeniorRatePlan extends AbstractRatePlan {
    @Override
    public String getName() {
        return "SENIOR";
    }
    
    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        // Validações específicas (ex: idade mínima)
    }
    
    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        return calculateStandardSubtotal(checkIn, checkOut, roomType);
    }
    
    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return MoneyUtil.percentage(subtotal, new BigDecimal("20"));
    }
    
    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
```

Depois adicione no `RatePlanFactory`:
```java
case "SENIOR" -> new SeniorRatePlan();
```

**Pronto!** O novo plano estará disponível sem mexer em nenhuma outra parte do código.

### 2. Arredondamento Correto

Todos os cálculos monetários usam `MoneyUtil.round()` que aplica `RoundingMode.HALF_EVEN` ("meio para o par") conforme especificado. Cada etapa do cálculo é arredondada separadamente:

- Preço de cada noite individual
- Soma das noites
- Desconto
- Total
- Reembolso

### 3. Alta Temporada

O cálculo de alta temporada está centralizado em `DateUtil.isHighSeason()`:
- 15/12 até fim de fevereiro
- Julho inteiro

Quando uma noite é de alta temporada, a diária é multiplicada por 1.20 (20% a mais).

### 4. Banco de Dados

O sistema cria automaticamente as tabelas necessárias na inicialização:
- `rooms` - Quartos da pousada
- `reservations` - Reservas
- `messages` - Mensagens enviadas (mock de email)

### 5. Validações dos Planos

Cada plano valida suas próprias regras:

- **CORPORATE**: Requer código de empresa válido (TECNOSUL, AGROVALE, FARMAVIDA)
- **WEEKEND_PACKAGE**: Entrada sexta, saída domingo, exatamente 2 noites
- **LONG_STAY**: Mínimo 7 noites

Quando a validação falha, retorna 400 com `INVALID_REQUEST`.

## Testes

Os testes unitários em `RatePlanTest.java` validam todos os planos de tarifa e os casos específicos mencionados:

1. ✓ Convênio Tecnosul: R$ 779,80 → desconto R$ 93,58 → total R$ 686,22
2. ✓ Pacote fim de semana: R$ 820,00 → reembolso R$ 410,00
3. ✓ Estadia longa: R$ 1.749,30 → desconto R$ 174,93 → total R$ 1.574,37
4. ✓ Alta temporada: julho aplica +20% nas diárias

Execute com: `mvn test`

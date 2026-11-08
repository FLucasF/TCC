# Arquitetura - Sistema de Reservas Pousada Maré Alta

## Visão Geral

Sistema de reservas desenvolvido com Java 21, Javalin 6.7.0 e PostgreSQL 18, sem uso de Spring Framework.

## Estrutura do Projeto

```
src/main/java/com/pousada/marealto/
├── Main.java                    # Ponto de entrada da aplicação
├── domain/                      # Entidades de domínio
│   ├── Room.java
│   ├── RoomType.java
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   └── RatePlan.java
├── rateplan/                    # Strategy Pattern para planos de tarifa
│   ├── RatePlanStrategy.java   # Interface da estratégia
│   ├── PriceCalculation.java   # DTO de retorno
│   ├── SeasonCalculator.java   # Utilitário para cálculos
│   ├── StandardRatePlan.java
│   ├── NonRefundableRatePlan.java
│   ├── CorporateRatePlan.java
│   ├── WeekendPackageRatePlan.java
│   ├── LongStayRatePlan.java
│   └── RatePlanFactory.java    # Factory para as estratégias
├── repository/                  # Acesso a dados
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── MessageRepository.java
├── service/                     # Lógica de negócio
│   ├── RoomService.java
│   ├── ReservationService.java
│   ├── MessageService.java
│   └── ReportService.java
├── api/                         # Controladores REST
│   ├── RoomController.java
│   ├── ReservationController.java
│   ├── MessageController.java
│   └── ReportController.java
├── messaging/                   # Sistema de mensagens
│   ├── Message.java
│   └── MessageChannel.java
└── database/
    └── DatabaseManager.java     # Gerenciamento do banco
```

## Padrão Strategy para Planos de Tarifa

O sistema foi projetado usando o **Strategy Pattern** para os planos de tarifa, permitindo que novos planos sejam adicionados sem modificar o código existente.

### Como funciona

Cada plano de tarifa implementa a interface `RatePlanStrategy`:

```java
public interface RatePlanStrategy {
    void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);
    PriceCalculation calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights);
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
```

### Como adicionar um novo plano de tarifa

1. **Criar a classe do novo plano** em `rateplan/`:

```java
public class NewRatePlan implements RatePlanStrategy {
    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        // Regras de validação específicas do plano
        if (/* condição não atendida */) {
            throw new IllegalArgumentException("Mensagem de erro");
        }
    }

    @Override
    public PriceCalculation calculatePrice(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights) {
        // Lógica de cálculo de preço
        BigDecimal subtotal = /* calcular */;
        BigDecimal discount = /* calcular */;
        BigDecimal total = SeasonCalculator.round(subtotal.subtract(discount));
        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        // Regra de reembolso
        return /* calcular reembolso */;
    }
}
```

2. **Adicionar o enum** em `domain/RatePlan.java`:

```java
public enum RatePlan {
    STANDARD,
    NON_REFUNDABLE,
    CORPORATE,
    WEEKEND_PACKAGE,
    LONG_STAY,
    NEW_PLAN  // Adicionar aqui
}
```

3. **Registrar no factory** em `rateplan/RatePlanFactory.java`:

```java
private static final Map<RatePlan, RatePlanStrategy> STRATEGIES = Map.of(
    RatePlan.STANDARD, new StandardRatePlan(),
    // ... outros planos ...
    RatePlan.NEW_PLAN, new NewRatePlan()  // Adicionar aqui
);
```

Pronto! O novo plano estará disponível sem modificar nenhum outro código.

## Planos de Tarifa Existentes

### STANDARD
- Sem desconto
- Reembolso: 100% ao cancelar

### NON_REFUNDABLE
- 15% de desconto sobre as diárias
- Reembolso: 0% ao cancelar

### CORPORATE
- Desconto por empresa: Tecnosul (12%), Agrovale (18%), Farmavida (25%)
- Reembolso: 100% ao cancelar
- Requer código da empresa válido

### WEEKEND_PACKAGE
- Preço fixo: R$ 520 (standard), R$ 820 (deluxe), R$ 1.200 (suíte)
- Deve ser sexta a domingo (2 noites)
- Reembolso: 50% ao cancelar

### LONG_STAY
- 10% de desconto sobre as diárias
- Mínimo 7 noites
- Reembolso: 100% ao cancelar

## Alta Temporada

- 15 de dezembro a fim de fevereiro
- Julho inteiro
- Acréscimo de 20% na diária

## Arredondamento

Todo cálculo monetário usa **arredondamento bancário** (HALF_EVEN):
- 0.5 → 0 (par mais próximo)
- 1.5 → 2 (par mais próximo)

## Execução

```bash
# Compilar
mvn package

# Executar
java -jar target/mare-alta-1.0.0.jar

# Com variáveis de ambiente customizadas
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=postgres \
PORT=7070 \
java -jar target/mare-alta-1.0.0.jar
```

## API Endpoints

- `GET /rooms` - Lista quartos
- `POST /reservations` - Cria reserva
- `GET /reservations/{id}` - Busca reserva
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Busca comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro

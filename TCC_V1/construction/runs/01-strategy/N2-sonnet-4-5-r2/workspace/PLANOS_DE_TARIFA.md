# Planos de Tarifa - Documentação Técnica

Este documento detalha a implementação dos planos de tarifa usando o **Strategy Pattern**.

## Arquitetura

Cada plano de tarifa implementa a interface `RatePlan`:

```java
public interface RatePlan {
    void validate(LocalDate checkIn, LocalDate checkOut, String companyCode);
    BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut);
    BigDecimal calculateDiscount(BigDecimal subtotal, RoomType roomType, LocalDate checkIn, LocalDate checkOut);
    BigDecimal calculateRefund(BigDecimal amountPaid);
}
```

## Planos Implementados

### 1. STANDARD (StandardRatePlan)

**Descrição**: Plano normal sem restrições.

**Validação**: Nenhuma restrição.

**Cálculo de Preço**:
- Soma as diárias (com alta temporada se aplicável)
- Sem desconto

**Reembolso**: 100% do valor pago

**Exemplo**:
```json
{
  "ratePlan": "STANDARD",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12"
}
```

### 2. NON_REFUNDABLE (NonRefundableRatePlan)

**Descrição**: Plano não reembolsável com desconto.

**Validação**: Nenhuma restrição.

**Cálculo de Preço**:
- Soma as diárias (com alta temporada se aplicável)
- Desconto de 15% sobre o subtotal

**Reembolso**: 0% (sem reembolso)

**Exemplo**:
```json
{
  "ratePlan": "NON_REFUNDABLE",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12"
}
```

### 3. CORPORATE (CorporateRatePlan)

**Descrição**: Plano corporativo com desconto por empresa.

**Validação**: Requer código de empresa válido (TECNOSUL, AGROVALE, FARMAVIDA).

**Cálculo de Preço**:
- Soma as diárias (com alta temporada se aplicável)
- Desconto variável por empresa:
  - TECNOSUL: 12%
  - AGROVALE: 18%
  - FARMAVIDA: 25%

**Reembolso**: 100% do valor pago

**Exemplo**:
```json
{
  "ratePlan": "CORPORATE",
  "companyCode": "TECNOSUL",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12"
}
```

### 4. WEEKEND_PACKAGE (WeekendPackageRatePlan)

**Descrição**: Pacote de fim de semana com preço fixo.

**Validação**:
- Deve ser exatamente 2 noites
- Entrada na sexta-feira
- Saída no domingo

**Cálculo de Preço**:
- Preço fixo por tipo de quarto (independente de temporada):
  - STANDARD: R$ 520,00
  - DELUXE: R$ 820,00
  - SUITE: R$ 1.200,00
- Sem desconto (preço já é fixo)

**Reembolso**: 50% do valor pago

**Exemplo**:
```json
{
  "ratePlan": "WEEKEND_PACKAGE",
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14"
}
```

### 5. LONG_STAY (LongStayRatePlan)

**Descrição**: Estadia longa com desconto.

**Validação**: Mínimo de 7 noites.

**Cálculo de Preço**:
- Soma as diárias (com alta temporada se aplicável)
- Desconto de 10% sobre o subtotal

**Reembolso**: 100% do valor pago

**Exemplo**:
```json
{
  "ratePlan": "LONG_STAY",
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-08"
}
```

## Alta Temporada

A alta temporada é calculada automaticamente e adiciona 20% ao valor da diária:

- 15 de dezembro a 28/29 de fevereiro
- Todo o mês de julho

## Arredondamento

Todos os valores monetários são arredondados para 2 casas decimais usando **HALF_EVEN** (arredondamento bancário) a cada passo do cálculo:
- Valor de cada noite
- Subtotal (soma das noites)
- Desconto
- Total

## Adicionando um Novo Plano

Para adicionar um novo plano de tarifa:

1. Criar uma nova classe que implementa `RatePlan`:
```java
public class NovoPlanoRatePlan implements RatePlan {
    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        // Implementar validações específicas
    }
    
    @Override
    public BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        // Implementar cálculo do subtotal
    }
    
    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        // Implementar cálculo do desconto
    }
    
    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        // Implementar cálculo do reembolso
    }
}
```

2. Adicionar o novo tipo em `RatePlanType`:
```java
public enum RatePlanType {
    STANDARD,
    NON_REFUNDABLE,
    CORPORATE,
    WEEKEND_PACKAGE,
    LONG_STAY,
    NOVO_PLANO  // Adicionar aqui
}
```

3. Registrar no `RatePlanRegistry`:
```java
private static final Map<RatePlanType, RatePlan> PLANS = Map.of(
    RatePlanType.STANDARD, new StandardRatePlan(),
    RatePlanType.NON_REFUNDABLE, new NonRefundableRatePlan(),
    RatePlanType.CORPORATE, new CorporateRatePlan(),
    RatePlanType.WEEKEND_PACKAGE, new WeekendPackageRatePlan(),
    RatePlanType.LONG_STAY, new LongStayRatePlan(),
    RatePlanType.NOVO_PLANO, new NovoPlanoRatePlan()  // Adicionar aqui
);
```

Pronto! O novo plano estará disponível sem necessidade de modificar nenhum código existente.

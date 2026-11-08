# Sistema de Reservas - Pousada Maré Alta

Sistema de gestão de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Arquitetura

O sistema foi construído seguindo o padrão **Strategy** para os planos de tarifa, permitindo que novos planos sejam adicionados facilmente sem modificar o código existente. Cada plano de tarifa implementa a interface `RatePlan` com:
- Validação de regras específicas
- Cálculo de preço
- Cálculo de reembolso no cancelamento

## Planos de Tarifa Implementados

1. **STANDARD**: Plano normal, sem desconto, reembolso total
2. **NON_REFUNDABLE**: 15% de desconto, sem reembolso
3. **CORPORATE**: Desconto por empresa conveniada (TECNOSUL 12%, AGROVALE 18%, FARMAVIDA 25%), reembolso total
4. **WEEKEND_PACKAGE**: Pacote de fim de semana (sexta a domingo), preço fixo, reembolso de 50%
5. **LONG_STAY**: Estadia longa (7+ noites), 10% de desconto, reembolso total

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.8+

## Compilação

```bash
mvn clean package
```

O comando gera um JAR executável em `target/reservas-1.0.0.jar`.

## Execução

### Variáveis de Ambiente

- `DB_URL`: URL de conexão JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta HTTP (padrão: `7070`)

### Executar

```bash
java -jar target/reservas-1.0.0.jar
```

Ou com variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=pousada \
DB_PASSWORD=senha123 \
PORT=8080 \
java -jar target/reservas-1.0.0.jar
```

## API REST

### Quartos

**GET /rooms** - Lista todos os quartos disponíveis

### Reservas

**POST /reservations** - Cria uma nova reserva
```json
{
  "roomNumber": "201",
  "guestName": "Carla Dias",
  "guestEmail": "carla@email.com",
  "guestCpf": "11122233344",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "CORPORATE",
  "companyCode": "TECNOSUL"
}
```

**GET /reservations/{id}** - Consulta uma reserva

**POST /reservations/{id}/confirmation** - Confirma a reserva (efetua pagamento)

**POST /reservations/{id}/check-in** - Faz check-in

**POST /reservations/{id}/check-out** - Faz check-out e retorna o comprovante

**POST /reservations/{id}/cancellation** - Cancela a reserva

**GET /reservations/{id}/document** - Segunda via do comprovante

### Mensagens

**GET /messages** - Lista todas as mensagens enviadas (emails)

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação por noite (CSV)

**GET /reports/revenue?month=2027-07** - Relatório financeiro mensal (CSV)

## Testes

Execute os testes unitários:

```bash
mvn test
```

Execute todos os testes (incluindo integração, requer Docker):

```bash
mvn verify
```

## Adicionando Novos Planos de Tarifa

Para adicionar um novo plano de tarifa:

1. Crie uma classe que implemente `RatePlan`:
```java
public class NovoPlano implements RatePlan {
    @Override
    public void validate(ReservationRequest request, Room room) {
        // Regras de validação
    }
    
    @Override
    public PriceCalculation calculatePrice(ReservationRequest request, Room room) {
        // Cálculo do preço
    }
    
    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        // Cálculo do reembolso
    }
}
```

2. Adicione o novo tipo em `RatePlanType`:
```java
public enum RatePlanType {
    STANDARD, NON_REFUNDABLE, CORPORATE, WEEKEND_PACKAGE, LONG_STAY,
    NOVO_PLANO  // Adicione aqui
}
```

3. Registre no `RatePlanFactory`:
```java
private static final Map<RatePlanType, RatePlan> PLANS = Map.of(
    // ... planos existentes ...
    RatePlanType.NOVO_PLANO, new NovoPlano()
);
```

Pronto! O novo plano estará disponível sem modificar nenhuma outra parte do sistema.

# Arquitetura do Sistema

## Visão Geral

O sistema foi construído seguindo os princípios SOLID e usando o **Strategy Pattern** para implementar os diferentes planos de tarifa, conforme orientado pelo CLAUDE.md.

## Estrutura de Pacotes

```
com.pousadamarealta/
├── domain/              # Entidades e enums de domínio
├── rateplan/            # Estratégias de planos de tarifa
├── repository/          # Acesso a dados (PostgreSQL)
├── service/             # Lógica de negócio
├── api/                 # Controllers REST
├── exception/           # Exceções customizadas
└── util/                # Utilitários (DB, cálculo de temporada)
```

## Padrões de Design

### Strategy Pattern - Planos de Tarifa

Cada plano de tarifa implementa a interface `RatePlanStrategy`:

```java
public interface RatePlanStrategy {
    void validate(...);           // Validações específicas do plano
    PricingResult calculatePrice(...);  // Cálculo de preço
    BigDecimal calculateRefund(...);    // Política de reembolso
}
```

**Implementações:**
- `StandardRatePlan`: Plano normal, reembolso total
- `NonRefundableRatePlan`: 15% desconto, sem reembolso
- `CorporateRatePlan`: Desconto corporativo, reembolso total
- `WeekendPackageRatePlan`: Preço fixo fim de semana, reembolso 50%
- `LongStayRatePlan`: 10% desconto para 7+ noites, reembolso total

**Registry:**
O `RatePlanRegistry` mapeia cada enum `RatePlan` para sua estratégia correspondente.

**Vantagens:**
- Adicionar um novo plano requer apenas criar uma nova classe e registrá-la
- Código existente não precisa mudar
- Cada plano encapsula suas próprias regras
- Fácil testar cada plano isoladamente

## Camadas

### Domain Layer
Entidades de domínio sem lógica de negócio complexa:
- `Room`: Quarto da pousada
- `Reservation`: Reserva com todos seus atributos
- `Message`: Mensagem enviada (email)

### Repository Layer
Acesso direto ao banco PostgreSQL usando JDBC:
- `RoomRepository`: CRUD de quartos
- `ReservationRepository`: CRUD de reservas + queries especializadas
- `MessageRepository`: Armazenamento de mensagens

### Service Layer
Lógica de negócio orquestrada:
- `ReservationService`: Criação, confirmação, cancelamento, check-in/out
- `NotificationService`: Geração de comprovantes e envio de emails
- `ReportService`: Geração de relatórios CSV

### API Layer
Controllers REST com Javalin:
- `RoomController`: Endpoints de quartos
- `ReservationController`: Endpoints de reservas
- `MessageController`: Endpoints de mensagens
- `ReportController`: Endpoints de relatórios

## Banco de Dados

### Tabelas

**rooms**
- `number` (PK): Número do quarto
- `type`: Tipo do quarto (STANDARD, DELUXE, SUITE)

**reservations**
- `id` (PK): ID auto-incremento
- `room_number` (FK): Referência ao quarto
- Dados do hóspede (name, email, cpf)
- Datas (check_in, check_out)
- Valores (subtotal, discount, total, amount_paid, refund)
- Estado (status)
- Configuração (rate_plan, company_code, guests, nights)

**messages**
- `id` (PK): ID auto-incremento
- `channel`: Canal de envio (EMAIL)
- `recipient`: Destinatário
- `subject`: Assunto
- `body`: Corpo da mensagem

### Inicialização
O `DatabaseManager` cria as tabelas automaticamente e popula os 5 quartos da pousada na primeira execução.

## Validações

### Reserva
- Número mínimo de hóspedes: 1
- Capacidade máxima do quarto
- Data de saída após data de entrada
- Quarto não pode ter reservas conflitantes
- Validações específicas de cada plano de tarifa

### Operações
- Confirmação: apenas reservas PENDING
- Check-in: apenas reservas CONFIRMED
- Check-out: apenas reservas CHECKED_IN
- Cancelamento: apenas PENDING ou CONFIRMED

## Extensibilidade

### Adicionar Novo Plano de Tarifa

1. Criar nova classe implementando `RatePlanStrategy`
2. Adicionar enum em `RatePlan`
3. Registrar no `RatePlanRegistry`
4. Nenhum código existente precisa mudar

Exemplo:
```java
public class SeniorCitizenRatePlan implements RatePlanStrategy {
    @Override
    public void validate(...) {
        // Validar idade > 60
    }
    
    @Override
    public PricingResult calculatePrice(...) {
        // 20% desconto
    }
    
    @Override
    public BigDecimal calculateRefund(...) {
        // Reembolso total
    }
}
```

### Adicionar Nova Funcionalidade

O sistema está preparado para expansão:
- Novos tipos de quarto: adicionar em `RoomType`
- Novos canais de mensagem: adicionar em `MessageChannel`
- Novos relatórios: adicionar métodos em `ReportService`
- Novos endpoints: adicionar em controllers correspondentes

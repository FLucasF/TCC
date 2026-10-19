# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas desenvolvido em Java 21 com Javalin, seguindo os padrões de design GoF.

## Arquitetura

O sistema foi construído aplicando três padrões de design principais:

### 1. **State Pattern** - Estados de Reserva
Cada estado da reserva (PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED) é representado por uma classe que implementa a interface `ReservationState`. Isso permite que:
- Novas transições entrem sem modificar código existente
- Cada estado defina suas próprias regras de transição
- O código fique mais legível e organizado

### 2. **Strategy Pattern** - Planos de Tarifa
Cada plano de tarifa (STANDARD, NON_REFUNDABLE) é uma estratégia que implementa `RatePlanStrategy`. Para adicionar um novo plano:
- Criar uma classe que implementa `RatePlanStrategy`
- Implementar o cálculo de desconto específico
- Adicionar no enum `RatePlan`

### 3. **Strategy Pattern** - Relatórios CSV
Cada tipo de relatório implementa `ReportStrategy`. Para adicionar um novo relatório:
- Criar uma classe que implementa `ReportStrategy`
- Implementar a lógica de geração do CSV
- Registrar no `ReportService`

## Compilar e Executar

```bash
# Compilar e executar testes
mvn clean verify

# Executar o sistema
java -jar target/mare-alta.jar

# Ou especificar a porta
PORT=8080 java -jar target/mare-alta.jar
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Consultar reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens
- `GET /messages` - Lista emails enviados

### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro
- `GET /reports/tourism-tax?month=YYYY-MM` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=YYYY-MM` - Relatório por plano de tarifa

## Regras de Negócio

### Preços
- Alta temporada (15/12 a 28/02 e todo mês de julho): +20%
- Plano NON_REFUNDABLE: -15% de desconto
- Arredondamento HALF_EVEN em cada etapa do cálculo

### Estados e Transições
- PENDING → CONFIRMED (ao pagar)
- CONFIRMED → CHECKED_IN ou CANCELLED
- CHECKED_IN → CHECKED_OUT
- Cancelamento só antes do check-in

### Reembolsos
- STANDARD: reembolso total se já pago
- NON_REFUNDABLE: sem reembolso
- PENDING: não há valor a reembolsar

## Extensibilidade

O sistema foi projetado para facilitar a adição de:

1. **Novos relatórios**: Implementar `ReportStrategy` e registrar no serviço
2. **Novos planos de tarifa**: Implementar `RatePlanStrategy`
3. **Novos canais de notificação**: Estender `NotificationService`
4. **Mudança na taxa de turismo**: Alterar constante em `TourismTaxReportStrategy`

Cada nova funcionalidade entra em um único lugar, sem modificar código existente.

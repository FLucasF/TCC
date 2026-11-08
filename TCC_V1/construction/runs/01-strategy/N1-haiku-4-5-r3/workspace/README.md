# Pousada Maré Alta - Sistema de Reservas

Sistema completo de reservas para a Pousada Maré Alta, construído com Java 21, Javalin, e PostgreSQL.

## Setup

### Requisitos
- Java 21
- Maven
- PostgreSQL 18
- Docker (opcional, para PostgreSQL)

### Variáveis de Ambiente
- `DB_URL`: URL de conexão JDBC para o PostgreSQL (ex: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco de dados
- `DB_PASSWORD`: Senha do banco de dados
- `PORT`: Porta para o servidor (padrão: 7070)

### Executar Localmente

```bash
# Compilar
mvn clean compile

# Executar testes
mvn verify

# Construir JAR
mvn package

# Executar
java -jar target/mare-alta-1.0.0.jar
```

## Arquitetura

### Padrão de Design
O sistema utiliza o padrão **Strategy** para os planos de tarifa, permitindo fácil adição de novos planos sem modificar o código existente.

### Estrutura do Código
```
src/main/java/com/pousada/
├── model/              # Modelos de domínio
├── repository/         # Acesso aos dados
├── service/            # Lógica de negócio
├── rateplan/          # Estratégias de plano de tarifa
├── api/                # Controllers da API REST
├── util/               # Utilitários
└── infrastructure/     # Configuração do banco de dados
```

### Planos de Tarifa

1. **STANDARD**: Preço normal, reembolsável 100%
2. **NON_REFUNDABLE**: 15% desconto, não reembolsável
3. **CORPORATE**: Desconto por empresa conveniada, reembolsável 100%
   - TECNOSUL: 12%
   - AGROVALE: 18%
   - FARMAVIDA: 25%
4. **WEEKEND_PACKAGE**: Entrada sexta, saída domingo, preço fixo, reembolsável 50%
   - STANDARD: R$ 520,00
   - DELUXE: R$ 820,00
   - SUITE: R$ 1.200,00
5. **LONG_STAY**: 7+ noites, 10% desconto, reembolsável 100%

### Alta Temporada
- 15 de dezembro a fim de fevereiro
- Julho inteiro
- Acréscimo de 20% nas diárias durante estes períodos

## API REST

### Endpoints

#### Quartos
- `GET /rooms` - Lista todos os quartos

#### Reservas
- `POST /reservations` - Criar reserva
- `GET /reservations/{id}` - Detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out (retorna comprovante)
- `GET /reservations/{id}/document` - Segunda via do comprovante

#### Mensagens
- `GET /messages` - Lista de mensagens enviadas (emails)

#### Relatórios
- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação de um dia (CSV)
- `GET /reports/revenue?month=YYYY-MM` - Receita de um mês (CSV)

## Arredondamento Monetário

O sistema utiliza arredondamento "meio para par" (HALF_EVEN) em todas as operações monetárias:
- Preço de cada noite
- Subtotal
- Desconto
- Total

## Exemplos

### Criar Reserva CORPORATE
```json
POST /reservations
{
  "roomNumber": 201,
  "guestName": "João Silva",
  "guestEmail": "joao@email.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "CORPORATE",
  "companyCode": "TECNOSUL"
}
```

### Criar Reserva WEEKEND_PACKAGE
```json
POST /reservations
{
  "roomNumber": 201,
  "guestName": "Maria Santos",
  "guestEmail": "maria@email.com",
  "guestCpf": "98765432101",
  "checkIn": "2027-03-12",
  "checkOut": "2027-03-14",
  "guests": 2,
  "ratePlan": "WEEKEND_PACKAGE"
}
```

### Criar Reserva LONG_STAY
```json
POST /reservations
{
  "roomNumber": 101,
  "guestName": "Carlos Costa",
  "guestEmail": "carlos@email.com",
  "guestCpf": "55555555501",
  "checkIn": "2027-03-01",
  "checkOut": "2027-03-08",
  "guests": 1,
  "ratePlan": "LONG_STAY"
}
```

## Desenvolvimento

O projeto segue as orientações do CLAUDE.md para organização do código, promovendo reutilização e minimizando duplicação.

### Adicionando um Novo Plano de Tarifa

1. Criar uma classe implementando `RatePlanStrategy`:
```java
public class MyNewPlan implements RatePlanStrategy {
  @Override
  public void validate(...) throws InvalidReservationException { ... }
  
  @Override
  public PriceCalculation calculatePrice(...) { ... }
  
  @Override
  public BigDecimal calculateRefund(BigDecimal amountPaid) { ... }
}
```

2. Adicionar o novo plano à enum `Reservation.RatePlan`

3. Atualizar `RatePlanFactory.create()` para retornar a nova implementação

4. Adicionar testes em `src/test/java/com/pousada/rateplan/`

# Sistema de Reservas - Pousada Maré Alta

Sistema completo de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin 6.7.0, PostgreSQL 18 e Maven.

## Características

- **Gerenciamento de Quartos**: 5 quartos com tipos e tarifas configuráveis
- **Ciclo de Vida de Reservas**: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT, com possibilidade de CANCELLED
- **Cálculo de Preços**: 
  - Arredondamento banker's (HALF_EVEN) em todos os valores
  - Ajuste de 20% na alta temporada (15-31 dez + 1-28 fev + julho)
  - Planos: STANDARD (sem desconto) e NON_REFUNDABLE (15% desconto)
- **Sistema de Notificações Extensível**: Observer pattern permitindo adicionar novos subscribers sem modificar código existente
  - Notificação ao hóspede (confirmação e comprovante)
  - Notificação à governança (datas e ocupação)
  - Notificação ao financeiro (pagamentos e reembolsos)
- **Relatórios**: CSV com ocupação por data e receita por mês
- **Banco de Dados**: PostgreSQL 18 com migrações automáticas via Flyway

## Arquitetura

O projeto segue **Domain-Driven Design** com separação clara de camadas:

```
domain/                 - Lógica pura de negócio
├── shared/             - Value objects (Money, DateRange, Guest)
├── room/               - Agregado Room
├── reservation/        - Agregado Reservation
├── pricing/            - Estratégias de precificação
├── notification/       - Eventos de domínio e NotificationSubscriber
└── repository/         - Interfaces de persistência

application/            - Serviços de aplicação
├── reservation/        - ReservationService, OccupancyCalculator

infrastructure/         - Implementações de baixo nível
├── persistence/        - Repositórios PostgreSQL
├── database/           - Setup de conexão e migrações
├── notification/       - Implementações de NotificationSubscriber
└── web/                - API Javalin, DTOs, handlers
```

## Pré-requisitos

- Java 21+
- PostgreSQL 18
- Maven 3.6+

## Instalação e Execução

### 1. Configurar Banco de Dados

```bash
# PostgreSQL deve estar em execução
createdb pousada_maraealta
```

### 2. Executar a Aplicação

```bash
# Build
mvn clean package

# Executar (usa porta padrão 7070)
java -jar target/mare-alta-reservas.jar

# Ou com porta customizada
PORT=8080 java -jar target/mare-alta-reservas.jar
```

### 3. Variáveis de Ambiente

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada_maraealta
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
PORT=7070  # opcional, padrão é 7070
```

## API Endpoints

### Quartos
- `GET /rooms` - Lista todos os quartos

### Reservas
- `POST /reservations` - Criar reserva (status PENDING)
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva (status CONFIRMED)
- `POST /reservations/{id}/check-in` - Check-in (status CHECKED_IN)
- `POST /reservations/{id}/check-out` - Check-out (status CHECKED_OUT)
- `POST /reservations/{id}/cancellation` - Cancelar reserva

### Mensagens e Relatórios
- `GET /messages` - Listar todas as notificações enviadas
- `GET /reports/occupancy?date=2027-07-10` - Ocupação por data (CSV)
- `GET /reports/revenue?month=2027-07` - Receita por mês (CSV)

## Exemplo de Requisição

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "111.222.333-44",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amount": 779.80}'
```

## Estrutura de Quartos

| Quarto | Tipo | Capacidade | Diária |
|--------|------|-----------|--------|
| 101    | STANDARD | 2 | R$ 249,90 |
| 102    | STANDARD | 2 | R$ 249,90 |
| 201    | DELUXE | 3 | R$ 389,90 |
| 202    | DELUXE | 3 | R$ 389,90 |
| 301    | SUITE | 4 | R$ 579,90 |

## Extensibilidade: Adicionando Novos Notificadores

O sistema permite adicionar novos notificadores (ex: Restaurante, Transfer Aeroporto) sem modificar código existente.

**Passo 1**: Criar uma classe que implemente `NotificationSubscriber`

```java
public class RestaurantNotifier implements NotificationSubscriber {
    @Override
    public String name() { return "RestaurantNotifier"; }
    
    @Override
    public PersistedNotification handle(DomainEvent event) {
        if (event instanceof ReservationConfirmed) {
            // Notificar restaurante sobre ocupação
            return PersistedNotification.success(...);
        }
        return null;
    }
}
```

**Passo 2**: Registrar no `GuesthouseApi`

```java
List<NotificationSubscriber> subscribers = Arrays.asList(
    new GuestNotifier(),
    new GovernanceNotifier(),
    new FinanceNotifier(),
    new RestaurantNotifier()  // ← Novo!
);
```

## Testes

```bash
mvn test
```

## Banco de Dados

Migrações Flyway são executadas automaticamente ao iniciar:
- V001: Criação da tabela `rooms`
- V002: Criação da tabela `reservations`
- V003: Criação da tabela `notifications`
- V004: Dados iniciais dos quartos

## Codesign Decisions

1. **Money Value Object**: Todas as operações monetárias usam a classe `Money` que encapsula `BigDecimal` com arredondamento banker's automático
2. **Observer Pattern**: Notificadores são completamente desacoplados da lógica de reservas através de `NotificationSubscriber`
3. **Domain Events**: `ReservationConfirmed` e `ReservationCancelled` carregam todo o contexto necessário
4. **Repository Pattern**: Abstração via interfaces permite trocar implementação de persistência facilmente
5. **Sem Frameworks de ORM**: Queries SQL diretas com JDBC para máximo controle e performance

## Relatórios CSV

### Ocupação
```
room,type,reservation,check_in,check_out
201,DELUXE,1,2027-07-10,2027-07-12
301,SUITE,2,2027-07-10,2027-07-15
total,2/5
```

### Receita
```
reservation,room,check_in,check_out,status,amount
1,201,2027-07-10,2027-07-12,CHECKED_OUT,779.80
2,301,2027-07-10,2027-07-15,CONFIRMED,1159.80
total,1939.60
```

## Logs

A aplicação usa SLF4J com Logback. Logs são exibidos no console com nível INFO por padrão.

## Licença

Proprietary - Pousada Maré Alta

# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para pousada, desenvolvido em Java 21 com Javalin 6.7.0 e PostgreSQL 18.

## Arquitetura

### Design Pattern: Observer

O sistema utiliza o padrão **Observer** para gerenciar as notificações quando eventos de domínio acontecem (confirmação e cancelamento de reservas).

**Estrutura:**
- **Eventos**: `ReservationConfirmed`, `ReservationCancelled`
- **Listener Interface**: `ReservationEventListener`
- **Listeners Concretos**:
  - `GuestNotifier` - envia comprovante para o hóspede
  - `HousekeepingNotifier` - notifica a governança sobre quartos
  - `FinanceNotifier` - notifica o financeiro sobre pagamentos/reembolsos

**Vantagens dessa abordagem:**
- Adicionar novos interessados (restaurante, transfer) requer apenas criar um novo listener e registrá-lo no wiring
- Cada listener contém sua própria lógica sobre quando e como reagir
- O serviço de reservas apenas anuncia os eventos, sem conhecer os interessados
- Nenhum código existente precisa ser modificado para adicionar novos comportamentos

Conforme especificado no `CLAUDE.md`, a estrutura foi organizada para que "acrescentar mais uma" notificação entre em um lugar só, sem mexer no que já funciona.

## Estrutura do Projeto

```
src/main/java/br/com/pousadamarealta/
├── domain/                     # Modelo de domínio
│   ├── Room, Reservation       # Entidades principais
│   ├── RoomType, RatePlan      # Enums
│   ├── PriceCalculator         # Lógica de cálculo de preços
│   ├── Formatter               # Formatação de valores e datas
│   └── events/                 # Padrão Observer
│       ├── ReservationEventListener
│       ├── ReservationConfirmed
│       └── ReservationCancelled
├── listeners/                  # Implementações do Observer
│   ├── GuestNotifier
│   ├── HousekeepingNotifier
│   └── FinanceNotifier
├── repository/                 # Camada de persistência
│   └── postgres/
├── service/                    # Lógica de negócio
│   ├── ReservationService
│   └── ReportService
├── api/                        # API REST
│   ├── ApiHandler
│   └── dto/
└── Main.java                   # Wiring e inicialização
```

## Funcionalidades

### Quartos
- 5 quartos fixos com tipos STANDARD, DELUXE e SUITE
- Capacidade e diárias conforme tipo

### Reservas
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- Cancelamento possível apenas antes do check-in
- Cálculo automático de preços com alta temporada (+20%)
- Plano NON_REFUNDABLE com desconto de 15%
- Arredondamento banker's rounding (HALF_EVEN) em cada etapa

### Notificações
Quando uma reserva é **confirmada**:
- Hóspede recebe e-mail com comprovante
- Governança recebe detalhes sobre o quarto e datas
- Financeiro recebe valor pago

Quando uma reserva **confirmada** é **cancelada**:
- Governança recebe notificação das datas liberadas
- Financeiro recebe valor a reembolsar (se aplicável)

### Relatórios
- **Ocupação**: quartos ocupados em uma data específica
- **Receita**: valores recebidos no mês (pagamentos - reembolsos)

## Execução

### Pré-requisitos
- Java 21
- PostgreSQL 18
- Maven

### Build
```bash
mvn clean package
```

### Execução
```bash
# Com variáveis de ambiente
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=senha \
PORT=7070 \
java -jar target/reservas-1.0.0.jar
```

O sistema cria automaticamente as tabelas necessárias e popula os 5 quartos.

## API REST

### Quartos
- `GET /rooms` - lista todos os quartos

### Reservas
- `POST /reservations` - cria reserva
- `GET /reservations/{id}` - busca reserva
- `POST /reservations/{id}/confirmation` - confirma reserva
- `POST /reservations/{id}/cancellation` - cancela reserva
- `POST /reservations/{id}/check-in` - faz check-in
- `POST /reservations/{id}/check-out` - faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - segunda via do comprovante

### Mensagens
- `GET /messages` - lista e-mails enviados

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - relatório de ocupação (CSV)
- `GET /reports/revenue?month=2027-07` - relatório financeiro (CSV)

## Testes

```bash
# Testes unitários
mvn test

# Build completo com verificação
mvn verify
```

Os testes cobrem:
- Cálculo de preços (alta temporada, descontos, arredondamento)
- Transições de estado da reserva
- Validações (capacidade, datas, hóspedes)
- Lógica de reembolso

# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.6+

## Configuração

O sistema requer as seguintes variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada
DB_USER=seu_usuario
DB_PASSWORD=sua_senha
PORT=7070  # opcional, padrão 7070
```

## Build

```bash
mvn clean package
```

## Execução

```bash
java -jar target/pousada-marealta-1.0.0.jar
```

O sistema criará automaticamente as tabelas e dados iniciais no banco de dados.

## Arquitetura

O sistema foi projetado seguindo os princípios descritos no CLAUDE.md:

### Sistema de Notificações Extensível

O ponto principal é o sistema de notificações que permite adicionar novos destinatários sem modificar código existente. Quando uma reserva é confirmada ou cancelada, eventos são disparados através de um `EventDispatcher`, e múltiplos handlers podem reagir a esses eventos:

- **GuestNotificationHandler**: Envia comprovante por email ao hóspede
- **HousekeepingNotificationHandler**: Notifica a governança sobre confirmações
- **HousekeepingCancellationHandler**: Notifica a governança sobre cancelamentos
- **FinanceNotificationHandler**: Notifica o financeiro sobre pagamentos
- **FinanceCancellationHandler**: Notifica o financeiro sobre reembolsos

Para adicionar um novo destinatário (como o restaurante ou transfer mencionados na especificação):

1. Crie uma nova classe que implementa `EventHandler<ReservationConfirmedEvent>` ou `EventHandler<ReservationCancelledEvent>`
2. Registre-a no `EventDispatcher` em `Main.registerEventHandlers()`

Nenhum código existente precisa ser modificado.

### Estrutura do Código

```
src/main/java/com/marealta/
├── domain/                      # Regras de negócio
│   ├── model/                  # Entidades (Room, Reservation, etc)
│   ├── event/                  # Eventos de domínio
│   ├── service/                # Serviços de domínio
│   └── repository/             # Interfaces de persistência
├── infrastructure/             # Implementações técnicas
│   ├── database/              # Implementações JDBC
│   ├── messaging/             # Sistema de mensagens
│   └── notification/          # Sistema de eventos
├── application/               # Casos de uso e handlers
│   └── handler/              # Handlers de notificação
└── api/                       # Camada HTTP
    ├── controller/           # Controllers REST
    ├── dto/                  # Objetos de transferência
    └── error/                # Tratamento de erros
```

## Endpoints Principais

- `GET /rooms` - Lista os quartos
- `POST /reservations` - Cria uma reserva
- `GET /reservations/{id}` - Consulta uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out
- `GET /reservations/{id}/document` - Segunda via do comprovante
- `GET /messages` - Lista mensagens enviadas
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Regras de Negócio

- **Alta temporada**: 15/12 a fim de fevereiro e todo o mês de julho (tarifa +20%)
- **Plano NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento
- **Arredondamento**: Todos os valores usam arredondamento "meio para o par" (HALF_EVEN)
- **Validações**: Não permite reservas conflitantes, excesso de hóspedes, ou transições de status inválidas

## Testes

```bash
mvn test
```

Os testes unitários cobrem os cálculos de preços e lógica de sazonalidade.

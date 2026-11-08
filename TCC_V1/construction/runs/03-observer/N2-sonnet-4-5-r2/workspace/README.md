# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin e PostgreSQL.

## Arquitetura

O sistema foi construído seguindo princípios de orientação a objetos e utiliza o **Observer Pattern** para notificações, permitindo que novos interessados sejam facilmente adicionados ao sistema sem modificar o código existente.

### Componentes Principais

- **Domain**: Entidades de negócio (Room, Reservation, Message)
- **Repository**: Acesso a dados com PostgreSQL
- **Notification**: Sistema extensível de notificações usando Observer Pattern
- **API**: Controllers REST para exposição dos endpoints

### Observer Pattern

O sistema de notificações foi implementado usando o Observer Pattern para atender ao requisito de extensibilidade. Quando uma reserva é confirmada ou cancelada, os seguintes observadores são notificados:

- **GuestNotifier**: Envia e-mail de confirmação para o hóspede
- **HousekeepingNotifier**: Notifica a governança sobre reservas confirmadas e canceladas
- **FinanceNotifier**: Notifica o financeiro sobre pagamentos e reembolsos

Para adicionar novos interessados (como restaurante ou transfer), basta:
1. Criar uma nova classe que implementa `ReservationObserver`
2. Registrá-la na inicialização do sistema em `App.java`

## Requisitos

- Java 21
- PostgreSQL 18
- Maven 3.9+

## Configuração

O sistema utiliza variáveis de ambiente para configuração:

- `DB_URL`: URL JDBC do PostgreSQL (padrão: `jdbc:postgresql://localhost:5432/pousada`)
- `DB_USER`: Usuário do banco (padrão: `postgres`)
- `DB_PASSWORD`: Senha do banco (padrão: `postgres`)
- `PORT`: Porta do servidor (padrão: `7070`)

## Build e Execução

### Compilar e testar

```bash
mvn verify
```

### Gerar JAR executável

```bash
mvn package
```

### Executar

```bash
java -jar target/reservas-1.0.0.jar
```

Ou com variáveis de ambiente:

```bash
DB_URL=jdbc:postgresql://localhost:5432/pousada \
DB_USER=postgres \
DB_PASSWORD=postgres \
PORT=8080 \
java -jar target/reservas-1.0.0.jar
```

## API REST

### Quartos

**GET /rooms** - Lista todos os quartos
```json
[
  {
    "number": "101",
    "type": "STANDARD",
    "capacity": 2,
    "nightlyRate": 249.90
  }
]
```

### Reservas

**POST /reservations** - Cria uma nova reserva
```json
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-07-10",
  "checkOut": "2027-07-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**GET /reservations/{id}** - Consulta uma reserva

**POST /reservations/{id}/confirmation** - Confirma uma reserva pendente

**POST /reservations/{id}/cancellation** - Cancela uma reserva

**POST /reservations/{id}/check-in** - Faz check-in

**POST /reservations/{id}/check-out** - Faz check-out e retorna comprovante

**GET /reservations/{id}/document** - Segunda via do comprovante

### Mensagens

**GET /messages** - Lista todas as mensagens enviadas

### Relatórios

**GET /reports/occupancy?date=2027-07-10** - Relatório de ocupação em CSV

**GET /reports/revenue?month=2027-07** - Relatório financeiro mensal em CSV

## Regras de Negócio

### Quartos

- 101, 102: STANDARD (2 pessoas, R$ 249,90)
- 201, 202: DELUXE (3 pessoas, R$ 389,90)
- 301: SUITE (4 pessoas, R$ 579,90)

### Tarifas

- **Alta Temporada** (15/dez a fim/fev + julho): +20%
- **Plano NON_REFUNDABLE**: -15% de desconto, sem reembolso

### Estados da Reserva

```
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
    ↓          ↓
CANCELLED  CANCELLED
```

### Notificações

**Ao confirmar:**
- Hóspede: E-mail com comprovante
- Governança: Aviso de nova reserva com datas e quantidade de hóspedes
- Financeiro: Aviso de pagamento recebido

**Ao cancelar (se estava confirmada):**
- Governança: Aviso de datas liberadas
- Financeiro: Aviso de reembolso (apenas se houver valor a devolver)

## Códigos de Erro

- `400 INVALID_REQUEST`: Dados inválidos ou ausentes
- `404 ROOM_NOT_FOUND`: Quarto não encontrado
- `404 RESERVATION_NOT_FOUND`: Reserva não encontrada
- `409 ROOM_UNAVAILABLE`: Quarto indisponível nas datas solicitadas
- `409 OPERATION_NOT_ALLOWED`: Operação não permitida no estado atual
- `422 CAPACITY_EXCEEDED`: Número de hóspedes excede capacidade do quarto

## Testes

O projeto inclui testes unitários para:
- Cálculo de valores e alta temporada
- Transições de estado das reservas
- Sistema de notificação com observers

Execute os testes com:
```bash
mvn test
```

## Estrutura do Banco de Dados

O sistema cria automaticamente as tabelas necessárias:

- **rooms**: Quartos da pousada
- **reservations**: Reservas
- **messages**: Mensagens enviadas (e-mails)

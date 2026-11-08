# Sistema de Reservas - Pousada Maré Alta

Sistema de gerenciamento de reservas para a Pousada Maré Alta.

## Tecnologias

- Java 21
- Javalin 6.7.0
- PostgreSQL 18
- Maven

## Estrutura do Projeto

O projeto está organizado seguindo princípios de Domain-Driven Design:

- **domain/model**: Entidades de domínio (Reservation, Room, Message)
- **domain/event**: Sistema de eventos para notificações
- **domain/repository**: Interfaces de repositórios
- **application/service**: Serviços de aplicação
- **application/notification**: Listeners de notificação (hóspede, governança, financeiro)
- **infrastructure/persistence**: Implementação de repositórios com PostgreSQL
- **infrastructure/http**: Controllers e DTOs da API REST

### Arquitetura de Eventos

O sistema utiliza um **EventPublisher** para desacoplar a lógica de negócio das notificações. Quando uma reserva é confirmada ou cancelada, eventos são publicados e múltiplos listeners podem reagir independentemente.

**Para adicionar um novo destinatário de notificações** (ex: restaurante, transfer), basta:
1. Criar uma nova classe que implementa `EventListener<ReservationConfirmedEvent>`
2. Registrá-la no `EventPublisher` no `Main.java`

Nenhum código existente precisa ser alterado - o padrão Observer garante extensibilidade.

## Compilação

```bash
mvn clean package
```

Isso gera o JAR executável em `target/pousada-reservas-1.0.0.jar`

## Execução

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pousada"
export DB_USER="postgres"
export DB_PASSWORD="password"
export PORT=7070  # Opcional, padrão é 7070

java -jar target/pousada-reservas-1.0.0.jar
```

O sistema cria automaticamente as tabelas necessárias no banco de dados na primeira execução.

## API REST

### Quartos

**GET /rooms**
Retorna todos os quartos disponíveis.

### Reservas

**POST /reservations**
Cria uma nova reserva (status PENDING).

Corpo:
```json
{
  "roomNumber": "101",
  "guestName": "João Silva",
  "guestEmail": "joao@example.com",
  "guestCpf": "12345678901",
  "checkIn": "2027-03-10",
  "checkOut": "2027-03-12",
  "guests": 2,
  "ratePlan": "STANDARD"
}
```

**GET /reservations/{id}**
Retorna os detalhes de uma reserva.

**POST /reservations/{id}/confirmation**
Confirma uma reserva e envia notificações para hóspede, governança e financeiro.

**POST /reservations/{id}/cancellation**
Cancela uma reserva. Se já estava confirmada, notifica governança e financeiro (se houver reembolso).

**POST /reservations/{id}/check-in**
Registra a entrada do hóspede.

**POST /reservations/{id}/check-out**
Registra a saída e retorna o comprovante.

**GET /reservations/{id}/document**
Segunda via do comprovante (somente para reservas pagas).

### Mensagens

**GET /messages**
Lista todas as mensagens de email enviadas pelo sistema.

### Relatórios

**GET /reports/occupancy?date=2027-07-10**
Relatório de ocupação em CSV para uma data específica.

**GET /reports/revenue?month=2027-07**
Relatório financeiro em CSV para um mês específico.

## Regras de Negócio

### Tipos de Quarto
- **STANDARD** (101, 102): 2 pessoas, R$ 249,90/noite
- **DELUXE** (201, 202): 3 pessoas, R$ 389,90/noite  
- **SUITE** (301): 4 pessoas, R$ 579,90/noite

### Alta Temporada
- 15 de dezembro até fim de fevereiro
- Julho inteiro
- Acréscimo de 20% sobre a diária

### Planos de Tarifa
- **STANDARD**: Preço normal, reembolsável
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso

### Estados da Reserva
PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT

Pode ser CANCELLED a partir de PENDING ou CONFIRMED (antes do check-in).

### Sistema de Notificações

Quando uma reserva é **confirmada**:
- **Hóspede**: Email com comprovante completo
- **Governança**: Email com datas e número de hóspedes
- **Financeiro**: Email com valor recebido

Quando uma reserva confirmada é **cancelada**:
- **Governança**: Email com datas que ficaram livres
- **Financeiro**: Email com valor a devolver (somente se houver reembolso)

## Testes

```bash
mvn test
```

Testes incluem:
- Cálculo de preços (baixa e alta temporada)
- Descontos de planos tarifários
- Transições de estado
- Sistema de eventos e notificações

## Códigos de Erro

- **400 INVALID_REQUEST**: Dados inválidos ou campos faltando
- **404 ROOM_NOT_FOUND**: Quarto não encontrado
- **404 RESERVATION_NOT_FOUND**: Reserva não encontrada
- **409 ROOM_UNAVAILABLE**: Quarto já reservado nas datas solicitadas
- **409 OPERATION_NOT_ALLOWED**: Operação não permitida no estado atual
- **422 CAPACITY_EXCEEDED**: Número de hóspedes excede capacidade do quarto

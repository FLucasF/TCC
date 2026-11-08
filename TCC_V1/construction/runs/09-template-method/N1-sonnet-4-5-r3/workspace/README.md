# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, construído com Java 21 e Javalin.

## Funcionalidades

- Gerenciamento de quartos (5 quartos: 2 STANDARD, 2 DELUXE, 1 SUITE)
- Criação e gerenciamento de reservas
- Cálculo automático de preços com alta temporada e descontos
- Confirmação de reservas com envio de emails (mockado)
- Check-in e check-out
- Cancelamento com reembolso (conforme plano de tarifa)
- Geração de comprovantes
- 4 relatórios em CSV:
  - Ocupação (por data)
  - Financeiro (por mês)
  - Taxa de turismo (por mês)
  - Planos de tarifa (por mês)

## Tecnologias

- Java 21
- Javalin 6.7.0 (web framework)
- PostgreSQL 18 (banco de dados)
- Maven (build)
- JUnit 5 (testes)

## Como Executar

### Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.8+

### Build

```bash
mvn clean package
```

### Executar

```bash
# Com variáveis de ambiente
export DB_URL=jdbc:postgresql://localhost:5432/pousada
export DB_USER=postgres
export DB_PASSWORD=postgres
export PORT=7070

java -jar target/mare-alta-1.0.0.jar
```

O sistema iniciará na porta 7070 (ou na porta especificada pela variável PORT).

### Executar com Docker Compose (exemplo)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: pousada
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
  
  app:
    image: openjdk:21-jdk-slim
    volumes:
      - ./target/mare-alta-1.0.0.jar:/app.jar
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/pousada
      DB_USER: postgres
      DB_PASSWORD: postgres
      PORT: 7070
    ports:
      - "7070:7070"
    command: java -jar /app.jar
    depends_on:
      - postgres
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Busca uma reserva
- `POST /reservations/{id}/confirmation` - Confirma uma reserva
- `POST /reservations/{id}/cancellation` - Cancela uma reserva
- `POST /reservations/{id}/check-in` - Realiza check-in
- `POST /reservations/{id}/check-out` - Realiza check-out
- `GET /reservations/{id}/document` - Busca segunda via do comprovante

### Mensagens

- `GET /messages` - Lista mensagens enviadas (emails mockados)

### Relatórios

- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro
- `GET /reports/tourism-tax?month=2027-07` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=2027-07` - Relatório por plano de tarifa

## Regras de Negócio

### Alta Temporada

- 15 de dezembro a fim de fevereiro
- Todo mês de julho
- Acréscimo de 20% no valor da diária

### Planos de Tarifa

- **STANDARD**: Preço normal, com reembolso total em caso de cancelamento
- **NON_REFUNDABLE**: 15% de desconto, sem reembolso em caso de cancelamento

### Status da Reserva

1. **PENDING**: Reserva criada mas não confirmada
2. **CONFIRMED**: Reserva confirmada com pagamento
3. **CHECKED_IN**: Hóspede fez check-in
4. **CHECKED_OUT**: Hóspede fez check-out
5. **CANCELLED**: Reserva cancelada

### Arredondamento

Todos os valores monetários são arredondados para 2 casas decimais usando o método "banker's rounding" (HALF_EVEN).

## Estrutura do Projeto

```
src/main/java/pousada/
├── Main.java                      # Ponto de entrada
├── config/
│   └── Database.java             # Configuração do banco
├── domain/
│   ├── Message.java              # Entidade de mensagem
│   ├── MessageChannel.java       # Enum de canais
│   ├── RatePlan.java            # Enum de planos
│   ├── Reservation.java         # Entidade de reserva
│   ├── ReservationStatus.java   # Enum de status
│   ├── Room.java                # Entidade de quarto
│   └── RoomType.java            # Enum de tipos de quarto
├── pricing/
│   └── PricingCalculator.java   # Cálculo de preços
├── repository/
│   ├── MessageRepository.java   # Repositório de mensagens
│   ├── ReservationRepository.java # Repositório de reservas
│   └── RoomRepository.java      # Repositório de quartos
├── service/
│   ├── DocumentFormatter.java   # Formatação de comprovantes
│   ├── MessageService.java      # Serviço de mensagens
│   └── ReservationService.java  # Serviço de reservas
├── report/
│   ├── Report.java              # Interface de relatório
│   ├── CsvReportWriter.java     # Escritor CSV comum
│   ├── OccupancyReport.java     # Relatório de ocupação
│   ├── RevenueReport.java       # Relatório financeiro
│   ├── TourismTaxReport.java    # Relatório de taxa
│   └── RatePlanReport.java      # Relatório de planos
├── controller/
│   ├── MessageController.java   # Controller de mensagens
│   ├── ReportController.java    # Controller de relatórios
│   ├── ReservationController.java # Controller de reservas
│   └── RoomController.java      # Controller de quartos
└── dto/
    ├── CreateReservationRequest.java # DTO de criação
    └── ErrorResponse.java       # DTO de erro
```

## Exemplo de Uso

### Criar uma reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation
```

### Obter relatório de ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

## Testes

```bash
# Executar testes unitários
mvn test

# Executar todos os testes (requer Docker para testcontainers)
mvn verify
```

## Extensibilidade

O sistema foi projetado seguindo princípios de extensibilidade:

- **Novos relatórios**: Implementar a interface `Report` e adicionar no `ReportController`
- **Novos canais de mensagem**: Adicionar ao enum `MessageChannel` e estender `MessageService`
- **Mudanças no formato de relatórios**: Modificar `CsvReportWriter` para aplicar a todos os relatórios
- **Novos tipos de quarto**: Adicionar ao enum `RoomType` e inicializar no `RoomRepository`

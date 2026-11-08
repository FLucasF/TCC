# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para pousada com 5 quartos, incluindo gestão de ocupação, relatórios financeiros e administrativos.

## Requisitos

- Java 21+
- PostgreSQL 18+
- Maven 3.6+

## Instalação e Execução

### 1. Configurar variáveis de ambiente

```bash
export DB_URL="jdbc:postgresql://localhost:5432/marealta"
export DB_USER="postgres"
export DB_PASSWORD="seu_password"
export PORT=7070  # opcional, padrão é 7070
```

### 2. Compilar e empacotar

```bash
mvn clean package
```

### 3. Executar a aplicação

```bash
java -jar target/mare-alta.jar
```

O servidor estará disponível em `http://localhost:7070`

## API Endpoints

### Quartos

- `GET /rooms` - Listar todos os quartos

### Reservas

- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva
- `POST /reservations/{id}/check-in` - Fazer check-in
- `POST /reservations/{id}/check-out` - Fazer check-out
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `GET /reservations/{id}/document` - Obter comprovante/segunda via

### Mensagens

- `GET /messages` - Listar mensagens enviadas

### Relatórios

- `GET /reports/occupancy?date=YYYY-MM-DD` - Relatório de ocupação
- `GET /reports/revenue?month=YYYY-MM` - Relatório financeiro
- `GET /reports/tourism-tax?month=YYYY-MM` - Relatório de taxa de turismo
- `GET /reports/rate-plans?month=YYYY-MM` - Relatório de planos de tarifa

## Estrutura da Aplicação

```
src/main/java/com/pousada/
├── Application.java                 # Classe principal
├── RoomsHandler.java               # Endpoints de quartos
├── ReservationsHandler.java        # Endpoints de reservas
├── MessagesHandler.java            # Endpoints de mensagens
├── ReportsHandler.java             # Endpoints de relatórios
├── application/
│   ├── ReservationService.java     # Lógica de negócio
│   └── ApiException.java           # Exceção customizada
├── domain/
│   ├── Room.java                   # Entidade de quarto
│   ├── Reservation.java            # Entidade de reserva
│   ├── Message.java                # Entidade de mensagem
│   ├── RoomType.java               # Enum de tipos de quarto
│   ├── RatePlan.java               # Enum de planos de tarifa
│   ├── ReservationStatus.java      # Enum de status de reserva
│   ├── MessageChannel.java         # Enum de canais de mensagem
│   ├── SeasonCalculator.java       # Cálculo de temporada
│   └── Rounding.java               # Arredondamento HALF_EVEN
├── infrastructure/
│   ├── Database.java               # Inicialização do banco
│   ├── RoomRepository.java         # Persistência de quartos
│   ├── ReservationRepository.java  # Persistência de reservas
│   └── MessageRepository.java      # Persistência de mensagens
├── api/dto/
│   ├── CreateReservationRequest.java
│   ├── ReservationResponse.java
│   ├── RoomResponse.java
│   ├── MessageResponse.java
│   └── ReceiptResponse.java
└── reports/
    ├── Report.java                 # Classe base de relatório
    ├── OccupancyReport.java        # Relatório de ocupação
    ├── RevenueReport.java          # Relatório financeiro
    ├── TourismTaxReport.java       # Relatório de taxa de turismo
    └── RatePlansReport.java        # Relatório de planos de tarifa
```

## Exemplo de Requisição

### Criar Reserva

```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678901",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar Reserva

```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json"
```

### Obter Relatório de Ocupação

```bash
curl http://localhost:7070/reports/occupancy?date=2027-07-10
```

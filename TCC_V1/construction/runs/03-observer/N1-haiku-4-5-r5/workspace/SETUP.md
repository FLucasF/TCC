# Pousada Maré Alta - Sistema de Reservas

## Estrutura do Projeto

- `pom.xml` - Configuração Maven
- `src/main/java/com/pousada/marealta/` - Código principal
  - `App.java` - Aplicação principal com endpoints REST
  - `db/` - Acesso ao banco de dados
  - `model/` - Classes de modelo (Room, Reservation, Message, Document)
  - `enums/` - Enumerações (RoomType, ReservationStatus, RatePlan, MessageChannel)
  - `service/` - Lógica de negócio (ReservationService, ReportService)
  - `notification/` - Sistema de notificações extensível
  - `api/` - Requisições e respostas HTTP
  - `util/` - Utilitários (PricingCalculator, Formatter, DocumentGenerator)
- `src/test/` - Testes

## Compilação e Execução

### Compilar
```bash
mvn clean package
```

Isto gerará um JAR em `target/mare-alta-reservas-1.0.0.jar`.

### Rodar Testes
```bash
mvn verify
```

### Executar a Aplicação
```bash
# Com variáveis de ambiente
DB_URL="jdbc:postgresql://localhost:5432/mare_alta" \
DB_USER="postgres" \
DB_PASSWORD="password" \
PORT=7070 \
java -jar target/mare-alta-reservas-1.0.0.jar
```

## Banco de Dados

A aplicação usa PostgreSQL 18 e cria automaticamente as tabelas:
- `rooms` - Quartos disponíveis
- `reservations` - Reservas
- `messages` - Mensagens enviadas

O banco é criado automaticamente na primeira execução.

## API Endpoints

### Quartos
- `GET /rooms` - Lista quartos disponíveis

### Reservas
- `POST /reservations` - Criar nova reserva
- `GET /reservations/{id}` - Obter detalhes da reserva
- `POST /reservations/{id}/confirmation` - Confirmar reserva (pagar)
- `POST /reservations/{id}/cancellation` - Cancelar reserva
- `POST /reservations/{id}/check-in` - Check-in
- `POST /reservations/{id}/check-out` - Check-out
- `GET /reservations/{id}/document` - Obter segunda via do comprovante

### Mensagens
- `GET /messages` - Listar mensagens enviadas

### Relatórios
- `GET /reports/occupancy?date=2027-07-10` - Relatório de ocupação
- `GET /reports/revenue?month=2027-07` - Relatório financeiro

## Sistema de Notificações

O sistema de notificações é extensível. Novos departamentos podem ser adicionados registrando um notificador em `NotificationManager`.

### Notificadores Implementados
- `GuestNotifier` - Envia e-mail de confirmação ao hóspede
- `HousekeepingNotifier` - Governa recebe notificações (entrada, saída, cancelamento)
- `FinanceNotifier` - Financeiro recebe notificações (pagamento, reembolso)

### Adicionar Novo Notificador

1. Criar classe implementando `ReservationNotifier`
2. Implementar método `handleEvent(ReservationEvent event)`
3. Registrar em `App.initializeNotifiers()` usando `NotificationManager.registerNotifier()`

## Cálculo de Preços

- Base: preço diário do quarto
- Alta temporada (15/12 a 28/02, julho): +20%
- Arredondamento: "meio para par" em cada passo
- NON_REFUNDABLE: 15% de desconto sobre subtotal

## Exemplo de Requisição

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "Carla Dias",
    "guestEmail": "carla@example.com",
    "guestCpf": "11122233344",
    "checkIn": "2027-03-10",
    "checkOut": "2027-03-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Obter comprovante
curl http://localhost:7070/reservations/1/document

# Check-in
curl -X POST http://localhost:7070/reservations/1/check-in

# Check-out
curl -X POST http://localhost:7070/reservations/1/check-out
```

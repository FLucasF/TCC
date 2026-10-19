# Sistema de Reservas - Pousada Maré Alta

Um sistema completo de gerenciamento de reservas para uma pequena pousada no litoral, construído com Java 21 e Javalin 6.7.0.

## Características

### Gerenciamento de Quartos
- 5 quartos com tipos e preços configuráveis
  - Quartos 101-102: STANDARD (2 pessoas, R$ 249,90/noite)
  - Quartos 201-202: DELUXE (3 pessoas, R$ 389,90/noite)
  - Quarto 301: SUITE (4 pessoas, R$ 579,90/noite)

### Gestão de Reservas
- Estados: PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT (ou CANCELLED)
- Cálculo automático de preços com:
  - Temporada alta (15 dez - 28 fev + julho inteiro): +20% nas diárias
  - Arredondamento banker's rounding (meio para o par)
  - Planos: STANDARD (100%) e NON_REFUNDABLE (15% de desconto)

### Sistema de Notificações Extensível
Quando uma reserva é confirmada, notificações são enviadas automaticamente para:
- **Hóspede**: Comprovante da reserva
- **Governança**: Datas e número de hóspedes (preparação dos quartos)
- **Financeiro**: Valor recebido

Quando uma reserva confirmada é cancelada:
- **Governança**: Datas liberadas
- **Financeiro**: Valor a reembolsar (se aplicável)

O sistema foi arquitetado para facilitar a adição de novos notificadores (restaurante, transfer, etc.) sem modificar o código existente.

### Relatórios
- **Ocupação**: CSV com quartos ocupados em uma data específica
- **Financeiro**: CSV com reservas confirmadas/canceladas de um mês, incluindo reembolsos

## Instalação e Execução

### Pré-requisitos
- Java 21
- Maven 3.6+

### Build
```bash
mvn clean package
```

### Execução
```bash
# Porta padrão 7070
java -jar target/mare-alta.jar

# Ou com porta customizada
PORT=8080 java -jar target/mare-alta.jar
```

O servidor iniciará na porta especificada (ou 7070 se não configurada).

## API Endpoints

### Quartos
```
GET /rooms
```
Retorna lista de quartos disponíveis.

### Reservas
```
POST /reservations
GET /reservations/{id}
POST /reservations/{id}/confirmation
POST /reservations/{id}/check-in
POST /reservations/{id}/check-out
POST /reservations/{id}/cancellation
GET /reservations/{id}/document
```

### Mensagens Enviadas
```
GET /messages
```
Retorna todas as mensagens de confirmação enviadas.

### Relatórios
```
GET /reports/occupancy?date=2027-07-10
GET /reports/revenue?month=2027-07
```

## Testes

```bash
# Executar testes unitários
mvn test

# Verificar build completo
mvn verify
```

Todos os 13 testes passam, validando:
- Cálculos de preço (temporada alta, descontos)
- Transições de estado de reserva
- Validações (capacidade, disponibilidade)
- Sistema de notificações
- Extensibilidade para novos notificadores

## Exemplos de Uso

### Criar reserva
```bash
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": 201,
    "guestName": "João Silva",
    "guestEmail": "joao@example.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'
```

### Confirmar reserva
```bash
curl -X POST http://localhost:7070/reservations/1/confirmation \
  -H "Content-Type: application/json" \
  -d '{"amountPaid": 599.76}'
```

### Check-in
```bash
curl -X POST http://localhost:7070/reservations/1/check-in
```

### Check-out
```bash
curl -X POST http://localhost:7070/reservations/1/check-out
```

## Tratamento de Erros

A API retorna respostas estruturadas com códigos de erro:
- `400 INVALID_REQUEST`: Dados inválidos ou formato incorreto
- `404 ROOM_NOT_FOUND`: Quarto não existe
- `404 RESERVATION_NOT_FOUND`: Reserva não existe
- `409 ROOM_UNAVAILABLE`: Quarto ocupado nas datas solicitadas
- `409 OPERATION_NOT_ALLOWED`: Transição de estado não permitida
- `422 CAPACITY_EXCEEDED`: Mais hóspedes que a capacidade do quarto

## Arquitetura

```
src/main/java/br/com/pousada/marealta/
├── Main.java                    # Ponto de entrada
├── api/                         # Endpoints REST
│   ├── *Controller.java         # Controladores de cada recurso
│   ├── ApiErrorHandler.java     # Tratamento centralizado de erros
│   └── dto/                     # Classes DTO para request/response
├── service/                     # Lógica de negócio
│   ├── ReservationService.java  # Gerenciamento de reservas
│   ├── NotificationService.java # Sistema de notificações extensível
│   ├── ReportService.java       # Geração de relatórios
│   └── MessageRepository.java   # Armazenamento de mensagens
├── model/                       # Modelos de dados
│   ├── Reservation.java
│   ├── Room.java
│   ├── Message.java
│   └── enums (RoomType, ReservationStatus, RatePlan, MessageChannel)
├── util/                        # Utilidades
│   ├── MoneyUtil.java           # Cálculos monetários com arredondamento
│   └── DateUtil.java            # Validação de temporada
└── config/
    └── JacksonConfig.java       # Configuração JSON

src/test/java/br/com/pousada/marealta/service/
├── ReservationServiceTest.java
└── NotificationExtensibilityTest.java
```

## Dados Armazenados em Memória

O sistema armazena dados em memória durante a execução. Se o servidor reiniciar, todos os dados são perdidos. Para persistência permanente, seria necessário integrar um banco de dados.

## Notas de Implementação

- O sistema foi projetado seguindo o princípio SOLID, especialmente a extensibilidade para novos notificadores
- Validações ocorrem apenas nas fronteiras do sistema (entrada de API)
- Cálculos monetários usam arredondamento banker's rounding (HALF_EVEN) conforme especificado
- Datas de alta temporada são validadas automaticamente
- O sistema impede duplas reservas no mesmo quarto e noite

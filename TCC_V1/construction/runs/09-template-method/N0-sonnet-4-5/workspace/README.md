# Pousada Maré Alta - Sistema de Reservas

Sistema de gerenciamento de reservas para a Pousada Maré Alta, desenvolvido em Java 21 com Javalin.

## Características

- **5 quartos**: 2 Standard, 2 Deluxe e 1 Suíte
- **Alta temporada**: Preços 20% mais altos de 15/dez a fim de fev e durante julho
- **Planos de tarifa**: Standard (reembolsável) e Não Reembolsável (15% de desconto)
- **Ciclo de vida**: Pending → Confirmed → Checked-In → Checked-Out
- **Relatórios CSV**: Ocupação, Financeiro, Taxa de Turismo e Planos de Tarifa

## Requisitos

- Java 21
- Maven 3.6+

## Build

```bash
mvn package
```

Gera o JAR executável em `target/reservas-1.0.0.jar`.

## Execução

```bash
java -jar target/reservas-1.0.0.jar
```

O servidor inicia na porta definida pela variável `PORT`, ou 7070 se não definida.

```bash
PORT=8080 java -jar target/reservas-1.0.0.jar
```

## Testes

```bash
mvn verify
```

## API Endpoints

### Quartos

- `GET /rooms` - Lista todos os quartos

### Reservas

- `POST /reservations` - Cria uma nova reserva
- `GET /reservations/{id}` - Busca reserva por ID
- `POST /reservations/{id}/confirmation` - Confirma reserva
- `POST /reservations/{id}/cancellation` - Cancela reserva
- `POST /reservations/{id}/check-in` - Faz check-in
- `POST /reservations/{id}/check-out` - Faz check-out e retorna comprovante
- `GET /reservations/{id}/document` - Segunda via do comprovante

### Mensagens

- `GET /messages` - Lista e-mails enviados

### Relatórios (CSV)

- `GET /reports/occupancy?date=YYYY-MM-DD` - Ocupação em uma data
- `GET /reports/revenue?month=YYYY-MM` - Receita do mês
- `GET /reports/tourism-tax?month=YYYY-MM` - Taxa de turismo do mês
- `GET /reports/rate-plans?month=YYYY-MM` - Receita por plano de tarifa

## Exemplo de Uso

```bash
# Criar reserva
curl -X POST http://localhost:7070/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "roomNumber": "101",
    "guestName": "João Silva",
    "guestEmail": "joao@email.com",
    "guestCpf": "12345678900",
    "checkIn": "2027-07-10",
    "checkOut": "2027-07-12",
    "guests": 2,
    "ratePlan": "STANDARD"
  }'

# Confirmar reserva
curl -X POST http://localhost:7070/reservations/1/confirmation

# Relatório de ocupação
curl "http://localhost:7070/reports/occupancy?date=2027-07-10"
```

## Arquitetura

O sistema segue uma arquitetura em camadas:

- **Domain**: Modelos de negócio (Room, Reservation, Message, etc.)
- **Repository**: Armazenamento em memória
- **Service**: Lógica de negócio (ReservationService, PricingService, ReportService)
- **API**: Handlers HTTP com Javalin

### Extensibilidade

O sistema foi projetado para facilitar futuras mudanças:

- **Relatórios**: A classe `ReportBuilder` centraliza a geração de CSV, facilitando a adição de novos campos (como data de geração)
- **Taxa de turismo**: Valor configurável em `PricingService.TOURISM_TAX_PER_GUEST_PER_NIGHT`
- **Novos relatórios**: Podem ser adicionados em `ReportService` seguindo o mesmo padrão

## Códigos de Erro

- `400 INVALID_REQUEST` - Dados inválidos ou campos faltando
- `404 ROOM_NOT_FOUND` - Quarto não encontrado
- `404 RESERVATION_NOT_FOUND` - Reserva não encontrada
- `409 ROOM_UNAVAILABLE` - Quarto indisponível nas datas
- `409 OPERATION_NOT_ALLOWED` - Operação não permitida no estado atual
- `422 CAPACITY_EXCEEDED` - Número de hóspedes excede capacidade
